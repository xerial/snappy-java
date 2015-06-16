package org.xerial.snappy.buffer;

import java.lang.ref.SoftReference;
import java.util.*;

/**
 * Cached buffer
 */
public class CachedBufferAllocator
        implements BufferAllocator
{
    private static BufferAllocatorFactory factory = new BufferAllocatorFactory()
    {
        @Override
        public BufferAllocator getBufferAllocator(int bufferSize)
        {
            return CachedBufferAllocator.getAllocator(bufferSize);
        }
    };

    public static void setBufferAllocatorFactory(BufferAllocatorFactory factory)
    {
        assert (factory != null);
        CachedBufferAllocator.factory = factory;
    }

    public static BufferAllocatorFactory getBufferAllocatorFactory()
    {
        return factory;
    }

    /**
     * Use SoftReference so that having this queueTable does not prevent the GC of CachedBufferAllocator instances
     */
    private static final Map<Integer, SoftReference<CachedBufferAllocator>> queueTable = new HashMap<Integer, SoftReference<CachedBufferAllocator>>();

    private final int bufferSize;
    private final Deque<byte[]> bufferQueue;

    public CachedBufferAllocator(int bufferSize)
    {
        this.bufferSize = bufferSize;
        this.bufferQueue = new ArrayDeque<byte[]>();
    }

    public static synchronized CachedBufferAllocator getAllocator(int bufferSize)
    {
        CachedBufferAllocator result = null;

        if (queueTable.containsKey(bufferSize)) {
            result = queueTable.get(bufferSize).get();
        }
        if (result == null) {
            result = new CachedBufferAllocator(bufferSize);
            queueTable.put(bufferSize, new SoftReference<CachedBufferAllocator>(result));
        }
        return result;
    }

    @Override
    public byte[] allocate(int size)
    {
        synchronized (this) {
            if (bufferQueue.isEmpty()) {
                return new byte[size];
            }
            else {
                return bufferQueue.pollFirst();
            }
        }
    }

    @Override
    public void release(byte[] buffer)
    {
        synchronized (this) {
            bufferQueue.addLast(buffer);
        }
    }
}
