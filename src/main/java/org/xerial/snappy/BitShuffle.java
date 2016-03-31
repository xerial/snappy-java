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
// BitShuffle.java
// Since: 2016/03/31
//
// $URL$
// $Author$
//--------------------------------------
package org.xerial.snappy;

import java.io.IOException;

public class BitShuffle
{
    static {
        try {
            impl = SnappyLoader.loadBitShuffleApi();
        }
        catch (Exception e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    /**
     * An instance of BitShuffleNative
     */
    private static BitShuffleNative impl;

    /**
     * Apply a bit-shuffling filter into the input short array.
     *
     * @param input
     * @return bit-shuffled byte array
     * @throws IOException
     */
    public static byte[] bitShuffle(short[] input) throws IOException {
        byte[] output = new byte[input.length * 2];
        if (impl.supportBitSuffle()) {
            impl.bitShuffle(input, 0, 2, input.length * 2, output, 0);
        } else {
            Snappy.arrayCopy(input, 0, input.length * 2, output, 0);
        }
        return output;
    }

    /**
     * Apply a bit-shuffling filter into the input int array.
     *
     * @param input
     * @return bit-shuffled byte array
     * @throws IOException
     */
    public static byte[] bitShuffle(int[] input) throws IOException {
        byte[] output = new byte[input.length * 4];
        if (impl.supportBitSuffle()) {
            impl.bitShuffle(input, 0, 4, input.length * 4, output, 0);
        } else {
            Snappy.arrayCopy(input, 0, input.length * 4, output, 0);
        }
        return output;
    }

    /**
     * Apply a bit-shuffling filter into the input long array.
     *
     * @param input
     * @return bit-shuffled byte array
     * @throws IOException
     */
    public static byte[] bitShuffle(long[] input) throws IOException {
        byte[] output = new byte[input.length * 8];
        if (impl.supportBitSuffle()) {
            impl.bitShuffle(input, 0, 8, input.length * 8, output, 0);
        } else {
            Snappy.arrayCopy(input, 0, input.length * 8, output, 0);
        }
        return output;
    }

    /**
     * Apply a bit-shuffling filter into the input float array.
     *
     * @param input
     * @return bit-shuffled byte array
     * @throws IOException
     */
    public static byte[] bitShuffle(float[] input) throws IOException {
        byte[] output = new byte[input.length * 4];
        if (impl.supportBitSuffle()) {
            impl.bitShuffle(input, 0, 4, input.length * 4, output, 0);
        } else {
            Snappy.arrayCopy(input, 0, input.length * 4, output, 0);
        }
        return output;
    }

    /**
     * Apply a bit-shuffling filter into the input double array.
     *
     * @param input
     * @return bit-shuffled byte array
     * @throws IOException
     */
    public static byte[] bitShuffle(double[] input) throws IOException {
        byte[] output = new byte[input.length * 8];
        if (impl.supportBitSuffle()) {
            impl.bitShuffle(input, 0, 8, input.length * 8, output, 0);
        } else {
            Snappy.arrayCopy(input, 0, input.length * 8, output, 0);
        }
        return output;
    }

    /**
     * Convert the input bit-shuffled byte array into an original short array.
     *
     * @param input
     * @return a short array
     * @throws IOException
     */
    public static short[] bitUnShuffleShortArray(byte[] input) throws IOException {
        short[] output = new short[input.length / 2];
        if (impl.supportBitSuffle()) {
            impl.bitUnShuffle(input, 0, 2, input.length, output, 0);
        } else {
            Snappy.arrayCopy(input, 0, input.length, output, 0);
        }
        return output;
    }

    /**
     * Convert the input bit-shuffled byte array into an original int array.
     *
     * @param input
     * @return an int array
     * @throws IOException
     */
    public static int[] bitUnShuffleIntArray(byte[] input) throws IOException {
        int[] output = new int[input.length / 4];
        if (impl.supportBitSuffle()) {
            impl.bitUnShuffle(input, 0, 4, input.length, output, 0);
        } else {
            Snappy.arrayCopy(input, 0, input.length, output, 0);
        }
        return output;
    }

    /**
     * Convert the input bit-shuffled byte array into an original long array.
     *
     * @param input
     * @return a long array
     * @throws IOException
     */
    public static long[] bitUnShuffleLongArray(byte[] input) throws IOException {
        long[] output = new long[input.length / 8];
        if (impl.supportBitSuffle()) {
            impl.bitUnShuffle(input, 0, 8, input.length, output, 0);
        } else {
            Snappy.arrayCopy(input, 0, input.length, output, 0);
        }
        return output;
    }

    /**
     * Convert the input bit-shuffled byte array into an original float array.
     *
     * @param input
     * @return an float array
     * @throws IOException
     */
    public static float[] bitUnShuffleFloatArray(byte[] input) throws IOException {
        float[] output = new float[input.length / 4];
        if (impl.supportBitSuffle()) {
            impl.bitUnShuffle(input, 0, 4, input.length, output, 0);
        } else {
            Snappy.arrayCopy(input, 0, input.length, output, 0);
        }
        return output;
    }

    /**
     * Convert the input bit-shuffled byte array into an original double array.
     *
     * @param input
     * @return a double array
     * @throws IOException
     */
    public static double[] bitUnShuffleDoubleArray(byte[] input) throws IOException {
        double[] output = new double[input.length / 8];
        if (impl.supportBitSuffle()) {
            impl.bitUnShuffle(input, 0, 8, input.length, output, 0);
        } else {
            Snappy.arrayCopy(input, 0, input.length, output, 0);
        }
        return output;
    }
}
