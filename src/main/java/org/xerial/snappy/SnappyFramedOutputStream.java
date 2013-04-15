/*
 * Created: Apr 12, 2013
 */
package org.xerial.snappy;

import static org.xerial.snappy.SnappyFramed.COMPRESSED_DATA_FLAG;
import static org.xerial.snappy.SnappyFramed.HEADER_BYTES;
import static org.xerial.snappy.SnappyFramed.UNCOMPRESSED_DATA_FLAG;
import static org.xerial.snappy.SnappyFramed.maskedCrc32c;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.WritableByteChannel;

/**
 * Implements the <a
 * href="http://snappy.googlecode.com/svn/trunk/framing_format.txt"
 * >x-snappy-framed</a> as an {@link OutputStream} and
 * {@link WritableByteChannel}.
 * 
 * @author Brett Okken
 * @since 1.1.0
 */
public final class SnappyFramedOutputStream extends OutputStream implements
        WritableByteChannel {

    /**
     * The x-snappy-framed specification allows for a chunk size up to
     * 16,777,211 bytes in length. However, it also goes on to state:
     * <p>
     * <code>
     * We place an additional restriction that the uncompressed data in a chunk
     * must be no longer than 65536 bytes. This allows consumers to easily use 
     * small fixed-size buffers.
     * </code>
     * </p>
     */
    public static final int MAX_BLOCK_SIZE = 64 * 1024;

    /**
     * The default block size to use.
     */
    public static final int DEFAULT_BLOCK_SIZE = MAX_BLOCK_SIZE;

    /**
     * The default min compression ratio to use.
     */
    public static final double DEFAULT_MIN_COMPRESSION_RATIO = 0.85d;

    // private final int blockSize;
    private final ByteBuffer buffer;
    private final byte[] outputBuffer;
    private final double minCompressionRatio;

    private final OutputStream out;

    // private int position;
    private boolean closed;

    /**
     * Creates a new {@link SnappyFramedOutputStream} using the {@link #DEFAULT_BLOCK_SIZE}
     * and {@link #DEFAULT_MIN_COMPRESSION_RATIO}.
     * @param out
     *            The underlying {@link OutputStream} to write to. Must not be
     *            {@code null}.
     * @throws IOException
     */
    public SnappyFramedOutputStream(OutputStream out) throws IOException {
        this(out, DEFAULT_BLOCK_SIZE, DEFAULT_MIN_COMPRESSION_RATIO);
    }

    /**
     * Creates a new {@link SnappyFramedOutputStream} instance.
     * 
     * @param out
     *            The underlying {@link OutputStream} to write to. Must not be
     *            {@code null}.
     * @param blockSize
     *            The block size (of raw data) to compress before writing frames
     *            to <i>out</i>. Must be in (0, 65536].
     * @param minCompressionRatio
     *            Defines the minimum compression ratio (
     *            {@code compressedLength / rawLength}) that must be achieved to
     *            write the compressed data. This must be in (0, 1.0].
     * @throws IOException
     */
    public SnappyFramedOutputStream(OutputStream out, int blockSize,
            double minCompressionRatio) throws IOException {
        if (out == null) {
            throw new NullPointerException();
        }

        if (minCompressionRatio <= 0 || minCompressionRatio > 1.0) {
            throw new IllegalArgumentException("minCompressionRatio "
                    + minCompressionRatio + " must be in (0,1.0]");
        }

        if (blockSize <= 0 || blockSize > MAX_BLOCK_SIZE) {
            throw new IllegalArgumentException("block size " + blockSize
                    + " must be in (0, 65536]");
        }

        this.out = out;
        this.minCompressionRatio = minCompressionRatio;
        buffer = ByteBuffer.allocate(blockSize);
        outputBuffer = new byte[Snappy.maxCompressedLength(blockSize)];

        writeHeader(out);
    }

    /**
     * Writes the implementation specific header or "marker bytes" to
     * <i>out</i>.
     * 
     * @param out
     *            The underlying {@link OutputStream}.
     * @throws IOException
     */
    private void writeHeader(OutputStream out) throws IOException {
        out.write(HEADER_BYTES);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isOpen() {
        return !closed;
    }

    @Override
    public void write(int b) throws IOException {
        if (closed) {
            throw new IOException("Stream is closed");
        }
        if (buffer.remaining() <= 0) {
            flushBuffer();
        }
        buffer.put((byte) b);
    }

    @Override
    public void write(byte[] input, int offset, int length) throws IOException {
        if (closed) {
            throw new IOException("Stream is closed");
        }

        write(ByteBuffer.wrap(input, offset, length));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int write(ByteBuffer src) throws IOException {
        if (closed) {
            throw new ClosedChannelException();
        }

        if (buffer.remaining() <= 0) {
            flushBuffer();
        }

        final int srcLength = src.remaining();

        // easy case: enough free space in buffer for entire input
        if (buffer.remaining() >= src.remaining()) {
            buffer.put(src);
            return srcLength;
        }

        // store current limit
        final int srcEnd = src.position() + src.remaining();

        while ((src.position() + buffer.remaining()) <= srcEnd) {
            // fill partial buffer as much as possible and flush
            src.limit(src.position() + buffer.remaining());
            buffer.put(src);
            flushBuffer();
        }

        // reset original limit
        src.limit(srcEnd);

        // copy remaining partial block into now-empty buffer
        buffer.put(src);

        return srcLength;
    }

    @Override
    public final void flush() throws IOException {
        if (closed) {
            throw new IOException("Stream is closed");
        }
        flushBuffer();
        out.flush();
    }

    @Override
    public final void close() throws IOException {
        if (closed) {
            return;
        }
        try {
            flush();
            out.close();
        } finally {
            closed = true;
        }
    }

    /**
     * Compresses and writes out any buffered data. This does nothing if there
     * is no currently buffered data.
     * 
     * @throws IOException
     */
    private void flushBuffer() throws IOException {
        if (buffer.position() > 0) {
            buffer.flip();
            writeCompressed(buffer);
            buffer.clear();
        }
    }

    /**
     * {@link #calculateCRC32C(byte[], int, int) Calculates} the crc, compresses
     * the data, determines if the compression ratio is acceptable and calls
     * {@link #writeBlock(OutputStream, byte[], int, int, boolean, int)} to
     * actually write the frame.
     * 
     * @param input
     *            The byte[] containing the raw data to be compressed.
     * @param offset
     *            The offset into <i>input</i> where the data starts.
     * @param length
     *            The amount of data in <i>input</i>.
     * @throws IOException
     */
    private void writeCompressed(ByteBuffer buffer) throws IOException {

        final byte[] input = buffer.array();
        final int length = buffer.remaining();

        // crc is based on the user supplied input data
        final int crc32c = calculateCRC32C(input, 0, length);

        final int compressedLength = Snappy.compress(input, 0, length,
                outputBuffer, 0);

        // only use the compressed data if copmression ratio is <= the
        // minCompressonRatio
        if (((double) compressedLength / (double) length) <= minCompressionRatio) {
            writeBlock(out, outputBuffer, 0, compressedLength, true, crc32c);
        } else {
            // otherwise use the uncomprssed data.
            writeBlock(out, input, 0, length, false, crc32c);
        }
    }

    /**
     * Calculates a masked CRC32C checksum over the data.
     * 
     * @param data
     * @param offset
     * @param length
     * @return The CRC32 checksum.
     */
    private int calculateCRC32C(byte[] data, int offset, int length) {
        return maskedCrc32c(data, offset, length);
    }

    /**
     * Write a frame (block) to <i>out</i>.
     * 
     * @param out
     *            The {@link OutputStream} to write to.
     * @param data
     *            The data to write.
     * @param offset
     *            The offset in <i>data</i> to start at.
     * @param length
     *            The length of <i>data</i> to use.
     * @param compressed
     *            Indicates if <i>data</i> is the compressed or raw content.
     *            This is based on whether the compression ratio desired is
     *            reached.
     * @param crc32c
     *            The calculated checksum.
     * @throws IOException
     */
    private void writeBlock(final OutputStream out, byte[] data, int offset,
            int length, boolean compressed, int crc32c) throws IOException {
        out.write(compressed ? COMPRESSED_DATA_FLAG : UNCOMPRESSED_DATA_FLAG);

        // the length written out to the header is both the checksum and the
        // frame
        final int headerLength = length + 4;

        // write length
        out.write(headerLength);
        out.write(headerLength >>> 8);
        out.write(headerLength >>> 16);

        // write crc32c of user input data
        out.write(crc32c);
        out.write(crc32c >>> 8);
        out.write(crc32c >>> 16);
        out.write(crc32c >>> 24);

        // write data
        out.write(data, offset, length);
    }
}
