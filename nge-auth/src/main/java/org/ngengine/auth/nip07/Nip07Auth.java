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

package org.ngengine.auth.nip07;

import java.io.IOException;
import java.util.List;
import java.util.logging.Logger;
import org.ngengine.auth.Auth;
import org.ngengine.auth.AuthStrategy;
import org.ngengine.nostr4j.signer.NostrNIP07Signer;
import org.ngengine.nostr4j.signer.NostrSigner;
import org.ngengine.store.DataStore;

public class Nip07Auth extends Auth {

    private static final Logger logger = Logger.getLogger(Nip07Auth.class.getName());
    private NostrNIP07Signer signer;

    public Nip07Auth(AuthStrategy strategy) {
        super(strategy, Nip07AuthWindow.class);
        signer = new NostrNIP07Signer();
    }

    @Override
    public boolean isStoreEnabled() {
        return false;
    }

    public NostrNIP07Signer getSigner() {
        return signer;
    }
    
    @Override
    public String getNewIdentityText() {
        return "Auth with Extension (NIP-07)";
    }

    @Override
    protected NostrSigner load(DataStore store, String pub, String encryptionKey) throws IOException {
        throw new IOException("Nip07Auth does not support loading from store");
    }

    @Override
    protected void delete(DataStore store, String pub) throws IOException {
    }

    @Override
    protected List<String> listSaved(DataStore store) {
        return List.of();
     }

    @Override
    protected void save(DataStore store, NostrSigner s, String encryptionKey) throws IOException {
    }

    @Override
    public boolean isEnabled() {
        try{
           return getOptions().getStrategy().isNip07IdentityEnabled() && signer.isAvailable().await();
        } catch(Exception ex){
            logger.warning("Error checking NIP-07 availability: "+ex.getMessage());
        }
        return false;
    }
}
