package io.github.jmecn.tiled.core;

import java.util.ArrayList;
import java.util.List;

/**
 * Defines a list of colors and any number of Wang tiles using these colors. (since 1.1)
 *
 * @author yanmaoyuan
 */
public class WangSet extends Base {

    /**
     * the tileset this wangset belongs to.
     */
    private Tileset tileset;

    /**
     * ID of this wangset.
     */
    private int id;

    /**
     * The name of the Wang set.
     */
    private String name;
    /**
     * The class of the Wang set (since 1.9, defaults to “”).
     */
    private String clazz;
    /**
     * The tile ID of the tile representing this Wang set.
     */
    private int tile;

    /**
     * Can contain up to 254.
     * (255 since Tiled 1.5, 254 since Tiled 1.10.2)
     */
    private List<WangColor> wangColors;

    /**
     * Can contain any number of Wang tiles.
     */
    private List<WangTile> wangTiles;

    public WangSet(String name) {
        this.name = name;
        wangColors = new ArrayList<>(255);
        wangTiles = new ArrayList<>();
    }

    public Tileset getTileset() {
        return tileset;
    }

    public void setTileset(Tileset tileset) {
        this.tileset = tileset;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getClazz() {
        return clazz;
    }

    public void setClazz(String clazz) {
        this.clazz = clazz;
    }

    public int getTile() {
        return tile;
    }

    public void setTile(int tile) {
        this.tile = tile;
    }

    public List<WangColor> getWangColors() {
        return wangColors;
    }

    public void addWangColor(WangColor wangColor) {
        wangColors.add(wangColor);
    }

    public List<WangTile> getWangTiles() {
        return wangTiles;
    }

    public void addWangTile(WangTile wangTile) {
        wangTiles.add(wangTile);
    }
}