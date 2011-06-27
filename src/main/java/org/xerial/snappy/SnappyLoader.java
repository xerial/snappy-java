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

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.net.URL;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.ProtectionDomain;
import java.util.ArrayList;
import java.util.List;
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
 * <li>Folders specified by java.lib.path system property (This is the default
 * path that JVM searches for native libraries)
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
public class SnappyLoader
{
    private static boolean         isInitialized = false;
    private static boolean         isLoaded      = false;
    private static SnappyNativeAPI api           = null;

    private static ClassLoader getRootClassLoader() {
        ClassLoader cl = SnappyLoader.class.getClassLoader();
        while (cl.getParent() != null) {
            cl = cl.getParent();
        }
        return cl;
    }

    private static byte[] getByteCode(String resourcePath) throws IOException {

        InputStream in = SnappyLoader.class.getResourceAsStream(resourcePath);
        if (in == null)
            throw new IOException(resourcePath + " is not found");
        byte[] buf = new byte[1024];
        ByteArrayOutputStream byteCodeBuf = new ByteArrayOutputStream();
        for (int readLength; (readLength = in.read(buf)) != -1;) {
            byteCodeBuf.write(buf, 0, readLength);
        }
        in.close();

        return byteCodeBuf.toByteArray();
    }

    public static boolean isNativeLibraryLoaded() {
        return isLoaded;
    }

    static SnappyNativeAPI load() {

        if (isInitialized)
            return api;

        isInitialized = true;
        final String nativeLoaderClassName = "org.xerial.snappy.SnappyNativeLoader";
        final String[] classesToPreload = new String[] { "org.xerial.snappy.SnappyNativeAPI",
                "org.xerial.snappy.SnappyNative", "org.xerial.snappy.SnappyErrorCode" };

        try {
            Class< ? > c = Class.forName(nativeLoaderClassName);
            // If this native loader class is already defined, it means that another class loader already loaded the native library of snappy
            api = (SnappyNativeAPI) Class.forName("org.xerial.snappy.SnappyNative").newInstance();
            isLoaded = true;
            return api;
        }
        catch (ClassNotFoundException e) {
            try {
                // Load a byte code 
                byte[] byteCode = getByteCode("/org/xerial/snappy/SnappyNativeLoader.bytecode");
                // In addition, we need to load the other dependent classes (e.g., SnappyNative and SnappyException) using the system class loader
                List<byte[]> preloadClassByteCode = new ArrayList<byte[]>(classesToPreload.length);
                for (String each : classesToPreload) {
                    preloadClassByteCode.add(getByteCode(String.format("/%s.class", each.replaceAll("\\.", "/"))));
                }

                // Create a new class from a byte code
                Class< ? > classLoader = Class.forName("java.lang.ClassLoader");
                Method defineClass = classLoader.getDeclaredMethod("defineClass", new Class[] { String.class,
                        byte[].class, int.class, int.class, ProtectionDomain.class });

                // Use parent class loader to load SnappyNative, since Tomcat, which uses different class loaders for each webapps, cannot load JNI interface twice  
                ClassLoader systemClassLoader = getRootClassLoader();
                ProtectionDomain pd = System.class.getProtectionDomain();

                // ClassLoader.defineClass is a protected method, so we have to make it accessible
                defineClass.setAccessible(true);
                try {
                    // Create a new class using a ClassLoader#defineClass
                    defineClass.invoke(systemClassLoader, nativeLoaderClassName, byteCode, 0, byteCode.length, pd);

                    for (int i = 0; i < classesToPreload.length; ++i) {
                        byte[] b = preloadClassByteCode.get(i);
                        defineClass.invoke(systemClassLoader, classesToPreload[i], b, 0, b.length, pd);
                    }
                }
                finally {
                    // Reset the accessibility to defineClass method
                    defineClass.setAccessible(false);
                }

                // Load the snappy loader class
                Class< ? > loaderClass = systemClassLoader.loadClass(nativeLoaderClassName);
                if (loaderClass != null) {

                    File nativeLib = findNativeLibrary();
                    if (nativeLib != null) {
                        // Load extracted or specified snappyjava native library. 
                        Method loadMethod = loaderClass.getDeclaredMethod("load", new Class[] { String.class });
                        loadMethod.invoke(null, nativeLib.getAbsolutePath());
                    }
                    else {
                        // Load preinstalled snappyjava (in the path -Djava.library.path) 
                        Method loadMethod = loaderClass.getDeclaredMethod("loadLibrary", new Class[] { String.class });
                        loadMethod.invoke(null, "snappyjava");
                    }

                    // And also, preload the other dependent classes 
                    for (String each : classesToPreload) {
                        systemClassLoader.loadClass(each);
                    }
                    isLoaded = true;

                    api = (SnappyNativeAPI) Class.forName("org.xerial.snappy.SnappyNative").newInstance();
                    return api;
                }
            }
            catch (ClassNotFoundException ee) {
                throw new SnappyError(SnappyErrorCode.FAILED_TO_LOAD_NATIVE_LIBRARY, ee.getMessage());
            }
            catch (Exception e2) {
                throw new SnappyError(SnappyErrorCode.FAILED_TO_LOAD_NATIVE_LIBRARY, e.getMessage());
            }
        }
        catch (Exception e) {
            throw new SnappyError(SnappyErrorCode.FAILED_TO_LOAD_NATIVE_LIBRARY, e.getMessage());
        }

        throw new SnappyError(SnappyErrorCode.FAILED_TO_LOAD_NATIVE_LIBRARY);
    }

    public static final String KEY_SNAPPY_LIB_PATH             = "org.xerial.snappy.lib.path";
    public static final String KEY_SNAPPY_LIB_NAME             = "org.xerial.snappy.lib.name";
    public static final String KEY_SNAPPY_TEMPDIR              = "org.xerial.snappy.tempdir";
    public static final String KEY_SNAPPY_DISABLE_BUNDLED_LIBS = "org.xerial.snappy.disable.bundled.libs";

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
    private static File extractLibraryFile(String libFolderForCurrentOS, String libraryFileName, String targetFolder) {
        String nativeLibraryFilePath = libFolderForCurrentOS + "/" + libraryFileName;
        final String prefix = "snappy-" + getVersion() + "-";
        String extractedLibFileName = prefix + libraryFileName;
        File extractedLibFile = new File(targetFolder, extractedLibFileName);

        try {
            if (extractedLibFile.exists()) {
                // test md5sum value
                String md5sum1 = md5sum(SnappyLoader.class.getResourceAsStream(nativeLibraryFilePath));
                String md5sum2 = md5sum(new FileInputStream(extractedLibFile));

                if (md5sum1.equals(md5sum2)) {
                    return new File(targetFolder, extractedLibFileName);
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

            // Extract a native library file into the target directory
            InputStream reader = SnappyLoader.class.getResourceAsStream(nativeLibraryFilePath);
            FileOutputStream writer = new FileOutputStream(extractedLibFile);
            byte[] buffer = new byte[8192];
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

            return new File(targetFolder, extractedLibFileName);
        }
        catch (IOException e) {
            e.printStackTrace(System.err);
            return null;
        }
    }

    static File findNativeLibrary() {
        // Try to load the library from org.xerial.snappy.lib.path library path */
        String snappyNativeLibraryPath = System.getProperty(KEY_SNAPPY_LIB_PATH);
        String snappyNativeLibraryName = System.getProperty(KEY_SNAPPY_LIB_NAME);

        if (System.getProperty(KEY_SNAPPY_DISABLE_BUNDLED_LIBS, "false").equals("false")) {
            // Resolve the library file name with a suffix (e.g., dll, .so, etc.) 
            if (snappyNativeLibraryName == null)
                snappyNativeLibraryName = System.mapLibraryName("snappyjava");

            if (snappyNativeLibraryPath != null) {
                File nativeLib = new File(snappyNativeLibraryPath, snappyNativeLibraryName);
                if (nativeLib.exists())
                    return nativeLib;
            }
        }

        {
            // Load an OS-dependent native library inside a jar file
            snappyNativeLibraryPath = "/org/xerial/snappy/native/" + OSInfo.getNativeLibFolderPathForCurrentOS();

            if (SnappyLoader.class.getResource(snappyNativeLibraryPath + "/" + snappyNativeLibraryName) != null) {
                // Temporary library folder. Use the value of org.xerial.snappy.tempdir or java.io.tmpdir
                String tempFolder = new File(System.getProperty(KEY_SNAPPY_TEMPDIR,
                        System.getProperty("java.io.tmpdir"))).getAbsolutePath();

                // Extract and load a native library inside the jar file
                return extractLibraryFile(snappyNativeLibraryPath, snappyNativeLibraryName, tempFolder);
            }
        }

        return null;
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

        URL versionFile = SnappyLoader.class
                .getResource("/META-INF/maven/org.xerial.snappy/snappy-java/pom.properties");
        if (versionFile == null)
            versionFile = SnappyLoader.class.getResource("/org/xerial/snappy/VERSION");

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
