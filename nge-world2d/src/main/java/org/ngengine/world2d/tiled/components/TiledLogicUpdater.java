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

package org.ngengine.world2d.tiled.components;

import java.util.function.Supplier;

import org.ngengine.components.Component;
import org.ngengine.components.ComponentManager;
import org.ngengine.components.runners.ComponentUpdater;

import com.jme3.scene.Spatial;

import org.ngengine.world2d.tiled.components.fragments.TiledEntityLogicFragment;
import org.ngengine.world2d.tiled.components.fragments.TiledEntityRenderFragment;
import org.ngengine.world2d.tiled.core.TiledBase;

/**
 * Per-entry updater that runs tiled logic/render fragments and network ticks.
 */
public class TiledLogicUpdater implements ComponentUpdater {
    private final Supplier<TiledBase> entrySupplier;
    private final Supplier<Spatial> spSupplier;
    public TiledLogicUpdater(Supplier<TiledBase> entrySupplier, Supplier<Spatial> sp) {
        this.entrySupplier = entrySupplier;
        this.spSupplier = sp;
    }

    @Override
    public boolean canUpdate(ComponentManager fragmentManager, Component component) {
        return component instanceof TiledEntityLogicFragment;
    }


    @Override
    public void update(ComponentManager mng, Component component, float tpf) {
        TiledBase entry = entrySupplier.get();
        if (entry == null) {
            return;
        }
        if (component instanceof TiledEntityLogicFragment) {
            TiledEntityLogicFragment fragment = (TiledEntityLogicFragment) component;
            fragment.onTiledEntityLogicUpdate(mng, tpf, entry);
        }
    }

    @Override
    public void render(ComponentManager mng, Component component) {
        TiledBase entry = entrySupplier.get();
        Spatial sp = spSupplier.get();
        if (entry == null) {
            return;
        }
        if (component instanceof TiledEntityRenderFragment) {
            TiledEntityRenderFragment fragment = (TiledEntityRenderFragment) component;
            fragment.onTiledEntryRender(mng, entry, sp);
        }
    }

    @Override
    public void afterRender(ComponentManager componentManager, Component component) {}

    @Override
    public void afterUpdate(ComponentManager componentManager, Component component) {}
}
