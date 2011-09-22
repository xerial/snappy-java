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

import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * <b>Internal only - Do not use this class.</b>
 * 
 * Interface to access the native code of Snappy. Although this class members
 * are public, do not use them directly. Use {@link Snappy} API instead.
 * 
 * 
 * @author leo
 * 
 */
public interface SnappyNativeAPI
{

    public String nativeLibraryVersion();

    // ------------------------------------------------------------------------
    // Generic compression/decompression routines.
    // ------------------------------------------------------------------------
    public int rawCompress(ByteBuffer input, int inputOffset, int inputLength, ByteBuffer compressed, int outputOffset)
            throws IOException;

    public int rawCompress(Object input, int inputOffset, int inputByteLength, Object output, int outputOffset);

    public int rawUncompress(ByteBuffer compressed, int inputOffset, int inputLength, ByteBuffer uncompressed,
            int outputOffset) throws IOException;

    public int rawUncompress(Object input, int inputOffset, int inputLength, Object output, int outputOffset)
            throws IOException;

    // Returns the maximal size of the compressed representation of
    // input data that is "source_bytes" bytes in length;
    public int maxCompressedLength(int source_bytes);

    // This operation takes O(1) time.
    public int uncompressedLength(ByteBuffer compressed, int offset, int len) throws IOException;

    public int uncompressedLength(Object input, int offset, int len) throws IOException;

    public boolean isValidCompressedBuffer(ByteBuffer compressed, int offset, int len) throws IOException;

    public boolean isValidCompressedBuffer(Object input, int offset, int len) throws IOException;

    public void arrayCopy(Object src, int offset, int byteLength, Object dest, int dOffset) throws IOException;

    public void throw_error(int errorCode) throws IOException;

}
