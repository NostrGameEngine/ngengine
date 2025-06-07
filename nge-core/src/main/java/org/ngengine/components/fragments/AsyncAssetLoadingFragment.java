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
package org.ngengine.components.fragments;

import org.ngengine.AsyncAssetManager;

/**
 * A fragment that can load assets asynchronously using the provided AssetManager. This is useful for
 * components that need to load assets without blocking the main thread.
 */
public interface AsyncAssetLoadingFragment extends Fragment {
    /**
     * This method is called from the asset loader thread and can be used to load and transform assets
     * asynchronously, without blocking the main thread.
     *
     * A component implementing this fragment will stay in "pending" state until the
     * {@link #loadAssetsAsync(AsyncAssetManager)} method completes its execution and they are subsequently
     * enabled by the component manager.
     *
     * <p>
     * <strong>Thread Safety Warning:</strong> This method executes on a background thread. You must not
     * modify the scene graph or access other thread-unsafe objects from this method. Instead, as a rule of
     * thumb, you should only load assets here and pass them to the
     * {@link org.ngengine.components.Component#onEnable(org.ngengine.components.ComponentManager, org.ngengine.components.Runner, org.ngengine.components.DataStoreProvider, boolean, Object)}
     * method using class fields.
     * </p>
     *
     * @param assetManager
     *            the AsyncAssetManager instance to use for loading assets
     */
    void loadAssetsAsync(AsyncAssetManager assetManager);
}
