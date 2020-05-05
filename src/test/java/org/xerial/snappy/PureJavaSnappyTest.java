package org.xerial.snappy;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 *
 */
public class PureJavaSnappyTest {
  @BeforeClass
  public static void setUp() throws Exception {
    Snappy.cleanUp();
    System.setProperty(SnappyLoader.KEY_SNAPPY_PUREJAVA, "true");
    SnappyLoader.loadSnappyApi();
  }

  @AfterClass
  public static void tearDown() throws Exception {
    System.setProperty(SnappyLoader.KEY_SNAPPY_PUREJAVA, "false");
    Snappy.cleanUp();
  }

  @Test
  public void testPureJavaCompression() throws Exception {
    assertTrue(SnappyLoader.isPureJava());

    String m = "Hello! 01234 ACGDSFSDFJ World. FDSDF02394234 fdsfda03924";
    byte[] input = m.getBytes();
    byte[] output = Snappy.compress(input);
    byte[] uncompressed = Snappy.uncompress(output);
    String m2 = new String(uncompressed);
    assertEquals(m, m2);
  }
}
