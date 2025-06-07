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
import java.util.ArrayList;
import java.util.Collection;
import java.util.function.BiFunction;
import java.util.logging.Level;
import org.ngengine.network.protocol.GrowableByteBuffer;

/**
 * Serializes collections.
 *
 * @author Riccardo Balbo
 */
public class CollectionSerializer extends DynamicSerializer {

    private final BiFunction<Object, GrowableByteBuffer, Void> serialize;
    private final BiFunction<ByteBuffer, Class<?>, Object> deserialize;

    public CollectionSerializer(
        BiFunction<Object, GrowableByteBuffer, Void> serialize,
        BiFunction<ByteBuffer, Class<?>, Object> deserialize
    ) {
        this.serialize = serialize;
        this.deserialize = deserialize;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> T readObject(ByteBuffer data, Class<T> c) throws IOException {
        int length = data.getInt();

        Collection collection;
        try {
            collection = (Collection) c.getDeclaredConstructor().newInstance();
        } catch (Exception e) {
            log.log(Level.FINE, "[Serializer][???] Could not determine collection type. Using ArrayList.");
            collection = new ArrayList(length);
        }

        if (length == 0) return (T) collection;

        for (int i = 0; i < length; i++) {
            Object element = this.deserialize.apply(data, c);
            collection.add(element);
        }

        return (T) collection;
    }

    @SuppressWarnings("rawtypes")
    @Override
    public void writeObject(GrowableByteBuffer buffer, Object object) throws IOException {
        Collection collection = (Collection) object;
        int length = collection.size();

        buffer.putInt(length);
        if (length == 0) return;

        for (Object element : collection) {
            this.serialize.apply(element, buffer);
        }
    }
}
