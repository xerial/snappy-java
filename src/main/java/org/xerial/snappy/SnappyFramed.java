/*
 * Created: Apr 12, 2013
 */
package org.xerial.snappy;

import static java.lang.invoke.MethodHandles.constant;
import static java.lang.invoke.MethodHandles.dropArguments;
import static java.lang.invoke.MethodHandles.filterReturnValue;
import static java.lang.invoke.MethodHandles.guardWithTest;
import static java.lang.invoke.MethodHandles.lookup;
import static java.lang.invoke.MethodType.methodType;

import java.io.IOException;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles.Lookup;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;
import java.security.AccessController;
import java.security.PrivilegedExceptionAction;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Constants and utilities for implementing x-snappy-framed.
 *
 * @author Brett Okken
 * @since 1.1.0
 */
final class SnappyFramed
{
    public static final int COMPRESSED_DATA_FLAG = 0x00;

    public static final int UNCOMPRESSED_DATA_FLAG = 0x01;

    public static final int STREAM_IDENTIFIER_FLAG = 0xff;

    private static final int MASK_DELTA = 0xa282ead8;

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
                            Logger.getLogger(SnappyFramed.class.getName()).log(Level.FINE, "unable to use java 9 Unsafe.invokeCleaner", e);
                            
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
                            final MethodHandle nonNullTest = lookup.findStatic(SnappyFramed.class, "nonNull", methodType(boolean.class, Object.class)).asType(methodType(boolean.class, cleanerClass));
                            final MethodHandle noop = dropArguments(constant(Void.class, null).asType(methodType(void.class)), 0, cleanerClass);
                            handle = filterReturnValue(directBufferCleanerMethod, guardWithTest(nonNullTest, cleanMethod, noop)).asType(methodType(void.class, ByteBuffer.class));
                        }
                    }

                    return handle;
                }
            };

            cleanHandle = AccessController.doPrivileged(action);

        } catch (Throwable t) {
            Logger.getLogger(SnappyFramed.class.getName()).log(Level.FINE, "Exception occurred attempting to lookup Sun specific DirectByteBuffer cleaner classes.", t);
        }
        CLEAN_HANDLE = cleanHandle;
    }

    /**
     * The header consists of the stream identifier flag, 3 bytes indicating a
     * length of 6, and "sNaPpY" in ASCII.
     */
    public static final byte[] HEADER_BYTES = new byte[] {
            (byte) STREAM_IDENTIFIER_FLAG, 0x06, 0x00, 0x00, 0x73, 0x4e, 0x61,
            0x50, 0x70, 0x59};

    public static int maskedCrc32c(byte[] data)
    {
        return maskedCrc32c(data, 0, data.length);
    }

    public static int maskedCrc32c(byte[] data, int offset, int length)
    {
        final PureJavaCrc32C crc32c = new PureJavaCrc32C();
        crc32c.update(data, offset, length);
        return mask(crc32c.getIntegerValue());
    }

    /**
     * Checksums are not stored directly, but masked, as checksumming data and
     * then its own checksum can be problematic. The masking is the same as used
     * in Apache Hadoop: Rotate the checksum by 15 bits, then add the constant
     * 0xa282ead8 (using wraparound as normal for unsigned integers). This is
     * equivalent to the following C code:
     * <p/>
     * <pre>
     * uint32_t mask_checksum(uint32_t x) {
     *     return ((x >> 15) | (x << 17)) + 0xa282ead8;
     * }
     * </pre>
     */
    public static int mask(int crc)
    {
        // Rotate right by 15 bits and add a constant.
        return ((crc >>> 15) | (crc << 17)) + MASK_DELTA;
    }

    static final int readBytes(ReadableByteChannel source, ByteBuffer dest)
            throws IOException
    {
        // tells how many bytes to read.
        final int expectedLength = dest.remaining();

        int totalRead = 0;

        // how many bytes were read.
        int lastRead = source.read(dest);

        totalRead = lastRead;

        // if we did not read as many bytes as we had hoped, try reading again.
        if (lastRead < expectedLength) {
            // as long the buffer is not full (remaining() == 0) and we have not reached EOF (lastRead == -1) keep reading.
            while (dest.remaining() != 0 && lastRead != -1) {
                lastRead = source.read(dest);

                // if we got EOF, do not add to total read.
                if (lastRead != -1) {
                    totalRead += lastRead;
                }
            }
        }

        if (totalRead > 0) {
            dest.limit(dest.position());
        }
        else {
            dest.position(dest.limit());
        }

        return totalRead;
    }

    static int skip(final ReadableByteChannel source, final int skip, final ByteBuffer buffer)
            throws IOException
    {
        if (skip <= 0) {
            return 0;
        }

        int toSkip = skip;
        int skipped = 0;
        while (toSkip > 0 && skipped != -1) {
            buffer.clear();
            if (toSkip < buffer.capacity()) {
                buffer.limit(toSkip);
            }

            skipped = source.read(buffer);
            if (skipped > 0) {
                toSkip -= skipped;
            }
        }

        buffer.clear();
        return skip - toSkip;
    }

    private static Class<?> lookupClassQuietly(String name)
    {
        try {
            return SnappyFramed.class.getClassLoader().loadClass(name);
        }
        catch (Throwable t) {
            Logger.getLogger(SnappyFramed.class.getName()).log(Level.FINE, "Did not find requested class: " + name, t);
        }

        return null;
    }

    /**
     * Provides jvm implementation specific operation to aggressively release resources associated with <i>buffer</i>.
     *
     * @param buffer The {@code ByteBuffer} to release. Must not be {@code null}. Must be  {@link ByteBuffer#isDirect() direct}.
     */
    static void releaseDirectByteBuffer(final ByteBuffer buffer)
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
                Logger.getLogger(SnappyFramed.class.getName()).log(Level.FINE, "Exception occurred attempting to clean up Sun specific DirectByteBuffer.", t);
            }
        }
    }

    static boolean nonNull(Object o) {
        return o != null;
    }
}
