/*
 * Copyright (c) 2026, Nostr Game Engine
 * All rights reserved.
 */
package org.ngengine.gui.component;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import com.jme3.math.Vector3f;
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

    @Test
    public void proportionalLayoutEvenlyDistributesZeroPreferredSizes() {
        assertZeroPreferredSizesAreDistributed(800f, 600f);
        assertZeroPreferredSizesAreDistributed(1.6f, 1f);
    }

    private void assertZeroPreferredSizesAreDistributed(float width, float height) {
        SpringGridLayout layout = new SpringGridLayout(
                Axis.Y, Axis.X, FillMode.Proportional, FillMode.Proportional);
        Container first = new Container();
        Container second = new Container();
        layout.addChild(0, 0, first);
        layout.addChild(0, 1, second);

        layout.reshape(new Vector3f(), new Vector3f(width, height, 0f));

        assertEquals(width / 2f, first.getSize().x, 0.0001f);
        assertEquals(width / 2f, second.getSize().x, 0.0001f);
        assertEquals(height, first.getSize().y, 0.0001f);
        assertEquals(height, second.getSize().y, 0.0001f);
    }
}
