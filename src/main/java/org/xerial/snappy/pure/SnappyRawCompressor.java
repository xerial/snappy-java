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
import java.util.Arrays;

import static org.xerial.snappy.pure.SnappyConstants.COPY_1_BYTE_OFFSET;
import static org.xerial.snappy.pure.SnappyConstants.COPY_2_BYTE_OFFSET;
import static org.xerial.snappy.pure.SnappyConstants.SIZE_OF_INT;
import static org.xerial.snappy.pure.SnappyConstants.SIZE_OF_LONG;
import static org.xerial.snappy.pure.SnappyConstants.SIZE_OF_SHORT;
import static org.xerial.snappy.pure.UnsafeUtil.UNSAFE;
import static java.lang.Integer.reverseBytes;
import static java.lang.Long.reverseBytes;
import static java.lang.Short.reverseBytes;

public final class SnappyRawCompressor
{
    // The size of a compression block. Note that many parts of the compression
    // code assumes that BLOCK_SIZE <= 65536; in particular, the hash table
    // can only store 16-bit offsets, and EmitCopy() also assumes the offset
    // is 65535 bytes or less. Note also that if you change this, it will
    // affect the framing format (see framing_format.txt).
    //
    // Note that there might be older data around that is compressed with larger
    // block sizes, so the decompression code should not rely on the
    // non-existence of long back-references.
    private static final int BLOCK_LOG = 16;
    private static final int BLOCK_SIZE = 1 << BLOCK_LOG;

    private static final int INPUT_MARGIN_BYTES = 15;

    private static final int MAX_HASH_TABLE_BITS = 14;
    public static final int MAX_HASH_TABLE_SIZE = 1 << MAX_HASH_TABLE_BITS;

    private static final ByteOrder byteOrder = ByteOrder.nativeOrder();

    private SnappyRawCompressor() {}

    private static int littleEndian(int i)
    {
        return (byteOrder == ByteOrder.LITTLE_ENDIAN) ? i : reverseBytes(i);
    }

    private static long littleEndian(long i)
    {
        return (byteOrder == ByteOrder.LITTLE_ENDIAN) ? i : reverseBytes(i);
    }

    private static short littleEndian(short i)
    {
        return (byteOrder == ByteOrder.LITTLE_ENDIAN) ? i : reverseBytes(i);
    }

    public static int maxCompressedLength(int sourceLength)
    {
        // Compressed data can be defined as:
        //    compressed := item* literal*
        //    item       := literal* copy
        //
        // The trailing literal sequence has a space blowup of at most 62/60
        // since a literal of length 60 needs one tag byte + one extra byte
        // for length information.
        //
        // Item blowup is trickier to measure.  Suppose the "copy" op copies
        // 4 bytes of data.  Because of a special check in the encoding code,
        // we produce a 4-byte copy only if the offset is < 65536.  Therefore
        // the copy op takes 3 bytes to encode, and this type of item leads
        // to at most the 62/60 blowup for representing literals.
        //
        // Suppose the "copy" op copies 5 bytes of data.  If the offset is big
        // enough, it will take 5 bytes to encode the copy op.  Therefore the
        // worst case here is a one-byte literal followed by a five-byte copy.
        // I.e., 6 bytes of input turn into 7 bytes of "compressed" data.
        //
        // This last factor dominates the blowup, so the final estimate is:
        return 32 + sourceLength + sourceLength / 6;
    }

    // suppress warnings is required to use assert
    @SuppressWarnings("IllegalToken")
    public static int compress(
            final Object inputBase,
            final long inputAddress,
            final long inputLimit,
            final Object outputBase,
            final long outputAddress,
            final long outputLimit,
            final short[] table)
    {
        // The compression code assumes output is larger than the max compression size (with 32 bytes of
        // extra padding), and does not check bounds for writing to output.
        int maxCompressedLength = maxCompressedLength((int) (inputLimit - inputAddress));
        if (outputLimit - outputAddress < maxCompressedLength) {
            throw new IllegalArgumentException("Output buffer must be at least " + maxCompressedLength + " bytes");
        }

        // First write the uncompressed size to the output as a variable length int
        long output = writeUncompressedLength(outputBase, outputAddress, (int) (inputLimit - inputAddress));

        for (long blockAddress = inputAddress; blockAddress < inputLimit; blockAddress += BLOCK_SIZE) {
            final long blockLimit = Math.min(inputLimit, blockAddress + BLOCK_SIZE);
            long input = blockAddress;
            assert blockLimit - blockAddress <= BLOCK_SIZE;

            int blockHashTableSize = getHashTableSize((int) (blockLimit - blockAddress));
            Arrays.fill(table, 0, blockHashTableSize, (short) 0);

            // todo given that hashTableSize is required to be a power of 2, this is overly complex
            final int shift = 32 - log2Floor(blockHashTableSize);
            assert (blockHashTableSize & (blockHashTableSize - 1)) == 0 : "table must be power of two";
            assert 0xFFFFFFFF >>> shift == blockHashTableSize - 1;

            // Bytes in [nextEmitAddress, input) will be emitted as literal bytes.  Or
            // [nextEmitAddress, inputLimit) after the main loop.
            long nextEmitAddress = input;

            final long fastInputLimit = blockLimit - INPUT_MARGIN_BYTES;
            while (input <= fastInputLimit) {
                assert nextEmitAddress <= input;

                // The body of this loop emits a literal once and then emits a copy one
                // or more times.  (The exception is that when we're close to exhausting
                // the input we exit and emit a literal.)
                //
                // In the first iteration of this loop we're just starting, so
                // there's nothing to copy, so we must emit a literal once.  And we
                // only start a new iteration when the current iteration has determined
                // that a literal will precede the next copy (if any).
                //
                // Step 1: Scan forward in the input looking for a 4-byte-long match.
                // If we get close to exhausting the input exit and emit a final literal.
                //
                // Heuristic match skipping: If 32 bytes are scanned with no matches
                // found, start looking only at every other byte. If 32 more bytes are
                // scanned, look at every third byte, etc.. When a match is found,
                // immediately go back to looking at every byte. This is a small loss
                // (~5% performance, ~0.1% density) for compressible data due to more
                // bookkeeping, but for non-compressible data (such as JPEG) it's a huge
                // win since the compressor quickly "realizes" the data is incompressible
                // and doesn't bother looking for matches everywhere.
                //
                // The "skip" variable keeps track of how many bytes there are since the
                // last match; dividing it by 32 (ie. right-shifting by five) gives the
                // number of bytes to move ahead for each iteration.
                int skip = 32;

                long candidateIndex = 0;
                for (input += 1; input + (skip >>> 5) <= fastInputLimit; input += ((skip++) >>> 5)) {
                    // hash the 4 bytes starting at the input pointer
                    int currentInt = littleEndian(UNSAFE.getInt(inputBase, input));
                    int hash = hashBytes(currentInt, shift);

                    // get the position of a 4 bytes sequence with the same hash
                    candidateIndex = blockAddress + (table[hash] & 0xFFFF);
                    assert candidateIndex >= 0;
                    assert candidateIndex < input;

                    // update the hash to point to the current position
                    table[hash] = (short) (input - blockAddress);

                    // if the 4 byte sequence a the candidate index matches the sequence at the
                    // current position, proceed to the next phase
                    if (currentInt == littleEndian(UNSAFE.getInt(inputBase, candidateIndex))) {
                        break;
                    }
                }
                if (input + (skip >>> 5) > fastInputLimit) {
                    break;
                }

                // Step 2: A 4-byte match has been found.  We'll later see if more
                // than 4 bytes match.  But, prior to the match, input
                // bytes [nextEmit, ip) are unmatched.  Emit them as "literal bytes."
                assert nextEmitAddress + 16 <= blockLimit;

                int literalLength = (int) (input - nextEmitAddress);
                output = emitLiteralLength(outputBase, output, literalLength);

                // Fast copy can use 8 extra bytes of input and output, which is safe because:
                //   - The input will always have INPUT_MARGIN_BYTES = 15 extra available bytes
                //   - The output will always have 32 spare bytes (see MaxCompressedLength).
                output = fastCopy(inputBase, nextEmitAddress, outputBase, output, literalLength);

                // Step 3: Call EmitCopy, and then see if another EmitCopy could
                // be our next move.  Repeat until we find no match for the
                // input immediately after what was consumed by the last EmitCopy call.
                //
                // If we exit this loop normally then we need to call EmitLiteral next,
                // though we don't yet know how big the literal will be.  We handle that
                // by proceeding to the next iteration of the main loop.  We also can exit
                // this loop via goto if we get close to exhausting the input.
                int inputBytes;
                do {
                    // We have a 4-byte match at input, and no need to emit any
                    // "literal bytes" prior to input.
                    assert (blockLimit >= input + SIZE_OF_INT);

                    // determine match length
                    int matched = count(inputBase, input + SIZE_OF_INT, candidateIndex + SIZE_OF_INT, blockLimit);
                    matched += SIZE_OF_INT;

                    // Emit the copy operation for this chunk
                    output = emitCopy(outputBase, output, input, candidateIndex, matched);
                    input += matched;

                    // are we done?
                    if (input >= fastInputLimit) {
                        break;
                    }

                    // We could immediately start working at input now, but to improve
                    // compression we first update table[Hash(ip - 1, ...)].
                    long longValue = littleEndian((UNSAFE.getLong(inputBase, input - 1)));
                    int prevInt = (int) longValue;
                    inputBytes = (int) (longValue >>> 8);

                    // add hash starting with previous byte
                    int prevHash = hashBytes(prevInt, shift);
                    table[prevHash] = (short) (input - blockAddress - 1);

                    // update hash of current byte
                    int curHash = hashBytes(inputBytes, shift);

                    candidateIndex = blockAddress + (table[curHash] & 0xFFFF);
                    table[curHash] = (short) (input - blockAddress);
                } while (inputBytes == littleEndian(UNSAFE.getInt(inputBase, candidateIndex)));
                nextEmitAddress = input;
            }

            // Emit the remaining bytes as a literal
            if (nextEmitAddress < blockLimit) {
                int literalLength = (int) (blockLimit - nextEmitAddress);
                output = emitLiteralLength(outputBase, output, literalLength);
                UNSAFE.copyMemory(inputBase, nextEmitAddress, outputBase, output, literalLength);
                output += literalLength;
            }
        }

        return (int) (output - outputAddress);
    }

    private static int count(Object inputBase, final long start, long matchStart, long matchLimit)
    {
        long current = start;

        // first, compare long at a time
        while (current < matchLimit - (SIZE_OF_LONG - 1)) {
            long diff = littleEndian(UNSAFE.getLong(inputBase, matchStart)) ^ littleEndian(UNSAFE.getLong(inputBase, current));
            if (diff != 0) {
                current += Long.numberOfTrailingZeros(diff) >> 3;
                return (int) (current - start);
            }

            current += SIZE_OF_LONG;
            matchStart += SIZE_OF_LONG;
        }

        if (current < matchLimit - (SIZE_OF_INT - 1) && littleEndian(UNSAFE.getInt(inputBase, matchStart)) == littleEndian(UNSAFE.getInt(inputBase, current))) {
            current += SIZE_OF_INT;
            matchStart += SIZE_OF_INT;
        }

        if (current < matchLimit - (SIZE_OF_SHORT - 1) && littleEndian(UNSAFE.getShort(inputBase, matchStart)) == littleEndian(UNSAFE.getShort(inputBase, current))) {
            current += SIZE_OF_SHORT;
            matchStart += SIZE_OF_SHORT;
        }

        if (current < matchLimit && UNSAFE.getByte(inputBase, matchStart) == UNSAFE.getByte(inputBase, current)) {
            ++current;
        }

        return (int) (current - start);
    }

    private static long emitLiteralLength(Object outputBase, long output, int literalLength)
    {
        int n = literalLength - 1;      // Zero-length literals are disallowed
        if (n < 60) {
            // Size fits in tag byte
            UNSAFE.putByte(outputBase, output++, (byte) (n << 2));
        }
        else {
            int bytes;
            if (n < (1 << 8)) {
                UNSAFE.putByte(outputBase, output++, (byte) (59 + 1 << 2));
                bytes = 1;
            }
            else if (n < (1 << 16)) {
                UNSAFE.putByte(outputBase, output++, (byte) (59 + 2 << 2));
                bytes = 2;
            }
            else if (n < (1 << 24)) {
                UNSAFE.putByte(outputBase, output++, (byte) (59 + 3 << 2));
                bytes = 3;
            }
            else {
                UNSAFE.putByte(outputBase, output++, (byte) (59 + 4 << 2));
                bytes = 4;
            }
            // System is assumed to be little endian, so low bytes will be zero for the smaller numbers
            UNSAFE.putInt(outputBase, output, littleEndian(n));
            output += bytes;
        }
        return output;
    }

    private static long fastCopy(final Object inputBase, long input, final Object outputBase, long output, final int literalLength)
    {
        final long outputLimit = output + literalLength;
        do {
            UNSAFE.putLong(outputBase, output, UNSAFE.getLong(inputBase, input));
            input += SIZE_OF_LONG;
            output += SIZE_OF_LONG;
        }
        while (output < outputLimit);
        return outputLimit;
    }

    private static long emitCopy(Object outputBase, long output, long input, long matchIndex, int matchLength)
    {
        long offset = input - matchIndex;

        // Emit 64 byte copies but make sure to keep at least four bytes reserved
        while (matchLength >= 68) {
            UNSAFE.putByte(outputBase, output++, (byte) (COPY_2_BYTE_OFFSET + ((64 - 1) << 2)));
            UNSAFE.putShort(outputBase, output, littleEndian((short) offset));
            output += SIZE_OF_SHORT;
            matchLength -= 64;
        }

        // Emit an extra 60 byte copy if have too much data to fit in one copy
        // length < 68
        if (matchLength > 64) {
            UNSAFE.putByte(outputBase, output++, (byte) (COPY_2_BYTE_OFFSET + ((60 - 1) << 2)));
            UNSAFE.putShort(outputBase, output, littleEndian((short) offset));
            output += SIZE_OF_SHORT;
            matchLength -= 60;
        }

        // Emit remainder
        if ((matchLength < 12) && (offset < 2048)) {
            int lenMinus4 = matchLength - 4;
            UNSAFE.putByte(outputBase, output++, (byte) (COPY_1_BYTE_OFFSET + ((lenMinus4) << 2) + ((offset >>> 8) << 5)));
            UNSAFE.putByte(outputBase, output++, (byte) (offset));
        }
        else {
            UNSAFE.putByte(outputBase, output++, (byte) (COPY_2_BYTE_OFFSET + ((matchLength - 1) << 2)));
            UNSAFE.putShort(outputBase, output, littleEndian((short) offset));
            output += SIZE_OF_SHORT;
        }
        return output;
    }

    @SuppressWarnings("IllegalToken")
    private static int getHashTableSize(int inputSize)
    {
        // Use smaller hash table when input.size() is smaller, since we
        // fill the table, incurring O(hash table size) overhead for
        // compression, and if the input is short, we won't need that
        // many hash table entries anyway.
        assert (MAX_HASH_TABLE_SIZE >= 256);

        // smallest power of 2 larger than inputSize
        int target = Integer.highestOneBit(inputSize - 1) << 1;

        // keep it between MIN_TABLE_SIZE and MAX_TABLE_SIZE
        return Math.max(Math.min(target, MAX_HASH_TABLE_SIZE), 256);
    }

    // Any hash function will produce a valid compressed stream, but a good
    // hash function reduces the number of collisions and thus yields better
    // compression for compressible input, and more speed for incompressible
    // input. Of course, it doesn't hurt if the hash function is reasonably fast
    // either, as it gets called a lot.
    private static int hashBytes(int value, int shift)
    {
        return (value * 0x1e35a7bd) >>> shift;
    }

    private static int log2Floor(int n)
    {
        return n == 0 ? -1 : 31 ^ Integer.numberOfLeadingZeros(n);
    }

    private static final int HIGH_BIT_MASK = 0x80;
    /**
     * Writes the uncompressed length as variable length integer.
     */
    private static long writeUncompressedLength(Object outputBase, long outputAddress, int uncompressedLength)
    {
        if (uncompressedLength < (1 << 7) && uncompressedLength >= 0) {
            UNSAFE.putByte(outputBase, outputAddress++, (byte) (uncompressedLength));
        }
        else if (uncompressedLength < (1 << 14) && uncompressedLength > 0) {
            UNSAFE.putByte(outputBase, outputAddress++, (byte) (uncompressedLength | HIGH_BIT_MASK));
            UNSAFE.putByte(outputBase, outputAddress++, (byte) (uncompressedLength >>> 7));
        }
        else if (uncompressedLength < (1 << 21) && uncompressedLength > 0) {
            UNSAFE.putByte(outputBase, outputAddress++, (byte) (uncompressedLength | HIGH_BIT_MASK));
            UNSAFE.putByte(outputBase, outputAddress++, (byte) ((uncompressedLength >>> 7) | HIGH_BIT_MASK));
            UNSAFE.putByte(outputBase, outputAddress++, (byte) (uncompressedLength >>> 14));
        }
        else if (uncompressedLength < (1 << 28) && uncompressedLength > 0) {
            UNSAFE.putByte(outputBase, outputAddress++, (byte) (uncompressedLength | HIGH_BIT_MASK));
            UNSAFE.putByte(outputBase, outputAddress++, (byte) ((uncompressedLength >>> 7) | HIGH_BIT_MASK));
            UNSAFE.putByte(outputBase, outputAddress++, (byte) ((uncompressedLength >>> 14) | HIGH_BIT_MASK));
            UNSAFE.putByte(outputBase, outputAddress++, (byte) (uncompressedLength >>> 21));
        }
        else {
            UNSAFE.putByte(outputBase, outputAddress++, (byte) (uncompressedLength | HIGH_BIT_MASK));
            UNSAFE.putByte(outputBase, outputAddress++, (byte) ((uncompressedLength >>> 7) | HIGH_BIT_MASK));
            UNSAFE.putByte(outputBase, outputAddress++, (byte) ((uncompressedLength >>> 14) | HIGH_BIT_MASK));
            UNSAFE.putByte(outputBase, outputAddress++, (byte) ((uncompressedLength >>> 21) | HIGH_BIT_MASK));
            UNSAFE.putByte(outputBase, outputAddress++, (byte) (uncompressedLength >>> 28));
        }
        return outputAddress;
    }
}
