/*
 * Created: Apr 12, 2013
 */
package org.xerial.snappy;

import static org.xerial.snappy.SnappyFramed.COMPRESSED_DATA_FLAG;
import static org.xerial.snappy.SnappyFramed.HEADER_BYTES;
import static org.xerial.snappy.SnappyFramed.UNCOMPRESSED_DATA_FLAG;
import static org.xerial.snappy.SnappyFramed.maskedCrc32c;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.Channels;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.util.zip.Checksum;

import org.xerial.snappy.pool.BufferPool;
import org.xerial.snappy.pool.DefaultPoolFactory;

/**
 * Implements the <a
 * href="http://snappy.googlecode.com/svn/trunk/framing_format.txt"
 * >x-snappy-framed</a> as an {@link OutputStream} and
 * {@link WritableByteChannel}.
 *
 * @author Brett Okken
 * @since 1.1.0
 */
public final class SnappyFramedOutputStream
        extends OutputStream
        implements
        WritableByteChannel
{

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

    private final Checksum crc32 = SnappyFramed.getCRC32C();
    private final ByteBuffer headerBuffer = ByteBuffer.allocate(8).order(
            ByteOrder.LITTLE_ENDIAN);
    private final BufferPool bufferPool;
    private final int blockSize;
    private final ByteBuffer buffer;
    private final ByteBuffer directInputBuffer;
    private final ByteBuffer outputBuffer;
    private final double minCompressionRatio;

    private final WritableByteChannel out;

    // private int position;
    private boolean closed;

    /**
     * Creates a new {@link SnappyFramedOutputStream} using the {@link #DEFAULT_BLOCK_SIZE}
     * and {@link #DEFAULT_MIN_COMPRESSION_RATIO}.
     * <p>
     * Uses {@link DefaultPoolFactory} to obtain {@link BufferPool} for buffers.
     * </p>
     *
     * @param out The underlying {@link OutputStream} to write to. Must not be
     * {@code null}.
     * @throws IOException
     */
    public SnappyFramedOutputStream(OutputStream out)
            throws IOException
    {
        this(out, DEFAULT_BLOCK_SIZE, DEFAULT_MIN_COMPRESSION_RATIO, DefaultPoolFactory.getDefaultPool());
    }

    /**
     * Creates a new {@link SnappyFramedOutputStream} using the {@link #DEFAULT_BLOCK_SIZE}
     * and {@link #DEFAULT_MIN_COMPRESSION_RATIO}.
     *
     * @param out The underlying {@link OutputStream} to write to. Must not be
     * {@code null}.
     * @param bufferPool Used to obtain buffer instances. Must not be {@code null}. 
     * @throws IOException
     */
    public SnappyFramedOutputStream(OutputStream out, BufferPool bufferPool)
            throws IOException
    {
        this(out, DEFAULT_BLOCK_SIZE, DEFAULT_MIN_COMPRESSION_RATIO, bufferPool);
    }

    /**
     * Creates a new {@link SnappyFramedOutputStream} instance.
     * <p>
     * Uses {@link DefaultPoolFactory} to obtain {@link BufferPool} for buffers.
     * </p>
     *
     * @param out The underlying {@link OutputStream} to write to. Must not be
     * {@code null}.
     * @param blockSize The block size (of raw data) to compress before writing frames
     * to <i>out</i>. Must be in (0, 65536].
     * @param minCompressionRatio Defines the minimum compression ratio (
     * {@code compressedLength / rawLength}) that must be achieved to
     * write the compressed data. This must be in (0, 1.0].
     * @throws IOException
     */
    public SnappyFramedOutputStream(OutputStream out, int blockSize,
            double minCompressionRatio)
            throws IOException
    {
        this(Channels.newChannel(out), blockSize, minCompressionRatio, DefaultPoolFactory.getDefaultPool());
    }

    /**
     * Creates a new {@link SnappyFramedOutputStream} instance.
     *
     * @param out The underlying {@link OutputStream} to write to. Must not be
     * {@code null}.
     * @param blockSize The block size (of raw data) to compress before writing frames
     * to <i>out</i>. Must be in (0, 65536].
     * @param minCompressionRatio Defines the minimum compression ratio (
     * {@code compressedLength / rawLength}) that must be achieved to
     * write the compressed data. This must be in (0, 1.0].
     * @param bufferPool Used to obtain buffer instances. Must not be {@code null}. 
     * @throws IOException
     */
    public SnappyFramedOutputStream(OutputStream out, int blockSize,
            double minCompressionRatio, BufferPool bufferPool)
            throws IOException
    {
        this(Channels.newChannel(out), blockSize, minCompressionRatio, bufferPool);
    }

    /**
     * Creates a new {@link SnappyFramedOutputStream} using the
     * {@link #DEFAULT_BLOCK_SIZE} and {@link #DEFAULT_MIN_COMPRESSION_RATIO}.
     * <p>
     * Uses {@link DefaultPoolFactory} to obtain {@link BufferPool} for buffers.
     * </p>
     *
     * @param out The underlying {@link WritableByteChannel} to write to. Must
     * not be {@code null}.
     * @throws IOException
     * @since 1.1.1
     */
    public SnappyFramedOutputStream(WritableByteChannel out)
            throws IOException
    {
        this(out, DEFAULT_BLOCK_SIZE, DEFAULT_MIN_COMPRESSION_RATIO, DefaultPoolFactory.getDefaultPool());
    }

    /**
     * Creates a new {@link SnappyFramedOutputStream} using the
     * {@link #DEFAULT_BLOCK_SIZE} and {@link #DEFAULT_MIN_COMPRESSION_RATIO}.
     * <p>
     * Uses {@link DefaultPoolFactory} to obtain {@link BufferPool} for buffers.
     * </p>
     *
     * @param out The underlying {@link WritableByteChannel} to write to. Must
     * not be {@code null}.
     * @param bufferPool Used to obtain buffer instances. Must not be {@code null}. 
     * @throws IOException
     */
    public SnappyFramedOutputStream(WritableByteChannel out, BufferPool bufferPool)
            throws IOException
    {
        this(out, DEFAULT_BLOCK_SIZE, DEFAULT_MIN_COMPRESSION_RATIO, bufferPool);
    }

    /**
     * Creates a new {@link SnappyFramedOutputStream} instance.
     *
     * @param out The underlying {@link WritableByteChannel} to write to. Must
     * not be {@code null}.
     * @param blockSize The block size (of raw data) to compress before writing frames
     * to <i>out</i>. Must be in (0, 65536].
     * @param minCompressionRatio Defines the minimum compression ratio (
     * {@code compressedLength / rawLength}) that must be achieved to
     * write the compressed data. This must be in (0, 1.0].
     * @throws IOException
     * @since 1.1.1
     */
    public SnappyFramedOutputStream(WritableByteChannel out, int blockSize,
            double minCompressionRatio)
            throws IOException
    {
        this(out, blockSize, minCompressionRatio, DefaultPoolFactory.getDefaultPool());
    }

    /**
     * Creates a new {@link SnappyFramedOutputStream} instance.
     *
     * @param out The underlying {@link WritableByteChannel} to write to. Must
     * not be {@code null}.
     * @param blockSize The block size (of raw data) to compress before writing frames
     * to <i>out</i>. Must be in (0, 65536].
     * @param minCompressionRatio Defines the minimum compression ratio (
     * {@code compressedLength / rawLength}) that must be achieved to
     * write the compressed data. This must be in (0, 1.0].
     * @param bufferPool Used to obtain buffer instances. Must not be {@code null}.
     * @throws IOException
     * @since 1.1.1
     */
    public SnappyFramedOutputStream(WritableByteChannel out, int blockSize,
            double minCompressionRatio, BufferPool bufferPool)
            throws IOException
    {
        if (out == null) {
            throw new NullPointerException("out is null");
        }

        if (bufferPool == null) {
            throw new NullPointerException("buffer pool is null");
        }

        if (minCompressionRatio <= 0 || minCompressionRatio > 1.0) {
            throw new IllegalArgumentException("minCompressionRatio "
                    + minCompressionRatio + " must be in (0,1.0]");
        }

        if (blockSize <= 0 || blockSize > MAX_BLOCK_SIZE) {
            throw new IllegalArgumentException("block size " + blockSize
                    + " must be in (0, 65536]");
        }
        this.blockSize = blockSize;
        this.out = out;
        this.minCompressionRatio = minCompressionRatio;

        this.bufferPool = bufferPool;
        buffer = ByteBuffer.wrap(bufferPool.allocateArray(blockSize), 0, blockSize);
        directInputBuffer = bufferPool.allocateDirect(blockSize);
        outputBuffer = bufferPool.allocateDirect(Snappy
                .maxCompressedLength(blockSize));

        writeHeader(out);
    }

    /**
     * Writes the implementation specific header or "marker bytes" to
     * <i>out</i>.
     *
     * @param out The underlying {@link OutputStream}.
     * @throws IOException
     */
    private void writeHeader(WritableByteChannel out)
            throws IOException
    {
        out.write(ByteBuffer.wrap(HEADER_BYTES));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isOpen()
    {
        return !closed;
    }

    @Override
    public void write(int b)
            throws IOException
    {
        if (closed) {
            throw new IOException("Stream is closed");
        }
        if (buffer.remaining() <= 0) {
            flushBuffer();
        }
        buffer.put((byte) b);
    }

    @Override
    public void write(byte[] input, int offset, int length)
            throws IOException
    {
        if (closed) {
            throw new IOException("Stream is closed");
        }

        if (input == null) {
            throw new NullPointerException();
        }
        else if ((offset < 0) || (offset > input.length) || (length < 0)
                || ((offset + length) > input.length)
                || ((offset + length) < 0)) {
            throw new IndexOutOfBoundsException();
        }

        while (length > 0) {
            if (buffer.remaining() <= 0) {
                flushBuffer();
            }

            final int toPut = Math.min(length, buffer.remaining());
            buffer.put(input, offset, toPut);
            offset += toPut;
            length -= toPut;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int write(ByteBuffer src)
            throws IOException
    {
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

    /**
     * Transfers all the content from <i>is</i> to this {@link OutputStream}.
     * This potentially limits the amount of buffering required to compress
     * content.
     *
     * @param is The source of data to compress.
     * @return The number of bytes read from <i>is</i>.
     * @throws IOException
     * @since 1.1.1
     */
    public long transferFrom(InputStream is)
            throws IOException
    {
        if (closed) {
            throw new ClosedChannelException();
        }

        if (is == null) {
            throw new NullPointerException();
        }

        if (buffer.remaining() == 0) {
            flushBuffer();
        }

        assert buffer.hasArray();
        final byte[] bytes = buffer.array();

        final int arrayOffset = buffer.arrayOffset();
        long totTransfered = 0;
        int read;
        while ((read = is.read(bytes, arrayOffset + buffer.position(),
                buffer.remaining())) != -1) {
            buffer.position(buffer.position() + read);

            if (buffer.remaining() == 0) {
                flushBuffer();
            }

            totTransfered += read;
        }

        return totTransfered;
    }

    /**
     * Transfers all the content from <i>rbc</i> to this
     * {@link WritableByteChannel}. This potentially limits the amount of
     * buffering required to compress content.
     *
     * @param rbc The source of data to compress.
     * @return The number of bytes read from <i>rbc</i>.
     * @throws IOException
     * @since 1.1.1
     */
    public long transferFrom(ReadableByteChannel rbc)
            throws IOException
    {
        if (closed) {
            throw new ClosedChannelException();
        }

        if (rbc == null) {
            throw new NullPointerException();
        }

        if (buffer.remaining() == 0) {
            flushBuffer();
        }

        long totTransfered = 0;
        int read;
        while ((read = rbc.read(buffer)) != -1) {
            if (buffer.remaining() == 0) {
                flushBuffer();
            }

            totTransfered += read;
        }

        return totTransfered;
    }

    @Override
    public final void flush()
            throws IOException
    {
        if (closed) {
            throw new IOException("Stream is closed");
        }
        flushBuffer();
    }

    @Override
    public final void close()
            throws IOException
    {
        if (closed) {
            return;
        }
        try {
            flush();
            out.close();
        }
        finally {
            closed = true;
            bufferPool.releaseArray(buffer.array());
            bufferPool.releaseDirect(directInputBuffer);
            bufferPool.releaseDirect(outputBuffer);
        }
    }

    /**
     * Compresses and writes out any buffered data. This does nothing if there
     * is no currently buffered data.
     *
     * @throws IOException
     */
    private void flushBuffer()
            throws IOException
    {
        if (buffer.position() > 0) {
            buffer.flip();
            writeCompressed(buffer);
            buffer.clear();
            buffer.limit(blockSize);
        }
    }

    /**
     * {@link SnappyFramed#maskedCrc32c(byte[], int, int)} the crc, compresses
     * the data, determines if the compression ratio is acceptable and calls
     * {@link #writeBlock(java.nio.channels.WritableByteChannel, java.nio.ByteBuffer, boolean, int)} to
     * actually write the frame.
     *
     * @param buffer
     * @throws IOException
     */
    private void writeCompressed(ByteBuffer buffer)
            throws IOException
    {

        final byte[] input = buffer.array();
        final int length = buffer.remaining();

        // crc is based on the user supplied input data
        final int crc32c = maskedCrc32c(crc32, input, 0, length);

        directInputBuffer.clear();
        directInputBuffer.put(buffer);
        directInputBuffer.flip();

        outputBuffer.clear();
        Snappy.compress(directInputBuffer, outputBuffer);

        final int compressedLength = outputBuffer.remaining();

        // only use the compressed data if compression ratio is <= the
        // minCompressonRatio
        if (((double) compressedLength / (double) length) <= minCompressionRatio) {
            writeBlock(out, outputBuffer, true, crc32c);
        }
        else {
            // otherwise use the uncompressed data.
            buffer.flip();
            writeBlock(out, buffer, false, crc32c);
        }
    }

    /**
     * Write a frame (block) to <i>out</i>.
     *
     * @param out The {@link OutputStream} to write to.
     * @param data The data to write.
     * @param compressed Indicates if <i>data</i> is the compressed or raw content.
     * This is based on whether the compression ratio desired is
     * reached.
     * @param crc32c The calculated checksum.
     * @throws IOException
     */
    private void writeBlock(final WritableByteChannel out, ByteBuffer data,
            boolean compressed, int crc32c)
            throws IOException
    {

        headerBuffer.clear();
        headerBuffer.put((byte) (compressed ? COMPRESSED_DATA_FLAG
                : UNCOMPRESSED_DATA_FLAG));

        // the length written out to the header is both the checksum and the
        // frame
        final int headerLength = data.remaining() + 4;

        // write length
        headerBuffer.put((byte) headerLength);
        headerBuffer.put((byte) (headerLength >>> 8));
        headerBuffer.put((byte) (headerLength >>> 16));

        // write crc32c of user input data
        headerBuffer.putInt(crc32c);

        headerBuffer.flip();

        // write the header
        out.write(headerBuffer);
        // write the raw data
        out.write(data);
    }
}
