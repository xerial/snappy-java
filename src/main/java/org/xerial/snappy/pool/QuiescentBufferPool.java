package org.xerial.snappy.pool;

import java.nio.ByteBuffer;

/**
 * A {@link BufferPool} implementation which does no pooling. New instances will be created for each call to allocate.
 * @author Brett Okken
 */
public final class QuiescentBufferPool implements BufferPool {

    private static final QuiescentBufferPool INSTANCE = new QuiescentBufferPool();

    private QuiescentBufferPool() {
    }

    /**
     * @return Instance of {@link BufferPool} which does no caching/reuse of instances.
     */
    public static BufferPool getInstance() {
        return INSTANCE;
    }
    
    /**
     * Creates a new {@code byte[]} of <i>size</i>.
     */
    @Override
    public byte[] allocateArray(int size) {
        return new byte[size];
    }

    /**
     * Does nothing.
     */
    @Override
    public void releaseArray(byte[] buffer) {
    }

    /**
     * {@link ByteBuffer#allocateDirect(int) Allocates} a direct {@link ByteBuffer} of <i>size</i>.
     */
    @Override
    public ByteBuffer allocateDirect(int size) {
        return ByteBuffer.allocateDirect(size);
    }

    /**
     * Aggressively releases native resources associated with <i>buffer</i>.
     */
    @Override
    public void releaseDirect(ByteBuffer buffer) {
        assert buffer != null && buffer.isDirect();
        DirectByteBuffers.releaseDirectByteBuffer(buffer);
    }

}
