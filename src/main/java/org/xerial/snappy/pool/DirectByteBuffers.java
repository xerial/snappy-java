package org.xerial.snappy.pool;

import static java.lang.invoke.MethodHandles.constant;
import static java.lang.invoke.MethodHandles.dropArguments;
import static java.lang.invoke.MethodHandles.filterReturnValue;
import static java.lang.invoke.MethodHandles.guardWithTest;
import static java.lang.invoke.MethodHandles.lookup;
import static java.lang.invoke.MethodType.methodType;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles.Lookup;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.security.AccessController;
import java.security.PrivilegedExceptionAction;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Utility to facilitate disposing of direct byte buffer instances.
 */
final class DirectByteBuffers {

    /**
     * Sun specific mechanisms to clean up resources associated with direct byte buffers.
     */
    @SuppressWarnings("unchecked")
    static final Class<? extends ByteBuffer> DIRECT_BUFFER_CLAZZ = (Class<? extends ByteBuffer>) lookupClassQuietly("java.nio.DirectByteBuffer");

    static final MethodHandle CLEAN_HANDLE;
    
    static {
        // this approach is based off that used by apache lucene and documented here: https://issues.apache.org/jira/browse/LUCENE-6989
        // and https://github.com/apache/lucene-solr/blob/7e03427fa14a024ce257babcb8362d2451941e21/lucene/core/src/java/org/apache/lucene/store/MMapDirectory.java
        MethodHandle cleanHandle = null;
        try {
            final PrivilegedExceptionAction<MethodHandle> action = new PrivilegedExceptionAction<MethodHandle>() {

                @Override
                public MethodHandle run() throws Exception {
                    MethodHandle handle = null;
                    if (DIRECT_BUFFER_CLAZZ != null) {
                        final Lookup lookup = lookup();

                        try {
                            // sun.misc.Unsafe unmapping (Java 9+)
                            final Class<?> unsafeClass = Class.forName("sun.misc.Unsafe");
                            // first check if Unsafe has the right method, otherwise we can give up
                            // without doing any security critical stuff:
                            final MethodHandle unmapper = lookup.findVirtual(unsafeClass, "invokeCleaner", methodType(void.class, ByteBuffer.class));
                            // fetch the unsafe instance and bind it to the virtual MH:
                            final Field f = unsafeClass.getDeclaredField("theUnsafe");
                            f.setAccessible(true);
                            final Object theUnsafe = f.get(null);
                            handle = unmapper.bindTo(theUnsafe);
                        } catch (Exception e) {
                            Logger.getLogger(DirectByteBuffers.class.getName()).log(Level.FINE, "unable to use java 9 Unsafe.invokeCleaner", e);
                            
                            // sun.misc.Cleaner unmapping (Java 8 and older)
                            final Method m = DIRECT_BUFFER_CLAZZ.getMethod("cleaner");
                            m.setAccessible(true);
                            final MethodHandle directBufferCleanerMethod = lookup.unreflect(m);
                            final Class<?> cleanerClass = directBufferCleanerMethod.type().returnType();

                            /*
                             * "Compile" a MethodHandle that basically is equivalent to the following code: 
                             *  void unmapper(ByteBuffer byteBuffer) 
                             *  { 
                             *      sun.misc.Cleaner cleaner = ((java.nio.DirectByteBuffer) byteBuffer).cleaner(); 
                             *      if (nonNull(cleaner)) 
                             *      { 
                             *          cleaner.clean(); 
                             *      } 
                             *      else 
                             *      { 
                             *          // the noop is needed because MethodHandles#guardWithTest always needs ELSE
                             *          noop(cleaner);  
                             *      } 
                             *  }
                             */
                            final MethodHandle cleanMethod = lookup.findVirtual(cleanerClass, "clean", methodType(void.class));
                            final MethodHandle nonNullTest = lookup.findStatic(DirectByteBuffers.class, "nonNull", methodType(boolean.class, Object.class)).asType(methodType(boolean.class, cleanerClass));
                            final MethodHandle noop = dropArguments(constant(Void.class, null).asType(methodType(void.class)), 0, cleanerClass);
                            handle = filterReturnValue(directBufferCleanerMethod, guardWithTest(nonNullTest, cleanMethod, noop)).asType(methodType(void.class, ByteBuffer.class));
                        }
                    }

                    return handle;
                }
            };

            cleanHandle = AccessController.doPrivileged(action);

        } catch (Throwable t) {
            Logger.getLogger(DirectByteBuffers.class.getName()).log(Level.FINE, "Exception occurred attempting to lookup Sun specific DirectByteBuffer cleaner classes.", t);
        }
        CLEAN_HANDLE = cleanHandle;
    }


    private static Class<?> lookupClassQuietly(String name)
    {
        try {
            return DirectByteBuffers.class.getClassLoader().loadClass(name);
        }
        catch (Throwable t) {
            Logger.getLogger(DirectByteBuffers.class.getName()).log(Level.FINE, "Did not find requested class: " + name, t);
        }

        return null;
    }


    static boolean nonNull(Object o) {
        return o != null;
    }

    /**
     * Provides jvm implementation specific operation to aggressively release resources associated with <i>buffer</i>.
     *
     * @param buffer The {@code ByteBuffer} to release. Must not be {@code null}. Must be  {@link ByteBuffer#isDirect() direct}.
     */
    public static void releaseDirectByteBuffer(final ByteBuffer buffer)
    {
        assert buffer != null && buffer.isDirect();

        if (CLEAN_HANDLE != null && DIRECT_BUFFER_CLAZZ.isInstance(buffer)) {
            try {
                final PrivilegedExceptionAction<Void> pea = new PrivilegedExceptionAction<Void>() {
                    @Override
                    public Void run() throws Exception {
                        try {
                            CLEAN_HANDLE.invokeExact(buffer);
                        } catch (Exception e) {
                            throw e;
                        } catch (Throwable t) {
                            //this will be an error
                            throw new RuntimeException(t);
                        }
                        return null;
                    }
                };
                AccessController.doPrivileged(pea);
            } catch (Throwable t) {
                Logger.getLogger(DirectByteBuffers.class.getName()).log(Level.FINE, "Exception occurred attempting to clean up Sun specific DirectByteBuffer.", t);
            }
        }
    }
}
