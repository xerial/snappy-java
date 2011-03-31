/*--------------------------------------------------------------------------
 *  Copyright 2011 Taro L. Saito
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *--------------------------------------------------------------------------*/
//--------------------------------------
// snappy-java Project
//
// Snappy.java
// Since: 2011/03/29
//
// $URL$ 
// $Author$
//--------------------------------------
package org.xerial.snappy;

import java.nio.ByteBuffer;

/**
 * Snappy API for data compression/decompression
 * 
 * @author leo
 * 
 */
public class Snappy
{

    /**
     * Get the native library version of the snappy
     * 
     * @return native library version
     */
    public static String getNativeLibraryVersion() {
        return SnappyNative.nativeLibraryVersion();
    }

    /**
     * Compress the content in the given input buffer. After the compression,
     * you can retrieve the compressed data from the output buffer [pos() ...
     * limit()) (compressed data size = limit() - pos() = remaining())
     * 
     * @param uncompressed
     *            buffer[pos() ... limit()) containing the input data
     * @param compressed
     *            output of the compressed data. Uses range [pos()..].
     * @return byte size of the compressed data.
     * 
     * @throws SnappyError
     *             when the input is not a direct buffer
     */
    public static int compress(ByteBuffer uncompressed, ByteBuffer compressed) throws SnappyException {

        if (!uncompressed.isDirect())
            throw new SnappyError(SnappyErrorCode.NOT_A_DIRECT_BUFFER, "input is not a direct buffer");
        if (!compressed.isDirect())
            throw new SnappyError(SnappyErrorCode.NOT_A_DIRECT_BUFFER, "destination is not a direct buffer");

        // input: uncompressed[pos(), limit())
        // output: compressed
        int uPos = uncompressed.position();
        int uLen = uncompressed.remaining();
        int compressedSize = SnappyNative.rawCompress(uncompressed, uPos, uLen, compressed, compressed.position());

        //         pos  limit
        // [ ......BBBBBBB.........]
        compressed.limit(compressed.position() + compressedSize);

        return compressedSize;
    }

    /**
     * Compress the input buffer content in [inputOffset,
     * ...inputOffset+inputLength) then output to the specified output buffer.
     * 
     * @param input
     * @param inputOffset
     * @param inputLength
     * @param output
     * @param outputOffset
     * @return byte size of the compressed data
     * @throws SnappyException
     *             when failed to access the input/output buffer
     */
    public static int compress(byte[] input, int inputOffset, int inputLength, byte[] output, int outputOffset)
            throws SnappyException {
        if (input == null || output == null)
            throw new NullPointerException("input or output is null");

        int compressedSize = SnappyNative.rawCompress(input, inputOffset, inputLength, output, outputOffset);
        return compressedSize;
    }

    /**
     * Uncompress the content in the input buffer. The result is dumped to the
     * specified output buffer.
     * 
     * Note that if you pass the wrong data or the range [pos(), limit()) that
     * cannot be uncompressed, your JVM might crash due to the access violation
     * exception issued in the native code written in C++. To avoid this type of
     * crash, use {@link #isValidCompressedBuffer(ByteBuffer)} first.
     * 
     * 
     * @param compressed
     *            buffer[pos() ... limit()) containing the input data
     * @param uncompressed
     *            output of the the uncompressed data. It uses buffer[pot()..]
     * @return uncompressed data size
     * 
     * @throws SnappyException
     *             when failed to uncompress the given input
     * @throws SnappyError
     *             when the input is not a direct buffer
     */
    public static int uncompress(ByteBuffer compressed, ByteBuffer uncompressed) throws SnappyException {

        if (!compressed.isDirect())
            throw new SnappyError(SnappyErrorCode.NOT_A_DIRECT_BUFFER, "input is not a direct buffer");
        if (!uncompressed.isDirect())
            throw new SnappyError(SnappyErrorCode.NOT_A_DIRECT_BUFFER, "destination is not a direct buffer");

        int cPos = compressed.position();
        int cLen = compressed.remaining();

        //         pos  limit
        // [ ......UUUUUU.........]
        int decompressedSize = SnappyNative
                .rawUncompress(compressed, cPos, cLen, uncompressed, uncompressed.position());
        uncompressed.limit(uncompressed.position() + decompressedSize);

        return decompressedSize;
    }

    /**
     * Uncompress the content in the input buffer. The uncompressed data is
     * written to the output buffer.
     * 
     * Note that if you pass the wrong data or the range [inputOffset,
     * inputOffset + inputLength) that cannot be uncompressed, your JVM might
     * crash due to the access violation exception issued in the native code
     * written in C++. To avoid this type of crash, use
     * {@link #isValidCompressedBuffer(byte[], int, int)} first.
     * 
     * @param input
     * @param inputOffset
     * @param inputLength
     * @param output
     * @param outputOffset
     * @return
     * @throws SnappyException
     */
    public static int uncompress(byte[] input, int inputOffset, int inputLength, byte[] output, int outputOffset)
            throws SnappyException {
        if (input == null || output == null)
            throw new NullPointerException("input or output is null");

        return SnappyNative.rawUncompress(input, inputOffset, inputLength, output, outputOffset);
    }

    /**
     * Get the uncompressed byte size of the given compressed input.
     * 
     * @param compressed
     *            input data [pos() ... limit())
     * @return uncompressed byte length of the given input
     * @throws SnappyException
     *             when failed to uncompress the given input
     * @throws SnappyError
     *             when the input is not a direct buffer
     */
    public static int uncompressedLength(ByteBuffer compressed) throws SnappyException {
        if (!compressed.isDirect())
            throw new SnappyError(SnappyErrorCode.NOT_A_DIRECT_BUFFER, "input is not a direct buffer");

        return SnappyNative.uncompressedLength(compressed, compressed.position(), compressed.remaining());
    }

    /**
     * Get the uncompressed byte size of the given compressed input
     * 
     * @param input
     * @param offset
     * @param length
     * @return umcompressed byte size of the the given input data
     * @throws SnappyException
     */
    public static int uncompressedLength(byte[] input, int offset, int length) throws SnappyException {
        if (input == null)
            throw new NullPointerException("input is null");
        return SnappyNative.uncompressedLength(input, offset, length);
    }

    /**
     * Get the maximum byte size needed for compressing a data of the given byte
     * size.
     * 
     * @param byteSize
     *            byte size of the data to compress
     * @return maximum byte size of the compressed data
     */
    public static int maxCompressedLength(int byteSize) {
        return SnappyNative.maxCompressedLength(byteSize);
    }

    /**
     * Returns true iff the contents of compressed buffer [pos() ... limit())
     * can be uncompressed successfully. Does not return the uncompressed data.
     * Takes time proportional to the input length, but is usually at least a
     * factor of four faster than actual decompression.
     */
    public static boolean isValidCompressedBuffer(ByteBuffer compressed) throws SnappyException {
        return SnappyNative.isValidCompressedBuffer(compressed, compressed.position(), compressed.remaining());
    }

    /**
     * Returns true iff the contents of compressed buffer [offset,
     * offset+length) can be uncompressed successfully. Does not return the
     * uncompressed data. Takes time proportional to the input length, but is
     * usually at least a factor of four faster than actual decompression.
     */
    public static boolean isValidCompressedBuffer(byte[] input, int offset, int length) throws SnappyException {
        if (input == null)
            throw new NullPointerException("input is null");
        return SnappyNative.isValidCompressedBuffer(input, offset, length);
    }

}
