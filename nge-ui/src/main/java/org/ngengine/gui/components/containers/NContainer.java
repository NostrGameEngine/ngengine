package org.ngengine.gui.components.containers;


import com.jme3.scene.Node;
import com.simsilica.lemur.Container;
import com.simsilica.lemur.core.GuiLayout;
import com.simsilica.lemur.style.ElementId;

public class NContainer extends Container{
        public NContainer( GuiLayout layout ) {
        super(layout);
    }

    public NContainer(GuiLayout layout, ElementId id) {
        super(layout, id);
    }
    public NRow addRow( ) {
        NRow row = new NRow();
        addChild(row);
        return row;
    }

    public NColumn addCol( ) {
        NColumn col = new NColumn();
        addChild(col);
        return col;
    }

    public NPanel addSubPanel() {
        NPanel panel = new NPanel();
        addChild(panel);
        return panel;
    }
 

}
