package org.ngengine.gui.nav;

/** A gesture measured entirely in the GUI viewport's logical coordinates. */
final class TouchListDrag {
    private final double startX;
    private final double startY;
    private final double slop;
    private final double rowStep;
    private double scrollY;
    private boolean dragged;

    TouchListDrag(double x, double y, double slop, double rowHeight) {
        startX = x;
        startY = y;
        scrollY = y;
        this.slop = slop;
        rowStep = Math.max(slop, rowHeight * 0.5);
    }

    int move(double x, double y) {
        if (!dragged && Math.hypot(x - startX, y - startY) <= slop) {
            return 0;
        }
        dragged = true;
        if (rowStep <= 0) {
            return 0;
        }
        int rows = (int) ((y - scrollY) / rowStep);
        scrollY += rows * rowStep;
        return rows;
    }

    boolean isDragged() {
        return dragged;
    }
}
