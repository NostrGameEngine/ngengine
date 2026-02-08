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

package org.ngengine.components.jme3;

import org.ngengine.components.Component;
import org.ngengine.components.ComponentManager;
import org.ngengine.components.fragments.SpatialLogicFragment;
import org.ngengine.components.fragments.SpatialRenderFragment;
import org.ngengine.components.runners.ComponentUpdater;

import com.jme3.renderer.RenderManager;

public class SpatialComponentUpdater implements ComponentUpdater {
    private final ComponentManagerControl control;

    public SpatialComponentUpdater(ComponentManagerControl control) {
        this.control = control;
    }

    @Override
    public boolean canUpdate(ComponentManager fragmentManager, Component component) {
        return component instanceof SpatialLogicFragment || component instanceof SpatialRenderFragment;
    }


    @Override
    public void update(ComponentManager mng, Component component, float tpf) {
        if (component instanceof SpatialLogicFragment) {
            SpatialLogicFragment appFragment = (SpatialLogicFragment) component;
            appFragment.updateSpatialLogic(mng, tpf, control.getSpatial());
        }
    }

    @Override
    public void render(ComponentManager mng, Component component) {
        if (component instanceof SpatialRenderFragment) {
            SpatialRenderFragment renderFragment = (SpatialRenderFragment) component;
            renderFragment.updateSpatialRender(mng, (RenderManager)mng.getInstanceOf(RenderManager.class),control.getLastViewPort(), control.getSpatial());
        }
    }
}
