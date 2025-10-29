package io.github.jmecn.tiled.core;

/**
 * Defines a Wang tile, by referring to a tile in the tileset and associating it with a certain Wang ID.
 *
 * @author yanmaoyuan
 */
public class WangTile {
    private int tileId;
    /**
     * The Wang ID, since Tiled 1.5 given by a comma-separated list of indexes (0-254) referring to
     * the Wang colors in the Wang set in the order: top, top-right, right, bottom-right, bottom,
     * bottom-left, left, top-left. Index 0 means unset and index 1 refers to the first Wang color.
     * Before Tiled 1.5, the Wang ID was saved as a 32-bit unsigned integer stored in the format
     * 0xCECECECE (where each C is a corner color and each E is an edge color, in reverse order).
     */
    private String wangId;

    public WangTile() {
    }
    public WangTile(int tileId, String wangId) {
        this.tileId = tileId;
        this.wangId = wangId;
    }

    public int getTileId() {
        return tileId;
    }

    public void setTileId(int tileId) {
        this.tileId = tileId;
    }

    public String getWangId() {
        return wangId;
    }

    public void setWangId(String wangId) {
        this.wangId = wangId;
    }
}