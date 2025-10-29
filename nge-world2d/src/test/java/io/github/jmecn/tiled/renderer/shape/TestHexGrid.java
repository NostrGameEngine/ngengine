package io.github.jmecn.tiled.renderer.shape;

import com.jme3.app.SimpleApplication;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.scene.Geometry;
import com.jme3.system.AppSettings;
import io.github.jmecn.tiled.enums.StaggerAxis;
import io.github.jmecn.tiled.enums.StaggerIndex;
import io.github.jmecn.tiled.renderer.factory.DefaultMaterialFactory;
import io.github.jmecn.tiled.renderer.factory.MaterialFactory;

/**
 * desc:
 *
 * @author yanmaoyuan
 */
public class TestHexGrid extends SimpleApplication {

    @Override
    public void simpleInitApp() {
        HexGrid grid = new HexGrid(10, 10, 14, 12, 6, StaggerAxis.X, StaggerIndex.EVEN);

        MaterialFactory factory = new DefaultMaterialFactory(assetManager);
        Material material = factory.newMaterial();
        factory.setColor(material, ColorRGBA.White);

        Geometry gridGeom = new Geometry("grid", grid);
        gridGeom.setMaterial(material);
        rootNode.attachChild(gridGeom);

        viewPort.setBackgroundColor(ColorRGBA.DarkGray);
        rootNode.scale(1/32f);

        flyCam.setMoveSpeed(10);
    }

    public static void main(String[] args) {
        TestHexGrid app = new TestHexGrid();
        app.setSettings(new AppSettings(true));
        app.start();
    }

}
