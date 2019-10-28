package org.xerial.snappy.pool;

import java.nio.ByteBuffer;

/**
 * Makes various types of buffers available for use and potential re-use.
 * 
 * <p>
 * Implementations must be safe for concurrent use by multiple threads.
 * </p>
 *
 * @author Brett Okken
 */
public interface BufferPool {
    
    /**
     * Returns a {@code byte[]} of <i>size</i> or greater length.
     * @param size The minimum size array required. Must be {@code >= 0}.
     * @return A {@code byte[]} with length of at least <i>size</i>.
     * @see #releaseArray(byte[])
     */
    public byte[] allocateArray(int size);

    /**
     * Returns instance to pool for potential future reuse.
     * <p>
     * Must not be returned more than 1 time. Must not be used by caller after return.
     * </p>
     * @param buffer Instance to return to pool. Must not be {@code null}.
     * Must not be returned more than 1 time. Must not be used by caller after return.
     */
    public void releaseArray(byte[] buffer);

    /**
     * Returns a {@link ByteBuffer#allocateDirect(int) direct ByteBuffer} of <i>size</i> or
     * greater {@link ByteBuffer#capacity() capacity}.
     * @param size The minimum size buffer required. Must be {@code >= 0}.
     * @return A {@code ByteBuffer} of <i>size</i> or greater {@link ByteBuffer#capacity() capacity}.
     * @see #releaseDirect(ByteBuffer)
     * @see ByteBuffer#allocateDirect(int)
     */
    public ByteBuffer allocateDirect(int size);

    /**
     * Returns instance to pool for potential future reuse.
     * <p>
     * Must not be returned more than 1 time. Must not be used by caller after return.
     * </p>
     * @param buffer Instance to return to pool. Must not be {@code null}.
     * Must not be returned more than 1 time. Must not be used by caller after return.
     */
    public void releaseDirect(ByteBuffer buffer);
}
