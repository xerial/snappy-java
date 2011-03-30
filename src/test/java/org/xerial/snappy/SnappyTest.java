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

import java.nio.ByteBuffer;

import org.junit.Test;
import org.xerial.util.log.Logger;

public class SnappyTest
{
    private static Logger _logger = Logger.getLogger(SnappyTest.class);

    @Test
    public void getVersion() throws Exception {
        String version = Snappy.nativeLibraryVersion();
        _logger.info("version: " + version);
    }

    @Test
    public void load() throws Exception {

        ByteBuffer src = ByteBuffer.allocate(1024);
        src.put("hello world".getBytes());
        ByteBuffer dest = ByteBuffer.allocate(1024);
        Snappy.compress(src, dest);

    }
}
