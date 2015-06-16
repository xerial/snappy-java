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

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.xerial.util.FileResource;
import org.xerial.util.log.Logger;

/**
 * Benchmark using Calgary data set
 *
 * @author leo
 */
public class CalgaryTest
{
    private static Logger _logger = Logger.getLogger(CalgaryTest.class);

    @Rule
    public final TemporaryFolder tempFolder = new TemporaryFolder();

    static byte[] readFile(String file)
            throws IOException
    {
        InputStream in = FileResource.find(CalgaryTest.class, file).openStream();
        if (in == null) {
            throw new IOException("file " + file + " is not found");
        }
        try {
            return SnappyInputStreamTest.readFully(in);
        }
        finally {
            if (in != null) {
                in.close();
            }
        }
    }

    public static final String[] files = {"bib", "book1", "book2", "geo", "news", "obj1", "obj2", "paper1", "paper2",
                                          "paper3", "paper4", "paper5", "paper6", "pic", "progc", "progl", "progp", "trans"};

    @Test
    public void block()
            throws Exception
    {
        for (String f : files) {
            byte[] orig = readFile("testdata/calgary/" + f);

            byte[] compressed = Snappy.compress(orig);
            byte[] uncompressed = Snappy.uncompress(compressed);

            assertArrayEquals(orig, uncompressed);
        }
    }

    @Test
    public void stream()
            throws Exception
    {
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
    public void streamFramed()
            throws Exception
    {
        for (String f : files) {
            byte[] orig = readFile("testdata/calgary/" + f);

            ByteArrayOutputStream compressedBuf = new ByteArrayOutputStream();
            SnappyFramedOutputStream out = new SnappyFramedOutputStream(compressedBuf);
            out.write(orig);
            out.close();

            SnappyFramedInputStream in = new SnappyFramedInputStream(new ByteArrayInputStream(compressedBuf.toByteArray()));

            byte[] uncompressed = new byte[orig.length];
            int readBytes = readBytes(in, uncompressed, 0, orig.length);

            assertEquals(orig.length, readBytes);
            assertArrayEquals(orig, uncompressed);
        }
    }

    @Test
    public void streamFramedToFile()
            throws Exception
    {
        for (String f : files) {
            byte[] orig = readFile("testdata/calgary/" + f);

            final File tempFile = tempFolder.newFile(f);
            final FileOutputStream compressedFOS = new FileOutputStream(tempFile);
            try {
                SnappyFramedOutputStream out = new SnappyFramedOutputStream(compressedFOS);
                out.write(orig);
                out.close();
            }
            finally {
                compressedFOS.close();
            }

            byte[] uncompressed = new byte[orig.length];

            final FileInputStream compressedFIS = new FileInputStream(tempFile);
            try {
                SnappyFramedInputStream in = new SnappyFramedInputStream(compressedFIS.getChannel());
                int readBytes = readBytes(in, uncompressed, 0, orig.length);

                assertEquals(orig.length, readBytes);
            }
            finally {
                compressedFIS.close();
            }

            assertArrayEquals(orig, uncompressed);
        }
    }

    @Test
    public void streamFramedNoCRCVerify()
            throws Exception
    {
        for (String f : files) {
            byte[] orig = readFile("testdata/calgary/" + f);

            ByteArrayOutputStream compressedBuf = new ByteArrayOutputStream();
            SnappyFramedOutputStream out = new SnappyFramedOutputStream(compressedBuf);
            out.write(orig);
            out.close();

            SnappyFramedInputStream in = new SnappyFramedInputStream(new ByteArrayInputStream(compressedBuf.toByteArray()), false);

            byte[] uncompressed = new byte[orig.length];
            int readBytes = readBytes(in, uncompressed, 0, orig.length);

            assertEquals(orig.length, readBytes);
            assertArrayEquals(orig, uncompressed);
        }
    }

    @Test
    public void byteWiseRead()
            throws Exception
    {
        for (String f : files) {
            byte[] orig = readFile("testdata/calgary/" + f);

            ByteArrayOutputStream compressedBuf = new ByteArrayOutputStream();
            SnappyOutputStream out = new SnappyOutputStream(compressedBuf);
            out.write(orig);
            out.close();

            SnappyInputStream in = new SnappyInputStream(new ByteArrayInputStream(compressedBuf.toByteArray()));
            byte[] uncompressed = new byte[orig.length];
            int cursor = 0;
            for (; ; ) {
                int b = in.read();
                if (b == -1) {
                    break;
                }
                uncompressed[cursor++] = (byte) b;
            }
            assertEquals(orig.length, cursor);
            assertArrayEquals(orig, uncompressed);
        }
    }

    static final int readBytes(InputStream source, byte[] dest, int offset, int length)
            throws IOException
    {
        // how many bytes were read.
        int lastRead = source.read(dest, offset, length);

        int totalRead = lastRead;

        // if we did not read as many bytes as we had hoped, try reading again.
        if (lastRead < length) {
            // as long the buffer is not full (remaining() == 0) and we have not reached EOF (lastRead == -1) keep reading.
            while (totalRead < length && lastRead != -1) {
                lastRead = source.read(dest, offset + totalRead, length - totalRead);

                // if we got EOF, do not add to total read.
                if (lastRead != -1) {
                    totalRead += lastRead;
                }
            }
        }
        return totalRead;
    }
}
