package org.ngengine.gui.components.containers;

  
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.simsilica.lemur.Container;
import com.simsilica.lemur.FillMode;
import com.simsilica.lemur.component.BorderLayout;
import com.simsilica.lemur.core.GuiLayout;
import com.simsilica.lemur.style.ElementId;

public class NPanel extends NContainer{
    public NPanel( ) {
        super(new BorderLayout());
    }

    public NPanel(ElementId id) {
        super(new BorderLayout(), id);
    }


    public NRow addRow() {
        NRow row = new NRow();
        addChild(row,BorderLayout.Position.Center);
        return row;
    }

    public NColumn addCol() {
        NColumn col = new NColumn();
        addChild(col, BorderLayout.Position.Center);
        return col;
    }

    public NPanel addSubPanel() {
        NPanel panel = new NPanel();
        addChild(panel, BorderLayout.Position.Center);
        return panel;
    }
 
    public NRow addRow(BorderLayout.Position position) {
        NRow row = new NRow();
        addChild(row, position);
        return row;
    }

    public NColumn addCol(BorderLayout.Position position) {
        NColumn col = new NColumn();
        addChild(col, position);
        return col;
    }

    public NPanel addSubPanel(BorderLayout.Position position) {
        NPanel panel = new NPanel();
        addChild(panel, position);
        return panel;
    }

    public <T extends Node> T addChild(T child, BorderLayout.Position position) {        
        return super.addChild(child, position );
    }

   
}
