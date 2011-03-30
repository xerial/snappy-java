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

    public native static int rawUncompress(ByteBuffer compressed, int inputOffset, int inputLength,
            ByteBuffer uncompressed, int outputOffset) throws SnappyException;

    // Returns the maximal size of the compressed representation of
    // input data that is "source_bytes" bytes in length;
    public native static int maxCompressedLength(int source_bytes);

    // This operation takes O(1) time.
    public native static int getUncompressedLength(ByteBuffer compressed, int offset, int len) throws SnappyException;

    public static void throw_error(int errorCode) throws SnappyException {
        throw new SnappyException(errorCode);
    }

}
