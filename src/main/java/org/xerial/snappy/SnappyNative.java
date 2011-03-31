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
// snappy-java Project
//
// SnappyNative.java
// Since: 2011/03/30
//
// $URL$ 
// $Author$
//--------------------------------------
package org.xerial.snappy;

import java.nio.ByteBuffer;

/**
 * Interface to access the native code of Snappy
 * 
 * @author leo
 * 
 */
public class SnappyNative
{
    static {
        LoadSnappy.load();
    }

    public native static String nativeLibraryVersion();

    // ------------------------------------------------------------------------
    // Generic compression/decompression routines.
    // ------------------------------------------------------------------------
    public native static int rawCompress(ByteBuffer input, int inputOffset, int inputLength, ByteBuffer compressed,
            int outputOffset) throws SnappyException;

    public native static int rawCompress(byte[] input, int inputOffset, int inputLength, byte[] output, int outputOffset)
            throws SnappyException;

    public native static int rawUncompress(ByteBuffer compressed, int inputOffset, int inputLength,
            ByteBuffer uncompressed, int outputOffset) throws SnappyException;

    public native static int rawUncompress(byte[] input, int inputOffset, int inputLength, byte[] output,
            int outputOffset) throws SnappyException;

    // Returns the maximal size of the compressed representation of
    // input data that is "source_bytes" bytes in length;
    public native static int maxCompressedLength(int source_bytes);

    // This operation takes O(1) time.
    public native static int uncompressedLength(ByteBuffer compressed, int offset, int len) throws SnappyException;

    public native static int uncompressedLength(byte[] input, int offset, int len) throws SnappyException;

    public native static boolean isValidCompressedBuffer(ByteBuffer compressed, int offset, int len)
            throws SnappyException;

    public native static boolean isValidCompressedBuffer(byte[] input, int offset, int len) throws SnappyException;

    public static void throw_error(int errorCode) throws SnappyException {
        throw new SnappyException(errorCode);
    }

}
