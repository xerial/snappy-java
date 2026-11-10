// Copyright 2023 Google LLC
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
//
////////////////////////////////////////////////////////////////////////////////

package org.xerial.snappy.fuzz;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import com.code_intelligence.jazzer.junit.FuzzTest;
import org.xerial.snappy.Snappy;
import org.xerial.snappy.SnappyFramedInputStream;
import org.xerial.snappy.SnappyFramedOutputStream;
import org.xerial.snappy.SnappyInputStream;
import org.xerial.snappy.SnappyOutputStream;
import org.xerial.snappy.SnappyHadoopCompatibleOutputStream;
import org.xerial.snappy.BitShuffle;
import org.xerial.snappy.PureJavaCrc32C;
import java.nio.ByteBuffer;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;

public class SnappyCombinedFuzzer {
    
    private static void runFuzz(FuzzBlock block) {
        try {
            block.run();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
    
    @FuzzTest
    public static void fuzzerTestOneInput(FuzzedDataProvider data) {
        int selector = data.consumeInt(0, 7);
        switch (selector) {
            case 0:
                testRawApi(data);
                break;
            case 1:
                testFramed(data);
                break;
            case 2:
                runFuzz(() -> testCrc32C(data));
                break;
            case 3:
                testBlockStream(data);
                break;
            case 4:
                testUtil(data);
                break;
            case 5:
                testBitShuffle(data);
                break;
            case 6:
                testHadoopStream(data);
                break;
            case 7:
                testByteBuffer(data);
                break;
        }
    }

    private static void testRawApi(FuzzedDataProvider data) {
        switch (data.consumeInt(0, 10)) {
            case 0:
                runFuzz(() -> {
                    byte[] input = data.consumeBytes(data.consumeInt(0, 4096));
                    byte[] compressed = Snappy.compress(input);
                    byte[] uncompressed = Snappy.uncompress(compressed);
                    if (!Arrays.equals(input, uncompressed)) {
                        throw new IllegalStateException("Raw compress/uncompress failed");
                    }
                });
                break;
            case 1:
                runFuzz(() -> {
                    byte[] input = data.consumeBytes(data.consumeInt(0, 4096));
                    byte[] rawCompressed = Snappy.rawCompress(input, input.length);
                    if (Snappy.isValidCompressedBuffer(rawCompressed)) {
                        int uncompressedLen = Snappy.uncompressedLength(rawCompressed);
                        if (uncompressedLen == input.length) {
                            byte[] rawUncompressed = new byte[uncompressedLen];
                            Snappy.rawUncompress(rawCompressed, 0, rawCompressed.length, rawUncompressed, 0);
                            if (!Arrays.equals(input, rawUncompressed)) {
                                throw new IllegalStateException("Raw compress/uncompress with byte[] failed");
                            }
                        }
                    }
                });
                break;
            case 2:
                runFuzz(() -> {
                    try {
                        byte[] input = data.consumeBytes(data.consumeInt(0, 4096));
                        Snappy.isValidCompressedBuffer(input);
                        Snappy.isValidCompressedBuffer(input, 0, input.length);
                    } catch (IOException e) {
                        // Expected for invalid compressed buffers during fuzzing
                    }
                });
                break;
            case 3:
                runFuzz(() -> {
                    int inputLength = data.consumeInt(0, 4096);
                    int maxLen = Snappy.maxCompressedLength(inputLength);
                    if (maxLen < inputLength) {
                        throw new IllegalStateException("maxCompressedLength too small");
                    }
                });
                break;
            case 4:
                runFuzz(() -> {
                    byte[] input = data.consumeBytes(data.consumeInt(0, 4096));
                    byte[] compressed = Snappy.compress(input);
                    if (Snappy.isValidCompressedBuffer(compressed)) {
                        int len = Snappy.uncompressedLength(compressed);
                        int len2 = Snappy.uncompressedLength(compressed, 0, compressed.length);
                        if (len != input.length || len2 != input.length) {
                            throw new IllegalStateException("uncompressedLength did not match original length");
                        }
                    }
                });
                break;
            case 5:
                runFuzz(() -> {
                    int[] intInput = data.consumeInts(data.consumeInt(0, 100));
                    byte[] compressedInts = Snappy.compress(intInput);
                    int[] uncompressedInts = Snappy.uncompressIntArray(compressedInts);
                    if (!Arrays.equals(intInput, uncompressedInts)) {
                        throw new IllegalStateException("Int array roundtrip failed");
                    }
                });
                break;
            case 6:
                runFuzz(() -> {
                    long[] longInput = data.consumeLongs(data.consumeInt(0, 50));
                    byte[] compressedLongs = Snappy.compress(longInput);
                    long[] uncompressedLongs = Snappy.uncompressLongArray(compressedLongs);
                    if (!Arrays.equals(longInput, uncompressedLongs)) {
                        throw new IllegalStateException("Long array roundtrip failed");
                    }
                });
                break;
            case 7:
                runFuzz(() -> {
                    byte[] input = new byte[0];
                    byte[] compressed = Snappy.compress(input);
                    byte[] uncompressed = Snappy.uncompress(compressed);
                    if (!Arrays.equals(input, uncompressed)) {
                        throw new IllegalStateException("Empty array roundtrip failed");
                    }
                });
                break;
            case 8:
                runFuzz(() -> {
                    byte[] input = new byte[]{data.consumeByte()};
                    byte[] compressed = Snappy.compress(input);
                    byte[] uncompressed = Snappy.uncompress(compressed);
                    if (!Arrays.equals(input, uncompressed)) {
                        throw new IllegalStateException("Single byte roundtrip failed");
                    }
                });
                break;
            case 9:
                runFuzz(() -> {
                    int size = data.consumeInt(4095, 4096);
                    byte[] input = data.consumeBytes(size);
                    byte[] compressed = Snappy.compress(input);
                    byte[] uncompressed = Snappy.uncompress(compressed);
                    if (!Arrays.equals(input, uncompressed)) {
                        throw new IllegalStateException("Max size roundtrip failed");
                    }
                });
                break;
            case 10:
                runFuzz(() -> {
                    try {
                        int len = data.consumeInt(Integer.MAX_VALUE - 1000, Integer.MAX_VALUE);
                        Snappy.maxCompressedLength(len);
                    } catch (Exception e) {
                        // Expected for overflow/illegal argument
                    }
                });
                break;
        }
    }

    private static void testFramed(FuzzedDataProvider data) {
        switch (data.consumeInt(0, 2)) {
            case 0:
                runFuzz(() -> {
                    byte[] original = data.consumeBytes(data.consumeInt(0, 4096));
                    ByteArrayOutputStream compressedBuf = new ByteArrayOutputStream();
                    SnappyFramedOutputStream framedOut = new SnappyFramedOutputStream(compressedBuf);
                    framedOut.write(original);
                    framedOut.close();
                    byte[] compressed = compressedBuf.toByteArray();

                    int bufferSize = data.consumeInt(1, 4096);
                    try (SnappyFramedInputStream framedIn = new SnappyFramedInputStream(
                        new ByteArrayInputStream(compressed), true)) {
                        ByteArrayOutputStream out = new ByteArrayOutputStream();
                        byte[] buf = new byte[bufferSize];
                        int readBytes;
                        while ((readBytes = framedIn.read(buf)) != -1) {
                            out.write(buf, 0, readBytes);
                        }
                        if (!Arrays.equals(original, out.toByteArray())) {
                            throw new IllegalStateException("Framed stream roundtrip failed");
                        }
                    }
                });
                break;
            case 1:
                runFuzz(() -> {
                    try (SnappyFramedInputStream invalidIn = new SnappyFramedInputStream(
                        new ByteArrayInputStream(data.consumeBytes(100)))) {
                        while (invalidIn.read() != -1) {}
                    } catch (IOException e) {
                        // Expected for malformed input during fuzzing
                    }
                });
                break;
            case 2:
                runFuzz(() -> {
                    byte[] original = data.consumeBytes(data.consumeInt(0, 4096));
                    ByteArrayOutputStream compressedBuf = new ByteArrayOutputStream();
                    SnappyFramedOutputStream framedOut = new SnappyFramedOutputStream(compressedBuf);
                    framedOut.write(original);
                    framedOut.close();
                    byte[] compressed = compressedBuf.toByteArray();

                    for (int bufferSize : new int[]{1, 64, 256, 1024, 4096}) {
                        try (SnappyFramedInputStream framedIn = new SnappyFramedInputStream(
                            new ByteArrayInputStream(compressed), true)) {
                            ByteArrayOutputStream out = new ByteArrayOutputStream();
                            byte[] buf = new byte[bufferSize];
                            int readBytes;
                            while ((readBytes = framedIn.read(buf)) != -1) {
                                out.write(buf, 0, readBytes);
                            }
                            if (!Arrays.equals(original, out.toByteArray())) {
                                throw new IllegalStateException("Framed stream roundtrip failed");
                            }
                        }
                    }
                });
                break;
        }
    }

    private static void testCrc32C(FuzzedDataProvider data) {
        switch (data.consumeInt(0, 2)) {
            case 0:
                runFuzz(() -> {
                    byte[] input = data.consumeBytes(data.consumeInt(0, 4096));
                    PureJavaCrc32C crc = new PureJavaCrc32C();
                    crc.update(input, 0, input.length);
                    long value = crc.getValue();
                    int intValue = crc.getIntegerValue();
                    if ((int) value != intValue) {
                        throw new IllegalStateException("CRC32C int value mismatch");
                    }
                });
                break;
            case 1:
                runFuzz(() -> {
                    byte[] input = data.consumeBytes(data.consumeInt(0, 4096));
                    PureJavaCrc32C crc = new PureJavaCrc32C();
                    crc.update(input, 0, input.length);
                    long value = crc.getValue();
                    crc.reset();
                    crc.update(input, 0, input.length);
                    long value2 = crc.getValue();
                    if (value != value2) {
                        throw new IllegalStateException("CRC32C reset produced different value");
                    }
                });
                break;
            case 2:
                runFuzz(() -> {
                    byte[] input = data.consumeBytes(data.consumeInt(0, 4096));
                    PureJavaCrc32C crcChunked = new PureJavaCrc32C();
                    for (int i = 0; i < input.length; i++) {
                        crcChunked.update(input[i] & 0xFF);
                    }
                    PureJavaCrc32C crcWhole = new PureJavaCrc32C();
                    crcWhole.update(input, 0, input.length);
                    if (crcChunked.getValue() != crcWhole.getValue()) {
                        throw new IllegalStateException("CRC32C chunked vs whole mismatch");
                    }
                });
                break;
        }
    }

    private static void testBlockStream(FuzzedDataProvider data) {
        switch (data.consumeInt(0, 1)) {
            case 0:
                runFuzz(() -> {
                    byte[] original = data.consumeBytes(data.consumeInt(0, 4096));
                    ByteArrayOutputStream compressedBuf = new ByteArrayOutputStream();
                    SnappyOutputStream out = new SnappyOutputStream(compressedBuf, SnappyOutputStream.MIN_BLOCK_SIZE);
                    out.write(original);
                    out.close();
                    byte[] compressed = compressedBuf.toByteArray();

                    int bufferSize = data.consumeInt(1, 4096);
                    try (SnappyInputStream in = new SnappyInputStream(new ByteArrayInputStream(compressed))) {
                        ByteArrayOutputStream result = new ByteArrayOutputStream();
                        byte[] buf = new byte[bufferSize];
                        int read;
                        while ((read = in.read(buf)) != -1) {
                            result.write(buf, 0, read);
                        }
                        if (!Arrays.equals(original, result.toByteArray())) {
                            throw new IllegalStateException("Block stream roundtrip failed");
                        }
                    }
                });
                break;
            case 1:
                runFuzz(() -> {
                    try (SnappyInputStream in = new SnappyInputStream(new ByteArrayInputStream(data.consumeBytes(100)))) {
                        while (in.read() != -1) {}
                    } catch (IOException e) {
                        // Expected for malformed input during fuzzing
                    }
                });
                break;
        }
    }

    private static void testUtil(FuzzedDataProvider data) {
        switch (data.consumeInt(0, 4)) {
            case 0:
                runFuzz(() -> {
                    String str = data.consumeString(data.consumeInt(0, 4096));
                    byte[] compressed = Snappy.compress(str);
                    String uncompressed = Snappy.uncompressString(compressed);
                    if (!str.equals(uncompressed)) {
                        throw new IllegalStateException("String roundtrip failed");
                    }
                });
                break;
            case 1:
                runFuzz(() -> {
                    short[] shortInput = data.consumeShorts(data.consumeInt(0, 100));
                    byte[] compressedShorts = Snappy.compress(shortInput);
                    short[] uncompressedShorts = Snappy.uncompressShortArray(compressedShorts);
                    if (!Arrays.equals(shortInput, uncompressedShorts)) {
                        throw new IllegalStateException("Short array roundtrip failed");
                    }
                });
                break;
            case 2:
                runFuzz(() -> {
                    char[] charInput = data.consumeString(data.consumeInt(0, 100)).toCharArray();
                    byte[] compressedChars = Snappy.compress(charInput);
                    char[] uncompressedChars = Snappy.uncompressCharArray(compressedChars);
                    if (!Arrays.equals(charInput, uncompressedChars)) {
                        throw new IllegalStateException("Char array roundtrip failed");
                    }
                });
                break;
            case 3:
                runFuzz(() -> {
                    float[] floatInput = data.consumeFloats(data.consumeInt(0, 100));
                    byte[] shuffled = BitShuffle.shuffle(floatInput);
                    float[] unshuffled = BitShuffle.unshuffleFloatArray(shuffled);
                    if (!Arrays.equals(floatInput, unshuffled)) {
                        throw new IllegalStateException("BitShuffle float failed");
                    }
                });
                break;
            case 4:
                runFuzz(() -> {
                    double[] doubleInput = data.consumeDoubles(data.consumeInt(0, 50));
                    byte[] shuffled = BitShuffle.shuffle(doubleInput);
                    double[] unshuffled = BitShuffle.unshuffleDoubleArray(shuffled);
                    if (!Arrays.equals(doubleInput, unshuffled)) {
                        throw new IllegalStateException("BitShuffle double failed");
                    }
                });
                break;
        }
    }

    private static void testBitShuffle(FuzzedDataProvider data) {
        switch (data.consumeInt(0, 2)) {
            case 0:
                runFuzz(() -> {
                    int[] intInput = data.consumeInts(data.consumeInt(0, 100));
                    byte[] shuffled = BitShuffle.shuffle(intInput);
                    int[] unshuffled = BitShuffle.unshuffleIntArray(shuffled);
                    if (!Arrays.equals(intInput, unshuffled)) {
                        throw new IllegalStateException("BitShuffle int failed");
                    }
                });
                break;
            case 1:
                runFuzz(() -> {
                    long[] longInput = data.consumeLongs(data.consumeInt(0, 50));
                    byte[] shuffled = BitShuffle.shuffle(longInput);
                    long[] unshuffled = BitShuffle.unshuffleLongArray(shuffled);
                    if (!Arrays.equals(longInput, unshuffled)) {
                        throw new IllegalStateException("BitShuffle long failed");
                    }
                });
                break;
            case 2:
                runFuzz(() -> {
                    short[] shortInput = data.consumeShorts(data.consumeInt(0, 100));
                    byte[] shuffled = BitShuffle.shuffle(shortInput);
                    short[] unshuffled = BitShuffle.unshuffleShortArray(shuffled);
                    if (!Arrays.equals(shortInput, unshuffled)) {
                        throw new IllegalStateException("BitShuffle short failed");
                    }
                });
                break;
        }
    }

    private static void testHadoopStream(FuzzedDataProvider data) {
        switch (data.consumeInt(0, 1)) {
            case 0:
                runFuzz(() -> {
                    byte[] original = data.consumeBytes(data.consumeInt(0, 4096));
                    ByteArrayOutputStream compressedBuf = new ByteArrayOutputStream();
                    SnappyHadoopCompatibleOutputStream out = new SnappyHadoopCompatibleOutputStream(compressedBuf);
                    out.write(original);
                    out.close();
                    byte[] compressed = compressedBuf.toByteArray();

                    int bufferSize = data.consumeInt(1, 4096);
                    try (SnappyInputStream in = new SnappyInputStream(
                        new ByteArrayInputStream(compressed))) {
                        ByteArrayOutputStream result = new ByteArrayOutputStream();
                        byte[] buf = new byte[bufferSize];
                        int read;
                        while ((read = in.read(buf)) != -1) {
                            result.write(buf, 0, read);
                        }
                        if (!Arrays.equals(original, result.toByteArray())) {
                            throw new IllegalStateException("Hadoop stream roundtrip failed");
                        }
                    }
                });
                break;
            case 1:
                runFuzz(() -> {
                    try (SnappyInputStream in = new SnappyInputStream(
                        new ByteArrayInputStream(data.consumeBytes(100)))) {
                        while (in.read() != -1) {}
                    } catch (IOException e) {
                        // Expected for malformed input during fuzzing
                    }
                });
                break;
        }
    }

    private static void testByteBuffer(FuzzedDataProvider data) {
        switch (data.consumeInt(0, 1)) {
            case 0:
                runFuzz(() -> {
                    byte[] input = data.consumeBytes(data.consumeInt(0, 4096));
                    ByteBuffer src = ByteBuffer.allocateDirect(input.length);
                    src.put(input);
                    src.flip();
                    ByteBuffer dst = ByteBuffer.allocateDirect(Snappy.maxCompressedLength(input.length));
                    int compressed = Snappy.compress(src, dst);
                    
                    dst.limit(compressed);
                    dst.position(0);
                    ByteBuffer uncompressedBuf = ByteBuffer.allocateDirect(input.length);
                    int uncompressed = Snappy.uncompress(dst, uncompressedBuf);
                    
                    uncompressedBuf.limit(uncompressed);
                    uncompressedBuf.position(0);
                    byte[] result = new byte[uncompressed];
                    uncompressedBuf.get(result);
                    
                    if (!Arrays.equals(input, result)) {
                        throw new IllegalStateException("ByteBuffer compress failed");
                    }
                });
                break;
            case 1:
                runFuzz(() -> {
                    byte[] input = data.consumeBytes(data.consumeInt(0, 4096));
                    ByteBuffer src = ByteBuffer.allocateDirect(input.length);
                    src.put(input);
                    src.flip();
                    ByteBuffer dst = ByteBuffer.allocateDirect(Snappy.maxCompressedLength(input.length));
                    try {
                        Snappy.compress(src, dst);
                    } catch (Exception e) {
                        // Expected for invalid input during fuzzing
                    }
                });
                break;
        }
    }
}
