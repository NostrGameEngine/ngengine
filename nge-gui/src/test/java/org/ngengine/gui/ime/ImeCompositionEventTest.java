/*
 * Copyright (c) 2026, Nostr Game Engine
 * All rights reserved.
 */
package org.ngengine.gui.ime;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ImeCompositionEventTest {

    @Test
    public void insertClampsStaleSelection() {
        ImeCompositionEvent event = new ImeCompositionEvent("sample text");

        event.setSelection(57, 19);
        event.insertAtCursor("!");

        assertEquals("sample text!", event.getText());
        assertEquals(12, event.getCursorStart());
        assertEquals(12, event.getCursorEnd());
    }

    @Test
    public void insertAcceptsReversedSelection() {
        ImeCompositionEvent event = new ImeCompositionEvent("abcdef");

        event.setSelection(5, 2);
        event.insertAtCursor("X");

        assertEquals("abXf", event.getText());
        assertEquals(3, event.getCursorStart());
        assertEquals(3, event.getCursorEnd());
    }

    @Test
    public void nullTextAndInsertAreSafe() {
        ImeCompositionEvent event = new ImeCompositionEvent(null);

        event.insertAtCursor(null);

        assertEquals("", event.getText());
        assertEquals(0, event.getCursorStart());
        assertEquals(0, event.getCursorEnd());
    }
}
