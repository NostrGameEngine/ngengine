package io.github.jmecn.tiled.core;

/**
 * This element is used to describe which transformations can be applied to the tiles
 * (e.g. to extend a Wang set by transforming existing tiles). (since 1.5)
 *
 * @author yanmaoyuan
 */
public class Transformations {
    /**
     * Whether the tiles in this set can be flipped horizontally (default 0)
     */
    private int verticallyFlip;
    /**
     * Whether the tiles in this set can be flipped vertically (default 0)
     */
    private int horizontallyFlip;
    /**
     * Whether the tiles in this set can be rotated in 90 degree increments (default 0)
     */
    private int rotate;
    /**
     * Whether untransformed tiles remain preferred, otherwise transformed tiles are
     * used to produce more variations (default 0)
     */
    private int preferUntransformed;

    public Transformations() {
        this.horizontallyFlip = 0;
        this.verticallyFlip = 0;
        this.rotate = 0;
        this.preferUntransformed = 0;
    }

    public Transformations(int verticallyFlip, int horizontallyFlip, int rotate, int preferUntransformed) {
        this.verticallyFlip = verticallyFlip;
        this.horizontallyFlip = horizontallyFlip;
        this.rotate = rotate;
        this.preferUntransformed = preferUntransformed;
    }

    public int getVerticallyFlip() {
        return verticallyFlip;
    }

    public void setVerticallyFlip(int verticallyFlip) {
        this.verticallyFlip = verticallyFlip;
    }

    public int getHorizontallyFlip() {
        return horizontallyFlip;
    }

    public void setHorizontallyFlip(int horizontallyFlip) {
        this.horizontallyFlip = horizontallyFlip;
    }

    public int getRotate() {
        return rotate;
    }

    public void setRotate(int rotate) {
        this.rotate = rotate;
    }

    public int getPreferUntransformed() {
        return preferUntransformed;
    }

    public void setPreferUntransformed(int preferUntransformed) {
        this.preferUntransformed = preferUntransformed;
    }
}