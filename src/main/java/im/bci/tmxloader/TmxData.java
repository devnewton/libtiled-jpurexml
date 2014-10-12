/*
 The MIT License (MIT)

 Copyright (c) 2013 devnewton <devnewton@bci.im>

 Permission is hereby granted, free of charge, to any person obtaining a copy
 of this software and associated documentation files (the "Software"), to deal
 in the Software without restriction, including without limitation the rights
 to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 copies of the Software, and to permit persons to whom the Software is
 furnished to do so, subject to the following conditions:

 The above copyright notice and this permission notice shall be included in
 all copies or substantial portions of the Software.

 THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 THE SOFTWARE.
 */
package im.bci.tmxloader;

import java.io.*;
import java.nio.*;
import java.util.zip.*;

import javax.xml.bind.DatatypeConverter;

/**
 *
 * @author devnewton
 * @author Casper Faergemand (shorttail at gmail dot com)
 */
public class TmxData {

    private String encoding;
    private String compression;
    private String data;

    public String getEncoding() {
        return encoding;
    }

    public void setEncoding(String encoding) {
        this.encoding = encoding;
    }

    public String getCompression() {
        return compression;
    }

    public void setCompression(String compression) {
        this.compression = compression;
    }

    public String getData() {
        return data;
    }

    public void setData(String data) {
        this.data = data;
    }

    public void decodeTo(int width, int height, int[][] data) {
        if ("csv".equals(encoding)) {
            decodeCsvTo(width, height, data);
        } else if ("base64".equals(encoding)) {
            decodeBase64To(width, height, data);
        } else {
            throw new RuntimeException("Unsupported tiled layer data encoding: " + encoding);
        }
    }

    private void decodeBase64To(int width, int height, int[][] gidArray) {
        byte[] decodedRaw = DatatypeConverter.parseBase64Binary(getData());
        if (compression == null) {
            // Do nothing.
        } else if ("gzip".equals(compression)) {
            try {
                ByteArrayInputStream bais = new ByteArrayInputStream(decodedRaw);
                GZIPInputStream gis = new GZIPInputStream(bais);
                DataInputStream dis = new DataInputStream(gis);
                byte[] uncompressedRaw = new byte[height * width * 4];
                dis.readFully(uncompressedRaw);
                decodedRaw = uncompressedRaw;
            } catch (IOException e) {
                throw new RuntimeException("Gzip decompression failed.", e);
            }
        } else if ("zlib".equals(compression)) {
            try {
                Inflater inflater = new Inflater();
                inflater.setInput(decodedRaw);
                byte[] uncompressedRaw = new byte[height * width * 4];
                inflater.inflate(uncompressedRaw);
                decodedRaw = uncompressedRaw;
            } catch (DataFormatException e) {
                throw new RuntimeException("Zlib decompression failed.", e);
            }
        } else {
            throw new RuntimeException("Unsupported compression: " + getCompression());
        }
        IntBuffer buffer = ByteBuffer.wrap(decodedRaw).order(ByteOrder.LITTLE_ENDIAN).asIntBuffer();
        int[] decodedInts = new int[buffer.remaining()];
        buffer.get(decodedInts);
        int i = 0;
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                gidArray[x][y] = decodedInts[i++];
            }
        }
    }

    private void decodeCsvTo(int width, int height, int[][] gidArray) {
        String[] values = this.data.replaceAll("[\\s]", "").split(",");
        int index = 0;
        for (int y = 0; y < height; ++y) {
            for (int x = 0; x < width; ++x) {
                String str = values[index++];
                gidArray[x][y] = Integer.parseInt(str);
            }
        }
    }
}
