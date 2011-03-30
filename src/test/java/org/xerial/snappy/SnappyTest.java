//--------------------------------------
// snappy-java Project
//
// SnappyTest.java
// Since: 2011/03/30
//
// $URL$ 
// $Author$
//--------------------------------------
package org.xerial.snappy;

import static org.junit.Assert.*;

import java.nio.ByteBuffer;

import org.junit.Test;
import org.xerial.util.log.Logger;

public class SnappyTest
{
    private static Logger _logger = Logger.getLogger(SnappyTest.class);

    @Test
    public void getVersion() throws Exception {
        String version = Snappy.getNativeLibraryVersion();
        _logger.info("version: " + version);
    }

    @Test
    public void directBufferCheck() throws Exception {

        try {
            ByteBuffer src = ByteBuffer.allocate(1024);
            src.put("hello world".getBytes());
            src.flip();
            ByteBuffer dest = ByteBuffer.allocate(1024);
            int maxCompressedLen = Snappy.compress(src, dest);
        }
        catch (IllegalArgumentException e) {
            // detected non-direct buffer. OK
            return;
        }

        fail("shouldn't reach here");

    }

    @Test
    public void load() throws Exception {

        ByteBuffer src = ByteBuffer.allocateDirect(1024);
        src.put("hello world".getBytes());

        src.flip();
        int maxCompressedLen = Snappy.getMaxCompressedLength(src.remaining());
        _logger.info("max compressed length:" + maxCompressedLen);

        int uncompressedLen = Snappy.getUncompressedLength(src);
        _logger.info("uncompressed length: " + uncompressedLen);

        ByteBuffer dest = ByteBuffer.allocateDirect(1024);
        Snappy.compress(src, dest);

    }
}
