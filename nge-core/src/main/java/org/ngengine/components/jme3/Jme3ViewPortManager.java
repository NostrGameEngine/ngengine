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
import com.jme3.post.FilterPostProcessor;
import com.jme3.renderer.Camera;
import com.jme3.renderer.ViewPort;
import java.util.Collections;
import java.util.List;
import org.ngengine.ViewPortManager;

public class Jme3ViewPortManager implements ViewPortManager {

    private final Application app;
    private final List<ViewPort> sceneViewPortsRO;

    public Jme3ViewPortManager(Application app) {
        this.app = app;
        this.sceneViewPortsRO = Collections.unmodifiableList(app.getRenderManager().getMainViews());
    }

    @Override
    public ViewPort getMainSceneViewPort() {
        return app.getViewPort();
    }

    @Override
    public ViewPort getGuiViewPort() {
        return app.getGuiViewPort();
    }

    @Override
    public ViewPort createNewGuiViewPort(String name, Camera cam) {
        ViewPort vp = app.getRenderManager().createManagedPostView(name, cam);
        vp.setClearFlags(false, false, false);
        Camera mainCamera = app.getCamera();
        if (mainCamera != null) {
            vp.setRenderTargetSize(mainCamera.getWidth(), mainCamera.getHeight());
        }
        return vp;
    }

    @Override
    public boolean removeGuiViewPort(ViewPort vp) {
        if (vp == null) {
            return false;
        }
        return app.getRenderManager().removePostView(vp);
    }

    @Override
    public List<ViewPort> getSceneViewPorts() {
        return sceneViewPortsRO;
    }

    @Override
    public ViewPort createNewSceneViewPort(String name, Camera cam) {
        return app.getRenderManager().createMainView(name, cam);
    }

    @Override
    public FilterPostProcessor getFilterPostProcessor(ViewPort vp) {
        FilterPostProcessor fpp = Utils.getFilterPostProcessor(
            app.getContext().getSettings(),
            app.getAssetManager(),
            app.getViewPort()
        );
        return fpp;
    }
}
