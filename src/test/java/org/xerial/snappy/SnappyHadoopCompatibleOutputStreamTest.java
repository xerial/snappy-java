package org.xerial.snappy;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.io.compress.SnappyCodec;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import java.io.*;
import java.lang.reflect.Field;
import java.util.LinkedHashMap;
import java.util.Map;

public class SnappyHadoopCompatibleOutputStreamTest
{
    private static File tempNativeLibFolder;

    @BeforeClass
    public static void loadHadoopNativeLibrary() throws Exception
    {
        final String libResourceFolder;
        Map<String, String> libraryNames = new LinkedHashMap<>();
        if (OSInfo.getOSName() == "Linux") {
            libResourceFolder = "/lib/Linux";
            libraryNames.put("libhadoop.so", "libhadoop.so");
            // certain Linux systems need these shared library be copied before the JVM started, see build.sbt
            libraryNames.put("libsnappy.so", "libsnappy.so");
            libraryNames.put("libsnappy.so.1", "libsnappy.so");
        } else if (OSInfo.getOSName() == "Mac") {
            libResourceFolder = "/lib/MacOSX";
            libraryNames.put("libhadoop.dylib", "libhadoop.dylib");
            libraryNames.put("libsnappy.dylib", "libsnappy.dylib");
            libraryNames.put("libsnappy.1.dylib", "libsnappy.dylib");
        } else {
            return; // not support
        }

        String testLibDir = System.getenv("XERIAL_SNAPPY_LIB");

        tempNativeLibFolder = new File(testLibDir);
        tempNativeLibFolder.mkdirs();

        for (Map.Entry<String, String> entry : libraryNames.entrySet()) {
            copyNativeLibraryToFS(libResourceFolder, entry.getValue(), entry.getKey());
        }

        System.setProperty("java.library.path", tempNativeLibFolder.getAbsolutePath());

        // credit: https://stackoverflow.com/questions/15409223/adding-new-paths-for-native-libraries-at-runtime-in-java
        //set sys_paths to null so that java.library.path will be reevalueted next time it is needed
        final Field sysPathsField = ClassLoader.class.getDeclaredField("sys_paths");
        sysPathsField.setAccessible(true);
        sysPathsField.set(null, null);
    }

    private static void copyNativeLibraryToFS(String libResourceFolder, String libraryName, String toLibraryName) {
        final String libraryResourceName = libResourceFolder + "/" + libraryName;
        final File libraryPath = new File(tempNativeLibFolder, toLibraryName);
        try (InputStream inputStream = SnappyHadoopCompatibleOutputStream.class.getResourceAsStream(libraryResourceName);
             FileOutputStream outputStream = new FileOutputStream(libraryPath)) {
            IOUtils.copy(inputStream, outputStream);
            FileDescriptor fd = outputStream.getFD();
            fd.sync();
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    @AfterClass
    public static void cleanUpLibraryFolder()
    {
        FileUtils.deleteQuietly(tempNativeLibFolder);
    }

    @Ignore("This test doesn't work with openjdk11 in GitHub Action")
    @Test
    public void testXerialCompressionHadoopDecompressionCodec() throws Exception
    {
        String os = OSInfo.getOSName();
        String arch = OSInfo.getArchName();
        if(!((os.equals("Linux") || os.equals("Mac")) && arch.equals("x86_64"))) {
           // This test works only in Linux/Mac x86_64
            System.err.println("SnappyHadoopCompatibleOutputStreamTest works only in 64-bit Linux/Mac");
            return;
        }

        File inputFile = File.createTempFile("TEST_hadoop_compatibility", ".txt");
        File snappyFile = File.createTempFile("TEST_hadoop_compatibility", ".snappy");
        InputStream snappyInput = null;
        FileOutputStream outputStream = new FileOutputStream(inputFile);
        try {
            String text = "";
            for (int i = 0; i < 1024; i++) {
                text += "Some long long strings to be compressed. Some long long strings to be compressed.";
            }
            text += "odd bytes";
            final byte[] bytes = text.getBytes("UTF-8");
            outputStream.write(bytes);
            outputStream.flush();
            outputStream.close();

            compress(inputFile, snappyFile);

            // Test using Hadoop's Snappy Codec
            if (tempNativeLibFolder != null) {
                SnappyCodec hadoopCodec = new SnappyCodec();
                hadoopCodec.setConf(new Configuration());
                snappyInput = hadoopCodec.createInputStream(new FileInputStream(snappyFile));
                byte[] buf = new byte[bytes.length];
                int byteRead = IOUtils.read(snappyInput, buf);
                String decompressed = new String(buf, 0, byteRead, "UTF-8");
                Assert.assertEquals(decompressed, text);
            } else {
                System.err.println("WARNING: no hadoop library for this platform. skip hadoop decompression test");
            }
        } finally {
            if (snappyInput != null) {
                snappyInput.close();
            }
            inputFile.delete();
            snappyFile.delete();
            outputStream.close();
        }
    }

    private void compress(File inputPath, File outputPath) throws Exception
    {
        FileInputStream fileInputStream = new FileInputStream(inputPath);
        FileOutputStream fileOutputStream = new FileOutputStream(outputPath);
        try {
            InputStream inputStream = new BufferedInputStream(fileInputStream);
            OutputStream outputStream = new SnappyHadoopCompatibleOutputStream(fileOutputStream);
            int readCount;
            byte[] buffer = new byte[64 * 1024];
            while ((readCount = inputStream.read(buffer)) > 0) {
                outputStream.write(buffer, 0, readCount);
            }
            inputStream.close();
            outputStream.close();
        } finally {
            fileInputStream.close();
            fileOutputStream.close();
        }
    }
}
