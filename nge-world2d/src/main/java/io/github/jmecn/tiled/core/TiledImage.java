package io.github.jmecn.tiled.core;

import com.jme3.texture.Texture2D;

/**
 * When read a &lt;image&gt; element there 5 attribute there. This class is just a data struct to return the whole image node.
 *
 * @author yanmaoyuan
 */
public class TiledImage {
    private final String source;
    private final String trans;
    private final String format;
    private final int width;
    private final int height;

    private Texture2D texture;

    public TiledImage(String source, String trans, String format, int width, int height) {
        this.source = source;
        this.trans = trans;
        this.format = format;
        this.width = width;
        this.height = height;
    }

    public String getSource() {
        return source;
    }

    public String getTrans() {
        return trans;
    }

    public String getFormat() {
        return format;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public Texture2D getTexture() {
        return texture;
    }

    public void setTexture(Texture2D texture) {
        this.texture = texture;
    }

    @Override
    public String toString() {
        return "TiledImage{" +
                "source='" + source + '\'' +
                ", width=" + width +
                ", height=" + height +
                ", trans='" + trans + '\'' +
                ", format='" + format + '\'' +
                '}';
    }
}