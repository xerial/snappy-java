//--------------------------------------
// snappy-java Project
//
// LoadSnappy.java
// Since: 2011/03/29
//
// $URL$ 
// $Author$
//--------------------------------------
package org.xerial.snappy;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Properties;

/**
 * 
 * 
 * @author leo
 * 
 */
public class LoadSnappy
{
    private static boolean extracted = false;

    public static boolean initialize() {
        if (!extracted)
            loadSnappyNativeLibrary();
        return extracted;
    }

    /**
     * Computes the MD5 value of the input stream
     * 
     * @param input
     * @return
     * @throws IOException
     * @throws NoSuchAlgorithmException
     */
    static String md5sum(InputStream input) throws IOException {
        BufferedInputStream in = new BufferedInputStream(input);

        try {
            MessageDigest digest = java.security.MessageDigest.getInstance("MD5");
            DigestInputStream digestInputStream = new DigestInputStream(in, digest);
            for (; digestInputStream.read() >= 0;) {

            }
            ByteArrayOutputStream md5out = new ByteArrayOutputStream();
            md5out.write(digest.digest());
            return md5out.toString();
        }
        catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("MD5 algorithm is not available: " + e);
        }
        finally {
            in.close();
        }
    }

    /**
     * Extract the specified library file to the target folder
     * 
     * @param libFolderForCurrentOS
     * @param libraryFileName
     * @param targetFolder
     * @return
     */
    private static boolean extractAndLoadLibraryFile(String libFolderForCurrentOS, String libraryFileName,
            String targetFolder) {
        String nativeLibraryFilePath = libFolderForCurrentOS + "/" + libraryFileName;
        final String prefix = "snappy-" + getVersion() + "-";

        String extractedLibFileName = prefix + libraryFileName;
        File extractedLibFile = new File(targetFolder, extractedLibFileName);

        try {
            if (extractedLibFile.exists()) {
                // test md5sum value
                String md5sum1 = md5sum(LoadSnappy.class.getResourceAsStream(nativeLibraryFilePath));
                String md5sum2 = md5sum(new FileInputStream(extractedLibFile));

                if (md5sum1.equals(md5sum2)) {
                    return loadNativeLibrary(targetFolder, extractedLibFileName);
                }
                else {
                    // remove old native library file
                    boolean deletionSucceeded = extractedLibFile.delete();
                    if (!deletionSucceeded) {
                        throw new IOException("failed to remove existing native library file: "
                                + extractedLibFile.getAbsolutePath());
                    }
                }
            }

            // extract file into the current directory
            InputStream reader = LoadSnappy.class.getResourceAsStream(nativeLibraryFilePath);
            FileOutputStream writer = new FileOutputStream(extractedLibFile);
            byte[] buffer = new byte[1024];
            int bytesRead = 0;
            while ((bytesRead = reader.read(buffer)) != -1) {
                writer.write(buffer, 0, bytesRead);
            }

            writer.close();
            reader.close();

            if (!System.getProperty("os.name").contains("Windows")) {
                try {
                    Runtime.getRuntime().exec(new String[] { "chmod", "755", extractedLibFile.getAbsolutePath() })
                            .waitFor();
                }
                catch (Throwable e) {}
            }

            return loadNativeLibrary(targetFolder, extractedLibFileName);
        }
        catch (IOException e) {
            System.err.println(e.getMessage());
            return false;
        }

    }

    private static synchronized boolean loadNativeLibrary(String path, String name) {
        File libPath = new File(path, name);
        if (libPath.exists()) {

            try {
                System.load(new File(path, name).getAbsolutePath());
                return true;
            }
            catch (UnsatisfiedLinkError e) {
                throw new SnappyError(SnappyErrorCode.FAILED_TO_LOAD_NATIVE_LIBRARY, e);
            }

        }
        else
            return false;
    }

    private static void loadSnappyNativeLibrary() {
        if (extracted)
            return;

        // Try loading library from org.sqlite.lib.path library path */
        String snappyNativeLibraryPath = System.getProperty("org.xerial.snappy.lib.path");
        String snappyNativeLibraryName = System.getProperty("org.xerial.snappy.lib.name");

        // Resolve the library file name with a suffix (e.g., dll, .so, etc.) 
        if (snappyNativeLibraryName == null)
            snappyNativeLibraryName = System.mapLibraryName("snappy");

        if (snappyNativeLibraryPath != null) {
            if (loadNativeLibrary(snappyNativeLibraryPath, snappyNativeLibraryName)) {
                extracted = true;
                return;
            }
        }

        // Load the os-dependent library from a jar file
        snappyNativeLibraryPath = "/org/xerial/snappy/native/" + OSInfo.getNativeLibFolderPathForCurrentOS();

        if (LoadSnappy.class.getResource(snappyNativeLibraryPath + "/" + snappyNativeLibraryName) == null) {
            // use nested VM version
            return;
        }

        // temporary library folder
        String tempFolder = new File(System.getProperty("java.io.tmpdir")).getAbsolutePath();
        // Try extracting the library from jar 
        if (extractAndLoadLibraryFile(snappyNativeLibraryPath, snappyNativeLibraryName, tempFolder)) {
            extracted = true;
            return;
        }

        extracted = false;
        return;
    }

    private static void getNativeLibraryFolderForTheCurrentOS() {
        String osName = OSInfo.getOSName();
        String archName = OSInfo.getArchName();

    }

    public static int getMajorVersion() {
        String[] c = getVersion().split("\\.");
        return (c.length > 0) ? Integer.parseInt(c[0]) : 1;
    }

    public static int getMinorVersion() {
        String[] c = getVersion().split("\\.");
        return (c.length > 1) ? Integer.parseInt(c[1]) : 0;
    }

    public static String getVersion() {

        URL versionFile = LoadSnappy.class.getResource("/META-INF/maven/org.xerial.snappy/snappy-java/pom.properties");
        if (versionFile == null)
            versionFile = LoadSnappy.class.getResource("/META-INF/maven/org.xerial.snappy/snappy-java/VERSION");

        String version = "unknown";
        try {
            if (versionFile != null) {
                Properties versionData = new Properties();
                versionData.load(versionFile.openStream());
                version = versionData.getProperty("version", version);
                version = version.trim().replaceAll("[^0-9\\.]", "");
            }
        }
        catch (IOException e) {
            System.err.println(e);
        }
        return version;
    }

}
