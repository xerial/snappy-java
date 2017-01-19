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
// BitShuffleTest.java
// Since: 2016/03/31
//
// $URL$
// $Author$
//--------------------------------------
package org.xerial.snappy;

import static org.junit.Assert.*;

import org.junit.Test;

public class BitShuffleTest {

    @Test
    public void bitShuffleLongArray()
            throws Exception
    {
        long[] data = new long[] {2, 3, 15, 4234, 43251531412342342L, 23423422342L};
        byte[] shuffledData = BitShuffle.bitShuffle(data);
        long[] result = BitShuffle.bitUnShuffleLongArray(shuffledData);
        assertArrayEquals(data, result);
    }

    @Test
    public void bitShuffleShortArray()
            throws Exception
    {
        short[] data = new short[] {432, -32267, 1, 3, 34, 43, 34, Short.MAX_VALUE, -1};
        byte[] shuffledData = BitShuffle.bitShuffle(data);
        short[] result = BitShuffle.bitUnShuffleShortArray(shuffledData);
        assertArrayEquals(data, result);
    }

    @Test
    public void bitShuffleIntArray()
            throws Exception
    {
        int[] data = new int[] {432, -32267, 1, 3, 34, 43, 34, Short.MAX_VALUE, -1, Integer.MAX_VALUE, 3424, 43};
        byte[] shuffledData = BitShuffle.bitShuffle(data);
        int[] result = BitShuffle.bitUnShuffleIntArray(shuffledData);
        assertArrayEquals(data, result);
    }

    @Test
    public void bitShuffleFloatArray()
            throws Exception
    {
        float[] data = new float[] {100.0f, 0.5f, -0.1f, 30.3f, Float.MIN_NORMAL, Float.MAX_EXPONENT, Float.MAX_VALUE, -0.1f, Integer.MIN_VALUE};
        byte[] shuffledData = BitShuffle.bitShuffle(data);
        float[] result = BitShuffle.bitUnShuffleFloatArray(shuffledData);
        assertArrayEquals(data, result, 0.0000001f);
    }

    @Test
    public void bitShuffleDoubleArray()
            throws Exception
    {
        double[] data = new double[] {100.0f, 0.5f, -0.1f, 30.3f, Float.MIN_NORMAL, Float.MAX_EXPONENT, Float.MAX_VALUE, -0.1f, Integer.MIN_VALUE};
        byte[] shuffledData = BitShuffle.bitShuffle(data);
        double[] result = BitShuffle.bitUnShuffleDoubleArray(shuffledData);
        assertArrayEquals(data, result, 0.0000001f);
    }
}
