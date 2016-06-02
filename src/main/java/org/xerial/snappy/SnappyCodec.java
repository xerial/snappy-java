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
// SnappyCodec.java
// Since: 2011/04/03 14:50:20
//
// $URL$
// $Author$
//--------------------------------------
package org.xerial.snappy;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;

/**
 * Preamble header for {@link SnappyOutputStream}.
 * <p/>
 * <p>
 * The magic header is the following 8 bytes data:
 * <p/>
 * <pre>
 * -126, 'S', 'N', 'A', 'P', 'P', 'Y', 0
 * </pre>
 * <p/>
 * </p>
 *
 * @author leo
 */
public class SnappyCodec
{
    static final byte[] MAGIC_HEADER = new byte[] {-126, 'S', 'N', 'A', 'P', 'P', 'Y', 0};
    public static final int MAGIC_LEN = MAGIC_HEADER.length;
    public static final int HEADER_SIZE = MAGIC_LEN + 8;
    public static final int MAGIC_HEADER_HEAD = SnappyOutputStream.readInt(MAGIC_HEADER, 0);

    static {
        assert (MAGIC_HEADER_HEAD < 0);
    }

    public static final int DEFAULT_VERSION = 1;
    public static final int MINIMUM_COMPATIBLE_VERSION = 1;
    public static final SnappyCodec currentHeader = new SnappyCodec(MAGIC_HEADER, DEFAULT_VERSION, MINIMUM_COMPATIBLE_VERSION);

    public final byte[] magic;
    public final int version;
    public final int compatibleVersion;
    private final byte[] headerArray;

    private SnappyCodec(byte[] magic, int version, int compatibleVersion)
    {
        this.magic = magic;
        this.version = version;
        this.compatibleVersion = compatibleVersion;

        ByteArrayOutputStream header = new ByteArrayOutputStream(HEADER_SIZE);
        DataOutputStream d = new DataOutputStream(header);
        try {
            d.write(magic, 0, MAGIC_LEN);
            d.writeInt(version);
            d.writeInt(compatibleVersion);
            d.close();
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }
        headerArray = header.toByteArray();
    }

    public static byte[] getMagicHeader()
    {
        return MAGIC_HEADER.clone();
    }

    @Override
    public String toString()
    {
        return String.format("version:%d, compatible version:%d", version, compatibleVersion);
    }

    public static int headerSize()
    {
        return HEADER_SIZE;
    }

    public int writeHeader(byte[] dst, int dstOffset)
    {
        System.arraycopy(headerArray, 0, dst, dstOffset, headerArray.length);
        return headerArray.length;
    }

    public int writeHeader(OutputStream out)
            throws IOException
    {
        out.write(headerArray, 0, headerArray.length);
        return headerArray.length;
    }

    public boolean isValidMagicHeader()
    {
        return Arrays.equals(MAGIC_HEADER, magic);
    }

    public static boolean hasMagicHeaderPrefix(byte[] b) {
        int limit = Math.min(MAGIC_LEN, b.length);
        int i = 0;
        while(i < limit) {
            if(b[i] != MAGIC_HEADER[i]) {
                return false;
            }
            ++i;
        }
        return true;
    }

    public static SnappyCodec readHeader(InputStream in)
            throws IOException
    {
        DataInputStream d = new DataInputStream(in);
        byte[] magic = new byte[MAGIC_LEN];
        d.readFully(magic, 0, MAGIC_LEN);
        int version = d.readInt();
        int compatibleVersion = d.readInt();
        return new SnappyCodec(magic, version, compatibleVersion);
    }
}

