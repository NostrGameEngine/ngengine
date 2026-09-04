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

import java.math.BigInteger;

import org.ngengine.components.ComponentManager;
import org.ngengine.network.components.NetcodeOrphanContext;
import org.ngengine.network.components.NetcodePartitioning;
import org.ngengine.nostr4j.keypair.NostrPublicKey;
import org.ngengine.world2d.tiled.components.TiledObjectSyncComponent;
import org.ngengine.world2d.tiled.core.TiledBase;
import org.ngengine.world2d.tiled.core.entity.TiledObjectEntity;

/** Synchronizes a transient player object and removes it after owner orphaning. */
public class TiledPlayerSyncComponent extends TiledObjectSyncComponent {
    @Override
    public void onTiledEntityInitialize(ComponentManager manager, TiledBase entry) {
        super.onTiledEntityInitialize(manager, entry);
        if (!(entry instanceof TiledObjectEntity)) {
            return;
        }
        TiledPlayerManagerComponent players =
            getInstanceOf(TiledPlayerManagerComponent.class);
        NostrPublicKey owner = ownerPublicKey(((TiledObjectEntity) entry).getId());
        if (players != null && owner != null) {
            players.rememberPlayerEntity(owner, (TiledObjectEntity) entry);
        }
    }

    @Override
    public void onNetworkOrphaned(NetcodeOrphanContext context) {
        TiledObjectEntity player = getNetworkEntity();
        if (player == null) {
            return;
        }
        TiledPlayerManagerComponent players =
            getInstanceOf(TiledPlayerManagerComponent.class);
        if (players != null) {
            players.forgetPlayerEntity(player);
        }
        player.removeFromLayer();
    }

    private static NostrPublicKey ownerPublicKey(BigInteger networkId) {
        BigInteger owner = NetcodePartitioning.decodeReservedOwnerKey(networkId);
        if (owner == null || owner.signum() < 0 || owner.bitLength() > 256) {
            return null;
        }
        String ownerHex = owner.toString(16);
        if (ownerHex.length() < 64) {
            ownerHex = "0".repeat(64 - ownerHex.length()) + ownerHex;
        }
        try {
            return NostrPublicKey.fromHex(ownerHex);
        } catch (RuntimeException invalidKey) {
            return null;
        }
    }
}
