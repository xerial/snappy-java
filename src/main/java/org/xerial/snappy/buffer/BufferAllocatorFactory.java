package org.xerial.snappy.buffer;

/**
 *
 */
public interface BufferAllocatorFactory
{

    BufferAllocator getBufferAllocator(int minSize);
}

