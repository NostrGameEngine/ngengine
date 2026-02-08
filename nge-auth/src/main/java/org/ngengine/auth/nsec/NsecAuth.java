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

package org.ngengine.auth.nsec;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.logging.Logger;
import org.ngengine.auth.Auth;
import org.ngengine.auth.AuthStrategy;
import org.ngengine.export.NostrPrivateKeySavableWrapper;
import org.ngengine.nostr4j.keypair.NostrKeyPair;
import org.ngengine.nostr4j.keypair.NostrPublicKey;
import org.ngengine.nostr4j.signer.NostrKeyPairSigner;
import org.ngengine.nostr4j.signer.NostrSigner;
import org.ngengine.store.DataStore;

public class NsecAuth extends Auth {

    private static final Logger log = Logger.getLogger(NsecAuth.class.getName());

    public NsecAuth(AuthStrategy strategy) {
        super(strategy, NsecAuthWindow.class);
    }

    @Override
    public String getNewIdentityText() {
        if (isStoreEnabled()) {
            return "Add Local Identity (NSEC)";
        } else {
            return "Authenticate with Local Identity (NSEC)";
        }
    }

    @Override
    public NostrSigner load(DataStore store, String pub, String password) throws IOException {
        String filePath = pub + ".nsecAuth";
        if (store.exists(filePath)) {
            NostrPrivateKeySavableWrapper wrap = store.read(filePath);
            try {
                return new NostrKeyPairSigner(new NostrKeyPair(wrap.get(password)));
            } catch (Exception e) {
                throw new RuntimeException("Failed to load key", e);
            }
        } else {
            throw new RuntimeException("User does not exist");
        }
        
    }

    @Override
    public void delete(DataStore store, String pub) throws IOException {
        String filePath = pub + ".nsecAuth";
        store.delete(filePath);
    }

    @Override
    public List<String> listSaved(DataStore store) throws IOException {
        List<String> users = new ArrayList<>();
        List<String> files = store.list();
        for (String file : files) {
            if (file.endsWith(".nsecAuth")) {
                try{
                    String username = file.substring(0, file.length() - ".nsecAuth".length());
                    users.add(username);
                } catch(Exception e){
                    log.log(java.util.logging.Level.WARNING, "Error listing saved identity: " + file, e);
                }
            }
        }
        return users;
    }

    @Override
    public void save(DataStore store, NostrSigner sn, String password) throws IOException {
        // we won't save cleartext keys
        Objects.requireNonNull(password, "Password cannot be null");

        NostrKeyPairSigner signer = (NostrKeyPairSigner) sn;
        NostrPublicKey pubKey = signer.getKeyPair().getPublicKey();
        if (password.isEmpty()) {
            throw new RuntimeException("Password cannot be empty");
        }
        String pub = pubKey.asBech32();
        String filePath = pub + ".nsecAuth";
        NostrPrivateKeySavableWrapper wrap = new NostrPrivateKeySavableWrapper(signer.getKeyPair().getPrivateKey(), password);
        store.write(filePath, wrap);
     

    }

    @Override
    public boolean isEnabled() {
        return getOptions().getStrategy().isLocalIdentityEnabled();
    }
}
