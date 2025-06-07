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
 * An interface to update components in the component manager.
 */
public interface ComponentUpdater {
    /**
     * Check if the component can be updated by this updater. This is used to determine if the component is
     * compatible with this updater and can be updated without issues. If the component is not compatible, it
     * will not be updated and the component manager will call the next available updater in the chain.
     *
     * @param componentManager
     *            the component manager
     * @param component
     *            the component to check
     * @return true if the component can be updated, false otherwise
     */
    boolean canUpdate(ComponentManager componentManager, Component component);

    /**
     * Update the component in the given component manager. This method is called every frame to update the
     * component's state.
     *
     * @param componentManager
     *            the component manager
     * @param component
     *            the component to update
     * @param tpf
     *            time per frame, used for time-based updates
     */

    void update(ComponentManager componentManager, Component component, float tpf);

    /**
     * Call the render method for the component in the given component manager. This method is called during
     * the render phase to perform low-level rendering logic for the component.
     *
     * @param componentManager
     *            the component manager
     * @param component
     *            the component to render
     */
    void render(ComponentManager componentManager, Component component);
}
