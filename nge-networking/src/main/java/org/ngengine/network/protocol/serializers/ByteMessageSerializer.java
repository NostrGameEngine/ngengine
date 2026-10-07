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
import org.ngengine.network.protocol.VarInt;
import org.ngengine.network.protocol.DynamicSerializerProtocol;
import org.ngengine.network.protocol.messages.ByteDataMessage;

public class ByteMessageSerializer extends DynamicSerializer {

    @Override
    public <T> T readObject(ByteBuffer buffer, Class<T> c) throws IOException {
        try {
            ByteDataMessage message = (ByteDataMessage) c.getDeclaredConstructor().newInstance();
            long len = VarInt.decodeUnsigned(buffer);
            int length = DynamicSerializerProtocol.checkedLength(len, DynamicSerializerProtocol.MAX_FRAME_BYTES, buffer.remaining());
            DynamicSerializerProtocol.chargeDecodedBytes(length);
            if (length > buffer.remaining()) {
                throw new IOException("Invalid ByteMessage length: " + length);
            }
            byte[] bytes = new byte[length];
            buffer.get(bytes);
            message.setData(ByteBuffer.wrap(bytes));
            return (T) message;
        } catch (Exception e) {
            throw new IOException("Error deserializing ByteMessage", e);
        }
    }

    @Override
    public void writeObject(GrowableByteBuffer buffer, Object object) throws IOException {
        ByteDataMessage message = (ByteDataMessage) object;
        ByteBuffer dataRef = message.getData();
        if (dataRef == null) throw new IOException("The message data is null");
        ByteBuffer data = dataRef.slice();
        VarInt.encodeUnsigned(data.remaining(), buffer);
        buffer.put(data);
    }
}
