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

package org.ngengine.auth;

import java.io.IOException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.ngengine.gui.win.NWindow;
import org.ngengine.gui.win.NWindowManager;
import org.ngengine.nostr4j.signer.NostrSigner;
import org.ngengine.store.DataStore;

public abstract class Auth {
    private final static Logger logger = Logger.getLogger(Auth.class.getName());

    protected final AuthConfig options;
    protected final Class<? extends NWindow<AuthConfig>> authWindow;

    public Auth(AuthStrategy strategy, Class<? extends NWindow<AuthConfig>> authWindow) {
        this.options = new AuthConfig(strategy);
        this.authWindow = authWindow;
    }

    public abstract String getNewIdentityText();

    public NWindow<AuthConfig> open(NWindowManager manager, String forPubKey) {
        AuthConfig options = this.options.clone();
        if (forPubKey != null) {
            options.setForNpub(forPubKey);
        }
        options.setAuth(this);
        NWindow<AuthConfig> win = manager.showWindow(
            authWindow,
            options);    
            return win;
           
    }

    public NWindow<AuthConfig> open(NWindowManager manager) {
       return open(manager, null);
    }

    public DataStore getStore() {
        return options.strategy.getStore();
    }

    public boolean isStoreEnabled() {
        return options.strategy.isStoreEnabled();
    }

    protected AuthConfig getOptions() {
        return options;
    }

    public abstract boolean isEnabled();

    protected abstract NostrSigner load(DataStore store, String pub, String encryptionKey) throws IOException;

    public NostrSigner load(String pub, String encryptionKey) throws IOException {
        DataStore store = getStore();
        if (store != null) {
            return load(store, pub, encryptionKey);
        } else {
           throw new IOException("No store available for auth");
        }
    }

    protected abstract void delete(DataStore store, String pub) throws IOException;

    public void delete(String pub) throws IOException{
        DataStore store = getStore();
        if (store != null) {
            delete(store, pub);
        }
    }

    protected abstract List<String> listSaved(DataStore store)throws IOException ;

    public List<String> listSaved(){
        try{
            DataStore store = getStore();
            if (store != null) {
                return listSaved(store);
            } else {
                return List.of();
            }
        } catch(Exception ex){
            logger.log(Level.WARNING, "Error listing saved identities", ex);
            return List.of();
        }
    }

    protected abstract void save(DataStore store, NostrSigner signer, String encryptionKey) throws IOException;

    public void save(NostrSigner signer, String encryptionKey) {
        try{
            DataStore store = getStore();
            if (store != null) {
                save(store, signer, encryptionKey);
            }    
        } catch(Exception ex){
            logger.log(Level.WARNING, "Error saving identity", ex);
        }
    }
}
