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
 * the BSD 3-Clause License. 
 */

package org.ngengine.auth.nip46;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import org.ngengine.auth.Auth;
import org.ngengine.auth.AuthStrategy;
import org.ngengine.export.Nip46SignerSavableWrapper;
import org.ngengine.nostr4j.keypair.NostrPublicKey;
import org.ngengine.nostr4j.signer.NostrNIP46Signer;
import org.ngengine.nostr4j.signer.NostrSigner;
import org.ngengine.store.DataStore;

public class Nip46Auth extends Auth {

    private static final Logger logger = Logger.getLogger(Nip46Auth.class.getName());

    public Nip46Auth(AuthStrategy strategy) {
        super(strategy, Nip46AuthWindow.class);
    }

    @Override
    public String getNewIdentityText() {
        if (isStoreEnabled()) {
            return "Add Remote Identity (NIP-46)";
        } else {
            return "Authenticate with Remote Identity (NIP-46)";
        }
    }

    @Override
    protected NostrSigner load(DataStore store, String pub, String encryptionKey) throws IOException {
        String filePath = pub + ".nip46";
        if(store.exists(filePath)){
            Nip46SignerSavableWrapper wrap = store.read(filePath);
            NostrNIP46Signer signer = wrap.get();
            return signer;
        } else {
            throw new IOException("User does not exist");
        }
    }

    @Override
    protected void delete(DataStore store, String pub) throws IOException {
        String filePath = pub + ".nip46";
        store.delete(filePath);
    }

    @Override
    protected List<String> listSaved(DataStore store) {
       
        List<String> files = store.list();
        List<String> users = new ArrayList<>();
        for (String file : files) {
            try{
                if (file.endsWith(".nip46")) {
                    String username = file.substring(0, file.length() - ".nip46".length());
                    users.add(username);
                }
            } catch(Exception e){
                logger.log(java.util.logging.Level.WARNING, "Error listing saved identity: " + file, e);
            }
        }
        return users;
     }

    @Override
    protected void save(DataStore store, NostrSigner s, String encryptionKey) throws IOException {
        try{
            NostrNIP46Signer signer = (NostrNIP46Signer) s;
            NostrPublicKey pubkey = signer.getPublicKey().await();

            String pub = pubkey.asBech32();
            String filePath = pub + ".nip46";

            Nip46SignerSavableWrapper wrap = new Nip46SignerSavableWrapper(signer);
            store.write(filePath,wrap);
        } catch(Exception e){
            throw new IOException("Failed to save key", e);
        }

    }

    @Override
    public boolean isEnabled() {
        return getOptions().getStrategy().isNip46RemoteIdentityEnabled();
    }
}
