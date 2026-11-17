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
package org.xerial.snappy;

import static org.junit.Assert.*;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Arrays;
import java.util.Collection;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.xerial.snappy.buffer.DefaultBufferAllocator;

@RunWith(Parameterized.class)
public class SnappyOutputStreamBlockSizeTest
{
    @Parameterized.Parameters(name = "blockSize={0}")
    public static Collection<Object[]> sizes()
    {
        return Arrays.asList(new Object[][] {{-1}, {0}, {1}, {1023}, {1024}, {1025}, {32768}});
    }

    private final int blockSize;

    public SnappyOutputStreamBlockSizeTest(int blockSize)
    {
        this.blockSize = blockSize;
    }

    @Test
    public void bulkBytesRoundTripAcrossBlocks()
            throws Exception
    {
        byte[] expected = new byte[70003];
        for (int i = 0; i < expected.length; i++) {
            expected[i] = (byte) (i * 31);
        }
        ByteArrayOutputStream compressed = new ByteArrayOutputStream();
        try (SnappyOutputStream out = new SnappyOutputStream(compressed, blockSize, DefaultBufferAllocator.factory)) {
            out.write(expected, 0, 3);
            out.flush();
            out.write(expected, 3, expected.length - 3);
        }
        assertArrayEquals(expected, decompress(compressed));
    }

    @Test
    public void individualBytesRoundTripAcrossBlocks()
            throws Exception
    {
        byte[] expected = new byte[2051];
        ByteArrayOutputStream compressed = new ByteArrayOutputStream();
        try (SnappyOutputStream out = new SnappyOutputStream(compressed, blockSize, DefaultBufferAllocator.factory)) {
            for (int i = 0; i < expected.length; i++) {
                expected[i] = (byte) i;
                out.write(expected[i] & 0xff);
            }
        }
        assertArrayEquals(expected, decompress(compressed));
    }

    @Test
    public void primitiveArrayRoundTripAcrossBlocks()
            throws Exception
    {
        long[] expected = new long[9001];
        for (int i = 0; i < expected.length; i++) {
            expected[i] = 0x1020304050607080L ^ i;
        }
        ByteArrayOutputStream compressed = new ByteArrayOutputStream();
        try (SnappyOutputStream out = new SnappyOutputStream(compressed, blockSize, DefaultBufferAllocator.factory)) {
            // Protect the JNI copy from an undersized array when run against the unfixed implementation.
            assertTrue(out.inputBuffer.length >= SnappyOutputStream.MIN_BLOCK_SIZE);
            out.write(expected);
        }
        byte[] bytes = decompress(compressed);
        assertEquals(expected.length * 8, bytes.length);
        long[] actual = new long[expected.length];
        ByteBuffer.wrap(bytes).order(ByteOrder.nativeOrder()).asLongBuffer().get(actual);
        assertArrayEquals(expected, actual);
    }

    private static byte[] decompress(ByteArrayOutputStream compressed)
            throws Exception
    {
        ByteArrayOutputStream result = new ByteArrayOutputStream();
        try (SnappyInputStream in = new SnappyInputStream(new ByteArrayInputStream(compressed.toByteArray()))) {
            byte[] buffer = new byte[731];
            for (int read; (read = in.read(buffer)) != -1;) {
                result.write(buffer, 0, read);
            }
        }
        return result.toByteArray();
    }
}
