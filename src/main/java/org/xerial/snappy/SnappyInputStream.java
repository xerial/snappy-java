/*--------------------------------------------------------------------------
 *  Copyright 2011 Taro L. Saito
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *--------------------------------------------------------------------------*/
//--------------------------------------
// XerialJ
//
// SnappyInputStream.java
// Since: 2011/03/31 20:14:56
//
// $URL$
// $Author$
//--------------------------------------
package org.xerial.snappy;

import java.io.IOException;
import java.io.InputStream;

/**
 * A stream filter for reading data compressed by {@link SnappyOutputStream}.
 * 
 * @author leo
 * 
 */
public class SnappyInputStream extends InputStream
{
    protected final InputStream in;
    private boolean             finishedReading    = false;
    private int                 blockSize          = SnappyOutputStream.DEFAULT_BLOCK_SIZE;

    private byte[]              compressed         = new byte[blockSize];

    private byte[]              uncompressed       = new byte[blockSize];
    private int                 uncompressedCursor = 0;
    private int                 uncompressedLimit  = 0;

    private byte[]              chunkSizeBuf       = new byte[4];

    public SnappyInputStream(InputStream input) throws IOException {
        this.in = input;
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        int wroteBytes = 0;
        for (; wroteBytes < len;) {
            if (uncompressedCursor >= uncompressedLimit) {
                if (hasNextChunk())
                    continue;
                else {
                    return wroteBytes == 0 ? -1 : wroteBytes;
                }
            }
            int bytesToWrite = Math.min(uncompressedLimit - uncompressedCursor, len);
            System.arraycopy(uncompressed, uncompressedCursor, b, off + wroteBytes, bytesToWrite);
            wroteBytes += bytesToWrite;
            uncompressedCursor += bytesToWrite;
        }

        return wroteBytes;
    }

    protected boolean hasNextChunk() throws IOException {
        if (finishedReading)
            return false;

        uncompressedCursor = 0;
        uncompressedLimit = 0;

        int chunkSizeDataLen = in.read(chunkSizeBuf, 0, 4);
        if (chunkSizeDataLen < 4) {
            finishedReading = true;
            return false;
        }
        int chunkSize = SnappyOutputStream.readInt(chunkSizeBuf, 0);
        // extend the compressed data buffer size
        if (chunkSize > compressed.length) {
            compressed = new byte[chunkSize];
        }
        int readBytes = in.read(compressed, 0, chunkSize);
        if (readBytes < chunkSize) {
            throw new IOException("failed to read chunk");
        }
        try {
            int uncompressedLength = Snappy.uncompressedLength(compressed, 0, chunkSize);
            if (uncompressedLength > uncompressed.length) {
                uncompressed = new byte[uncompressedLength];
            }
            int actualUncompressedLength = Snappy.uncompress(compressed, 0, chunkSize, uncompressed, 0);
            if (uncompressedLength != actualUncompressedLength) {
                throw new IOException("invalid uncompressed byte size");
            }
            uncompressedLimit = actualUncompressedLength;
        }
        catch (SnappyException e) {
            throw new IOException("failed to uncompress the chunk: " + e.getMessage());
        }

        return true;
    }

    @Override
    public int read() throws IOException {
        byte[] buf = new byte[1];
        return read(buf, 0, 1);
    }

}
