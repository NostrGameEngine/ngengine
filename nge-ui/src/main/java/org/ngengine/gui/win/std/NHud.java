package org.ngengine.gui.win.std;

import org.ngengine.gui.components.containers.NColumn;
import org.ngengine.gui.components.containers.NPanel;
import org.ngengine.gui.components.containers.NRow;
import org.ngengine.gui.win.NWindow;

import com.jme3.math.Vector3f;
import com.simsilica.lemur.FillMode;
import com.simsilica.lemur.component.BorderLayout;
import com.simsilica.lemur.component.DynamicInsetsComponent;
import com.simsilica.lemur.style.ElementId;

public class NHud extends NWindow<Void> {
    private NPanel top;
    private NPanel center;
    private NPanel bottom;

    private NColumn centerCenter;
    private NColumn centerLeft;
    private NColumn centerRight;

    private NRow bottomCenter;
    private NRow bottomLeft;
    private NRow bottomRight;

    private NRow topCenter;
    private NRow topLeft;
    private NRow topRight;

    private NPanel gameConsole;
    private boolean initialized = false;

    public NHud() {
        super(new ElementId("hud"));
        top = new NPanel();
        top.setInsetsComponent(new DynamicInsetsComponent(0, 0, 1, 0));

        center = new NPanel();

        bottom = new NPanel();
        bottom.setInsetsComponent(new DynamicInsetsComponent(1, 0, 0, 0));

        centerCenter = new NColumn();
        centerLeft = new NColumn();
        centerLeft.setInsetsComponent(new DynamicInsetsComponent(0, 0, 0, 1));

        centerRight = new NColumn();
        centerRight.setInsetsComponent(new DynamicInsetsComponent(0, 1, 0, 0));

        bottomCenter = new NRow();
        bottomLeft = new NRow();
        bottomLeft.setInsetsComponent(new DynamicInsetsComponent(0, 0, 0, 1));

        bottomRight = new NRow();
        bottomRight.setInsetsComponent(new DynamicInsetsComponent(0,1, 0, 0));

        topCenter = new NRow();
        topLeft = new NRow();
        topLeft.setInsetsComponent(new DynamicInsetsComponent(0, 0, 0, 1));

        topRight = new NRow();
        topRight.setInsetsComponent(new DynamicInsetsComponent(0, 1, 0, 0));

        top.addChild(topCenter, BorderLayout.Position.Center);
        top.addChild(topLeft, BorderLayout.Position.West);
        top.addChild(topRight, BorderLayout.Position.East);

        center.addChild(centerCenter, BorderLayout.Position.Center);
        center.addChild(centerLeft, BorderLayout.Position.West);
        center.addChild(centerRight, BorderLayout.Position.East);

        bottom.addChild(bottomCenter, BorderLayout.Position.Center);
        bottom.addChild(bottomLeft, BorderLayout.Position.West);
        bottom.addChild(bottomRight, BorderLayout.Position.East);
    }

    @Override
    protected void preCompose(Vector3f size, Void args) throws Throwable {
        if(!initialized){
        

        


            
            

            initialize(size);
            initialized = true;
        }
        setFitContent(false);
        setSize(size);

        NPanel content = getContent();
        content.addChild(top, BorderLayout.Position.North);
        content.addChild(center, BorderLayout.Position .Center);
        content.addChild(bottom, BorderLayout.Position.South);
        
    }

    protected void initialize(Vector3f size) throws Throwable {
     
    }

    @Override
    protected void compose(Vector3f size, Void args) throws Throwable {
       
    }


    public NRow getTop() {
        return topCenter;
    }
    public NRow getTopLeft() {
        return topLeft;
    }

    public NRow getTopRight() {
        return topRight;
    }

    public NColumn getCenter() {
        return centerCenter;
    }
    

    public NColumn getCenterLeft() {
        return centerLeft;
    }
    public NColumn getCenterRight() {
        return centerRight;
    }

    public NRow getBottom() {
        return bottomCenter;
    }

    public NRow getBottomLeft() {
        return bottomLeft;
    }

    public NRow getBottomRight() {
        return bottomRight;
    }

    

     
    
}
