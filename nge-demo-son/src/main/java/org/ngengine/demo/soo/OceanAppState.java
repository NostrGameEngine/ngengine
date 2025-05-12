package org.ngengine.demo.soo;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import javax.swing.text.View;

import com.jme3.app.Application;
import com.jme3.app.state.BaseAppState;
import com.jme3.asset.AssetManager;
import com.jme3.bullet.BulletAppState;
import com.jme3.export.InputCapsule;
import com.jme3.export.JmeImporter;
import com.jme3.material.MatParamOverride;
import com.jme3.material.Material;
import com.jme3.material.RenderState.BlendMode;
import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.math.Plane;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.jme3.post.FilterPostProcessor;
import com.jme3.post.SceneProcessor;
import com.jme3.profile.AppProfiler;
import com.jme3.renderer.Camera;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.ViewPort;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.renderer.queue.RenderQueue.Bucket;
import com.jme3.renderer.queue.RenderQueue.ShadowMode;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.VertexBuffer;
import com.jme3.scene.control.AbstractControl;
import com.jme3.scene.shape.Box;
import com.jme3.scene.shape.Quad;
import com.jme3.shader.VarType;
import com.jme3.texture.FrameBuffer;
import com.jme3.texture.Image;
import com.jme3.texture.Image.Format;
import com.jme3.texture.Texture;
import com.jme3.texture.Texture2D;
import com.jme3.ui.Picture;
import com.jme3.util.BufferUtils;
import com.jme3.util.mikktspace.MikktspaceTangentGenerator;
import com.jme3.water.ReflectionProcessor;
import com.jme3.water.WaterUtils;
import com.jme3.texture.FrameBuffer.FrameBufferTarget;
import com.jme3.texture.Texture.WrapMode;

public class OceanAppState extends BaseAppState {
    private Material oceanMaterial;
    private Geometry oceanGeometry;
    private Geometry oceanProcessor;
    private FlipBookTexture oceanNormalHeight;
    FrameBuffer bouyancyFrameBuffer;
    private List<ViewPort> innerViewPorts = new ArrayList<>();

    private final int VERTEX_DENSITY = 512;
    private final float GRID_SIZE = 160f;
    private final float HORIZON_EXTENT = 4000f;
    private final Format processorFormat = Image.Format.RGB8;
    private final Format reflectionsFormat = Image.Format.RGB8;
    private final List<BuoyancyControl> controls = new ArrayList<>();
    private final int buoyancyMapResolution = 64;
    private final Vector3f WAVE_SCALE = new Vector3f(3, 2.6f, 3);

    private final int reflectionSize = 1024;
    private Texture2D reflectionMap;

    private float animSpeed = 22f;
    private float waterHeight = 0f;
    private float time = 0;
    private Texture2D buoyancyMap;
    private ByteBuffer buoyancyBuffer = null;

    protected OceanAppState() {

    }

    @Override
    protected void initialize(Application app) {

    }

    @Override
    protected void cleanup(Application app) {

    }

    public Material getMaterial() {
        return oceanMaterial;
    }

    /**
     * Generate a flat grid mesh in the XZ‑plane, centered at origin.
     * 
     * @param xSamples
     *            number of vertices in X direction
     * @param zSamples
     *            number of vertices in Z direction
     * @param size
     *            world‑space width and depth of the grid
     */
    public static Mesh generateGridMesh(int centerRes, float centerSize, float outerSize) {
        // centerRes = resolution of the detailed center area (e.g., 1024)
        // centerSize = size of the detailed center area (e.g., 200f)
        // outerSize = total size of the entire mesh (e.g., 2000f)

        // Calculate additional vertices needed for outer rings
        final int outerRes = 32; // Resolution for the outer sections (low detail)
        final int totalRes = centerRes + outerRes * 2; // Total grid resolution including outer parts

        Mesh mesh = new Mesh();
        int vertCount = totalRes * totalRes;
        Vector3f[] positions = new Vector3f[vertCount];
        Vector2f[] uvs = new Vector2f[vertCount];
        int[] indices = new int[(totalRes - 1) * (totalRes - 1) * 6];
        Vector3f[] normals = new Vector3f[vertCount];

        // Parameters for transitioning from center to outer
        float outerOffset = centerSize / 2f;
        float outerExtent = (outerSize - centerSize) / 2f;

        // Fill positions & UVs
        int idx = 0;
        for (int z = 0; z < totalRes; z++) {
            for (int x = 0; x < totalRes; x++) {
                // Determine in which region this vertex falls
                boolean inCenterX = x >= outerRes && x < centerRes + outerRes;
                boolean inCenterZ = z >= outerRes && z < centerRes + outerRes;

                float posX, posZ;
                float uvX = (float) x / (totalRes - 1);
                float uvZ = (float) z / (totalRes - 1);

                if (inCenterX) {
                    // Inside center region on X axis
                    float centerX = (float) (x - outerRes) / centerRes;
                    posX = (centerX - 0.5f) * centerSize;
                } else if (x < outerRes) {
                    // Left outer region
                    float t = (float) x / outerRes;
                    posX = -outerOffset - outerExtent * (1f - t);
                } else {
                    // Right outer region
                    float t = (float) (x - centerRes - outerRes) / outerRes;
                    posX = outerOffset + outerExtent * t;
                }

                if (inCenterZ) {
                    // Inside center region on Z axis
                    float centerZ = (float) (z - outerRes) / centerRes;
                    posZ = (centerZ - 0.5f) * centerSize;
                } else if (z < outerRes) {
                    // Bottom outer region
                    float t = (float) z / outerRes;
                    posZ = -outerOffset - outerExtent * (1f - t);
                } else {
                    // Top outer region
                    float t = (float) (z - centerRes - outerRes) / outerRes;
                    posZ = outerOffset + outerExtent * t;
                }

                positions[idx] = new Vector3f(posX, 0, posZ);
                uvs[idx] = new Vector2f(uvX, uvZ);
                normals[idx] = new Vector3f(0, 1, 0);
                idx++;
            }
        }

        // Fill triangle indices
        int tri = 0;
        for (int z = 0; z < totalRes - 1; z++) {
            for (int x = 0; x < totalRes - 1; x++) {
                int i0 = z * totalRes + x;
                int i1 = i0 + 1;
                int i2 = i0 + totalRes;
                int i3 = i2 + 1;
                // first triangle
                indices[tri++] = i0;
                indices[tri++] = i2;
                indices[tri++] = i1;
                // second triangle
                indices[tri++] = i1;
                indices[tri++] = i2;
                indices[tri++] = i3;
            }
        }

        // Send to mesh
        mesh.setBuffer(VertexBuffer.Type.Position, 3, BufferUtils.createFloatBuffer(positions));
        mesh.setBuffer(VertexBuffer.Type.TexCoord, 2, BufferUtils.createFloatBuffer(uvs));
        mesh.setBuffer(VertexBuffer.Type.Index, 3, BufferUtils.createIntBuffer(indices));
        mesh.setBuffer(VertexBuffer.Type.Normal, 3, BufferUtils.createFloatBuffer(normals));

        mesh.updateBound();
        return mesh;
    }

    Camera envCam;
    ReflectionBaker reflectionProcessor;
    Plane plane;

    private void initializeReflectionView(ViewPort vp, Node scene) {
        AssetManager assetManager = getApplication().getAssetManager();
        RenderManager renderManager = getApplication().getRenderManager();

        FrameBuffer refbuf = new FrameBuffer(reflectionSize, reflectionSize, 1);
        reflectionMap = new Texture2D(reflectionSize, reflectionSize, reflectionsFormat);
        reflectionMap.setName("ReflectionMap");
        refbuf.addColorTarget(FrameBufferTarget.newTarget(reflectionMap));

        envCam = new Camera(reflectionSize, reflectionSize);
        plane = new Plane(Vector3f.UNIT_Y, new Vector3f(0, waterHeight, 0).dot(Vector3f.UNIT_Y));

        reflectionProcessor = new ReflectionBaker(oceanGeometry, envCam, refbuf, plane);
        reflectionProcessor.setReflectionClipPlane(plane);
        ViewPort reflectionViewPort = renderManager.createPreView("ReflectionViewport", envCam);
        reflectionViewPort.setClearFlags(true, true, true);
        reflectionViewPort.setBackgroundColor(ColorRGBA.Pink);
        reflectionViewPort.attachScene(scene);
        reflectionViewPort.addProcessor(reflectionProcessor);

        // Create frame buffer for height sampling

    }

    private void initializeOceanView(Node scene) {
        AssetManager assetManager = getApplication().getAssetManager();
        RenderManager renderManager = getApplication().getRenderManager();

        Camera buoyancyCam = new Camera(buoyancyMapResolution, buoyancyMapResolution);
        buoyancyCam.setViewPort(0, 1, 0, 1);
        buoyancyCam.update();

        ViewPort buoyancyViewPort = renderManager.createPreView("OceanViewport", buoyancyCam);
        buoyancyViewPort.setClearFlags(true, true, true);
        buoyancyViewPort.setBackgroundColor(ColorRGBA.Pink);
        innerViewPorts.add(buoyancyViewPort);

        Node bakeScene = new Node("BuoyancyBakeScene");
        bakeScene.setCullHint(Spatial.CullHint.Never);
        bakeScene.attachChild(oceanProcessor);
        buoyancyViewPort.attachScene(bakeScene);

        // Create frame buffer for height sampling
        bouyancyFrameBuffer = new FrameBuffer(buoyancyMapResolution, buoyancyMapResolution, 1);
        buoyancyMap = new Texture2D(buoyancyMapResolution, buoyancyMapResolution, processorFormat);
        buoyancyMap.setName("BuoyancyMap");
        bouyancyFrameBuffer.addColorTarget(FrameBufferTarget.newTarget(buoyancyMap));
        buoyancyViewPort.setOutputFrameBuffer(bouyancyFrameBuffer);

        MatParamOverride renderBuoyancyMap = new MatParamOverride(VarType.Boolean, "RenderBuoyancyMap", true);
        buoyancyViewPort.addProcessor(new SceneProcessor() {

            @Override
            public void initialize(RenderManager rm, ViewPort vp) {

            }

            @Override
            public void reshape(ViewPort vp, int w, int h) {

            }

            @Override
            public boolean isInitialized() {
                return true;
            }

            @Override
            public void preFrame(float tpf) {
                renderManager.addForcedMatParam(renderBuoyancyMap);
            }

            @Override
            public void cleanup() {
                renderManager.removeForcedMatParam(renderBuoyancyMap);
            }

            @Override
            public void postQueue(RenderQueue rq) {

            }

            @Override
            public void postFrame(FrameBuffer out) {
                cleanup();
            }

            @Override
            public void setProfiler(AppProfiler profiler) {

            }

        });

    }

    @Override
    protected void onEnable() {
        AssetManager assetManager = getApplication().getAssetManager();
        RenderManager rm = getApplication().getRenderManager();
        ViewPort vp = getApplication().getViewPort();
        Node rootNode = (Node) vp.getScenes().get(0);

        // load materials
        oceanMaterial = new Material(assetManager, "ibocean/Ocean.j3md");
        oceanMaterial.getAdditionalRenderState().setBlendMode(BlendMode.AlphaSumA);
        oceanMaterial.setVector3("Size", new Vector3f(GRID_SIZE, WAVE_SCALE.y, GRID_SIZE));
        oceanMaterial.setVector3("Scale", WAVE_SCALE);

        { // foam
            Texture2D foamTexture = (Texture2D) assetManager
                    .loadTexture("Common/MatDefs/Water/Textures/foam2.jpg");
            foamTexture.setWrap(WrapMode.Repeat);
            oceanMaterial.setTexture("FoamTexture", foamTexture);
        }
        { // normals

            oceanNormalHeight = FlipBookTexture.load(getApplication().getAssetManager(),
                    "ibocean/normalHeight", ".png", 64, true);
            oceanNormalHeight.apply(oceanMaterial, "NormalHeight");

        }

        // create geometries
        oceanGeometry = new Geometry("OceanSurface",
                generateGridMesh(VERTEX_DENSITY, GRID_SIZE, HORIZON_EXTENT));
        oceanGeometry.setMaterial(oceanMaterial);
        oceanGeometry.setQueueBucket(Bucket.Transparent);
        oceanGeometry.setShadowMode(ShadowMode.Receive);
        rootNode.attachChild(oceanGeometry);

        oceanProcessor = new Picture("oceanImposter");
        oceanProcessor.setMaterial(oceanMaterial);
        oceanProcessor.setShadowMode(ShadowMode.Off);

        // generate tangents
        MikktspaceTangentGenerator.generate(oceanGeometry);

        initializeOceanView(rootNode);
        initializeReflectionView(vp, rootNode);
        oceanMaterial.setTexture("RefMap", reflectionMap);

        // Picture debugPic = new Picture("BuoyancyDebug");
        // debugPic.setTexture(getApplication().getAssetManager(), buoyancyMap, false);
        // debugPic.setPosition(20, 20);
        // debugPic.setWidth(200);
        // debugPic.setHeight(200);
        // getApplication().getGuiNode().attachChild(debugPic);

    }


    @Override
    public void update(float tpf) {
        super.update(tpf);
        Camera cam = getApplication().getCamera();

        WaterUtils.updateReflectionCam(envCam, plane, cam);

        time += tpf * animSpeed;
        int animFrame = (int) ((time) % oceanNormalHeight.getFrameCount());
        oceanNormalHeight.setFrame(animFrame);

        Vector3f t = oceanGeometry.getLocalTranslation();
        t.x=cam.getLocation().getX();
        t.z=cam.getLocation().getZ();
        t.y = waterHeight;
        oceanGeometry.setLocalTranslation(t);

        // scale.set(SIZE * s1, waveHeight, SIZE * s1);
        // oceanMaterial.setVector3("Scale", scale);

        // offset.x = cam.getLocation().x;
        // offset.z = cam.getLocation().z;
        // offset.y = waterHeight;
        // oceanMaterial.setVector3("Offset", offset);

        for (ViewPort vp : innerViewPorts) {
            for (Spatial spat : vp.getScenes()) {
                spat.updateGeometricState();
                spat.updateLogicalState(tpf);
            }
        }

        // pull map to cpu
        if (buoyancyBuffer == null) {
            buoyancyBuffer = BufferUtils
                    .createByteBuffer(buoyancyMap.getImage().getWidth() * buoyancyMap.getImage().getHeight()
                            * 3 * (buoyancyMap.getImage().getFormat().getBitsPerPixel() / 8));
        }
        if (buoyancyBuffer != null) {
            buoyancyBuffer.rewind();
            getApplication().getRenderer().readFrameBufferWithFormat(bouyancyFrameBuffer, buoyancyBuffer,
                    processorFormat);
            buoyancyBuffer.rewind();
        }

        // float y = getWaterHeightAt(testCube.getLocalTranslation().x, testCube.getLocalTranslation().z);
        // testCube.setLocalTranslation(testCube.getLocalTranslation().x, y,
        // testCube.getLocalTranslation().z);

        // selectedEnvFrameBuffer = (selectedEnvFrameBuffer + 1) % envFrameBuffers.length;
        // reflectionProcessor.setReflectionBuffer(envFrameBuffers[selectedEnvFrameBuffer]);

    }

    public float getWaterHeightAt(float x, float z) {
        if (buoyancyBuffer == null || buoyancyMap == null) {
            return 0; // Default if buffer not ready
        }

        // Get current ocean center position (which follows camera)
        Vector3f oceanCenter = oceanGeometry.getWorldTranslation();

        // Convert world coordinates to coordinates relative to ocean plane
        float relativeX = x - oceanCenter.x;
        float relativeZ = z - oceanCenter.z;

        int width = buoyancyMap.getImage().getWidth();
        int height = buoyancyMap.getImage().getHeight();
        int xIndex = (int) ((relativeX + GRID_SIZE / 2) / GRID_SIZE * width);
        int zIndex = (int) ((relativeZ + GRID_SIZE / 2) / GRID_SIZE * height);

        // Bounds checking
        if (xIndex < 0 || xIndex >= width || zIndex < 0 || zIndex >= height) {
            return 0; // Out of bounds
        }

        float h = 0;
        if (processorFormat == Format.RGB32F) {
            int byteOffset = (zIndex * width + xIndex) * 3 * 4;
            h = buoyancyBuffer.getFloat(byteOffset);
        } else if (processorFormat == Format.RGB8) {
            int byteOffset = (zIndex * width + xIndex) * 3;
            h = (buoyancyBuffer.get(byteOffset) & 0xFF) / 255f;
        } else {
            throw new UnsupportedOperationException("Buoyancy map format not supported: " + processorFormat);
        }
        h *= WAVE_SCALE.y;
        h += waterHeight;
        return h;

    }

    @Override
    protected void onDisable() {
     

    }

    public void add(BuoyancyControl control) {
        controls.add(control);
    }

    public void add(Spatial spat) {
        BuoyancyControl bc = spat.getControl(BuoyancyControl.class);
        if (bc == null) {
            bc = new BuoyancyControl();
            spat.addControl(bc);

        }
        bc.setAppState(this);

        controls.add(bc);
    }

    public void remove(BuoyancyControl control) {
        controls.remove(control);
    }

    public void remove(Spatial spat) {
        BuoyancyControl bc = spat.getControl(BuoyancyControl.class);
        if (bc != null) {
            controls.remove(bc);
            bc.setAppState(null);

        }
    }
}
