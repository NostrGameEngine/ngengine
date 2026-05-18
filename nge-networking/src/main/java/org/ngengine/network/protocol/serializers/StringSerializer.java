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
import java.nio.charset.StandardCharsets;
import org.ngengine.network.protocol.GrowableByteBuffer;
import org.ngengine.network.protocol.VarInt;

/**
 * Boolean serializer.
 *
 * @author Riccardo Balbo
 */
@SuppressWarnings("unchecked")
public class StringSerializer extends DynamicSerializer {

    @Override
    public String readObject(ByteBuffer data, Class c) throws IOException {
        long len = VarInt.decodeUnsigned(data);
        if (len > Integer.MAX_VALUE) {
            throw new IOException("String length too large: " + len);
        }
        int length = (int) len;
        if (length > data.remaining()) {
            throw new IOException("Invalid string length: " + length);
        }
        byte[] bytes = new byte[length];
        data.get(bytes);
        String str = new String(bytes, StandardCharsets.UTF_8);
        return str;
    }

    @Override
    public void writeObject(GrowableByteBuffer buffer, Object object) throws IOException {
        String str = (String) object;
        if (str == null) throw new IOException("The string is null");
        byte[] bytes = str.getBytes(StandardCharsets.UTF_8);
        VarInt.encodeUnsigned(bytes.length, buffer);
        buffer.put(bytes);
    }
}
