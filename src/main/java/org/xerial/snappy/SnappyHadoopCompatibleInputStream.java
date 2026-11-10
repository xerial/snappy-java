package org.xerial.snappy;

import java.io.IOException;
import java.io.InputStream;

public class SnappyHadoopCompatibleInputStream extends InputStream {

    private final InputStream in;

    private byte[] header = new byte[4];
    private byte[] compressed;
    private byte[] uncompressed;
    private int uncompressedLimit = 0;
    private int uncompressedCursor = 0;

    public SnappyHadoopCompatibleInputStream(InputStream in) {
        this.in = in;
    }

    @Override
    public int read() throws IOException {
        if (uncompressed != null && uncompressedCursor < uncompressedLimit) {
            return uncompressed[uncompressedCursor++] & 0xFF;
        }

        int ignore = in.read(header, 0, 4);

        if (ignore == -1)
            return -1;

        while (ignore < 4) {
            ignore += in.read(header, ignore, 4 - ignore);
        }

        int readBytes = in.read(header, 0, 4);

        if (readBytes == -1)
            return -1;

        while (readBytes < 4) {
            readBytes += in.read(header, readBytes, 4 - readBytes);
        }


        int chunkSize = SnappyOutputStream.readInt(header, 0);
        if (null == this.compressed || this.compressed.length < chunkSize)
            compressed = new byte[chunkSize];

        int chunkReadBytes = in.read(compressed, 0, chunkSize);

        if (chunkReadBytes == -1)
            return -1;

        while (chunkReadBytes < chunkSize) {
            chunkReadBytes += in.read(compressed, chunkReadBytes, chunkSize - chunkReadBytes);
        }

        if (chunkReadBytes != chunkSize) {
            throw new IOException("Cannot decompress input stream");
        }

        if (!Snappy.isValidCompressedBuffer(compressed, 0, chunkSize))
            throw new IOException("invalid input stream");

        int length = Snappy.uncompressedLength(compressed, 0, chunkSize);
        this.uncompressedLimit = length;
        if (null == this.uncompressed || this.uncompressed.length < length)
            this.uncompressed = new byte[length];
        uncompressedCursor = 0;

        Snappy.uncompress(compressed, 0, chunkSize, uncompressed, 0);

        return read();
    }

}
