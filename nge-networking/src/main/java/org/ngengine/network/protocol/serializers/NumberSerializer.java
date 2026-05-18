/**
 * Copyright (c) 2025-2026, Nostr Game Engine
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
 * the BSD 3-Clause License. 
 */

package org.ngengine.network.protocol.serializers;

import java.io.IOException;
import java.nio.ByteBuffer;
import org.ngengine.network.protocol.GrowableByteBuffer;
import org.ngengine.network.protocol.VarFloat;
import org.ngengine.network.protocol.VarInt;

/**
 * Boolean serializer.
 *
 * @author Riccardo Balbo
 */
@SuppressWarnings("unchecked")
public class NumberSerializer extends DynamicSerializer {

    @Override
    public Number readObject(ByteBuffer data, Class c) throws IOException {
        if (c == Byte.class || c == byte.class) {
            return data.get();
        } else if (c == Short.class || c == short.class) {
            long value = VarInt.decodeSigned(data);
            if (value < Short.MIN_VALUE || value > Short.MAX_VALUE) {
                throw new IOException("Decoded short out of range: " + value);
            }
            return (short) value;
        } else if (c == Integer.class || c == int.class) {
            long value = VarInt.decodeSigned(data);
            if (value < Integer.MIN_VALUE || value > Integer.MAX_VALUE) {
                throw new IOException("Decoded int out of range: " + value);
            }
            return (int) value;
        } else if (c == Long.class || c == long.class) {
            return VarInt.decodeSigned(data);
        } else if (c == Float.class || c == float.class) {
            return VarFloat.decodeFloat(data);
        } else if (c == Double.class || c == double.class) {
            return VarFloat.decodeDouble(data);
        } else {
            throw new IOException("Unsupported number type: " + c);
        }
    }

    @Override
    public void writeObject(GrowableByteBuffer buffer, Object object) throws IOException {
        if (object instanceof Byte) {
            buffer.put((Byte) object);
        } else if (object instanceof Short) {
            VarInt.encodeSigned((Short) object, buffer);
        } else if (object instanceof Integer) {
            VarInt.encodeSigned((Integer) object, buffer);
        } else if (object instanceof Long) {
            VarInt.encodeSigned((Long) object, buffer);
        } else if (object instanceof Float) {
            VarFloat.encodeFloat((Float) object, buffer);
        } else if (object instanceof Double) {
            VarFloat.encodeDouble((Double) object, buffer);
        } else {
            throw new IOException("Unsupported number type: " + object.getClass());
        }
    }
}
