package org.xerial.snappy;

import java.io.OutputStream;

import org.xerial.snappy.buffer.BufferAllocatorFactory;
import org.xerial.snappy.buffer.CachedBufferAllocator;

public class SnappyHadoopCompatibleOutputStream extends SnappyOutputStream
{
	public SnappyHadoopCompatibleOutputStream(OutputStream out)
	{
		this(out, DEFAULT_BLOCK_SIZE);
	}

	public SnappyHadoopCompatibleOutputStream(OutputStream out, int blockSize)
	{
		this(out, blockSize, CachedBufferAllocator.getBufferAllocatorFactory());
	}

	public SnappyHadoopCompatibleOutputStream(OutputStream out, int blockSize,
			BufferAllocatorFactory bufferAllocatorFactory)
	{
		super(out, blockSize, bufferAllocatorFactory, false); // do not write header
	}

	@Override
	protected void writeBlockPreemble()
	{
		writeCurrentDataSize();
	}
}
