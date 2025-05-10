package org.ngengine.demo.soo;

import java.io.IOException;


import com.jme3.app.Application;
import com.jme3.app.state.BaseAppState;
import com.jme3.asset.AssetManager;
import com.jme3.export.InputCapsule;
import com.jme3.export.JmeImporter;
import com.jme3.material.Material;
import com.jme3.material.RenderState.BlendMode;
import com.jme3.math.ColorRGBA;
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
import com.jme3.texture.FrameBuffer;
import com.jme3.texture.Image;
import com.jme3.texture.Texture;
import com.jme3.texture.Texture2D;
import com.jme3.ui.Picture;
import com.jme3.util.BufferUtils;
import com.jme3.util.mikktspace.MikktspaceTangentGenerator;
import com.jme3.texture.FrameBuffer.FrameBufferTarget;
import com.jme3.texture.Texture.WrapMode;

public class OceanAppState extends BaseAppState {
    private static final int GPU_RES = 256; 
    private Material oceanMat;
    private Geometry oceanGeom;

    protected OceanAppState() {

    }

    @Override
    protected void initialize(Application app) {

    }

    @Override
    protected void cleanup(Application app) {

    }

    public Material getMaterial() {
        return oceanMat;
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
    public static Mesh generateGridMesh(int xSamples, int zSamples, float size) {
        Mesh mesh = new Mesh();
        int vertCount = xSamples * zSamples;
        Vector3f[] positions = new Vector3f[vertCount];
        Vector2f[] uvs = new Vector2f[vertCount];
        int[] indices = new int[(xSamples - 1) * (zSamples - 1) * 6];
        Vector3f[] normals = new Vector3f[vertCount];

        // Fill positions & UVs
        int idx = 0;
        for (int z = 0; z < zSamples; z++) {
            for (int x = 0; x < xSamples; x++) {
                float fx = ((float) x / (xSamples - 1) - 0.5f) * size;
                float fz = ((float) z / (zSamples - 1) - 0.5f) * size;
                positions[idx] = new Vector3f(fx, 0, fz);
                uvs[idx] = new Vector2f((float) x / (xSamples - 1), (float) z / (zSamples - 1));
                normals[idx] = new Vector3f(0, 1, 0);
                idx++;
            }
        }

      

        // Fill triangle indices
        int tri = 0;
        for (int z = 0; z < zSamples - 1; z++) {
            for (int x = 0; x < xSamples - 1; x++) {
                int i0 = z * xSamples + x;
                int i1 = i0 + 1;
                int i2 = i0 + xSamples;
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
    FlipBookTexture oceanNormalHeight;
  FlipBookTexture oceanCrest;

    @Override
    protected void onEnable() {
        AssetManager assetManager = getApplication().getAssetManager();
        RenderManager rm = getApplication().getRenderManager();

 

   

        oceanNormalHeight = FlipBookTexture.load(getApplication().getAssetManager(),
                "ibocean/normalHeight", ".png", 64, true);
        oceanCrest = FlipBookTexture.load(getApplication().getAssetManager(),
                "ibocean/crest", ".png", 64, true);
        
        oceanMat = new Material(assetManager, "ibocean/Ocean.j3md");
        Texture2D foamTexture = (Texture2D) assetManager.loadTexture("Common/MatDefs/Water/Textures/foam2.jpg");
        foamTexture.setWrap(WrapMode.Repeat);
        oceanMat.setTexture("FoamTexture", foamTexture);
        oceanNormalHeight.apply(oceanMat, "NormalHeight");

        oceanCrest.apply(oceanMat, "Crest");

        // oceanMat.setInt("N", GPU_RES);
        Node rootNode = (Node) rm.getMainViews().get(0).getScenes().get(0);

        // 1) Generate mesh and geometry
        Mesh oceanMesh = generateGridMesh(GPU_RES, GPU_RES, SIZE);
        oceanGeom = new Geometry("OceanSurface", oceanMesh);
        MikktspaceTangentGenerator.generate(oceanGeom);

        oceanGeom.setMaterial(oceanMat);
        rootNode.attachChild(oceanGeom);

        oceanMat.getAdditionalRenderState().setBlendMode(BlendMode.AlphaSumA);
        oceanGeom.setQueueBucket(Bucket.Transparent);
        oceanGeom.setShadowMode(ShadowMode.Receive);

    }

    float SIZE = 200f;
    float time = 0;
    Vector3f offset = new Vector3f();
    Vector3f scale = new Vector3f();
    Vector3f scale2 = new Vector3f();
    float s1 = 0.03f;
    float s2 = 0.002f;
    @Override
    public void update(float tpf) {
        super.update(tpf);
        time+= tpf*22f;
        int animFrame = (int) ((time) % oceanNormalHeight.getFrameCount());
        oceanNormalHeight.setFrame(animFrame);
        oceanCrest.setFrame(animFrame);

        Vector3f t = oceanGeom.getLocalTranslation();
        Camera cam = getApplication().getCamera();
        t.x=cam.getLocation().getX();
        t.z=cam.getLocation().getZ();
        oceanGeom.setLocalTranslation(t);


        scale.set(SIZE * s1, 4, SIZE * s1);
        oceanMat.setVector3("Scale", scale);

        offset.set(cam.getLocation()).divideLocal(scale);
        oceanMat.setVector3("Offset", offset);
    }

    @Override
    protected void onDisable() {
     

    }
}
