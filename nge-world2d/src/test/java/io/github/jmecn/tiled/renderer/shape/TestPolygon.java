package io.github.jmecn.tiled.renderer.shape;

import java.util.ArrayList;
import java.util.List;

import com.jme3.app.SimpleApplication;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector2f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import com.jme3.system.AppSettings;
import io.github.jmecn.tiled.renderer.MaterialConst;

public class TestPolygon extends SimpleApplication {

    @Override
    public void simpleInitApp() {

        // points
        List<Vector2f> points = new ArrayList<Vector2f>();
        points.add(new Vector2f(0,0));
        points.add(new Vector2f(55,-23));
        points.add(new Vector2f(96,-117));
        points.add(new Vector2f(110,-61));
        points.add(new Vector2f(104,-42));
        points.add(new Vector2f(119,-33));
        points.add(new Vector2f(116,6));
        points.add(new Vector2f(104,9));
        points.add(new Vector2f(100,36));
        points.add(new Vector2f(60,43));
        points.add(new Vector2f(53,58));
        points.add(new Vector2f(43,58));
        points.add(new Vector2f(34,74));
        points.add(new Vector2f(21,69));
        points.add(new Vector2f(18,90));
        points.add(new Vector2f(0,89));
        
        Mesh mesh = new Polygon(points, false);
        
        Material mat = new Material(assetManager, MaterialConst.TILED_J3MD);
        mat.setColor(MaterialConst.COLOR, ColorRGBA.Red);

        Geometry geom = new Geometry("polygon", mesh);
        geom.setMaterial(mat);
        
        viewPort.setBackgroundColor(ColorRGBA.DarkGray);
        rootNode.attachChild(geom.scale(1/32f));
        
        flyCam.setMoveSpeed(10);
    }

    public static void main(String[] args) {
        TestPolygon app = new TestPolygon();
        app.setSettings(new AppSettings(true));
        app.start();
    }

}
