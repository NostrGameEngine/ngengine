package org.ngengine.world2d.tiled.renderer;

import com.jme3.material.Material;
import com.jme3.material.MaterialDef;
import com.jme3.material.RenderState;
import com.jme3.math.ColorRGBA;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.shader.VarType;
import org.junit.jupiter.api.Test;
import org.ngengine.world2d.tiled.enums.LayerBlendMode;
import org.ngengine.world2d.tiled.renderer.factory.DefaultMaterialFactory;

import static org.junit.jupiter.api.Assertions.*;

public class TestSpatialMaterialProperties {
    @Test
    public void standardSpatialMaterialsKeepAuthoredAppearanceWithoutTileParameters() {
        MaterialDef definition = new MaterialDef(null, "standard-model");
        definition.addMaterialParam(VarType.Vector4, "BaseColor", ColorRGBA.White);
        Material material = new Material(definition);
        material.setColor("BaseColor", ColorRGBA.Brown);
        material.getAdditionalRenderState().setBlendMode(RenderState.BlendMode.Off);
        Node root = new Node();
        Node child = new Node();
        Geometry geometry = new Geometry();
        geometry.setMaterial(material);
        child.attachChild(geometry);
        root.attachChild(child);
        DefaultMaterialFactory factory = new DefaultMaterialFactory(null);
        factory.setTintColor(root, ColorRGBA.Red);
        factory.setLayerOpacity(root, 1f);
        factory.setOpacity(root, 1f);
        factory.setBlendMode(root, LayerBlendMode.NORMAL);
        assertEquals(ColorRGBA.Brown, material.getParam("BaseColor").getValue());
        assertEquals(RenderState.BlendMode.Off, material.getAdditionalRenderState().getBlendMode());
    }

    @Test
    public void tileParametersStillReachDeeplyNestedGeometry() {
        MaterialDef definition = new MaterialDef(null, "tile");
        definition.addMaterialParam(VarType.Boolean, MaterialConst.USE_TINT_COLOR, false);
        definition.addMaterialParam(VarType.Vector4, MaterialConst.TINT_COLOR, ColorRGBA.White);
        definition.addMaterialParam(VarType.Float, MaterialConst.LAYER_OPACITY, 1f);
        definition.addMaterialParam(VarType.Float, MaterialConst.OPACITY, 1f);
        Material material = new Material(definition);
        Geometry geometry = new Geometry();
        geometry.setMaterial(material);
        Node root = new Node();
        Node child = new Node();
        Node grandchild = new Node();
        root.attachChild(child);
        child.attachChild(grandchild);
        grandchild.attachChild(geometry);
        DefaultMaterialFactory factory = new DefaultMaterialFactory(null);
        factory.setTintColor(root, ColorRGBA.Red);
        factory.setOpacity(root, 0.3f);
        factory.setLayerOpacity(root, 0.5f);
        factory.setBlendMode(root, LayerBlendMode.NORMAL);
        assertEquals(ColorRGBA.Red, material.getParam(MaterialConst.TINT_COLOR).getValue());
        assertEquals(0.3f, (Float) material.getParam(MaterialConst.OPACITY).getValue(), 0f);
        assertEquals(0.5f, (Float) material.getParam(MaterialConst.LAYER_OPACITY).getValue(), 0f);
        assertEquals(RenderState.BlendMode.Alpha, material.getAdditionalRenderState().getBlendMode());
    }
}
