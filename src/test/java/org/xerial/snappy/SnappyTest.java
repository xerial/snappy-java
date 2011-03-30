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
// SnappyTest.java
// Since: 2011/03/30
//
// $URL$ 
// $Author$
//--------------------------------------
package org.xerial.snappy;

import static org.junit.Assert.*;

import java.nio.ByteBuffer;

import org.junit.Assert;
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
        catch (SnappyError e) {
            Assert.assertTrue(e.errorCode == SnappyErrorCode.NOT_A_DIRECT_BUFFER);
            return;
        }

        fail("shouldn't reach here");

    }

    @Test
    public void load() throws Exception {

        StringBuilder s = new StringBuilder();
        for (int i = 0; i < 20; ++i) {
            s.append("Hello world!");
        }
        String origStr = s.toString();
        byte[] orig = origStr.getBytes();
        int BUFFER_SIZE = orig.length;
        ByteBuffer src = ByteBuffer.allocateDirect(orig.length);
        src.put(orig);
        src.flip();
        _logger.info("input size: " + src.remaining());
        int maxCompressedLen = Snappy.maxCompressedLength(src.remaining());
        _logger.info("max compressed length:" + maxCompressedLen);

        ByteBuffer compressed = ByteBuffer.allocateDirect(maxCompressedLen);
        int compressedSize = Snappy.compress(src, compressed);
        _logger.info("compressed length: " + compressedSize);

        assertEquals(0, src.position());
        assertEquals(orig.length, src.remaining());
        assertEquals(orig.length, src.limit());

        assertEquals(0, compressed.position());
        assertEquals(compressedSize, compressed.limit());
        assertEquals(compressedSize, compressed.remaining());

        int uncompressedLen = Snappy.uncompressedLength(compressed);
        _logger.info("uncompressed length: " + uncompressedLen);
        ByteBuffer extract = ByteBuffer.allocateDirect(uncompressedLen);
        int uncompressedLen2 = Snappy.uncompress(compressed, extract);
        assertEquals(uncompressedLen, uncompressedLen2);
        assertEquals(uncompressedLen, extract.remaining());

        byte[] b = new byte[uncompressedLen];
        extract.get(b);
        String decompressed = new String(b);
        _logger.info(decompressed);

        assertEquals(origStr, decompressed);
    }

    @Test
    public void intermediateBuffer() throws Exception {

        String m = "ACCAGGGGGGGGGGGGGGGGGGGGATAGATATTTCCCGAGATATTTTATATAAAAAAA";
        byte[] orig = m.getBytes();
        final int offset = 100;
        ByteBuffer input = ByteBuffer.allocateDirect(orig.length + offset);
        input.position(offset);
        input.put(orig);
        input.flip();
        input.position(offset);

        // compress
        int maxCompressedLength = Snappy.maxCompressedLength(input.remaining());
        final int offset2 = 40;
        ByteBuffer compressed = ByteBuffer.allocateDirect(maxCompressedLength + offset2);
        compressed.position(offset2);
        Snappy.compress(input, compressed);

        // uncompress
        final int offset3 = 80;
        int uncompressedLength = Snappy.uncompressedLength(compressed);
        ByteBuffer uncompressed = ByteBuffer.allocateDirect(uncompressedLength + offset3);
        uncompressed.position(offset3);
        Snappy.uncompress(compressed, uncompressed);
        assertEquals(offset3, uncompressed.position());
        assertEquals(offset3 + uncompressedLength, uncompressed.limit());
        assertEquals(uncompressedLength, uncompressed.remaining());

        // extract string
        byte[] recovered = new byte[uncompressedLength];
        uncompressed.get(recovered);
        String m2 = new String(recovered);

        assertEquals(m, m2);
    }

}
