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

package org.ngengine.web.context; 

import com.jme3.asset.AssetEventListener;
import com.jme3.asset.AssetInfo;
import com.jme3.asset.AssetKey;
import com.jme3.asset.AssetLoader;
import com.jme3.asset.AssetLocator;
import com.jme3.asset.AssetManager;
import com.jme3.asset.FilterKey;
import com.jme3.asset.ModelKey;
import com.jme3.asset.TextureKey;
import com.jme3.audio.AudioData;
import com.jme3.audio.AudioKey;
import com.jme3.font.BitmapFont;
import com.jme3.material.Material;
import com.jme3.post.FilterPostProcessor;
import com.jme3.renderer.Caps;
import com.jme3.scene.Spatial;
import com.jme3.shader.ShaderGenerator;
import com.jme3.texture.Texture;
import java.io.InputStream;
import java.util.EnumSet;

/**
 * An asynchronous wrapper for the JME AssetManager that allows loading assets in a separate thread and
 * provides callbacks for when the asset is loaded. This is useful to avoid blocking the main thread during
 * asset loading.
 */
public class SyncAssetManager implements AssetManager {

    protected final AssetManager assetManager;
 



    protected SyncAssetManager(AssetManager assetManager) {
        this.assetManager = assetManager;
    }

   
    

    @Override
    public synchronized <T> T loadAsset(AssetKey<T> key) {
        return assetManager.loadAsset(key);
    }

 

    @Override
    public synchronized Object loadAsset(String name) {
        return assetManager.loadAsset(name);
    }

   
    @Override
    public synchronized Texture loadTexture(TextureKey key) {
        return assetManager.loadTexture(key);
    }



    @Override
    public synchronized Texture loadTexture(String name) {
        return assetManager.loadTexture(name);
    }

   

    @Override
    public synchronized AudioData loadAudio(AudioKey key) {
        return assetManager.loadAudio(key);
    }

 

    @Override
    public synchronized AudioData loadAudio(String name) {
        return assetManager.loadAudio(name);
    }

 

    @Override
    public synchronized Spatial loadModel(ModelKey key) {
        return assetManager.loadModel(key);
    }

  

    @Override
    public synchronized Spatial loadModel(String name) {
        return assetManager.loadModel(name);
    }

  

    @Override
    public synchronized Material loadMaterial(String name) {
        return assetManager.loadMaterial(name);
    }

 

    @Override
    public synchronized BitmapFont loadFont(String name) {
        return assetManager.loadFont(name);
    }



    @Override
    public synchronized FilterPostProcessor loadFilter(FilterKey key) {
        return assetManager.loadFilter(key);
    }


    @Override
    public synchronized FilterPostProcessor loadFilter(String name) {
        return assetManager.loadFilter(name);
    }

   

    @Override
    public synchronized AssetInfo locateAsset(AssetKey<?> key) {
        return assetManager.locateAsset(key);
    }


    @Override
    public synchronized <T> T loadAssetFromStream(AssetKey<T> key, InputStream inputStream) {
        return assetManager.loadAssetFromStream(key, inputStream);
    }


    @Override
    public synchronized void registerLoader(Class<? extends AssetLoader> loaderClass, String... extensions) {
        assetManager.registerLoader(loaderClass, extensions);
    }

    @Override
    public synchronized void unregisterLoader(Class<? extends AssetLoader> loaderClass) {
        assetManager.unregisterLoader(loaderClass);
    }

    @Override
    public synchronized void registerLocator(String rootPath, Class<? extends AssetLocator> locatorClass) {
        assetManager.registerLocator(rootPath, locatorClass);
    }

    @Override
    public synchronized void unregisterLocator(String rootPath, Class<? extends AssetLocator> locatorClass) {
        assetManager.unregisterLocator(rootPath, locatorClass);
    }

    @Override
    public synchronized void addAssetEventListener(AssetEventListener listener) {
        assetManager.addAssetEventListener(listener);
    }

    @Override
    public synchronized void removeAssetEventListener(AssetEventListener listener) {
        assetManager.removeAssetEventListener(listener);
    }

    @Override
    public synchronized void clearAssetEventListeners() {
        assetManager.clearAssetEventListeners();
    }

    @Override
    public synchronized void setShaderGenerator(ShaderGenerator generator) {
        assetManager.setShaderGenerator(generator);
    }

    @Override
    public synchronized ShaderGenerator getShaderGenerator(EnumSet<Caps> caps) {
        return assetManager.getShaderGenerator(caps);
    }

    @Override
    public synchronized <T> T getFromCache(AssetKey<T> key) {
        return assetManager.getFromCache(key);
    }

    @Override
    public synchronized <T> void addToCache(AssetKey<T> key, T asset) {
        assetManager.addToCache(key, asset);
    }

    @Override
    public synchronized <T> boolean deleteFromCache(AssetKey<T> key) {
        return assetManager.deleteFromCache(key);
    }

    @Override
    public synchronized void clearCache() {
        assetManager.clearCache();
    }
}
