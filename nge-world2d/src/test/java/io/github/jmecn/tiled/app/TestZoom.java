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

package io.github.jmecn.tiled.app;

import com.jme3.app.SimpleApplication;
import com.jme3.asset.plugins.FileLocator;
import com.jme3.input.KeyInput;
import com.jme3.input.MouseInput;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.KeyTrigger;
import com.jme3.input.controls.MouseButtonTrigger;
import com.jme3.math.Vector2f;
import com.jme3.system.AppSettings;
import io.github.jmecn.tiled.TmxLoader;
import io.github.jmecn.tiled.core.TiledMap;
import io.github.jmecn.tiled.enums.ZoomMode;

/**
 * Test zoom mode
 * @author yanmaoyuan
 *
 */
public class TestZoom extends SimpleApplication {
    private TiledMapAppState tiledMapState;

    public void click() {
        Vector2f cursor = inputManager.getCursorPosition();

        System.out.println("Click! ======");
        System.out.println("cursor tile: " + tiledMapState.getCursorTileCoordinate(cursor));
        System.out.println("cursor pixel: " + tiledMapState.getCursorPixelCoordinate(cursor));
        System.out.println("cursor screen: " + cursor);
        System.out.println("center tile: " + tiledMapState.getCameraTileCoordinate());
        System.out.println("center pixel: " + tiledMapState.getCameraPixelCoordinate());
        System.out.println("center screen: " + tiledMapState.getCameraScreenCoordinate());
        System.out.println("map scare: " + tiledMapState.getMapScale());
    }

    public void initInput() {
        inputManager.setCursorVisible(true);

        inputManager.addMapping("CLICK", new MouseButtonTrigger(MouseInput.BUTTON_LEFT));
        inputManager.addListener((ActionListener) (name, isPressed, tpf) -> {
            if (isPressed) {
                click();
                tiledMapState.zoom(1.1f);
            }
        }, "CLICK");

        inputManager.addMapping("ZOOM_MODE_MAP", new KeyTrigger(KeyInput.KEY_1));
        inputManager.addListener((ActionListener) (name, isPressed, tpf) -> {
            System.out.println("Set Zoom Mode to MAP");
            tiledMapState.setZoomMode(ZoomMode.MAP);
        }, "ZOOM_MODE_MAP");

        inputManager.addMapping("ZOOM_MODE_CAMERA", new KeyTrigger(KeyInput.KEY_2));
        inputManager.addListener((ActionListener) (name, isPressed, tpf) -> {
            System.out.println("Set Zoom Mode to CAMERA");
            tiledMapState.setZoomMode(ZoomMode.CAMERA);
        }, "ZOOM_MODE_CAMERA");

    }

    @Override
    public void simpleInitApp() {
        TmxLoader.registerLoader(assetManager);

        TiledMap tiledMap = (TiledMap) assetManager.loadAsset("tmx/Isometric/01.tmx");
        assert tiledMap != null;

        tiledMapState = new TiledMapAppState();
        stateManager.attach(tiledMapState);
        tiledMapState.initialize(stateManager, this);

        tiledMapState.setMap(tiledMap);

        initInput();
    }

    public static void main(String[] args) {
        AppSettings settings = new AppSettings(true);
        settings.setWidth(1280);
        settings.setHeight(720);
        settings.setSamples(4);

        TestZoom app = new TestZoom();
        app.setSettings(settings);
        app.start();
    }
}