package io.github.jmecn.tiled.renderer;

import java.util.Objects;

import com.jme3.math.ColorRGBA;

import io.github.jmecn.tiled.core.TiledImage;
import jakarta.annotation.Nullable;

class LayerMaterialCacheKey implements Cloneable{
    @Nullable TiledImage img;
    @Nullable ColorRGBA color;
    @Nullable ColorRGBA tint;
    
    public LayerMaterialCacheKey(TiledImage img, ColorRGBA color, ColorRGBA tint) {
        this.img = img;
        this.color = color;
        this.tint = tint;
    }

    @Override
    public int hashCode() {
        return Objects.hash(img, color, tint);
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        LayerMaterialCacheKey other = (LayerMaterialCacheKey) obj;
        return Objects.equals(img, other.img) && Objects.equals(color, other.color)
                && Objects.equals(tint, other.tint);        
    }

    @Override
    public LayerMaterialCacheKey clone(){
        return new LayerMaterialCacheKey(img, color, tint);
    }
}
