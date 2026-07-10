/*
 * Copyright (c) 2026, Nostr Game Engine
 * All rights reserved.
 */
package org.ngengine.gui.nav;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.jme3.bounding.BoundingVolume;
import com.jme3.renderer.Camera;
import com.jme3.scene.Node;
import org.junit.jupiter.api.Test;

public class PopupHandlerTest {

    @Test
    public void missingPopupBoundsHaveNeutralDepth() {
        TestPopupHandler handler = new TestPopupHandler();

        assertEquals(0f, handler.minZ(null));
    }

    private static class TestPopupHandler extends PopupHandler {
        TestPopupHandler() {
            super(new Node("GuiNode"), new Camera(800, 600));
        }

        float minZ(BoundingVolume bounds) {
            return getMinZ(bounds);
        }
    }
}
