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

package org.ngengine.web.context;

import com.jme3.asset.AssetManager;
import com.jme3.audio.AudioRenderer;
import com.jme3.plugins.json.Json;
import com.jme3.system.JmeSystemDelegate;
import com.jme3.system.Platform;
import com.jme3.system.AppSettings;
import com.jme3.system.JmeContext;
import com.jme3.system.JmeContext.Type;
import com.jme3.util.res.Resources;
import org.ngengine.web.audio.WebAudioRenderer;
import org.ngengine.web.filesystem.WebLocator;
import org.ngengine.web.filesystem.WebResourceLoader;

import java.io.IOException;
import java.io.OutputStream;
import java.net.URL;
import java.nio.ByteBuffer;
import java.util.logging.Logger;

import org.ngengine.web.WebBinds;
import org.ngengine.web.WebBindsAsync;
import org.ngengine.web.json.TeaJSONParser;

public class WebSystem extends JmeSystemDelegate {
    protected final static Logger logger = Logger.getLogger(WebSystem.class.getName());

    public WebSystem() {
        super();    
        System.setProperty("nge-platforms.allowLoopbackInURIs", "true");
 
        // System.out.println(System.getProperty(Resources.PROPERTY_RESOURCE_LOADER_IMPLEMENTATION));
        // System.out.println(System.getProperty(Json.PROPERTY_JSON_PARSER_IMPLEMENTATION));
        // System.out.println(System.getProperty(BufferAllocatorFactory.PROPERTY_BUFFER_ALLOCATOR_IMPLEMENTATION));
        
        // WORKAROUND: teavm is not very happy with reflect at this stage...
        Resources.setResourceLoader(new WebResourceLoader());
        Json.setParser(new TeaJSONParser());
        
        WebBindsAsync.connectNip07Backend();
        WebBindsAsync.connectWebRTCBackend();
        WebBindsAsync.connectClipboardBackend();

    }
    
    @Override
    public void writeImageFile(OutputStream outStream, String format, ByteBuffer imageData, int width, int height) throws IOException {
        logger.warning("Unimplemented method 'writeImageFile'");
    }

    @Override
    public URL getPlatformAssetConfigURL() {
        return null;
     }

    @Override
    public JmeContext newContext(AppSettings settings, Type contextType) {
        WebBinds.helloBinds();
        initialize(settings);
        JmeContext ctx = new WebContext();
        ctx.setSettings(settings);
        return ctx;
    }

    @Override
    public AudioRenderer newAudioRenderer(AppSettings settings) {
        return new WebAudioRenderer();
    }

    @Override
    public void initialize(AppSettings settings) {
        logger.info("Initialize jme web");
    }

    @Override
    public void showSoftKeyboard(boolean show) {
       logger.warning("Unimplemented method 'showSoftKeyboard'");
    }


    public final AssetManager newAssetManager(URL configFile) {
        AssetManager am = super.newAssetManager();
        loadDefaults(am);
        // return new SyncAssetManager(am);
        return am;
   
    }

    private void loadDefaults(AssetManager assetManager) {
        assetManager.registerLocator("/", WebLocator.class);
        assetManager.registerLoader(com.jme3.audio.plugins.WAVLoader.class, "wav");
        assetManager.registerLoader(com.jme3.material.plugins.J3MLoader.class, "j3m", "j3md");
        assetManager.registerLoader(com.jme3.material.plugins.ShaderNodeDefinitionLoader.class, "j3sn");
        assetManager.registerLoader(com.jme3.font.plugins.BitmapFontLoader.class, "fnt");
        assetManager.registerLoader(com.jme3.texture.plugins.DDSLoader.class, "dds");
        assetManager.registerLoader(com.jme3.texture.plugins.PFMLoader.class, "pfm");
        assetManager.registerLoader(com.jme3.texture.plugins.StbImageLoader.class, "jpg", "bmp", "gif", "png", "jpeg", "tga", "psd", "hdr");
        assetManager.registerLoader(com.jme3.export.binary.BinaryLoader.class, "j3o", "j3f");
        assetManager.registerLoader(com.jme3.scene.plugins.OBJLoader.class, "obj");
        assetManager.registerLoader(com.jme3.scene.plugins.MTLLoader.class, "mtl");
        assetManager.registerLoader(com.jme3.shader.plugins.GLSLLoader.class, "vert", "frag", "geom", "tsctrl", "tseval", "glsl", "glsllib");
        assetManager.registerLoader(com.jme3.scene.plugins.gltf.GltfLoader.class, "gltf");
        assetManager.registerLoader(com.jme3.scene.plugins.gltf.BinLoader.class, "bin");
        assetManager.registerLoader(com.jme3.scene.plugins.gltf.GlbLoader.class, "glb");
        assetManager.registerLoader(com.jme3.texture.plugins.WebpImageLoader.class, "webp");
        assetManager.registerLoader(org.ngengine.web.filesystem.WebImageLoader.class, "svg");
        assetManager.registerLoader(com.jme3.audio.plugins.OGGLoader.class, "ogg");
    }

    public final AssetManager newAssetManager() {
        AssetManager am = super.newAssetManager();
        loadDefaults(am);
        return new SyncAssetManager(am);
    }
    
    public Platform getPlatform() {      
        return Platform.Web;      
    }
}
