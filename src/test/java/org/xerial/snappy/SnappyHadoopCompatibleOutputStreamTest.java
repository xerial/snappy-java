package org.xerial.snappy;

import java.io.*;
import java.lang.reflect.Field;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.SystemUtils;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.io.compress.SnappyCodec;
import org.junit.*;

public class SnappyHadoopCompatibleOutputStreamTest
{

    private static File tempNativeLibFolder;

    @BeforeClass
    public static void loadHadoopNativeLibrary() throws Exception
    {
        final String libResourceFolder;

        if (SystemUtils.IS_OS_LINUX) {
            libResourceFolder = "/lib/Linux";
        } else if (SystemUtils.IS_OS_MAC_OSX) {
            libResourceFolder = "/lib/MacOSX";
        } else {
            return; // not support
        }

        final String libraryName = System.mapLibraryName("hadoop");
        final String libraryResourceName = libResourceFolder + "/" + libraryName;
        tempNativeLibFolder = File.createTempFile(SnappyHadoopCompatibleOutputStreamTest.class.getSimpleName(),
                ".libhadoop");
        tempNativeLibFolder.delete();
        tempNativeLibFolder.mkdirs();

        final File libraryPath = new File(tempNativeLibFolder, libraryName);
        try (InputStream inputStream = SnappyHadoopCompatibleOutputStream.class.getResourceAsStream(libraryResourceName);
             OutputStream outputStream = new FileOutputStream(libraryPath)) {
            IOUtils.copy(inputStream, outputStream);
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }

        System.setProperty("java.library.path", tempNativeLibFolder.getAbsolutePath());

        // credit: https://stackoverflow.com/questions/15409223/adding-new-paths-for-native-libraries-at-runtime-in-java
        //set sys_paths to null so that java.library.path will be reevalueted next time it is needed
        final Field sysPathsField = ClassLoader.class.getDeclaredField("sys_paths");
        sysPathsField.setAccessible(true);
        sysPathsField.set(null, null);
    }

    @AfterClass
    public static void cleanUpLibraryFolder()
    {
        FileUtils.deleteQuietly(tempNativeLibFolder);
    }

    @Test
    public void testXerialCompressionHadoopDecompressionCodec() throws Exception
    {
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
