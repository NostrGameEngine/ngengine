package jme3test.texture;
import com.jme3.app.LostFocusBehavior;
import com.jme3.app.SimpleApplication;
import com.jme3.asset.AssetManager;
import com.jme3.input.KeyInput;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.KeyTrigger;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.jme3.renderer.Limits;
import com.jme3.renderer.TextureUnitException;
import com.jme3.scene.Geometry;
import com.jme3.scene.shape.RectangleMesh;
import com.jme3.system.AppSettings;
import com.jme3.terrain.noise.Color;
import com.jme3.texture.Image;
import com.jme3.texture.Image.Format;
import com.jme3.texture.Texture;
import com.jme3.texture.Texture2D;
import com.jme3.texture.image.ColorSpace;
import com.jme3.texture.image.ImageRaster;
import com.jme3.util.BufferUtils;
public class TestAsyncTexture  extends SimpleApplication  {
    private boolean withAsyncTexture = true;
    private Material mat;
    @Override
    public void simpleInitApp() {
 
        flyCam.setDragToRotate(true);
        flyCam.setMoveSpeed(100);
        cam.setLocation(new Vector3f(197.02617f, 4.6769195f, -194.89545f));
        cam.setRotation(new Quaternion(0.07921988f, 0.8992258f, -0.18292196f, 0.38943136f));

 RectangleMesh rm = new RectangleMesh(
                    new Vector3f(-500, 0, 500),
                    new Vector3f(500, 0, 500),
                    new Vector3f(-500, 0, -500));
            rm.scaleTextureCoordinates(new Vector2f(1000, 1000));
            Geometry geom = new Geometry("rectangle", rm);

         mat = createCheckerBoardMaterial(assetManager);
    geom.setMaterial(mat);
            rootNode.attachChild(geom);
    }
    
    private static Material createCheckerBoardMaterial(AssetManager assetManager) {
        Material mat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
    
        return mat;
    }
    
    private static Texture2D createBigTexture2d() {
        int size = 4096;
        Image image = new Image(Format.RGBA8, size, size, BufferUtils.createByteBuffer(size * size * 4), ColorSpace.sRGB);
        
      
        Texture2D tex = new Texture2D(image);
        tex.setMagFilter(Texture.MagFilter.Nearest);
        tex.setMinFilter(Texture.MinFilter.NearestNoMipMaps);
        tex.setWrap(Texture.WrapMode.Repeat);
        return tex;
    }


    int initFrames = 0;
    boolean creating = false;

    @Override
    public void simpleUpdate(float tpf) {
        super.simpleUpdate(tpf);
        
        initFrames++;
        if(initFrames < 2) return; 
        
        if(tpf>1f/50) System.out.println("Frame drop!");
    
            
        if(!creating){
            creating = true;
            new Thread(()->{
                System.out.println("Creating texture in thread: " + Thread.currentThread().getName());
                Texture2D tx = createBigTexture2d();
                if(withAsyncTexture) {
                    tx.getImage().setAsync(true);
                }
                System.out.println("Enqueuing texture swap");
                enqueue(()->{
                    System.out.println("Preloading texture for swap");
                    try {
                        renderManager.preload(tx);
                    } catch (TextureUnitException e) {
                        e.printStackTrace();
                    }
                    enqueue(()->{
                        enqueue(()->{
                            System.out.println("Swapping texture");
                            mat.setTexture("ColorMap", tx);
                            creating = false;
                        });
                    });
                });
                
            }).start();
        }
          
        
    }

    public static void main(String[] args) {
        AppSettings settings = new AppSettings(true);
        settings.setFrameRate(60);
        settings.setX11PlatformPreferred(true);
        TestAsyncTexture app = new TestAsyncTexture();
        app.setLostFocusBehavior(LostFocusBehavior.Disabled);
        app.setSettings(settings);
        app.start();
    }
}
