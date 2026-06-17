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

package ngetests.tests.world2d.tiled.app;

import com.jme3.app.SimpleApplication;
import com.jme3.input.KeyInput;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.AnalogListener;
import com.jme3.input.controls.KeyTrigger;
import com.jme3.system.AppSettings;
import org.ngengine.world2d.tiled.TmxLoader;
import org.ngengine.world2d.tiled.core.TiledMap;
import org.ngengine.world2d.tiled.core.TiledObjectLayer;
import org.ngengine.world2d.tiled.core.TiledTileLayer;
import org.ngengine.world2d.tiled.core.entity.TiledObjectEntity;
import org.ngengine.world2d.tiled.core.tileset.Tile;
import org.ngengine.world2d.tiled.enums.RenderingMode;
import java.util.logging.Logger;

/**
 * Manual visual test for tiled instanced batching and batch-debug overlays.
 */
public class TestInstancedBatchingDebug extends SimpleApplication {
    private static final Logger logger = Logger.getLogger(TestInstancedBatchingDebug.class.getName());
    private static final String NEXT_MAP = "BATCH_DEBUG_NEXT_MAP";
    private static final String MOVE_LEFT = "BATCH_DEBUG_MOVE_LEFT";
    private static final String MOVE_RIGHT = "BATCH_DEBUG_MOVE_RIGHT";
    private static final String MOVE_UP = "BATCH_DEBUG_MOVE_UP";
    private static final String MOVE_DOWN = "BATCH_DEBUG_MOVE_DOWN";
    private static final float MANUAL_MOVE_SPEED = 160f;
    private static final String[] MAPS = {
            "tmx/InstancingDebug/orthogonal.tmx",
            "tmx/InstancingDebug/isometric.tmx",
            "tmx/InstancingDebug/staggered.tmx",
            "tmx/InstancingDebug/hexagonal-y.tmx",
            "tmx/InstancingDebug/hexagonal-x.tmx"
    };

    private TiledMapAppState tiledMapState;
    private TiledObjectEntity movingObject;
    private TiledObjectEntity toggledObject;
    private float time;
    private int mapIndex;
    private boolean automaticMotion = true;

    @Override
    public void simpleInitApp() {
        TmxLoader.registerLoader(assetManager);

        tiledMapState = new TiledMapAppState();
        stateManager.attach(tiledMapState);
        tiledMapState.initialize(stateManager, this);
        loadDebugMap();

        inputManager.addMapping("TOGGLE_BATCH_OBJECT", new KeyTrigger(KeyInput.KEY_SPACE));
        inputManager.addListener((ActionListener) (name, isPressed, tpf) -> {
            if (isPressed && toggledObject != null) {
                toggledObject.setVisible(!toggledObject.isVisible());
                logger.info("Toggled batch debug object: " + toggledObject.isVisible());
            }
        }, "TOGGLE_BATCH_OBJECT");

        inputManager.addMapping(NEXT_MAP, new KeyTrigger(KeyInput.KEY_N));
        inputManager.addListener((ActionListener) (name, isPressed, tpf) -> {
            if (isPressed) {
                mapIndex = (mapIndex + 1) % MAPS.length;
                automaticMotion = true;
                loadDebugMap();
            }
        }, NEXT_MAP);

        inputManager.addMapping(MOVE_LEFT, new KeyTrigger(KeyInput.KEY_A));
        inputManager.addMapping(MOVE_RIGHT, new KeyTrigger(KeyInput.KEY_D));
        inputManager.addMapping(MOVE_UP, new KeyTrigger(KeyInput.KEY_W));
        inputManager.addMapping(MOVE_DOWN, new KeyTrigger(KeyInput.KEY_S));
        inputManager.addListener((AnalogListener) (name, value, tpf) -> {
            if (movingObject == null) {
                return;
            }
            if (automaticMotion) {
                automaticMotion = false;
                logger.info("Automatic movement disabled. Move the debug tile with WASD.");
            }
            float step = MANUAL_MOVE_SPEED * tpf;
            if (MOVE_LEFT.equals(name)) {
                movingObject.setX(movingObject.getX() - step);
            } else if (MOVE_RIGHT.equals(name)) {
                movingObject.setX(movingObject.getX() + step);
            } else if (MOVE_UP.equals(name)) {
                movingObject.setY(movingObject.getY() - step);
            } else if (MOVE_DOWN.equals(name)) {
                movingObject.setY(movingObject.getY() + step);
            }
        }, MOVE_LEFT, MOVE_RIGHT, MOVE_UP, MOVE_DOWN);

        logger.info("Batch overlay starts ON. Press N for map, B/F8 for overlay, M for rendering mode, SPACE to hide/reinsert one object, WASD to take manual control.");
    }

    private void loadDebugMap() {
        movingObject = null;
        toggledObject = null;
        TiledMap map = (TiledMap) assetManager.loadAsset(MAPS[mapIndex]);
        TiledTileLayer tileLayer = (TiledTileLayer) map.getLayer("Ground");
        Tile tile = tileLayer.getTileAt(0, 0).getTile();

        TiledObjectLayer objectLayer = new TiledObjectLayer(map.getWidth(), map.getHeight());
        objectLayer.setName("Batching debug objects");
        objectLayer.setRenderingMode(RenderingMode.INSTANCED_BATCH_CULLED);
        for (int i = 0; i < 8; i++) {
            TiledObjectEntity object = new TiledObjectEntity(200000 + i, 96 + (i % 4) * 64, 96 + i * 32, tile);
            object.setName("batch debug object " + i);
            objectLayer.add(object);
            if (i == 2) {
                movingObject = object;
            } else if (i == 5) {
                toggledObject = object;
            }
        }
        map.addLayer(objectLayer);

        tiledMapState.setMap(map);
        tiledMapState.moveToPixel(map.getTileWidth() * 3f, map.getTileHeight() * 3f);
        tiledMapState.getMapRenderer().setInstancedObjectBatchHeight(64);
        tiledMapState.getMapRenderer().setBatchDebugEnabled(true);
        logger.info("Loaded batch debug map: " + MAPS[mapIndex]);
    }

    @Override
    public void simpleUpdate(float tpf) {
        time += tpf;
        if (automaticMotion && movingObject != null) {
            movingObject.setX(160 + Math.sin(time * 2.0) * 96.0);
            movingObject.setY(160);
        }
    }


    public static void main(String[] args) {
        AppSettings settings = new AppSettings(true);
        settings.setWidth(1280);
        settings.setHeight(720);
        settings.setSamples(4);

        TestInstancedBatchingDebug app = new TestInstancedBatchingDebug();
        app.setSettings(settings);
        app.start();
    }
}
