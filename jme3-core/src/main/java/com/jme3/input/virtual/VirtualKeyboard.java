/*
 * Copyright (c) 2009-2026 jMonkeyEngine
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
package com.jme3.input.virtual;

import com.jme3.asset.AssetManager;
import com.jme3.font.BitmapFont;
import com.jme3.font.BitmapText;
import com.jme3.font.Rectangle;
import com.jme3.input.JoystickButton;
import com.jme3.input.KeyInput;
import com.jme3.input.RawInputListener;
import com.jme3.input.event.InputEvent;
import com.jme3.input.event.KeyInputEvent;
import com.jme3.input.icons.JoystickButtonIcons;
import com.jme3.material.Material;
import com.jme3.material.RenderState.BlendMode;
import com.jme3.math.ColorRGBA;
import com.jme3.renderer.queue.RenderQueue.Bucket;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.shape.Quad;
import com.jme3.texture.Texture;
import com.jme3.texture.Texture2D;
import com.jme3.ui.Picture;
import java.util.ArrayDeque;

/**
 * Fixed on-screen keyboard used by desktop backends when no native software
 * keyboard exists.
 */
public final class VirtualKeyboard {

    public enum IconStyle {
        OUTLINE,
        FULL
    }

    private static final String ROOT_NAME = "Virtual Keyboard";
    private static final String LAYER_USER_DATA = "layer";
    private static final String EFFECTIVE_LAYER_USER_DATA = "effectiveLayer";
    private static final int KEYBOARD_LAYER = 20_000;
    private static final float KEYBOARD_Z = 100_000f;
    private static final Key[][] LAYOUT = {
            {
                    key(KeyInput.KEY_1, '1', '!', "1"),
                    key(KeyInput.KEY_2, '2', '@', "2"),
                    key(KeyInput.KEY_3, '3', '#', "3"),
                    key(KeyInput.KEY_4, '4', '$', "4"),
                    key(KeyInput.KEY_5, '5', '%', "5"),
                    key(KeyInput.KEY_6, '6', '^', "6"),
                    key(KeyInput.KEY_7, '7', '&', "7"),
                    key(KeyInput.KEY_8, '8', '*', "8"),
                    key(KeyInput.KEY_9, '9', '(', "9"),
                    key(KeyInput.KEY_0, '0', ')', "0"),
                    key(KeyInput.KEY_MINUS, '-', '_', "-"),
                    key(KeyInput.KEY_EQUALS, '=', '+', "="),
                    key(KeyInput.KEY_BACK, '\0', "Backspace", 1.8f)
            },
            {
                    key(KeyInput.KEY_Q, 'q', "Q"),
                    key(KeyInput.KEY_W, 'w', "W"),
                    key(KeyInput.KEY_E, 'e', "E"),
                    key(KeyInput.KEY_R, 'r', "R"),
                    key(KeyInput.KEY_T, 't', "T"),
                    key(KeyInput.KEY_Y, 'y', "Y"),
                    key(KeyInput.KEY_U, 'u', "U"),
                    key(KeyInput.KEY_I, 'i', "I"),
                    key(KeyInput.KEY_O, 'o', "O"),
                    key(KeyInput.KEY_P, 'p', "P"),
                    key(KeyInput.KEY_LBRACKET, '[', '{', "["),
                    key(KeyInput.KEY_RBRACKET, ']', '}', "]"),
                    key(KeyInput.KEY_BACKSLASH, '\\', '|', "\\")
            },
            {
                    key(KeyInput.KEY_A, 'a', "A"),
                    key(KeyInput.KEY_S, 's', "S"),
                    key(KeyInput.KEY_D, 'd', "D"),
                    key(KeyInput.KEY_F, 'f', "F"),
                    key(KeyInput.KEY_G, 'g', "G"),
                    key(KeyInput.KEY_H, 'h', "H"),
                    key(KeyInput.KEY_J, 'j', "J"),
                    key(KeyInput.KEY_K, 'k', "K"),
                    key(KeyInput.KEY_L, 'l', "L"),
                    key(KeyInput.KEY_SEMICOLON, ';', ':', ";"),
                    key(KeyInput.KEY_APOSTROPHE, '\'', '"', "'"),
                    key(KeyInput.KEY_RETURN, '\n', "Enter", 1.6f)
            },
            {
                    toggle(KeyInput.KEY_LSHIFT, "Shift", 1.8f),
                    key(KeyInput.KEY_Z, 'z', "Z"),
                    key(KeyInput.KEY_X, 'x', "X"),
                    key(KeyInput.KEY_C, 'c', "C"),
                    key(KeyInput.KEY_V, 'v', "V"),
                    key(KeyInput.KEY_B, 'b', "B"),
                    key(KeyInput.KEY_N, 'n', "N"),
                    key(KeyInput.KEY_M, 'm', "M"),
                    key(KeyInput.KEY_GRAVE, '`', '~', "`"),
                    key(KeyInput.KEY_COMMA, ',', '<', ","),
                    key(KeyInput.KEY_PERIOD, '.', '>', "."),
                    key(KeyInput.KEY_SLASH, '/', '?', "/")
            },
            {
                    key(KeyInput.KEY_SPACE, ' ', "Space", 7.5f)
            }
    };

    private static final VirtualKeyboard INSTANCE = new VirtualKeyboard();

    private ArrayDeque<InputEvent> events = new ArrayDeque<>();
    private ArrayDeque<InputEvent> readyEvents = new ArrayDeque<>();
    private final Object lock = new Object();

    private volatile boolean visible;
    private volatile IconStyle iconStyle = IconStyle.OUTLINE;
    private volatile boolean hasEvents;
    private Node visualRoot;
    private Node visualParent;
    private int visualWidth;
    private int visualHeight;
    private BitmapFont font;
    private Key pressedKey;
    private boolean shift;
    private int selectedRow;
    private int selectedColumn;

    private VirtualKeyboard() {
    }

    public static VirtualKeyboard getInstance() {
        return INSTANCE;
    }

    public boolean isVisible() {
        return visible;
    }

    public void setVisible(boolean visible) {
        synchronized (lock) {
            this.visible = visible;
            pressedKey = null;
            if (!visible) {
                shift = false;
            } else {
                selectedRow = Math.max(0, Math.min(selectedRow, LAYOUT.length - 1));
                selectedColumn = Math.max(0, Math.min(selectedColumn, LAYOUT[selectedRow].length - 1));
            }
        }
    }

    public IconStyle getIconStyle() {
        return iconStyle;
    }

    public void setIconStyle(IconStyle iconStyle) {
        this.iconStyle = iconStyle == null ? IconStyle.OUTLINE : iconStyle;
        synchronized (lock) {
            visualWidth = 0;
            visualHeight = 0;
        }
    }

    public boolean onPointerDown(int pointerId, float x, float y, long time) {
        if (pointerId != 0) {
            return false;
        }
        synchronized (lock) {
            if (!visible) {
                return false;
            }
            Key key = keyAt(x, y);
            pressedKey = key;
            select(key);
            return key != null || containsKeyboard(x, y);
        }
    }

    public boolean onPointerMove(int pointerId, float x, float y, long time) {
        synchronized (lock) {
            if (!visible) {
                return false;
            }
            Key key = keyAt(x, y);
            if (key != null) {
                select(key);
            }
            return visible && (pressedKey != null || containsKeyboard(x, y));
        }
    }

    public boolean onPointerUp(int pointerId, float x, float y, long time) {
        if (pointerId != 0) {
            return false;
        }
        synchronized (lock) {
            if (!visible) {
                return false;
            }
            Key upKey = keyAt(x, y);
            select(upKey);
            Key downKey = pressedKey;
            pressedKey = null;
            if (downKey != null && downKey == upKey) {
                press(downKey, time);
                return true;
            }
            return downKey != null || containsKeyboard(x, y);
        }
    }

    public boolean navigate(int x, int y, long time) {
        synchronized (lock) {
            if (!visible) {
                return false;
            }
            if (x != 0 && y != 0) {
                if (Math.abs(x) >= Math.abs(y)) {
                    y = 0;
                } else {
                    x = 0;
                }
            }
            selectedRow = Math.max(0, Math.min(LAYOUT.length - 1, selectedRow));
            selectedColumn = Math.max(0, Math.min(LAYOUT[selectedRow].length - 1, selectedColumn));

            if (x != 0) {
                selectedColumn = Math.max(0, Math.min(LAYOUT[selectedRow].length - 1, selectedColumn + x));
            }
            if (y != 0) {
                Key current = LAYOUT[selectedRow][selectedColumn];
                int targetRow = Math.max(0, Math.min(LAYOUT.length - 1, selectedRow - y));
                Key[] targetKeys = LAYOUT[targetRow];
                if (current.width > 0f && targetKeys.length > 0 && targetKeys[0].width > 0f) {
                    float currentCenter = current.x + current.width * 0.5f;
                    int closestColumn = 0;
                    float closestDistance = Float.MAX_VALUE;
                    for (int i = 0; i < targetKeys.length; i++) {
                        Key key = targetKeys[i];
                        float distance = Math.abs((key.x + key.width * 0.5f) - currentCenter);
                        if (distance < closestDistance) {
                            closestDistance = distance;
                            closestColumn = i;
                        }
                    }
                    selectedColumn = closestColumn;
                } else {
                    selectedColumn = Math.max(0, Math.min(targetKeys.length - 1, selectedColumn));
                }
                selectedRow = targetRow;
            }
            return true;
        }
    }

    public boolean action(boolean pressed, long time) {
        synchronized (lock) {
            if (!visible) {
                return false;
            }
            if (!pressed) {
                press(LAYOUT[selectedRow][selectedColumn], time);
            }
            return true;
        }
    }

    public boolean cancel(boolean pressed, long time) {
        synchronized (lock) {
            if (!visible) {
                return false;
            }
            if (!pressed) {
                enqueue(new KeyInputEvent(KeyInput.KEY_ESCAPE, '\0', true, false), time);
                enqueue(new KeyInputEvent(KeyInput.KEY_ESCAPE, '\0', false, false), time);
                setVisible(false);
            }
            return true;
        }
    }

    public boolean key(int keyCode, long time) {
        synchronized (lock) {
            if (!visible) {
                return false;
            }
            enqueue(new KeyInputEvent(keyCode, '\0', true, false), time);
            enqueue(new KeyInputEvent(keyCode, '\0', false, false), time);
            return true;
        }
    }

    public void dispatchEvents(RawInputListener listener) {
        if (!hasEvents) {
            return;
        }

        synchronized (lock) {
            if (!hasEvents) {
                return;
            }
            if (listener == null) {
                events.clear();
                readyEvents.clear();
                hasEvents = false;
                return;
            }
            ArrayDeque<InputEvent> pendingEvents = events;
            events = readyEvents;
            readyEvents = pendingEvents;
            hasEvents = false;
        }

        InputEvent event;
        while ((event = readyEvents.poll()) != null) {
            if (event instanceof KeyInputEvent) {
                listener.onKeyEvent((KeyInputEvent) event);
            }
        }
    }

    public void updateVisuals(Node parent, AssetManager assetManager, int width, int height, float tpf) {
        if (parent == null || assetManager == null || width <= 0 || height <= 0) {
            return;
        }
        if (visualRoot == null) {
            visualRoot = new Node(ROOT_NAME);
            visualRoot.setQueueBucket(Bucket.Gui);
        }
        attachVisualRootOnTop(parent);

        if (!visible) {
            if (visualRoot.getQuantity() > 0) {
                visualRoot.detachAllChildren();
            }
            return;
        }

        if (width != visualWidth || height != visualHeight || visualRoot.getQuantity() == 0) {
            visualWidth = width;
            visualHeight = height;
            buildVisuals(assetManager, width, height);
        }
        updateKeyStyles();
    }

    private void press(Key key, long time) {
        if (key.toggle) {
            shift = !shift;
            enqueue(new KeyInputEvent(key.code, '\0', true, false), time);
            enqueue(new KeyInputEvent(key.code, '\0', false, false), time);
            return;
        }

        char character = key.character;
        if (shift && key.shiftCharacter != '\0') {
            character = key.shiftCharacter;
        } else if (shift && Character.isLetter(character)) {
            character = Character.toUpperCase(character);
        }
        int code = key.isTextKey() ? KeyInput.KEY_UNKNOWN : key.code;
        enqueue(new KeyInputEvent(code, character, true, false), time);
        enqueue(new KeyInputEvent(code, character, false, false), time);
        if (shift && key.isTextKey()) {
            shift = false;
        }
    }

    private void select(Key key) {
        if (key == null) {
            return;
        }
        selectedRow = key.rowIndex;
        selectedColumn = key.columnIndex;
    }

    private void enqueue(KeyInputEvent event, long time) {
        event.setTime(time);
        events.add(event);
        hasEvents = true;
    }

    private Key keyAt(float x, float y) {
        for (Key[] row : LAYOUT) {
            for (Key key : row) {
                if (key.contains(x, y)) {
                    return key;
                }
            }
        }
        return null;
    }

    private boolean containsKeyboard(float x, float y) {
        if (LAYOUT.length == 0 || LAYOUT[0].length == 0) {
            return false;
        }
        Key first = LAYOUT[0][0];
        Key lastRowFirst = LAYOUT[LAYOUT.length - 1][0];
        return x >= first.x
                && x <= first.x + first.keyboardWidth
                && y >= lastRowFirst.y
                && y <= first.y + first.height;
    }

    private void attachVisualRootOnTop(Node parent) {
        visualRoot.setLocalTranslation(0f, 0f, KEYBOARD_Z);
        if (!Integer.valueOf(KEYBOARD_LAYER).equals(visualRoot.getUserData(LAYER_USER_DATA))) {
            visualRoot.setUserData(LAYER_USER_DATA, KEYBOARD_LAYER);
            visualRoot.setUserData(EFFECTIVE_LAYER_USER_DATA, null);
        }
        if (visualParent != parent || visualRoot.getParent() != parent) {
            visualRoot.removeFromParent();
            parent.attachChild(visualRoot);
            visualParent = parent;
            return;
        }

        int childIndex = parent.getChildIndex(visualRoot);
        int topIndex = parent.getQuantity() - 1;
        if (childIndex >= 0 && childIndex < topIndex) {
            visualRoot.removeFromParent();
            parent.attachChild(visualRoot);
        }
    }

    private void buildVisuals(AssetManager assetManager, int width, int height) {
        visualRoot.detachAllChildren();

        float edge = Math.max(10f, width * 0.018f);
        float padding = Math.max(10f, width * 0.014f);
        float gap = Math.max(4f, width * 0.006f);
        float keyboardWidth = width - edge * 2f - padding * 2f;
        float unit = (keyboardWidth - (widestRowKeys() - 1) * gap) / widestRowUnits();
        float keyHeight = Math.max(34f, Math.min(unit * 0.68f, height * 0.09f));
        float keyboardHeight = LAYOUT.length * keyHeight + (LAYOUT.length - 1) * gap;
        float hintAreaHeight = Math.max(18f, keyHeight * 0.42f);
        float backgroundHeight = keyboardHeight + padding * 2f + hintAreaHeight;
        float startX = edge + padding;
        float startY = padding + hintAreaHeight;

        Geometry background = new Geometry("Virtual Keyboard Background",
                new Quad(width, backgroundHeight));
        background.setMaterial(createMaterial(assetManager, new ColorRGBA(0.010f, 0.012f, 0.018f, 0.97f)));
        background.setQueueBucket(Bucket.Gui);
        background.setLocalTranslation(0f, 0f, 0f);
        visualRoot.attachChild(background);

        if (font == null) {
            font = assetManager.loadFont("Interface/Fonts/Default.fnt");
        }

        BitmapText hint = new BitmapText(font);
        hint.setText("Close");
        hint.setSize(Math.max(12f, keyHeight * 0.28f));
        hint.setColor(new ColorRGBA(1f, 1f, 1f, 0.62f));
        float hintIconSize = Math.max(16f, hint.getLineHeight() * 0.82f);
        float hintGap = Math.max(5f, hintIconSize * 0.32f);
        float hintWidth = hintIconSize + hintGap + hint.getLineWidth();
        float hintX = width - edge - padding - hintWidth;
        float hintCenterY = padding + hintAreaHeight * 0.5f;
        float hintTextY = hintCenterY + hint.getHeight() * 0.5f;
        Texture bTexture = JoystickButtonIcons.getIcon(assetManager, null, JoystickButton.BUTTON_XBOX_B, "B");
        if (bTexture instanceof Texture2D) {
            Picture bIcon = new Picture("Virtual Keyboard Close Button Hint");
            bIcon.setTexture(assetManager, (Texture2D) bTexture, true);
            bIcon.setWidth(hintIconSize);
            bIcon.setHeight(hintIconSize);
            bIcon.setLocalTranslation(hintX, hintCenterY - hintIconSize * 0.5f, 1f);
            visualRoot.attachChild(bIcon);
            hint.setLocalTranslation(hintX + hintIconSize + hintGap, hintTextY, 1f);
        } else {
            hint.setText("B  Close");
            hint.setLocalTranslation(width - edge - padding - hint.getLineWidth(), hintTextY, 1f);
        }
        visualRoot.attachChild(hint);

        for (int rowIndex = 0; rowIndex < LAYOUT.length; rowIndex++) {
            Key[] row = LAYOUT[rowIndex];
            float rowWidth = 0f;
            for (Key key : row) {
                rowWidth += key.widthUnits * unit;
            }
            rowWidth += (row.length - 1) * gap;

            float x = startX + (keyboardWidth - rowWidth) * 0.5f;
            float y = startY + (LAYOUT.length - rowIndex - 1) * (keyHeight + gap);
            for (int columnIndex = 0; columnIndex < row.length; columnIndex++) {
                Key key = row[columnIndex];
                float keyWidth = key.widthUnits * unit;
                key.rowIndex = rowIndex;
                key.columnIndex = columnIndex;
                key.x = x;
                key.y = y;
                key.width = keyWidth;
                key.height = keyHeight;
                key.keyboardWidth = keyboardWidth;

                Node keyNode = new Node("Virtual Keyboard " + key.label);
                Geometry border = new Geometry("border", new Quad(keyWidth, keyHeight));
                Geometry fill = new Geometry("fill", new Quad(keyWidth - 4f, keyHeight - 4f));
                BitmapText label = new BitmapText(font);

                border.setMaterial(createMaterial(assetManager, ColorRGBA.White));
                fill.setMaterial(createMaterial(assetManager, ColorRGBA.White));
                border.setQueueBucket(Bucket.Gui);
                fill.setQueueBucket(Bucket.Gui);
                fill.setLocalTranslation(2f, 2f, 0.02f);
                label.setQueueBucket(Bucket.Gui);
                label.setBox(new Rectangle(0f, keyHeight, keyWidth, keyHeight));
                label.setAlignment(BitmapFont.Align.Center);
                label.setVerticalAlignment(BitmapFont.VAlign.Center);
                label.setLocalTranslation(0f, 0f, 0.05f);

                keyNode.attachChild(border);
                keyNode.attachChild(fill);
                keyNode.attachChild(label);
                keyNode.setLocalTranslation(x, y, 1f);

                key.node = keyNode;
                key.border = border;
                key.fill = fill;
                key.text = label;
                updateKeyVisual(key, false);
                visualRoot.attachChild(keyNode);
                x += keyWidth + gap;
            }
        }
    }

    private void updateKeyStyles() {
        selectedRow = Math.max(0, Math.min(LAYOUT.length - 1, selectedRow));
        selectedColumn = Math.max(0, Math.min(LAYOUT[selectedRow].length - 1, selectedColumn));
        for (Key[] row : LAYOUT) {
            for (Key key : row) {
                boolean active = key.isSelected(selectedRow, selectedColumn) || (shift && key.toggle);
                if (key.node != null) {
                    updateKeyVisual(key, active);
                }
            }
        }
    }

    private void updateKeyVisual(Key key, boolean active) {
        ColorRGBA borderColor = active
                ? new ColorRGBA(0.56f, 0.88f, 1f, 0.95f)
                : new ColorRGBA(0.34f, 0.42f, 0.50f, 0.70f);
        ColorRGBA fillColor = active
                ? new ColorRGBA(0.76f, 0.90f, 1f, 0.94f)
                : new ColorRGBA(0.075f, 0.090f, 0.115f, 0.92f);
        ColorRGBA textColor = active
                ? new ColorRGBA(0.020f, 0.030f, 0.045f, 1f)
                : new ColorRGBA(0.93f, 0.96f, 1f, 0.88f);

        key.border.getMaterial().setColor("Color", borderColor);
        key.fill.getMaterial().setColor("Color", fillColor);
        key.text.setColor(textColor);

        String label = key.label;
        if (shift && key.shiftCharacter != '\0') {
            label = String.valueOf(key.shiftCharacter);
        } else if (key.character != '\0' && Character.isLetter(key.character)) {
            label = shift ? label.toUpperCase() : label.toLowerCase();
        }
        key.text.setText(label);
        key.text.setSize(Math.max(13f, key.height * (label.length() > 3 ? 0.26f : 0.34f)));
    }

    private Material createMaterial(AssetManager assetManager, ColorRGBA color) {
        Material material = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        material.setColor("Color", color);
        material.getAdditionalRenderState().setBlendMode(BlendMode.Alpha);
        return material;
    }

    private float widestRowUnits() {
        float max = 0f;
        for (Key[] row : LAYOUT) {
            float rowUnits = 0f;
            for (Key key : row) {
                rowUnits += key.widthUnits;
            }
            max = Math.max(max, rowUnits);
        }
        return max;
    }

    private int widestRowKeys() {
        int max = 0;
        for (Key[] row : LAYOUT) {
            max = Math.max(max, row.length);
        }
        return max;
    }

    private static Key key(int code, char character, String label) {
        return new Key(code, character, '\0', label, 1f, false);
    }

    private static Key key(int code, char character, char shiftCharacter, String label) {
        return new Key(code, character, shiftCharacter, label, 1f, false);
    }

    private static Key key(int code, char character, String label, float widthUnits) {
        return new Key(code, character, '\0', label, widthUnits, false);
    }

    private static Key toggle(int code, String label, float widthUnits) {
        return new Key(code, '\0', '\0', label, widthUnits, true);
    }

    private static final class Key {
        final int code;
        final char character;
        final char shiftCharacter;
        final String label;
        final float widthUnits;
        final boolean toggle;
        Node node;
        Geometry border;
        Geometry fill;
        BitmapText text;
        int rowIndex;
        int columnIndex;
        float x;
        float y;
        float width;
        float height;
        float keyboardWidth;

        Key(int code, char character, char shiftCharacter, String label, float widthUnits, boolean toggle) {
            this.code = code;
            this.character = character;
            this.shiftCharacter = shiftCharacter;
            this.label = label;
            this.widthUnits = widthUnits;
            this.toggle = toggle;
        }

        boolean contains(float px, float py) {
            return px >= x && px <= x + width && py >= y && py <= y + height;
        }

        boolean isSelected(int selectedRow, int selectedColumn) {
            return rowIndex == selectedRow && columnIndex == selectedColumn;
        }

        boolean isTextKey() {
            return character != '\0' && character != '\n' && character != ' ';
        }
    }
}
