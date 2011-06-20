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

import java.io.ByteArrayInputStream;
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
    private boolean             finishedReading    = false;
    protected final InputStream in;

    private byte[]              compressed;
    private byte[]              uncompressed;
    private int                 uncompressedCursor = 0;
    private int                 uncompressedLimit  = 0;

    private byte[]              chunkSizeBuf       = new byte[4];

    public SnappyInputStream(InputStream input) throws IOException {
        this.in = input;
        readHeader();
    }

    protected void readHeader() throws IOException {
        byte[] header = new byte[SnappyCodec.headerSize()];
        int readBytes = in.read(header, 0, header.length);

        // Quick test of the header 
        if (header[0] != SnappyCodec.MAGIC_HEADER[0]) {
            // do the default uncompression
            readFully(header, readBytes);
            return;
        }

        SnappyCodec codec = SnappyCodec.readHeader(new ByteArrayInputStream(header));
        if (codec.isValidMagicHeader()) {
            // The input data is compressed by SnappyOutputStream
            if (codec.version < SnappyCodec.MINIMUM_COMPATIBLE_VERSION) {
                throw new IOException(String.format(
                        "compressed with imcompatible codec version %d. At least version %d is required",
                        codec.version, SnappyCodec.MINIMUM_COMPATIBLE_VERSION));
            }
        }
        else {
            // (probably) compressed by Snappy.compress(byte[])
            readFully(header, readBytes);
            return;
        }
    }

    protected void readFully(byte[] fragment, int fragmentLength) throws IOException {
        // read the entire input data to the buffer 
        compressed = new byte[Math.max(8 * 1024, fragmentLength)]; // 8K
        System.arraycopy(fragment, 0, compressed, 0, fragmentLength);
        int cursor = fragmentLength;
        for (int readBytes = 0; (readBytes = in.read(compressed, cursor, compressed.length - cursor)) != -1;) {
            cursor += readBytes;
            if (cursor >= compressed.length) {
                byte[] newBuf = new byte[(compressed.length * 2)];
                System.arraycopy(compressed, 0, newBuf, 0, compressed.length);
                compressed = newBuf;
            }
        }

        finishedReading = true;

        // Uncompress
        try {
            int uncompressedLength = Snappy.uncompressedLength(compressed, 0, cursor);
            uncompressed = new byte[uncompressedLength];
            Snappy.uncompress(compressed, 0, cursor, uncompressed, 0);
            this.uncompressedCursor = 0;
            this.uncompressedLimit = uncompressedLength;
        }
        catch (SnappyException e) {
            throw new IOException(e.getMessage());
        }

    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        int writtenBytes = 0;
        for (; writtenBytes < len;) {
            if (uncompressedCursor >= uncompressedLimit) {
                if (hasNextChunk())
                    continue;
                else {
                    return writtenBytes == 0 ? -1 : writtenBytes;
                }
            }
            int bytesToWrite = Math.min(uncompressedLimit - uncompressedCursor, len - writtenBytes);
            System.arraycopy(uncompressed, uncompressedCursor, b, off + writtenBytes, bytesToWrite);
            writtenBytes += bytesToWrite;
            uncompressedCursor += bytesToWrite;
        }

        return writtenBytes;
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
        if (compressed == null || chunkSize > compressed.length) {
            compressed = new byte[chunkSize];
        }
        int readBytes = in.read(compressed, 0, chunkSize);
        if (readBytes < chunkSize) {
            throw new IOException("failed to read chunk");
        }
        try {
            int uncompressedLength = Snappy.uncompressedLength(compressed, 0, chunkSize);
            if (uncompressed == null || uncompressedLength > uncompressed.length) {
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
        if (uncompressedCursor < uncompressedLimit) {
            return uncompressed[uncompressedCursor++] & 0xFF;
        }
        else {
            if (hasNextChunk())
                return read();
            else
                return -1;
        }
    }

}
