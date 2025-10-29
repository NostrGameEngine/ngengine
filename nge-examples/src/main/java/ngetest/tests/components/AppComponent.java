package ngetest.tests.components;

import java.util.function.Consumer;

import org.ngengine.AsyncAssetManager;
import org.ngengine.Components;
import org.ngengine.ViewPortManager;
import org.ngengine.components.Component;
import org.ngengine.components.ComponentManager;
import org.ngengine.components.fragments.AsyncAssetLoadingFragment;
import org.ngengine.components.fragments.InputHandlerFragment;
import org.ngengine.gui.components.NLabel;
import org.ngengine.gui.components.containers.NRow;
import org.ngengine.gui.win.NWindowManagerComponent;
import org.ngengine.gui.win.std.NHud;
import org.ngengine.runner.Runner;
import org.ngengine.store.DataStore;
import org.ngengine.store.DataStoreProvider;

import com.jme3.environment.EnvironmentProbeControl;
import com.jme3.input.KeyInput;
import com.jme3.input.event.JoyAxisEvent;
import com.jme3.input.event.KeyInputEvent;
import com.jme3.input.event.MouseButtonEvent;
import com.jme3.material.Material;
import com.jme3.material.Materials;
import com.jme3.math.ColorRGBA;
import com.jme3.post.FilterPostProcessor;
import com.jme3.post.filters.KHRToneMapFilter;
import com.jme3.renderer.ViewPort;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.shape.Box;
import com.jme3.util.SkyFactory;
import com.jme3.util.SkyFactory.EnvMapType;

public class AppComponent implements Component, AsyncAssetLoadingFragment{
    
    private Spatial sky;
    private EnvironmentProbeControl evp;
    private Node characterNode;
    private NHud hud;

    @Override
    public void loadAssetsAsync(ComponentManager mng, AsyncAssetManager assetManager, DataStore assetCache, Consumer<Object> preload){
        // load resources
        sky = SkyFactory.createSky(assetManager, "Textures/Sky/Bright/BrightSky.dds",  SkyFactory.EnvMapType.CubeMap);
        evp = new EnvironmentProbeControl(assetManager, 256);

        // Tag sky for environment baking
        EnvironmentProbeControl.tagGlobal(sky);

        // load character model
        characterNode = new Node("CharacterNode");
        Geometry characterGeom = new Geometry("MyCharacter", new Box(1f,1f,1f));
        characterNode.attachChild(characterGeom);
        Components.mount(characterNode, new SpatialComponent()).enable();

        // set up material for character
        Material characterMat = new Material(assetManager, Materials.PBR);
        characterMat.setColor("BaseColor", ColorRGBA.White);
        characterMat.setFloat("Metallic", 1.0f);
        characterMat.setFloat("Roughness", 0.0f);
        characterGeom.setMaterial(characterMat);       

    }


   
 

    @Override
    public void onEnable(ComponentManager mng, Runner runner, DataStoreProvider dataStore, boolean firstTime) {
        // the global ViewPortManager is used to access and manage viewports in the application
        ViewPortManager vpm = mng.getGlobalInstance(ViewPortManager.class);
        
        // The main viewport represent the primary view on the 3d scene
        ViewPort vp = vpm.getMainSceneViewPort();

        // The rootNode of the scene
        Node rootNode = vpm.getRootNode(vp);

        // Compose the scene
        rootNode.attachChild(sky);
        rootNode.addControl(evp);
        rootNode.attachChild(characterNode);     

        // show a simple hud
        NWindowManagerComponent windowManager = mng.getComponent(NWindowManagerComponent.class);
        hud = windowManager.showWindow(NHud.class);
        NRow topRow = hud.getTop();
        NLabel label = new NLabel("Click around to move the cube");
        topRow.addChild(label);            
    
    }


   

    @Override
    public void onDisable(ComponentManager mng, Runner runner, DataStoreProvider dataStore) {
        // clean up 
        ViewPortManager vpm = mng.getGlobalInstance(ViewPortManager.class);
        ViewPort vp = vpm.getMainSceneViewPort();
        Node rootNode = vpm.getRootNode(vp);
        rootNode.detachChild(sky);
        rootNode.removeControl(evp);
        rootNode.detachChild(characterNode);
        hud.close();
        hud = null;        
    }





    @Override
    public Component newInstance() {
        return new AppComponent();
    }



    
}
