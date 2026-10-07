package com.jme3.anim;

import com.jme3.material.Material;
import com.jme3.material.MaterialDef;
import com.jme3.renderer.Camera;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.ViewPort;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.VertexBuffer;
import com.jme3.scene.mesh.MorphTarget;
import com.jme3.scene.shape.Box;
import com.jme3.shader.VarType;
import com.jme3.system.NullRenderer;
import com.jme3.util.BufferUtils;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class MorphControlZeroSlotsTest {
    @Test
    void noAvailableGpuSlotsDoesNotIndexTheMissingFallbackSlot() {
        Box mesh = new Box(1, 1, 1);
        MorphTarget target = new MorphTarget("target");
        target.setBuffer(VertexBuffer.Type.Position, BufferUtils.createFloatBuffer(mesh.getVertexCount() * 3));
        mesh.addMorphTarget(target);
        MaterialDef definition = new MaterialDef(null, "morph");
        definition.addMaterialParam(VarType.FloatArray, "MorphWeights", null);
        Material material = new Material(definition);
        material.setParam("MorphWeights", VarType.FloatArray, new float[0]);
        Geometry geometry = new Geometry("geometry", mesh);
        geometry.setMaterial(material);
        geometry.setNbSimultaneousGPUMorph(0);
        geometry.setMorphState(new float[]{1f});
        Node root = new Node();
        root.attachChild(geometry);
        MorphControl control = new MorphControl();
        root.addControl(control);
        RenderManager manager = new RenderManager(new NullRenderer());
        ViewPort viewport = new ViewPort("test", new Camera(64, 64));
        assertDoesNotThrow(() -> control.render(manager, viewport));
        assertFalse(geometry.isDirtyMorph());
        assertNull(geometry.getFallbackMorphTarget());
    }
}
