/*
 * Created: Apr 12, 2013
 */
package org.xerial.snappy;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;

/**
 * Constants and utilities for implementing x-snappy-framed.
 * 
 * @author Brett Okken
 * @since 1.1.0
 */
final class SnappyFramed {
    public static final int COMPRESSED_DATA_FLAG = 0x00;

    public static final int UNCOMPRESSED_DATA_FLAG = 0x01;

    public static final int STREAM_IDENTIFIER_FLAG = 0xff;
    
    private static final int MASK_DELTA = 0xa282ead8;

    /**
     * The header consists of the stream identifier flag, 3 bytes indicating a
     * length of 6, and "sNaPpY" in ASCII.
     */
    public static final byte[] HEADER_BYTES = new byte[] {
            (byte) STREAM_IDENTIFIER_FLAG, 0x06, 0x00, 0x00, 0x73, 0x4e, 0x61,
            0x50, 0x70, 0x59 };

    public static int maskedCrc32c(byte[] data)
    {
        return maskedCrc32c(data, 0, data.length);
    }

    public static int maskedCrc32c(byte[] data, int offset, int length)
    {
        final PureJavaCrc32C crc32c = new PureJavaCrc32C();
        crc32c.update(data, offset, length);
        return mask(crc32c.getIntegerValue());
    }

    /**
     * Checksums are not stored directly, but masked, as checksumming data and
     * then its own checksum can be problematic. The masking is the same as used
     * in Apache Hadoop: Rotate the checksum by 15 bits, then add the constant
     * 0xa282ead8 (using wraparound as normal for unsigned integers). This is
     * equivalent to the following C code:
     * 
     * <pre>
     * uint32_t mask_checksum(uint32_t x) {
     *     return ((x >> 15) | (x << 17)) + 0xa282ead8; 
     * }
     * </pre>
     */
    public static int mask(int crc)
    {
        // Rotate right by 15 bits and add a constant.
        return ((crc >>> 15) | (crc << 17)) + MASK_DELTA;
    }
    

    static final int readBytes(ReadableByteChannel source, ByteBuffer dest) throws IOException
    {
        // tells how many bytes to read.
        final int expectedLength = dest.remaining();

        int totalRead = 0;

        // how many bytes were read.
        int lastRead = source.read(dest);

        totalRead = lastRead;

        // if we did not read as many bytes as we had hoped, try reading again.
        if (lastRead < expectedLength)
        {
            // as long the buffer is not full (remaining() == 0) and we have not reached EOF (lastRead == -1) keep reading.
            while (dest.remaining() != 0 && lastRead != -1)
            {
                lastRead = source.read(dest);

                // if we got EOF, do not add to total read.
                if (lastRead != -1)
                {
                    totalRead += lastRead;
                }
            }
        }

        if (totalRead > 0)
        {
            dest.limit(dest.position());
        }
        else
        {
            dest.position(dest.limit());
        }

        return totalRead;
    }
    
    static int skip(final ReadableByteChannel source, final int skip, final ByteBuffer buffer) throws IOException
    {
        if (skip <= 0) {
            return 0;
        }
        
        int toSkip = skip;
        int skipped = 0; 
        while(toSkip > 0 && skipped != -1) {
            buffer.clear();
            if (toSkip < buffer.capacity()) {
                buffer.limit(toSkip);
            }
            
            skipped = source.read(buffer);
            if (skipped > 0) {
                toSkip -= skipped;
            }
        }

        buffer.clear();
        return skip - toSkip;
    }
}
