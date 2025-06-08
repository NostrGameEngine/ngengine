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

import com.jme3.asset.AssetManager;
import com.jme3.post.FilterPostProcessor;
import com.jme3.renderer.ViewPort;
import com.jme3.scene.Node;

/**
 * A fragment that provides access to a ViewPort. A ViewPort is an object that contains the camera and scene
 * to be rendered.
 */
public interface MainViewPortFragment extends Fragment {
    /**
     * Get the first scene node of the ViewPort. This is usually the root node of the scene graph.
     *
     * @param vp
     *            the ViewPort instance
     * @return the root node of the scene graph
     * @see ViewPort#getScenes()
     */
    default Node getRootNode(ViewPort vp) {
        return (Node) vp.getScenes().get(0);
    }

    /**
     * Receive a ViewPort instance as soon as it is available. The reference to the ViewPort can be stored and
     * used later in the component logic.
     *
     * @param viewPort
     *            the ViewPort instance
     */
    default void receiveMainViewPort(ViewPort viewPort) {}

    /**
     * Receive a FilterPostProcessor instance as soon as it is available. The reference to the
     * FilterPostProcessor can be stored and used later in the component logic.
     *
     * @param fpp
     *            the FilterPostProcessor instance
     */
    default void receiveMainViewPortFilterPostProcessor(FilterPostProcessor fpp) {}

    /**
     * Configure the filter post processor for the passed viewport. This can be used to dynamically attach or
     * configure filters.
     *
     * @param assetManager
     *            the AssetManager instance to load assets
     * @param fpp
     *            the FilterPostProcessor instance to configure
     */
    default void loadMainViewPortFilterPostprocessor(AssetManager assetManager, FilterPostProcessor fpp) {}

    /**
     * Update the ViewPort with the given time per frame (tpf). This method is called every frame and can be
     * used to get the ViewPort or scene info, the camera, tweak the camera or even update the scene graph.
     *
     * This method is called before {@link LogicFragment#update(float)}.
     *
     * @param viewPort
     *            the ViewPort instance
     * @param tpf
     *            time per frame
     */
    default void updateMainViewPort(ViewPort viewPort, float tpf) {}
}
