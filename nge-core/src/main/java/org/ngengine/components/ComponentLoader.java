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
package org.ngengine.components;

/**
 * An interface to load resources and configurations for components in a component manager.
 */

public interface ComponentLoader {
    /**
     * Load the component in the given component manager. This method is called after the component is
     * initialized
     *
     * @param mng
     *            the component manager
     * @param fragment
     *            the component to load
     * @param markReady
     *            a runnable that should be called when the loading is complete
     * @return how many times the markReady callback is expected to be called to mark the complete
     *         loading.
     */
    int load(ComponentManager mng, Component fragment, Runnable markReady);

    /**
     * Frees up resources and configurations for the component in the given component manager.
     *
     * @param mng
     *            the component manager
     * @param fragment
     *            the component to cleanup
     */
    void unload(ComponentManager mng, Component fragment);

    /**
     * Check if the component can be loaded by this loader. This is used to determine if the component is
     * compatible with this loader and can be initialized without issues. If the component is not compatible,
     *  the next available loader in the chain.
     *
     * @param mng
     *            the component manager
     * @param fragment
     *            the component to check
     * @return true if the component can be initialized, false otherwise
     */
    boolean canLoad(ComponentManager mng, Component fragment);
}
