package org.ngengine;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;

import javax.imageio.ImageIO;

import com.jme3.asset.AssetInfo;
import com.jme3.asset.AssetLoader;
import com.jme3.asset.TextureKey;
import com.jme3.texture.Image;
import com.jme3.texture.plugins.AWTLoader;

public class WebpImageLoader  implements AssetLoader {
    private AWTLoader loader = new AWTLoader();
    
    @Override
    public Object load(AssetInfo assetInfo) throws IOException {
        try(InputStream is = assetInfo.openStream()){
            BufferedImage img = ImageIO.read(is);
            boolean flipY = false;
            if(assetInfo.getKey() instanceof TextureKey) {
                flipY = ((TextureKey) assetInfo.getKey()).isFlipY();
            }
            Image timg = loader.load(img, flipY);            
            return timg;
        }
    }    
}
