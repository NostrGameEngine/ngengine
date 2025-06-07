/**
 * Copyright (c) 2025, Nostr Game Engine
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 * 
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 
 * 3. Neither the name of the copyright holder nor the names of its
 *    contributors may be used to endorse or promote products derived from
 *    this software without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 * 
 * Nostr Game Engine is a fork of the jMonkeyEngine, which is licensed under
 * the BSD 3-Clause License. The original jMonkeyEngine license is as follows:
 */
package org.ngengine.network.protocol;

import java.nio.ByteBuffer;

public class GrowableByteBuffer {

    private ByteBuffer buffer;
    private final int chunkSize;

    public GrowableByteBuffer(ByteBuffer initial, int chunkSize) {
        this.buffer = initial;
        this.chunkSize = chunkSize;
    }

    public ByteBuffer getBuffer() {
        return buffer;
    }

    private void ensureCapacity(int size) {
        int currentLimit = buffer.limit();
        int currentPosition = buffer.position();

        if (buffer.capacity() < buffer.position() + size) {
            if (chunkSize <= 0) throw new RuntimeException("Buffer is not growable");

            int growth = (size / chunkSize) + 1;
            long newSize = buffer.capacity() + (growth * chunkSize);
            if (newSize > Integer.MAX_VALUE) {
                throw new RuntimeException("Buffer size exceeds maximum limit");
            }

            buffer.flip();

            ByteBuffer newBuffer = ByteBuffer.allocate((int) newSize);
            newBuffer.put(buffer);
            newBuffer.position(currentPosition);
            newBuffer.limit(currentPosition + size);

            buffer.position(currentPosition);
            buffer.limit(currentLimit);

            buffer = newBuffer;
        } else {
            int newLimit = currentPosition + size;
            if (newLimit > buffer.limit()) {
                buffer.limit(newLimit);
            }
        }
    }

    public int remaining() {
        return buffer.remaining();
    }

    public void put(byte[] data) {
        ensureCapacity(data.length);
        buffer.put(data);
    }

    public void putShort(short value) {
        ensureCapacity(Short.BYTES);
        buffer.putShort(value);
    }

    public void putInt(int value) {
        ensureCapacity(Integer.BYTES);
        buffer.putInt(value);
    }

    public void putLong(long value) {
        ensureCapacity(Long.BYTES);
        buffer.putLong(value);
    }

    public void putFloat(float value) {
        ensureCapacity(Float.BYTES);
        buffer.putFloat(value);
    }

    public void putDouble(double value) {
        ensureCapacity(Double.BYTES);
        buffer.putDouble(value);
    }

    public void putChar(char value) {
        ensureCapacity(Character.BYTES);
        buffer.putChar(value);
    }

    public void put(byte value) {
        ensureCapacity(Byte.BYTES);
        buffer.put(value);
    }

    public void put(byte[] data, int offset, int length) {
        ensureCapacity(length);
        buffer.put(data, offset, length);
    }

    public byte get() {
        return buffer.get();
    }

    public byte get(int index) {
        return buffer.get(index);
    }

    public short getShort() {
        return buffer.getShort();
    }

    public short getShort(int index) {
        return buffer.getShort(index);
    }

    public int getInt() {
        return buffer.getInt();
    }

    public int getInt(int index) {
        return buffer.getInt(index);
    }

    public long getLong() {
        return buffer.getLong();
    }

    public long getLong(int index) {
        return buffer.getLong(index);
    }

    public float getFloat() {
        return buffer.getFloat();
    }

    public float getFloat(int index) {
        return buffer.getFloat(index);
    }

    public double getDouble() {
        return buffer.getDouble();
    }

    public double getDouble(int index) {
        return buffer.getDouble(index);
    }

    public char getChar() {
        return buffer.getChar();
    }

    public char getChar(int index) {
        return buffer.getChar(index);
    }

    public int position() {
        return buffer.position();
    }

    public void position(int newPosition) {
        ensureCapacity(newPosition - buffer.position());
        buffer.position(newPosition);
    }

    public void flip() {
        buffer.flip();
    }

    public ByteBuffer slice() {
        return buffer.slice();
    }

    public int limit() {
        return buffer.limit();
    }

    public void clear() {
        buffer.clear();
    }

    public int capacity() {
        return buffer.capacity();
    }

    public void limit(int newLimit) {
        buffer.limit(newLimit);
    }

    public void put(ByteBuffer data) {
        ensureCapacity(data.remaining());
        buffer.put(data);
    }

    public void get(byte[] inputBytes) {
        buffer.get(inputBytes);
    }
}
