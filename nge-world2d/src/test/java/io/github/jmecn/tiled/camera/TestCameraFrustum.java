package io.github.jmecn.tiled.camera;

import com.jme3.app.SimpleApplication;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.jme3.renderer.Camera;
import com.jme3.scene.Geometry;
import com.jme3.scene.debug.WireFrustum;
import com.jme3.shadow.ShadowUtil;

/**
 * desc:
 *
 * @author yanmaoyuan
 */
public class TestCameraFrustum extends SimpleApplication {

    public static void main(String[] args) {
        TestCameraFrustum app = new TestCameraFrustum();
        app.start();
    }

    @Override
    public void simpleInitApp() {
        cam.setLocation(new Vector3f(10, 5, 10));
        cam.lookAt(Vector3f.ZERO, Vector3f.UNIT_Y);

        Camera cam = setupCamera();
        createCameraFrustum(cam);
        flyCam.setMoveSpeed(10);
    }

    private Camera setupCamera() {
        float near = -10f;
        float far = 10f;
        float width = 16f;
        float height = 9f;
        float halfWidth = width * 0.5f;
        float halfHeight = height * 0.5f;

        Camera cam = new Camera(16,9);
        cam.setParallelProjection(true);
        cam.setFrustum(near, far, -halfWidth, halfWidth, halfHeight, -halfHeight);
        cam.setLocation(new Vector3f(halfWidth, 0, halfHeight));
        cam.lookAtDirection(new Vector3f(0f, -1f, 0f), new Vector3f(0f, 0f, -1f));

        return cam;
    }

    private void createCameraFrustum(Camera cam) {
        Vector3f[] points = new Vector3f[8];
        for (int i = 0; i < 8; i++) {
            points[i] = new Vector3f();
        }

        ShadowUtil.updateFrustumPoints2(cam, points);

        WireFrustum frustum = new WireFrustum(points);
        Geometry geom = new Geometry("frustum", frustum);

        Material mat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        mat.setColor("Color", ColorRGBA.White);
        geom.setMaterial(mat);
        rootNode.attachChild(geom);
    }
}
