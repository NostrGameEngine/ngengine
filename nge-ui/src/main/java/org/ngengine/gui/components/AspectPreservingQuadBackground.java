package org.ngengine.gui.components;

import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.simsilica.lemur.component.QuadBackgroundComponent;

public class AspectPreservingQuadBackground extends QuadBackgroundComponent {
    
    public AspectPreservingQuadBackground() {
        super();
    }

    public AspectPreservingQuadBackground( ColorRGBA color ) {
        super(color);
    }

    public AspectPreservingQuadBackground( ColorRGBA color, float xMargin, float yMargin ) {
        super(color, xMargin, yMargin);
    }

    public AspectPreservingQuadBackground( ColorRGBA color,
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
