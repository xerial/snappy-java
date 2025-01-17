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
// SnappyLoader.java
// Since: 2011/03/29
//
// $URL$
// $Author$
//--------------------------------------
package org.xerial.snappy;

import java.io.*;
import java.net.JarURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.Enumeration;
import java.util.Properties;
import java.util.UUID;

/**
 * <b>Internal only - Do not use this class.</b> This class loads a native
 * library of snappy-java (snappyjava.dll, libsnappy.so, etc.) according to the
 * user platform (<i>os.name</i> and <i>os.arch</i>). The natively compiled
 * libraries bundled to snappy-java contain the codes of the original snappy and
 * JNI programs to access Snappy.
 * <p/>
 * In default, no configuration is required to use snappy-java, but you can load
 * your own native library created by 'make native' command.
 * <p/>
 * This SnappyLoader searches for native libraries (snappyjava.dll,
 * libsnappy.so, etc.) in the following order:
 * <ol>
 * <li>If system property <i>org.xerial.snappy.use.systemlib</i> is set to true,
 * lookup folders specified by <i>java.lib.path</i> system property (This is the
 * default path that JVM searches for native libraries)
 * <li>(System property: <i>org.xerial.snappy.lib.path</i>)/(System property:
 * <i>org.xerial.lib.name</i>)
 * <li>One of the libraries embedded in snappy-java-(version).jar extracted into
 * (System property: <i>java.io.tmpdir</i>). If
 * <i>org.xerial.snappy.tempdir</i> is set, use this folder instead of
 * <i>java.io.tmpdir</i>.
 * </ol>
 * <p/>
 * <p>
 * If you do not want to use folder <i>java.io.tmpdir</i>, set the System
 * property <i>org.xerial.snappy.tempdir</i>. For example, to use
 * <i>/tmp/leo</i> as a temporary folder to copy native libraries, use -D option
 * of JVM:
 * <p/>
 * <pre>
 * <code>
 * java -Dorg.xerial.snappy.tempdir="/tmp/leo" ...
 * </code>
 * </pre>
 * <p/>
 * </p>
 *
 * @author leo
 */
public class SnappyLoader
{
    public static final String SNAPPY_SYSTEM_PROPERTIES_FILE = "org-xerial-snappy.properties";
    public static final String KEY_SNAPPY_LIB_PATH = "org.xerial.snappy.lib.path";
    public static final String KEY_SNAPPY_LIB_NAME = "org.xerial.snappy.lib.name";
    public static final String KEY_SNAPPY_PUREJAVA = "org.xerial.snappy.purejava";
    public static final String KEY_SNAPPY_TEMPDIR = "org.xerial.snappy.tempdir";
    public static final String KEY_SNAPPY_USE_SYSTEMLIB = "org.xerial.snappy.use.systemlib";
    public static final String KEY_SNAPPY_DISABLE_BUNDLED_LIBS = "org.xerial.snappy.disable.bundled.libs"; // Depreciated, but preserved for backward compatibility

    private static boolean isLoaded = false;

    private static volatile SnappyApi snappyApi = null;
    private static volatile BitShuffleNative bitshuffleApi = null;

    private static File nativeLibFile = null;

    static void cleanUpExtractedNativeLib()
    {
        if (nativeLibFile != null && nativeLibFile.exists()) {
            boolean deleted = nativeLibFile.delete();
            if (!deleted) {
                // Deleting native lib has failed, but it's not serious so simply ignore it here
            }
            snappyApi = null;
            bitshuffleApi = null;
        }
    }

    /**
     * Set the `snappyApi` instance.
     *
     * @param apiImpl
     */
    static synchronized void setSnappyApi(SnappyApi apiImpl)
    {
        snappyApi = apiImpl;
    }

    /**
     * load system properties when configuration file of the name
     * {@link #SNAPPY_SYSTEM_PROPERTIES_FILE} is found
     */
    private static void loadSnappySystemProperties()
    {
        try {
            InputStream is = Thread.currentThread().getContextClassLoader()
                    .getResourceAsStream(SNAPPY_SYSTEM_PROPERTIES_FILE);

            if (is == null) {
                return; // no configuration file is found
            }

            // Load property file
            Properties props = new Properties();
            props.load(is);
            is.close();
            Enumeration<?> names = props.propertyNames();
            while (names.hasMoreElements()) {
                String name = (String) names.nextElement();
                if (name.startsWith("org.xerial.snappy.")) {
                    if (System.getProperty(name) == null) {
                        System.setProperty(name, props.getProperty(name));
                    }
                }
            }
        }
        catch (Throwable ex) {
            System.err.println("Could not load '" + SNAPPY_SYSTEM_PROPERTIES_FILE + "' from classpath: "
                    + ex.toString());
        }
    }

    static {
        loadSnappySystemProperties();
    }

    static synchronized SnappyApi loadSnappyApi()
    {
        if (snappyApi != null) {
            return snappyApi;
        }
        loadNativeLibrary();
        setSnappyApi(new SnappyNative());
        return snappyApi;
    }

    static synchronized BitShuffleNative loadBitShuffleApi()
    {
        if (bitshuffleApi != null) {
            return bitshuffleApi;
        }
        loadNativeLibrary();
        bitshuffleApi = new BitShuffleNative();
        return bitshuffleApi;
    }

    /**
     * Load a native library of snappy-java
     */
    private synchronized static void loadNativeLibrary()
    {
        if (!isLoaded) {
            try {
                nativeLibFile = findNativeLibrary();
                if (nativeLibFile != null) {
                    // Load extracted or specified snappyjava native library.
                    System.load(nativeLibFile.getAbsolutePath());
                } else {
                    // Load preinstalled snappyjava (in the path -Djava.library.path)
                    System.loadLibrary("snappyjava");
                }
            }
            catch (Exception e) {
                e.printStackTrace();
                throw new SnappyError(SnappyErrorCode.FAILED_TO_LOAD_NATIVE_LIBRARY, e.getMessage());
            }
            isLoaded = true;
        }
    }

    private static boolean contentsEquals(InputStream in1, InputStream in2)
            throws IOException
    {
        if (!(in1 instanceof BufferedInputStream)) {
            in1 = new BufferedInputStream(in1);
        }
        if (!(in2 instanceof BufferedInputStream)) {
            in2 = new BufferedInputStream(in2);
        }

        int ch = in1.read();
        while (ch != -1) {
            int ch2 = in2.read();
            if (ch != ch2) {
                return false;
            }
            ch = in1.read();
        }
        int ch2 = in2.read();
        return ch2 == -1;
    }

    /**
     * Extract the specified library file to the target folder
     *
     * @param libFolderForCurrentOS
     * @param libraryFileName
     * @param targetFolder
     * @return
     */
    private static File extractLibraryFile(String libFolderForCurrentOS, String libraryFileName, String targetFolder)
    {
        String nativeLibraryFilePath = libFolderForCurrentOS + "/" + libraryFileName;

        // Attach UUID to the native library file to ensure multiple class loaders can read the libsnappy-java multiple times.
        String uuid = UUID.randomUUID().toString();
        String extractedLibFileName = String.format("snappy-%s-%s-%s", getVersion(), uuid, libraryFileName);
        File extractedLibFile = new File(targetFolder, extractedLibFileName);

        try {
            // Extract a native library file into the target directory
            InputStream reader = null;
            FileOutputStream writer = null;
            try {
                reader = getResourceAsInputStream(nativeLibraryFilePath);
                try {
                    writer = new FileOutputStream(extractedLibFile);

                    byte[] buffer = new byte[8192];
                    int bytesRead = 0;
                    while ((bytesRead = reader.read(buffer)) != -1) {
                        writer.write(buffer, 0, bytesRead);
                    }
                }
                finally {
                    if (writer != null) {
                        writer.close();
                    }
                }
            }
            finally {
                if (reader != null) {
                    reader.close();
                }

                // Delete the extracted lib file on JVM exit.
                extractedLibFile.deleteOnExit();
            }

            // Set executable (x) flag to enable Java to load the native library
            boolean success = extractedLibFile.setReadable(true) &&
                    extractedLibFile.setWritable(true, true) &&
                    extractedLibFile.setExecutable(true);
            if (!success) {
                // Setting file flag may fail, but in this case another error will be thrown in later phase
            }

            // Check whether the contents are properly copied from the resource folder
            {
                InputStream nativeIn = null;
                InputStream extractedLibIn = null;
                try {
                    nativeIn = getResourceAsInputStream(nativeLibraryFilePath);
                    extractedLibIn = new FileInputStream(extractedLibFile);

                    if (!contentsEquals(nativeIn, extractedLibIn)) {
                        throw new SnappyError(SnappyErrorCode.FAILED_TO_LOAD_NATIVE_LIBRARY, String.format("Failed to write a native library file at %s", extractedLibFile));
                    }
                }
                finally {
                    if (nativeIn != null) {
                        nativeIn.close();
                    }
                    if (extractedLibIn != null) {
                        extractedLibIn.close();
                    }
                }
            }

            return new File(targetFolder, extractedLibFileName);
        }
        catch (IOException e) {
            e.printStackTrace(System.err);
            return null;
        }
    }

    static File findNativeLibrary()
    {

        boolean useSystemLib = Boolean.parseBoolean(System.getProperty(KEY_SNAPPY_USE_SYSTEMLIB, "false"));
        boolean disabledBundledLibs = Boolean
                .parseBoolean(System.getProperty(KEY_SNAPPY_DISABLE_BUNDLED_LIBS, "false"));
        if (useSystemLib || disabledBundledLibs) {
            return null; // Use a pre-installed libsnappyjava
        }

        // Try to load the library in org.xerial.snappy.lib.path  */
        String snappyNativeLibraryPath = System.getProperty(KEY_SNAPPY_LIB_PATH);
        String snappyNativeLibraryName = System.getProperty(KEY_SNAPPY_LIB_NAME);

        // Resolve the library file name with a suffix (e.g., dll, .so, etc.)
        if (snappyNativeLibraryName == null) {
            snappyNativeLibraryName = System.mapLibraryName("snappyjava");
        }

        if (snappyNativeLibraryPath != null) {
            File nativeLib = new File(snappyNativeLibraryPath, snappyNativeLibraryName);
            if (nativeLib.exists()) {
                return nativeLib;
            }
        }

        // Load an OS-dependent native library inside a jar file
        snappyNativeLibraryPath = "/org/xerial/snappy/native/" + OSInfo.getNativeLibFolderPathForCurrentOS();
        boolean hasNativeLib = hasResource(snappyNativeLibraryPath + "/" + snappyNativeLibraryName);
        if (!hasNativeLib) {
            if (OSInfo.getOSName().equals("Mac")) {
                // Fix for openjdk7 for Mac
                String altName = "libsnappyjava.dylib";
                if (hasResource(snappyNativeLibraryPath + "/" + altName)) {
                    snappyNativeLibraryName = altName;
                    hasNativeLib = true;
                }
            }
        }

        if (!hasNativeLib) {
            String errorMessage = String.format("no native library is found for os.name=%s and os.arch=%s", OSInfo.getOSName(), OSInfo.getArchName());
            throw new SnappyError(SnappyErrorCode.FAILED_TO_LOAD_NATIVE_LIBRARY, errorMessage);
        }

        // Temporary folder for the native lib. Use the value of org.xerial.snappy.tempdir or java.io.tmpdir
        File tempFolder = new File(System.getProperty(KEY_SNAPPY_TEMPDIR, System.getProperty("java.io.tmpdir")));
        if (!tempFolder.exists()) {
            boolean created = tempFolder.mkdirs();
            if (!created) {
                // if created == false, it will fail eventually in the later part
            }
        }

        // Extract and load a native library inside the jar file
        return extractLibraryFile(snappyNativeLibraryPath, snappyNativeLibraryName, tempFolder.getAbsolutePath());
    }

    private static boolean hasResource(String path)
    {
        return SnappyLoader.class.getResource(path) != null;
    }

    /**
     * Get the snappy-java version by reading pom.properties embedded in jar.
     * This version data is used as a suffix of a dll file extracted from the
     * jar.
     *
     * @return the version string
     */
    public static String getVersion()
    {

        URL versionFile = SnappyLoader.class
                .getResource("/META-INF/maven/org.xerial.snappy/snappy-java/pom.properties");
        if (versionFile == null) {
            versionFile = SnappyLoader.class.getResource("/org/xerial/snappy/VERSION");
        }

        String version = "unknown";
        try {
            if (versionFile != null) {
                Properties versionData = new Properties();
                versionData.load(versionFile.openStream());
                version = versionData.getProperty("version", version);
                if (version.equals("unknown")) {
                    version = versionData.getProperty("SNAPPY_VERSION", version);
                }
                version = version.trim().replaceAll("[^0-9M\\.]", "");
            }
        }
        catch (IOException e) {
            System.err.println(e);
        }
        return version;
    }

    private static InputStream getResourceAsInputStream(String resourcePath) throws IOException {
        URL url = SnappyLoader.class.getResource(resourcePath);
        URLConnection connection = url.openConnection();
        if (connection instanceof JarURLConnection) {
            JarURLConnection jarConnection = (JarURLConnection) connection;
            jarConnection.setUseCaches(false);  // workaround for https://bugs.openjdk.org/browse/JDK-8205976
            return jarConnection.getInputStream();
        } else {
            return connection.getInputStream();
        }
    }
}
