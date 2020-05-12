package org.xerial.snappy;

import org.junit.AfterClass;
import org.junit.BeforeClass;

/**
 *
 */
public class PureJavaSnappyOutputStreamTest
        extends SnappyOutputStreamTest
{
    @BeforeClass
    public static void setUp()
            throws Exception
    {
        PureJavaSnappyTestUtil.setUp();
    }

    @AfterClass
    public static void tearDown()
            throws Exception
    {
        PureJavaSnappyTestUtil.tearDown();
    }
}
