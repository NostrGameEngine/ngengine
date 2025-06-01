package org.ngengine.gui.components;

import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.simsilica.lemur.component.QuadBackgroundComponent;

public class NAspectPreservingQuadBackground extends QuadBackgroundComponent {
    
    public NAspectPreservingQuadBackground() {
        super();
    }

    public NAspectPreservingQuadBackground( ColorRGBA color ) {
        super(color);
    }

    public NAspectPreservingQuadBackground( ColorRGBA color, float xMargin, float yMargin ) {
        super(color, xMargin, yMargin);
    }

    public NAspectPreservingQuadBackground( ColorRGBA color,
                                    float xMargin, float yMargin, float zOffset,
                                    boolean lit ) {
        super(color, xMargin, yMargin, zOffset, lit);
    }

    @Override
    public void reshape(Vector3f pos, Vector3f size) {
        float smallestDimension = Math.min(size.x, size.y);
        float xMargin =  size.x - smallestDimension;
        float yMargin =  size.y - smallestDimension;
        size.x = smallestDimension;
        size.y = smallestDimension;        
        pos.x += xMargin / 2;
        pos.y += yMargin / 2;
        
        super.reshape(pos, size);
    }

}
