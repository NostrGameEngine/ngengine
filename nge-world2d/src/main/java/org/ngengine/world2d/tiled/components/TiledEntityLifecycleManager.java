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

package org.ngengine.world2d.tiled.components;

import java.util.Map;
import java.util.WeakHashMap;
import java.util.function.Supplier;

import org.ngengine.components.Component;
import org.ngengine.components.ComponentManager;
import org.ngengine.components.fragments.Fragment;
import org.ngengine.components.runners.ComponentInitializer;
import org.ngengine.components.runners.ComponentUpdater;

import com.jme3.renderer.ViewPort;
import com.jme3.scene.Spatial;

import org.ngengine.world2d.tiled.components.fragments.TiledEntityLifecycleFragment;
import org.ngengine.world2d.tiled.core.TiledBase;

public class TiledEntityLifecycleManager implements ComponentUpdater, ComponentInitializer {
    private final Supplier<TiledBase> entrySupplier;
    private final Map<Fragment, TiledBase> initializedEntries = new WeakHashMap<>();

    public TiledEntityLifecycleManager(Supplier<TiledBase> entrySupplier) {
        this.entrySupplier = entrySupplier;
    }

    public boolean canUpdate(ComponentManager componentManager, Component fragment){
         TiledBase entry = entrySupplier.get();
        if (entry == null) return false;
        return fragment instanceof TiledEntityLifecycleFragment;
    }

     @Override
    public boolean canInitialize(ComponentManager mng, Component fragment) {
        return canUpdate(mng, fragment);
    }
    
    public void update(ComponentManager componentManager, Component component, float tpf){
        TiledBase entry = entrySupplier.get();
        if (component instanceof TiledEntityLifecycleFragment){
            TiledBase oldEntry =  initializedEntries.get((Fragment)component);
            if (entry == oldEntry) return;
            TiledEntityLifecycleFragment lifecycleFragment = (TiledEntityLifecycleFragment) component;
            if (oldEntry!=null){
                lifecycleFragment.onTiledEntityCleanup(componentManager, oldEntry);        
                initializedEntries.remove((Fragment) component);
            }
            if(entry!=null){
                lifecycleFragment.onTiledEntityInitialize(componentManager, entry);
                initializedEntries.put((Fragment) component, entry);
            }
        }
    }

    public void render(ComponentManager componentManager, Component component){

    }
   

    @Override
    public void cleanup(ComponentManager mng, Component fragment) {
        if (fragment instanceof TiledEntityLifecycleFragment) {
            TiledBase initializedEntry = initializedEntries.remove((Fragment) fragment);
            if (initializedEntry == null) {
                return;
            }
            TiledEntityLifecycleFragment lifecycleFragment = (TiledEntityLifecycleFragment) fragment;
            lifecycleFragment.onTiledEntityCleanup(mng, initializedEntry);
        }
    }

    @Override
    public int initialize(ComponentManager mng, Component fragment, Runnable markReady) {
        TiledBase entry = entrySupplier.get();
        if (entry == null) return 0;
        
        if (fragment instanceof TiledEntityLifecycleFragment) {
            TiledEntityLifecycleFragment lifecycleFragment = (TiledEntityLifecycleFragment) fragment;
            lifecycleFragment.onTiledEntityInitialize(mng, entry);
            initializedEntries.put((Fragment) fragment, entry);
            markReady.run();
            return 1;
        }
        return 0;
    } 

   

 
}
