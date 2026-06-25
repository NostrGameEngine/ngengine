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

package ngetests.tests.components;

import java.util.function.Consumer;

import org.ngengine.AsyncAssetManager;
import org.ngengine.Components;
import org.ngengine.ViewPortManager;
import org.ngengine.components.AbstractComponent;
import org.ngengine.components.ComponentManager;
import org.ngengine.components.ReloadableComponent;
import org.ngengine.components.fragments.AsyncAssetLoadingFragment;
import org.ngengine.gui.guix.NLabel;
import org.ngengine.gui.guix.containers.NRow;
import org.ngengine.gui.guix.win.NHud;
import org.ngengine.gui.guix.win.NWindowManagerComponent;
import org.ngengine.store.DataStore;
import com.jme3.environment.EnvironmentProbeControl;
import com.jme3.material.Material;
import com.jme3.material.Materials;
import com.jme3.math.ColorRGBA;
import com.jme3.renderer.ViewPort;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.shape.Box;
import com.jme3.util.SkyFactory;

public class AppComponent extends AbstractComponent implements AsyncAssetLoadingFragment, ReloadableComponent{
    
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
    public void onEnable(ComponentManager mng, boolean firstTime) {
        // the global ViewPortManager is used to access and manage viewports in the application
        ViewPortManager vpm = mng.getInstanceOf(ViewPortManager.class);
        
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
    public void onDisable(ComponentManager mng) {
        // clean up 
        ViewPortManager vpm = mng.getInstanceOf(ViewPortManager.class);
        ViewPort vp = vpm.getMainSceneViewPort();
        Node rootNode = vpm.getRootNode(vp);
        rootNode.detachChild(sky);
        rootNode.removeControl(evp);
        rootNode.detachChild(characterNode);
        hud.close();
        hud = null;        
    }

    @Override
    public void reload() {
        ComponentManager mng = getComponentManager();
        if (mng != null && hud != null) {
            onDisable(mng);
            onEnable(mng, false);
        }
    }



 





 


    
}
