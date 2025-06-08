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

/*
 * QR Code generator library (Java)
 *
 * Copyright (c) Project Nayuki. (MIT License)
 * https://www.nayuki.io/page/qr-code-generator-library
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal in
 * the Software without restriction, including without limitation the rights to
 * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of
 * the Software, and to permit persons to whom the Software is furnished to do so,
 * subject to the following conditions:
 * - The above copyright notice and this permission notice shall be included in
 *   all copies or substantial portions of the Software.
 * - The Software is provided "as is", without warranty of any kind, express or
 *   implied, including but not limited to the warranties of merchantability,
 *   fitness for a particular purpose and noninfringement. In no event shall the
 *   authors or copyright holders be liable for any claim, damages or other
 *   liability, whether in an action of contract, tort or otherwise, arising from,
 *   out of or in connection with the Software or the use or other dealings in the
 *   Software.
 */

/**
 * Thrown when the supplied data does not fit any QR Code version. Ways to handle this exception include:
 * <ul>
 * <li>
 * <p>
 * Decrease the error correction level if it was greater than {@code Ecc.LOW}.
 * </p>
 * </li>
 * <li>
 * <p>
 * If the advanced {@code encodeSegments()} function with 6 arguments or the {@code makeSegmentsOptimally()}
 * function was called, then increase the maxVersion argument if it was less than {@link QrCode#MAX_VERSION}.
 * (This advice does not apply to the other factory functions because they search all versions up to
 * {@code QrCode.MAX_VERSION}.)
 * </p>
 * </li>
 * <li>
 * <p>
 * Split the text data into better or optimal segments in order to reduce the number of bits required. (See
 * {@link QrSegmentAdvanced#makeSegmentsOptimally(CharSequence,QrCode.Ecc,int,int)
 * QrSegmentAdvanced.makeSegmentsOptimally()}.)
 * </p>
 * </li>
 * <li>
 * <p>
 * Change the text or binary data to be shorter.
 * </p>
 * </li>
 * <li>
 * <p>
 * Change the text to fit the character set of a particular segment mode (e.g. alphanumeric).
 * </p>
 * </li>
 * <li>
 * <p>
 * Propagate the error upward to the caller/user.
 * </p>
 * </li>
 * </ul>
 *
 * @see QrCode#encodeText(CharSequence, QrCode.Ecc)
 * @see QrCode#encodeBinary(byte[], QrCode.Ecc)
 * @see QrCode#encodeSegments(java.util.List, QrCode.Ecc)
 * @see QrCode#encodeSegments(java.util.List, QrCode.Ecc, int, int, int, boolean)
 * @see QrSegmentAdvanced#makeSegmentsOptimally(CharSequence, QrCode.Ecc, int, int)
 */
public class DataTooLongException extends IllegalArgumentException {

    public DataTooLongException() {}

    public DataTooLongException(String msg) {
        super(msg);
    }
}
