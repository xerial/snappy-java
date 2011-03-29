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
    static {
        LoadSnappy.initialize();
    }

    // ------------------------------------------------------------------------
    // Generic compression/decompression routines.
    // ------------------------------------------------------------------------

    public native static long compress(ByteBuffer uncompressed, ByteBuffer compressed);

    public native static boolean uncompress(ByteBuffer compressed, ByteBuffer uncompressed);

    // Returns the maximal size of the compressed representation of
    // input data that is "source_bytes" bytes in length;
    public native static long maxCompressedLength(long source_bytes);

    // This operation takes O(1) time.
    public native static long getUncompressedLength(ByteBuffer compressed);

}
