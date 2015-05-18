package org.xerial.snappy.buffer;

/**
 * Simple buffer allocator, which does not reuse the allocated buffer
 */
public class DefaultBufferAllocator
        implements BufferAllocator
{

    public static BufferAllocatorFactory factory = new BufferAllocatorFactory()
    {
        public BufferAllocator singleton = new DefaultBufferAllocator();

        @Override
        public BufferAllocator getBufferAllocator(int bufferSize)
        {
            return singleton;
        }
    };

    @Override
    public byte[] allocate(int size)
    {
        return new byte[size];
    }

    @Override
    public void release(byte[] buffer)
    {
        // do nothing
    }
}
