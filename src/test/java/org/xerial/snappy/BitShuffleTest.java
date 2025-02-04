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

import java.nio.ByteBuffer;

import org.junit.Assert;
import org.junit.Test;

public class BitShuffleTest {

    @Test
    public void directBufferCheck()
            throws Exception
    {
        ByteBuffer heapBuf = ByteBuffer.allocate(64);
        ByteBuffer directBuf = ByteBuffer.allocateDirect(64);

        // Tests for BitShuffle.shuffle()
        try {
            BitShuffle.shuffle(heapBuf, BitShuffleType.BYTE, directBuf);
            fail("no expected exception happened");
        }
        catch (SnappyError e) {
            Assert.assertTrue(e.errorCode == SnappyErrorCode.NOT_A_DIRECT_BUFFER);
            Assert.assertTrue(e.getMessage().contains("input is not a direct buffer"));
        }
        try {
            BitShuffle.shuffle(directBuf, BitShuffleType.BYTE, heapBuf);
            fail("no expected exception happened");
        }
        catch (SnappyError e) {
            Assert.assertTrue(e.errorCode == SnappyErrorCode.NOT_A_DIRECT_BUFFER);
            Assert.assertTrue(e.getMessage().contains("destination is not a direct buffer"));
        }

        // Then, tests for BitShuffle.unshuffle()
        try {
            BitShuffle.unshuffle(heapBuf, BitShuffleType.BYTE, directBuf);
            fail("no expected exception happened");
        }
        catch (SnappyError e) {
            Assert.assertTrue(e.errorCode == SnappyErrorCode.NOT_A_DIRECT_BUFFER);
            Assert.assertTrue(e.getMessage().contains("input is not a direct buffer"));
        }
        try {
            BitShuffle.unshuffle(directBuf, BitShuffleType.BYTE, heapBuf);
            fail("no expected exception happened");
        }
        catch (SnappyError e) {
            Assert.assertTrue(e.errorCode == SnappyErrorCode.NOT_A_DIRECT_BUFFER);
            Assert.assertTrue(e.getMessage().contains("destination is not a direct buffer"));
        }
    }

    @Test
    public void inputBufferSizeCheck()
            throws Exception
    {
        ByteBuffer inputBuf = ByteBuffer.allocateDirect(9);
        ByteBuffer outputBuf = ByteBuffer.allocateDirect(8);

        try {
            BitShuffle.shuffle(inputBuf, BitShuffleType.INT, outputBuf);
            fail("no expected exception happened");
        } catch (IllegalArgumentException e) {
            Assert.assertTrue(e.getMessage().startsWith("input length must be a multiple of the given type size"));
        }
        try {
            BitShuffle.unshuffle(inputBuf, BitShuffleType.INT, outputBuf);
            fail("no expected exception happened");
        } catch (IllegalArgumentException e) {
            Assert.assertTrue(e.getMessage().startsWith("length of input shuffled data must be a multiple of the given type size"));
        }
    }

    @Test
    public void outputBufferSizeCheck()
            throws Exception {
        ByteBuffer inputBuf = ByteBuffer.allocateDirect(12);
        ByteBuffer outputBuf = ByteBuffer.allocateDirect(3);

        try {
            BitShuffle.shuffle(inputBuf, BitShuffleType.INT, outputBuf);
            fail("no expected exception happened");
        } catch (IllegalArgumentException e) {
            Assert.assertTrue(e.getMessage().equals("not enough space for output"));
        }
        try {
            BitShuffle.unshuffle(inputBuf, BitShuffleType.INT, outputBuf);
            fail("no expected exception happened");
        } catch (IllegalArgumentException e) {
            Assert.assertTrue(e.getMessage().equals("not enough space for output"));
        }
    }

    @Test
    public void shuffleDirectLongArray()
            throws Exception
    {
        ByteBuffer testData = ByteBuffer.allocateDirect(48);
        ByteBuffer shuffled = ByteBuffer.allocateDirect(48);
        testData.putLong(2);
        testData.putLong(3);
        testData.putLong(15);
        testData.putLong(4234);
        testData.putLong(43251531412342342L);
        testData.putLong(23423422342L);
        testData.flip();
        BitShuffle.shuffle(testData, BitShuffleType.LONG, shuffled);
        ByteBuffer result = ByteBuffer.allocateDirect(48);
        BitShuffle.unshuffle(shuffled, BitShuffleType.LONG, result);
        assertEquals(2L, result.getLong());
        assertEquals(3L, result.getLong());
        assertEquals(15L, result.getLong());
        assertEquals(4234L, result.getLong());
        assertEquals(43251531412342342L, result.getLong());
        assertEquals(23423422342L, result.getLong());
    }

    @Test
    public void shuffleDirectShortArray()
            throws Exception
    {
        ByteBuffer testData = ByteBuffer.allocateDirect(18);
        ByteBuffer shuffled = ByteBuffer.allocateDirect(18);
        testData.putShort((short) 432);
        testData.putShort((short) -32267);
        testData.putShort((short) 1);
        testData.putShort((short) 3);
        testData.putShort((short) 34);
        testData.putShort((short) 43);
        testData.putShort((short) 34);
        testData.putShort(Short.MAX_VALUE);
        testData.putShort((short) -1);
        testData.flip();
        BitShuffle.shuffle(testData, BitShuffleType.SHORT, shuffled);
        ByteBuffer result = ByteBuffer.allocateDirect(18);
        BitShuffle.unshuffle(shuffled, BitShuffleType.SHORT, result);
        assertEquals(432, result.getShort());
        assertEquals(-32267, result.getShort());
        assertEquals(1, result.getShort());
        assertEquals(3, result.getShort());
        assertEquals(34, result.getShort());
        assertEquals(43, result.getShort());
        assertEquals(34, result.getShort());
        assertEquals(Short.MAX_VALUE, result.getShort());
        assertEquals(-1, result.getShort());
    }

    @Test
    public void shuffleDirectIntArray()
            throws Exception
    {
        ByteBuffer testData = ByteBuffer.allocateDirect(48);
        ByteBuffer shuffled = ByteBuffer.allocateDirect(48);
        testData.putInt(432);
        testData.putInt(-32267);
        testData.putInt(1);
        testData.putInt(3);
        testData.putInt(34);
        testData.putInt(43);
        testData.putInt(34);
        testData.putInt(Short.MAX_VALUE);
        testData.putInt(-1);
        testData.putInt(Integer.MAX_VALUE);
        testData.putInt(3424);
        testData.putInt(43);
        testData.flip();
        BitShuffle.shuffle(testData, BitShuffleType.INT, shuffled);
        ByteBuffer result = ByteBuffer.allocateDirect(48);
        BitShuffle.unshuffle(shuffled, BitShuffleType.INT, result);
        assertEquals(432, result.getInt());
        assertEquals(-32267, result.getInt());
        assertEquals(1, result.getInt());
        assertEquals(3, result.getInt());
        assertEquals(34, result.getInt());
        assertEquals(43, result.getInt());
        assertEquals(34, result.getInt());
        assertEquals(Short.MAX_VALUE, result.getInt());
        assertEquals(-1, result.getInt());
        assertEquals(Integer.MAX_VALUE, result.getInt());
        assertEquals(3424, result.getInt());
        assertEquals(43, result.getInt());
    }

    @Test
    public void shuffleDirectFloatArray()
            throws Exception
    {
        ByteBuffer testData = ByteBuffer.allocateDirect(36);
        ByteBuffer shuffled = ByteBuffer.allocateDirect(36);
        testData.putFloat(100.0f);
        testData.putFloat(0.5f);
        testData.putFloat(-0.1f);
        testData.putFloat(30.3f);
        testData.putFloat(Float.MIN_NORMAL);
        testData.putFloat(Float.MAX_EXPONENT);
        testData.putFloat(Float.MAX_VALUE);
        testData.putFloat(-0.1f);
        testData.putFloat(Integer.MIN_VALUE);
        testData.flip();
        BitShuffle.shuffle(testData, BitShuffleType.FLOAT, shuffled);
        ByteBuffer result = ByteBuffer.allocateDirect(36);
        BitShuffle.unshuffle(shuffled, BitShuffleType.FLOAT, result);
        assertEquals(100.0f, result.getFloat(), 0.0000001f);
        assertEquals(0.5f, result.getFloat(), 0.0000001f);
        assertEquals(-0.1f, result.getFloat(), 0.0000001f);
        assertEquals(30.3f, result.getFloat(), 0.0000001f);
        assertEquals(Float.MIN_NORMAL, result.getFloat(), 0.0000001f);
        assertEquals(Float.MAX_EXPONENT, result.getFloat(), 0.0000001f);
        assertEquals(Float.MAX_VALUE, result.getFloat(), 0.0000001f);
        assertEquals(-0.1f, result.getFloat(), 0.0000001f);
        assertEquals(Integer.MIN_VALUE, result.getFloat(), 0.0000001f);
    }

    @Test
    public void shuffleDirectDoubleArray()
            throws Exception
    {
        ByteBuffer testData = ByteBuffer.allocateDirect(72);
        ByteBuffer shuffled = ByteBuffer.allocateDirect(72);
        testData.putDouble(100.0);
        testData.putDouble(0.5);
        testData.putDouble(-0.1);
        testData.putDouble(30.3);
        testData.putDouble(Double.MIN_NORMAL);
        testData.putDouble(Double.MAX_EXPONENT);
        testData.putDouble(Double.MAX_VALUE);
        testData.putDouble(-0.1);
        testData.putDouble(Integer.MIN_VALUE);
        testData.flip();
        BitShuffle.shuffle(testData, BitShuffleType.DOUBLE, shuffled);
        ByteBuffer result = ByteBuffer.allocateDirect(72);
        BitShuffle.unshuffle(shuffled, BitShuffleType.DOUBLE, result);
        assertEquals(100.0, result.getDouble(), 0.0000001);
        assertEquals(0.5, result.getDouble(), 0.0000001);
        assertEquals(-0.1, result.getDouble(), 0.0000001);
        assertEquals(30.3, result.getDouble(), 0.0000001);
        assertEquals(Double.MIN_NORMAL, result.getDouble(), 0.0000001);
        assertEquals(Double.MAX_EXPONENT, result.getDouble(), 0.0000001);
        assertEquals(Double.MAX_VALUE, result.getDouble(), 0.0000001);
        assertEquals(-0.1, result.getDouble(), 0.0000001);
        assertEquals(Integer.MIN_VALUE, result.getDouble(), 0.0000001);
    }

    @Test
    public void shuffleLongArray()
            throws Exception
    {
        long[] data = new long[] {2, 3, 15, 4234, 43251531412342342L, 23423422342L};
        byte[] shuffledData = BitShuffle.shuffle(data);
        long[] result = BitShuffle.unshuffleLongArray(shuffledData);
        assertArrayEquals(data, result);
    }

    @Test
    public void shuffleShortArray()
            throws Exception
    {
        short[] data = new short[] {432, -32267, 1, 3, 34, 43, 34, Short.MAX_VALUE, -1};
        byte[] shuffledData = BitShuffle.shuffle(data);
        short[] result = BitShuffle.unshuffleShortArray(shuffledData);
        assertArrayEquals(data, result);
    }

    @Test
    public void shuffleIntArray()
            throws Exception
    {
        int[] data = new int[] {432, -32267, 1, 3, 34, 43, 34, Short.MAX_VALUE, -1, Integer.MAX_VALUE, 3424, 43};
        byte[] shuffledData = BitShuffle.shuffle(data);
        int[] result = BitShuffle.unshuffleIntArray(shuffledData);
        assertArrayEquals(data, result);
    }

    @Test
    public void shuffleFloatArray()
            throws Exception
    {
        float[] data = new float[] {100.0f, 0.5f, -0.1f, 30.3f, Float.MIN_NORMAL, Float.MAX_EXPONENT, Float.MAX_VALUE, -0.1f, Integer.MIN_VALUE};
        byte[] shuffledData = BitShuffle.shuffle(data);
        float[] result = BitShuffle.unshuffleFloatArray(shuffledData);
        assertArrayEquals(data, result, 0.0000001f);
    }

    @Test
    public void shuffleDoubleArray()
            throws Exception
    {
        double[] data = new double[] {100.0f, 0.5f, -0.1f, 30.3f, Float.MIN_NORMAL, Float.MAX_EXPONENT, Float.MAX_VALUE, -0.1f, Integer.MIN_VALUE};
        byte[] shuffledData = BitShuffle.shuffle(data);
        double[] result = BitShuffle.unshuffleDoubleArray(shuffledData);
        assertArrayEquals(data, result, 0.0000001f);
    }
}
