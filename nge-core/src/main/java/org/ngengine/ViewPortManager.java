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

package org.ngengine;

import com.jme3.post.FilterPostProcessor;
import com.jme3.renderer.Camera;
import com.jme3.renderer.ViewPort;
import com.jme3.scene.Node;
import java.util.List;

/**
 * Get and manage ViewPorts in the application.
 */
public interface ViewPortManager {
    /**
     * Get the main scene ViewPort.
     * @return the main scene ViewPort
     */
    ViewPort getMainSceneViewPort();

    /**
     * Get the GUI ViewPort.
     * @return the GUI ViewPort
     */
    ViewPort getGuiViewPort();

    /**
     * Get all scene ViewPorts (read-only).
     * @return the list of scene ViewPorts
     */
    List<ViewPort> getSceneViewPorts();

    /**
     * Create a new scene ViewPort with the given name and camera.
     * @param name the name of the ViewPort
     * @param cam the camera to use
     * @return the created ViewPort
     */
    ViewPort createNewSceneViewPort(String name, Camera cam);

    /**
     * Get and create if needed a FilterPostProcessor for the given ViewPort.
     * @param vp the ViewPort
     * @return the FilterPostProcessor
     */
    FilterPostProcessor getFilterPostProcessor(ViewPort vp);

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
}
