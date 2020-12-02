package org.xerial.snappy.pure;

import static org.xerial.snappy.pure.UnsafeUtil.getAddress;
import static sun.misc.Unsafe.ARRAY_BYTE_BASE_OFFSET;

import java.io.IOException;
import java.lang.ref.SoftReference;
import java.nio.ByteBuffer;
import java.util.concurrent.ConcurrentLinkedDeque;

import org.xerial.snappy.SnappyApi;

/**
 * A pure-java Snappy implementation using https://github.com/airlift/aircompressor
 */
public class PureJavaSnappy implements SnappyApi
{
    /**
     * Using a {@link ConcurrentLinkedDeque}, with values constantly popped and pushed from the head, leads to the fewest
     * {@code short[]} instances remaining live over time.
     */
    private final static ConcurrentLinkedDeque<SoftReference<short[]>> CACHED_TABLES = new ConcurrentLinkedDeque<>();

    private final static int MAX_OUTPUT_LENGTH = Integer.MAX_VALUE;

    @Override
    public long rawCompress(long inputAddr, long inputSize, long destAddr)
            throws IOException
    {
        final short[] table = getTable();
        try
        {
            return SnappyRawCompressor.compress(null, inputAddr, inputSize, null, destAddr, MAX_OUTPUT_LENGTH, table);
        }
        finally
        {
            returnTable(table);
        }
    }

    @Override
    public long rawUncompress(long inputAddr, long inputSize, long destAddr)
            throws IOException
    {
        return SnappyRawDecompressor.decompress(null, inputAddr, inputSize, null, destAddr, MAX_OUTPUT_LENGTH);
    }

    @Override
    public int rawCompress(ByteBuffer input, int inputOffset, int inputLength, ByteBuffer compressed, int outputOffset)
            throws IOException
    {
        Object inputBase;
        long inputAddress;
        long inputLimit;
        if (input.isDirect()) {
            inputBase = null;
            long address = getAddress(input);
            inputAddress = address + input.position();
            inputLimit = address + input.limit();
        }
        else if (input.hasArray()) {
            inputBase = input.array();
            inputAddress = ARRAY_BYTE_BASE_OFFSET + input.arrayOffset() + input.position();
            inputLimit = ARRAY_BYTE_BASE_OFFSET + input.arrayOffset() + input.limit();
        }
        else {
            throw new IllegalArgumentException("Unsupported input ByteBuffer implementation " + input.getClass().getName());
        }

        Object outputBase;
        long outputAddress;
        long outputLimit;
        if (compressed.isDirect()) {
            outputBase = null;
            long address = getAddress(compressed);
            outputAddress = address + compressed.position();
            outputLimit = address + compressed.limit();
        }
        else if (compressed.hasArray()) {
            outputBase = compressed.array();
            outputAddress = ARRAY_BYTE_BASE_OFFSET + compressed.arrayOffset() + compressed.position();
            outputLimit = ARRAY_BYTE_BASE_OFFSET + compressed.arrayOffset() + compressed.limit();
        }
        else {
            throw new IllegalArgumentException("Unsupported output ByteBuffer implementation " + compressed.getClass().getName());
        }

        // HACK: Assure JVM does not collect Slice wrappers while compressing, since the
        // collection may trigger freeing of the underlying memory resulting in a segfault
        // There is no other known way to signal to the JVM that an object should not be
        // collected in a block, and technically, the JVM is allowed to eliminate these locks.
        synchronized (input) {
            synchronized (compressed) {
                final short[] table = getTable();
                try
                {
                    int written = SnappyRawCompressor.compress(
                            inputBase,
                            inputAddress,
                            inputLimit,
                            outputBase,
                            outputAddress,
                            outputLimit,
                            table);
                    compressed.position(compressed.position() + written);
                    return written;
                }
                finally
                {
                    returnTable(table);
                }
            }
        }
    }

    @Override
    public int rawCompress(Object input, int inputOffset, int inputByteLength, Object output, int outputOffset)
            throws IOException
    {
        long inputAddress = ARRAY_BYTE_BASE_OFFSET + inputOffset;
        long inputLimit = inputAddress + inputByteLength;
        long outputAddress = ARRAY_BYTE_BASE_OFFSET + outputOffset;
        long outputLimit = outputAddress + MAX_OUTPUT_LENGTH;

        final short[] table = getTable();
        try
        {
            return SnappyRawCompressor.compress(input, inputAddress, inputLimit, output, outputAddress, outputLimit, table);
        }
        finally
        {
            returnTable(table);
        }   
    }

    @Override
    public int rawUncompress(ByteBuffer compressed, int inputOffset, int inputLength, ByteBuffer uncompressed, int outputOffset)
            throws IOException
    {
        Object inputBase;
        long inputAddress;
        long inputLimit;
        if (compressed.isDirect()) {
            inputBase = null;
            long address = getAddress(compressed);
            inputAddress = address + compressed.position();
            inputLimit = address + compressed.limit();
        }
        else if (compressed.hasArray()) {
            inputBase = compressed.array();
            inputAddress = ARRAY_BYTE_BASE_OFFSET + compressed.arrayOffset() + compressed.position();
            inputLimit = ARRAY_BYTE_BASE_OFFSET + compressed.arrayOffset() + compressed.limit();
        }
        else {
            throw new IllegalArgumentException("Unsupported input ByteBuffer implementation " + compressed.getClass().getName());
        }

        Object outputBase;
        long outputAddress;
        long outputLimit;
        if (uncompressed.isDirect()) {
            outputBase = null;
            long address = getAddress(uncompressed);
            outputAddress = address + uncompressed.position();
            outputLimit = address + uncompressed.limit();
        }
        else if (uncompressed.hasArray()) {
            outputBase = uncompressed.array();
            outputAddress = ARRAY_BYTE_BASE_OFFSET + uncompressed.arrayOffset() + uncompressed.position();
            outputLimit = ARRAY_BYTE_BASE_OFFSET + uncompressed.arrayOffset() + uncompressed.limit();
        }
        else {
            throw new IllegalArgumentException("Unsupported output ByteBuffer implementation " + uncompressed.getClass().getName());
        }

        // HACK: Assure JVM does not collect Slice wrappers while decompressing, since the
        // collection may trigger freeing of the underlying memory resulting in a segfault
        // There is no other known way to signal to the JVM that an object should not be
        // collected in a block, and technically, the JVM is allowed to eliminate these locks.
        synchronized (compressed) {
            synchronized (uncompressed) {
                int written = SnappyRawDecompressor.decompress(inputBase, inputAddress, inputLimit, outputBase, outputAddress, outputLimit);
                uncompressed.position(uncompressed.position() + written);
                return written;
            }
        }
    }

    @Override
    public int rawUncompress(Object input, int inputOffset, int inputLength, Object output, int outputOffset)
            throws IOException
    {
        long inputAddress = ARRAY_BYTE_BASE_OFFSET + inputOffset;
        long inputLimit = inputAddress + inputLength;
        long outputAddress = ARRAY_BYTE_BASE_OFFSET + outputOffset;
        long outputLimit = outputAddress + MAX_OUTPUT_LENGTH;

        return SnappyRawDecompressor.decompress(input, inputAddress, inputLimit, output, outputAddress, outputLimit);
    }

    @Override
    public int maxCompressedLength(int source_bytes)
    {
        return SnappyRawCompressor.maxCompressedLength(source_bytes);
    }

    @Override
    public int uncompressedLength(ByteBuffer compressed, int offset, int len)
            throws IOException
    {
        Object inputBase;
        long inputAddress;
        long inputLimit;
        if(compressed.isDirect()) {
            inputBase = null;
            long address = getAddress(compressed);
            inputAddress = address + compressed.position();
            inputLimit = address + compressed.limit();
        }
        else if (compressed.hasArray()){
            inputBase = compressed.array();
            inputAddress = ARRAY_BYTE_BASE_OFFSET + offset;
            inputLimit = ARRAY_BYTE_BASE_OFFSET + len;
        }
        else {
            throw new IllegalArgumentException("Unsupported input ByteBuffer implementation: " + compressed.getClass().getName());
        }
        return SnappyRawDecompressor.getUncompressedLength(inputBase, inputAddress, inputLimit);
    }

    @Override
    public int uncompressedLength(Object input, int offset, int len)
            throws IOException
    {
        long compressedAddress = ARRAY_BYTE_BASE_OFFSET + offset;
        long compressedLimit = ARRAY_BYTE_BASE_OFFSET + len;

        return SnappyRawDecompressor.getUncompressedLength(input, compressedAddress, compressedLimit);
    }

    @Override
    public long uncompressedLength(long inputAddr, long len)
            throws IOException
    {
        return SnappyRawDecompressor.getUncompressedLength(null, inputAddr, inputAddr + len);
    }

    @Override
    public boolean isValidCompressedBuffer(ByteBuffer compressed, int offset, int len)
            throws IOException
    {
        throw new UnsupportedOperationException("isValidCompressedBuffer is not supported in pure-java mode");
    }

    @Override
    public boolean isValidCompressedBuffer(Object input, int offset, int len)
            throws IOException
    {
        throw new UnsupportedOperationException("isValidCompressedBuffer is not supported in pure-java mode");
    }

    @Override
    public boolean isValidCompressedBuffer(long inputAddr, long offset, long len)
            throws IOException
    {
        throw new UnsupportedOperationException("isValidCompressedBuffer is not supported in pure-java mode");
    }

    @Override
    public void arrayCopy(Object src, int offset, int byteLength, Object dest, int dOffset)
            throws IOException
    {
        System.arraycopy(src, offset, dest, dOffset, byteLength);
    }

    private static short[] getTable()
    {
        SoftReference<short[]> existingRef;
        while((existingRef = CACHED_TABLES.poll()) != null)
        {
            short[] table = existingRef.get();
            if (table != null)
            {
                //purge oldest entries have lost references
                SoftReference<short[]> entry;
                boolean lastEmpty = true;
                while (lastEmpty && (entry = CACHED_TABLES.peekLast()) != null)
                {
                    if (entry.get() == null)
                    {
                        CACHED_TABLES.removeLastOccurrence(entry);
                    }
                    else
                    {
                        lastEmpty = false;
                    }
                }

                return table;
            }
        }
        return new short[SnappyRawCompressor.MAX_HASH_TABLE_SIZE];
    }

    private static void returnTable(short[] table)
    {
        CACHED_TABLES.addFirst(new SoftReference<short[]>(table));
    }
}
