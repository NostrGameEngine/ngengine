package org.ngengine.gui.components.containers;

 
import com.simsilica.lemur.Axis;
import com.simsilica.lemur.Container;
import com.simsilica.lemur.FillMode;
import com.simsilica.lemur.component.BoxLayout;
import com.simsilica.lemur.component.SpringGridLayout;
import com.simsilica.lemur.core.GuiLayout;

public class NColumn extends NContainer { 
    public NColumn( ) {
        super(new SpringGridLayout(Axis.Y, Axis.X, FillMode.None, FillMode.Even));
    }

    public void setFillMode(FillMode horizontalFill, FillMode verticalFill) {
        setLayout(new SpringGridLayout(Axis.X, Axis.Y, horizontalFill, verticalFill));
    }

}