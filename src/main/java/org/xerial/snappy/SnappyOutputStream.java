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
// SnappyOutputStream.java
// Since: 2011/03/31 17:44:10
//
// $URL$
// $Author$
//--------------------------------------
package org.xerial.snappy;

import java.io.IOException;
import java.io.OutputStream;

/**
 * This class implements a stream filter for writing compressed data using
 * Snappy.
 * 
 * The input data is blocked into 32KB size, and each block is compressed and
 * then passed to the given {@link OutputStream}.
 * 
 * @author leo
 * 
 */
public class SnappyOutputStream extends OutputStream
{
    protected final OutputStream out;

    private int                  cursor     = 0;
    protected byte[]             uncompressed;
    protected byte[]             compressed;

    protected final int          BLOCK_SIZE = 1 << 15; // use 2^15 = 32KB as block size

    public SnappyOutputStream(OutputStream out) {
        this.out = out;
        uncompressed = new byte[BLOCK_SIZE];
        compressed = new byte[Snappy.maxCompressedLength(BLOCK_SIZE)];
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException {

        for (int readBytes = 0; readBytes < len;) {
            int copyLen = Math.min(uncompressed.length - cursor, len - readBytes);
            System.arraycopy(b, off + readBytes, uncompressed, cursor, copyLen);
            readBytes += copyLen;
            cursor += copyLen;

            if (cursor >= uncompressed.length) {
                dump();
            }
        }
    }

    @Override
    public void write(int b) throws IOException {
        if (cursor >= uncompressed.length) {
            dump();
        }
        uncompressed[cursor++] = (byte) b;
    }

    @Override
    public void flush() throws IOException {
        dump();
        out.flush();
    }

    protected void dump() throws IOException {
        if (cursor <= 0)
            return; // no need to dump

        // Compress and dump the buffer content
        try {
            int compressedSize = Snappy.compress(uncompressed, 0, cursor, compressed, 0);
            out.write(compressed, 0, compressedSize);
            cursor = 0;
        }
        catch (SnappyException e) {
            throw new IOException(e);
        }
    }

    @Override
    public void close() throws IOException {
        flush();

        super.close();
        out.close();
    }

}
