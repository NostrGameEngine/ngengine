package org.ngengine.gui.components;

import com.simsilica.lemur.Panel;
import com.simsilica.lemur.style.ElementId;

public class VSpacer extends Panel{
    public static final String ELEMENT_ID = "vspacer";

    public VSpacer(){
        super(new ElementId(ELEMENT_ID));
        setPreferredSize(new com.jme3.math.Vector3f(0, 12, 0));
        setBackground(null);
        setBorder(null);
    }

    public void setHeight(int height){
        setPreferredSize(new com.jme3.math.Vector3f(0, height, 0));
    }
    
}
