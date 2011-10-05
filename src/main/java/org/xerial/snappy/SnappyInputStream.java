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
// XerialJ
//
// SnappyInputStream.java
// Since: 2011/03/31 20:14:56
//
// $URL$
// $Author$
//--------------------------------------
package org.xerial.snappy;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * A stream filter for reading data compressed by {@link SnappyOutputStream}.
 * 
 * 
 * @author leo
 * 
 */
public class SnappyInputStream extends InputStream
{
    private boolean             finishedReading    = false;
    protected final InputStream in;

    private byte[]              compressed;
    private byte[]              uncompressed;
    private int                 uncompressedCursor = 0;
    private int                 uncompressedLimit  = 0;

    private byte[]              chunkSizeBuf       = new byte[4];

    /**
     * Create a filter for reading compressed data as a uncompressed stream
     * 
     * @param input
     * @throws IOException
     */
    public SnappyInputStream(InputStream input) throws IOException {
        this.in = input;
        readHeader();
    }

    /**
     * Close the stream
     */
    /* (non-Javadoc)
     * @see java.io.InputStream#close()
     */
    @Override
    public void close() throws IOException {
        compressed = null;
        uncompressed = null;
        if (in != null)
            in.close();
    }

    protected void readHeader() throws IOException {
        byte[] header = new byte[SnappyCodec.headerSize()];
        int readBytes = 0;
        while (readBytes < header.length) {
            int ret = in.read(header, readBytes, header.length - readBytes);
            if (ret == -1)
                break;
            readBytes += ret;
        }

        // Quick test of the header 
        if (readBytes < header.length || header[0] != SnappyCodec.MAGIC_HEADER[0]) {
            // do the default uncompression
            readFully(header, readBytes);
            return;
        }

        SnappyCodec codec = SnappyCodec.readHeader(new ByteArrayInputStream(header));
        if (codec.isValidMagicHeader()) {
            // The input data is compressed by SnappyOutputStream
            if (codec.version < SnappyCodec.MINIMUM_COMPATIBLE_VERSION) {
                throw new IOException(String.format(
                        "compressed with imcompatible codec version %d. At least version %d is required",
                        codec.version, SnappyCodec.MINIMUM_COMPATIBLE_VERSION));
            }
        }
        else {
            // (probably) compressed by Snappy.compress(byte[])
            readFully(header, readBytes);
            return;
        }
    }

    protected void readFully(byte[] fragment, int fragmentLength) throws IOException {
        // read the entire input data to the buffer 
        compressed = new byte[Math.max(8 * 1024, fragmentLength)]; // 8K
        System.arraycopy(fragment, 0, compressed, 0, fragmentLength);
        int cursor = fragmentLength;
        for (int readBytes = 0; (readBytes = in.read(compressed, cursor, compressed.length - cursor)) != -1;) {
            cursor += readBytes;
            if (cursor >= compressed.length) {
                byte[] newBuf = new byte[(compressed.length * 2)];
                System.arraycopy(compressed, 0, newBuf, 0, compressed.length);
                compressed = newBuf;
            }
        }

        finishedReading = true;

        // Uncompress
        int uncompressedLength = Snappy.uncompressedLength(compressed, 0, cursor);
        uncompressed = new byte[uncompressedLength];
        Snappy.uncompress(compressed, 0, cursor, uncompressed, 0);
        this.uncompressedCursor = 0;
        this.uncompressedLimit = uncompressedLength;

    }

    /**
     * Reads up to len bytes of data from the input stream into an array of
     * bytes.
     */
    /* (non-Javadoc)
     * @see java.io.InputStream#read(byte[], int, int)
     */
    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        return rawRead(b, off, len);
    }

    /**
     * Read uncompressed data into the specified array
     * 
     * @param array
     * @param byteOffset
     * @param byteLength
     * @return written bytes
     * @throws IOException
     */
    public int rawRead(Object array, int byteOffset, int byteLength) throws IOException {
        int writtenBytes = 0;
        for (; writtenBytes < byteLength;) {
            if (uncompressedCursor >= uncompressedLimit) {
                if (hasNextChunk())
                    continue;
                else {
                    return writtenBytes == 0 ? -1 : writtenBytes;
                }
            }
            int bytesToWrite = Math.min(uncompressedLimit - uncompressedCursor, byteLength - writtenBytes);
            Snappy.arrayCopy(uncompressed, uncompressedCursor, bytesToWrite, array, byteOffset + writtenBytes);
            writtenBytes += bytesToWrite;
            uncompressedCursor += bytesToWrite;
        }

        return writtenBytes;
    }

    /**
     * Read long array from the stream
     * 
     * @param d
     *            input
     * @param off
     *            offset
     * @param len
     *            the number of long elements to read
     * @return the total number of bytes read into the buffer, or -1 if there is
     *         no more data because the end of the stream has been reached.
     * @throws IOException
     */
    public int read(long[] d, int off, int len) throws IOException {
        return rawRead(d, off * 8, len * 8);
    }

    /**
     * Read long array from the stream
     * 
     * @param d
     * @return the total number of bytes read into the buffer, or -1 if there is
     *         no more data because the end of the stream has been reached.
     * @throws IOException
     */
    public int read(long[] d) throws IOException {
        return read(d, 0, d.length);
    }

    /**
     * Read double array from the stream
     * 
     * @param d
     *            input
     * @param off
     *            offset
     * @param len
     *            the number of double elements to read
     * @return the total number of bytes read into the buffer, or -1 if there is
     *         no more data because the end of the stream has been reached.
     * @throws IOException
     */
    public int read(double[] d, int off, int len) throws IOException {
        return rawRead(d, off * 8, len * 8);
    }

    /**
     * Read double array from the stream
     * 
     * @param d
     * @return the total number of bytes read into the buffer, or -1 if there is
     *         no more data because the end of the stream has been reached.
     * @throws IOException
     */
    public int read(double[] d) throws IOException {
        return read(d, 0, d.length);
    }

    /**
     * Read int array from the stream
     * 
     * @param d
     * @return the total number of bytes read into the buffer, or -1 if there is
     *         no more data because the end of the stream has been reached.
     * @throws IOException
     */
    public int read(int[] d) throws IOException {
        return read(d, 0, d.length);
    }

    /**
     * Read int array from the stream
     * 
     * @param d
     *            input
     * @param off
     *            offset
     * @param len
     *            the number of int elements to read
     * @return the total number of bytes read into the buffer, or -1 if there is
     *         no more data because the end of the stream has been reached.
     * @throws IOException
     */
    public int read(int[] d, int off, int len) throws IOException {
        return rawRead(d, off * 4, len * 4);
    }

    /**
     * Read float array from the stream
     * 
     * @param d
     *            input
     * @param off
     *            offset
     * @param len
     *            the number of float elements to read
     * @return the total number of bytes read into the buffer, or -1 if there is
     *         no more data because the end of the stream has been reached.
     * @throws IOException
     */
    public int read(float[] d, int off, int len) throws IOException {
        return rawRead(d, off * 4, len * 4);
    }

    /**
     * Read float array from the stream
     * 
     * @param d
     * @return the total number of bytes read into the buffer, or -1 if there is
     *         no more data because the end of the stream has been reached.
     * @throws IOException
     */
    public int read(float[] d) throws IOException {
        return read(d, 0, d.length);
    }

    /**
     * Read short array from the stream
     * 
     * @param d
     *            input
     * @param off
     *            offset
     * @param len
     *            the number of short elements to read
     * @return the total number of bytes read into the buffer, or -1 if there is
     *         no more data because the end of the stream has been reached.
     * @throws IOException
     */
    public int read(short[] d, int off, int len) throws IOException {
        return rawRead(d, off * 2, len * 2);
    }

    /**
     * Read short array from the stream
     * 
     * @param d
     * @return the total number of bytes read into the buffer, or -1 if there is
     *         no more data because the end of the stream has been reached.
     * @throws IOException
     */
    public int read(short[] d) throws IOException {
        return read(d, 0, d.length);
    }

    protected boolean hasNextChunk() throws IOException {
        if (finishedReading)
            return false;

        uncompressedCursor = 0;
        uncompressedLimit = 0;

        int readBytes = 0;
        while (readBytes < 4) {
            int ret = in.read(chunkSizeBuf, readBytes, 4 - readBytes);
            if (ret == -1) {
                finishedReading = true;
                return false;
            }
            readBytes += ret;
        }
        int chunkSize = SnappyOutputStream.readInt(chunkSizeBuf, 0);
        // extend the compressed data buffer size
        if (compressed == null || chunkSize > compressed.length) {
            compressed = new byte[chunkSize];
        }
        readBytes = 0;
        while (readBytes < chunkSize) {
            int ret = in.read(compressed, readBytes, chunkSize - readBytes);
            if (ret == -1)
                break;
            readBytes += ret;
        }
        if (readBytes < chunkSize) {
            throw new IOException("failed to read chunk");
        }
        try {
            int uncompressedLength = Snappy.uncompressedLength(compressed, 0, chunkSize);
            if (uncompressed == null || uncompressedLength > uncompressed.length) {
                uncompressed = new byte[uncompressedLength];
            }
            int actualUncompressedLength = Snappy.uncompress(compressed, 0, chunkSize, uncompressed, 0);
            if (uncompressedLength != actualUncompressedLength) {
                throw new IOException("invalid uncompressed byte size");
            }
            uncompressedLimit = actualUncompressedLength;
        }
        catch (IOException e) {
            throw new IOException("failed to uncompress the chunk: " + e.getMessage());
        }

        return true;
    }

    /**
     * Reads the next byte of uncompressed data from the input stream. The value
     * byte is returned as an int in the range 0 to 255. If no byte is available
     * because the end of the stream has been reached, the value -1 is returned.
     * This method blocks until input data is available, the end of the stream
     * is detected, or an exception is thrown.
     */
    /* (non-Javadoc)
     * @see java.io.InputStream#read()
     */
    @Override
    public int read() throws IOException {
        if (uncompressedCursor < uncompressedLimit) {
            return uncompressed[uncompressedCursor++] & 0xFF;
        }
        else {
            if (hasNextChunk())
                return read();
            else
                return -1;
        }
    }

}
