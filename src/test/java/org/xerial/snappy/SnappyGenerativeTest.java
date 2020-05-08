package org.xerial.snappy;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import static org.junit.Assert.assertArrayEquals;

@RunWith(Parameterized.class)
public class SnappyGenerativeTest {

  @Parameterized.Parameters
  public static Iterable<Object[]> data() {
    List<Object[]> testCases = new ArrayList<>(100);
    for (int i = 0 ; i < 100; ++i) {
      testCases.add(randomData());
    }
    return testCases;
  }


  private final byte[] input;

  public SnappyGenerativeTest(byte[] input) {
    this.input = input;
  }

  @Test
  public void roundTripDirectToDirect() throws IOException {
    ByteBuffer in = ByteBuffer.allocateDirect(input.length);
    in.put(input);
    ByteBuffer compressed = ByteBuffer.allocateDirect(input.length * 2);
    Snappy.compress(in, compressed);
    Snappy.uncompress(compressed, in);
    byte[] result = new byte[input.length];
    in.flip();
    in.get(result);
    assertArrayEquals(input, result);
  }

  @Test
  public void roundTripDirectToHeap() throws IOException {
    ByteBuffer in = ByteBuffer.allocateDirect(input.length);
    in.put(input);
    in.flip();
    ByteBuffer compressed = ByteBuffer.allocateDirect(input.length * 2);
    Snappy.compress(in, compressed);
    ByteBuffer out = ByteBuffer.allocate(input.length);
    Snappy.uncompress(compressed, out);
    out.flip();
    assertArrayEquals(input, out.array());
  }

  private static Object[] randomData() {
    int length = Math.abs(ThreadLocalRandom.current().nextInt(10,10_000));
    byte[] data = new byte[length];
    ThreadLocalRandom.current().nextBytes(data);
    return new Object[] {data};
  }
}
