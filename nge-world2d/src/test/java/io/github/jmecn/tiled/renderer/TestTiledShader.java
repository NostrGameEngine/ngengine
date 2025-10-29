package io.github.jmecn.tiled.renderer;

import com.jme3.app.SimpleApplication;
import com.jme3.asset.TextureKey;
import com.jme3.asset.plugins.FileLocator;
import com.jme3.material.Material;
import com.jme3.math.*;
import com.jme3.scene.Geometry;
import com.jme3.system.AppSettings;
import com.jme3.texture.Texture;
import com.jme3.texture.Texture2D;
import io.github.jmecn.tiled.renderer.shape.TileMesh;

import static io.github.jmecn.tiled.renderer.MaterialConst.TILED_J3MD;

/**
 * @author yanmaoyuan
 */
public class TestTiledShader extends SimpleApplication {

    public static void main(String[] args) {
        AppSettings settings = new AppSettings(true);
        settings.setResolution(1280, 720);
        settings.setFrameRate(60);
        settings.setSamples(4);
        settings.setGammaCorrection(false);

        TestTiledShader app = new TestTiledShader();
        app.setSettings(settings);
        app.start();
    }

    private Geometry tile;

    @Override
    public void simpleInitApp() {
        assetManager.registerLocator("examples", FileLocator.class);

        viewPort.setBackgroundColor(ColorRGBA.Pink);

        setupTile();

        setupCamera();
    }

    private void setupTile() {

        TextureKey key = new TextureKey("Orthogonal/perspective_walls.png", true);
        key.setGenerateMips(false);

        Texture2D texture = (Texture2D) assetManager.loadTexture(key);
        texture.setMagFilter(Texture.MagFilter.Nearest);

        Vector2f imageSize = new Vector2f(texture.getImage().getWidth(), texture.getImage().getHeight());// 256x256
        TileMesh mesh = new TileMesh(new Vector2f(128, 0), new Vector2f(1, 1), new Vector2f(0, 0), new Vector2f(-0.5f, 0.5f));

        // create material
        Material mat = new Material(assetManager, TILED_J3MD);
        mat.setTexture("ColorMap", texture);
        mat.setVector2("ImageSize", imageSize);
        mat.setBoolean("UseTilesetImage", true);
        mat.setVector4("TileSize", new Vector4f(64, 64, 0, 0));

        Geometry geom = new Geometry("Tile", mesh);
        geom.setMaterial(mat);

        tile = geom;
        rootNode.attachChild(geom);
    }

    private void setupCamera() {
        flyCam.setEnabled(false);

        float near = -1f;
        float far = 1f;
        float ratio = (float) cam.getWidth() / cam.getHeight();
        cam.setFrustum(near, far, -ratio, ratio, 1f, -1f);

        cam.setParallelProjection(true);
        cam.lookAtDirection(new Vector3f(0f, -1f, 0f), new Vector3f(0f, 0f, -1f));
        cam.setLocation(new Vector3f(0f, 0, 0f));
    }
}
