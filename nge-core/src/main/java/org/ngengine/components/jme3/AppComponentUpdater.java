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
import org.ngengine.components.Component;
import org.ngengine.components.ComponentManager;
import org.ngengine.components.ComponentUpdater;
import org.ngengine.components.fragments.LogicFragment;
import org.ngengine.components.fragments.RenderFragment;

/**
 * Updates components using JME3 application resources.
 */
public class AppComponentUpdater implements ComponentUpdater {

    private final Application app;

    public AppComponentUpdater(Application app) {
        this.app = app;
    }

    @Override
    public boolean canUpdate(ComponentManager fragmentManager, Component component) {
        return component instanceof LogicFragment || component instanceof RenderFragment;
    }

    @Override
    public void update(ComponentManager fragmentManager, Component component, float tpf) {
        if (component instanceof LogicFragment) {
            LogicFragment appFragment = (LogicFragment) component;
            appFragment.updateAppLogic(tpf);
        }
    }

    @Override
    public void render(ComponentManager fragmentManager, Component component) {
        if (component instanceof RenderFragment) {
            RenderFragment renderFragment = (RenderFragment) component;
            renderFragment.updateRender(app.getRenderManager());
        }
    }
}
