package org.xerial.snappy;

import java.io.OutputStream;

import org.xerial.snappy.buffer.CachedBufferAllocator;

public class SnappyHadoopCompatibleOutputStream extends SnappyOutputStream
{
    public SnappyHadoopCompatibleOutputStream(OutputStream out)
    {
        this(out, DEFAULT_BLOCK_SIZE);
    }

    public SnappyHadoopCompatibleOutputStream(OutputStream out, int blockSize)
    {
        super(out, blockSize, CachedBufferAllocator.getBufferAllocatorFactory());
    }

    @Override
    protected int writeHeader()
    {
        return 0;
    }

    @Override
    protected void writeBlockPreemble()
    {
        writeCurrentDataSize();
    }
}
