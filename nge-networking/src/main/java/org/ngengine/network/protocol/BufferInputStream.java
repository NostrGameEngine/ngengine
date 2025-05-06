package org.ngengine.network.protocol;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

public class BufferInputStream extends InputStream {

    ByteBuffer input;

    public BufferInputStream(ByteBuffer input) {
        this.input = input;
    }

    @Override
    public int read() throws IOException {
        if (input.remaining() == 0) return -1;
        else return input.get() & 0xff;
    }

    @Override
    public int read(byte[] b) {
        return read(b, 0, b.length);
    }

    @Override
    public int read(byte[] b, int off, int len) {
        int toRead = len > input.remaining() ? input.remaining() : len;
        input.get(b, off, toRead);
        return toRead;
    }

}