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
 * the BSD 3-Clause License. The original jMonkeyEngine license is as follows:
 */
package org.ngengine;

import com.jme3.app.Application;
import com.jme3.app.SimpleApplication;
import com.jme3.app.state.BaseAppState;
import com.jme3.asset.AssetManager;
import com.jme3.input.InputManager;
import com.jme3.input.KeyInput;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.KeyTrigger;
import com.jme3.material.MatParam;
import com.jme3.material.Material;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.RendererException;
import com.jme3.scene.Geometry;
import com.jme3.scene.Spatial;
import com.jme3.scene.shape.Box;
import com.jme3.util.MaterialDebugAppState;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

public class DevMode extends BaseAppState implements ActionListener {

    private static Map<Object, Runnable> onReloadMap = new WeakHashMap<>();
    private static ArrayList<WeakReference<Spatial>> reloadableSpatials = new ArrayList<>();
    private static ArrayList<WeakReference<Material>> reloadableMaterials = new ArrayList<>();
    private static volatile boolean needsReload = false;

    public static void registerReloadCallback(Object ref, Runnable callback) {
        onReloadMap.put(ref, callback);
    }

    public static void registerForReload(Spatial spatial) {
        reloadableSpatials.add(new WeakReference<>(spatial));
    }

    public static void registerForReload(Material material) {
        reloadableMaterials.add(new WeakReference<>(material));
    }

    public static void reload() {
        needsReload = true;
    }

    @Override
    protected void initialize(Application app) {}

    @Override
    protected void cleanup(Application app) {}

    @Override
    public void update(float tpf) {
        super.update(tpf);
        if (needsReload) {
            needsReload = false;
            AssetManager assetManager = getApplication().getAssetManager();
            RenderManager renderManager = getApplication().getRenderManager();
            for (Runnable callback : onReloadMap.values()) {
                try {
                    callback.run();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            Iterator<WeakReference<Spatial>> it = reloadableSpatials.iterator();
            while (it.hasNext()) {
                try {
                    WeakReference<Spatial> ref = it.next();
                    Spatial spatial = ref.get();
                    if (spatial == null) {
                        it.remove();
                        continue;
                    }
                    spatial.depthFirstTraversal(sx -> {
                        if (sx instanceof Geometry) {
                            Geometry geom = (Geometry) sx;
                            Material mat = geom.getMaterial();
                            if (mat != null) {
                                reloadMaterial(assetManager, renderManager, mat);
                            }
                        }
                    });
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            Iterator<WeakReference<Material>> it2 = reloadableMaterials.iterator();
            while (it2.hasNext()) {
                try {
                    WeakReference<Material> ref = it2.next();
                    Material mat = ref.get();
                    if (mat == null) {
                        it2.remove();
                        continue;
                    }

                    reloadMaterial(assetManager, renderManager, mat);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    @Override
    protected void onEnable() {
        InputManager im = getApplication().getInputManager();
        im.addMapping("DEVMODE_RELOAD", new KeyTrigger(KeyInput.KEY_F5));
        im.addMapping("TOGGLE_MOUSE_CURSOR", new KeyTrigger(KeyInput.KEY_F6));
        im.addMapping("TOGGLE_FLYCAMERA", new KeyTrigger(KeyInput.KEY_F7));

        im.addListener(this, "DEVMODE_RELOAD");
        im.addListener(this, "TOGGLE_MOUSE_CURSOR");
        im.addListener(this, "TOGGLE_FLYCAMERA");

        registerReloadCallback(
            this,
            () -> {
                getApplication().getAssetManager().clearCache();
            }
        );
    }

    @Override
    protected void onDisable() {
        InputManager im = getApplication().getInputManager();
        im.removeListener(this);
        im.deleteMapping("DEVMODE_RELOAD");
    }

    @Override
    public void onAction(String name, boolean isPressed, float tpf) {
        if (name.equals("DEVMODE_RELOAD") && isPressed) {
            reload();
        }
        if (name.equals("TOGGLE_MOUSE_CURSOR") && isPressed) {
            Application app = getApplication();
            if (app.getInputManager().isCursorVisible()) {
                app.getInputManager().setCursorVisible(false);
            } else {
                app.getInputManager().setCursorVisible(true);
            }
        }
        if (name.equals("TOGGLE_FLYCAMERA") && isPressed) {
            Application app = getApplication();
            if (app instanceof SimpleApplication) {
                SimpleApplication simpleApp = (SimpleApplication) app;
                if (simpleApp.getFlyByCamera().isEnabled()) {
                    simpleApp.getFlyByCamera().setEnabled(false);
                } else {
                    simpleApp.getFlyByCamera().setEnabled(true);
                }
            }
        }
    }

    public static Material reloadMaterial(AssetManager assetManager, RenderManager renderManager, Material mat) {
        // clear the entire cache, there might be more clever things to do, like
        // clearing only the matdef, and the associated shaders.
        assetManager.clearCache();

        // creating a dummy mat with the mat def of the mat to reload
        // Force the reloading of the asset, otherwise the new shader code will not be applied.
        Material dummy = new Material(assetManager, mat.getMaterialDef().getAssetName());

        for (MatParam matParam : mat.getParams()) {
            dummy.setParam(matParam.getName(), matParam.getVarType(), matParam.getValue());
        }

        dummy.getAdditionalRenderState().set(mat.getAdditionalRenderState());

        // creating a dummy geom and assigning the dummy material to it
        Geometry dummyGeom = new Geometry("dummyGeom", new Box(1f, 1f, 1f));
        dummyGeom.setMaterial(dummy);

        try {
            // preloading the dummyGeom, this call will compile the shader again
            renderManager.preloadScene(dummyGeom);
        } catch (RendererException e) {
            // compilation error, the shader code will be output to the console
            // the following code will output the error
            Logger.getLogger(MaterialDebugAppState.class.getName()).log(Level.SEVERE, e.getMessage());
            return null;
        }

        Logger.getLogger(MaterialDebugAppState.class.getName()).log(Level.INFO, "Material successfully reloaded");
        return dummy;
    }
}
