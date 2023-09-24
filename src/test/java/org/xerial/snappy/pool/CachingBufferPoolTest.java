package org.xerial.snappy.pool;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.lang.ref.Reference;
import java.lang.ref.WeakReference;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import org.junit.Test;


public class CachingBufferPoolTest {

    private static final int LIST_COUNT = 2048;

    @Test
    public void testAdjustSize() {
        assertEquals(4 * 1024, CachingBufferPool.adjustSize(2));
        assertEquals(4 * 1024, CachingBufferPool.adjustSize(1023));
        assertEquals(4 * 1024, CachingBufferPool.adjustSize(1024));
        assertEquals(4 * 1024, CachingBufferPool.adjustSize(1025));
        assertEquals(4 * 1024, CachingBufferPool.adjustSize(4 * 1024));
        assertEquals((4 + 2) * 1024, CachingBufferPool.adjustSize((4 * 1024) + 1));
        assertEquals(6 * 1024, CachingBufferPool.adjustSize(5 * 1024));
        assertEquals(6 * 1024, CachingBufferPool.adjustSize((5 * 1024) + 1));

        assertEquals(32 * 1024, CachingBufferPool.adjustSize(32 * 1024));
        assertEquals((32 + 16) * 1024, CachingBufferPool.adjustSize((32 * 1024) + 1));

        assertEquals(2 * 1024 * 1024, CachingBufferPool.adjustSize(2 * 1024 * 1024));
        assertEquals(((2 * 1024) + 512) * 1024, CachingBufferPool.adjustSize((2 * 1024 * 1024) + 1));

        assertEquals(16 * 1024 * 1024, CachingBufferPool.adjustSize(16 * 1024 * 1024));
        assertEquals((16 + 4) * 1024 * 1024, CachingBufferPool.adjustSize((16 * 1024 * 1024) + 1));

        assertEquals(128 * 1024 * 1024, CachingBufferPool.adjustSize(128 * 1024 * 1024));
        assertEquals((128 + 16) * 1024 * 1024, CachingBufferPool.adjustSize((128 * 1024 * 1024) + 1));

        assertEquals(512 * 1024 * 1024, CachingBufferPool.adjustSize(512 * 1024 * 1024));
        assertEquals((512 + 128) * 1024 * 1024, CachingBufferPool.adjustSize((512 * 1024 * 1024) + 1));
        assertEquals(0x6000_0000, CachingBufferPool.adjustSize(0x6000_0000));
        assertEquals(0x6000_0000, CachingBufferPool.adjustSize(0x6000_0000 - 1));
        assertEquals(Integer.MAX_VALUE, CachingBufferPool.adjustSize(0x6000_0001));
        assertEquals(Integer.MAX_VALUE, CachingBufferPool.adjustSize(Integer.MAX_VALUE));
        assertEquals(Integer.MAX_VALUE, CachingBufferPool.adjustSize(Integer.MAX_VALUE - 1));
    }

    @Test
    public void testDirectByteBuffers() throws Exception {

        BufferPool pool = CachingBufferPool.getInstance();

        ByteBuffer bb1 = pool.allocateDirect(12 * 1024);
        assertNotNull(bb1);
        assertEquals(12 * 1024, bb1.limit());
        assertEquals(12 * 1024, bb1.capacity());
        assertEquals(0, bb1.position());

        ByteBuffer bb2 = pool.allocateDirect(12 * 1024);
        assertNotNull(bb2);
        assertEquals(12 * 1024, bb2.limit());
        assertEquals(12 * 1024, bb2.capacity());
        assertEquals(0, bb2.position());

        assertNotSame(bb1, bb2);

        bb2.position(18);
        pool.releaseDirect(bb2);

        ByteBuffer bb3 = pool.allocateDirect(12 * 1024);
        assertNotNull(bb3);
        assertEquals(12 * 1024, bb3.limit());
        assertEquals(12 * 1024, bb3.capacity());
        assertEquals(0, bb3.position());

        assertNotSame(bb1, bb2);
        assertSame(bb2, bb3);

        pool.releaseDirect(bb1);

        ByteBuffer bb4 = pool.allocateDirect((12 * 1024) - 1);
        assertNotNull(bb4);
        assertEquals(12 * 1024, bb4.limit());
        assertEquals(12 * 1024, bb4.capacity());
        assertEquals(0, bb4.position());

        assertSame(bb1, bb4);
    }

    @Test
    public void testArrays() throws Exception {

        BufferPool pool = CachingBufferPool.getInstance();

        byte[] bb1 = pool.allocateArray(12 * 1024);
        assertNotNull(bb1);
        assertEquals(12 * 1024, bb1.length);

        byte[] bb2 = pool.allocateArray(12 * 1024);
        assertNotNull(bb2);
        assertEquals(12 * 1024, bb2.length);

        assertNotSame(bb1, bb2);

        pool.releaseArray(bb2);

        byte[] bb3 = pool.allocateArray(12 * 1024);
        assertNotNull(bb3);
        assertEquals(12 * 1024, bb3.length);

        assertNotSame(bb1, bb2);
        assertSame(bb2, bb3);

        pool.releaseArray(bb1);

        byte[] bb4 = pool.allocateArray((12 * 1024) - 1);
        assertNotNull(bb4);
        assertEquals(12 * 1024, bb4.length);

        assertSame(bb1, bb4);
    }

    @Test
    public void testSoftReferences() {

        BufferPool pool = CachingBufferPool.getInstance();
        byte[] bb1 = pool.allocateArray(8 * 1024);
        Reference<byte[]> ref = new WeakReference<byte[]>(bb1);
        bb1[0] = 123;
        bb1[8000] = -74;
        int bb1HC = System.identityHashCode(bb1);

        pool.releaseArray(bb1);

        byte[] bb1_copy = pool.allocateArray(8 * 1024);
        assertSame(bb1, bb1_copy);
        assertEquals(123, bb1_copy[0]);
        assertEquals(-74, bb1_copy[8000]);
        assertEquals(bb1HC, System.identityHashCode(bb1_copy));

        //release back into pool (again)
        pool.releaseArray(bb1);

        //release strong references
        bb1_copy = null;
        bb1 = null;
        assertNotNull(ref.get());

        //force an OOME to for SoftReferences to be collected
        try {
            List<byte[]> vals = forceOOMEGC(LIST_COUNT);
            assertTrue("count: " + vals.size(), vals.size() < LIST_COUNT);
        } catch (OutOfMemoryError e) {
            //
        }

        //assert that our test reference has been cleared
        assertNull(ref.get());

        //get another value from the pool
        byte[] bb2 = pool.allocateArray(8 * 1024);
        //assert that it is indeed a new value, and not same from previous
        assertNotEquals(123, bb2[0]);
        assertNotEquals(-74, bb2[8000]);
        assertNotEquals(bb1HC, System.identityHashCode(bb2));
    }

    private static List<byte[]> forceOOMEGC(int count) {
        final List<byte[]> vals = new ArrayList<>(count);

        try {
            for (int i = 0; i < count; ++i) {
                vals.add(new byte[10 * 1024 * 1024]);
            }
        } catch (Error e) {

        }
        return vals;
    }
}
