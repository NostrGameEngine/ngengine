/*
 * Copyright (c) 2026, Nostr Game Engine
 * All rights reserved.
 */
package org.ngengine.gui;

import static org.junit.jupiter.api.Assertions.assertSame;

import org.junit.jupiter.api.Test;

public class PanelTest {

    @Test
    public void predicateQueryReturnsDirectMatch() {
        Container root = new Container();
        Panel child = root.addChild(new Panel());

        assertSame(child, root.querySelector(panel -> panel == child));
    }
}
