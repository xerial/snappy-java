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
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.nio.ByteOrder;

import org.junit.Test;
import org.junit.Assert;
import org.xerial.snappy.buffer.BufferAllocatorFactory;
import org.xerial.snappy.buffer.CachedBufferAllocator;
import org.xerial.snappy.buffer.DefaultBufferAllocator;
import org.xerial.util.FileResource;
import org.xerial.util.log.Logger;

public class SnappyOutputStreamTest
{
    private static Logger _logger = Logger.getLogger(SnappyOutputStreamTest.class);

    @Test
    public void test()
            throws Exception
    {
        ByteArrayOutputStream buf = new ByteArrayOutputStream();
        SnappyOutputStream sout = new SnappyOutputStream(buf);

        BufferedInputStream input = new BufferedInputStream(FileResource.find(SnappyOutputStreamTest.class,
                "alice29.txt").openStream());
        assertNotNull(input);

        ByteArrayOutputStream orig = new ByteArrayOutputStream();
        byte[] tmp = new byte[1024];
        for (int readBytes = 0; (readBytes = input.read(tmp)) != -1; ) {
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
        for (int cursor = SnappyCodec.headerSize(); cursor < compressed.length; ) {
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
    public void bufferSize()
            throws Exception
    {
        ByteArrayOutputStream b = new ByteArrayOutputStream();
        SnappyOutputStream os = new SnappyOutputStream(b, 1500);
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
        while (is.read(buf) != -1) {
        }
        is.close();
    }

    @Test(expected = IllegalArgumentException.class)
    public void invalidBlockSize()
            throws Exception
    {
        // We rely on catch below, if there is no error this test will pass
        // This can be done better with Assertions.assertThrows
        Boolean exceptionThrown = false;
        ByteArrayOutputStream b = new ByteArrayOutputStream();
        SnappyOutputStream os = new SnappyOutputStream(b, 1024 * 1024 * 1024);
    }

    @Test
    public void smallWrites()
            throws Exception
    {

        byte[] orig = CalgaryTest.readFile("alice29.txt");
        ByteArrayOutputStream b = new ByteArrayOutputStream();
        SnappyOutputStream out = new SnappyOutputStream(b);

        for (byte c : orig) {
            out.write(c);
        }
        out.close();

        SnappyInputStream is = new SnappyInputStream(new ByteArrayInputStream(b.toByteArray()));
        byte[] decompressed = new byte[orig.length];
        int cursor = 0;
        int readLen = 0;
        for (int i = 0; i < decompressed.length && (readLen = is.read(decompressed, i, decompressed.length - i)) != -1; ) {
            i += readLen;
        }
        is.close();
        assertArrayEquals(orig, decompressed);
    }

    /**
     * Compress the input array by passing it chunk-by-chunk to a SnappyOutputStream.
     *
     * @param orig the data to compress
     * @param maxChunkSize the maximum chunk size, in bytes.
     * @return the compressed bytes
     */
    private static byte[] compressAsChunks(byte[] orig, int maxChunkSize)
            throws Exception
    {
        ByteArrayOutputStream b = new ByteArrayOutputStream();
        SnappyOutputStream out = new SnappyOutputStream(b);

        int remaining = orig.length;
        for (int start = 0; start < orig.length; start += maxChunkSize) {
            out.write(orig, start, remaining < maxChunkSize ? remaining : maxChunkSize);
            remaining -= maxChunkSize;
        }
        out.close();
        return b.toByteArray();
    }

    @Test
    public void batchingOfWritesShouldNotAffectCompressedDataSize()
            throws Exception
    {
        // Regression test for issue #100, a bug where the size of compressed data could be affected
        // by the batching of writes to the SnappyOutputStream rather than the total amount of data
        // written to the stream.
        byte[] orig = CalgaryTest.readFile("alice29.txt");
        // Compress the data once so that we know the expected size:
        byte[] expectedCompressedData = compressAsChunks(orig, Integer.MAX_VALUE);
        // Hardcoding an expected compressed size here will catch regressions that lower the
        // compression quality:
        if (ByteOrder.nativeOrder() == ByteOrder.BIG_ENDIAN)
            assertEquals(90992, expectedCompressedData.length);
        else
            assertEquals(91080, expectedCompressedData.length);
        // The chunk size should not affect the size of the compressed output:
        int[] chunkSizes = new int[] {1, 100, 1023, 1024, 10000};
        for (int chunkSize : chunkSizes) {
            byte[] compressedData = compressAsChunks(orig, chunkSize);
            assertEquals(String.format("when chunk size = %,d", chunkSize), expectedCompressedData.length, compressedData.length);
            assertArrayEquals(expectedCompressedData, compressedData);
        }
    }

    @Test
    public void closeShouldBeIdempotent()
            throws Exception
    {
        // Regression test for issue #107, a bug where close() was non-idempotent and would release
        // its buffers to the allocator multiple times, which could cause scenarios where two open
        // SnappyOutputStreams could share the same buffers, leading to stream corruption issues.
        final BufferAllocatorFactory bufferAllocatorFactory = CachedBufferAllocator.getBufferAllocatorFactory();
        final int BLOCK_SIZE = 4096;
        // Create a stream, use it, then close it once:
        ByteArrayOutputStream ba1 = new ByteArrayOutputStream();
        SnappyOutputStream os1 = new SnappyOutputStream(ba1, BLOCK_SIZE, bufferAllocatorFactory);
        os1.write(42);
        os1.close();
        // Create a new output stream, which should end up re-using the first stream's freed buffers
        ByteArrayOutputStream ba2 = new ByteArrayOutputStream();
        SnappyOutputStream os2 = new SnappyOutputStream(ba2, BLOCK_SIZE, bufferAllocatorFactory);
        // Close the first stream a second time, which is supposed to be safe due to idempotency:
        os1.close();
        // Allocate a third output stream, which is supposed to get its own fresh set of buffers:
        ByteArrayOutputStream ba3 = new ByteArrayOutputStream();
        SnappyOutputStream os3 = new SnappyOutputStream(ba3, BLOCK_SIZE, bufferAllocatorFactory);
        // Since the second and third streams should have distinct sets of buffers, writes to these
        // streams should not interfere with one another:
        os2.write(2);
        os3.write(3);
        os2.close();
        os3.close();
        SnappyInputStream in2 = new SnappyInputStream(new ByteArrayInputStream(ba2.toByteArray()));
        assertEquals(2, in2.read());
        in2.close();
        SnappyInputStream in3 = new SnappyInputStream(new ByteArrayInputStream(ba3.toByteArray()));
        assertEquals(3, in3.read());
        in3.close();
    }

    @Test
    public void writingToClosedStreamShouldThrowIOException()
            throws IOException
    {
        ByteArrayOutputStream b = new ByteArrayOutputStream();
        SnappyOutputStream os = new SnappyOutputStream(b);
        os.close();
        try {
            os.write(4);
            fail("Expected write() to throw IOException");
        }
        catch (IOException e) {
            // Expected exception
        }
        try {
            os.write(new int[] {1, 2, 3, 4});
            fail("Expected write() to throw IOException");
        }
        catch (IOException e) {
            // Expected exception
        }
    }

    @Test
    public void flushingClosedStreamShouldThrowIOException()
            throws IOException
    {
        ByteArrayOutputStream b = new ByteArrayOutputStream();
        SnappyOutputStream os = new SnappyOutputStream(b);
        os.close();
        try {
            os.flush();
        }
        catch (IOException e) {
            // Expected exception
        }
    }

    @Test
    public void closingStreamShouldMakeBuffersEligibleForGarbageCollection()
            throws IOException
    {
        ByteArrayOutputStream b = new ByteArrayOutputStream();
        SnappyOutputStream os = new SnappyOutputStream(b, 4095, DefaultBufferAllocator.factory);
        WeakReference<byte[]> inputBuffer = new WeakReference<byte[]>(os.inputBuffer);
        WeakReference<byte[]> outputBuffer = new WeakReference<byte[]>(os.inputBuffer);
        os.close();
        System.gc();
        assertNull(inputBuffer.get());
        assertNull(outputBuffer.get());
    }

    @Test
    public void longArrayCompress()
            throws Exception
    {
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
    public void writeDoubleArray()
            throws Exception
    {
        ByteArrayOutputStream b = new ByteArrayOutputStream();
        SnappyOutputStream os = new SnappyOutputStream(b);

        double[] orig = new double[] {1.0, 2.0, 1.4, 0.00343430014, -4.4, 4e-20};
        os.write(orig);
        os.close();

        SnappyInputStream is = new SnappyInputStream(new ByteArrayInputStream(b.toByteArray()));
        double[] uncompressed = new double[orig.length];
        is.read(uncompressed);
        is.close();

        assertArrayEquals(orig, uncompressed, 0.0);
    }

    @Test
    public void writeFloatArray()
            throws Exception
    {
        ByteArrayOutputStream b = new ByteArrayOutputStream();
        SnappyOutputStream os = new SnappyOutputStream(b);

        float[] orig = new float[] {1.0f, 2.0f, 1.4f, 0.00343430014f, -4.4f, 4e-20f};
        os.write(orig);
        os.close();

        SnappyInputStream is = new SnappyInputStream(new ByteArrayInputStream(b.toByteArray()));
        float[] uncompressed = new float[orig.length];
        is.read(uncompressed);
        is.close();

        assertArrayEquals(orig, uncompressed, 0.0f);
    }

    @Test
    public void writeIntArray()
            throws Exception
    {
        ByteArrayOutputStream b = new ByteArrayOutputStream();
        SnappyOutputStream os = new SnappyOutputStream(b);

        int[] orig = new int[] {0, -1, -34, 43, 234, 34324, -234};
        os.write(orig);
        os.close();

        SnappyInputStream is = new SnappyInputStream(new ByteArrayInputStream(b.toByteArray()));
        int[] uncompressed = new int[orig.length];
        is.read(uncompressed);
        is.close();

        assertArrayEquals(orig, uncompressed);
    }

    @Test
    public void writeShortArray()
            throws Exception
    {
        ByteArrayOutputStream b = new ByteArrayOutputStream();
        SnappyOutputStream os = new SnappyOutputStream(b);

        short[] orig = new short[] {0, -1, -34, 43, 234, 324, -234};
        os.write(orig);
        os.close();

        SnappyInputStream is = new SnappyInputStream(new ByteArrayInputStream(b.toByteArray()));
        short[] uncompressed = new short[orig.length];
        is.read(uncompressed);
        is.close();

        assertArrayEquals(orig, uncompressed);
    }
}
