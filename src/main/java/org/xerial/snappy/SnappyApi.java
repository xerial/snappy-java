package org.xerial.snappy;

import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * Snappy compressor/decompressor interface. The implementation can be JNI binding or pure-java Snappy implementation.
 */
public interface SnappyApi
{
    // ------------------------------------------------------------------------
    // Generic compression/decompression routines.
    // ------------------------------------------------------------------------
    long rawCompress(long inputAddr, long inputSize, long destAddr)
            throws IOException;

    long rawUncompress(long inputAddr, long inputSize, long destAddr)
            throws IOException;

    int rawCompress(ByteBuffer input, int inputOffset, int inputLength, ByteBuffer compressed,
            int outputOffset)
            throws IOException;

    int rawCompress(Object input, int inputOffset, int inputByteLength, Object output, int outputOffset)
            throws IOException;

    int rawUncompress(ByteBuffer compressed, int inputOffset, int inputLength, ByteBuffer uncompressed,
            int outputOffset)
            throws IOException;

    int rawUncompress(Object input, int inputOffset, int inputLength, Object output, int outputOffset)
            throws IOException;

    // Returns the maximal size of the compressed representation of
    // input data that is "source_bytes" bytes in length;
    int maxCompressedLength(int source_bytes);

    // This operation takes O(1) time.
    int uncompressedLength(ByteBuffer compressed, int offset, int len)
            throws IOException;

    int uncompressedLength(Object input, int offset, int len)
            throws IOException;

    long uncompressedLength(long inputAddr, long len)
            throws IOException;

    boolean isValidCompressedBuffer(ByteBuffer compressed, int offset, int len)
            throws IOException;

    boolean isValidCompressedBuffer(Object input, int offset, int len)
            throws IOException;

    boolean isValidCompressedBuffer(long inputAddr, long offset, long len)
            throws IOException;

    void arrayCopy(Object src, int offset, int byteLength, Object dest, int dOffset)
            throws IOException;

}
