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
// Snappy.java
// Since: 2011/03/29
//
// $URL$ 
// $Author$
//--------------------------------------
package org.xerial.snappy;

import java.nio.ByteBuffer;

/**
 * Snappy API
 * 
 * @author leo
 * 
 */
public class Snappy
{

    public static String getNativeLibraryVersion() {
        return SnappyNative.nativeLibraryVersion();
    }

    /**
     * Compress the content of the given input, then output the compressed data.
     * 
     * @param uncompressed
     *            input is at buffer[pos() ... limit())
     * @param compressed
     *            output compressed data to buffer[pos()..]
     * @return byte size of the compressed data.
     */
    public static int compress(ByteBuffer uncompressed, ByteBuffer compressed) {

        if (!uncompressed.isDirect())
            throw new IllegalArgumentException("input is not a direct buffer");
        if (!compressed.isDirect())
            throw new IllegalArgumentException("destination is not a direct buffer");

        // input: uncompressed[pos(), limit())
        // output: compressed
        int uPos = uncompressed.position();
        int uLen = uncompressed.remaining();
        int compressedSize = SnappyNative.rawCompress(uncompressed, uPos, uLen, compressed, compressed.position());

        //         pos  limit
        // [ ......BBBBBBB.........]
        compressed.limit(compressed.position() + compressedSize);

        return compressedSize;
    }

    /**
     * Uncompress the compressed buffer to the specified output buffer
     * 
     * @param compressed
     *            input is at buffer[pos() ... limit())
     * @param uncompressed
     *            output the uncompressed data to buffer[pot())
     * @return uncompressed data size
     * 
     */
    public static int uncompress(ByteBuffer compressed, ByteBuffer uncompressed) throws SnappyException {

        if (!compressed.isDirect())
            throw new IllegalArgumentException("input is not a direct buffer");
        if (!uncompressed.isDirect())
            throw new IllegalArgumentException("destination is not a direct buffer");

        int cPos = compressed.position();
        int cLen = compressed.remaining();

        //         pos  limit
        // [ ......UUUUUU.........]
        int decompressedSize = SnappyNative
                .rawUncompress(compressed, cPos, cLen, uncompressed, uncompressed.position());
        uncompressed.limit(uncompressed.position() + decompressedSize);

        return decompressedSize;
    }

    /**
     * Get the uncompressed size of the given compressed input
     * 
     * @param compressed
     *            data [pos() ... limit())
     * @return
     */
    public static int uncompressedLength(ByteBuffer compressed) throws SnappyException {
        if (!compressed.isDirect())
            throw new IllegalArgumentException("input is not a direct buffer");

        return SnappyNative.uncompressedLength(compressed, compressed.position(), compressed.remaining());
    }

    /**
     * Get the maximum size of the compressed data of a given byte size
     * 
     * @param byteSize
     *            byte size of the data to compress
     * @return maximum byte size of the compressed data
     */
    public static int maxCompressedLength(int byteSize) {
        return SnappyNative.maxCompressedLength(byteSize);
    }

}
