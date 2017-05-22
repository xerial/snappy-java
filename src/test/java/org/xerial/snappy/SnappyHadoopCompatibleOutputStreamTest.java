package org.xerial.snappy;

import java.io.*;

import org.apache.commons.io.IOUtils;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.io.compress.SnappyCodec;
import org.junit.Assert;
import org.junit.Test;

public class SnappyHadoopCompatibleOutputStreamTest
{

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

            try {
                SnappyCodec hadoopCodec = new SnappyCodec();
                hadoopCodec.setConf(new Configuration());
                snappyInput = hadoopCodec.createInputStream(new FileInputStream(snappyFile));
                byte[] buf = new byte[bytes.length];
                int byteRead = IOUtils.read(snappyInput, buf);
                String decompressed = new String(buf, 0, byteRead, "UTF-8");
                Assert.assertEquals(decompressed, text);
            } catch (UnsatisfiedLinkError e) {
                System.err.println("WARNING: missing hadoop native library. Hadoop decompression test skipped");
                System.err.println("WARNING: error message: " + e.getMessage());
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
