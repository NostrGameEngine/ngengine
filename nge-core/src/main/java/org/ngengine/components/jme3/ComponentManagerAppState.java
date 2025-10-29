/**
 * Copyright (c) 2025, Nostr Game Engine
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
package org.ngengine.components.jme3;

import com.jme3.app.Application;
import com.jme3.app.state.AppState;
import com.jme3.app.state.AppStateManager;
import com.jme3.app.state.BaseAppState;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.ViewPort;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.control.Control;
import com.jme3.util.SafeArrayList;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.ngengine.ViewPortManager;
import org.ngengine.components.ComponentManager;
import org.ngengine.config.NGEAppSettings;

/**
 * A component manager that manages components in a JME3 application.
 */
public class ComponentManagerAppState extends Jme3ComponentManager implements AppState {

     

     private static final Logger log = Logger.getLogger(BaseAppState.class.getName());

    private boolean initialized;
    private boolean enabled = true;
    private final String id = "ComponentManager";
     private NGEAppSettings settings;

     @Override
    public String getId() {
        return id;
    }

   
    public ComponentManagerAppState(NGEAppSettings settings) {
        this.settings = settings;
    }

   

    @Override
    public final void initialize(AppStateManager stateManager, Application app) {
        log.log(Level.FINEST, "initialize():{0}", this);
        this.initializeComponentManager(app,settings);
        initialized = true;
        if (isEnabled()) {
            log.log(Level.FINEST, "onEnable():{0}", this);
        }
        

    }

    @Override
    public final boolean isInitialized() {
        return initialized;
    }
 
 
  

    @Override
    public final void setEnabled(boolean enabled) {
        if (this.enabled == enabled)
            return;
        this.enabled = enabled;
        if (!isInitialized())
            return;
        if (enabled) {
            log.log(Level.FINEST, "onEnable():{0}", this);
         } else {
            log.log(Level.FINEST, "onDisable():{0}", this);
         }
    }

    @Override
    public final boolean isEnabled() {
        return enabled;
    }

    @Override
    public void stateAttached(AppStateManager stateManager) {
    }

    @Override
    public void stateDetached(AppStateManager stateManager) {
    }

 

    @Override
    public void postRender() {
    }

    /**
     *  Do not call directly: Called by the state manager to terminate this
     *  state post-detachment or during state manager termination.
     *  This implementation calls onDisable() if the state is enabled and
     *  then cleanup(app).
     */
    @Override
    public final void cleanup() {
        log.log(Level.FINEST, "cleanup():{0}", this);

        if (isEnabled()) {
            log.log(Level.FINEST, "onDisable():{0}", this);
        }
         initialized = false;
    }
  

    @Override
    public void update(float tpf) {
        onUpdate(tpf);
        ViewPortManager vpm = getGlobalInstance(ViewPortManager.class);
        for(ViewPort vp : vpm.getSceneViewPorts()){
            SafeArrayList<Spatial> scenes = vp.getScenes();
            for(int i=0;i<scenes.size();i++){
                Spatial s = scenes.get(i);
                if(!(s instanceof Node)) continue;
                
                Node scene = (Node)s;                
                SafeArrayList<Spatial> sps = scene.getUpdateList();
                
                for(int j=0;j<sps.size();j++){
                    Spatial sp = sps.get(j);
                    for(int c=0;c<sp.getNumControls();c++){
                        Control ctrl = sp.getControl(c);
                        if(ctrl instanceof ComponentManagerControl){
                            ((ComponentManagerControl)ctrl).setParent(this);
                        }
                    }
                }
            }
        }
    }

    @Override
    public void render(RenderManager rm) {
        onRender(rm);
    }




    
 

}
