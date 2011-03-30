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
 * Snappy API
 * 
 * @author leo
 * 
 */
public class Snappy
{

    public static String getNativeLibraryVersion() {
        return SnappyNative.nativeLibraryVersion();
    }

    /**
     * Compress the content of the given input, then output the compressed data.
     * 
     * @param uncompressed
     *            input is at buffer[pos() ... limit())
     * @param compressed
     *            output compressed data to buffer[pos()..]
     * @return byte size of the compressed data.
     */
    public static int compress(ByteBuffer uncompressed, ByteBuffer compressed) {

        if (!uncompressed.isDirect())
            throw new IllegalArgumentException("input is not a direct buffer");
        if (!compressed.isDirect())
            throw new IllegalArgumentException("destination is not a direct buffer");

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
     * @param compressed
     *            input is at buffer[pos() ... limit())
     * @param uncompressed
     *            output decompressed data to buffer[pot())
     * @return decompressed data size
     * 
     */
    public static int uncompress(ByteBuffer compressed, ByteBuffer decompressed) throws SnappyException {

        if (!compressed.isDirect())
            throw new IllegalArgumentException("input is not a direct buffer");
        if (!decompressed.isDirect())
            throw new IllegalArgumentException("destination is not a direct buffer");

        int cPos = compressed.position();
        int cLen = compressed.remaining();

        //         pos  limit
        // [ ......UUUUUU.........]
        int decompressedSize = SnappyNative
                .rawUncompress(compressed, cPos, cLen, decompressed, decompressed.position());
        decompressed.limit(decompressed.position() + decompressedSize);

        return decompressedSize;
    }

    /**
     * Get the uncompressed size of the compressed input
     * 
     * @param compressed
     *            data [pos() ... limit())
     * @return
     */
    public static int uncompressedLength(ByteBuffer compressed) throws SnappyException {
        if (!compressed.isDirect())
            throw new IllegalArgumentException("input is not a direct buffer");

        return SnappyNative.getUncompressedLength(compressed, compressed.position(), compressed.remaining());
    }

    /**
     * Get the maximum size of the compressed data of a given byte size
     * 
     * @param byteSize
     *            byte size of the data to compress
     * @return maxmum byte size of the compressed data
     */
    public static int maxCompressedLength(int byteSize) {
        return SnappyNative.maxCompressedLength(byteSize);
    }

}
