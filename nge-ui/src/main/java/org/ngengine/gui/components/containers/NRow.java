package org.ngengine.gui.components.containers;

 

import com.simsilica.lemur.Axis;
import com.simsilica.lemur.FillMode;
import com.simsilica.lemur.component.SpringGridLayout;

public class NRow extends NContainer{
 
    
   
    public NRow( ) {
        super(new SpringGridLayout(Axis.X, Axis.Y, FillMode.Even, FillMode.None));
    }


    public void setFillMode(FillMode horizontalFill, FillMode verticalFill) {        
        setLayout(new SpringGridLayout(Axis.X, Axis.Y, horizontalFill, verticalFill));
    }


}
