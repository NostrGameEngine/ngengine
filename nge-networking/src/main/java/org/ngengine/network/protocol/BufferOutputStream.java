package org.ngengine.network.protocol;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;

public class BufferOutputStream extends OutputStream {

    ByteBuffer output;

    public BufferOutputStream(ByteBuffer output) {
        this.output = output;
    }

    @Override
    public void write(int b) throws IOException {
        output.put((byte) b);
    }

    @Override
    public void write(byte[] b) {
        output.put(b);
    }

    @Override
    public void write(byte[] b, int off, int len) {
        output.put(b, off, len);
    }
}

