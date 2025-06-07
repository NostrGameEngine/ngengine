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
package org.ngengine.network.protocol.serializers;

import java.io.IOException;
import java.nio.ByteBuffer;
import org.ngengine.network.protocol.GrowableByteBuffer;

/**
 * Boolean serializer.
 *
 * @author Riccardo Balbo
 */
@SuppressWarnings("unchecked")
public class NumberSerializer extends DynamicSerializer {

    @Override
    public Number readObject(ByteBuffer data, Class c) throws IOException {
        if (c == Byte.class) {
            return data.get();
        } else if (c == Short.class) {
            return data.getShort();
        } else if (c == Integer.class) {
            return data.getInt();
        } else if (c == Long.class) {
            return data.getLong();
        } else if (c == Float.class) {
            return data.getFloat();
        } else if (c == Double.class) {
            return data.getDouble();
        } else {
            throw new IOException("Unsupported number type: " + c);
        }
    }

    @Override
    public void writeObject(GrowableByteBuffer buffer, Object object) throws IOException {
        if (object instanceof Byte) {
            buffer.put((Byte) object);
        } else if (object instanceof Short) {
            buffer.putShort((Short) object);
        } else if (object instanceof Integer) {
            buffer.putInt((Integer) object);
        } else if (object instanceof Long) {
            buffer.putLong((Long) object);
        } else if (object instanceof Float) {
            buffer.putFloat((Float) object);
        } else if (object instanceof Double) {
            buffer.putDouble((Double) object);
        } else {
            throw new IOException("Unsupported number type: " + object.getClass());
        }
    }
}
