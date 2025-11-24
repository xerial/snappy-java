import org.xerial.snappy.Snappy;

/**
 * Simple integration test to verify snappy-java works in a separate JVM.
 * This is compiled and run as a standalone program to test the JAR manifest
 * and native library loading on different JDK versions.
 */
public class SnappyIntegrationTest {
    public static void main(String[] args) throws Exception {
        String input = "Hello snappy-java! Snappy-java is a JNI-based wrapper of "
            + "Snappy, a fast compresser/decompresser.";

        // Test compression
        byte[] compressed = Snappy.compress(input.getBytes("UTF-8"));
        System.out.println("Compressed " + input.length() + " bytes to " + compressed.length + " bytes");

        // Test decompression
        byte[] uncompressed = Snappy.uncompress(compressed);
        String result = new String(uncompressed, "UTF-8");

        // Verify result matches input
        if (!result.equals(input)) {
            System.err.println("ERROR: Decompressed data does not match input!");
            System.err.println("Expected: " + input);
            System.err.println("Got: " + result);
            System.exit(1);
        }

        System.out.println("SUCCESS: " + result);
        System.exit(0);
    }
}
