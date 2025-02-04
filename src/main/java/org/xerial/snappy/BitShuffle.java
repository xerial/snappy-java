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
import java.nio.ByteBuffer;

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
     * Apply a bit-shuffling filter into the content in the given input buffer. After bit-shuffling,
     * you can retrieve the shuffled data from the output buffer [pos() ...limit())
     * (shuffled data size = limit() - pos() = remaining()).
     *
     * @param input buffer[pos() ... limit()) containing the input data
     * @param type element type of the input data
     * @param shuffled output of the shuffled data. Uses range [pos()..].
     * @return byte size of the shuffled data.
     * @throws SnappyError when the input is not a direct buffer
     * @throws IllegalArgumentException when the input length is not a multiple of the given type size
     */
    public static int shuffle(ByteBuffer input, BitShuffleType type, ByteBuffer shuffled) throws IOException {
        if (!input.isDirect()) {
            throw new SnappyError(SnappyErrorCode.NOT_A_DIRECT_BUFFER, "input is not a direct buffer");
        }
        if (!shuffled.isDirect()) {
            throw new SnappyError(SnappyErrorCode.NOT_A_DIRECT_BUFFER, "destination is not a direct buffer");
        }

        // input: input[pos(), limit())
        // output: shuffled
        int uPos = input.position();
        int uLen = input.remaining();
        int typeSize = type.getTypeSize();
        if (uLen % typeSize != 0) {
            throw new IllegalArgumentException("input length must be a multiple of the given type size: " + typeSize);
        }
        if (shuffled.remaining() < uLen) {
            throw new IllegalArgumentException("not enough space for output");
        }
        int numProcessed = impl.shuffleDirectBuffer(input, uPos, typeSize, uLen, shuffled, shuffled.position());
        assert(numProcessed == uLen);

        //         pos   limit
        // [ ......BBBBBBB.........]
        shuffled.limit(shuffled.position() + numProcessed);
        return numProcessed;
    }

    /**
     * Apply a bit-shuffling filter into the input short array.
     *
     * @param input
     * @return bit-shuffled byte array
     * @throws IOException
     */
    public static byte[] shuffle(short[] input) throws IOException {
        if (input.length * 2 < input.length) {
            throw new SnappyError(SnappyErrorCode.TOO_LARGE_INPUT, "input array size is too large: " + input.length);
        }
        byte[] output = new byte[input.length * 2];
        int numProcessed = impl.shuffle(input, 0, 2, input.length * 2, output, 0);
        assert(numProcessed == input.length * 2);
        return output;
    }

    /**
     * Apply a bit-shuffling filter into the input int array.
     *
     * @param input
     * @return bit-shuffled byte array
     * @throws IOException
     */
    public static byte[] shuffle(int[] input) throws IOException {
        if (input.length * 4 < input.length) {
            throw new SnappyError(SnappyErrorCode.TOO_LARGE_INPUT, "input array size is too large: " + input.length);
        }
        byte[] output = new byte[input.length * 4];
        int numProcessed = impl.shuffle(input, 0, 4, input.length * 4, output, 0);
        assert(numProcessed == input.length * 4);
        return output;
    }

    /**
     * Apply a bit-shuffling filter into the input long array.
     *
     * @param input
     * @return bit-shuffled byte array
     * @throws IOException
     */
    public static byte[] shuffle(long[] input) throws IOException {
        if (input.length * 8 < input.length) {
            throw new SnappyError(SnappyErrorCode.TOO_LARGE_INPUT, "input array size is too large: " + input.length);
        }
        byte[] output = new byte[input.length * 8];
        int numProcessed = impl.shuffle(input, 0, 8, input.length * 8, output, 0);
        assert(numProcessed == input.length * 8);
        return output;
    }

    /**
     * Apply a bit-shuffling filter into the input float array.
     *
     * @param input
     * @return bit-shuffled byte array
     * @throws IOException
     */
    public static byte[] shuffle(float[] input) throws IOException {
        if (input.length * 4 < input.length) {
            throw new SnappyError(SnappyErrorCode.TOO_LARGE_INPUT, "input array size is too large: " + input.length);
        }
        byte[] output = new byte[input.length * 4];
        int numProcessed = impl.shuffle(input, 0, 4, input.length * 4, output, 0);
        assert(numProcessed == input.length * 4);
        return output;
    }

    /**
     * Apply a bit-shuffling filter into the input double array.
     *
     * @param input
     * @return bit-shuffled byte array
     * @throws IOException
     */
    public static byte[] shuffle(double[] input) throws IOException {
        if (input.length * 8 < input.length) {
            throw new SnappyError(SnappyErrorCode.TOO_LARGE_INPUT, "input array size is too large: " + input.length);
        }
        byte[] output = new byte[input.length * 8];
        int numProcessed = impl.shuffle(input, 0, 8, input.length * 8, output, 0);
        assert(numProcessed == input.length * 8);
        return output;
    }

    /**
     * Convert the input bit-shuffled byte array into an original array. The result is dumped
     * to the specified output buffer.
     *
     * @param shuffled buffer[pos() ... limit()) containing the input shuffled data
     * @param type element type of the input data
     * @param output output of the the original data. It uses buffer[pos()..]
     * @return byte size of the unshuffled data.
     * @throws IOException when failed to unshuffle the given input
     * @throws SnappyError when the input is not a direct buffer
     * @throws IllegalArgumentException when the length of input shuffled data is not a multiple of the given type size
     */
    public static int unshuffle(ByteBuffer shuffled, BitShuffleType type, ByteBuffer output) throws IOException {
        if (!shuffled.isDirect()) {
            throw new SnappyError(SnappyErrorCode.NOT_A_DIRECT_BUFFER, "input is not a direct buffer");
        }
        if (!output.isDirect()) {
            throw new SnappyError(SnappyErrorCode.NOT_A_DIRECT_BUFFER, "destination is not a direct buffer");
        }

        // input: input[pos(), limit())
        // output: shuffled
        int uPos = shuffled.position();
        int uLen = shuffled.remaining();
        int typeSize = type.getTypeSize();
        if (uLen % typeSize != 0) {
            throw new IllegalArgumentException("length of input shuffled data must be a multiple of the given type size: " + typeSize);
        }
        if (output.remaining() < uLen) {
            throw new IllegalArgumentException("not enough space for output");
        }
        int numProcessed = impl.unshuffleDirectBuffer(shuffled, uPos, typeSize, uLen, output, shuffled.position());
        assert(numProcessed == uLen);

        //         pos   limit
        // [ ......BBBBBBB.........]
        shuffled.limit(shuffled.position() + numProcessed);
        return numProcessed;
    }

    /**
     * Convert the input bit-shuffled byte array into an original short array.
     *
     * @param input
     * @return a short array
     * @throws IOException
     */
    public static short[] unshuffleShortArray(byte[] input) throws IOException {
        short[] output = new short[input.length / 2];
        int numProcessed = impl.unshuffle(input, 0, 2, input.length, output, 0);
        assert(numProcessed == input.length);
        return output;
    }

    /**
     * Convert the input bit-shuffled byte array into an original int array.
     *
     * @param input
     * @return an int array
     * @throws IOException
     */
    public static int[] unshuffleIntArray(byte[] input) throws IOException {
        int[] output = new int[input.length / 4];
        int numProcessed = impl.unshuffle(input, 0, 4, input.length, output, 0);
        assert(numProcessed == input.length);
        return output;
    }

    /**
     * Convert the input bit-shuffled byte array into an original long array.
     *
     * @param input
     * @return a long array
     * @throws IOException
     */
    public static long[] unshuffleLongArray(byte[] input) throws IOException {
        long[] output = new long[input.length / 8];
        int numProcessed = impl.unshuffle(input, 0, 8, input.length, output, 0);
        assert(numProcessed == input.length);
        return output;
    }

    /**
     * Convert the input bit-shuffled byte array into an original float array.
     *
     * @param input
     * @return an float array
     * @throws IOException
     */
    public static float[] unshuffleFloatArray(byte[] input) throws IOException {
        float[] output = new float[input.length / 4];
        int numProcessed = impl.unshuffle(input, 0, 4, input.length, output, 0);
        assert(numProcessed == input.length);
        return output;
    }

    /**
     * Convert the input bit-shuffled byte array into an original double array.
     *
     * @param input
     * @return a double array
     * @throws IOException
     */
    public static double[] unshuffleDoubleArray(byte[] input) throws IOException {
        double[] output = new double[input.length / 8];
        int numProcessed = impl.unshuffle(input, 0, 8, input.length, output, 0);
        assert(numProcessed == input.length);
        return output;
    }
}
