/*
 * Created: Apr 15, 2013
 */
package org.xerial.snappy;

import static java.lang.Math.min;
import static org.xerial.snappy.SnappyFramed.*;
import static org.xerial.snappy.SnappyFramedOutputStream.*;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.ReadableByteChannel;
import java.util.Arrays;

/**
 * Implements the <a
 * href="http://snappy.googlecode.com/svn/trunk/framing_format.txt"
 * >x-snappy-framed</a> as an {@link InputStream} and
 * {@link ReadableByteChannel}.
 * 
 * @author Brett Okken
 * @since 1.1.0
 */
public final class SnappyFramedInputStream extends InputStream implements
        ReadableByteChannel {

    private final ReadableByteChannel rbc;
    private final ByteBuffer frameHeader;
    private final boolean verifyChecksums;

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
     * Indicates if we have reached the EOF on {@link #in}.
     */
    private boolean eof;

    /**
     * The position in {@link buffer} to read to.
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
     * 
     * @param in
     *            the underlying input stream. Must not be {@code null}.
     */
    public SnappyFramedInputStream(InputStream in) throws IOException {
        this(in, true);
    }

    /**
     * Creates a Snappy input stream to read data from the specified underlying
     * input stream.
     * 
     * @param in
     *            the underlying input stream. Must not be {@code null}.
     * @param verifyChecksums
     *            if true, checksums in input stream will be verified
     */
    public SnappyFramedInputStream(InputStream in, boolean verifyChecksums)
            throws IOException {
        this(Channels.newChannel(in), verifyChecksums);
    }

    /**
     * Creates a Snappy input stream to read data from the specified underlying
     * channel.
     * 
     * @param in
     *            the underlying readable channel. Must not be {@code null}.
     */
    public SnappyFramedInputStream(ReadableByteChannel in)
            throws IOException {
        this(in, true);
    }

    /**
     * Creates a Snappy input stream to read data from the specified underlying
     * channel.
     * 
     * @param in
     *            the underlying readable channel. Must not be {@code null}.
     * @param verifyChecksums
     *            if true, checksums in input stream will be verified
     */
    public SnappyFramedInputStream(ReadableByteChannel in,
            boolean verifyChecksums) throws IOException {
        if (in == null) {
            throw new NullPointerException("in is null");
        }

        this.rbc = in;
        this.verifyChecksums = verifyChecksums;

        allocateBuffersBasedOnSize(MAX_BLOCK_SIZE + 5);
        this.frameHeader = ByteBuffer.allocate(4);

        // stream must begin with stream header
        final byte[] expectedHeader = HEADER_BYTES;
        final byte[] actualHeader = new byte[expectedHeader.length];
        final ByteBuffer actualBuffer = ByteBuffer.wrap(actualHeader);

        // assume that if the input cannot read 4 bytes that something is
        // wrong.
        final int read = in.read(actualBuffer);
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
    private void allocateBuffersBasedOnSize(int size) {

        input = ByteBuffer.allocateDirect(size);
        final int maxCompressedLength = Snappy.maxCompressedLength(size);
        uncompressedDirect = ByteBuffer.allocateDirect(maxCompressedLength);
        buffer = new byte[maxCompressedLength];
    }

    @Override
    public int read() throws IOException {
        if (closed) {
            return -1;
        }
        if (!ensureBuffer()) {
            return -1;
        }
        return buffer[position++] & 0xFF;
    }

    @Override
    public int read(byte[] output, int offset, int length) throws IOException {

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
    public int available() throws IOException {
        if (closed) {
            return 0;
        }
        return valid - position;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isOpen() {
        return !closed;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int read(ByteBuffer dst) throws IOException {

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

    @Override
    public void close() throws IOException {
        try {
            rbc.close();
        } finally {
            if (!closed) {
                closed = true;
            }
        }
    }

    static enum FrameAction {
        RAW, SKIP, UNCOMPRESS;
    }

    public static final class FrameMetaData {
        final int length;
        final FrameAction frameAction;

        /**
         * @param frameAction
         * @param length
         */
        public FrameMetaData(FrameAction frameAction, int length) {
            super();
            this.frameAction = frameAction;
            this.length = length;
        }
    }

    public static final class FrameData {
        final int checkSum;
        final int offset;

        /**
         * @param checkSum
         * @param offset
         */
        public FrameData(int checkSum, int offset) {
            super();
            this.checkSum = checkSum;
            this.offset = offset;
        }
    }

    private boolean ensureBuffer() throws IOException {
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
                uncompressedDirect = ByteBuffer
                        .allocateDirect(uncompressedLength);
                buffer = new byte[Math.max(input.capacity(), uncompressedLength)];
            }
            
            uncompressedDirect.clear();

            this.valid = Snappy.uncompress(input, uncompressedDirect);

            uncompressedDirect.get(buffer, 0, valid);
            this.position = 0;
        } else {
            // we need to start reading at the offset
            input.position(frameData.offset);
            this.position = 0;
            this.valid = input.remaining();
            this.input.get(buffer, 0, input.remaining());
        }

        if (verifyChecksums) {
            final int actualCrc32c = SnappyFramed.maskedCrc32c(buffer,
                    position, valid - position);
            if (frameData.checkSum != actualCrc32c) {
                throw new IOException("Corrupt input: invalid checksum");
            }
        }

        return true;
    }

    private boolean readBlockHeader() throws IOException {
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
     * 
     * @param frameHeader
     * @return
     * @throws IOException
     */
    private FrameMetaData getFrameMetaData(ByteBuffer frameHeader)
            throws IOException {

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
     * 
     * @param content
     * @return
     * @throws IOException
     */
    private FrameData getFrameData(ByteBuffer content) throws IOException {
        return new FrameData(getCrc32c(content), 4);
    }

    private int getCrc32c(ByteBuffer content) {

        final int position = content.position();

        return ((content.get(position + 3) & 0xFF) << 24)
                | ((content.get(position + 2) & 0xFF) << 16)
                | ((content.get(position + 1) & 0xFF) << 8)
                | (content.get(position) & 0xFF);
    }
}
