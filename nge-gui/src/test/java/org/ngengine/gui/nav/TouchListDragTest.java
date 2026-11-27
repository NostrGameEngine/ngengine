package org.ngengine.gui.nav;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

public class TouchListDragTest {
    @Test
    public void swipesScrollRowsInRelativeGuiCoordinates() {
        TouchListDrag drag = new TouchListDrag(.5, .35, .02, .2);
        assertEquals(0, drag.move(.51, .36));
        assertFalse(drag.isDragged());
        assertEquals(1, drag.move(.51, .47));
        assertTrue(drag.isDragged());
        assertEquals(1, drag.move(.51, .58));
        assertEquals(-2, drag.move(.51, .33));
    }

    @Test
    public void aSwipeThatDoesNotScrollStillDoesNotCountAsATap() {
        TouchListDrag drag = new TouchListDrag(.5, .35, .02, .2);
        assertEquals(0, drag.move(.55, .35));
        assertTrue(drag.isDragged());
        assertEquals(0, drag.move(.5, .35));
        assertTrue(drag.isDragged());
    }
}
