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
import java.time.Instant;

import org.ngengine.network.protocol.GrowableByteBuffer;
import org.ngengine.network.protocol.VarInt;

public class InstantSerializer extends DynamicSerializer {

    /**
     * Special-case encoding for epoch milli == 0.
     */
    private static final byte MODE_ZERO = 0x00;

    /**
     * Absolute epoch millis encoded as signed varint.
     * Used for all timestamps before Bitcoin genesis, including negative ones.
     */
    private static final byte MODE_ABSOLUTE = 0x01;

    /**
     * Compact encoding:
     *   delta = epochMilli - BITCOIN_GENESIS_EPOCH_MILLIS
     *   chunkIndex = delta / CHUNK_MILLIS
     *   remainder = delta % CHUNK_MILLIS
     *
     * Serialized as:
     *   header byte
     *   unsigned varint chunkIndex
     *   unsigned varint remainder
     */
    private static final byte MODE_COMPACT = 0x02;

    /**
     * Bitcoin genesis block timestamp:
     * 2009-01-03T18:15:05Z
     */
    private static final long BITCOIN_GENESIS_EPOCH_MILLIS = 1231006505000L;

    /**
     * Fixed chunk size: 90 days in milliseconds.
     */
    private static final long CHUNK_MILLIS = 90L * 24L * 60L * 60L * 1000L;

    @Override
    public Instant readObject(ByteBuffer data, Class c) throws IOException {
        if (!data.hasRemaining()) {
            throw new IOException("Missing Instant header byte");
        }

        byte mode = data.get();

        switch (mode) {
            case MODE_ZERO:
                return Instant.ofEpochMilli(0L);

            case MODE_ABSOLUTE: {
                long epochMilli = VarInt.decodeSigned(data);
                return Instant.ofEpochMilli(epochMilli);
            }

            case MODE_COMPACT: {
                long chunkIndex = VarInt.decodeUnsigned(data);
                long remainder = VarInt.decodeUnsigned(data);

                if (remainder < 0 || remainder >= CHUNK_MILLIS) {
                    throw new IOException("Invalid compact Instant remainder: " + remainder
                            + " (must be in [0, " + CHUNK_MILLIS + "))");
                }

                long epochMilli;
                try {
                    epochMilli = Math.addExact(
                            BITCOIN_GENESIS_EPOCH_MILLIS,
                            Math.addExact(
                                    Math.multiplyExact(chunkIndex, CHUNK_MILLIS),
                                    remainder
                            )
                    );
                } catch (ArithmeticException ex) {
                    throw new IOException("Compact Instant overflow while decoding", ex);
                }

                return Instant.ofEpochMilli(epochMilli);
            }

            default:
                throw new IOException("Unknown Instant encoding mode: " + (mode & 0xFF));
        }
    }

    @Override
    public void writeObject(GrowableByteBuffer buffer, Object object) throws IOException {
        Instant instant = (Instant) object;
        long epochMilli = instant.toEpochMilli();

        // Hardcoded special case: zero timestamp
        if (epochMilli == 0L) {
            buffer.put(MODE_ZERO);
            return;
        }

        // Any timestamp before Bitcoin genesis uses absolute encoding.
        // This also correctly covers negative epoch millis.
        if (epochMilli < BITCOIN_GENESIS_EPOCH_MILLIS) {
            buffer.put(MODE_ABSOLUTE);
            VarInt.encodeSigned(epochMilli, buffer);
            return;
        }

        long delta = epochMilli - BITCOIN_GENESIS_EPOCH_MILLIS;
        long chunkIndex = delta / CHUNK_MILLIS;
        long remainder = delta % CHUNK_MILLIS;

        buffer.put(MODE_COMPACT);
        VarInt.encodeUnsigned(chunkIndex, buffer);
        VarInt.encodeUnsigned(remainder, buffer);
    }
}