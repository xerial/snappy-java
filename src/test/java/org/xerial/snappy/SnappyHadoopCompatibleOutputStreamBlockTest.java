package org.xerial.snappy;

import org.junit.Assert;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Random;

/**
 * Regression test for https://github.com/xerial/snappy-java/issues/767: the Hadoop block preamble
 * (raw length + compressed length = 8 bytes) must be reserved before compressing a block.
 */
public class SnappyHadoopCompatibleOutputStreamBlockTest
{
    @Test
    public void incompressibleTrailingBlock()
            throws Exception
    {
        int blockSize = SnappyOutputStream.DEFAULT_BLOCK_SIZE;
        byte[] data = new byte[2 * blockSize];
        new Random(0).nextBytes(data);
        // A full incompressible block followed by a trailing block of every size: some tail sizes leave
        // between 4 and 8 bytes of slack in the output buffer, which used to overflow it.
        for (int tail = 1; tail < blockSize; tail++) {
            byte[] input = Arrays.copyOf(data, blockSize + tail);

            ByteArrayOutputStream compressed = new ByteArrayOutputStream();
            SnappyHadoopCompatibleOutputStream os = new SnappyHadoopCompatibleOutputStream(compressed, blockSize);
            os.write(input);
            os.close();

            Assert.assertArrayEquals("tail=" + tail, input, decodeHadoopBlocks(compressed.toByteArray()));
        }
    }

    private static byte[] decodeHadoopBlocks(byte[] data)
            throws IOException
    {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        int pos = 0;
        while (pos < data.length) {
            int rawLength = SnappyOutputStream.readInt(data, pos);
            int compressedLength = SnappyOutputStream.readInt(data, pos + 4);
            pos += 8;
            byte[] raw = Snappy.uncompress(Arrays.copyOfRange(data, pos, pos + compressedLength));
            Assert.assertEquals(rawLength, raw.length);
            out.write(raw);
            pos += compressedLength;
        }
        return out.toByteArray();
    }
}
