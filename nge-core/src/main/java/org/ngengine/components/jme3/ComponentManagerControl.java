package org.ngengine.components.jme3;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.ngengine.components.Component;
import org.ngengine.config.NGEAppSettings;

import com.jme3.app.Application;
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

public class ComponentManagerControl extends Jme3ComponentManager implements Control, JmeCloneable{
    protected boolean enabled = true;
    protected Spatial spatial;
    protected boolean initialized = false;
    protected final List<Component> componentsPaused = new ArrayList<>();
    protected ViewPort viewPort;
  
    public ComponentManagerControl() {

    }
 
    @Override
    public void attachedToScene() {
        for(Component c:componentsPaused){             
            this.enableComponent(c);            
        }
        this.componentsPaused.clear();
    }

 
    @Override
    public void detachedFromScene() {   
        componentsPaused.clear();
        for(ComponentMount mount : this.componentMounts){
            if(mount.enabled || mount.desiredEnabledState){
                componentsPaused.add(mount.component);
            }            
        }
        for(Component c:componentsPaused){             
            this.disableComponent(c);            
        }
        this.runMountUpdate();
    }
 

    @Override
    protected void initializeComponentManager(Application app, NGEAppSettings settings){        
        super.initializeComponentManager(app, settings);
        updaters.add(new SpatialComponentUpdater(this));
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
            this.initializeComponentManager(getApplication(), getSettings());
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


 


}
