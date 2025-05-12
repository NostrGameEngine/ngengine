package org.ngengine.demo.soo;

import java.util.function.Predicate;

import com.jme3.math.Plane;
import com.jme3.renderer.Camera;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.ViewPort;
import com.jme3.scene.Geometry;
import com.jme3.texture.FrameBuffer;
import com.jme3.water.ReflectionProcessor;

public class ReflectionBaker extends ReflectionProcessor {
    private final Geometry oceanGeom;
    private RenderManager rm;
    private Predicate<Geometry> lastFilter =null;
    
    public ReflectionBaker(
        Geometry oceanGeom, 
        Camera reflectionCam, FrameBuffer reflectionBuffer, Plane reflectionClipPlane) {
        super(reflectionCam, reflectionBuffer, reflectionClipPlane);
        this.oceanGeom = oceanGeom;
    }

    @Override
    public void initialize(RenderManager rm, ViewPort vp) {
        this.rm = rm;
        super.initialize(rm, vp);
    }

    @Override
    public void preFrame(float tpf) {
        this.lastFilter = rm.getRenderFilter();
        // rm.setRenderFilter(g->!g.equals(oceanGeom));
        super.preFrame(tpf);
    }

    @Override
    public void postFrame(FrameBuffer out) {
        super.postFrame(out);
        // rm.setRenderFilter(lastFilter);
        lastFilter = null;
    }


}
