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

package io.github.jmecn.tiled.enums;

public enum ObjectShape {
    /**
     * No need to explain.
     */
    RECTANGLE,
    /**
     * Used to mark an object as an ellipse. The existing x, y, width and
     * height attributes are used to determine the size of the ellipse.
     */
    ELLIPSE,
    /**
     * Used to mark an object as a point. The existing x and y attributes
     * are used to determine the position of the point.
     */
    POINT,
    /**
     * A list of x,y coordinates in pixels.
     *
     * Each polygon object is made up of a space-delimited list of x,y
     * coordinates. The origin for these coordinates is the location of the
     * parent object. By default, the first point is created as 0,0 denoting
     * that the point will originate exactly where the object is placed.
     */
    POLYGON,
    /**
     * A polyline follows the same placement definition as a polygon object.
     */
    POLYLINE,
    /**
     * Used to mark an object as a text object. Contains the actual text as character data.
     *
     * For alignment purposes, the bottom of the text is the descender height of the font,
     * and the top of the text is the ascender height of the font. For example, bottom
     * alignment of the word “cat” will leave some space below the text, even though it is
     * unused for this word with most fonts. Similarly, top alignment of the word “cat” will
     * leave some space above the “t” with most fonts, because this space is used for diacritics.
     *
     * If the text is larger than the object’s bounds, it is clipped to the bounds of the object.
     */
    TEXT,
    /**
     * An tile references to a tile with it's gid.
     */
    TILE,
    /**
     * An image
     */
    IMAGE;
}