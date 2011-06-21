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
 * This class loads a native library of snappy-java (snappyjava.dll,
 * libsnappy.so, etc.) according to the user platform (<i>os.name</i> and
 * <i>os.arch</i>). The natively compiled libraries bundled to snappy-java
 * contain the codes of the original snappy and JNI programs to access Snappy.
 * 
 * In default, no configuration is required to use snappy-java, but you can load
 * your own native library created by 'make native' command.
 * 
 * LoadSnappy searches for native libraries (snappyjava.dll, libsnappy.so, etc.)
 * in the following order:
 * <ol>
 * <li>(System property: <i>org.xerial.snappy.lib.path</i>)/(System property:
 * <i>org.xerial.lib.name</i>)
 * <li>One of the libraries embedded in snappy-java-(version).jar extracted into
 * (System property: <i>java.io.tempdir</i> or if
 * <i>org.xerial.snappy.tempdir</i> is set, use this folder.)
 * <li>Folders in LD_PATH environment variable (This is the default path that
 * JVM searches for native libraries)
 * </ol>
 * 
 * <p>
 * If you do not want to use folder <i>java.io.tempdir</i>, set the System
 * property <i>org.xerial.snappy.tempdir</i>. For example, to use
 * <i>/tmp/leo</i> as a temporary folder to copy native libraries, use -D option
 * of JVM:
 * 
 * <pre>
 * <code>
 * java -Dorg.xerial.snappy.tempdir="/tmp/leo" ...
 * </code>
 * </pre>
 * 
 * </p>
 * 
 * @author leo
 * 
 */
public class LoadSnappy
{
    private static boolean isLoaded = false;

    public static boolean load() {
        if (!isLoaded) {
            loadSnappyNativeLibrary();
        }
        return isLoaded;
    }

    public static final String KEY_SNAPPY_LIB_PATH = "org.xerial.snappy.lib.path";
    public static final String KEY_SNAPPY_LIB_NAME = "org.xerial.snappy.lib.name";
    public static final String KEY_SNAPPY_TEMPDIR  = "org.xerial.snappy.tempdir";

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

            // extract a native library file into the target directory
            InputStream reader = LoadSnappy.class.getResourceAsStream(nativeLibraryFilePath);
            FileOutputStream writer = new FileOutputStream(extractedLibFile);
            byte[] buffer = new byte[1024];
            int bytesRead = 0;
            while ((bytesRead = reader.read(buffer)) != -1) {
                writer.write(buffer, 0, bytesRead);
            }

            writer.close();
            reader.close();

            // Set executable (x) flag to enable Java to load the native library
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
        if (isLoaded)
            return;

        // Try loading the library from org.xerial.snappy.lib.path library path */
        String snappyNativeLibraryPath = System.getProperty(KEY_SNAPPY_LIB_PATH);
        String snappyNativeLibraryName = System.getProperty(KEY_SNAPPY_LIB_NAME);

        // Resolve the library file name with a suffix (e.g., dll, .so, etc.) 
        if (snappyNativeLibraryName == null)
            snappyNativeLibraryName = System.mapLibraryName("snappyjava");

        if (snappyNativeLibraryPath != null) {
            if (loadNativeLibrary(snappyNativeLibraryPath, snappyNativeLibraryName)) {
                isLoaded = true;
                return;
            }
        }

        // Load an os-dependent native library inside a jar file
        snappyNativeLibraryPath = "/org/xerial/snappy/native/" + OSInfo.getNativeLibFolderPathForCurrentOS();

        if (LoadSnappy.class.getResource(snappyNativeLibraryPath + "/" + snappyNativeLibraryName) != null) {
            // Temporary library folder. Use the value of java.io.tmpdir
            String tempFolder = new File(System.getProperty(KEY_SNAPPY_TEMPDIR, System.getProperty("java.io.tmpdir")))
                    .getAbsolutePath();

            // Try extracting the library from jar
            if (extractAndLoadLibraryFile(snappyNativeLibraryPath, snappyNativeLibraryName, tempFolder)) {
                isLoaded = true;
                return;
            }
        }
        // Try to load snappyjava DLL in LD_PATH
        try {
            System.loadLibrary("snappyjava");
            isLoaded = true;
        }
        catch (UnsatisfiedLinkError e) {
            throw new SnappyError(SnappyErrorCode.FAILED_TO_LOAD_NATIVE_LIBRARY, e);
        }
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
            versionFile = LoadSnappy.class.getResource("/org/xerial/snappy/VERSION");

        String version = "unknown";
        try {
            if (versionFile != null) {
                Properties versionData = new Properties();
                versionData.load(versionFile.openStream());
                version = versionData.getProperty("version", version);
                if (version.equals("unknown"))
                    version = versionData.getProperty("VERSION", version);
                version = version.trim().replaceAll("[^0-9\\.]", "");
            }
        }
        catch (IOException e) {
            System.err.println(e);
        }
        return version;
    }

}
