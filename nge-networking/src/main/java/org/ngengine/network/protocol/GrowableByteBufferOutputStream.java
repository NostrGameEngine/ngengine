package org.ngengine.network.protocol;

import java.io.IOException;
import java.io.OutputStream;

public class GrowableByteBufferOutputStream extends OutputStream {

    GrowableByteBuffer output;

    public GrowableByteBufferOutputStream(GrowableByteBuffer output) {
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