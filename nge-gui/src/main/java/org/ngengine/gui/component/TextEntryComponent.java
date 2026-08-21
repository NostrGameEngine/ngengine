/*
 * $Id$
 *
 * Copyright (c) 2012-2012 jMonkeyEngine
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 * * Redistributions of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 *
 * * Redistributions in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer in the
 *   documentation and/or other materials provided with the distribution.
 *
 * * Neither the name of 'jMonkeyEngine' nor the names of its contributors
 *   may be used to endorse or promote products derived from this software
 *   without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
 * TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package org.ngengine.gui.component;

import java.util.function.Function;
import java.util.logging.Logger;

import org.ngengine.gui.HAlignment;
import org.ngengine.gui.NGEGui;
import org.ngengine.gui.VAlignment;

import com.jme3.font.BitmapFont;
import com.jme3.font.BitmapText;
import com.jme3.font.LineWrapMode;
import com.jme3.font.Rectangle;
import com.jme3.material.RenderState.BlendMode;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Spatial;
import com.jme3.scene.Spatial.CullHint;
import com.jme3.scene.shape.Quad;
import org.ngengine.gui.core.GuiControl;
import org.ngengine.gui.core.GuiMaterial;
import org.ngengine.gui.ime.ImeCompositionEvent;
import org.ngengine.gui.nav.FocusTarget;
import org.ngengine.gui.nav.ScrollDirection;

/**
 * A basic text entry component that allows displaying and editing of text based.
 *
 * @author Riccardo Balbo
 */
public class TextEntryComponent extends AbstractGuiComponent implements FocusTarget, ColoredComponent {

    static Logger log = Logger.getLogger(TextEntryComponent.class.getName());

    private BitmapFont font;
    private BitmapText bitmapText;
    private Rectangle textBox;

    private HAlignment hAlign = HAlignment.Left;
    private VAlignment vAlign = VAlignment.Top;

    private Vector3f preferredSize;
    private float preferredWidth;
    private int preferredLineCount;

    private boolean singleLine;
    private boolean focused;
    private boolean cursorVisible = true;
    private Float preferredCursorWidth = null;

    private String text = "";
    private int caretFrom = 0;
    private int caretTo = 0;

    // horizontal column offset applied per line
    private int textOffset = 0;
    // vertical offset in explicit lines
    private int lineOffset = 0;

    // Cursor geometry
    private Quad cursorQuad;
    private Geometry cursor;
    private Function<String, String> outputTransform = s -> s;
    private Function<Character, Character> inputTransform = c -> c;
    private Runnable closeKeyboard = null;

    public TextEntryComponent(BitmapFont font) {
        this.font = font;
        this.bitmapText = new BitmapText(font);
        bitmapText.setLineWrapMode(LineWrapMode.Clip);

        cursorQuad = new Quad(getCursorWidth(), bitmapText.getLineHeight());
        cursor = new Geometry("cursor", cursorQuad);
        GuiMaterial mat = NGEGui.createMaterial(new ColorRGBA(1, 1, 1, 0.75f), false);
        cursor.setMaterial(mat.getMaterial());
        cursor.getMaterial().getAdditionalRenderState().setBlendMode(BlendMode.Alpha);
        cursor.setUserData("layer", 1);
        bitmapText.attachChild(cursor);

        // Initialize visible text
        resetText();
        resetCursorPosition();
        resetCursorState();
    }

    private Geometry getCursor() {
        return cursor;
    }

    public void setOutputTransform(Function<String, String> transform) {
        this.outputTransform = transform;
        resetText();
        resetCursorPosition();
    }

    public void setInputTransform(Function<Character, Character> transform) {
        this.inputTransform = transform;
        if (closeKeyboard != null) openKeyboard();

    }

    @Override
    public TextEntryComponent clone() {
        TextEntryComponent result = (TextEntryComponent) super.clone();

        // Recreate bitmapText + cursor
        result.font = this.font;
        result.bitmapText = new BitmapText(result.font);
        result.bitmapText.setLineWrapMode(this.bitmapText.getLineWrapMode());
        result.bitmapText.setSize(this.bitmapText.getSize());
        result.bitmapText.setColor(this.bitmapText.getColor());
        result.bitmapText.setAlpha(this.bitmapText.getAlpha());

        result.cursorQuad = new Quad(result.getCursorWidth(), result.bitmapText.getLineHeight());
        result.cursor = new Geometry("cursor", result.cursorQuad);
        GuiMaterial mat = NGEGui.createMaterial(new ColorRGBA(1, 1, 1, 0.75f), false);
        result.cursor.setMaterial(mat.getMaterial());
        result.cursor.getMaterial().getAdditionalRenderState().setBlendMode(BlendMode.Alpha);
        result.cursor.setUserData("layer", 1);
        result.bitmapText.attachChild(result.cursor);

        // Copy state
        result.textBox = null;
        result.hAlign = this.hAlign;
        result.vAlign = this.vAlign;
        result.preferredSize = null;
        result.preferredWidth = this.preferredWidth;
        result.preferredLineCount = this.preferredLineCount;

        result.singleLine = this.singleLine;
        result.focused = this.focused;
        result.cursorVisible = this.cursorVisible;
        result.preferredCursorWidth = this.preferredCursorWidth;

        result.text = this.text;
        result.caretFrom = this.caretFrom;
        result.caretTo = this.caretTo;
        result.textOffset = this.textOffset;
        result.lineOffset = this.lineOffset;

        // Apply alignment/box later when attached/reshaped
        result.resetText();
        result.resetCursorPosition();
        result.resetCursorState();

        return result;
    }

    @Override
    public void attach(GuiControl parent) {
        super.attach(parent);
        getNode().attachChild(bitmapText);
        resetCursorPosition();
        resetCursorState();
    }

    @Override
    public void detach(GuiControl parent) {
        getNode().detachChild(bitmapText);
        super.detach(parent);
    }

    @Override
    public boolean isFocused() {
        return focused;
    }

    @Override
    public boolean isFocusable() {
        return true;
    }

    @Override
    public void focusGained() {
        this.focusGained(getNode());
    }

    @Override
    public void focusLost() {
        this.focusLost(getNode());
        if (closeKeyboard != null) {
            closeKeyboard.run();
            closeKeyboard = null;
        }
        this.focused = false;
        resetCursorState();
    }

    @Override
    public void focusGained(Spatial target) {

    }

    @Override
    public void focusAction(Spatial target, boolean pressed) {
        if (!pressed) {
            return;
        }
        if (!this.focused || closeKeyboard == null) {
            this.focused = true;
            resetCursorState();
            openKeyboard();
        }
    }

    @Override
    public void focusAction(Spatial target, boolean pressed, float x, float y) {
        if (pressed) {
            setCursorFromWorld(x, y);
            if (this.focused && closeKeyboard != null) {
                openKeyboard(caretTo, caretTo);
                return;
            }
        }
        focusAction(target, pressed);
    }

    @Override
    public void focusLost(Spatial target) {

    }

    public void setSingleLine(boolean f) {
        this.singleLine = f;
        if (singleLine) {
            // In single line mode, vertical offset makes no sense.
            lineOffset = 0;
        }
        resetText();
        resetCursorPosition();
    }

    public boolean isSingleLine() {
        return singleLine;
    }

    public void setFont(BitmapFont font) {
        if (font == bitmapText.getFont()) return;

        boolean attached = isAttached();
        if (attached) {
            bitmapText.removeFromParent();
        }

        // Detach cursor from old bitmapText and attach to new
        if (cursor.getParent() == bitmapText) {
            bitmapText.detachChild(cursor);
        }

        BitmapText newText = new BitmapText(font);
        newText.setLineWrapMode(bitmapText.getLineWrapMode());
        newText.setSize(getFontSize());
        newText.setColor(getColor());
        newText.setAlpha(getAlpha());
        this.bitmapText = newText;

        // Attach cursor to new bitmapText
        bitmapText.attachChild(cursor);

        // Update font reference
        this.font = font;

        // Re-apply box + alignment if available
        if (textBox != null) {
            bitmapText.setBox(textBox);
            resetAlignment();
        }

        resizeCursor();
        resetText();
        resetCursorPosition();
        resetCursorState();

        if (attached) {
            getNode().attachChild(bitmapText);
        }
    }

    public BitmapFont getFont() {
        return bitmapText.getFont();
    }

    public void setFontSize(float f) {
        this.bitmapText.setSize(f);
        resizeCursor();
        resetText();
        resetCursorPosition();
    }

    public float getFontSize() {
        return bitmapText.getSize();
    }

    protected void resetCursorColor() {
        float alpha = bitmapText.getAlpha();
        if (alpha == -1) alpha = 1;

        ColorRGBA color = bitmapText.getColor();
        Geometry cursor = getCursor();

        if (alpha == 1) {
            cursor.getMaterial().setColor("Color", color);
        } else {
            ColorRGBA cursorColor = color != null ? color.clone() : ColorRGBA.White.clone();
            cursorColor.a = alpha;
            cursor.getMaterial().setColor("Color", cursorColor);
        }
    }

    @Override
    public void setColor(ColorRGBA color) {
        float alpha = bitmapText.getAlpha();
        bitmapText.setColor(color);
        if (alpha != 1) {
            bitmapText.setAlpha(alpha);
        }
        resetCursorColor();
    }

    @Override
    public ColorRGBA getColor() {
        return bitmapText.getColor();
    }

    @Override
    public void setAlpha(float f) {
        bitmapText.setAlpha(f);
        resetCursorColor();
    }

    @Override
    public float getAlpha() {
        return bitmapText.getAlpha();
    }

    protected float getVisibleWidth(String s) {
        float x = font.getLineWidth(s + " ");
        x -= font.getLineWidth(" ");
        float scale = bitmapText.getSize() / font.getPreferredSize();
        x *= scale;
        return x;
    }

    public void setPreferredCursorWidth(Float f) {
        this.preferredCursorWidth = f;
        resizeCursor();
        resetCursorPosition();
    }

    public Float getPreferredCursorWidth() {
        return preferredCursorWidth;
    }

    public float getCursorWidth() {
        if (preferredCursorWidth != null) {
            return preferredCursorWidth;
        }
        float height = bitmapText.getLineHeight();
        if (height > 5) {
            return Math.max(1, height / 16f);
        }
        return height / 16f;
    }

    protected void resizeCursor() {
        cursorQuad.updateGeometry(getCursorWidth(), bitmapText.getLineHeight());
        cursorQuad.clearCollisionData();
    }

    protected void resetCursorState() {
        Geometry cursor = getCursor();
        if (isAttached() && focused && cursorVisible) {
            cursor.setCullHint(CullHint.Inherit);
        } else {
            cursor.setCullHint(CullHint.Always);
        }
    }

    public void setText(String text) {
        this.text = text == null ? "" : text;

        // clamp caret to new length
        int len = this.text.length();
        caretFrom = clamp(caretFrom, 0, len);
        caretTo = clamp(caretTo, 0, len);

        // clamp offsets
        if (singleLine) {
            lineOffset = 0;
        } else if (isAutoGrowingMultiline()) {
            lineOffset = 0;
        } else {
            // keep lineOffset as-is but can't exceed lastline
            lineOffset = Math.max(0, lineOffset);
        }
        textOffset = Math.max(0, textOffset);

        resetText();
        resetCursorPosition();
    }

    public String getText() {
        return text;
    }

    public String getDisplayText() {
        return bitmapText.getText();
    }

    /**
     * Set caret selection range [from, to]. For now selection rendering is not implemented.
     */
    protected void setCursor(int from, int to) {
        int len = text.length();
        caretFrom = clamp(from, 0, len);
        caretTo = clamp(to, 0, len);

        if (singleLine || isAutoGrowingMultiline()) lineOffset = 0;
        resetCursorPosition();
    }

    public void setCursorFromWorld(float x, float y) {
        if (!isAttached()) {
            return;
        }

        Vector3f local = getNode().worldToLocal(new Vector3f(x, y, 0), null);
        Vector3f textPos = bitmapText.getLocalTranslation();
        float textX = Math.max(0, local.x - textPos.x);
        float textY = local.y - textPos.y;
        int clickedLine = singleLine ? 0 : Math.max(0, (int) Math.floor(-textY / bitmapText.getLineHeight()));
        int lineStart = findLineStart(lineOffset + clickedLine);
        int lineEnd = findLineEnd(lineStart);
        int lineLength = lineEnd - lineStart;
        int visibleStart = Math.min(textOffset, lineLength);

        int lo = visibleStart;
        int hi = lineLength;
        while (lo < hi) {
            int mid = (lo + hi) >>> 1;
            float width = getVisibleWidth(outputTransform.apply(text.substring(lineStart + visibleStart, lineStart + mid)));
            if (width < textX) {
                lo = mid + 1;
            } else {
                hi = mid;
            }
        }

        int col = lo;
        if (col > visibleStart && col < lineLength) {
            float left = getVisibleWidth(outputTransform.apply(text.substring(lineStart + visibleStart, lineStart + col - 1)));
            float right = getVisibleWidth(outputTransform.apply(text.substring(lineStart + visibleStart, lineStart + col)));
            if ((textX - left) <= (right - textX)) {
                col--;
            }
        }
        setCursor(lineStart + col, lineStart + col);
    }

    private int findLineStart(int line) {
        int start = 0;
        int currentLine = 0;
        while (start < text.length() && currentLine < line) {
            if (text.charAt(start++) == '\n') {
                currentLine++;
            }
        }
        return start;
    }

    private int findLineEnd(int start) {
        int end = text.indexOf('\n', start);
        return end < 0 ? text.length() : end;
    }

    /**
     * Builds the string that is actually shown in BitmapText by applying: - lineOffset: drop first N logical
     * lines (explicit newlines only) - textOffset: drop first N columns from EACH remaining line
     *
     * Newlines are normalized to '\n' in the visible string.
     */
    private String buildVisibleText() {
        String s = text;
        if (s == null) s = "";

        int len = s.length();
        if (len == 0) {
            return "";
        }

        // advance to lineOffset (explicit newlines)
        int start = 0;
        int linesToSkip = Math.max(0, lineOffset);
        int skipped = 0;

        while (start < len && skipped < linesToSkip) {
            char c = s.charAt(start++);
            if (c == '\n') {
                skipped++;
            } else if (c == '\r') {
                // Treat \r\n as a single newline
                if (start < len && s.charAt(start) == '\n') {
                    start++;
                }
                skipped++;
            }
        }

        if (start >= len) {
            return "";
        }

        // apply horizontal trim to each line
        int hOff = Math.max(0, textOffset);
        if (hOff == 0) {
            // Still normalize newlines to '\n' so cursor math and BitmapText stay consistent
            return outputTransform.apply(normalizeNewlines(s.substring(start)));
        }

        StringBuilder out = new StringBuilder(Math.max(16, len - start));
        int col = 0;

        for (int i = start; i < len; i++) {
            char c = s.charAt(i);

            if (c == '\n') {
                out.append('\n');
                col = 0;
                continue;
            }

            if (c == '\r') {
                // normalize \r or \r\n to '\n'
                if (i + 1 < len && s.charAt(i + 1) == '\n') {
                    i++;
                }
                out.append('\n');
                col = 0;
                continue;
            }

            if (col >= hOff) {
                out.append(c);
            }
            col++;
        }

        return outputTransform.apply(out.toString());

    }

    private static String normalizeNewlines(String s) {
        if (s == null || s.isEmpty()) return s == null ? "" : s;

        // Fast path: no '\r'
        if (s.indexOf('\r') < 0) {
            return s;
        }

        StringBuilder out = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '\r') {
                if (i + 1 < s.length() && s.charAt(i + 1) == '\n') {
                    i++;
                }
                out.append('\n');
            } else {
                out.append(c);
            }
        }
        return out.toString();
    }

    /**
     * Updates BitmapText if needed.
     */
    protected void resetText() {
        if (singleLine || isAutoGrowingMultiline()) lineOffset = 0;

        String visible = buildVisibleText();

        if (visible.equals(bitmapText.getText())) {
            return;
        }

        bitmapText.setText(visible);
        invalidate();
    }

    private static final class CaretInfo {
        int line; // 0-based explicit line index
        int column; // 0-based column within that explicit line
        int lineStart; // absolute char index in original string

    }

    private CaretInfo caretInfo = new CaretInfo();

    private CaretInfo locateCaret(String s, int pos) {
        if (s == null) s = "";
        int len = s.length();
        pos = clamp(pos, 0, len);

        int line = 0;
        int lineStart = 0;

        for (int i = 0; i < pos && i < len; i++) {
            char c = s.charAt(i);
            if (c == '\n') {
                line++;
                lineStart = i + 1;
            } else if (c == '\r') {
                // treat \r\n as one break
                if (i + 1 < len && s.charAt(i + 1) == '\n') {
                    i++;
                }
                line++;
                lineStart = i + 1;
            }
        }

        int col = pos - lineStart;
        if (col < 0) col = 0;
        caretInfo.line = line;
        caretInfo.column = col;
        caretInfo.lineStart = lineStart;
        return caretInfo;
    }

    private int findLineEnd(String s, int lineStart) {
        int len = s.length();
        for (int i = lineStart; i < len; i++) {
            char c = s.charAt(i);
            if (c == '\n' || c == '\r') {
                return i;
            }
        }
        return len;
    }

    private String extractLineText(String s, int lineStart) {
        if (s == null) return "";
        int start = clamp(lineStart, 0, s.length());
        int end = findLineEnd(s, start);
        if (end < start) end = start;
        return s.substring(start, end);
    }

    protected void resetCursorPosition() {
        if (cursor == null) return;
        String sourceText = getText();

        int caretPos = caretTo;
        int len = sourceText.length();
        caretPos = clamp(caretPos, 0, len);

        for (int pass = 0; pass < 16; pass++) {

            boolean changedOffsets = false;

            CaretInfo ci = locateCaret(sourceText, caretPos);
            int caretLine = ci.line;
            int caretColumn = ci.column;

            // Single-line mode: no vertical scroll
            if ((singleLine || isAutoGrowingMultiline()) && lineOffset != 0) {
                lineOffset = 0;
                changedOffsets = true;
            }

            // Vertical scroll (explicit lines only), if we have a box
            if (!singleLine && !isAutoGrowingMultiline() && textBox != null) {
                int visibleLines = Math.max(1, (int) Math.floor(textBox.height / bitmapText.getLineHeight()));
                int visibleLineIndex = caretLine - lineOffset;

                if (visibleLineIndex < 0) {
                    lineOffset = caretLine;
                    changedOffsets = true;
                } else if (visibleLineIndex >= visibleLines) {
                    lineOffset = Math.max(0, caretLine - visibleLines + 1);
                    changedOffsets = true;
                }
            }

            // Horizontal scroll (per-line columns)
            if (caretColumn < textOffset) {
                textOffset = caretColumn;
                changedOffsets = true;
            }

            if (changedOffsets) {
                resetText();
                continue;
            }

            // compute cursor position on the visible line/column
            int visibleLine = caretLine - lineOffset;
            if (visibleLine < 0) visibleLine = 0;

            String lineText = extractLineText(sourceText, ci.lineStart);

            int start = Math.min(textOffset, lineText.length());
            int end = Math.min(caretColumn, lineText.length());
            if (end < start) end = start;

            String prefix = outputTransform.apply(lineText.substring(start, end));

            float x;
            if (font.isRightToLeft()) {
                x = font.getLineWidth(prefix);
            } else {
                x = font.getLineWidth(prefix + " ");
                x -= font.getLineWidth(" ");
            }

            float scale = bitmapText.getSize() / font.getPreferredSize();
            x *= scale;

            if (font.isRightToLeft()) {
                float maxWidth;
                if (preferredWidth != 0) {
                    maxWidth = preferredWidth;
                } else if (textBox != null) {
                    maxWidth = textBox.width;
                } else {
                    // Fallback: measure the whole line (not the whole document)
                    maxWidth = font.getLineWidth(outputTransform.apply(lineText)) * scale;
                }
                x = maxWidth - x;
            }

            float y = cursorY(visibleLine);

            // If caret is out of horizontal bounds, scroll right (works in singleLine and multiline)
            if (textBox != null && x > textBox.width && textOffset < caretColumn) {
                textOffset++;
                resetText();
                continue;
            }

            // If caret is "too far left" due to offset, it'll be at x=0 already.
            // Cursor visibility: hide if outside vertical box
            if (textBox != null && !singleLine) {
                int visibleLines = Math.max(1, (int) Math.floor(textBox.height / bitmapText.getLineHeight()));
                if (visibleLine < 0 || visibleLine >= visibleLines) {
                    cursorVisible = false;
                    resetCursorState();
                    return;
                }
            }

            cursorVisible = true;
            resetCursorState();

            cursor.setLocalTranslation(x - getCursorWidth() * 0.5f, y, 0.01f);
            return;
        }

        // If we failed to stabilize, just hide cursor rather than jitter.
        cursorVisible = false;
        resetCursorState();
    }

    public void setHAlignment(HAlignment a) {
        if (hAlign == a) return;
        hAlign = a;
        resetAlignment();
    }

    float cursorY(int visibleLine) {
        float lineHeight = bitmapText.getLineHeight();
        float y = -(visibleLine + 1) * lineHeight;
        if (!singleLine || textBox == null) {
            return y;
        }

        float remainingHeight = Math.max(0f, textBox.height - lineHeight);
        return switch (vAlign) {
            case Top -> y;
            case Center -> y - remainingHeight * 0.5f;
            case Bottom -> y - remainingHeight;
        };
    }

    public HAlignment getHAlignment() {
        return hAlign;
    }

    public void setVAlignment(VAlignment a) {
        if (vAlign == a) return;
        vAlign = a;
        resetAlignment();
        resetCursorPosition();
    }

    public VAlignment getVAlignment() {
        return vAlign;
    }

    public void setPreferredSize(Vector3f v) {
        this.preferredSize = v;
        invalidate();
    }

    public Vector3f getPreferredSize() {
        return preferredSize;
    }

    public void setPreferredWidth(float f) {
        this.preferredWidth = f;
        invalidate();
    }

    public float getPreferredWidth() {
        return preferredWidth;
    }

    public void setPreferredLineCount(int i) {
        this.preferredLineCount = i;
        invalidate();
    }

    public float getPreferredLineCount() {
        return preferredLineCount;
    }

    @Override
    public void reshape(Vector3f pos, Vector3f size) {
        bitmapText.setLocalTranslation(pos.x, pos.y, pos.z);
        textBox = new Rectangle(0, 0, size.x, size.y);
        bitmapText.setBox(textBox);
        resetAlignment();

        // Box changed: offsets might need adjusting (especially vertical)
        resetText();
        resetCursorPosition();
    }

    @Override
    public void calculatePreferredSize(Vector3f size) {
        if (preferredSize != null) {
            size.set(preferredSize);
            return;
        }

        if (preferredWidth == 0) {
            if (singleLine) {
                bitmapText.setBox(null);
                size.x = bitmapText.getLineWidth();
                bitmapText.setBox(textBox);
            } else {
                String displayText = outputTransform.apply(normalizeNewlines(text));
                float maxLineWidth = 0;
                int lineStart = 0;
                for (int i = 0; i < displayText.length(); i++) {
                    if (displayText.charAt(i) == '\n') {
                        maxLineWidth = Math.max(maxLineWidth, getVisibleWidth(displayText.substring(lineStart, i)));
                        lineStart = i + 1;
                    }
                }
                size.x = Math.max(maxLineWidth, getVisibleWidth(displayText.substring(lineStart)));
            }
        } else {
            size.x = preferredWidth;
        }
        if (preferredLineCount == 0) {
            if (singleLine) {
                bitmapText.setBox(null);
                size.y = bitmapText.getHeight();
                bitmapText.setBox(textBox);
            } else {
                int lineCount = 1;
                String normalized = normalizeNewlines(text);
                for (int i = 0; i < normalized.length(); i++) {
                    if (normalized.charAt(i) == '\n') {
                        lineCount++;
                    }
                }
                size.y = bitmapText.getLineHeight() * lineCount;
            }
        } else {
            size.y = bitmapText.getLineHeight() * preferredLineCount;
        }
        size.x += 0.01f;
    }

    protected void resetAlignment() {
        if (textBox == null) return;

        switch (hAlign) {
            case Left:
                bitmapText.setAlignment(BitmapFont.Align.Left);
                break;
            case Right:
                bitmapText.setAlignment(BitmapFont.Align.Right);
                break;
            case Center:
                bitmapText.setAlignment(BitmapFont.Align.Center);
                break;
        }

        switch (vAlign) {
            case Top:
                bitmapText.setVerticalAlignment(BitmapFont.VAlign.Top);
                break;
            case Bottom:
                bitmapText.setVerticalAlignment(BitmapFont.VAlign.Bottom);
                break;
            case Center:
                bitmapText.setVerticalAlignment(BitmapFont.VAlign.Center);
                break;
        }
    }

    @Override
    public void focusAction(boolean pressed) {

    }

    @Override
    public void focusScrollUpdate(ScrollDirection dir, double value) {
    }

    public void openKeyboard() {
        openKeyboard(caretTo, caretTo);
    }

    private boolean isAutoGrowingMultiline() {
        return !singleLine && preferredSize == null && preferredLineCount == 0;
    }

    private void openKeyboard(int cursorStart, int cursorEnd) {
        if (getGuiControl() == null) {
            return;
        }
        if (closeKeyboard != null) {
            closeKeyboard.run();
        }
        ImeCompositionEvent event = new ImeCompositionEvent(getText(), !singleLine);
        event.setSelection(cursorStart, cursorEnd);
        NGEGui.get(getNode()).openKeyboard((ev) -> {
            setText(ev.getText());
            setCursor(ev.getCursorStart(), ev.getCursorEnd());
            if (ev.isFinished()) {
                closeKeyboard = null;
                focused = false;
                resetCursorState();
            } else {
                closeKeyboard = ev::end;
                focused = true;
                resetCursorState();
            }
        }, event, inputTransform, (s) -> bitmapText.getFont().getLineWidth(s));
    }

    @Override
    public void focusScrollUpdate(Spatial target, ScrollDirection dir, double value) {
    }

    private static int clamp(int v, int min, int max) {
        if (v < min) return min;
        if (v > max) return max;
        return v;
    }
}
