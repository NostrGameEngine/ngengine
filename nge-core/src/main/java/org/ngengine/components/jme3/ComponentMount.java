package org.ngengine.components.jme3;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;

import org.ngengine.components.Component;
import org.ngengine.components.ComponentManager;

import com.jme3.export.InputCapsule;
import com.jme3.export.JmeExporter;
import com.jme3.export.JmeImporter;
import com.jme3.export.OutputCapsule;
import com.jme3.export.Savable;

/**
 * Component mount info
 */
class ComponentMount implements Savable {
    Component component;
    boolean enabled;
    boolean desiredEnabledState;
    boolean ready;
    boolean markForRemoval;
    boolean isNew = true;
    Object[] deps;
    final AtomicInteger initialized = new AtomicInteger(Integer.MIN_VALUE); // Integer.MIN_VALUE means not
    boolean newlyAttached = true;
    ComponentManager manager;
    // initialized, 0 means ready,
    // >0 means pending
    final AtomicInteger loaded = new AtomicInteger(Integer.MIN_VALUE); // Integer.MIN_VALUE means not
    // loaded, 0 means ready,
    // >0 means pending

    public void enable(){
        this.manager.enableComponent(component);
    }

    public void disable(){
        this.manager.disableComponent(component);
    }

    @Override
    public void write(JmeExporter ex) throws IOException {
        OutputCapsule capsule = ex.getCapsule(this);
        capsule.write(component, "component", null);
        capsule.write(desiredEnabledState, "desiredEnabledState", false);
        capsule.write(ready, "ready", false);
        capsule.write(markForRemoval, "markForRemoval", false);
        capsule.write(isNew, "isNew", true);
        capsule.write(deps.length, "ndeps", 0);
        for (int i = 0; i < deps.length; i++) {
            Object dep = deps[i];
            if(dep instanceof Savable){
                capsule.write(0, "dep_type_" + i, 0); // 0 = Savable
                capsule.write((Savable)dep, "dep_value_" + i, null);
            } else if(dep instanceof String){
                capsule.write(1, "dep_type_" + i, 0); // 1 = String
                capsule.write((String)dep, "dep_value_" + i, null);
            } else if(dep instanceof Class<?>){
                capsule.write(2, "dep_type_" + i, 0); // 2 = Class
                capsule.write(((Class<?>)dep).getName(), "dep_value_" + i, null);
            } else {
                throw new IOException("Unsupported dep type: " + dep.getClass().getName());
            }
        }
    }

    @Override
    public void read(JmeImporter im) throws IOException {
        InputCapsule capsule = im.getCapsule(this);
        component = (Component) capsule.readSavable("component", null);
        desiredEnabledState = capsule.readBoolean("desiredEnabledState", false);
        ready = capsule.readBoolean("ready", false);
        markForRemoval = capsule.readBoolean("markForRemoval", false);
        isNew = capsule.readBoolean("isNew", true);
        int ndeps = capsule.readInt("ndeps", 0);
        deps = new Object[ndeps];
        for (int i = 0; i < ndeps; i++) {
            int depType = capsule.readInt("dep_type_" + i, 0);
            switch (depType) {
                case 0: // Savable
                    deps[i] = capsule.readSavable("dep_value_" + i, null);
                    break;
                case 1: // String
                    deps[i] = capsule.readString("dep_value_" + i, null);
                    break;
                case 2: // Class
                    String className = capsule.readString("dep_value_" + i, null);
                    try {
                        deps[i] = Class.forName(className);
                    } catch (ClassNotFoundException e) {
                        throw new IOException("Class not found: " + className, e);
                    }
                    break;
                default:
                    throw new IOException("Unknown dep type: " + depType);
            }
        }
        initialized.set(Integer.MIN_VALUE);
        loaded.set(Integer.MIN_VALUE);
        isNew = true;
        enabled = false;
    }

}