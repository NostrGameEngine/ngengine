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
package org.ngengine.world2d.player;

import org.ngengine.components.AbstractComponent;
import org.ngengine.components.ComponentManager;
import org.ngengine.components.fragments.LogicFragment;
import org.ngengine.network.components.NetcodeManagerComponent;
import org.ngengine.player.Player;
import org.ngengine.world2d.tiled.core.entity.TiledObjectEntity;

/**
 * Resolves the current peer's player object and exposes lifecycle hooks for
 * local-only input, camera, audio listener, and HUD components.
 */
public class TiledLocalPlayerComponent extends AbstractComponent implements LogicFragment {
    private TiledObjectEntity playerEntity;

    public final TiledObjectEntity getPlayerEntity() {
        return playerEntity;
    }

    @Override
    protected void onEnable(ComponentManager manager, boolean firstTime) {
    }

    @Override
    protected void onDisable(ComponentManager manager) {
        clearLocalPlayer();
    }

    @Override
    public final void updateAppLogic(ComponentManager manager, float tpf) {
        if (playerEntity != null && playerEntity.getObjectGroup() == null) {
            clearLocalPlayer();
        }
        if (playerEntity != null) {
            return;
        }
        NetcodeManagerComponent netcode = getInstanceOf(NetcodeManagerComponent.class);
        if (netcode == null || !netcode.isNetworkSessionActive()) {
            return;
        }
        Player localPlayer = netcode.getLocalPlayer();
        TiledPlayerManagerComponent players =
            getInstanceOf(TiledPlayerManagerComponent.class);
        if (localPlayer == null || players == null) {
            return;
        }
        TiledObjectEntity entity = players.ensurePlayerSpawned(localPlayer);
        if (entity == null) {
            return;
        }
        playerEntity = entity;
        onLocalPlayerReady(entity);
    }

    /** Called once after a local player object is available. */
    protected void onLocalPlayerReady(TiledObjectEntity entity) {
    }

    /** Called before the local player reference is cleared. */
    protected void onLocalPlayerCleared(TiledObjectEntity entity) {
    }

    private void clearLocalPlayer() {
        if (playerEntity == null) {
            return;
        }
        TiledObjectEntity previous = playerEntity;
        playerEntity = null;
        onLocalPlayerCleared(previous);
    }
}
