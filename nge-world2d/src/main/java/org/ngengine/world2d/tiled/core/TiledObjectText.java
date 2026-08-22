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

package org.ngengine.world2d.tiled.core;

import com.jme3.math.ColorRGBA;

/**
 * Used to mark an object as a text object. Contains the actual text as character data.
 * For alignment purposes, the bottom of the text is the descender height of the font,
 * and the top of the text is the ascender height of the font. For example, bottom
 * alignment of the word “cat” will leave some space below the text, even though it is
 * unused for this word with most fonts. Similarly, top alignment of the word “cat” will
 * leave some space above the “t” with most fonts, because this space is used for diacritics.
 * If the text is larger than the object’s bounds, it is clipped to the bounds of the object.
 *
 * @author yanmaoyuan
 */
public class TiledObjectText {
    /**
     * Whether to use a bold font (default: false)
     */
    private boolean bold;
    /**
     * Hex-formatted color (#RRGGBB or #AARRGGBB) (default: #000000)
     */
    private ColorRGBA color;
    /**
     * Font family (default: sans-serif)
     */
    private String fontFamily;
    /**
     * Horizontal alignment (center, right, justify or left (default))
     */
    private String horizontalAlignment;
    /**
     * Vertical alignment (center, bottom or top (default))
     */
    private String verticalAlignment;
    /**
     * Whether to use an italic font (default: false)
     */
    private boolean italic;
    /**
     * Whether to use kerning when placing characters (default: true)
     */
    private boolean kerning;
    /**
     * Pixel size of font (default: 16)
     */
    private int pixelSize;
    /**
     * Whether to strike out the text (default: false)
     */
    private boolean strikeout;
    /**
     * Text
     */
    private String text;
    /**
     * Whether to underline the text (default: false)
     */
    private boolean underline;
    /**
     * Whether the text is wrapped within the object bounds (default: false)
     */
    private boolean wrap;

    public TiledObjectText(String text) {
        this.text = text;
        bold = false;
        italic = false;
        underline = false;
        strikeout = false;
        kerning = true;
        wrap = false;
        color = new ColorRGBA(0, 0, 0, 1);
        fontFamily = "sans-serif";
        pixelSize = 16;
        horizontalAlignment = "left";
        verticalAlignment = "top";
    }

    public boolean isBold() {
        return bold;
    }

    public void setBold(boolean bold) {
        this.bold = bold;
    }

    public ColorRGBA getColor() {
        return color;
    }

    public void setColor(ColorRGBA color) {
        this.color = color;
    }

    public String getFontFamily() {
        return fontFamily;
    }

    public void setFontFamily(String fontFamily) {
        this.fontFamily = fontFamily;
    }

    public String getHorizontalAlignment() {
        return horizontalAlignment;
    }

    public void setHorizontalAlignment(String horizontalAlignment) {
        this.horizontalAlignment = horizontalAlignment;
    }

    public String getVerticalAlignment() {
        return verticalAlignment;
    }

    public void setVerticalAlignment(String verticalAlignment) {
        this.verticalAlignment = verticalAlignment;
    }

    public boolean isItalic() {
        return italic;
    }

    public void setItalic(boolean italic) {
        this.italic = italic;
    }

    public boolean isKerning() {
        return kerning;
    }

    public void setKerning(boolean kerning) {
        this.kerning = kerning;
    }

    public int getPixelSize() {
        return pixelSize;
    }

    public void setPixelSize(int pixelSize) {
        this.pixelSize = pixelSize;
    }

    public boolean isStrikeout() {
        return strikeout;
    }

    public void setStrikeout(boolean strikeout) {
        this.strikeout = strikeout;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public boolean isUnderline() {
        return underline;
    }

    public void setUnderline(boolean underline) {
        this.underline = underline;
    }

    public boolean isWrap() {
        return wrap;
    }

    public void setWrap(boolean wrap) {
        this.wrap = wrap;
    }
}
