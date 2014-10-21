package org.xerial.snappy.buffer;

import java.lang.ref.SoftReference;
import java.util.*;

/**
 * Cached buffer
 */
public class CachedBufferAllocator implements BufferAllocator {

    public static BufferAllocatorFactory factory = new BufferAllocatorFactory() {
        @Override
        public BufferAllocator getBufferAllocator(int bufferSize) {
            return CachedBufferAllocator.getAllocator(bufferSize);
        }
    };

    /**
     * Use SoftReference so that having this queueTable does not prevent the GC of CachedBufferAllocator instances
     */
    public static Map<Integer, SoftReference<CachedBufferAllocator>> queueTable = new HashMap<Integer, SoftReference<CachedBufferAllocator>>();

    private final int bufferSize;
    private final Deque<byte[]> bufferQueue;

    public CachedBufferAllocator(int bufferSize) {
        this.bufferSize = bufferSize;
        this.bufferQueue = new ArrayDeque<byte[]>();
    }

    public static synchronized CachedBufferAllocator getAllocator(int bufferSize) {
        if(!queueTable.containsKey(bufferSize)) {
            queueTable.put(bufferSize, new SoftReference<CachedBufferAllocator>(new CachedBufferAllocator(bufferSize)));
        }
        return queueTable.get(bufferSize).get();
    }

    @Override
    public byte[] allocate(int size) {
        synchronized(this) {
            if(bufferQueue.isEmpty()) {
                return new byte[size];
            }
            else {
                return bufferQueue.pollFirst();
            }
        }
    }
    @Override
    public void release(byte[] buffer) {
        synchronized(this) {
            bufferQueue.addLast(buffer);
        }
    }
}
