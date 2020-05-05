package org.xerial.snappy;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.xerial.util.log.Logger;

/**
 *
 */
public abstract class PureJavaSnappyTestUtil
{
    private static Logger _logger = Logger.getLogger(PureJavaSnappyTestUtil.class);

    @BeforeClass
    public static void setUp()
            throws Exception
    {
        _logger.info("Loading pure-java Snappy");
        Snappy.cleanUp();
        System.setProperty(SnappyLoader.KEY_SNAPPY_PUREJAVA, "true");
        SnappyLoader.loadSnappyApi();
    }

    @AfterClass
    public static void tearDown()
            throws Exception
    {
        System.setProperty(SnappyLoader.KEY_SNAPPY_PUREJAVA, "false");
        Snappy.cleanUp();
        _logger.info("Unloading pure-java Snappy");
        // Initialize Snappy again for the other tests
        Snappy.init();
    }
}
