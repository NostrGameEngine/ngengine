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

package org.ngengine.components.jme3;

import com.jme3.app.Application;
import com.jme3.app.state.AppStateManager;
import com.jme3.asset.AssetManager;
import com.jme3.audio.AudioRenderer;
import com.jme3.input.InputManager;
import com.jme3.renderer.Camera;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.ViewPort;
import com.jme3.system.AppSettings;

import java.util.logging.Logger;
import org.ngengine.AsyncAssetManager;
import org.ngengine.ViewPortManager;
import org.ngengine.components.AbstractComponentManager;
import org.ngengine.components.ComponentManager;
import org.ngengine.components.actions.ActionsComponentInitializer;
import org.ngengine.config.NGEAppSettings;
import org.ngengine.runner.MainThreadRunner;
import org.ngengine.runner.Runner;
import org.ngengine.store.DataStoreProvider;

/**
 * A component manager that manages components in a JME3 application.
 */
public abstract class Jme3AppComponentManager extends AbstractComponentManager{
    private static final Logger log = Logger.getLogger(Jme3AppComponentManager.class.getName());
    private DataStoreProvider dataStoreProvider;
    private NGEAppSettings settings;
    private Application app;
    private ComponentManager parent;


    @Override
    public Runner getRunner(){
        Runner runner = super.getRunner();
        if(runner != null) return runner;
        return MainThreadRunner.of(this.getApplication());
    }

    @Override
    public NGEAppSettings getSettings(){
        NGEAppSettings settings = super.getSettings();
        if(settings !=null) return settings;
        return this.settings;
    }

    protected void setApplication(Application app){
        this.app = app;
    }

    protected void setSettings(NGEAppSettings settings){
        this.settings = settings;
    }



 

    @Override
    protected void initialize(AbstractComponentManager mng, NGEAppSettings settings){
        super.initialize(mng, settings);
        mng.getInitializers().add(new AppComponentInitializer(app));
        mng.getInitializers().add(new ActionsComponentInitializer());   
        mng.getUpdaters().add(new AppViewPortComponentUpdater(app));
        mng.getUpdaters().add(new AppComponentUpdater(app));
        mng.getLoaders().add(new AppComponentLoader(app));

    }
    
    @Override
    protected void cleanup(){
        initializers.clear();
        updaters.clear();
        loaders.clear();
        super.cleanup();
    }



    protected Application getApplication() {
        if(parent != null && parent instanceof Jme3AppComponentManager){
            return ((Jme3AppComponentManager)parent).getApplication();
        }
        return app;
    }

 
   

    @Override
    public DataStoreProvider getDataStoreProvider() {
        DataStoreProvider dsp = super.getDataStoreProvider();
        if(dsp != null) return dsp;
        if (dataStoreProvider == null) {
            String id = getSettings().getAppId().asBech32();
            if (id == null || id.isEmpty()) {
                id = this.getApplication().getContext().getSettings().getTitle();
            }
            dataStoreProvider = new DataStoreProvider(id, this.getApplication().getAssetManager());
        }
        return dataStoreProvider;
    }

    @Override
    public <T> T getInstanceOf(Class<T> type) {      
        if(type == AudioRenderer.class){
            return type.cast(this.getApplication().getAudioRenderer());
        }
        if (type == InputManager.class) {
            return type.cast(this.getApplication().getInputManager());
        }
        if (type == Application.class) {
            return type.cast(this.getApplication());
        }
        if (type == ComponentManager.class) {
            return type.cast(this);
        }
        if (type == Camera.class) {
            return type.cast(this.getApplication().getCamera());
        }
        if (type == RenderManager.class) {
            return type.cast(this.getApplication().getRenderManager());
        }
        if (type == AppStateManager.class) {
            return type.cast(this.getApplication().getStateManager());
        }
        if (type == DataStoreProvider.class) {
            return type.cast(getDataStoreProvider());
        }
        if (type == ViewPort.class) {
            return type.cast(this.getApplication().getViewPort());
        }
        if (type == AssetManager.class) {
            return type.cast(this.getApplication().getAssetManager());
        }
        if (type == AsyncAssetManager.class) {
            return type.cast(AsyncAssetManager.of(this.getApplication().getAssetManager(), this.getApplication()));
        }
        if (type == ViewPortManager.class) {
            return type.cast( new Jme3ViewPortManager(this.getApplication()));
        }
        if (type==MainThreadRunner.class){
            return type.cast(MainThreadRunner.of(this.getApplication()));
        }
        if(type==AppSettings.class){
            return type.cast(this.getApplication().getContext().getSettings());
        }
        return super.getInstanceOf(type);
    }
}
