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

package org.ngengine.components;

import java.lang.reflect.InvocationTargetException;

import org.ngengine.runner.Runner;
import org.ngengine.store.DataStoreProvider;

import com.jme3.util.clone.Cloner;

public abstract class AbstractComponent implements Component, ComponentManagerProvider {
    private ComponentManager mng;
    
    
    public AbstractComponent(){
        
    }

    @Override
    public final void onAttached(ComponentManager mng, Runner runner, DataStoreProvider dataStore) {
        this.mng = mng;
        onAttached();
    }

    public final void onDetached(ComponentManager mng, Runner runner, DataStoreProvider dataStore) {
        this.mng = mng;
        onDetached();
    }


    protected void onDetached(){
        
    }

    protected void onAttached(){
        
    } 

    @Override
    public final void onEnable(ComponentManager mng, Runner runner, DataStoreProvider dataStore,
            boolean firstTime) {
        this.mng = mng;
        onEnable(mng, firstTime);
    }

    @Override
    public final void onDisable(ComponentManager mng, Runner runner, DataStoreProvider dataStore) {
        this.mng = mng;
        onDisable(mng);
    }

    protected abstract void onEnable(ComponentManager mng, boolean firstTime);
    protected abstract void onDisable(ComponentManager mng);

    @Override
    public Component newInstance() {
        // try {
        //     return (Component) this.clone();
        // } catch (CloneNotSupportedException e) {
        //     try {
        //         return getClass().getDeclaredConstructor().newInstance();
        //     } catch ( Exception e1) {
        //         throw new RuntimeException("Cannot create new instance of component "+this.getClass().getName(), e1);
        //     }
        // }        
        Cloner cloner = new Cloner();
        return cloner.clone(this);
    }

    @Override
    public final ComponentManager getComponentManager() {
        return mng;
    }

  
    
}
