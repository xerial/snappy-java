/*
 * Created: Mar 14, 2013
 */
package org.xerial.snappy;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.xerial.snappy.SnappyFramed.COMPRESSED_DATA_FLAG;
import static org.xerial.snappy.SnappyFramed.HEADER_BYTES;
import static org.xerial.snappy.SnappyFramed.UNCOMPRESSED_DATA_FLAG;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.channels.Channels;
import java.nio.channels.WritableByteChannel;
import java.util.Arrays;

import org.junit.Test;

/**
 * Tests the functionality of {@link org.xerial.snappy.SnappyFramedInputStream}
 * and {@link org.xerial.snappy.SnappyFramedOutputStream}.
 *
 * @author Brett Okken
 */
public class SnappyFramedStreamTest
{

    /**
     * @throws IOException
     */
    protected OutputStream createOutputStream(OutputStream target)
            throws IOException
    {
        return new SnappyFramedOutputStream(target);
    }

    /**
     * {@inheritDoc}
     *
     * @throws IOException
     */
    protected InputStream createInputStream(InputStream source,
            boolean verifyCheckSums)
            throws IOException
    {
        return new SnappyFramedInputStream(source, verifyCheckSums);
    }

    protected byte[] getMarkerFrame()
    {
        return HEADER_BYTES;
    }

    @Test
    public void testSimple()
            throws Exception
    {
        byte[] original = "aaaaaaaaaaaabbbbbbbaaaaaa".getBytes("utf-8");

        byte[] compressed = compress(original);
        byte[] uncompressed = uncompress(compressed);

        assertArrayEquals(uncompressed, original);
        // 10 byte stream header, 4 byte block header, 4 byte crc, 19 bytes
        assertEquals(compressed.length, 37);

        // stream header
        assertArrayEquals(Arrays.copyOf(compressed, 10), HEADER_BYTES);

        // flag: compressed
        assertEquals(toInt(compressed[10]), COMPRESSED_DATA_FLAG);

        // length: 23 = 0x000017
        assertEquals(toInt(compressed[11]), 0x17);
        assertEquals(toInt(compressed[12]), 0x00);
        assertEquals(toInt(compressed[13]), 0x00);

        // crc32c: 0x9274cda8
        assertEquals(toInt(compressed[17]), 0x92);
        assertEquals(toInt(compressed[16]), 0x74);
        assertEquals(toInt(compressed[15]), 0xCD);
        assertEquals(toInt(compressed[14]), 0xA8);
    }

    @Test
    public void testUncompressable()
            throws Exception
    {
        byte[] random = getRandom(1, 5000);
        int crc32c = maskedCrc32c(random);

        byte[] compressed = compress(random);
        byte[] uncompressed = uncompress(compressed);

        assertArrayEquals(uncompressed, random);
        assertEquals(compressed.length, random.length + 10 + 4 + 4);

        // flag: uncompressed
        assertEquals(toInt(compressed[10]), UNCOMPRESSED_DATA_FLAG);

        // length: 5004 = 0x138c
        assertEquals(toInt(compressed[13]), 0x00);
        assertEquals(toInt(compressed[12]), 0x13);
        assertEquals(toInt(compressed[11]), 0x8c);
    }

    @Test
    public void testEmptyCompression()
            throws Exception
    {
        byte[] empty = new byte[0];
        assertArrayEquals(compress(empty), HEADER_BYTES);
        assertArrayEquals(uncompress(HEADER_BYTES), empty);
    }

    @Test(expected = EOFException.class)
    public void testShortBlockHeader()
            throws Exception
    {
        uncompressBlock(new byte[] {0});
    }

    @Test(expected = EOFException.class)
    public void testShortBlockData()
            throws Exception
    {
        // flag = 0, size = 8, crc32c = 0, block data= [x, x]
        uncompressBlock(new byte[] {1, 8, 0, 0, 0, 0, 0, 0, 'x', 'x'});
    }

    @Test
    public void testUnskippableChunkFlags()
            throws Exception
    {
        for (int i = 2; i <= 0x7f; i++) {
            try {
                uncompressBlock(new byte[] {(byte) i, 5, 0, 0, 0, 0, 0, 0, 0});
                fail("no exception thrown with flag: " + Integer.toHexString(i));
            }
            catch (IOException e) {

            }
        }
    }

    @Test
    public void testSkippableChunkFlags()
            throws Exception
    {
        for (int i = 0x80; i <= 0xfe; i++) {
            try {
                uncompressBlock(new byte[] {(byte) i, 5, 0, 0, 0, 0, 0, 0, 0});
            }
            catch (IOException e) {
                fail("exception thrown with flag: " + Integer.toHexString(i));
            }
        }
    }

    @Test(expected = IOException.class)
    public void testInvalidBlockSizeZero()
            throws Exception
    {
        // flag = '0', block size = 4, crc32c = 0
        uncompressBlock(new byte[] {1, 4, 0, 0, 0, 0, 0, 0});
    }

    @Test(expected = IOException.class)
    public void testInvalidChecksum()
            throws Exception
    {
        // flag = 0, size = 5, crc32c = 0, block data = [a]
        uncompressBlock(new byte[] {1, 5, 0, 0, 0, 0, 0, 0, 'a'});
    }

    @Test
    public void testInvalidChecksumIgnoredWhenVerificationDisabled()
            throws Exception
    {
        // flag = 0, size = 4, crc32c = 0, block data = [a]
        byte[] block = {1, 5, 0, 0, 0, 0, 0, 0, 'a'};
        ByteArrayInputStream inputData = new ByteArrayInputStream(
                blockToStream(block));
        assertArrayEquals(toByteArray(createInputStream(inputData, false)),
                new byte[] {'a'});
    }

    @Test
    public void testTransferFrom_InputStream()
            throws IOException
    {
        final byte[] random = getRandom(0.5, 100000);

        final ByteArrayOutputStream baos = new ByteArrayOutputStream(
                random.length);
        final SnappyFramedOutputStream sfos = new SnappyFramedOutputStream(baos);

        sfos.transferFrom(new ByteArrayInputStream(random));

        sfos.close();

        final byte[] uncompressed = uncompress(baos.toByteArray());

        assertArrayEquals(random, uncompressed);
    }

    @Test
    public void testTransferFrom_ReadableByteChannel()
            throws IOException
    {
        final byte[] random = getRandom(0.5, 100000);

        final ByteArrayOutputStream baos = new ByteArrayOutputStream(
                random.length);
        final SnappyFramedOutputStream sfos = new SnappyFramedOutputStream(baos);

        sfos.transferFrom(Channels.newChannel(new ByteArrayInputStream(random)));

        sfos.close();

        final byte[] uncompressed = uncompress(baos.toByteArray());

        assertArrayEquals(random, uncompressed);
    }

    @Test
    public void testTransferTo_OutputStream()
            throws IOException
    {
        final byte[] random = getRandom(0.5, 100000);

        final byte[] compressed = compress(random);
        final SnappyFramedInputStream sfis = new SnappyFramedInputStream(
                new ByteArrayInputStream(compressed));

        final ByteArrayOutputStream baos = new ByteArrayOutputStream(
                random.length);
        sfis.transferTo(baos);

        assertArrayEquals(random, baos.toByteArray());
    }

    @Test
    public void testTransferTo_WritableByteChannel()
            throws IOException
    {
        final byte[] random = getRandom(0.5, 100000);

        final byte[] compressed = compress(random);
        final SnappyFramedInputStream sfis = new SnappyFramedInputStream(
                new ByteArrayInputStream(compressed));

        final ByteArrayOutputStream baos = new ByteArrayOutputStream(
                random.length);
        final WritableByteChannel wbc = Channels.newChannel(baos);
        sfis.transferTo(wbc);
        wbc.close();

        assertArrayEquals(random, baos.toByteArray());
    }

    @Test
    public void testLargerFrames_raw_()
            throws IOException
    {
        final byte[] random = getRandom(0.5, 100000);

        final byte[] stream = new byte[HEADER_BYTES.length + 8 + random.length];
        System.arraycopy(HEADER_BYTES, 0, stream, 0, HEADER_BYTES.length);

        stream[10] = UNCOMPRESSED_DATA_FLAG;

        int length = random.length + 4;
        stream[11] = (byte) length;
        stream[12] = (byte) (length >>> 8);
        stream[13] = (byte) (length >>> 16);

        int crc32c = maskedCrc32c(random);
        stream[14] = (byte) crc32c;
        stream[15] = (byte) (crc32c >>> 8);
        stream[16] = (byte) (crc32c >>> 16);
        stream[17] = (byte) (crc32c >>> 24);

        System.arraycopy(random, 0, stream, 18, random.length);

        final byte[] uncompressed = uncompress(stream);

        assertArrayEquals(random, uncompressed);
    }

    @Test
    public void testLargerFrames_compressed_()
            throws IOException
    {
        final byte[] random = getRandom(0.5, 500000);

        final byte[] compressed = Snappy.compress(random);

        final byte[] stream = new byte[HEADER_BYTES.length + 8 + compressed.length];
        System.arraycopy(HEADER_BYTES, 0, stream, 0, HEADER_BYTES.length);

        stream[10] = COMPRESSED_DATA_FLAG;

        int length = compressed.length + 4;
        stream[11] = (byte) length;
        stream[12] = (byte) (length >>> 8);
        stream[13] = (byte) (length >>> 16);

        int crc32c = maskedCrc32c(random);
        stream[14] = (byte) crc32c;
        stream[15] = (byte) (crc32c >>> 8);
        stream[16] = (byte) (crc32c >>> 16);
        stream[17] = (byte) (crc32c >>> 24);

        System.arraycopy(compressed, 0, stream, 18, compressed.length);

        final byte[] uncompressed = uncompress(stream);

        assertArrayEquals(random, uncompressed);
    }

    @Test
    public void testLargerFrames_compressed_smaller_raw_larger()
            throws IOException
    {
        final byte[] random = getRandom(0.5, 100000);

        final byte[] compressed = Snappy.compress(random);

        final byte[] stream = new byte[HEADER_BYTES.length + 8
                + compressed.length];
        System.arraycopy(HEADER_BYTES, 0, stream, 0, HEADER_BYTES.length);

        stream[10] = COMPRESSED_DATA_FLAG;

        int length = compressed.length + 4;
        stream[11] = (byte) length;
        stream[12] = (byte) (length >>> 8);
        stream[13] = (byte) (length >>> 16);

        int crc32c = maskedCrc32c(random);
        stream[14] = (byte) crc32c;
        stream[15] = (byte) (crc32c >>> 8);
        stream[16] = (byte) (crc32c >>> 16);
        stream[17] = (byte) (crc32c >>> 24);

        System.arraycopy(compressed, 0, stream, 18, compressed.length);

        final byte[] uncompressed = uncompress(stream);

        assertArrayEquals(random, uncompressed);
    }

    private byte[] uncompressBlock(byte[] block)
            throws IOException
    {
        return uncompress(blockToStream(block));
    }

    private static byte[] blockToStream(byte[] block)
    {
        byte[] stream = new byte[HEADER_BYTES.length + block.length];
        System.arraycopy(HEADER_BYTES, 0, stream, 0, HEADER_BYTES.length);
        System.arraycopy(block, 0, stream, HEADER_BYTES.length, block.length);
        return stream;
    }

    protected byte[] compress(byte[] original)
            throws IOException
    {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        OutputStream snappyOut = createOutputStream(out);
        snappyOut.write(original);
        snappyOut.close();
        return out.toByteArray();
    }

    protected byte[] uncompress(byte[] compressed)
            throws IOException
    {
        return toByteArray(createInputStream(new ByteArrayInputStream(
                compressed), true));
    }

    private static byte[] toByteArray(InputStream createInputStream)
            throws IOException
    {
        final ByteArrayOutputStream baos = new ByteArrayOutputStream(64 * 1024);

        final byte[] buffer = new byte[8 * 1024];

        int read;
        while ((read = createInputStream.read(buffer)) > 0) {
            baos.write(buffer, 0, read);
        }

        return baos.toByteArray();
    }

    static int toInt(byte value)
    {
        return value & 0xFF;
    }

    private byte[] getRandom(double compressionRatio, int length)
    {
        RandomGenerator gen = new RandomGenerator(
                compressionRatio);
        gen.getNextPosition(length);
        byte[] random = Arrays.copyOf(gen.data, length);
        assertEquals(random.length, length);
        return random;
    }

    public static int maskedCrc32c(byte[] data)
    {
        return SnappyFramed.maskedCrc32c(new PureJavaCrc32C(), data, 0, data.length);
    }
}
