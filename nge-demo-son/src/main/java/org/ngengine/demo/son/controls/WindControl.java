package org.ngengine.demo.son.controls;

import com.jme3.material.MatParamOverride;
import com.jme3.math.Vector3f;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.ViewPort;
import com.jme3.scene.control.AbstractControl;
import com.jme3.shader.VarType;

public class WindControl extends AbstractControl {
    private Vector3f wind = new Vector3f(0, 0, 1f);
 
    private MatParamOverride windDirection = new MatParamOverride(VarType.Vector3, "WindDirection", wind);

    public void setWind(Vector3f wind) {
        this.wind = wind;
        this.windDirection.setValue(wind);
    }


    public Vector3f getWind() {
        return wind;
    }


    @Override 
    public void setSpatial(com.jme3.scene.Spatial spatial) {
        super.setSpatial(spatial);
        if (spatial != null) {
            spatial.addMatParamOverride(windDirection);
        }
    }

    @Override
    protected void controlUpdate(float tpf) {
       
    }

    @Override
    protected void controlRender(RenderManager rm, ViewPort vp) {
        
    }
    
}
