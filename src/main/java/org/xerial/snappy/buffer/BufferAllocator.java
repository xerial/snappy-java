package org.xerial.snappy.buffer;

/**
 * BufferAllocator interface. The implementation of this interface must be thread-safe
 */
public interface BufferAllocator
{

    public byte[] allocate(int size);

    public void release(byte[] buffer);
}
