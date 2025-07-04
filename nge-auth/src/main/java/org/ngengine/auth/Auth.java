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

import java.util.List;
import java.util.function.BiConsumer;
import org.ngengine.gui.win.NWindow;
import org.ngengine.gui.win.NWindowManagerComponent;
import org.ngengine.nostr4j.signer.NostrSigner;
import org.ngengine.platform.AsyncTask;
import org.ngengine.platform.NGEPlatform;
import org.ngengine.platform.VStore;

public abstract class Auth {

    protected final AuthConfig options;
    protected final Class<? extends NWindow<AuthConfig>> authWindow;

    public Auth(AuthStrategy strategy, Class<? extends NWindow<AuthConfig>> authWindow) {
        this.options = new AuthConfig(strategy);
        this.authWindow = authWindow;
    }

    public abstract String getNewIdentityText();

    public void open(NWindowManagerComponent manager, String forPubKey, BiConsumer<NWindow<AuthConfig>, Throwable> callback) {
        AuthConfig options = this.options.clone();
        if (forPubKey != null) {
            options.setForNpub(forPubKey);
        }
        options.setAuth(this);
        manager.showWindow(
            authWindow,
            options,
            (win, error) -> {
                if (callback == null) {
                    return;
                }
                if (error != null) {
                    callback.accept(null, error);
                } else {
                    callback.accept((NWindow<AuthConfig>) win, null);
                }
            }
        );
    }

    public void open(NWindowManagerComponent manager, BiConsumer<NWindow<AuthConfig>, Throwable> callback) {
        open(manager, null, callback);
    }

    public VStore getStore() {
        return options.strategy.getStore();
    }

    public boolean isStoreEnabled() {
        return options.strategy.isStoreEnabled();
    }

    protected AuthConfig getOptions() {
        return options;
    }

    public abstract boolean isEnabled();

    protected abstract AsyncTask<NostrSigner> load(VStore store, String pub, String encryptionKey);

    public AsyncTask<NostrSigner> load(String pub, String encryptionKey) {
        VStore store = getStore();
        if (store != null) {
            return load(store, pub, encryptionKey);
        } else {
            return NGEPlatform.get().wrapPromise((res, rej) -> rej.accept(new RuntimeException("No store available for auth")));
        }
    }

    protected abstract AsyncTask<Void> delete(VStore store, String pub);

    public AsyncTask<Void> delete(String pub) {
        VStore store = getStore();
        if (store != null) {
            return delete(store, pub);
        } else {
            return NGEPlatform.get().wrapPromise((res, rej) -> rej.accept(new RuntimeException("No store available for auth")));
        }
    }

    protected abstract AsyncTask<List<String>> listSaved(VStore store);

    public AsyncTask<List<String>> listSaved() {
        VStore store = getStore();
        if (store != null) {
            return listSaved(store);
        } else {
            return NGEPlatform
                .get()
                .wrapPromise((res, rej) -> {
                    res.accept(List.of());
                });
        }
    }

    protected abstract AsyncTask<Void> save(VStore store, NostrSigner signer, String encryptionKey);

    public AsyncTask<Void> save(NostrSigner signer, String encryptionKey) {
        VStore store = getStore();
        if (store != null) {
            return save(store, signer, encryptionKey);
        } else {
            return NGEPlatform
                .get()
                .wrapPromise((res, rej) -> {
                    res.accept(null);
                });
        }
    }
}
