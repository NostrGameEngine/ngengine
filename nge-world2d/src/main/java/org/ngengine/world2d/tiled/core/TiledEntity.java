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

package org.ngengine.world2d.tiled.core;

import java.math.BigInteger;

import org.ngengine.components.ComponentManagerProvider;

import org.ngengine.world2d.tiled.components.TiledComponentManager;
import org.ngengine.world2d.tiled.components.TiledObjectSyncComponent;
import org.ngengine.world2d.tiled.components.TiledComponentReflectionMounting;
import org.ngengine.world2d.tiled.core.entity.TiledObjectEntity;

/**
 * An entry that can be placed on a map
 * @author Riccardo Balbo
 */
public abstract class TiledEntity extends TiledBase implements ComponentManagerProvider {
    private TiledComponentManager componentManager;

    @Override
    public TiledComponentManager getComponentManager() {
        if(componentManager==null){
            componentManager = new TiledComponentManager();
            if (this instanceof TiledObjectEntity) {
                String syncComponentClass = null;
                Object syncComponentProp = getProperty("net.sync.component");
                if (syncComponentProp != null) {
                    syncComponentClass = String.valueOf(syncComponentProp).trim();
                }
                if (syncComponentClass != null && !syncComponentClass.isEmpty()) {
                    if (TiledComponentReflectionMounting.mountByClassName(componentManager, syncComponentClass, this) == null) {
                        componentManager.addComponent(new TiledObjectSyncComponent());
                        componentManager.enableComponent(TiledObjectSyncComponent.class);
                    }
                } else {
                    componentManager.addComponent(new TiledObjectSyncComponent());
                    componentManager.enableComponent(TiledObjectSyncComponent.class);
                }
            }
            TiledComponentReflectionMounting.mountFromProperty(this, componentManager);
            TiledComponentReflectionMounting.mountBuiltInsFromProperties(this, componentManager);
        }
        return componentManager;
    }


    public abstract void removeFromLayer();
    protected void detached(){
        if(componentManager!=null){
            componentManager.notifyEntityDetached(this);
            componentManager.setEnabled(false);
        }
    }

    protected void attached(){
        if(componentManager!=null){
            componentManager.setEnabled(true);
        }
    }

  
    public abstract double getHeight();
    public abstract double getWidth();
    public abstract double getY();
    public abstract double getX();
    public abstract String getClazz();
    public abstract BigInteger getId();
}
