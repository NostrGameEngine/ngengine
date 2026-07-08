/*
 * Copyright (c) 2026, Nostr Game Engine
 * All rights reserved.
 */
package org.ngengine.gui.component;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import org.junit.jupiter.api.Test;
import org.ngengine.gui.Axis;
import org.ngengine.gui.Container;
import org.ngengine.gui.FillMode;

public class SpringGridLayoutTest {

    @Test
    public void removeChildCompactsRowIndexes() {
        SpringGridLayout layout = new SpringGridLayout(Axis.Y, Axis.X, FillMode.ForcedEven, FillMode.Even);
        Container first = new Container();
        Container second = new Container();
        Container third = new Container();

        layout.addChild(first);
        layout.addChild(second);
        layout.addChild(third);

        layout.removeChild(second);

        assertSame(first, layout.getChild(0, 0));
        assertSame(third, layout.getChild(1, 0));
        assertNull(layout.getChild(2, 0));
    }

    @Test
    public void replacingOccupiedCellDoesNotShiftLaterRowsIntoReplacementCell() {
        SpringGridLayout layout = new SpringGridLayout(Axis.Y, Axis.X, FillMode.ForcedEven, FillMode.Even);
        Container first = new Container();
        Container second = new Container();
        Container third = new Container();
        Container replacement = new Container();

        layout.addChild(first);
        layout.addChild(second);
        layout.addChild(third);

        layout.addChild(1, 0, replacement);

        assertSame(first, layout.getChild(0, 0));
        assertSame(replacement, layout.getChild(1, 0));
        assertSame(third, layout.getChild(2, 0));
    }
}
