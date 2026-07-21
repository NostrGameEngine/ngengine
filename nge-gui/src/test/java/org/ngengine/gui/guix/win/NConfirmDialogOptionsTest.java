package org.ngengine.gui.guix.win;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class NConfirmDialogOptionsTest {

    @Test
    void dialogHasADefaultTitleAndAcceptsASpecificOne() {
        NConfirmDialogOptions options = new NConfirmDialogOptions();
        assertEquals("Confirm", options.getTitle());

        assertEquals("Restart required", options.setTitle("Restart required").getTitle());
    }
}
