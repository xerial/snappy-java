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

public class Snappy
{

    public static String getNativeLibraryVersion() {
        return SnappyNative.nativeLibraryVersion();
    }

    /**
     * @param uncompressed
     *            input is at buffer[pos() ... limit())
     * @param compressed
     *            output compressed data to buffer[pos()]
     * @return byte size of the compressed data
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

        //        //            pos      limit
        //        // [ ....XXXXXX.........]
        //        uncompressed.limit(uncompressed.capacity());
        //        uncompressed.position(uPos + uLen);

        //         pos  limit
        // [ ......BBBBBBB.........]
        compressed.limit(compressed.position() + compressedSize);

        return compressedSize;
    }

    /**
     * @param compressed
     *            input is at buffer[pos() ... limit())
     * @param decompressed
     *            output decompressed data to buffer[pot())
     * @return
     */
    public static boolean decompress(ByteBuffer compressed, ByteBuffer decompressed) {

        if (!compressed.isDirect())
            throw new IllegalArgumentException("input is not a direct buffer");
        if (!decompressed.isDirect())
            throw new IllegalArgumentException("destination is not a direct buffer");

        int cPos = compressed.position();
        int cLen = compressed.remaining();

        boolean ret = SnappyNative.rawDecompress(compressed, cPos, cLen, decompressed, decompressed.position());

        //        compressed.limit(compressed.capacity());
        //        compressed.position(cPos + cLen);

        return ret;
    }

    public static int getUncompressedLength(ByteBuffer compressed) {
        if (!compressed.isDirect())
            throw new IllegalArgumentException("input is not a direct buffer");

        return SnappyNative.getUncompressedLength(compressed, compressed.position(), compressed.remaining());
    }

    public static int getMaxCompressedLength(int byteSize) {
        return SnappyNative.maxCompressedLength(byteSize);
    }

}
