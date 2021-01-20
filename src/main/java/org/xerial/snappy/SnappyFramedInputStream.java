/*
 * Created: Apr 15, 2013
 */
package org.xerial.snappy;

import static java.lang.Math.min;
import static org.xerial.snappy.SnappyFramed.COMPRESSED_DATA_FLAG;
import static org.xerial.snappy.SnappyFramed.HEADER_BYTES;
import static org.xerial.snappy.SnappyFramed.STREAM_IDENTIFIER_FLAG;
import static org.xerial.snappy.SnappyFramed.UNCOMPRESSED_DATA_FLAG;
import static org.xerial.snappy.SnappyFramed.readBytes;
import static org.xerial.snappy.SnappyFramedOutputStream.MAX_BLOCK_SIZE;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.util.Arrays;
import java.util.zip.Checksum;

import org.xerial.snappy.pool.BufferPool;
import org.xerial.snappy.pool.DefaultPoolFactory;

/**
 * Implements the <a
 * href="http://snappy.googlecode.com/svn/trunk/framing_format.txt"
 * >x-snappy-framed</a> as an {@link InputStream} and
 * {@link ReadableByteChannel}.
 *
 * @author Brett Okken
 * @since 1.1.0
 */
public final class SnappyFramedInputStream
        extends InputStream
        implements
        ReadableByteChannel
{

    private final Checksum crc32 = SnappyFramed.getCRC32C();
    private final ReadableByteChannel rbc;
    private final ByteBuffer frameHeader;
    private final boolean verifyChecksums;
    private final BufferPool bufferPool;

    /**
     * A single frame read from the underlying {@link InputStream}.
     */
    private ByteBuffer input;

    /**
     * The decompressed data from {@link #input}.
     */
    private ByteBuffer uncompressedDirect;

    /**
     * Indicates if this instance has been closed.
     */
    private boolean closed;

    /**
     * Indicates if we have reached the EOF on {@link #input}.
     */
    private boolean eof;

    /**
     * The position in {@link #input} buffer to read to.
     */
    private int valid;

    /**
     * The next position to read from {@link #buffer}.
     */
    private int position;

    /**
     * Buffer contains a copy of the uncompressed data for the block.
     */
    private byte[] buffer;

    /**
     * Creates a Snappy input stream to read data from the specified underlying
     * input stream.
     * <p>
     * Uses {@link DefaultPoolFactory} to obtain {@link BufferPool} for buffers.
     * </p>
     * 
     * @param in the underlying input stream. Must not be {@code null}. 
     * @throws IOException
     */
    public SnappyFramedInputStream(InputStream in)
            throws IOException
    {
        this(in, true, DefaultPoolFactory.getDefaultPool());
    }

    /**
     * Creates a Snappy input stream to read data from the specified underlying
     * input stream.
     *
     * @param in the underlying input stream. Must not be {@code null}.
     * @param bufferPool Used to obtain buffer instances. Must not be {@code null}.
     * @throws IOException 
     */
    public SnappyFramedInputStream(InputStream in, BufferPool bufferPool)
            throws IOException
    {
        this(in, true, bufferPool);
    }

    /**
     * Creates a Snappy input stream to read data from the specified underlying
     * input stream.
     * <p>
     * Uses {@link DefaultPoolFactory} to obtain {@link BufferPool} for buffers.
     * </p>
     *
     * @param in the underlying input stream. Must not be {@code null}.
     * @param verifyChecksums if true, checksums in input stream will be verified
     * @throws IOException
     */
    public SnappyFramedInputStream(InputStream in, boolean verifyChecksums)
            throws IOException
    {
        this(in, verifyChecksums, DefaultPoolFactory.getDefaultPool());
    }

    /**
     * Creates a Snappy input stream to read data from the specified underlying
     * input stream.
     *
     * @param in the underlying input stream. Must not be {@code null}.
     * @param verifyChecksums if true, checksums in input stream will be verified
     * @param bufferPool Used to obtain buffer instances. Must not be {@code null}.
     * @throws IOException
     */
    public SnappyFramedInputStream(InputStream in, boolean verifyChecksums, 
            BufferPool bufferPool)
            throws IOException
    {
        this(Channels.newChannel(in), verifyChecksums, bufferPool);
    }

    /**
     * Creates a Snappy input stream to read data from the specified underlying
     * channel.
     *
     * @param in the underlying readable channel. Must not be {@code null}.
     * @param bufferPool Used to obtain buffer instances. Must not be {@code null}.
     * @throws IOException
     */
    public SnappyFramedInputStream(ReadableByteChannel in, BufferPool bufferPool)
            throws IOException
    {
        this(in, true, bufferPool);
    }

    /**
     * Creates a Snappy input stream to read data from the specified underlying
     * channel.
     * <p>
     * Uses {@link DefaultPoolFactory} to obtain {@link BufferPool} for buffers.
     * </p>
     *
     * @param in the underlying readable channel. Must not be {@code null}.
     * @throws IOException
     */
    public SnappyFramedInputStream(ReadableByteChannel in)
            throws IOException
    {
        this(in, true);
    }

    /**
     * Creates a Snappy input stream to read data from the specified underlying
     * channel.
     * <p>
     * Uses {@link DefaultPoolFactory} to obtain {@link BufferPool} for buffers.
     * </p>
     *
     * @param in the underlying readable channel. Must not be {@code null}.
     * @param verifyChecksums if true, checksums in input stream will be verified
     * @throws IOException
     */
    public SnappyFramedInputStream(ReadableByteChannel in,
            boolean verifyChecksums)
            throws IOException
    {
        this(in, verifyChecksums, DefaultPoolFactory.getDefaultPool());
    }

    /**
     * Creates a Snappy input stream to read data from the specified underlying
     * channel.
     *
     * @param in the underlying readable channel. Must not be {@code null}.
     * @param verifyChecksums if true, checksums in input stream will be verified
     * @param bufferPool Used to obtain buffer instances. Must not be {@code null}. 
     * @throws IOException
     */
    public SnappyFramedInputStream(ReadableByteChannel in,
            boolean verifyChecksums, BufferPool bufferPool)
            throws IOException
    {
        if (in == null) {
            throw new NullPointerException("in is null");
        }

        if (bufferPool == null) {
            throw new NullPointerException("bufferPool is null");
        }

        this.bufferPool = bufferPool;
        this.rbc = in;
        this.verifyChecksums = verifyChecksums;

        allocateBuffersBasedOnSize(MAX_BLOCK_SIZE + 5);
        this.frameHeader = ByteBuffer.allocate(4);

        // stream must begin with stream header
        final byte[] expectedHeader = HEADER_BYTES;
        final byte[] actualHeader = new byte[expectedHeader.length];
        final ByteBuffer actualBuffer = ByteBuffer.wrap(actualHeader);

        final int read = SnappyFramed.readBytes(in, actualBuffer);
        if (read < expectedHeader.length) {
            throw new EOFException(
                    "encountered EOF while reading stream header");
        }
        if (!Arrays.equals(expectedHeader, actualHeader)) {
            throw new IOException("invalid stream header");
        }
    }

    /**
     * @param size
     */
    private void allocateBuffersBasedOnSize(int size)
    {
        if (input != null) {
            bufferPool.releaseDirect(input);
        }

        if (uncompressedDirect != null) {
            bufferPool.releaseDirect(uncompressedDirect);
        }

        if (buffer != null) {
            bufferPool.releaseArray(buffer);
        }

        input = bufferPool.allocateDirect(size);
        final int maxCompressedLength = Snappy.maxCompressedLength(size);
        uncompressedDirect = bufferPool.allocateDirect(maxCompressedLength);
        buffer = bufferPool.allocateArray(maxCompressedLength);
    }

    @Override
    public int read()
            throws IOException
    {
        if (closed) {
            return -1;
        }
        if (!ensureBuffer()) {
            return -1;
        }
        return buffer[position++] & 0xFF;
    }

    @Override
    public int read(byte[] output, int offset, int length)
            throws IOException
    {

        if (output == null) {
            throw new IllegalArgumentException("output is null");
        }

        if (offset < 0 || length < 0 || offset + length > output.length) {
            throw new IllegalArgumentException("invalid offset [" + offset
                    + "] and length [" + length + ']');
        }

        if (closed) {
            throw new ClosedChannelException();
        }

        if (length == 0) {
            return 0;
        }
        if (!ensureBuffer()) {
            return -1;
        }

        final int size = min(length, available());
        System.arraycopy(buffer, position, output, offset, size);
        position += size;
        return size;
    }

    @Override
    public int available()
            throws IOException
    {
        if (closed) {
            return 0;
        }
        return valid - position;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isOpen()
    {
        return !closed;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int read(ByteBuffer dst)
            throws IOException
    {

        if (dst == null) {
            throw new IllegalArgumentException("dst is null");
        }

        if (closed) {
            throw new ClosedChannelException();
        }

        if (dst.remaining() == 0) {
            return 0;
        }
        if (!ensureBuffer()) {
            return -1;
        }

        final int size = min(dst.remaining(), available());
        dst.put(buffer, position, size);
        position += size;
        return size;
    }

    /**
     * Transfers the entire content of this {@link InputStream} to <i>os</i>.
     * This potentially limits the amount of buffering required to decompress
     * content.
     * <p>
     * Unlike {@link #read(byte[], int, int)}, this method does not need to be
     * called multiple times. A single call will transfer all available content.
     * Any calls after the source has been exhausted will result in a return
     * value of {@code 0}.
     * </p>
     *
     * @param os The destination to write decompressed content to.
     * @return The number of bytes transferred.
     * @throws IOException
     * @since 1.1.1
     */
    public long transferTo(OutputStream os)
            throws IOException
    {
        if (os == null) {
            throw new IllegalArgumentException("os is null");
        }

        if (closed) {
            throw new ClosedChannelException();
        }

        long totTransfered = 0;

        while (ensureBuffer()) {
            final int available = available();
            os.write(buffer, position, available);
            position += available;
            totTransfered += available;
        }

        return totTransfered;
    }

    /**
     * Transfers the entire content of this {@link ReadableByteChannel} to
     * <i>wbc</i>. This potentially limits the amount of buffering required to
     * decompress content.
     * <p/>
     * <p>
     * Unlike {@link #read(ByteBuffer)}, this method does not need to be called
     * multiple times. A single call will transfer all available content. Any
     * calls after the source has been exhausted will result in a return value
     * of {@code 0}.
     * </p>
     *
     * @param wbc The destination to write decompressed content to.
     * @return The number of bytes transferred.
     * @throws IOException
     * @since 1.1.1
     */
    public long transferTo(WritableByteChannel wbc)
            throws IOException
    {
        if (wbc == null) {
            throw new IllegalArgumentException("wbc is null");
        }

        if (closed) {
            throw new ClosedChannelException();
        }

        final ByteBuffer bb = ByteBuffer.wrap(buffer);

        long totTransfered = 0;

        while (ensureBuffer()) {
            bb.clear();
            bb.position(position);
            bb.limit(position + available());

            wbc.write(bb);

            final int written = bb.position() - position;
            position += written;

            totTransfered += written;
        }

        return totTransfered;
    }

    @Override
    public void close()
            throws IOException
    {
        try {
            rbc.close();
        }
        finally {
            if (!closed) {
                closed = true;

                if (input != null) {
                    bufferPool.releaseDirect(input);
                    input = null;
                }

                if (uncompressedDirect != null) {
                    bufferPool.releaseDirect(uncompressedDirect);
                    uncompressedDirect = null;
                }

                if (buffer != null) {
                    bufferPool.releaseArray(buffer);
                    buffer = null;
                }
            }
        }
    }

    static enum FrameAction
    {
        RAW, SKIP, UNCOMPRESS;
    }

    public static final class FrameMetaData
    {
        final int length;
        final FrameAction frameAction;

        /**
         * @param frameAction
         * @param length
         */
        public FrameMetaData(FrameAction frameAction, int length)
        {
            super();
            this.frameAction = frameAction;
            this.length = length;
        }
    }

    public static final class FrameData
    {
        final int checkSum;
        final int offset;

        /**
         * @param checkSum
         * @param offset
         */
        public FrameData(int checkSum, int offset)
        {
            super();
            this.checkSum = checkSum;
            this.offset = offset;
        }
    }

    private boolean ensureBuffer()
            throws IOException
    {
        if (available() > 0) {
            return true;
        }
        if (eof) {
            return false;
        }

        if (!readBlockHeader()) {
            eof = true;
            return false;
        }

        // get action based on header
        final FrameMetaData frameMetaData = getFrameMetaData(frameHeader);

        if (FrameAction.SKIP == frameMetaData.frameAction) {
            SnappyFramed.skip(rbc, frameMetaData.length,
                    ByteBuffer.wrap(buffer));
            return ensureBuffer();
        }

        if (frameMetaData.length > input.capacity()) {
            allocateBuffersBasedOnSize(frameMetaData.length);
        }

        input.clear();
        input.limit(frameMetaData.length);

        final int actualRead = readBytes(rbc, input);
        if (actualRead != frameMetaData.length) {
            throw new EOFException("unexpectd EOF when reading frame");
        }
        input.flip();

        final FrameData frameData = getFrameData(input);

        if (FrameAction.UNCOMPRESS == frameMetaData.frameAction) {

            input.position(frameData.offset);

            final int uncompressedLength = Snappy.uncompressedLength(input);

            if (uncompressedLength > uncompressedDirect.capacity()) {
                bufferPool.releaseDirect(uncompressedDirect);
                bufferPool.releaseArray(buffer);
                uncompressedDirect = bufferPool.allocateDirect(uncompressedLength);
                buffer = bufferPool.allocateArray(uncompressedLength);
            }

            uncompressedDirect.clear();

            this.valid = Snappy.uncompress(input, uncompressedDirect);

            uncompressedDirect.get(buffer, 0, valid);
            this.position = 0;
        }
        else {
            // we need to start reading at the offset
            input.position(frameData.offset);
            this.position = 0;
            this.valid = input.remaining();
            this.input.get(buffer, 0, input.remaining());
        }

        if (verifyChecksums) {
            final int actualCrc32c = SnappyFramed.maskedCrc32c(crc32, buffer,
                    position, valid - position);
            if (frameData.checkSum != actualCrc32c) {
                throw new IOException("Corrupt input: invalid checksum");
            }
        }

        return true;
    }

    private boolean readBlockHeader()
            throws IOException
    {
        frameHeader.clear();
        int read = readBytes(rbc, frameHeader);

        if (read == -1) {
            return false;
        }

        if (read < frameHeader.capacity()) {
            throw new EOFException("encountered EOF while reading block header");
        }
        frameHeader.flip();

        return true;
    }

    /**
     * @param frameHeader
     * @return
     * @throws IOException
     */
    private FrameMetaData getFrameMetaData(ByteBuffer frameHeader)
            throws IOException
    {

        assert frameHeader.hasArray();

        final byte[] frameHeaderArray = frameHeader.array();

        int length = (frameHeaderArray[1] & 0xFF);
        length |= (frameHeaderArray[2] & 0xFF) << 8;
        length |= (frameHeaderArray[3] & 0xFF) << 16;

        int minLength = 0;
        final FrameAction frameAction;
        final int flag = frameHeaderArray[0] & 0xFF;
        switch (flag) {
            case COMPRESSED_DATA_FLAG:
                frameAction = FrameAction.UNCOMPRESS;
                minLength = 5;
                break;
            case UNCOMPRESSED_DATA_FLAG:
                frameAction = FrameAction.RAW;
                minLength = 5;
                break;
            case STREAM_IDENTIFIER_FLAG:
                if (length != 6) {
                    throw new IOException(
                            "stream identifier chunk with invalid length: "
                                    + length);
                }
                frameAction = FrameAction.SKIP;
                minLength = 6;
                break;
            default:
                // Reserved unskippable chunks (chunk types 0x02-0x7f)
                if (flag <= 0x7f) {
                    throw new IOException("unsupported unskippable chunk: "
                            + Integer.toHexString(flag));
                }

                // all that is left is Reserved skippable chunks (chunk types
                // 0x80-0xfe)
                frameAction = FrameAction.SKIP;
                minLength = 0;
        }

        if (length < minLength) {
            throw new IOException("invalid length: " + length
                    + " for chunk flag: " + Integer.toHexString(flag));
        }

        return new FrameMetaData(frameAction, length);
    }

    /**
     * @param content
     * @return
     * @throws IOException
     */
    private FrameData getFrameData(ByteBuffer content)
            throws IOException
    {
        return new FrameData(getCrc32c(content), 4);
    }

    private int getCrc32c(ByteBuffer content)
    {

        final int position = content.position();

        return ((content.get(position + 3) & 0xFF) << 24)
                | ((content.get(position + 2) & 0xFF) << 16)
                | ((content.get(position + 1) & 0xFF) << 8)
                | (content.get(position) & 0xFF);
    }
}
