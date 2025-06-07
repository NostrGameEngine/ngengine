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
package org.ngengine.gui.qr;

import java.util.BitSet;
import java.util.Objects;

/**
 * An appendable sequence of bits (0s and 1s). Mainly used by {@link QrSegment}.
 */
public final class BitBuffer implements Cloneable {

    /*---- Fields ----*/

    private BitSet data;

    private int bitLength; // Non-negative

    /*---- Constructor ----*/

    /**
     * Constructs an empty bit buffer (length 0).
     */
    public BitBuffer() {
        data = new BitSet();
        bitLength = 0;
    }

    /*---- Methods ----*/

    /**
     * Returns the length of this sequence, which is a non-negative value.
     *
     * @return the length of this sequence
     */
    public int bitLength() {
        assert bitLength >= 0;
        return bitLength;
    }

    /**
     * Returns the bit at the specified index, yielding 0 or 1.
     *
     * @param index
     *            the index to get the bit at
     * @return the bit at the specified index
     * @throws IndexOutOfBoundsException
     *             if index &lt; 0 or index &#x2265; bitLength
     */
    public int getBit(int index) {
        if (index < 0 || index >= bitLength) throw new IndexOutOfBoundsException();
        return data.get(index) ? 1 : 0;
    }

    /**
     * Appends the specified number of low-order bits of the specified value to this buffer. Requires 0
     * &#x2264; len &#x2264; 31 and 0 &#x2264; val &lt; 2<sup>len</sup>.
     *
     * @param val
     *            the value to append
     * @param len
     *            the number of low-order bits in the value to take
     * @throws IllegalArgumentException
     *             if the value or number of bits is out of range
     * @throws IllegalStateException
     *             if appending the data would make bitLength exceed Integer.MAX_VALUE
     */
    public void appendBits(int val, int len) {
        if (len < 0 || len > 31 || val >>> len != 0) throw new IllegalArgumentException("Value out of range");
        if (Integer.MAX_VALUE - bitLength < len) throw new IllegalStateException("Maximum length reached");
        for (int i = len - 1; i >= 0; i--, bitLength++) data.set(bitLength, QrCode.getBit(val, i)); // Append bit by bit
    }

    /**
     * Appends the content of the specified bit buffer to this buffer.
     *
     * @param bb
     *            the bit buffer whose data to append (not {@code null})
     * @throws NullPointerException
     *             if the bit buffer is {@code null}
     * @throws IllegalStateException
     *             if appending the data would make bitLength exceed Integer.MAX_VALUE
     */
    public void appendData(BitBuffer bb) {
        Objects.requireNonNull(bb);
        if (Integer.MAX_VALUE - bitLength < bb.bitLength) throw new IllegalStateException("Maximum length reached");
        for (int i = 0; i < bb.bitLength; i++, bitLength++) data.set(bitLength, bb.data.get(i)); // Append bit by bit
    }

    /**
     * Returns a new copy of this buffer.
     *
     * @return a new copy of this buffer (not {@code null})
     */
    public BitBuffer clone() {
        try {
            BitBuffer result = (BitBuffer) super.clone();
            result.data = (BitSet) result.data.clone();
            return result;
        } catch (CloneNotSupportedException e) {
            throw new AssertionError(e);
        }
    }
}
