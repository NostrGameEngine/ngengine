/*
 * Copyright (c) 2026, Nostr Game Engine
 * All rights reserved.
 */
package org.ngengine.gui.guix;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.ngengine.gui.NGEStyle;

public class NChipTest {

    @Test
    public void defaultInsetsKeepContentClearOfFramedPanelEdges() {
        assertEquals(NGEStyle.px(14f), NChip.defaultPaddingX());
        assertEquals(NGEStyle.px(7f), NChip.defaultPaddingY());
    }
}
