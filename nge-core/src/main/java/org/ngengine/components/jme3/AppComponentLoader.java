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
package org.ngengine.components.jme3;

import com.jme3.app.Application;
import com.jme3.post.FilterPostProcessor;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.ngengine.AsyncAssetManager;
import org.ngengine.components.Component;
import org.ngengine.components.ComponentLoader;
import org.ngengine.components.ComponentManager;
import org.ngengine.components.fragments.AssetLoadingFragment;
import org.ngengine.components.fragments.AsyncAssetLoadingFragment;
import org.ngengine.components.fragments.ViewPortFragment;

/**
 * load components by connecting them to JME3 application resources.
 */
public class AppComponentLoader implements ComponentLoader {

    private static final Logger log = Logger.getLogger(AppComponentInitializer.class.getName());

    private final Application app;
    private final AsyncAssetManager assetManager;

    public AppComponentLoader(Application app) {
        this.app = app;
        this.assetManager = AsyncAssetManager.of(app.getAssetManager(), app);
    }

    @Override
    public int load(ComponentManager mng, Component fragment, Runnable markReady) {
        int i = 0;

        if (fragment instanceof AssetLoadingFragment) {
            i++;
            AssetLoadingFragment f = (AssetLoadingFragment) fragment;
            f.loadAssets(assetManager);
            markReady.run();
        }
        if ((fragment instanceof AsyncAssetLoadingFragment)) {
            i++;
            AsyncAssetLoadingFragment f = (AsyncAssetLoadingFragment) fragment;
            assetManager.runInLoaderThread(
                am -> {
                    f.loadAssetsAsync(assetManager);
                    return null;
                },
                (d, err) -> {
                    if (err != null) {
                        log.log(
                            Level.SEVERE,
                            "Error during async asset loading in fragment: " + f.getClass().getSimpleName(),
                            err
                        );
                    }
                    markReady.run();
                }
            );
        }

        if (fragment instanceof ViewPortFragment) {
            i++;
            ViewPortFragment f = (ViewPortFragment) fragment;
            FilterPostProcessor fpp = Utils.getFilterPostProcessor(
                app.getContext().getSettings(),
                assetManager,
                app.getViewPort()
            );
            f.loadViewPortFilterPostprocessor(assetManager, fpp);
            markReady.run();
        }

        return i;
    }

    @Override
    public boolean canLoad(ComponentManager mng, Component fragment) {
        return (
            fragment instanceof AsyncAssetLoadingFragment ||
            fragment instanceof AssetLoadingFragment ||
            fragment instanceof ViewPortFragment
        );
    }

    @Override
    public void unload(ComponentManager mng, Component fragment) {}
}
