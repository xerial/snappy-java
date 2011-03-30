//--------------------------------------
// snappy-java Project
//
// SnappyNative.java
// Since: 2011/03/30
//
// $URL$ 
// $Author$
//--------------------------------------
package org.xerial.snappy;

import java.nio.ByteBuffer;

public class SnappyNative
{
    static {
        LoadSnappy.initialize();
    }

    public native static String nativeLibraryVersion();

    // ------------------------------------------------------------------------
    // Generic compression/decompression routines.
    // ------------------------------------------------------------------------
    public native static int rawCompress(ByteBuffer input, int inputOffset, int inputLength, ByteBuffer compressed,
            int outputOffset);

    public native static boolean rawDecompress(ByteBuffer compressed, int inputOffset, int inputLength,
            ByteBuffer uncompressed, int outputOffset);

    // Returns the maximal size of the compressed representation of
    // input data that is "source_bytes" bytes in length;
    public native static int maxCompressedLength(int source_bytes);

    // This operation takes O(1) time.
    public native static int getUncompressedLength(ByteBuffer compressed, int offset, int len);

}
