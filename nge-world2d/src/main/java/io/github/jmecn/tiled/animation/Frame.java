package io.github.jmecn.tiled.animation;

/**
 * a single frame of an animated tile
 * 
 * @author yanmaoyuan
 * 
 */
public class Frame {
    /**
     * The local ID of a tile within the parent tileset.
     */
    private int tileId;
    /**
     * How long (in milliseconds) this frame should be displayed before
     * advancing to the next frame.
     */
    private int duration;

    public Frame(int tileId, int duration) {
        this.tileId = tileId;
        this.duration = duration;
    }

    public int getTileId() {
        return tileId;
    }

    public void setTileId(int tileId) {
        this.tileId = tileId;
    }

    public int getDuration() {
        return duration;
    }

    public void setDuration(int duration) {
        this.duration = duration;
    }
}