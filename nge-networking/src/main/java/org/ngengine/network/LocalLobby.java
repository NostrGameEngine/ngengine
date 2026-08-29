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

package org.ngengine.network;

import java.time.Instant;
import java.util.function.Consumer;
import java.util.logging.Logger;
import org.ngengine.nostr4j.keypair.NostrPublicKey;
import org.ngengine.platform.NGEPlatform;
import org.ngengine.platform.NGEUtils;

public class LocalLobby extends Lobby {

    private static final Logger logger = Logger.getLogger(LocalLobby.class.getName());

    private transient volatile boolean updateNeeded = false;
    private transient Consumer<NostrPublicKey> banKickHandler;

    LocalLobby(
            String roomId,
            String roomKey,
            String roomRawData,
            Instant expiration,
            Instant creationTime,
            NostrPublicKey owner) {
        super(roomId, roomKey, roomRawData, expiration, creationTime, owner);
    }

    @Override
    public boolean isOwnedByLocalPeer() {
        return true;
    }

    public void setData(String key, String value) {
        if (BANNED_PEERS_DATA_KEY.equals(key)) {
            throw new IllegalArgumentException("The lobby ban list is managed through banPeer and unbanPeer.");
        }
        super.setData(key, value);
        markUpdateNeeded();
    }

    /** Adds a peer to this lobby's persistent ban list and kicks it immediately. */
    public boolean banPeer(NostrPublicKey peer) {
        if (peer == null || isOwner(peer) || !addBannedPeer(peer)) {
            return false;
        }
        super.setData(BANNED_PEERS_DATA_KEY, serializeBannedPeers());
        markUpdateNeeded();
        Consumer<NostrPublicKey> handler = banKickHandler;
        if (handler != null) {
            handler.accept(peer);
        }
        return true;
    }

    /** Removes a peer from this lobby's persistent ban list. */
    public boolean unbanPeer(NostrPublicKey peer) {
        if (!removeBannedPeer(peer)) {
            return false;
        }
        String encoded = serializeBannedPeers();
        super.setData(BANNED_PEERS_DATA_KEY, encoded.isEmpty() ? null : encoded);
        markUpdateNeeded();
        return true;
    }

    private void markUpdateNeeded() {
        NGEPlatform p = NGEUtils.getPlatform();
        String rawData = p.toJSON(this.data);
        this.roomRawData = rawData;
        this.updateNeeded = true;
    }

    protected void setDataSilent(String key, String value) {
        super.setData(key, value);
    }

    protected boolean isUpdateNeeded() {
        return updateNeeded;
    }

    protected void clearUpdateNeeded() {
        this.updateNeeded = false;
    }

    void setBanKickHandler(Consumer<NostrPublicKey> handler) {
        this.banKickHandler = handler;
    }
}
