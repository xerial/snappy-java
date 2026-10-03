package org.xerial.snappy;

import static org.junit.Assert.*;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Random;

import org.junit.Test;

/**
 * Regression tests for buffer bounds checks before calling native code, and for limits on sizes declared in
 * untrusted compressed data.
 */
public class SnappyBoundsCheckTest
{
    private static byte[] randomBytes(int size)
    {
        byte[] data = new byte[size];
        new Random(42).nextBytes(data);
        return data;
    }

    private static ByteBuffer directBufferOf(byte[] data)
    {
        ByteBuffer buf = ByteBuffer.allocateDirect(data.length);
        buf.put(data);
        buf.flip();
        return buf;
    }

    private static byte[] varint(long v)
    {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        while ((v & ~0x7FL) != 0) {
            out.write((int) ((v & 0x7F) | 0x80));
            v >>>= 7;
        }
        out.write((int) v);
        return out.toByteArray();
    }

    // https://github.com/xerial/snappy-java/issues/732
    @Test(expected = IllegalArgumentException.class)
    public void compressByteBufferToUndersizedOutput()
            throws Exception
    {
        ByteBuffer src = directBufferOf(randomBytes(1024 * 1024));
        Snappy.compress(src, ByteBuffer.allocateDirect(64));
    }

    @Test
    public void compressByteBufferToMaxCompressedLengthOutput()
            throws Exception
    {
        byte[] data = randomBytes(1024 * 1024);
        ByteBuffer compressed = ByteBuffer.allocateDirect(Snappy.maxCompressedLength(data.length));
        Snappy.compress(directBufferOf(data), compressed);
        ByteBuffer uncompressed = ByteBuffer.allocateDirect(data.length);
        assertEquals(data.length, Snappy.uncompress(compressed, uncompressed));
        byte[] result = new byte[data.length];
        uncompressed.get(result);
        assertArrayEquals(data, result);
    }

    @Test(expected = IllegalArgumentException.class)
    public void compressByteArrayToUndersizedOutput()
            throws Exception
    {
        byte[] data = randomBytes(1024 * 1024);
        Snappy.compress(data, 0, data.length, new byte[64], 0);
    }

    @Test(expected = IllegalArgumentException.class)
    public void compressByteArrayWithOutOfRangeInput()
            throws Exception
    {
        byte[] data = randomBytes(100);
        Snappy.compress(data, 50, 100, new byte[Snappy.maxCompressedLength(100)], 0);
    }

    @Test(expected = IllegalArgumentException.class)
    public void compressByteArrayWithNegativeOutputOffset()
            throws Exception
    {
        byte[] data = randomBytes(100);
        Snappy.compress(data, 0, data.length, new byte[1024], -1);
    }

    @Test(expected = IllegalArgumentException.class)
    public void rawCompressWithTooLargeByteSize()
            throws Exception
    {
        Snappy.rawCompress(new int[10], 41);
    }

    // https://github.com/xerial/snappy-java/issues/728 (CVE-2026-90559)
    @Test(expected = IllegalArgumentException.class)
    public void uncompressByteBufferToUndersizedOutput()
            throws Exception
    {
        byte[] data = new byte[1024 * 1024];
        ByteBuffer compressed = ByteBuffer.allocateDirect(Snappy.maxCompressedLength(data.length));
        Snappy.compress(directBufferOf(data), compressed);
        Snappy.uncompress(compressed, ByteBuffer.allocateDirect(64));
    }

    @Test(expected = IllegalArgumentException.class)
    public void uncompressByteArrayToUndersizedOutput()
            throws Exception
    {
        byte[] compressed = Snappy.compress(new byte[1024 * 1024]);
        Snappy.uncompress(compressed, 0, compressed.length, new byte[64], 0);
    }

    @Test(expected = IllegalArgumentException.class)
    public void uncompressByteArrayWithOutOfRangeOutputOffset()
            throws Exception
    {
        byte[] compressed = Snappy.compress(new byte[100]);
        Snappy.uncompress(compressed, 0, compressed.length, new byte[200], 150);
    }

    @Test(expected = IllegalArgumentException.class)
    public void uncompressByteArrayWithOutOfRangeInput()
            throws Exception
    {
        byte[] compressed = Snappy.compress(new byte[100]);
        Snappy.uncompress(compressed, 1, compressed.length, new byte[200], 0);
    }

    @Test(expected = IllegalArgumentException.class)
    public void rawUncompressToUndersizedTypedArray()
            throws Exception
    {
        byte[] compressed = Snappy.compress(new int[100]);
        Snappy.rawUncompress(compressed, 0, compressed.length, new int[99], 0);
    }

    @Test
    public void uncompressByteArrayAtOffset()
            throws Exception
    {
        byte[] data = randomBytes(1000);
        byte[] compressed = Snappy.compress(data);
        byte[] output = new byte[1100];
        assertEquals(data.length, Snappy.uncompress(compressed, 0, compressed.length, output, 100));
        assertArrayEquals(data, Arrays.copyOfRange(output, 100, 1100));
    }

    // https://github.com/xerial/snappy-java/issues/729
    @Test
    public void uncompressTypedArrayWithMisalignedLength()
            throws Exception
    {
        // 15 bytes is not a multiple of any of the element sizes
        byte[] compressed = Snappy.compress(randomBytes(15));
        try {
            Snappy.uncompressLongArray(compressed);
            fail("expected SnappyIOException");
        }
        catch (SnappyIOException e) {
            assertEquals(SnappyErrorCode.FAILED_TO_UNCOMPRESS, e.getErrorCode());
        }
        try {
            Snappy.uncompressDoubleArray(compressed);
            fail("expected SnappyIOException");
        }
        catch (SnappyIOException e) {
            // expected
        }
        try {
            Snappy.uncompressIntArray(compressed);
            fail("expected SnappyIOException");
        }
        catch (SnappyIOException e) {
            // expected
        }
        try {
            Snappy.uncompressFloatArray(compressed);
            fail("expected SnappyIOException");
        }
        catch (SnappyIOException e) {
            // expected
        }
        try {
            Snappy.uncompressShortArray(compressed);
            fail("expected SnappyIOException");
        }
        catch (SnappyIOException e) {
            // expected
        }
        try {
            Snappy.uncompressCharArray(compressed);
            fail("expected SnappyIOException");
        }
        catch (SnappyIOException e) {
            // expected
        }
    }

    // https://github.com/xerial/snappy-java/issues/625
    @Test
    public void uncompressWithHugeDeclaredLength()
            throws Exception
    {
        byte[] data = new byte[] {(byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0x07};
        try {
            Snappy.uncompress(data);
            fail("expected SnappyIOException");
        }
        catch (SnappyIOException e) {
            assertEquals(SnappyErrorCode.PARSING_ERROR, e.getErrorCode());
        }
    }

    // https://github.com/xerial/snappy-java/issues/625
    @Test
    public void uncompressWithNegativeDeclaredLength()
            throws Exception
    {
        byte[] data = new byte[] {(byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0x08};
        try {
            Snappy.uncompress(data);
            fail("expected SnappyIOException");
        }
        catch (SnappyIOException e) {
            assertEquals(SnappyErrorCode.PARSING_ERROR, e.getErrorCode());
        }
    }

    @Test
    public void uncompressedLengthAcceptsHighlyCompressibleData()
            throws Exception
    {
        // all-zero input is close to the maximum compression ratio of Snappy
        byte[] data = new byte[16 * 1024 * 1024];
        byte[] compressed = Snappy.compress(data);
        assertEquals(data.length, Snappy.uncompressedLength(compressed));
        assertArrayEquals(data, Snappy.uncompress(compressed));
        assertEquals(0, Snappy.uncompress(Snappy.compress(new byte[0])).length);
    }

    @Test(expected = IllegalArgumentException.class)
    public void uncompressedLengthWithOutOfRangeInput()
            throws Exception
    {
        Snappy.uncompressedLength(new byte[10], 5, 10);
    }

    @Test(expected = IllegalArgumentException.class)
    public void isValidCompressedBufferWithOutOfRangeInput()
            throws Exception
    {
        Snappy.isValidCompressedBuffer(new byte[10], -1, 5);
    }

    private static void writeChunk(ByteArrayOutputStream out, int flag, byte[] body)
    {
        out.write(flag);
        out.write(body.length & 0xFF);
        out.write((body.length >>> 8) & 0xFF);
        out.write((body.length >>> 16) & 0xFF);
        out.write(body, 0, body.length);
    }

    private static byte[] readAll(InputStream in)
            throws IOException
    {
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buf = new byte[4096];
            int n;
            while ((n = in.read(buf)) != -1) {
                out.write(buf, 0, n);
            }
            return out.toByteArray();
        }
        finally {
            in.close();
        }
    }

    // https://github.com/xerial/snappy-java/issues/731
    @Test
    public void framedStreamWithManySkippableChunks()
            throws Exception
    {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        stream.write(SnappyFramed.HEADER_BYTES);
        for (int i = 0; i < 200000; i++) {
            writeChunk(stream, 0x80, new byte[0]);
        }
        byte[] data = "hello".getBytes("UTF-8");
        ByteArrayOutputStream body = new ByteArrayOutputStream();
        int crc = SnappyFramed.maskedCrc32c(SnappyFramed.getCRC32C(), data, 0, data.length);
        for (int i = 0; i < 4; i++) {
            body.write((crc >>> (8 * i)) & 0xFF);
        }
        body.write(data);
        writeChunk(stream, 0x01, body.toByteArray());
        for (int i = 0; i < 200000; i++) {
            writeChunk(stream, 0xfe, new byte[0]);
        }

        byte[] result = readAll(new SnappyFramedInputStream(new ByteArrayInputStream(stream.toByteArray())));
        assertArrayEquals(data, result);
    }

    // https://github.com/xerial/snappy-java/issues/730
    @Test
    public void framedStreamWithHugeDeclaredUncompressedLength()
            throws Exception
    {
        ByteArrayOutputStream body = new ByteArrayOutputStream();
        body.write(new byte[4]); // checksum, which must not be reached
        body.write(varint(1L << 30));
        body.write(new byte[] {0x00, 0x61}); // a single literal
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        stream.write(SnappyFramed.HEADER_BYTES);
        writeChunk(stream, 0x00, body.toByteArray());

        try {
            readAll(new SnappyFramedInputStream(new ByteArrayInputStream(stream.toByteArray())));
            fail("expected SnappyIOException");
        }
        catch (SnappyIOException e) {
            assertEquals(SnappyErrorCode.PARSING_ERROR, e.getErrorCode());
        }
    }

    // https://github.com/xerial/snappy-java/issues/671
    @Test
    public void maxCompressedLengthOverflow()
            throws Exception
    {
        // 32 + n + n / 6 is Integer.MAX_VALUE for n = 1840700242
        assertEquals(Integer.MAX_VALUE, Snappy.maxCompressedLength(1840700242));
        try {
            Snappy.maxCompressedLength(1840700243);
            fail("expected SnappyError");
        }
        catch (SnappyError e) {
            assertEquals(SnappyErrorCode.TOO_LARGE_INPUT, e.errorCode);
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void maxCompressedLengthOfNegativeSize()
            throws Exception
    {
        Snappy.maxCompressedLength(-1);
    }

    @Test(expected = IllegalArgumentException.class)
    public void arrayCopyOutOfRange()
            throws Exception
    {
        Snappy.arrayCopy(new int[4], 0, 17, new byte[100], 0);
    }

    // https://github.com/xerial/snappy-java/issues/513
    @Test(expected = IllegalArgumentException.class)
    public void outputStreamRawWriteOutOfRange()
            throws Exception
    {
        SnappyOutputStream out = new SnappyOutputStream(new ByteArrayOutputStream());
        out.rawWrite(new int[4], 0, 1024);
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void outputStreamWriteTypedArrayOutOfRange()
            throws Exception
    {
        SnappyOutputStream out = new SnappyOutputStream(new ByteArrayOutputStream());
        out.write(new long[4], 2, 3);
    }

    @Test(expected = SnappyError.class)
    public void elementRangeWithOverflowingByteSize()
    {
        // a long[] range whose byte offset overflows int; called directly to avoid allocating a 2GB array
        int length = Integer.MAX_VALUE / 8 + 2;
        Snappy.checkElementRange(length, length - 1, 1, 8);
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void outputStreamWriteBytesOutOfRange()
            throws Exception
    {
        SnappyOutputStream out = new SnappyOutputStream(new ByteArrayOutputStream());
        out.write(new byte[10], 5, 10);
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void inputStreamReadTypedArrayOutOfRange()
            throws Exception
    {
        ByteArrayOutputStream compressed = new ByteArrayOutputStream();
        SnappyOutputStream out = new SnappyOutputStream(compressed);
        out.write(new int[100]);
        out.close();
        SnappyInputStream in = new SnappyInputStream(new ByteArrayInputStream(compressed.toByteArray()));
        in.read(new int[10], 5, 10);
    }

    @Test(expected = IllegalArgumentException.class)
    public void inputStreamRawReadOutOfRange()
            throws Exception
    {
        ByteArrayOutputStream compressed = new ByteArrayOutputStream();
        SnappyOutputStream out = new SnappyOutputStream(compressed);
        out.write(new int[100]);
        out.close();
        SnappyInputStream in = new SnappyInputStream(new ByteArrayInputStream(compressed.toByteArray()));
        in.rawRead(new int[10], 0, 400);
    }

    /**
     * An output stream that fails on write, as when the disk is full, and records whether it was closed
     */
    private static class FailingOutputStream
            extends java.io.OutputStream
    {
        boolean closed = false;

        @Override
        public void write(int b)
                throws IOException
        {
            throw new IOException("No space left on device");
        }

        @Override
        public void write(byte[] b, int off, int len)
                throws IOException
        {
            throw new IOException("No space left on device");
        }

        @Override
        public void close()
        {
            closed = true;
        }
    }

    // https://github.com/xerial/snappy-java/issues/216
    @Test
    public void outputStreamClosesUnderlyingStreamWhenFlushFails()
            throws Exception
    {
        FailingOutputStream underlying = new FailingOutputStream();
        SnappyOutputStream out = new SnappyOutputStream(underlying);
        out.write(new byte[100]);
        try {
            out.close();
            fail("expected IOException");
        }
        catch (IOException e) {
            // expected: the flush failure must still be reported
        }
        assertTrue(underlying.closed);
    }

    // https://github.com/xerial/snappy-java/issues/216
    @Test
    public void framedOutputStreamClosesUnderlyingStreamWhenFlushFails()
            throws Exception
    {
        // the constructor writes the stream header, so fail only on the writes after it
        final FailingOutputStream failAfterHeader = new FailingOutputStream()
        {
            int written = 0;

            @Override
            public void write(byte[] b, int off, int len)
                    throws IOException
            {
                if (written > 0) {
                    super.write(b, off, len);
                }
                written += len;
            }
        };
        SnappyFramedOutputStream out = new SnappyFramedOutputStream(failAfterHeader);
        out.write(new byte[100]);
        try {
            out.close();
            fail("expected IOException");
        }
        catch (IOException e) {
            // expected
        }
        assertTrue(failAfterHeader.closed);
    }

    // GHSA-6gp7-6wmv-gxqw: a long run of concatenated stream headers must not exhaust the stack
    @Test
    public void inputStreamWithManyConcatenatedHeaders()
            throws Exception
    {
        ByteArrayOutputStream single = new ByteArrayOutputStream();
        SnappyOutputStream out = new SnappyOutputStream(single);
        out.write("hello".getBytes("UTF-8"));
        out.close();
        byte[] stream = single.toByteArray();
        byte[] header = Arrays.copyOf(stream, SnappyCodec.headerSize());

        ByteArrayOutputStream data = new ByteArrayOutputStream();
        data.write(stream);
        for (int i = 0; i < 100000; i++) {
            data.write(header);
        }
        data.write(stream, header.length, stream.length - header.length);

        byte[] result = readAll(new SnappyInputStream(new ByteArrayInputStream(data.toByteArray())));
        assertEquals("hellohello", new String(result, "UTF-8"));
    }

    // GHSA-6gp7-6wmv-gxqw: a chunk header alone must not allocate the declared chunk size
    @Test
    public void inputStreamWithTruncatedLargeChunk()
            throws Exception
    {
        ByteArrayOutputStream data = new ByteArrayOutputStream();
        ByteArrayOutputStream single = new ByteArrayOutputStream();
        new SnappyOutputStream(single).close();
        data.write(Arrays.copyOf(single.toByteArray(), SnappyCodec.headerSize()));
        // declare a 500 MiB chunk followed by only a few bytes
        int chunkSize = 500 * 1024 * 1024;
        data.write(new byte[] {(byte) (chunkSize >>> 24), (byte) (chunkSize >>> 16), (byte) (chunkSize >>> 8),
                (byte) chunkSize, 1, 2, 3});

        long before = usedHeap();
        try {
            readAll(new SnappyInputStream(new ByteArrayInputStream(data.toByteArray())));
            fail("expected IOException");
        }
        catch (IOException e) {
            // expected: the chunk is truncated
        }
        assertTrue("allocated too much memory for a truncated chunk", usedHeap() - before < 100L * 1024 * 1024);
    }

    private static long usedHeap()
    {
        Runtime rt = Runtime.getRuntime();
        return rt.totalMemory() - rt.freeMemory();
    }

    // GHSA-c73m-r934-8qvg: a failed buffer allocation must not lead to releasing the same buffer twice
    @Test
    public void framedStreamDoesNotReleaseBuffersTwiceWhenAllocationFails()
            throws Exception
    {
        final java.util.IdentityHashMap<Object, Boolean> released = new java.util.IdentityHashMap<Object, Boolean>();
        org.xerial.snappy.pool.BufferPool pool = new org.xerial.snappy.pool.BufferPool()
        {
            @Override
            public byte[] allocateArray(int size)
            {
                return new byte[size];
            }

            @Override
            public void releaseArray(byte[] buffer)
            {
                assertNull("array released twice", released.put(buffer, Boolean.TRUE));
            }

            @Override
            public ByteBuffer allocateDirect(int size)
            {
                if (size > 1024 * 1024) {
                    throw new OutOfMemoryError("simulated");
                }
                return ByteBuffer.allocateDirect(size);
            }

            @Override
            public void releaseDirect(ByteBuffer buffer)
            {
                assertNull("direct buffer released twice", released.put(buffer, Boolean.TRUE));
            }
        };

        // a valid frame whose uncompressed size (2 MiB) exceeds the initial buffers
        byte[] compressed = Snappy.compress(new byte[2 * 1024 * 1024]);
        ByteArrayOutputStream body = new ByteArrayOutputStream();
        body.write(new byte[4]);
        body.write(compressed);
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        stream.write(SnappyFramed.HEADER_BYTES);
        writeChunk(stream, 0x00, body.toByteArray());

        SnappyFramedInputStream in = new SnappyFramedInputStream(
                java.nio.channels.Channels.newChannel(new ByteArrayInputStream(stream.toByteArray())), false, pool);
        try {
            in.read();
            fail("expected OutOfMemoryError");
        }
        catch (OutOfMemoryError e) {
            // expected
        }
        finally {
            in.close();
        }
    }
}
