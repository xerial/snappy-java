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

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;

/**
 * Snappy API for data compression/decompression
 * 
 * @author leo
 * 
 */
public class Snappy
{
    private static SnappyNativeAPI impl;

    static {
        impl = SnappyLoader.load();
    }

    /**
     * Copy bytes from source to destination
     * 
     * @param src
     *            pointer to the source array
     * @param offset
     *            byte offset in the source array
     * @param byteLength
     *            the number of bytes to copy
     * @param dest
     *            pointer to the destination array
     * @param dest_offset
     *            byte offset in the destination array
     * @throws IOException
     */
    public static void arrayCopy(Object src, int offset, int byteLength, Object dest, int dest_offset)
            throws IOException {
        impl.arrayCopy(src, offset, byteLength, dest, dest_offset);
    }

    /**
     * High-level API for compressing the input byte array. This method performs
     * array copy to generate the result. If you want to reduce the memory copy
     * cost, use {@link #compress(byte[], int, int, byte[], int)} or
     * {@link #compress(ByteBuffer, ByteBuffer)}.
     * 
     * @param input
     *            the input data
     * @return the compressed byte array
     * @throws IOException
     */
    public static byte[] compress(byte[] input) throws IOException {
        return rawCompress(input, input.length);
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
     * @throws IOException
     *             when failed to access the input/output buffer
     */
    public static int compress(byte[] input, int inputOffset, int inputLength, byte[] output, int outputOffset)
            throws IOException {
        return rawCompress(input, inputOffset, inputLength, output, outputOffset);
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
    public static int compress(ByteBuffer uncompressed, ByteBuffer compressed) throws IOException {

        if (!uncompressed.isDirect())
            throw new SnappyError(SnappyErrorCode.NOT_A_DIRECT_BUFFER, "input is not a direct buffer");
        if (!compressed.isDirect())
            throw new SnappyError(SnappyErrorCode.NOT_A_DIRECT_BUFFER, "destination is not a direct buffer");

        // input: uncompressed[pos(), limit())
        // output: compressed
        int uPos = uncompressed.position();
        int uLen = uncompressed.remaining();
        int compressedSize = impl.rawCompress(uncompressed, uPos, uLen, compressed, compressed.position());

        //         pos  limit
        // [ ......BBBBBBB.........]
        compressed.limit(compressed.position() + compressedSize);

        return compressedSize;
    }

    public static byte[] compress(char[] input) {
        return rawCompress(input, input.length * 2); // char uses 2 bytes
    }

    public static byte[] compress(double[] input) {
        return rawCompress(input, input.length * 8); // double uses 8 bytes
    }

    public static byte[] compress(float[] input) {
        return rawCompress(input, input.length * 4); // float uses 4 bytes
    }

    public static byte[] compress(int[] input) {
        return rawCompress(input, input.length * 4); // int uses 4 bytes
    }

    public static byte[] compress(long[] input) {
        return rawCompress(input, input.length * 8); // long uses 8 bytes
    }

    public static byte[] compress(short[] input) {
        return rawCompress(input, input.length * 2); // short uses 2 bytes
    }

    public static byte[] compress(String s) throws IOException {
        try {
            return compress(s, "UTF-8");
        }
        catch (UnsupportedEncodingException e) {
            throw new IllegalStateException("UTF-8 encoder is not found");
        }
    }

    public static byte[] compress(String s, String encoding) throws UnsupportedEncodingException, IOException {
        byte[] data = s.getBytes(encoding);
        return compress(data);
    }

    /**
     * Get the native library version of the snappy
     * 
     * @return native library version
     */
    public static String getNativeLibraryVersion() {
        return impl.nativeLibraryVersion();
    }

    /**
     * Returns true iff the contents of compressed buffer [offset,
     * offset+length) can be uncompressed successfully. Does not return the
     * uncompressed data. Takes time proportional to the input length, but is
     * usually at least a factor of four faster than actual decompression.
     */
    public static boolean isValidCompressedBuffer(byte[] input, int offset, int length) throws IOException {
        if (input == null)
            throw new NullPointerException("input is null");
        return impl.isValidCompressedBuffer(input, offset, length);
    }

    /**
     * Returns true iff the contents of compressed buffer [offset,
     * offset+length) can be uncompressed successfully. Does not return the
     * uncompressed data. Takes time proportional to the input length, but is
     * usually at least a factor of four faster than actual decompression.
     */
    public static boolean isValidCompressedBuffer(byte[] input) throws IOException {
        return isValidCompressedBuffer(input, 0, input.length);
    }

    /**
     * Returns true iff the contents of compressed buffer [pos() ... limit())
     * can be uncompressed successfully. Does not return the uncompressed data.
     * Takes time proportional to the input length, but is usually at least a
     * factor of four faster than actual decompression.
     */
    public static boolean isValidCompressedBuffer(ByteBuffer compressed) throws IOException {
        return impl.isValidCompressedBuffer(compressed, compressed.position(), compressed.remaining());
    }

    /**
     * Get the maximum byte size needed for compressing data of the given byte
     * size.
     * 
     * @param byteSize
     *            byte size of the data to compress
     * @return maximum byte size of the compressed data
     */
    public static int maxCompressedLength(int byteSize) {
        return impl.maxCompressedLength(byteSize);
    }

    /**
     * Compress the input data and produce a byte array of the uncompressed data
     * 
     * @param data
     *            input array. The input MUST be an array type
     * @param byteSize
     *            the input byte size
     * @return compressed data
     */
    public static byte[] rawCompress(Object data, int byteSize) {
        byte[] buf = new byte[Snappy.maxCompressedLength(byteSize)];
        int compressedByteSize = impl.rawCompress(data, 0, byteSize, buf, 0);
        byte[] result = new byte[compressedByteSize];
        System.arraycopy(buf, 0, result, 0, compressedByteSize);
        return result;
    }

    /**
     * Compress the input buffer [offset,... ,offset+length) contents, then
     * write the compressed data to the output buffer[offset, ...)
     * 
     * @param input
     *            input array. This MUST be primitive array type
     * @param inputOffset
     *            byte offset at the output array
     * @param inputLength
     *            byte length of the input data
     * @param output
     *            output array. This MUST be primitive array type
     * @param outputOffset
     *            byte offset at the output array
     * @return byte size of the compressed data
     * @throws IOException
     */
    public static int rawCompress(Object input, int inputOffset, int inputLength, byte[] output, int outputOffset)
            throws IOException {
        if (input == null || output == null)
            throw new NullPointerException("input or output is null");

        int compressedSize = impl.rawCompress(input, inputOffset, inputLength, output, outputOffset);
        return compressedSize;
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
     *            input byte array
     * @param inputOffset
     *            byte offset
     * @param inputLength
     *            byte length of the input data
     * @param output
     *            output buffer, MUST be a primitive type array
     * @param outputOffset
     *            byte offset
     * @return the byte size of the uncompressed data
     * @throws IOException
     */
    public static int rawUncompress(byte[] input, int inputOffset, int inputLength, Object output, int outputOffset)
            throws IOException {
        if (input == null || output == null)
            throw new NullPointerException("input or output is null");
        return impl.rawUncompress(input, inputOffset, inputLength, output, outputOffset);
    }

    /**
     * High-level API for uncompressing the input byte array.
     * 
     * @param input
     * @return the uncompressed byte array
     * @throws IOException
     */
    public static byte[] uncompress(byte[] input) throws IOException {
        byte[] result = new byte[Snappy.uncompressedLength(input)];
        int byteSize = Snappy.uncompress(input, 0, input.length, result, 0);
        return result;
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
     * @return the byte size of the uncompressed data
     * @throws IOException
     */
    public static int uncompress(byte[] input, int inputOffset, int inputLength, byte[] output, int outputOffset)
            throws IOException {
        return rawUncompress(input, inputOffset, inputLength, output, outputOffset);
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
     * @throws IOException
     *             when failed to uncompress the given input
     * @throws SnappyError
     *             when the input is not a direct buffer
     */
    public static int uncompress(ByteBuffer compressed, ByteBuffer uncompressed) throws IOException {

        if (!compressed.isDirect())
            throw new SnappyError(SnappyErrorCode.NOT_A_DIRECT_BUFFER, "input is not a direct buffer");
        if (!uncompressed.isDirect())
            throw new SnappyError(SnappyErrorCode.NOT_A_DIRECT_BUFFER, "destination is not a direct buffer");

        int cPos = compressed.position();
        int cLen = compressed.remaining();

        //         pos  limit
        // [ ......UUUUUU.........]
        int decompressedSize = impl.rawUncompress(compressed, cPos, cLen, uncompressed, uncompressed.position());
        uncompressed.limit(uncompressed.position() + decompressedSize);

        return decompressedSize;
    }

    public static char[] uncompressCharArray(byte[] input) throws IOException {
        return uncompressCharArray(input, 0, input.length);
    }

    public static char[] uncompressCharArray(byte[] input, int offset, int length) throws IOException {
        int uncompressedLength = Snappy.uncompressedLength(input, offset, length);
        char[] result = new char[uncompressedLength / 2];
        int byteSize = impl.rawUncompress(input, offset, length, result, 0);
        return result;
    }

    public static double[] uncompressDoubleArray(byte[] input) throws IOException {
        int uncompressedLength = Snappy.uncompressedLength(input, 0, input.length);
        double[] result = new double[uncompressedLength / 8];
        int byteSize = impl.rawUncompress(input, 0, input.length, result, 0);
        return result;
    }

    /**
     * Get the uncompressed byte size of the given compressed input. This
     * operation takes O(1) time.
     * 
     * @param input
     * @return umcompressed byte size of the the given input data
     * @throws IOException
     *             when failed to uncompress the given input. The error code is
     *             {@link SnappyErrorCode#PARSING_ERROR}
     */
    public static int uncompressedLength(byte[] input) throws IOException {
        return impl.uncompressedLength(input, 0, input.length);
    }

    /**
     * Get the uncompressed byte size of the given compressed input. This
     * operation takes O(1) time.
     * 
     * @param input
     * @param offset
     * @param length
     * @return umcompressed byte size of the the given input data
     * @throws IOException
     *             when failed to uncompress the given input. The error code is
     *             {@link SnappyErrorCode#PARSING_ERROR}
     */
    public static int uncompressedLength(byte[] input, int offset, int length) throws IOException {
        if (input == null)
            throw new NullPointerException("input is null");

        return impl.uncompressedLength(input, offset, length);
    }

    /**
     * Get the uncompressed byte size of the given compressed input. This
     * operation taks O(1) time.
     * 
     * @param compressed
     *            input data [pos() ... limit())
     * @return uncompressed byte length of the given input
     * @throws IOException
     *             when failed to uncompress the given input. The error code is
     *             {@link SnappyErrorCode#PARSING_ERROR}
     * @throws SnappyError
     *             when the input is not a direct buffer
     */
    public static int uncompressedLength(ByteBuffer compressed) throws IOException {
        if (!compressed.isDirect())
            throw new SnappyError(SnappyErrorCode.NOT_A_DIRECT_BUFFER, "input is not a direct buffer");

        return impl.uncompressedLength(compressed, compressed.position(), compressed.remaining());
    }

    public static float[] uncompressFloatArray(byte[] input) throws IOException {
        return uncompressFloatArray(input, 0, input.length);
    }

    public static float[] uncompressFloatArray(byte[] input, int offset, int length) throws IOException {
        int uncompressedLength = Snappy.uncompressedLength(input, offset, length);
        float[] result = new float[uncompressedLength / 4];
        int byteSize = impl.rawUncompress(input, offset, length, result, 0);
        return result;
    }

    public static int[] uncompressIntArray(byte[] input) throws IOException {
        return uncompressIntArray(input, 0, input.length);
    }

    public static int[] uncompressIntArray(byte[] input, int offset, int length) throws IOException {
        int uncompressedLength = Snappy.uncompressedLength(input, offset, length);
        int[] result = new int[uncompressedLength / 4];
        int byteSize = impl.rawUncompress(input, offset, length, result, 0);
        return result;
    }

    public static long[] uncompressLongArray(byte[] input) throws IOException {
        return uncompressLongArray(input, 0, input.length);
    }

    public static long[] uncompressLongArray(byte[] input, int offset, int length) throws IOException {
        int uncompressedLength = Snappy.uncompressedLength(input, offset, length);
        long[] result = new long[uncompressedLength / 8];
        int byteSize = impl.rawUncompress(input, offset, length, result, 0);
        return result;
    }

    public static short[] uncompressShortArray(byte[] input) throws IOException {
        return uncompressShortArray(input, 0, input.length);
    }

    public static short[] uncompressShortArray(byte[] input, int offset, int length) throws IOException {
        int uncompressedLength = Snappy.uncompressedLength(input, offset, length);
        short[] result = new short[uncompressedLength / 2];
        int byteSize = impl.rawUncompress(input, offset, length, result, 0);
        return result;
    }

    public static String uncompressString(byte[] input) throws IOException {
        try {
            return uncompressString(input, "UTF-8");
        }
        catch (UnsupportedEncodingException e) {
            throw new IllegalStateException("UTF-8 decoder is not found");
        }
    }

    public static String uncompressString(byte[] input, int offset, int length) throws IOException {
        try {
            return uncompressString(input, offset, length, "UTF-8");
        }
        catch (UnsupportedEncodingException e) {
            throw new IllegalStateException("UTF-8 decoder is not found");
        }
    }

    public static String uncompressString(byte[] input, int offset, int length, String encoding) throws IOException,
            UnsupportedEncodingException {
        byte[] uncompressed = new byte[uncompressedLength(input, offset, length)];
        int compressedSize = uncompress(input, offset, length, uncompressed, 0);
        return new String(uncompressed, encoding);
    }

    public static String uncompressString(byte[] input, String encoding) throws IOException,
            UnsupportedEncodingException {
        byte[] uncompressed = uncompress(input);
        return new String(uncompressed, encoding);
    }

}
