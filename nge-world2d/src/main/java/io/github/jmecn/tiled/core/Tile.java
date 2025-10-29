package io.github.jmecn.tiled.core;

import java.util.ArrayList;
import java.util.List;

import io.github.jmecn.tiled.animation.Animation;
import io.github.jmecn.tiled.animation.Frame;

public class Tile extends Base {

    /**
     * When you use the tile flipping feature added in Tiled Qt 0.7, the highest
     * two bits of the gid store the flipped state. Bit 32 is used for storing
     * whether the tile is horizontally flipped and bit 31 is used for the
     * vertically flipped tiles. And since Tiled Qt 0.8, bit 30 means whether
     * the tile is flipped (anti) diagonally, enabling tile rotation. These bits
     * have to be read and cleared before you can find out which tileset a tile
     * belongs to.
     * 
     * When rendering a tile, the order of operation matters. The diagonal flip
     * (x/y axis swap) is done first, followed by the horizontal and vertical
     * flips.
     */
    // Bits on the far end of the 32-bit global tile ID are used for tile flags
    public static final int FLIPPED_HORIZONTALLY_FLAG = 0x80000000;
    public static final int FLIPPED_VERTICALLY_FLAG = 0x40000000;
    public static final int FLIPPED_DIAGONALLY_FLAG = 0x20000000;
    public static final int ROTATED_HEXAGONAL_120_FLAG = 0x10000000;
    public static final int FLIPPED_MASK = FLIPPED_HORIZONTALLY_FLAG | FLIPPED_VERTICALLY_FLAG
            | FLIPPED_DIAGONALLY_FLAG | ROTATED_HEXAGONAL_120_FLAG;

    private Tileset tileset;
    private int id = -1;
    private int gid = 0;
    private String clazz;

    /**
     * position in the image
     */
    private int x;
    private int y;
    private int width;
    private int height;

    private TiledImage image;

    // animation
    private final List<Animation> animations = new ArrayList<>();

    // Terrain
    /**
     * Defines the terrain type of each corner of the tile, given as
     * comma-separated indexes in the terrain types array in the order top-left,
     * top-right, bottom-left, bottom-right. Leaving out a value means that
     * corner has no terrain. (optional) (since 0.9)
     */
    private int terrain = -1;// unsigned int
    /**
     * A percentage indicating the probability that this tile is chosen when it
     * competes with others while editing with the terrain tool. (optional)
     * (since 0.9)
     */
    private float probability = -1;

    private ObjectGroup collisions;

    /**
     * Default constructor
     */
    public Tile() {
        x = y = 0;
        width = height = -1;
    }

    public Tile(int x, int y, int width, int height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    /*
     * getters && setters
     */

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

    public int getGid() {
        return gid;
    }

    public void setGid(int gid) {
        this.gid = gid;
    }

    public int getGidNoMask() {
        return gid & ~FLIPPED_MASK;
    }

    public void setClazz(String clazz) {
        this.clazz = clazz;
    }

    public String getClazz() {
        return clazz;
    }

    public boolean isFlippedHorizontally() {
        return (gid & FLIPPED_HORIZONTALLY_FLAG) != 0;
    }

    public boolean isFlippedVertically() {
        return (gid & FLIPPED_VERTICALLY_FLAG) != 0;
    }

    public boolean isFlippedAntiDiagonally() {
        return (gid & FLIPPED_DIAGONALLY_FLAG) != 0;
    }

    public boolean isRotatedHexagonal120() {
        return (gid & ROTATED_HEXAGONAL_120_FLAG) != 0;
    }

    public int getX() {
        return x;
    }

    public void setX(int x) {
        this.x = x;
    }

    public int getY() {
        return y;
    }

    public void setY(int y) {
        this.y = y;
    }

    public int getWidth() {
        return width;
    }

    public void setWidth(int width) {
        this.width = width;
    }

    public int getHeight() {
        return height;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    public TiledImage getImage() {
        return image;
    }

    public void setImage(TiledImage image) {
        this.image = image;
    }

    /*
     * This part is about the animation.
     * 
     * As of Tiled 0.10, each tile can have exactly one animation associated
     * with it. In the future, there could be support for multiple named
     * animations on a tile
     */

    /**
     * Add an animation to the tile.
     * 
     * @param animation The animation of this tile.
     */
    public void addAnimation(Animation animation) {
        animation.setId(animations.size());
        animations.add(animation);
    }

    /**
     * Add an animation to the tile.
     * 
     * @param name
     *            animation's name
     * @param frames
     *            the frames
     */
    public void addAnimation(String name, List<Frame> frames) {
        Animation animation = new Animation(name, frames);
        animation.setId(animations.size());
        animations.add(animation);
    }

    /**
     * Add an animation to the tile.
     * 
     * @param frames
     *            the frames
     */
    public void addAnimation(List<Frame> frames) {
        Animation animation = new Animation(null, frames);
        animation.setId(animations.size());
        animations.add(animation);
    }

    public Animation getAnimation(String name) {
        int len = animations.size();
        if (len == 0 || name == null) {
            return null;
        }

        for (Animation anim : animations) {
            if (anim.equalsIgnoreCase(name)) {
                return anim;
            }
        }

        return null;
    }

    public List<Animation> getAnimations() {
        return animations;
    }

    public boolean isAnimated() {
        return !animations.isEmpty();
    }

    /*
     * This part is about the terrain. It's useless in jme3.
     */

    public int getTerrain() {
        return terrain;
    }

    public void setTerrain(int terrain) {
        this.terrain = terrain;
    }

    public float getProbability() {
        return probability;
    }

    public void setProbability(float probability) {
        this.probability = probability;
    }

    public ObjectGroup getCollisions() {
        return collisions;
    }

    public void setCollisions(ObjectGroup collisions) {
        this.collisions = collisions;
    }

    /**
     * Tile was cloned when TileLayer and ObjectGroup need a tile as a part of
     * them.
     * @return a new Tile object.
     */
    public Tile copy() {
        // tile base
        Tile tile = new Tile(x, y, width, height);
        tile.id = id;
        tile.gid = gid;
        tile.tileset = tileset;// share the tileset
        tile.image = image;

        // animation
        tile.animations.addAll(animations);// share the animation

        // terrain
        tile.terrain = terrain;
        tile.probability = probability;
        tile.properties = properties;
        tile.collisions = collisions;

        return tile;
    }

    @Override
    public String toString() {
        return "Tile{" +
                "id=" + id +
                ", gid=" + gid +
                ", x=" + x +
                ", y=" + y +
                ", width=" + width +
                ", height=" + height +
                ", image=" + image +
                ", terrain=" + terrain +
                ", probability=" + probability +
                ", gidNoMask=" + getGidNoMask() +
                ", H=" + isFlippedHorizontally() +
                ", V=" + isFlippedVertically() +
                ", AD=" + isFlippedAntiDiagonally() +
                ", Rot120=" + isRotatedHexagonal120() +
                ", animated=" + isAnimated() +
                '}';
    }
}
