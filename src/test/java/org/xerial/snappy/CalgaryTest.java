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
// CalgaryTest.java
// Since: 2011/04/04 12:10:36
//
// $URL$
// $Author$
//--------------------------------------
package org.xerial.snappy;

import static org.junit.Assert.*;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import org.junit.Test;
import org.xerial.util.FileResource;
import org.xerial.util.log.Logger;

/**
 * Benchmark using Calgary data set
 * 
 * @author leo
 * 
 */
public class CalgaryTest
{
    private static Logger _logger = Logger.getLogger(CalgaryTest.class);

    static byte[] readFile(String file) throws IOException {
        InputStream in = FileResource.find(CalgaryTest.class, file).openStream();
        if (in == null)
            throw new IOException("file " + file + " is not found");
        try {
            return SnappyInputStreamTest.readFully(in);
        }
        finally {
            if (in != null)
                in.close();
        }
    }

    public static final String[] files = { "bib", "book1", "book2", "geo", "news", "obj1", "obj2", "paper1", "paper2",
            "paper3", "paper4", "paper5", "paper6", "pic", "progc", "progl", "progp", "trans" };

    @Test
    public void block() throws Exception {
        for (String f : files) {
            byte[] orig = readFile("testdata/calgary/" + f);

            byte[] compressed = Snappy.compress(orig);
            byte[] uncompressed = Snappy.uncompress(compressed);

            assertArrayEquals(orig, uncompressed);
        }
    }

    @Test
    public void stream() throws Exception {
        for (String f : files) {
            byte[] orig = readFile("testdata/calgary/" + f);

            ByteArrayOutputStream compressedBuf = new ByteArrayOutputStream();
            SnappyOutputStream out = new SnappyOutputStream(compressedBuf);
            out.write(orig);
            out.close();

            SnappyInputStream in = new SnappyInputStream(new ByteArrayInputStream(compressedBuf.toByteArray()));
            byte[] uncompressed = new byte[orig.length];
            int readBytes = in.read(uncompressed);
            assertEquals(orig.length, readBytes);
            assertArrayEquals(orig, uncompressed);
        }
    }

    @Test
    public void byteWiseRead() throws Exception {
        for (String f : files) {
            byte[] orig = readFile("testdata/calgary/" + f);

            ByteArrayOutputStream compressedBuf = new ByteArrayOutputStream();
            SnappyOutputStream out = new SnappyOutputStream(compressedBuf);
            out.write(orig);
            out.close();

            SnappyInputStream in = new SnappyInputStream(new ByteArrayInputStream(compressedBuf.toByteArray()));
            byte[] uncompressed = new byte[orig.length];
            int cursor = 0;
            for (;;) {
                int b = in.read();
                if (b == -1)
                    break;
                uncompressed[cursor++] = (byte) b;
            }
            assertEquals(orig.length, cursor);
            assertArrayEquals(orig, uncompressed);
        }
    }

}
