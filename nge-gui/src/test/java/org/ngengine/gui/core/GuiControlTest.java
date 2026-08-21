/*
 * Copyright (c) 2026, Nostr Game Engine
 * All rights reserved.
 */
package org.ngengine.gui.core;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.jme3.math.Vector3f;
import org.junit.jupiter.api.Test;

public class GuiControlTest {

    @Test
    public void equalPreferredSizeDoesNotInvalidateAgain() {
        CountingGuiControl control = new CountingGuiControl();

        control.setPreferredSize(new Vector3f(120f, 40f, 0f));
        control.setPreferredSize(new Vector3f(120f, 40f, 0f));

        assertEquals(1, control.invalidationCount);
    }

    @Test
    public void preferredSizeIsCopiedBeforeChangeDetection() {
        CountingGuiControl control = new CountingGuiControl();
        Vector3f preferred = new Vector3f(120f, 40f, 0f);
        control.setPreferredSize(preferred);

        preferred.x = 160f;
        assertEquals(120f, control.getPreferredSize().x, 0.0001f);

        control.setPreferredSize(preferred);
        assertEquals(2, control.invalidationCount);
        assertEquals(160f, control.getPreferredSize().x, 0.0001f);
    }

    private static final class CountingGuiControl extends GuiControl {
        private int invalidationCount;

        private CountingGuiControl() {
            super(new String[0]);
        }

        @Override
        public void invalidate() {
            invalidationCount++;
            super.invalidate();
        }
    }
}
