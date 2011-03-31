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
    private int                 blockSize = SnappyOutputStream.DEFAULT_BLOCK_SIZE;

    private byte[]              compressed;
    private byte[]              uncompressed;

    public SnappyInputStream(InputStream input) throws IOException {
        this.in = input;
        readHeader();
    }

    protected void readHeader() throws IOException {
        byte[] header = new byte[SnappyOutputStream.PREAMBLE_SIZE];
        int readBytes = in.read(header);
        if (readBytes != header.length) {
            throw new IOException("Invalid Snappy stream");
        }
        String headerStr = new String(header, 0, SnappyOutputStream.HEADER_SIZE, "UTF-8");
        if (!headerStr.startsWith(SnappyOutputStream.STREAM_FORMAT_VERSION)) {
            throw new IOException("Incompatible stream version");
        }
        blockSize = SnappyOutputStream.readInt(header, SnappyOutputStream.HEADER_SIZE);
        if (blockSize < 0) {
            throw new IOException("Invalid block size: " + blockSize);
        }

        compressed = new byte[blockSize];
        uncompressed = new byte[blockSize];

    }

    @Override
    public int read() throws IOException {
        // TODO Auto-generated method stub
        return 0;
    }

}
