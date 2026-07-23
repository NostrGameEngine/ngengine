/*
 * Copyright (c) 2026, Nostr Game Engine
 * All rights reserved.
 */
package org.ngengine.gui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;

public class TabbedPanelTest {

    @Test
    public void tabDividerIsPresentButDoesNotChangeDefaultLayoutHeight() {
        TabbedPanel tabs = new TabbedPanel();

        assertNotNull(tabs.getTabDivider());
        assertEquals(0f, tabs.getTabDivider().getPreferredSize().y, 0.0001f);
    }
}
