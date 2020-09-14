/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.xerial.snappy.pure;

import java.nio.ByteOrder;

import org.xerial.snappy.SnappyError;
import org.xerial.snappy.SnappyErrorCode;

import static org.xerial.snappy.pure.SnappyConstants.LITERAL;
import static org.xerial.snappy.pure.SnappyConstants.SIZE_OF_INT;
import static org.xerial.snappy.pure.SnappyConstants.SIZE_OF_LONG;
import static org.xerial.snappy.pure.UnsafeUtil.UNSAFE;
import static java.lang.Integer.reverseBytes;

public final class SnappyRawDecompressor
{
    private static final int[] DEC_32_TABLE = {4, 1, 2, 1, 4, 4, 4, 4};
    private static final int[] DEC_64_TABLE = {0, 0, 0, -1, 0, 1, 2, 3};

    private SnappyRawDecompressor() {}

    private static final ByteOrder byteOrder = ByteOrder.nativeOrder();

    private static int littleEndian(int i) {
        return (byteOrder == ByteOrder.LITTLE_ENDIAN) ? i : reverseBytes(i);
    }

    public static int getUncompressedLength(Object compressed, long compressedAddress, long compressedLimit)
    {
        return readUncompressedLength(compressed, compressedAddress, compressedLimit)[0];
    }

    public static int decompress(
            final Object inputBase,
            final long inputAddress,
            final long inputLimit,
            final Object outputBase,
            final long outputAddress,
            final long outputLimit)
    {
        // Read the uncompressed length from the front of the input
        long input = inputAddress;
        int[] varInt = readUncompressedLength(inputBase, input, inputLimit);
        int expectedLength = varInt[0];
        input += varInt[1];

        if(!(expectedLength <= (outputLimit - outputAddress))) {
            throw new SnappyError(SnappyErrorCode.INVALID_CHUNK_SIZE, String.format("Uncompressed length %s must be less than %s", expectedLength, (outputLimit - outputAddress)));
        }

        // Process the entire input
        int uncompressedSize = uncompressAll(
                inputBase,
                input,
                inputLimit,
                outputBase,
                outputAddress,
                outputLimit);

        if (!(expectedLength == uncompressedSize)) {
            throw new SnappyError(SnappyErrorCode.INVALID_CHUNK_SIZE, String.format("Recorded length is %s bytes but actual length after decompression is %s bytes ",
                    expectedLength,
                    uncompressedSize));
        }

        return expectedLength;
    }

    private static int uncompressAll(
            final Object inputBase,
            final long inputAddress,
            final long inputLimit,
            final Object outputBase,
            final long outputAddress,
            final long outputLimit)
    {
        final long fastOutputLimit = outputLimit - SIZE_OF_LONG; // maximum offset in output buffer to which it's safe to write long-at-a-time

        long output = outputAddress;
        long input = inputAddress;

        while (input < inputLimit) {
            int opCode = UNSAFE.getByte(inputBase, input++) & 0xFF;
            int entry = opLookupTable[opCode] & 0xFFFF;

            int trailerBytes = entry >>> 11;
            int trailer = 0;
            if (input + SIZE_OF_INT < inputLimit) {
                trailer = littleEndian(UNSAFE.getInt(inputBase, input)) & wordmask[trailerBytes];
            }
            else {
                if (input + trailerBytes > inputLimit) {
                    throw new SnappyError(SnappyErrorCode.PARSING_ERROR, String.format("position: %d", input - inputAddress));
                }
                switch (trailerBytes) {
                    case 4:
                        trailer = (UNSAFE.getByte(inputBase, input + 3) & 0xff) << 24;
                    case 3:
                        trailer |= (UNSAFE.getByte(inputBase, input + 2) & 0xff) << 16;
                    case 2:
                        trailer |= (UNSAFE.getByte(inputBase, input + 1) & 0xff) << 8;
                    case 1:
                        trailer |= (UNSAFE.getByte(inputBase, input) & 0xff);
                }
            }
            if (trailer < 0) {
                throw new SnappyError(SnappyErrorCode.PARSING_ERROR, String.format("position: %d", input - inputAddress));
            }
            input += trailerBytes;

            int length = entry & 0xff;
            if (length == 0) {
                continue;
            }

            if ((opCode & 0x3) == LITERAL) {
                int literalLength = length + trailer;

                // copy literal
                long literalOutputLimit = output + literalLength;
                if (literalOutputLimit > fastOutputLimit || input + literalLength > inputLimit - SIZE_OF_LONG) {
                    if (literalOutputLimit > outputLimit) {
                        throw new SnappyError(SnappyErrorCode.PARSING_ERROR, String.format("position: %d", input - inputAddress));
                    }

                    // slow, precise copy
                    UNSAFE.copyMemory(inputBase, input, outputBase, output, literalLength);
                    input += literalLength;
                    output += literalLength;
                }
                else {
                    // fast copy. We may over-copy but there's enough room in input and output to not overrun them
                    do {
                        UNSAFE.putLong(outputBase, output, UNSAFE.getLong(inputBase, input));
                        input += SIZE_OF_LONG;
                        output += SIZE_OF_LONG;
                    }
                    while (output < literalOutputLimit);
                    input -= (output - literalOutputLimit); // adjust index if we over-copied
                    output = literalOutputLimit;
                }
            }
            else {
                // matchOffset/256 is encoded in bits 8..10.  By just fetching
                // those bits, we get matchOffset (since the bit-field starts at
                // bit 8).
                int matchOffset = entry & 0x700;
                matchOffset += trailer;

                long matchAddress = output - matchOffset;
                if (matchAddress < outputAddress || output + length > outputLimit) {
                    throw new SnappyError(SnappyErrorCode.PARSING_ERROR, String.format("position: %d", input - inputAddress));
                }
                long matchOutputLimit = output + length;

                if (output > fastOutputLimit) {
                    // slow match copy
                    while (output < matchOutputLimit) {
                        UNSAFE.putByte(outputBase, output++, UNSAFE.getByte(outputBase, matchAddress++));
                    }
                }
                else {
                    // copy repeated sequence
                    if (matchOffset < SIZE_OF_LONG) {
                        // 8 bytes apart so that we can copy long-at-a-time below
                        int increment32 = DEC_32_TABLE[matchOffset];
                        int decrement64 = DEC_64_TABLE[matchOffset];

                        UNSAFE.putByte(outputBase, output, UNSAFE.getByte(outputBase, matchAddress));
                        UNSAFE.putByte(outputBase, output + 1, UNSAFE.getByte(outputBase, matchAddress + 1));
                        UNSAFE.putByte(outputBase, output + 2, UNSAFE.getByte(outputBase, matchAddress + 2));
                        UNSAFE.putByte(outputBase, output + 3, UNSAFE.getByte(outputBase, matchAddress + 3));
                        output += SIZE_OF_INT;
                        matchAddress += increment32;

                        UNSAFE.putInt(outputBase, output, UNSAFE.getInt(outputBase, matchAddress));
                        output += SIZE_OF_INT;
                        matchAddress -= decrement64;
                    }
                    else {
                        UNSAFE.putLong(outputBase, output, UNSAFE.getLong(outputBase, matchAddress));
                        matchAddress += SIZE_OF_LONG;
                        output += SIZE_OF_LONG;
                    }

                    if (matchOutputLimit > fastOutputLimit) {
                        if (matchOutputLimit > outputLimit) {
                            throw new SnappyError(SnappyErrorCode.PARSING_ERROR, String.format("position: %d", input - inputAddress));
                        }

                        while (output < fastOutputLimit) {
                            UNSAFE.putLong(outputBase, output, UNSAFE.getLong(outputBase, matchAddress));
                            matchAddress += SIZE_OF_LONG;
                            output += SIZE_OF_LONG;
                        }

                        while (output < matchOutputLimit) {
                            UNSAFE.putByte(outputBase, output++, UNSAFE.getByte(outputBase, matchAddress++));
                        }
                    }
                    else {
                        while (output < matchOutputLimit) {
                            UNSAFE.putLong(outputBase, output, UNSAFE.getLong(outputBase, matchAddress));
                            matchAddress += SIZE_OF_LONG;
                            output += SIZE_OF_LONG;
                        }
                    }
                }
                output = matchOutputLimit; // correction in case we over-copied
            }
        }

        return (int) (output - outputAddress);
    }

    // Mapping from i in range [0,4] to a mask to extract the bottom 8*i bits
    private static final int[] wordmask = new int[] {
            0, 0xff, 0xffff, 0xffffff, 0xffffffff
    };

    // Data stored per entry in lookup table:
    //      Range   Bits-used       Description
    //      ------------------------------------
    //      1..64   0..7            Literal/copy length encoded in opcode byte
    //      0..7    8..10           Copy offset encoded in opcode byte / 256
    //      0..4    11..13          Extra bytes after opcode
    //
    // We use eight bits for the length even though 7 would have sufficed
    // because of efficiency reasons:
    //      (1) Extracting a byte is faster than a bit-field
    //      (2) It properly aligns copy offset so we do not need a <<8
    private static final short[] opLookupTable = new short[] {
            0x0001, 0x0804, 0x1001, 0x2001, 0x0002, 0x0805, 0x1002, 0x2002,
            0x0003, 0x0806, 0x1003, 0x2003, 0x0004, 0x0807, 0x1004, 0x2004,
            0x0005, 0x0808, 0x1005, 0x2005, 0x0006, 0x0809, 0x1006, 0x2006,
            0x0007, 0x080a, 0x1007, 0x2007, 0x0008, 0x080b, 0x1008, 0x2008,
            0x0009, 0x0904, 0x1009, 0x2009, 0x000a, 0x0905, 0x100a, 0x200a,
            0x000b, 0x0906, 0x100b, 0x200b, 0x000c, 0x0907, 0x100c, 0x200c,
            0x000d, 0x0908, 0x100d, 0x200d, 0x000e, 0x0909, 0x100e, 0x200e,
            0x000f, 0x090a, 0x100f, 0x200f, 0x0010, 0x090b, 0x1010, 0x2010,
            0x0011, 0x0a04, 0x1011, 0x2011, 0x0012, 0x0a05, 0x1012, 0x2012,
            0x0013, 0x0a06, 0x1013, 0x2013, 0x0014, 0x0a07, 0x1014, 0x2014,
            0x0015, 0x0a08, 0x1015, 0x2015, 0x0016, 0x0a09, 0x1016, 0x2016,
            0x0017, 0x0a0a, 0x1017, 0x2017, 0x0018, 0x0a0b, 0x1018, 0x2018,
            0x0019, 0x0b04, 0x1019, 0x2019, 0x001a, 0x0b05, 0x101a, 0x201a,
            0x001b, 0x0b06, 0x101b, 0x201b, 0x001c, 0x0b07, 0x101c, 0x201c,
            0x001d, 0x0b08, 0x101d, 0x201d, 0x001e, 0x0b09, 0x101e, 0x201e,
            0x001f, 0x0b0a, 0x101f, 0x201f, 0x0020, 0x0b0b, 0x1020, 0x2020,
            0x0021, 0x0c04, 0x1021, 0x2021, 0x0022, 0x0c05, 0x1022, 0x2022,
            0x0023, 0x0c06, 0x1023, 0x2023, 0x0024, 0x0c07, 0x1024, 0x2024,
            0x0025, 0x0c08, 0x1025, 0x2025, 0x0026, 0x0c09, 0x1026, 0x2026,
            0x0027, 0x0c0a, 0x1027, 0x2027, 0x0028, 0x0c0b, 0x1028, 0x2028,
            0x0029, 0x0d04, 0x1029, 0x2029, 0x002a, 0x0d05, 0x102a, 0x202a,
            0x002b, 0x0d06, 0x102b, 0x202b, 0x002c, 0x0d07, 0x102c, 0x202c,
            0x002d, 0x0d08, 0x102d, 0x202d, 0x002e, 0x0d09, 0x102e, 0x202e,
            0x002f, 0x0d0a, 0x102f, 0x202f, 0x0030, 0x0d0b, 0x1030, 0x2030,
            0x0031, 0x0e04, 0x1031, 0x2031, 0x0032, 0x0e05, 0x1032, 0x2032,
            0x0033, 0x0e06, 0x1033, 0x2033, 0x0034, 0x0e07, 0x1034, 0x2034,
            0x0035, 0x0e08, 0x1035, 0x2035, 0x0036, 0x0e09, 0x1036, 0x2036,
            0x0037, 0x0e0a, 0x1037, 0x2037, 0x0038, 0x0e0b, 0x1038, 0x2038,
            0x0039, 0x0f04, 0x1039, 0x2039, 0x003a, 0x0f05, 0x103a, 0x203a,
            0x003b, 0x0f06, 0x103b, 0x203b, 0x003c, 0x0f07, 0x103c, 0x203c,
            0x0801, 0x0f08, 0x103d, 0x203d, 0x1001, 0x0f09, 0x103e, 0x203e,
            0x1801, 0x0f0a, 0x103f, 0x203f, 0x2001, 0x0f0b, 0x1040, 0x2040
    };

    /**
     * Reads the variable length integer encoded a the specified offset, and
     * returns this length with the number of bytes read.
     */
    static int[] readUncompressedLength(Object compressed, long compressedAddress, long compressedLimit)
    {
        int result;
        int bytesRead = 0;
        {
            int b = getUnsignedByteSafe(compressed, compressedAddress + bytesRead, compressedLimit);
            bytesRead++;
            result = b & 0x7f;
            if ((b & 0x80) != 0) {
                b = getUnsignedByteSafe(compressed, compressedAddress + bytesRead, compressedLimit);
                bytesRead++;
                result |= (b & 0x7f) << 7;
                if ((b & 0x80) != 0) {
                    b = getUnsignedByteSafe(compressed, compressedAddress + bytesRead, compressedLimit);
                    bytesRead++;
                    result |= (b & 0x7f) << 14;
                    if ((b & 0x80) != 0) {
                        b = getUnsignedByteSafe(compressed, compressedAddress + bytesRead, compressedLimit);
                        bytesRead++;
                        result |= (b & 0x7f) << 21;
                        if ((b & 0x80) != 0) {
                            b = getUnsignedByteSafe(compressed, compressedAddress + bytesRead, compressedLimit);
                            bytesRead++;
                            result |= (b & 0x7f) << 28;
                            if ((b & 0x80) != 0) {
                                throw new SnappyError(SnappyErrorCode.PARSING_ERROR, String.format("position: %d, error: %s", compressedAddress + bytesRead, "last byte of compressed length int has high bit set"));
                            }
                        }
                    }
                }
            }
        }
        return new int[] {result, bytesRead};
    }

    private static int getUnsignedByteSafe(Object base, long address, long limit)
    {
        if (address >= limit) {
            throw new SnappyError(SnappyErrorCode.PARSING_ERROR, String.format("position: %d, error: %s", limit - address, "Input is truncated"));
        }
        return UNSAFE.getByte(base, address) & 0xFF;
    }
}
