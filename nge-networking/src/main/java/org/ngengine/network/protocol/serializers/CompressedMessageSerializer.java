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

import com.jme3.network.Message;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.*;
import java.util.function.BiFunction;
import java.util.logging.Logger;
import java.util.zip.Deflater;
import java.util.zip.Inflater;
import org.ngengine.network.protocol.GrowableByteBuffer;
import org.ngengine.network.protocol.VarInt;
import org.ngengine.network.protocol.DynamicSerializerProtocol;
import java.util.function.IntConsumer;
import org.ngengine.network.protocol.messages.CompressedMessage;

/**
 * The field serializer is the default serializer used for custom class.
 *
 * @author Lars Wesselius, Nathan Sweet
 */
public class CompressedMessageSerializer extends DynamicSerializer {

    private static final Logger log = Logger.getLogger(CompressedMessageSerializer.class.getName());
    protected final BiFunction<Object, GrowableByteBuffer, Void> serialize;
    protected final BiFunction<ByteBuffer, Class<?>, Object> deserialize;
    private final IntConsumer chargeExpanded;

    public CompressedMessageSerializer(
        BiFunction<Object, GrowableByteBuffer, Void> serialize,
        BiFunction<ByteBuffer, Class<?>, Object> deserialize
    ) {
        this(serialize, deserialize, length -> {});
    }

    public CompressedMessageSerializer(
        BiFunction<Object, GrowableByteBuffer, Void> serialize,
        BiFunction<ByteBuffer, Class<?>, Object> deserialize,
        IntConsumer chargeExpanded
    ) {
        this.serialize = serialize;
        this.deserialize = deserialize;
        this.chargeExpanded = chargeExpanded;
    }

    protected void serialize(Object object, GrowableByteBuffer buffer) {
        this.serialize.apply(object, buffer);
    }

    protected Object deserialize(ByteBuffer data, Class<?> c) {
        return this.deserialize.apply(data, c);
    }

    @Override
    public <T> T readObject(ByteBuffer data, Class<T> c) throws IOException {
        try {
            if (data.remaining() < 1) {
                throw new IOException("Not enough data to read compressed message");
            }
            long len = VarInt.decodeUnsigned(data);
            int length = DynamicSerializerProtocol.checkedLength(len, DynamicSerializerProtocol.MAX_FRAME_BYTES, data.remaining());
            DynamicSerializerProtocol.chargeDecodedBytes(length + Math.min(length, 1024) + 1024);
            byte[] compressedData = new byte[length];
            data.get(compressedData);
            Inflater inflater = new Inflater();
            ByteArrayOutputStream bos = new ByteArrayOutputStream(Math.min(length, 1024));
            int decompressedSize = 0;
            try {
                inflater.setInput(compressedData);
                byte[] chunk = new byte[1024];
                while (!inflater.finished()) {
                    int count = inflater.inflate(chunk);
                    if (count == 0) throw new IOException("Incomplete or stalled compressed message");
                    if (count > DynamicSerializerProtocol.MAX_FRAME_BYTES - decompressedSize) {
                        throw new IOException("Expanded message exceeds frame bound");
                    }
                    chargeExpanded.accept(count);
                    bos.write(chunk, 0, count);
                    decompressedSize += count;
                }
                if (inflater.getRemaining() != 0) throw new IOException("Trailing compressed data");
            } finally {
                inflater.end();
            }
            byte[] decompressedData = bos.toByteArray();
            ByteBuffer buffer = ByteBuffer.wrap(decompressedData, 0, decompressedSize);

            Message object = (Message) this.deserialize(buffer, Object.class);
            if (buffer.hasRemaining()) throw new IOException("Trailing expanded data");
            CompressedMessage compressedMessage = new CompressedMessage(object);
            return (T) compressedMessage;
        } catch (Exception e) {
            throw new IOException(e);
        }
    }

    @Override
    public void writeObject(GrowableByteBuffer buffer, Object cm) throws IOException {
        CompressedMessage compressedMessage = (CompressedMessage) cm;
        Message object = compressedMessage.getMessage();

        GrowableByteBuffer tmp = new GrowableByteBuffer(ByteBuffer.allocate(1024), 1024, DynamicSerializerProtocol.MAX_FRAME_BYTES);
        this.serialize(object, tmp);
        tmp.flip();

        Deflater deflater = new Deflater();
        byte[] inputBytes = new byte[tmp.limit()];
        tmp.get(inputBytes);
        deflater.setInput(inputBytes);
        deflater.finish();

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        byte[] chunk = new byte[1024];
        while (!deflater.finished()) {
            int count = deflater.deflate(chunk);
            if (count > 0) {
                outputStream.write(chunk, 0, count);
            } else {
                break;
            }
        }
        deflater.end();

        byte[] compressedData = outputStream.toByteArray();
        VarInt.encodeUnsigned(compressedData.length, buffer);
        buffer.put(compressedData);
    }
}
