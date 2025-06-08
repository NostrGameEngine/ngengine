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
package org.ngengine.auth;

import java.util.function.Consumer;
import org.ngengine.nostr4j.signer.NostrSigner;
import org.ngengine.platform.VStore;
import org.ngengine.player.PlayerManagerComponent;

public class AuthStrategy {

    protected Nip46AuthStrategy nip46;
    protected boolean localIdentity = true;
    protected VStore store;
    protected boolean isStoreSet = false;
    protected Consumer<NostrSigner> callback;
    protected PlayerManagerComponent playerManager;

    public AuthStrategy(Consumer<NostrSigner> callback) {
        this.callback = callback;
    }

    public Consumer<NostrSigner> getCallback() {
        return callback;
    }

    public AuthStrategy enableNip46RemoteIdentity(Nip46AuthStrategy nip46) {
        this.nip46 = nip46;
        return this;
    }

    public AuthStrategy enableStore(VStore store) {
        this.isStoreSet = true;
        this.store = store;
        return this;
    }

    public boolean isAutoStore() {
        return !isStoreSet;
    }

    public AuthStrategy disableStore() {
        this.isStoreSet = true;
        this.store = null;
        return this;
    }

    public AuthStrategy autoStore() {
        this.store = null;
        this.isStoreSet = false;
        return this;
    }

    public AuthStrategy setPlayerManager(PlayerManagerComponent playerManager) {
        this.playerManager = playerManager;
        return this;
    }

    public PlayerManagerComponent getPlayerManager() {
        return playerManager;
    }

    public boolean isStoreEnabled() {
        return store != null;
    }

    public VStore getStore() {
        return store;
    }

    public AuthStrategy enableLocalIdentity() {
        this.localIdentity = true;
        return this;
    }

    public AuthStrategy disableNip46RemoteIdentity() {
        nip46 = null;
        return this;
    }

    public AuthStrategy disableLocalIdentity() {
        localIdentity = false;
        return this;
    }

    public AuthStrategy enableNip07Identity(Object nip07adapter) {
        return this;
    }

    public AuthStrategy disableNip07Identity() {
        return this;
    }

    public boolean isNip46RemoteIdentityEnabled() {
        return nip46 != null;
    }

    public boolean isLocalIdentityEnabled() {
        return localIdentity;
    }

    public boolean isNip07IdentityEnabled() {
        return false;
    }

    public Nip46AuthStrategy getNip46RemoteIdentityStrategy() {
        return nip46;
    }
}
