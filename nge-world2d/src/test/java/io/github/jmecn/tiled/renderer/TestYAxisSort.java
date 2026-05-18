/**
 * Copyright (c) 2025-2026, Nostr Game Engine
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 * 
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 
 * 3. Neither the name of the copyright holder nor the names of its
 *    contributors may be used to endorse or promote products derived from
 *    this software without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 * 
 * Nostr Game Engine is a fork of the jMonkeyEngine, which is licensed under
 * the BSD 3-Clause License. 
 */

package io.github.jmecn.tiled.renderer;

import com.jme3.app.SimpleApplication;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.jme3.renderer.Camera;
import com.jme3.renderer.ViewPort;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.debug.WireFrustum;
import com.jme3.scene.shape.Quad;
import com.jme3.shadow.ShadowUtil;
import com.jme3.system.AppSettings;
import com.jme3.texture.FrameBuffer;
import com.jme3.texture.Image;
import com.jme3.texture.Texture;
import com.jme3.texture.Texture2D;
import io.github.jmecn.tiled.core.TiledMap;
import io.github.jmecn.tiled.enums.RenderOrder;
import io.github.jmecn.tiled.renderer.factory.DefaultMaterialFactory;
import io.github.jmecn.tiled.renderer.factory.MaterialFactory;
import io.github.jmecn.tiled.renderer.queue.YAxisComparator;
import io.github.jmecn.tiled.renderer.shape.TileMesh;

/**
 * desc:
 *
 * @author yanmaoyuan
 */
public class TestYAxisSort extends SimpleApplication {

    public static final int TILE_SIZE = 10;
    // the layer distance

    public static void main(String[] args) {
        AppSettings settings = new AppSettings(true);
        settings.setResolution(1280, 720);
        settings.setSamples(4);

        TestYAxisSort app = new TestYAxisSort();
        app.setSettings(settings);
        app.start();
    }

    @Override
    public void simpleInitApp() {
        cam.setLocation(new Vector3f(-4.364303f, 4.856053f, 18.433529f));
        cam.setRotation(new Quaternion(0.015008871f, 0.95650566f, -0.050019775f, 0.2870012f));

        TiledMap tiledMap = new TiledMap(16, 9);
        tiledMap.setTileWidth(TILE_SIZE);
        tiledMap.setTileHeight(TILE_SIZE);
        tiledMap.setRenderOrder(RenderOrder.RIGHT_DOWN);

        OrthogonalRenderer renderer = new OrthogonalRenderer(tiledMap,32,new Node());
        renderer.setLayerDistance(20f);
        renderer.setLayerGap(1f);

        // setup off-screen camera and scene
        Node offScene = createScene(renderer);
        Camera offCamera = setupCamera();
        Texture offTex = setupOffTexture(offCamera, offScene);

        // display the offCamera
        Node scene = createScene(renderer);
        Geometry cameraFrustum = createCameraFrustum(offCamera);

        Node offRoot = new Node("OffRoot");
        offRoot.attachChild(scene);
        offRoot.attachChild(cameraFrustum);
        offRoot.scale(0.1f);// make smaller

        rootNode.attachChild(offRoot);

        Geometry display = displayOffTexture(offTex);
        rootNode.attachChild(display);

        flyCam.setMoveSpeed(10f);
    }

    private Geometry createCameraFrustum(Camera cam) {
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
        return geom;
    }

    private Node createScene(OrthogonalRenderer renderer) {
        Node scene = new Node("Scene");
        scene.setQueueBucket(RenderQueue.Bucket.Opaque);

        MaterialFactory materialFactory = new DefaultMaterialFactory(assetManager);

        // the tile map
        int w = 16;
        int h = 9;
        renderer.visitTiles((x, y, z) -> {
            float r = (float) y / h;
            float g = (float) x / w;
            ColorRGBA color = new ColorRGBA(r, g, 1f, 1f);
            Material mat = materialFactory.newMaterial();
            materialFactory.setColor(mat, color);

            // position
            float px = x * 10f;
            float py = y * 10f;
            float pz = renderer.getTileZAxis(x, y);
            // float pz = getZIndex(x, y);// z-index in the layer

            TileMesh mesh = new TileMesh(new Vector2f(), new Vector2f(TILE_SIZE, TILE_SIZE), new Vector2f(), new Vector2f(0, TILE_SIZE));
            Geometry geom = new Geometry("tile#" + x + "," + y, mesh);
            geom.setMaterial(mat);
            scene.attachChild(geom);

            geom.move(px, pz, py);
        });

        scene.updateGeometricState();
        return scene;
    }

    private Geometry displayOffTexture(Texture offTex) {
        // create a quad to display the texture
        Quad quad = new Quad(16, 9);
        Geometry geom = new Geometry("quad", quad);
        Material mat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        mat.setTexture("ColorMap", offTex);
        geom.setMaterial(mat);

        return geom;
    }

    private Texture setupOffTexture(Camera camera, Node offScene) {
        ViewPort offView = renderManager.createPreView("off-screen", camera);

        // sort by y-axis
        offView.getQueue().setGeometryComparator(RenderQueue.Bucket.Opaque, new YAxisComparator());

        offView.attachScene(offScene);// attach the scene to the viewport to be rendered
        offView.setClearFlags(true, true, true);
        offView.setBackgroundColor(ColorRGBA.DarkGray);

        //setup frame buffer's texture
        Texture2D offTex = new Texture2D(camera.getWidth(), camera.getHeight(), Image.Format.RGBA8);
        offTex.setMinFilter(Texture.MinFilter.Trilinear);
        offTex.setMagFilter(Texture.MagFilter.Nearest);

        // create off-screen frame buffer
        FrameBuffer offBuffer = new FrameBuffer(camera.getWidth(), camera.getHeight(), 1);

        //setup frame buffer to use texture
        offBuffer.addColorTarget(FrameBuffer.FrameBufferTarget.newTarget(offTex));

        //set viewport to render to off-screen frame buffer.
        offView.setOutputFrameBuffer(offBuffer);

        return offTex;
    }

    private Camera setupCamera() {
        float near = -20f;
        float far = 0f;
        float width = 160f;
        float height = 90f;
        float halfWidth = width * 0.5f;
        float halfHeight = height * 0.5f;

        Camera cam = new Camera(160,90);
        cam.setParallelProjection(true);
        cam.setFrustum(near, far, -halfWidth, halfWidth, halfHeight, -halfHeight);
        cam.setLocation(new Vector3f(halfWidth, 0, halfHeight));
        cam.lookAtDirection(new Vector3f(0f, -1f, 0f), new Vector3f(0f, 0f, -1f));

        return cam;
    }
}
