package org.ngengine.demo.soo;

import java.util.ArrayList;
import java.util.function.BiFunction;

import com.jme3.asset.AssetManager;
import com.jme3.asset.TextureKey;
import com.jme3.material.Material;
import com.jme3.math.Vector2f;
import com.jme3.texture.Image;
import com.jme3.texture.Texture;
import com.jme3.texture.TextureArray;

public class FlipBookTexture extends TextureArray{
    private final Vector2f flipData= new Vector2f(0, 0);

    public static FlipBookTexture load(
        AssetManager assetManager, 
        String basePath,

        int frameCount,
        boolean flipY,
        BiFunction<String, Integer, String> framePathSupplier
    ){
        ArrayList<Image> images = new ArrayList<>(frameCount);
        int frameWidth = 0;
        int frameHeight = 0;
        for(int i = 0; i < frameCount; i++){
            String path = framePathSupplier.apply(basePath, i);
            TextureKey key = new TextureKey(path, flipY);
            Texture tx = assetManager.loadTexture(key);
            Image image = tx.getImage();
            if(frameWidth == 0 && frameHeight == 0){
                frameWidth = image.getWidth();
                frameHeight = image.getHeight();
            }else if(frameWidth != image.getWidth() || frameHeight != image.getHeight()){
                throw new IllegalArgumentException("All images must have the same size");
            }
            images.add(image);
        }

        FlipBookTexture texture = new FlipBookTexture(images);
        texture.flipData.setY(frameCount);
        return texture;
    }

   
    
    public static FlipBookTexture load(AssetManager assetManager, String basePath, String fileExt,  int frameCount, boolean flipY) {
        return load(assetManager, basePath, frameCount, flipY,
                (path, index) -> {
                    StringBuilder sb = new StringBuilder(path);
                    if(!path.endsWith("/")) {
                        sb.append("/");
                    }
                    sb.append(String.format("%04d", index+1));
                    sb.append(fileExt);
                    return sb.toString();
                });

    }


    public FlipBookTexture(ArrayList<Image> images) {
        super(images);
        setWrap(WrapMode.Repeat);
        setMagFilter(MagFilter.Bilinear);
        setMinFilter(MinFilter.BilinearNoMipMaps);
    }

    public Vector2f getFlipData() {
        return flipData;
    }

    public void setFrame(int frame){
        flipData.setX(frame);
    }

    public int getFrameCount(){
        return (int) flipData.y;
    }

    public void apply(Material mat, String paramName) {
        mat.setTexture(paramName, this);
        mat.setVector2(paramName + "Data", flipData);
    }
}
