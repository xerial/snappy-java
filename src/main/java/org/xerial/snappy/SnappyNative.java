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
 * <b>Internal only - Do not use this class.</b> JNI interface of the
 * {@link SnappyNativeAPI} implementation. The native method in this class is
 * defined in SnappyNative.h (genereted by javah) and SnappyNative.cpp
 * 
 * <p>
 * <b> DO NOT USE THIS CLASS since the direct use of this class might break the
 * native library code loading in {@link SnappyLoader}. </b>
 * </p>
 * 
 * @author leo
 * 
 */
public class SnappyNative implements SnappyNativeAPI
{

    public native String nativeLibraryVersion();

    // ------------------------------------------------------------------------
    // Generic compression/decompression routines.
    // ------------------------------------------------------------------------
    public native int rawCompress(ByteBuffer input, int inputOffset, int inputLength, ByteBuffer compressed,
            int outputOffset) throws IOException;

    public native int rawCompress(Object input, int inputOffset, int inputByteLength, Object output, int outputOffset);

    public native int rawUncompress(ByteBuffer compressed, int inputOffset, int inputLength, ByteBuffer uncompressed,
            int outputOffset) throws IOException;

    public native int rawUncompress(Object input, int inputOffset, int inputLength, Object output, int outputOffset)
            throws IOException;

    // Returns the maximal size of the compressed representation of
    // input data that is "source_bytes" bytes in length;
    public native int maxCompressedLength(int source_bytes);

    // This operation takes O(1) time.
    public native int uncompressedLength(ByteBuffer compressed, int offset, int len) throws IOException;

    public native int uncompressedLength(Object input, int offset, int len) throws IOException;

    public native boolean isValidCompressedBuffer(ByteBuffer compressed, int offset, int len) throws IOException;

    public native boolean isValidCompressedBuffer(Object input, int offset, int len) throws IOException;

    public native void arrayCopy(Object src, int offset, int byteLength, Object dest, int dOffset) throws IOException;

    public void throw_error(int errorCode) throws IOException {
        throw new IOException(String.format("%s(%d)", SnappyErrorCode.getErrorMessage(errorCode), errorCode));
    }

}
