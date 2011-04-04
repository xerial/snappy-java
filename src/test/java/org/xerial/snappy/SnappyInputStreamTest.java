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
// XerialJ
//
// SnappyInputStreamTest.java
// Since: 2011/03/31 22:31:51
//
// $URL$
// $Author$
//--------------------------------------
package org.xerial.snappy;

import static org.junit.Assert.*;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import org.junit.Test;
import org.xerial.util.FileResource;
import org.xerial.util.log.Logger;

public class SnappyInputStreamTest
{
    private static Logger _logger = Logger.getLogger(SnappyInputStreamTest.class);

    public static byte[] readResourceFile(String fileName) throws IOException {
        BufferedInputStream input = new BufferedInputStream(FileResource.find(SnappyOutputStreamTest.class, fileName)
                .openStream());
        assertNotNull(input);
        return readFully(input);
    }

    public static byte[] readFully(InputStream input) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buf = new byte[4096];
        for (int readBytes = 0; (readBytes = input.read(buf)) != -1;) {
            out.write(buf, 0, readBytes);
        }
        out.flush();
        return out.toByteArray();
    }

    public static byte[] biteWiseReadFully(InputStream input) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buf = new byte[4096];
        for (int readData = 0; (readData = input.read()) != -1;) {
            out.write(readData);
        }
        out.flush();
        return out.toByteArray();
    }

    @Test
    public void read() throws Exception {
        ByteArrayOutputStream compressedBuf = new ByteArrayOutputStream();
        SnappyOutputStream snappyOut = new SnappyOutputStream(compressedBuf);
        byte[] orig = readResourceFile("alice29.txt");
        snappyOut.write(orig);
        snappyOut.close();
        byte[] compressed = compressedBuf.toByteArray();
        _logger.debug("compressed size: " + compressed.length);

        SnappyInputStream in = new SnappyInputStream(new ByteArrayInputStream(compressed));
        byte[] uncompressed = readFully(in);

        assertEquals(orig.length, uncompressed.length);
        assertArrayEquals(orig, uncompressed);

    }

    @Test
    public void readBlockCompressedData() throws Exception {
        byte[] orig = readResourceFile("alice29.txt");
        byte[] compressed = Snappy.compress(orig);

        SnappyInputStream in = new SnappyInputStream(new ByteArrayInputStream(compressed));
        byte[] uncompressed = readFully(in);

        assertEquals(orig.length, uncompressed.length);
        assertArrayEquals(orig, uncompressed);
    }

    @Test
    public void biteWiseRead() throws Exception {
        byte[] orig = readResourceFile("testdata/calgary/paper6");
        byte[] compressed = Snappy.compress(orig);

        SnappyInputStream in = new SnappyInputStream(new ByteArrayInputStream(compressed));
        byte[] uncompressed = biteWiseReadFully(in);

        assertEquals(orig.length, uncompressed.length);
        assertArrayEquals(orig, uncompressed);
    }

}
