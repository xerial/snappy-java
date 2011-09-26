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
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.Properties;

/**
 * Snappy API for data compression/decompression
 * 
 * @author leo
 * 
 */
public class Snappy
{
    static {
        try {
            impl = SnappyLoader.load();
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * An instance of SnappyNativeAPI
     */
    private static Object impl;

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
        ((SnappyNativeAPI) impl).arrayCopy(src, offset, byteLength, dest, dest_offset);
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
        int compressedSize = ((SnappyNativeAPI) impl).rawCompress(uncompressed, uPos, uLen, compressed,
                compressed.position());

        //         pos  limit
        // [ ......BBBBBBB.........]
        compressed.limit(compressed.position() + compressedSize);

        return compressedSize;
    }

    /**
     * Compress the input char array
     * 
     * @param input
     * @return the compressed data
     */
    public static byte[] compress(char[] input) {
        return rawCompress(input, input.length * 2); // char uses 2 bytes
    }

    /**
     * Compress the input double array
     * 
     * @param input
     * @return the compressed data
     */
    public static byte[] compress(double[] input) {
        return rawCompress(input, input.length * 8); // double uses 8 bytes
    }

    /**
     * Compress the input float array
     * 
     * @param input
     * @return the compressed data
     */
    public static byte[] compress(float[] input) {
        return rawCompress(input, input.length * 4); // float uses 4 bytes
    }

    /**
     * Compress the input int array
     * 
     * @param input
     * @return the compressed data
     */
    public static byte[] compress(int[] input) {
        return rawCompress(input, input.length * 4); // int uses 4 bytes
    }

    /**
     * Compress the input long array
     * 
     * @param input
     * @return the compressed data
     */
    public static byte[] compress(long[] input) {
        return rawCompress(input, input.length * 8); // long uses 8 bytes
    }

    /**
     * Compress the input short array
     * 
     * @param input
     * @return the compressed data
     */
    public static byte[] compress(short[] input) {
        return rawCompress(input, input.length * 2); // short uses 2 bytes
    }

    /**
     * Compress the input String
     * 
     * @param s
     * @return the compressed data
     * @throws IOException
     */
    public static byte[] compress(String s) throws IOException {
        try {
            return compress(s, "UTF-8");
        }
        catch (UnsupportedEncodingException e) {
            throw new IllegalStateException("UTF-8 encoder is not found");
        }
    }

    /**
     * Compress the input string using the given encoding
     * 
     * @param s
     * @param encoding
     * @return the compressed data
     * @throws UnsupportedEncodingException
     * @throws IOException
     */
    public static byte[] compress(String s, String encoding) throws UnsupportedEncodingException, IOException {
        byte[] data = s.getBytes(encoding);
        return compress(data);
    }

    /**
     * Compress the input string using the given encoding
     * 
     * @param s
     * @param encoding
     * @return the compressed data
     * @throws UnsupportedEncodingException
     * @throws IOException
     */
    public static byte[] compress(String s, Charset encoding) throws IOException {
        byte[] data = s.getBytes(encoding);
        return compress(data);
    }

    /**
     * Get the native library version of the snappy
     * 
     * @return native library version
     */
    public static String getNativeLibraryVersion() {

        URL versionFile = SnappyLoader.class.getResource("/org/xerial/snappy/VERSION");

        String version = "unknown";
        try {
            if (versionFile != null) {
                Properties versionData = new Properties();
                versionData.load(versionFile.openStream());
                version = versionData.getProperty("version", version);
                if (version.equals("unknown"))
                    version = versionData.getProperty("VERSION", version);
                version = version.trim().replaceAll("[^0-9\\.]", "");
            }
        }
        catch (IOException e) {
            e.printStackTrace();
        }
        return version;
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
        return ((SnappyNativeAPI) impl).isValidCompressedBuffer(input, offset, length);
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
        return ((SnappyNativeAPI) impl).isValidCompressedBuffer(compressed, compressed.position(),
                compressed.remaining());
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
        return ((SnappyNativeAPI) impl).maxCompressedLength(byteSize);
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
        int compressedByteSize = ((SnappyNativeAPI) impl).rawCompress(data, 0, byteSize, buf, 0);
        byte[] result = new byte[compressedByteSize];
        System.arraycopy(buf, 0, result, 0, compressedByteSize);
        return result;
    }

    /**
     * Compress the input buffer [offset,... ,offset+length) contents, then
     * write the compressed data to the output buffer[offset, ...)
     * 
     * @param input
     *            input array. This MUST be a primitive array type
     * @param inputOffset
     *            byte offset at the output array
     * @param inputLength
     *            byte length of the input data
     * @param output
     *            output array. This MUST be a primitive array type
     * @param outputOffset
     *            byte offset at the output array
     * @return byte size of the compressed data
     * @throws IOException
     */
    public static int rawCompress(Object input, int inputOffset, int inputLength, byte[] output, int outputOffset)
            throws IOException {
        if (input == null || output == null)
            throw new NullPointerException("input or output is null");

        int compressedSize = ((SnappyNativeAPI) impl)
                .rawCompress(input, inputOffset, inputLength, output, outputOffset);
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
     *            byte offset in the input byte array
     * @param inputLength
     *            byte length of the input data
     * @param output
     *            output buffer, MUST be a primitive type array
     * @param outputOffset
     *            byte offset in the output buffer
     * @return the byte size of the uncompressed data
     * @throws IOException
     *             when failed to uncompress the input data
     */
    public static int rawUncompress(byte[] input, int inputOffset, int inputLength, Object output, int outputOffset)
            throws IOException {
        if (input == null || output == null)
            throw new NullPointerException("input or output is null");
        return ((SnappyNativeAPI) impl).rawUncompress(input, inputOffset, inputLength, output, outputOffset);
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
     *            output of the the uncompressed data. It uses buffer[pos()..]
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
        int decompressedSize = ((SnappyNativeAPI) impl).rawUncompress(compressed, cPos, cLen, uncompressed,
                uncompressed.position());
        uncompressed.limit(uncompressed.position() + decompressedSize);

        return decompressedSize;
    }

    /**
     * Uncompress the input data as char array
     * 
     * @param input
     * @return the uncompressed data
     * @throws IOException
     */
    public static char[] uncompressCharArray(byte[] input) throws IOException {
        return uncompressCharArray(input, 0, input.length);
    }

    /**
     * Uncompress the input[offset, .., offset+length) as a char array
     * 
     * @param input
     * @param offset
     * @param length
     * @return the uncompressed data
     * @throws IOException
     */
    public static char[] uncompressCharArray(byte[] input, int offset, int length) throws IOException {
        int uncompressedLength = Snappy.uncompressedLength(input, offset, length);
        char[] result = new char[uncompressedLength / 2];
        int byteSize = ((SnappyNativeAPI) impl).rawUncompress(input, offset, length, result, 0);
        return result;
    }

    /**
     * Uncompress the input as a double array
     * 
     * @param input
     * @return the uncompressed data
     * @throws IOException
     */
    public static double[] uncompressDoubleArray(byte[] input) throws IOException {
        int uncompressedLength = Snappy.uncompressedLength(input, 0, input.length);
        double[] result = new double[uncompressedLength / 8];
        int byteSize = ((SnappyNativeAPI) impl).rawUncompress(input, 0, input.length, result, 0);
        return result;
    }

    /**
     * Get the uncompressed byte size of the given compressed input. This
     * operation takes O(1) time.
     * 
     * @param input
     * @return uncompressed byte size of the the given input data
     * @throws IOException
     *             when failed to uncompress the given input. The error code is
     *             {@link SnappyErrorCode#PARSING_ERROR}
     */
    public static int uncompressedLength(byte[] input) throws IOException {
        return ((SnappyNativeAPI) impl).uncompressedLength(input, 0, input.length);
    }

    /**
     * Get the uncompressed byte size of the given compressed input. This
     * operation takes O(1) time.
     * 
     * @param input
     * @param offset
     * @param length
     * @return uncompressed byte size of the the given input data
     * @throws IOException
     *             when failed to uncompress the given input. The error code is
     *             {@link SnappyErrorCode#PARSING_ERROR}
     */
    public static int uncompressedLength(byte[] input, int offset, int length) throws IOException {
        if (input == null)
            throw new NullPointerException("input is null");

        return ((SnappyNativeAPI) impl).uncompressedLength(input, offset, length);
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

        return ((SnappyNativeAPI) impl).uncompressedLength(compressed, compressed.position(), compressed.remaining());
    }

    /**
     * Uncompress the input as a float array
     * 
     * @param input
     * @return the uncompressed data
     * @throws IOException
     */
    public static float[] uncompressFloatArray(byte[] input) throws IOException {
        return uncompressFloatArray(input, 0, input.length);
    }

    /**
     * Uncompress the input[offset, offset+length) as a float array
     * 
     * @param input
     * @param offset
     * @param length
     * @return the uncompressed data
     * @throws IOException
     */
    public static float[] uncompressFloatArray(byte[] input, int offset, int length) throws IOException {
        int uncompressedLength = Snappy.uncompressedLength(input, offset, length);
        float[] result = new float[uncompressedLength / 4];
        int byteSize = ((SnappyNativeAPI) impl).rawUncompress(input, offset, length, result, 0);
        return result;
    }

    /**
     * Uncompress the input data as an int array
     * 
     * @param input
     * @return the uncompressed data
     * @throws IOException
     */
    public static int[] uncompressIntArray(byte[] input) throws IOException {
        return uncompressIntArray(input, 0, input.length);
    }

    /**
     * Uncompress the input[offset, offset+length) as an int array
     * 
     * @param input
     * @param offset
     * @param length
     * @return the uncompressed data
     * @throws IOException
     */
    public static int[] uncompressIntArray(byte[] input, int offset, int length) throws IOException {
        int uncompressedLength = Snappy.uncompressedLength(input, offset, length);
        int[] result = new int[uncompressedLength / 4];
        int byteSize = ((SnappyNativeAPI) impl).rawUncompress(input, offset, length, result, 0);
        return result;
    }

    /**
     * Uncompress the input data as a long array
     * 
     * @param input
     * @return the uncompressed data
     * @throws IOException
     */
    public static long[] uncompressLongArray(byte[] input) throws IOException {
        return uncompressLongArray(input, 0, input.length);
    }

    /**
     * Uncompress the input[offset, offset+length) as a long array
     * 
     * @param input
     * @param offset
     * @param length
     * @return the uncompressed data
     * @throws IOException
     */
    public static long[] uncompressLongArray(byte[] input, int offset, int length) throws IOException {
        int uncompressedLength = Snappy.uncompressedLength(input, offset, length);
        long[] result = new long[uncompressedLength / 8];
        int byteSize = ((SnappyNativeAPI) impl).rawUncompress(input, offset, length, result, 0);
        return result;
    }

    /**
     * Uncompress the input as a short array
     * 
     * @param input
     * @return the uncompressed data
     * @throws IOException
     */
    public static short[] uncompressShortArray(byte[] input) throws IOException {
        return uncompressShortArray(input, 0, input.length);
    }

    /**
     * Uncompress the input[offset, offset+length) as a short array
     * 
     * @param input
     * @param offset
     * @param length
     * @return the uncompressed data
     * @throws IOException
     */
    public static short[] uncompressShortArray(byte[] input, int offset, int length) throws IOException {
        int uncompressedLength = Snappy.uncompressedLength(input, offset, length);
        short[] result = new short[uncompressedLength / 2];
        int byteSize = ((SnappyNativeAPI) impl).rawUncompress(input, offset, length, result, 0);
        return result;
    }

    /**
     * Uncompress the input as a String
     * 
     * @param input
     * @return the uncompressed dasta
     * @throws IOException
     */
    public static String uncompressString(byte[] input) throws IOException {
        try {
            return uncompressString(input, "UTF-8");
        }
        catch (UnsupportedEncodingException e) {
            throw new IllegalStateException("UTF-8 decoder is not found");
        }
    }

    /**
     * Uncompress the input[offset, offset+length) as a String
     * 
     * @param input
     * @param offset
     * @param length
     * @return the uncompressed data
     * @throws IOException
     */
    public static String uncompressString(byte[] input, int offset, int length) throws IOException {
        try {
            return uncompressString(input, offset, length, "UTF-8");
        }
        catch (UnsupportedEncodingException e) {
            throw new IllegalStateException("UTF-8 decoder is not found");
        }
    }

    /**
     * Uncompress the input[offset, offset+length) as a String of the given
     * encoding
     * 
     * @param input
     * @param offset
     * @param length
     * @param encoding
     * @return the uncompressed data
     * @throws IOException
     */
    public static String uncompressString(byte[] input, int offset, int length, String encoding) throws IOException,
            UnsupportedEncodingException {
        byte[] uncompressed = new byte[uncompressedLength(input, offset, length)];
        int compressedSize = uncompress(input, offset, length, uncompressed, 0);
        return new String(uncompressed, encoding);
    }

    /**
     * Uncompress the input[offset, offset+length) as a String of the given
     * encoding
     * 
     * @param input
     * @param offset
     * @param length
     * @param encoding
     * @return the uncompressed data
     * @throws IOException
     */
    public static String uncompressString(byte[] input, int offset, int length, Charset encoding) throws IOException,
            UnsupportedEncodingException {
        byte[] uncompressed = new byte[uncompressedLength(input, offset, length)];
        int compressedSize = uncompress(input, offset, length, uncompressed, 0);
        return new String(uncompressed, encoding);
    }

    /**
     * Uncompress the input as a String of the given encoding
     * 
     * @param input
     * @param encoding
     * @return the uncompressed data
     * @throws IOException
     * @throws UnsupportedEncodingException
     */
    public static String uncompressString(byte[] input, String encoding) throws IOException,
            UnsupportedEncodingException {
        byte[] uncompressed = uncompress(input);
        return new String(uncompressed, encoding);
    }

    /**
     * Uncompress the input as a String of the given encoding
     * 
     * @param input
     * @param encoding
     * @return the uncompressed data
     * @throws IOException
     */
    public static String uncompressString(byte[] input, Charset encoding) throws IOException,
            UnsupportedEncodingException {
        byte[] uncompressed = uncompress(input);
        return new String(uncompressed, encoding);
    }
}
