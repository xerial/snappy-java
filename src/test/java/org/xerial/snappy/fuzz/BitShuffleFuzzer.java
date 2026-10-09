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
import org.xerial.snappy.BitShuffle;
import java.util.Objects;

public class BitShuffleFuzzer {
  private static final int SIZE = 4096;

  @FuzzTest
  public static void fuzzerTestOneInput(FuzzedDataProvider data) {
    switch (data.consumeInt(0, 9)) {
      case 0:
        fuzzBitshuffle(data.consumeInts(SIZE), BitShuffle::shuffle, BitShuffle::unshuffleIntArray, "int[]");
        break;
      case 1:
        fuzzBitshuffle(data.consumeLongs(SIZE), BitShuffle::shuffle, BitShuffle::unshuffleLongArray, "long[]");
        break;
      case 2:
        fuzzBitshuffle(data.consumeShorts(SIZE), BitShuffle::shuffle, BitShuffle::unshuffleShortArray, "short[]");
        break;
      case 3:
        fuzzBitshuffle(data.consumeFloats(SIZE), BitShuffle::shuffle, BitShuffle::unshuffleFloatArray, "float[]");
        break;
      case 4:
        fuzzBitshuffle(data.consumeDoubles(SIZE), BitShuffle::shuffle, BitShuffle::unshuffleDoubleArray, "double[]");
        break;
      case 5:
        fuzzBitshuffle(new int[0], BitShuffle::shuffle, BitShuffle::unshuffleIntArray, "empty int[]");
        break;
      case 6:
        fuzzBitshuffle(new float[0], BitShuffle::shuffle, BitShuffle::unshuffleFloatArray, "empty float[]");
        break;
      case 7:
        fuzzBitshuffle(new double[0], BitShuffle::shuffle, BitShuffle::unshuffleDoubleArray, "empty double[]");
        break;
      case 8:
        fuzzBitshuffle(new short[0], BitShuffle::shuffle, BitShuffle::unshuffleShortArray, "empty short[]");
        break;
      case 9:
        fuzzBitshuffle(new long[0], BitShuffle::shuffle, BitShuffle::unshuffleLongArray, "empty long[]");
        break;
    }
  }

  @FunctionalInterface
  private interface ShuffleFn<T> {
    byte[] apply(T input) throws Exception;
  }

  @FunctionalInterface
  private interface UnshuffleFn<T> {
    T apply(byte[] input) throws Exception;
  }

  private static <T> void fuzzBitshuffle(T original, ShuffleFn<T> shuffle, UnshuffleFn<T> unshuffle, String typeName) {
    try {
      byte[] shuffledByteArray = shuffle.apply(original);
      byte[] compressed = Snappy.compress(shuffledByteArray);
      byte[] uncompressed = Snappy.uncompress(compressed);
      T result = unshuffle.apply(uncompressed);
      if (!Objects.deepEquals(original, result)) {
        throw new IllegalStateException("Original and uncompressed " + typeName + " data are different");
      }
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }
}
