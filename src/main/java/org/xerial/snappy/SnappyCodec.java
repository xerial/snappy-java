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
 * 
 * <p>
 * The magic header is the following 8 bytes data:
 * 
 * <pre>
 * -126, 'S', 'N', 'A', 'P', 'P', 'Y', 0
 * </pre>
 * 
 * </p>
 * 
 * @author leo
 * 
 */
public class SnappyCodec
{
    public static final byte[] MAGIC_HEADER               = new byte[] { -126, 'S', 'N', 'A', 'P', 'P', 'Y', 0 };
    public static final int    MAGIC_LEN                  = 8;

    public static final int    DEFAULT_VERSION            = 1;
    public static final int    MINIMUM_COMPATIBLE_VERSION = 1;

    public final byte[]        magic;
    public final int           version;
    public final int           compatibleVersion;

    private SnappyCodec(byte[] magic, int version, int compatibleVersion) {
        this.magic = magic;
        this.version = version;
        this.compatibleVersion = compatibleVersion;
    }

    @Override
    public String toString() {
        return String.format("version:%d, compatible version:%d", version, compatibleVersion);
    }

    public static int headerSize() {
        return MAGIC_LEN + 4 * 2;
    }

    public void writeHeader(OutputStream out) throws IOException {
        ByteArrayOutputStream header = new ByteArrayOutputStream();
        DataOutputStream d = new DataOutputStream(header);
        d.write(magic, 0, MAGIC_LEN);
        d.writeInt(version);
        d.writeInt(compatibleVersion);
        d.close();
        out.write(header.toByteArray(), 0, header.size());
    }

    public boolean isValidMagicHeader() {
        return Arrays.equals(MAGIC_HEADER, magic);
    }

    public static SnappyCodec readHeader(InputStream in) throws IOException {
        DataInputStream d = new DataInputStream(in);
        byte[] magic = new byte[MAGIC_LEN];
        d.read(magic, 0, MAGIC_LEN);
        int version = d.readInt();
        int compatibleVersion = d.readInt();
        return new SnappyCodec(magic, version, compatibleVersion);
    }

    public static SnappyCodec currentHeader() {
        return new SnappyCodec(MAGIC_HEADER, DEFAULT_VERSION, MINIMUM_COMPATIBLE_VERSION);
    }

}
