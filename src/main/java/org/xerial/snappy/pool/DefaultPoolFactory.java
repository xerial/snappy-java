package org.xerial.snappy.pool;

/**
 * Manages implementation of {@link BufferPool} to use by default. Setting the system property {@link #DISABLE_CACHING_PROPERTY} to {@code true} will
 * cause the {@link QuiescentBufferPool} to be used by default. Otherwise, {@link CachingBufferPool} will be used by default.
 * {@link #setDefaultPool(BufferPool)} can be used to explicitly control the implementation to use.
 */
public final class DefaultPoolFactory {

    /**
     * Name of system property to disable use of {@link CachingBufferPool} by default.
     */
    public static final String DISABLE_CACHING_PROPERTY = "org.xerial.snappy.pool.disable";

    private static volatile BufferPool defaultPool = "true".equalsIgnoreCase(System.getProperty(DISABLE_CACHING_PROPERTY)) 
                                                         ? QuiescentBufferPool.getInstance() 
                                                         : CachingBufferPool.getInstance();

    /**
     * @return The default instance to use.
     */
    public static BufferPool getDefaultPool() {
        return defaultPool;
    }

    /**
     * Sets the default instance to use.
     * @param pool The default instance to use. Must not be {@code null}.
     * @see #getDefaultPool()
     */
    public static void setDefaultPool(BufferPool pool) {
        if (pool == null) {
            throw new IllegalArgumentException("pool is null");
        }
        defaultPool = pool;
    }
}
