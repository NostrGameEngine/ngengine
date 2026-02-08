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

package org.ngengine.components;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;

import com.jme3.export.InputCapsule;
import com.jme3.export.JmeExporter;
import com.jme3.export.JmeImporter;
import com.jme3.export.OutputCapsule;
import com.jme3.export.Savable;

/**
 * Component mount info
 */
public class ComponentMount implements Savable {
    public Component component;
    public boolean enabled;
    public boolean desiredEnabledState;
    public boolean ready;
    public boolean markForRemoval;
    public boolean isNew = true;
    public Object[] deps;
    public final AtomicInteger initialized = new AtomicInteger(Integer.MIN_VALUE); // Integer.MIN_VALUE means not
    public boolean newlyAttached = true;
    public ComponentManager manager;
    // initialized, 0 means ready,
    // >0 means pending
    public final AtomicInteger loaded = new AtomicInteger(Integer.MIN_VALUE); // Integer.MIN_VALUE means not
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