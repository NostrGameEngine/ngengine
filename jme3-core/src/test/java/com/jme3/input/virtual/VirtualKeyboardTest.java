/*
 * Copyright (c) 2026, Nostr Game Engine
 * All rights reserved.
 */
package com.jme3.input.virtual;

import com.jme3.input.RawInputListenerAdapter;
import com.jme3.input.KeyInput;
import com.jme3.input.event.KeyInputEvent;
import com.jme3.scene.Node;
import com.jme3.system.TestUtil;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class VirtualKeyboardTest {

    @AfterEach
    public void cleanup() {
        VirtualKeyboard.getInstance().setVisible(false);
    }

    @Test
    public void controllerNavigationDispatchesKeyInputEvents() {
        VirtualKeyboard keyboard = VirtualKeyboard.getInstance();
        List<KeyInputEvent> events = new ArrayList<>();

        keyboard.setVisible(true);
        resetToTopLeft(keyboard);
        keyboard.navigate(0, -1, 0L);
        keyboard.action(true, 1L);
        keyboard.action(false, 2L);
        keyboard.dispatchEvents(new RawInputListenerAdapter() {
            @Override
            public void onKeyEvent(KeyInputEvent evt) {
                events.add(evt);
            }
        });

        assertEquals(2, events.size());
        assertEquals('q', events.get(0).getKeyChar());
        assertEquals(true, events.get(0).isPressed());
        assertEquals('q', events.get(1).getKeyChar());
        assertEquals(true, events.get(1).isReleased());
    }

    @Test
    public void enterDispatchesKeyEventWithoutClosingKeyboard() {
        VirtualKeyboard keyboard = VirtualKeyboard.getInstance();
        List<KeyInputEvent> events = new ArrayList<>();

        keyboard.setVisible(true);
        resetToTopLeft(keyboard);
        keyboard.navigate(0, 99, 0L);
        keyboard.navigate(0, -2, 0L);
        keyboard.navigate(99, 0, 0L);
        keyboard.action(true, 1L);
        keyboard.action(false, 2L);
        keyboard.dispatchEvents(new RawInputListenerAdapter() {
            @Override
            public void onKeyEvent(KeyInputEvent evt) {
                events.add(evt);
            }
        });

        assertEquals(true, keyboard.isVisible());
        assertEquals(2, events.size());
        assertEquals(KeyInput.KEY_RETURN, events.get(0).getKeyCode());
        assertEquals(true, events.get(0).isPressed());
        assertEquals(KeyInput.KEY_RETURN, events.get(1).getKeyCode());
        assertEquals(true, events.get(1).isReleased());
    }

    @Test
    public void dispatchesNavigationKeys() {
        VirtualKeyboard keyboard = VirtualKeyboard.getInstance();
        List<KeyInputEvent> events = new ArrayList<>();

        keyboard.setVisible(true);
        resetToTopLeft(keyboard);
        keyboard.key(KeyInput.KEY_RIGHT, 1L);
        keyboard.dispatchEvents(new RawInputListenerAdapter() {
            @Override
            public void onKeyEvent(KeyInputEvent evt) {
                events.add(evt);
            }
        });

        assertEquals(2, events.size());
        assertEquals(KeyInput.KEY_RIGHT, events.get(0).getKeyCode());
        assertEquals(true, events.get(0).isPressed());
        assertEquals(KeyInput.KEY_RIGHT, events.get(1).getKeyCode());
        assertEquals(true, events.get(1).isReleased());
    }

    @Test
    public void verticalNavigationUsesNearestKeyCenterAcrossStaggeredRows() {
        VirtualKeyboard keyboard = VirtualKeyboard.getInstance();
        List<KeyInputEvent> events = new ArrayList<>();

        keyboard.setVisible(true);
        keyboard.updateVisuals(new Node("root"), TestUtil.createAssetManager(), 1280, 720, 0f);
        resetToTopLeft(keyboard);
        keyboard.navigate(0, -1, 0L);
        keyboard.navigate(0, -1, 0L);
        keyboard.navigate(0, -1, 0L);
        keyboard.navigate(1, 0, 0L);
        keyboard.navigate(1, 0, 0L);
        keyboard.navigate(1, 0, 0L);
        keyboard.navigate(1, 0, 0L);
        keyboard.navigate(1, 0, 0L);
        keyboard.navigate(1, 0, 0L);
        keyboard.navigate(0, 1, 0L);
        keyboard.action(true, 1L);
        keyboard.action(false, 2L);
        keyboard.dispatchEvents(new RawInputListenerAdapter() {
            @Override
            public void onKeyEvent(KeyInputEvent evt) {
                events.add(evt);
            }
        });

        assertEquals(2, events.size());
        assertEquals('k', events.get(0).getKeyChar());
        assertEquals(true, events.get(0).isPressed());
        assertEquals('k', events.get(1).getKeyChar());
        assertEquals(true, events.get(1).isReleased());
    }

    @Test
    public void diagonalNavigationMovesOnlyOneAxis() {
        VirtualKeyboard keyboard = VirtualKeyboard.getInstance();
        List<KeyInputEvent> events = new ArrayList<>();

        keyboard.setVisible(true);
        resetToTopLeft(keyboard);
        keyboard.navigate(1, -1, 0L);
        keyboard.action(true, 1L);
        keyboard.action(false, 2L);
        keyboard.dispatchEvents(new RawInputListenerAdapter() {
            @Override
            public void onKeyEvent(KeyInputEvent evt) {
                events.add(evt);
            }
        });

        assertEquals(2, events.size());
        assertEquals('2', events.get(0).getKeyChar());
        assertEquals(true, events.get(0).isPressed());
        assertEquals('2', events.get(1).getKeyChar());
        assertEquals(true, events.get(1).isReleased());
    }

    @Test
    public void shiftSelectsSymbolCharacters() {
        VirtualKeyboard keyboard = VirtualKeyboard.getInstance();
        List<KeyInputEvent> events = new ArrayList<>();

        keyboard.setVisible(true);
        resetToTopLeft(keyboard);
        keyboard.navigate(0, -3, 0L);
        keyboard.action(true, 1L);
        keyboard.action(false, 2L);
        resetToTopLeft(keyboard);
        keyboard.action(true, 3L);
        keyboard.action(false, 4L);
        keyboard.dispatchEvents(new RawInputListenerAdapter() {
            @Override
            public void onKeyEvent(KeyInputEvent evt) {
                events.add(evt);
            }
        });

        assertEquals(4, events.size());
        assertEquals(KeyInput.KEY_LSHIFT, events.get(0).getKeyCode());
        assertEquals(true, events.get(0).isPressed());
        assertEquals(KeyInput.KEY_LSHIFT, events.get(1).getKeyCode());
        assertEquals(true, events.get(1).isReleased());
        assertEquals(KeyInput.KEY_UNKNOWN, events.get(2).getKeyCode());
        assertEquals('!', events.get(2).getKeyChar());
        assertEquals(true, events.get(2).isPressed());
        assertEquals(KeyInput.KEY_UNKNOWN, events.get(3).getKeyCode());
        assertEquals('!', events.get(3).getKeyChar());
        assertEquals(true, events.get(3).isReleased());
    }

    private void resetToTopLeft(VirtualKeyboard keyboard) {
        keyboard.navigate(0, 99, 0L);
        keyboard.navigate(-99, 0, 0L);
    }
}
