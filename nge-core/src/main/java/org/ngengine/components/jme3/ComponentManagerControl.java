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

import java.io.IOException;
import java.util.ArrayList;
import org.ngengine.components.AbstractComponentManager;
import org.ngengine.components.Component;
import org.ngengine.components.ComponentMount;
import org.ngengine.config.NGEAppSettings;

import com.jme3.export.InputCapsule;
import com.jme3.export.JmeExporter;
import com.jme3.export.JmeImporter;
import com.jme3.export.OutputCapsule;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.ViewPort;
import com.jme3.scene.Spatial;
import com.jme3.scene.control.Control;
import com.jme3.util.clone.Cloner;
import com.jme3.util.clone.JmeCloneable;

public class ComponentManagerControl extends Jme3AppComponentManager implements Control, JmeCloneable{
    protected boolean enabled = true;
    protected Spatial spatial;
    protected boolean initialized = false;
    protected ViewPort viewPort;
   
    public ComponentManagerControl() {

    }
 
    @Override
    public void attachedToScene() {
        setEnabled(true);
    }

 
    @Override
    public void detachedFromScene() {
        setEnabled(false);      
    }
 

    @Override
    protected void initialize(AbstractComponentManager mng, NGEAppSettings settings){
        super.initialize(mng, settings);
        mng.getUpdaters().add(new SpatialComponentUpdater(this));
    }
 
    @Override
    public void setSpatial(Spatial spatial) {
        if (this.spatial != null && spatial != null && spatial != this.spatial) {
            throw new IllegalStateException("This control has already been added to a Spatial");
        }
        this.spatial = spatial;
    }

    public Spatial getSpatial(){
        return spatial;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isEnabled() {
        return enabled;
    }

 
    @Deprecated
    @Override
    public Control cloneForSpatial(Spatial spatial) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Object jmeClone() {
        ComponentManagerControl cmp =  new ComponentManagerControl();
        cmp.setParent(getParent());
        cmp.spatial = this.spatial;
        cmp.enabled = this.enabled;
        cmp.componentMounts.addAll(this.componentMounts);
        cmp.componentsPaused.addAll(this.componentsPaused);
        return cmp;
    }

    @Override
    public void cloneFields(Cloner cloner, Object original) {
        this.spatial = cloner.clone(spatial);

        ArrayList<ComponentMount> toRemount = new ArrayList<>();
        toRemount.addAll(this.componentMounts);
        
        ArrayList<Component> paused = new ArrayList<>();
        paused.addAll(this.componentsPaused);

        for(ComponentMount mount:toRemount){
            // clone and add the component
            Component clone = mount.component.newInstance();
            this.addComponent(clone, mount.deps);

            if(mount.enabled || mount.desiredEnabledState){
                // set enabled state
                this.enableComponent(clone);
            } else if (paused.contains(mount.component)){
                // or add it to the paused list
                this.componentsPaused.add(clone);
            }
        }
        
    }

    @Override
    public void update(float tpf) {
        if (!enabled || getParent() == null){
            return;
        }

        if(!initialized){
            this.setApplication(getApplication());
            this.setSettings(getSettings());
            initialize();
            initialized = true;
        }
        this.onUpdate(tpf);

     }

    @Override
    public void render(RenderManager rm, ViewPort vp) {
        if (!enabled || getParent() == null)
            return;
        viewPort = vp;
        this.onRender(rm);
    }
    public ViewPort getLastViewPort(){
        return viewPort;
    }

    @Override
    public void write(JmeExporter ex) throws IOException {
        super.write(ex);
        OutputCapsule oc = ex.getCapsule(this);
        oc.write(enabled, "enabled", true);
        oc.write(spatial, "spatial", null);
    }

    @Override
    public void read(JmeImporter im) throws IOException {
        super.read(im);
        InputCapsule ic = im.getCapsule(this);
        enabled = ic.readBoolean("enabled", true);
        spatial = (Spatial) ic.readSavable("spatial", null);
    }


 
    @Override
    public <T> T getInstanceOf(Class<T> type) {      
        if (type == Spatial.class) {
            return type.cast(this.getSpatial());
        }
        return super.getInstanceOf(type);
    }  

}
