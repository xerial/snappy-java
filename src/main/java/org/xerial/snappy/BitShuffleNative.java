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
// BitShuffleNative.java
// Since: 2016/03/31
//
// $URL$
// $Author$
//--------------------------------------
package org.xerial.snappy;

import java.io.IOException;

/**
 * JNI interfaces of the {@link BitShuffle} implementation. The native method in this class is
 * defined in BitShuffleNative.h (genereted by javah) and BitShuffleNative.cpp
 * <p/>
 * <p>
 * <b> DO NOT USE THIS CLASS since the direct use of this class might break the
 * native library code loading in {@link SnappyLoader}. </b>
 * </p>
 *
 * @author leo
 */
public class BitShuffleNative
{

    // ------------------------------------------------------------------------
    // Bit-shuffling routines to improve compression of typed binary data.
    // A quick benchmark result can be found in a gist below;
    // https://gist.github.com/maropu/01103215df34b317a7a7
    // ------------------------------------------------------------------------

    // Returns true iff a native library loaded in JVMs can run bit-shuffling.
    // Bit-shuffling is executable only in x86 environments that support SSE/AVX instructions.
    public native boolean supportBitSuffle();

    public native int bitShuffle(Object input, int inputOffset, int typeSize, int byteLength, Object output, int outputOffset)
            throws IOException;

    public native int bitUnShuffle(Object input, int inputOffset, int typeSize, int byteLength, Object output, int outputOffset)
            throws IOException;
}
