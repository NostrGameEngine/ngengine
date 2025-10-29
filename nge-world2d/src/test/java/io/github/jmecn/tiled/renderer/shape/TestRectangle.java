package io.github.jmecn.tiled.renderer.shape;

import com.jme3.app.SimpleApplication;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import com.jme3.system.AppSettings;
import io.github.jmecn.tiled.renderer.MaterialConst;

public class TestRectangle extends SimpleApplication {

    @Override
    public void simpleInitApp() {

        Mesh mesh = new Rect(30, 22, false);
        
        Material mat = new Material(assetManager, MaterialConst.TILED_J3MD);
        mat.setColor(MaterialConst.COLOR, ColorRGBA.Red);

        Geometry geom = new Geometry("rectangle", mesh);
        geom.setMaterial(mat);
        
        viewPort.setBackgroundColor(ColorRGBA.DarkGray);
        rootNode.attachChild(geom.scale(1/32f));
        
        flyCam.setMoveSpeed(10);
    }

    public static void main(String[] args) {
        TestRectangle app = new TestRectangle();
        app.setSettings(new AppSettings(true));
        app.start();
    }

}
