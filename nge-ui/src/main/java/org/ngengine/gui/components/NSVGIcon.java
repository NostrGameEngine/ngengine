package org.ngengine.gui.components;

 
import org.ngengine.gui.svg.SVGTextureKey;

import com.jme3.math.Vector2f;
import com.simsilica.lemur.GuiGlobals;
import com.simsilica.lemur.component.IconComponent;

public class NSVGIcon extends IconComponent{

    public NSVGIcon(String imagePath, int width, int height) {
        this(imagePath, width, height, new Vector2f(1, 1), 0, 0, 0, false);
        if (width < 2) width = 2;
        if (height < 2) height = 2;
        setIconSize(new Vector2f(width, height));
    }
    
    public NSVGIcon( String imagePath, int width, int height, Vector2f iconScale,
                          float xMargin, float yMargin, float zOffset,
                          boolean lit ) {
        super(GuiGlobals.getInstance().loadTexture(
                new SVGTextureKey(imagePath, width < 2 ? 2 : width, height < 2 ? 2 : height), false),
                iconScale, xMargin, yMargin, zOffset, lit);
        if (width < 2) width = 2;
        if (height < 2) height = 2;
        setIconSize(new Vector2f(width, height));

    }
}
