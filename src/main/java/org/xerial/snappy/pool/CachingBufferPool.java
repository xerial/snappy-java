package org.xerial.snappy.pool;

import java.lang.ref.SoftReference;
import java.nio.ByteBuffer;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ConcurrentMap;

/**
 * A {@link BufferPool} implementation which caches values at fixed sizes.
 * <p>
 * Pooled instances are held as {@link SoftReference} to allow GC if necessary.
 * </p>
 * <p>
 * The current fixed sizes are calculated as follows:
 * <ul>
 * <li>Values < 4KB return 4KB</li>
 * <li>4KB - 32KB to 2KB</li>
 * <li>32KB - 512KB  to 16KB</li>
 * <li>512KB - 2MB to 128KB</li>
 * <li>2MB - 16MB to 512KB</li>
 * <li>16MB - 128MB to 4MB</li>
 * <li>128MB - 512MB to 16MB</li>
 * <li>512MB - 1.5 GB to 128MB</li>
 * <li>Values > 1.5GB return {@link Integer#MAX_VALUE}</li>
 * </ul>
 * </p>
 * @author Brett Okken
 */
public final class CachingBufferPool implements BufferPool {

    private static interface IntFunction<E> {
        public E create(int size);
    }

    private static final IntFunction<byte[]> ARRAY_FUNCTION = new IntFunction<byte[]>() {
        @Override
        public byte[] create(int size) {
            return new byte[size];
        }
    };

    private static final IntFunction<ByteBuffer> DBB_FUNCTION = new IntFunction<ByteBuffer>() {
        @Override
        public ByteBuffer create(int size) {
            return ByteBuffer.allocateDirect(size);
        }
    };

    private static final CachingBufferPool INSTANCE = new CachingBufferPool();

    private final ConcurrentMap<Integer, ConcurrentLinkedDeque<SoftReference<byte[]>>> bytes = new ConcurrentHashMap<>();
    private final ConcurrentMap<Integer, ConcurrentLinkedDeque<SoftReference<ByteBuffer>>> buffers = new ConcurrentHashMap<>();

    private CachingBufferPool() {
    }

    /**
     * Returns instance of {@link CachingBufferPool} for using cached buffers.
     * @return instance of {@link CachingBufferPool} for using cached buffers.
     */
    public static BufferPool getInstance() {
        return INSTANCE;
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public byte[] allocateArray(int size) {
        if (size <= 0) {
            throw new IllegalArgumentException("size is invalid: " + size);
        }

        return getOrCreate(size, bytes, ARRAY_FUNCTION);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void releaseArray(byte[] buffer) {
        if (buffer == null) {
            throw new IllegalArgumentException("buffer is null");
        }
        returnValue(buffer, buffer.length, bytes);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ByteBuffer allocateDirect(int size) {
        if (size <= 0) {
            throw new IllegalArgumentException("size is invalid: " + size);
        }

        return getOrCreate(size, buffers, DBB_FUNCTION);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void releaseDirect(ByteBuffer buffer) {
        if (buffer == null) {
            throw new IllegalArgumentException("buffer is null");
        }
        buffer.clear();
        returnValue(buffer, buffer.capacity(), buffers);
    }

    private static <E> E getOrCreate(final int size, final ConcurrentMap<Integer, ConcurrentLinkedDeque<SoftReference<E>>> map, final IntFunction<E> creator) {
        assert size > 0;
        final int adjustedSize = adjustSize(size);
        final ConcurrentLinkedDeque<SoftReference<E>> queue = optimisticGetEntry(adjustedSize, map);
        SoftReference<E> entry;
        while ((entry = queue.pollFirst()) != null) {
            final E val = entry.get();
            if (val != null) {
                return val;
            }
        }

        return creator.create(adjustedSize);
    }

    /*
     * This is package scope to allow direct unit testing.
     */
    static int adjustSize(int size) {
        assert size > 0;

        switch (Integer.numberOfLeadingZeros(size)) {
            case 1:  // 1GB - 2GB
            case 2:  // 512MB 
                //if 512MB - 1.5 GB round to nearest 128 MB (2^27), else Integer.MAX_VALUE
                return size <= 0x6000_0000 ? roundToPowers(size, 27) : Integer.MAX_VALUE; 
            case 3:  //256MB 
            case 4:  //128MB
                //if 128MB - 512MB, round to nearest 16 MB
                return roundToPowers(size, 24);
            case 5:  // 64MB
            case 6:  // 32MB
            case 7:  // 16MB
                //if 16MB - 128MB, round to nearest 4MB
                return roundToPowers(size, 22);
            case 8:  //  8MB
            case 9:  //  4MB
            case 10: //  2MB
                //if 2MB - 16MB, round to nearest 512KB
                return roundToPowers(size, 19);
            case 11: //  1MB
            case 12: //512KB
                //if 512KB - 2MB, round to nearest 128KB
                return roundToPowers(size, 17);
            case 13: //256KB
            case 14: //128KB
            case 15: // 64KB
            case 16: // 32KB
                //if 32KB to 512KB, round to nearest 16KB
                return roundToPowers(size, 14);
            case 17: // 16KB
            case 18: //  8KB
            case 19: //  4KB
                // if 4KB - 32KB, round to nearest 2KB 
                return roundToPowers(size, 11);
            default:
                return 4 * 1024;
        }
    }

    private static int roundToPowers(int number, int bits) {
        final int mask = (0x7FFF_FFFF >> bits) << bits;
        final int floor = number & mask;
        return floor == number ? number : floor + (1 << bits);
    }

    private static <E> ConcurrentLinkedDeque<SoftReference<E>> optimisticGetEntry(Integer key, ConcurrentMap<Integer, ConcurrentLinkedDeque<SoftReference<E>>> map) {
        ConcurrentLinkedDeque<SoftReference<E>> val = map.get(key);
        if (val == null) {
            map.putIfAbsent(key, new ConcurrentLinkedDeque<SoftReference<E>>());
            val = map.get(key);
        }
        return val;
    }

    private static <E> void returnValue(E value, Integer size, ConcurrentMap<Integer, ConcurrentLinkedDeque<SoftReference<E>>> map) {
        final ConcurrentLinkedDeque<SoftReference<E>> queue = map.get(size);
        //no queue will exist if buffer was not originally obtained from this class
        if (queue != null) {
            //push this value onto deque first so that concurrent request can use it
            queue.addFirst(new SoftReference<E>(value));

            //purge oldest entries have lost references
            SoftReference<E> entry;
            boolean lastEmpty = true;
            while(lastEmpty && (entry = queue.peekLast()) != null) {
                if (entry.get() == null) {
                    queue.removeLastOccurrence(entry);
                } else {
                    lastEmpty = false;
                }
            }
        }   
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return "CachingBufferPool [bytes=" + this.bytes + ", buffers=" + this.buffers + "]";
    }
}

