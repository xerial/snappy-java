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

    public static byte[] readResourceFile(String fileName)
            throws IOException
    {
        BufferedInputStream input = new BufferedInputStream(FileResource.find(SnappyOutputStreamTest.class, fileName)
                .openStream());
        assertNotNull(input);
        return readFully(input);
    }

    public static byte[] readFully(InputStream input)
            throws IOException
    {
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buf = new byte[4096];
            for (int readBytes = 0; (readBytes = input.read(buf)) != -1; ) {
                out.write(buf, 0, readBytes);
            }
            out.flush();
            return out.toByteArray();
        }
        finally {
            input.close();
        }
    }

    public static byte[] byteWiseReadFully(InputStream input)
            throws IOException
    {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buf = new byte[4096];
        for (int readData = 0; (readData = input.read()) != -1; ) {
            out.write(readData);
        }
        out.flush();
        return out.toByteArray();
    }

    @Test
    public void read()
            throws Exception
    {
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
    public void readBlockCompressedData()
            throws Exception
    {
        byte[] orig = readResourceFile("alice29.txt");
        byte[] compressed = Snappy.compress(orig);

        SnappyInputStream in = new SnappyInputStream(new ByteArrayInputStream(compressed));
        byte[] uncompressed = readFully(in);

        assertEquals(orig.length, uncompressed.length);
        assertArrayEquals(orig, uncompressed);
    }

    @Test
    public void biteWiseRead()
            throws Exception
    {
        byte[] orig = readResourceFile("testdata/calgary/paper6");
        byte[] compressed = Snappy.compress(orig);

        SnappyInputStream in = new SnappyInputStream(new ByteArrayInputStream(compressed));
        byte[] uncompressed = byteWiseReadFully(in);

        assertEquals(orig.length, uncompressed.length);
        assertArrayEquals(orig, uncompressed);
    }

    @Test
    public void available()
            throws Exception
    {
        byte[] orig = readResourceFile("testdata/calgary/paper6");
        byte[] compressed = Snappy.compress(orig);

        SnappyInputStream in = new SnappyInputStream(new ByteArrayInputStream(compressed));
        byte[] buf = new byte[4];
        for (int readBytes = 0; (readBytes = in.read(buf)) != -1; ) {
            assertTrue(in.available() >= 0);
        }
        assertTrue(in.available() == 0);
        in.close();
    }

    @Test
    public void emptyStream()
            throws Exception
    {
        try {
            SnappyInputStream in = new SnappyInputStream(new ByteArrayInputStream(new byte[0]));
            byte[] uncompressed = readFully(in);
            assertEquals(0, uncompressed.length);
            fail("should not reach here");
        }
        catch (SnappyIOException e) {
            assertEquals(SnappyErrorCode.EMPTY_INPUT, e.getErrorCode());
        }
    }

    public static byte[] compressResource(String resourcePath)
            throws Exception
    {
        ByteArrayOutputStream compressedBuf = new ByteArrayOutputStream();
        SnappyOutputStream snappyOut = new SnappyOutputStream(compressedBuf);
        byte[] orig = readResourceFile(resourcePath);
        snappyOut.write(orig);
        snappyOut.close();
        return compressedBuf.toByteArray();
    }

    @Test
    public void chunkRead()
            throws Exception
    {
        byte[] chunk1 = compressResource("alice29.txt");
        byte[] chunk2 = compressResource("testdata/calgary/paper6");

        byte[] concatenated = new byte[chunk1.length + chunk2.length];
        System.arraycopy(chunk1, 0, concatenated, 0, chunk1.length);
        System.arraycopy(chunk2, 0, concatenated, chunk1.length, chunk2.length);

        SnappyInputStream in = new SnappyInputStream(new ByteArrayInputStream(concatenated));
        byte[] uncompressed = readFully(in);

        byte[] orig1 = readResourceFile("alice29.txt");
        byte[] orig2 = readResourceFile("testdata/calgary/paper6");
        assertEquals(orig1.length + orig2.length, uncompressed.length);
        byte[] uncompressed1 = new byte[orig1.length];
        byte[] uncompressed2 = new byte[orig2.length];
        System.arraycopy(uncompressed, 0, uncompressed1, 0, orig1.length);
        System.arraycopy(uncompressed, orig1.length, uncompressed2, 0, orig2.length);

        assertArrayEquals(orig1, uncompressed1);
        assertArrayEquals(orig2, uncompressed2);
    }

    @Test
    public void readSnappyCompressResult()
            throws Exception
    {
        byte[] orig = readResourceFile("alice29.txt");
        byte[] compressed = Snappy.compress(orig);
        SnappyInputStream in = new SnappyInputStream(new ByteArrayInputStream(compressed));
        byte[] uncompressed = readFully(in);

        assertArrayEquals(orig, uncompressed);
    }

    @Test
    public void checkMagicHeader()
            throws IOException
    {
        ByteArrayOutputStream b = new ByteArrayOutputStream();
        // Write uncompressed length beginning with -126 (the same with magicheader[0])
        b.write(SnappyCodec.MAGIC_HEADER[0]);
        b.write(0x01);
        // uncompressed data length = 130

        ByteArrayOutputStream data = new ByteArrayOutputStream();
        for(int i=0; i<130; ++i) {
            data.write('A');
        }
        byte[] dataMoreThan8Len = data.toByteArray();

        // write literal (lower 2-bit of the first tag byte is 00, upper 6-bits represents data size)
        b.write(60<<2); // 1-byte data length follows
        b.write(dataMoreThan8Len.length-1); // subsequent data length
        b.write(dataMoreThan8Len);

        byte[] compressed = b.toByteArray();

        // This should succeed
        assertArrayEquals(dataMoreThan8Len, Snappy.uncompress(compressed));

        // Reproduce error in #142
        SnappyInputStream in = new SnappyInputStream(new ByteArrayInputStream(b.toByteArray()));
        byte[] uncompressed = readFully(in);

        assertArrayEquals(dataMoreThan8Len, uncompressed);
    }
}
