package org.ngengine.gui.svg;

import com.jme3.asset.TextureKey;

public class SVGTextureKey extends TextureKey {
    private final int width;
    private final int height;
    
    public SVGTextureKey(String name, int width, int height) {
        super(name);
        this.width = width;
        this.height = height;
    }

    public SVGTextureKey(String name, boolean flipY, int width, int height) {
        super(name, flipY);
        this.width = width;
        this.height = height;
    }

    @Override
    public String getExtension() {
        return "svg";
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }   

    @Override
    public boolean equals(Object obj) {
        if(!super.equals(obj)) {
            return false;
        }
        SVGTextureKey other = (SVGTextureKey) obj;
        return width == other.width && height == other.height;
    }


    @Override
    public int hashCode() {
        int hash = super.hashCode();
        hash = 31 * hash + width;
        hash = 31 * hash + height;
        return hash;
    }
    
}
