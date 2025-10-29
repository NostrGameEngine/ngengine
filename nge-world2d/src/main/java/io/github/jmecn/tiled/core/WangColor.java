package io.github.jmecn.tiled.core;

import com.jme3.math.ColorRGBA;

/**
 * A color that can be used to define the corner and/or edge of a Wang tile.
 *
 * @author yanmaoyuan
 */
public class WangColor extends Base {
    /**
     * The name of this color.
     */
    private String name;
    /**
     * The class of this color (since 1.9, defaults to “”).
     */
    private String clazz;
    /**
     * The color in #RRGGBB format (example: #c17d11).
     */
    private ColorRGBA color;
    /**
     * The tile ID of the tile representing this color.
     */
    private int tile;
    /**
     * The relative probability that this color is chosen over others in case of multiple options. (defaults to 0)
     */
    private float probability;

    public WangColor() {
        super();
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

    public ColorRGBA getColor() {
        return color;
    }

    public void setColor(ColorRGBA color) {
        this.color = color;
    }

    public int getTile() {
        return tile;
    }

    public void setTile(int tile) {
        this.tile = tile;
    }

    public float getProbability() {
        return probability;
    }

    public void setProbability(float probability) {
        this.probability = probability;
    }
}