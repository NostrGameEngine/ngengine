package org.ngengine.demo.son.ocean;

import java.io.IOException;

import com.jme3.export.InputCapsule;
import com.jme3.export.JmeExporter;
import com.jme3.export.JmeImporter;
import com.jme3.export.OutputCapsule;
import com.jme3.export.Savable;
import com.jme3.math.ColorRGBA;
import com.jme3.texture.Image;
import com.jme3.texture.image.ImageRaster;

public class IBOceanLayer implements Savable {
    private Image image;
    private transient ImageRaster imageRaster;

    protected IBOceanLayer() {
    }

    protected IBOceanLayer(Image image) {
        this.image = image; 
    }

    public Image getImage() {
        return image;
    }

    
    public float sample(float xf, float yf) {
        if(imageRaster == null) {
            imageRaster = ImageRaster.create(image);
        }
        int x = (int) (xf * image.getWidth());
        int y = (int) (yf * image.getHeight());
        if (x < 0 || x >= image.getWidth() || y < 0 || y >= image.getHeight()) {
            return 0.0f; 
        }
        ColorRGBA c = imageRaster.getPixel(x, y);
        return c.a;
    }
   

    @Override
    public void write(JmeExporter ex) throws IOException {
        OutputCapsule oc = ex.getCapsule(this);
        oc.write(image, "image", null);
    }

    @Override
    public void read(JmeImporter im) throws IOException {
        InputCapsule ic = im.getCapsule(this);
        image = (Image) ic.readSavable("image", null);
    }
}