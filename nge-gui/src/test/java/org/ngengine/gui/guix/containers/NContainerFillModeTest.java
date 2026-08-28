/*
 * Copyright (c) 2026, Nostr Game Engine
 * All rights reserved.
 */
package org.ngengine.gui.guix.containers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import org.junit.jupiter.api.Test;
import org.ngengine.gui.FillMode;
import org.ngengine.gui.component.SpringGridLayout;

public class NContainerFillModeTest {

    @Test
    public void columnMapsFillModesWithoutReplacingItsVerticalLayout() {
        NColumn column = new NColumn();
        SpringGridLayout layout = (SpringGridLayout) column.getLayout();

        column.setFillMode(FillMode.Proportional, FillMode.ForcedEven);

        assertSame(layout, column.getLayout());
        assertEquals(FillMode.ForcedEven, layout.getMainFillMode());
        assertEquals(FillMode.Proportional, layout.getMinorFillMode());
    }

    @Test
    public void rowMapsFillModesWithoutReplacingItsHorizontalLayout() {
        NRow row = new NRow();
        SpringGridLayout layout = (SpringGridLayout) row.getLayout();

        row.setFillMode(FillMode.Proportional, FillMode.ForcedEven);

        assertSame(layout, row.getLayout());
        assertEquals(FillMode.Proportional, layout.getMainFillMode());
        assertEquals(FillMode.ForcedEven, layout.getMinorFillMode());
    }
}
