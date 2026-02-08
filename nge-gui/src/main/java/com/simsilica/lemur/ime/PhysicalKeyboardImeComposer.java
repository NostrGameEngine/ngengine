/**
 * Copyright (c) 2026, Nostr Game Engine
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
 * 
 * #########################################
 * 
 * nge-gui is built and based on Lemur, which is licensed under the BSD 3-Clause License.
 * - Copyright (c) 2012-2026 jMonkeyEngine All rights reserved. 
 * - Copyright (c) 2016-2026, Simsilica, LLC All rights reserved.
 * 
 * https://github.com/jMonkeyEngine-Contributions/Lemur
 */

package com.simsilica.lemur.ime;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;

import com.jme3.input.InputManager;
import com.jme3.input.KeyInput;
import com.jme3.input.RawInputListener;
import com.jme3.input.event.JoyAxisEvent;
import com.jme3.input.event.JoyButtonEvent;
import com.jme3.input.event.KeyInputEvent;
import com.jme3.input.event.MouseButtonEvent;
import com.jme3.input.event.MouseMotionEvent;
import com.jme3.input.event.TouchEvent;
import com.jme3.scene.Spatial;
import com.simsilica.lemur.GuiContext;
import com.simsilica.lemur.nav.NavigatorListener;
import com.simsilica.lemur.nav.ScrollDirection;
import com.simsilica.lemur.nav.TraversalDirection;

/**
 *  A Text composer that binds to the physical keyboard using a RawInputListener.
 * 
 *  @author Riccardo Balbo, GPT-5.2
 */
public class PhysicalKeyboardImeComposer implements ImeComposer, RawInputListener {

    private ImeCompositionEvent currentEvent = null;
    private Consumer<ImeCompositionEvent> listener = null;
    private final InputManager inputManager;
    private Function<Character, Character> inputTransform = c -> c;

    private boolean shiftDown;
    private boolean ctrlDown;
    private Integer selectionAnchor;
    private GuiContext ctx;

    private Float preferredCaretX = null;

    private Function<String, Float> getLineWidth = null;

    private final Map<Integer, Consumer<KeyInputEvent>> keyActions = new HashMap<>();
    private final Map<Integer, Consumer<KeyInputEvent>> ctrlKeyActions = new HashMap<>();

    private final NavigatorListener navigatorBlocker = new NavigatorListener() {
        public boolean beforeNavigatorScroll(ScrollDirection dir, double delta) {
            return true;
        }

        public boolean beforeNavigatorAction(boolean pressed) {
            return true;
        }

        public boolean beforeNavigatorNavigate(TraversalDirection dir) {
            return false;
        }

        public boolean beforeNavigatorFocus(Spatial newFocus) {
            return true;
        }

        public boolean beforeNavigatorNavigateTo(TraversalDirection dir, Spatial from, Spatial to) {
            return true;
        }
    };

    public PhysicalKeyboardImeComposer(InputManager inputManager) {
        this.inputManager = inputManager;
        registerKeyActions();
    }

    private void registerKeyActions() {
        keyActions.put(KeyInput.KEY_ESCAPE, evt -> {
            resetPreferredCaretX();
            close();
        });

        keyActions.put(KeyInput.KEY_RETURN, evt -> {
            resetPreferredCaretX();
            if (!currentEvent.isMultiline()) {
                close();
            } else {
                currentEvent.insertAtCursor("\n");
            }
        });

        keyActions.put(KeyInput.KEY_LEFT, evt -> {
            resetPreferredCaretX();
            if (shiftDown) {
                if (selectionAnchor == null) selectionAnchor = currentEvent.getCursorStart();
                int caret = (currentEvent.getCursorStart() != currentEvent.getCursorEnd() ? Math.min(currentEvent.getCursorStart(),
                        currentEvent.getCursorEnd()) : currentEvent.getCursorStart()) - 1;
                selectRange(selectionAnchor, caret);
            } else {
                moveCursorTo(currentEvent.getCursorStart() != currentEvent.getCursorEnd()
                                                                           ? Math.min(currentEvent.getCursorStart(),
                                                                                   currentEvent.getCursorEnd())
                                                                           : currentEvent.getCursorStart() - 1);
                selectionAnchor = null;
            }
        });

        keyActions.put(KeyInput.KEY_RIGHT, evt -> {
            resetPreferredCaretX();
            if (shiftDown) {
                if (selectionAnchor == null) selectionAnchor = currentEvent.getCursorStart();
                int caret = (currentEvent.getCursorStart() != currentEvent.getCursorEnd() ? Math.max(currentEvent.getCursorStart(),
                        currentEvent.getCursorEnd()) : currentEvent.getCursorStart()) + 1;
                selectRange(selectionAnchor, caret);
            } else {
                moveCursorTo(currentEvent.getCursorStart() != currentEvent.getCursorEnd()
                                                                           ? Math.max(currentEvent.getCursorStart(),
                                                                                   currentEvent.getCursorEnd())
                                                                           : currentEvent.getCursorStart() + 1);
                selectionAnchor = null;
            }
        });

        keyActions.put(KeyInput.KEY_HOME, evt -> {
            resetPreferredCaretX();
            if (shiftDown) {
                if (selectionAnchor == null) selectionAnchor = currentEvent.getCursorStart();
                selectRange(selectionAnchor, 0);
            } else {
                moveCursorTo(0);
                selectionAnchor = null;
            }
        });

        keyActions.put(KeyInput.KEY_END, evt -> {
            resetPreferredCaretX();
            if (shiftDown) {
                if (selectionAnchor == null) selectionAnchor = currentEvent.getCursorStart();
                selectRange(selectionAnchor, currentEvent.getText().length());
            } else {
                moveCursorTo(currentEvent.getText().length());
                selectionAnchor = null;
            }
        });

        keyActions.put(KeyInput.KEY_BACK, evt -> {
            resetPreferredCaretX();
            backspace();
            selectionAnchor = null;
        });

        keyActions.put(KeyInput.KEY_DELETE, evt -> {
            resetPreferredCaretX();
            deleteForward();
            selectionAnchor = null;
        });

        // Up/Down keep same visual X (using prefix-width measurement when available)
        keyActions.put(KeyInput.KEY_UP, evt -> moveCaretVertically(false));
        keyActions.put(KeyInput.KEY_DOWN, evt -> moveCaretVertically(true));

        ctrlKeyActions.put(KeyInput.KEY_A, evt -> {
            resetPreferredCaretX();
            currentEvent.setSelection(0, currentEvent.getText().length());
        });
        ctrlKeyActions.put(KeyInput.KEY_LEFT, evt -> {
            resetPreferredCaretX();
            moveCursorTo(prevWordStart(currentEvent.getText(), currentEvent.getCursorStart()));
        });
        ctrlKeyActions.put(KeyInput.KEY_RIGHT, evt -> {
            resetPreferredCaretX();
            moveCursorTo(nextWordEnd(currentEvent.getText(), currentEvent.getCursorStart()));
        });
        ctrlKeyActions.put(KeyInput.KEY_BACK, evt -> {
            resetPreferredCaretX();
            deletePrevWord();
        });
        ctrlKeyActions.put(KeyInput.KEY_DELETE, evt -> {
            resetPreferredCaretX();
            deleteNextWord();
        });
        ctrlKeyActions.put(KeyInput.KEY_HOME, evt -> {
            resetPreferredCaretX();
            moveCursorTo(0);
        });
        ctrlKeyActions.put(KeyInput.KEY_END, evt -> {
            resetPreferredCaretX();
            moveCursorTo(currentEvent.getText().length());
        });
    }

    private void resetModifiers() {
        shiftDown = false;
        ctrlDown = false;
        selectionAnchor = null;
        preferredCaretX = null;
    }

    private void resetPreferredCaretX() {
        preferredCaretX = null;
    }

    @Override
    public boolean isOpen() {
        return currentEvent != null;
    }

    private void update() {
        if (!isOpen()) return;
        listener.accept(currentEvent);
    }

    @Override
    public void open(GuiContext ctx, Consumer<ImeCompositionEvent> listener, ImeCompositionEvent event,
            Function<Character, Character> inputFilter, Function<String, Float> getLineWidth) {

        if (isOpen()) close();

        this.inputTransform = inputFilter;
        this.getLineWidth = getLineWidth;

        this.ctx = ctx;
        this.currentEvent = event;
        this.currentEvent.setEndAction(this::close);
        this.listener = listener;

        resetModifiers();
        update();

        inputManager.addRawInputListener(this);
        this.ctx.getNavigator().addNavigatorListener(navigatorBlocker);
    }

    @Override
    public void close() {
        if (!isOpen()) return;

        this.ctx.getNavigator().removeNavigatorListener(navigatorBlocker);

        currentEvent.setFinished(true);
        update();

        listener = null;
        currentEvent = null;

        resetModifiers();
        getLineWidth = null;

        inputManager.removeRawInputListener(this);
    }

    @Override
    public void beginInput() {
    }

    @Override
    public void endInput() {
    }

    @Override
    public void onJoyAxisEvent(JoyAxisEvent evt) {
    }

    @Override
    public void onJoyButtonEvent(JoyButtonEvent evt) {
    }

    @Override
    public void onMouseMotionEvent(MouseMotionEvent evt) {
    }

    @Override
    public void onMouseButtonEvent(MouseButtonEvent evt) {
    }

    @Override
    public void onTouchEvent(TouchEvent evt) {
    }

    private void updateModifierState(KeyInputEvent evt) {
        int code = evt.getKeyCode();
        boolean down = evt.isPressed();

        switch (code) {
            case KeyInput.KEY_LSHIFT:
            case KeyInput.KEY_RSHIFT:
                shiftDown = down;
                if (!shiftDown) {
                    selectionAnchor = null;
                } else if (selectionAnchor == null && isOpen()) {
                    selectionAnchor = currentEvent.getCursorStart();
                }
                break;

            case KeyInput.KEY_LCONTROL:
            case KeyInput.KEY_RCONTROL:
                ctrlDown = down;
                break;

            default:
                break;
        }
    }

    private static boolean isModifierOnlyKey(int code) {
        switch (code) {
            case KeyInput.KEY_LSHIFT:
            case KeyInput.KEY_RSHIFT:
            case KeyInput.KEY_LCONTROL:
            case KeyInput.KEY_RCONTROL:
            case KeyInput.KEY_LMENU:
            case KeyInput.KEY_RMENU:
            case KeyInput.KEY_LMETA:
            case KeyInput.KEY_RMETA:
                return true;
            default:
                return false;
        }
    }

    private static boolean isPrintableChar(Character c) {
        if (c == null) return false;
        if (c <= 0) return false;
        return !Character.isISOControl(c);
    }

    private void runMappedAction(KeyInputEvent evt, Consumer<KeyInputEvent> action) {
        action.accept(evt);
        evt.setConsumed();
        update();
    }

    @Override
    public void onKeyEvent(KeyInputEvent evt) {
        updateModifierState(evt);

        if (!isOpen()) return;

        if (!evt.isPressed()) return;

        int code = evt.getKeyCode();
        if (isModifierOnlyKey(code)) return;

        Consumer<KeyInputEvent> action = null;
        if (ctrlDown) action = ctrlKeyActions.get(code);
        if (action == null) action = keyActions.get(code);

        if (action != null) {
            runMappedAction(evt, action);
            return;
        }

        Character c = evt.getKeyChar();
        boolean ctrlControlChar = ctrlDown && c != null && c > 0 && c < 32;

        if (isPrintableChar(c) && !ctrlControlChar) {
            resetPreferredCaretX();

            Character out = inputTransform.apply(c);
            evt.setConsumed();

            if (out != null) {
                currentEvent.insertAtCursor(String.valueOf(out));
                update();
            }
        }
    }


    private void moveCaretVertically(boolean down) {
        String s = currentEvent.getText();

        // Collapse selection to edge in travel direction
        int baseCaret;
        if (currentEvent.getCursorStart() != currentEvent.getCursorEnd()) {
            baseCaret = down ? Math.max(currentEvent.getCursorStart(), currentEvent.getCursorEnd())
                             : Math.min(currentEvent.getCursorStart(), currentEvent.getCursorEnd());
        } else {
            baseCaret = currentEvent.getCursorStart();
        }

        int curLineStart = lineStart(s, baseCaret);
        int curLineEnd = lineEnd(s, baseCaret); // exclusive, no '\n'

        // Initialize preferredCaretX from the current caret position
        if (preferredCaretX == null) {
            int col = clamp(baseCaret - curLineStart, 0, curLineEnd - curLineStart);
            preferredCaretX = caretXForColumnByPrefix(s, curLineStart, curLineEnd, col);
        }

        int targetPos;
        if (!down) {
            if (curLineStart == 0) {
                targetPos = 0;
            } else {
                int prevLineEnd = curLineStart - 1; // index of '\n'
                int prevLineStart = lineStart(s, prevLineEnd);
                int prevLineEndNoNl = prevLineEnd;
                int col = columnForXByPrefix(s, prevLineStart, prevLineEndNoNl, preferredCaretX);
                targetPos = prevLineStart + col;
            }
        } else {
            if (curLineEnd >= s.length()) {
                targetPos = s.length();
            } else {
                int nextLineStart = curLineEnd + 1;
                int nextLineEnd = lineEnd(s, nextLineStart);
                int col = columnForXByPrefix(s, nextLineStart, nextLineEnd, preferredCaretX);
                targetPos = nextLineStart + col;
            }
        }

        if (shiftDown) {
            if (selectionAnchor == null) selectionAnchor = currentEvent.getCursorStart();
            selectRange(selectionAnchor, targetPos);
        } else {
            moveCursorTo(targetPos);
            selectionAnchor = null;
        }
        // do not reset preferredCaretX so repeated up/down keeps same visual column
    }

    private int lineStart(String s, int pos) {
        int p = clamp(pos, 0, s.length());
        int i = s.lastIndexOf('\n', p - 1);
        return i < 0 ? 0 : i + 1;
    }

    private int lineEnd(String s, int pos) {
        int p = clamp(pos, 0, s.length());
        int i = s.indexOf('\n', p);
        return i < 0 ? s.length() : i;
    }

    private float caretXForColumnByPrefix(String fullText, int lineStart, int lineEnd, int col) {
        if (getLineWidth == null) return (float) col; // fallback: x == column
        String line = fullText.substring(lineStart, lineEnd);
        int c = clamp(col, 0, line.length());
        // prefix width is exact caret-x (includes kerning/shaping in context)
        return safeLineWidth(line.substring(0, c));
    }

    private int columnForXByPrefix(String fullText, int lineStart, int lineEnd, float x) {
        int len = clamp(lineEnd - lineStart, 0, Integer.MAX_VALUE);
        if (len == 0) return 0;

        if (getLineWidth == null) {
            return clamp(Math.round(x), 0, len); // fallback: x == column
        }

        String line = fullText.substring(lineStart, lineEnd);

        // Binary search the smallest col where prefixWidth(col) >= x
        int lo = 0;
        int hi = line.length(); // inclusive upper bound in our [0..len] caret positions
        while (lo < hi) {
            int mid = (lo + hi) >>> 1;
            float w = safeLineWidth(line.substring(0, mid));
            if (w < x) {
                lo = mid + 1;
            } else {
                hi = mid;
            }
        }

        int col = lo;
        if (col <= 0) return 0;
        if (col >= line.length()) return line.length();

        // Pick closer between col-1 and col
        float w0 = safeLineWidth(line.substring(0, col - 1));
        float w1 = safeLineWidth(line.substring(0, col));
        return (x - w0) <= (w1 - x) ? (col - 1) : col;
    }

    private float safeLineWidth(String line) {
        try {
            Float v = getLineWidth.apply(line);
            return v != null ? v.floatValue() : 0f;
        } catch (Exception e) {
            return 0f;
        }
    }

    // ----- Editing helpers (unchanged logic) -----

    private int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private void deleteSelection() {
        if (currentEvent.getCursorStart() != currentEvent.getCursorEnd()) {
            currentEvent.insertAtCursor("");
        }
    }

    private void backspace() {
        if (currentEvent.getCursorStart() != currentEvent.getCursorEnd()) {
            deleteSelection();
            return;
        }
        if (currentEvent.getCursorStart() <= 0) return;
        int start = currentEvent.getCursorStart() - 1;
        currentEvent.setSelection(start, currentEvent.getCursorStart());
        currentEvent.insertAtCursor("");
    }

    private void deleteForward() {
        if (currentEvent.getCursorStart() != currentEvent.getCursorEnd()) {
            deleteSelection();
            return;
        }
        if (currentEvent.getCursorStart() >= currentEvent.getText().length()) return;
        int end = currentEvent.getCursorStart() + 1;
        currentEvent.setSelection(currentEvent.getCursorStart(), end);
        currentEvent.insertAtCursor("");
    }

    private void moveCursorTo(int pos) {
        int clamped = clamp(pos, 0, currentEvent.getText().length());
        currentEvent.setCursor(clamped);
    }

    private void selectRange(int anchor, int caret) {
        int a = clamp(anchor, 0, currentEvent.getText().length());
        int b = clamp(caret, 0, currentEvent.getText().length());
        currentEvent.setSelection(Math.min(a, b), Math.max(a, b));
    }

    private int prevWordStart(String s, int pos) {
        int i = clamp(pos, 0, s.length());
        while (i > 0 && Character.isWhitespace(s.charAt(i - 1))) i--;
        while (i > 0 && !Character.isWhitespace(s.charAt(i - 1))) i--;
        return i;
    }

    private int nextWordEnd(String s, int pos) {
        int i = clamp(pos, 0, s.length());
        while (i < s.length() && Character.isWhitespace(s.charAt(i))) i++;
        while (i < s.length() && !Character.isWhitespace(s.charAt(i))) i++;
        return i;
    }

    private void deletePrevWord() {
        if (currentEvent.getCursorStart() != currentEvent.getCursorEnd()) {
            deleteSelection();
            return;
        }
        int start = prevWordStart(currentEvent.getText(), currentEvent.getCursorStart());
        if (start == currentEvent.getCursorStart()) return;

        currentEvent.setSelection(start, currentEvent.getCursorStart());
        currentEvent.insertAtCursor("");
    }

    private void deleteNextWord() {
        if (currentEvent.getCursorStart() != currentEvent.getCursorEnd()) {
            deleteSelection();
            return;
        }
        int end = nextWordEnd(currentEvent.getText(), currentEvent.getCursorStart());
        if (end == currentEvent.getCursorStart()) return;

        currentEvent.setSelection(currentEvent.getCursorStart(), end);
        currentEvent.insertAtCursor("");
    }
}
