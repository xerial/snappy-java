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
// SnappyOutputStreamTest.java
// Since: 2011/03/31 18:26:31
//
// $URL$
// $Author$
//--------------------------------------
package org.xerial.snappy;

import static org.junit.Assert.*;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

import org.junit.Test;
import org.xerial.util.FileResource;
import org.xerial.util.log.Logger;

public class SnappyOutputStreamTest
{
    private static Logger _logger = Logger.getLogger(SnappyOutputStreamTest.class);

    @Test
    public void test() throws Exception {
        ByteArrayOutputStream buf = new ByteArrayOutputStream();
        SnappyOutputStream sout = new SnappyOutputStream(buf);

        BufferedInputStream input = new BufferedInputStream(FileResource.find(SnappyOutputStreamTest.class,
                "alice29.txt").openStream());
        assertNotNull(input);

        ByteArrayOutputStream orig = new ByteArrayOutputStream();
        byte[] tmp = new byte[1024];
        for (int readBytes = 0; (readBytes = input.read(tmp)) != -1;) {
            sout.write(tmp, 0, readBytes);
            orig.write(tmp, 0, readBytes); // preserve the original data
        }
        input.close();
        sout.flush();
        orig.flush();

        int compressedSize = buf.size();
        _logger.debug("compressed size: " + compressedSize);

        ByteArrayOutputStream decompressed = new ByteArrayOutputStream();
        byte[] compressed = buf.toByteArray();
        // decompress
        for (int cursor = SnappyCodec.headerSize(); cursor < compressed.length;) {
            int chunkSize = SnappyOutputStream.readInt(compressed, cursor);
            cursor += 4;
            byte[] tmpOut = new byte[Snappy.uncompressedLength(compressed, cursor, chunkSize)];
            int decompressedSize = Snappy.uncompress(compressed, cursor, chunkSize, tmpOut, 0);
            cursor += chunkSize;

            decompressed.write(tmpOut);
        }
        decompressed.flush();
        assertEquals(orig.size(), decompressed.size());
        assertArrayEquals(orig.toByteArray(), decompressed.toByteArray());
    }

    @Test
    public void bufferSize() throws Exception {
        ByteArrayOutputStream b = new ByteArrayOutputStream();
        SnappyOutputStream os = new SnappyOutputStream(b, 500);
        final int bytesToWrite = 5000;
        byte[] orig = new byte[bytesToWrite];
        for (int i = 0; i < 5000; ++i) {
            byte v = (byte) (i % 128);
            orig[i] = v;
            os.write(v);
        }
        os.close();
        SnappyInputStream is = new SnappyInputStream(new ByteArrayInputStream(b.toByteArray()));
        byte[] buf = new byte[bytesToWrite / 101];
        while (is.read(buf) != -1) {}
        is.close();
    }

    @Test
    public void longArrayCompress() throws Exception {
        long[] l = new long[10];
        for (int i = 0; i < l.length; ++i) {
            l[i] = i % 3 + i * 11;
        }

        ByteArrayOutputStream b = new ByteArrayOutputStream();
        SnappyOutputStream os = new SnappyOutputStream(b);

        os.write(l);
        os.close();
        SnappyInputStream is = new SnappyInputStream(new ByteArrayInputStream(b.toByteArray()));
        long[] l2 = new long[10];
        int readBytes = is.read(l2);
        is.close();

        assertEquals(10 * 8, readBytes);
        assertArrayEquals(l, l2);

    }

    @Test
    public void writeDoubleArray() throws Exception {
        ByteArrayOutputStream b = new ByteArrayOutputStream();
        SnappyOutputStream os = new SnappyOutputStream(b);

        double[] orig = new double[] { 1.0, 2.0, 1.4, 0.00343430014, -4.4, 4e-20 };
        os.write(orig);
        os.close();

        SnappyInputStream is = new SnappyInputStream(new ByteArrayInputStream(b.toByteArray()));
        double[] uncompressed = new double[orig.length];
        is.read(uncompressed);
        is.close();

        assertArrayEquals(orig, uncompressed, 0.0);
    }

    @Test
    public void writeFloatArray() throws Exception {
        ByteArrayOutputStream b = new ByteArrayOutputStream();
        SnappyOutputStream os = new SnappyOutputStream(b);

        float[] orig = new float[] { 1.0f, 2.0f, 1.4f, 0.00343430014f, -4.4f, 4e-20f };
        os.write(orig);
        os.close();

        SnappyInputStream is = new SnappyInputStream(new ByteArrayInputStream(b.toByteArray()));
        float[] uncompressed = new float[orig.length];
        is.read(uncompressed);
        is.close();

        assertArrayEquals(orig, uncompressed, 0.0f);
    }

    @Test
    public void writeIntArray() throws Exception {
        ByteArrayOutputStream b = new ByteArrayOutputStream();
        SnappyOutputStream os = new SnappyOutputStream(b);

        int[] orig = new int[] { 0, -1, -34, 43, 234, 34324, -234 };
        os.write(orig);
        os.close();

        SnappyInputStream is = new SnappyInputStream(new ByteArrayInputStream(b.toByteArray()));
        int[] uncompressed = new int[orig.length];
        is.read(uncompressed);
        is.close();

        assertArrayEquals(orig, uncompressed);
    }

    @Test
    public void writeShortArray() throws Exception {
        ByteArrayOutputStream b = new ByteArrayOutputStream();
        SnappyOutputStream os = new SnappyOutputStream(b);

        short[] orig = new short[] { 0, -1, -34, 43, 234, 324, -234 };
        os.write(orig);
        os.close();

        SnappyInputStream is = new SnappyInputStream(new ByteArrayInputStream(b.toByteArray()));
        short[] uncompressed = new short[orig.length];
        is.read(uncompressed);
        is.close();

        assertArrayEquals(orig, uncompressed);
    }

}
