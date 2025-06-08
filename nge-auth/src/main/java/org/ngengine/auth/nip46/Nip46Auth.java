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
package org.ngengine.auth.nip46;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import org.ngengine.auth.Auth;
import org.ngengine.auth.AuthStrategy;
import org.ngengine.nostr4j.signer.NostrNIP46Signer;
import org.ngengine.nostr4j.signer.NostrSigner;
import org.ngengine.platform.AsyncTask;
import org.ngengine.platform.VStore;

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
    protected AsyncTask<NostrSigner> load(VStore store, String pub, String encryptionKey) {
        String filePath = pub + ".nip46";

        return store
            .exists(filePath)
            .compose(v -> {
                if (v) {
                    return store
                        .read(filePath)
                        .then(in -> {
                            ObjectInputStream ois = null;
                            try {
                                ois = new ObjectInputStream(in);
                                NostrNIP46Signer signer = (NostrNIP46Signer) ois.readObject();
                                return signer;
                            } catch (Exception e) {
                                throw new RuntimeException("Failed to load key", e);
                            } finally {
                                try {
                                    if (ois != null) ois.close();
                                } catch (IOException e) {
                                    logger.warning("Failed to close input stream: " + e.getMessage());
                                }
                            }
                        });
                } else {
                    throw new RuntimeException("User does not exist");
                }
            });
    }

    @Override
    protected AsyncTask<Void> delete(VStore store, String pub) {
        String filePath = pub + ".nip46";
        return store
            .exists(filePath)
            .compose(v -> {
                if (v) {
                    return store.delete(filePath);
                } else {
                    throw new RuntimeException("User does not exist");
                }
            });
    }

    @Override
    protected AsyncTask<List<String>> listSaved(VStore store) {
        return store
            .listAll()
            .then(files -> {
                List<String> users = new ArrayList<>();
                for (String file : files) {
                    if (file.endsWith(".nip46")) {
                        String username = file.substring(0, file.length() - ".nip46".length());
                        users.add(username);
                    }
                }
                return users;
            });
    }

    @Override
    protected AsyncTask<Void> save(VStore store, NostrSigner signer, String encryptionKey) {
        return signer
            .getPublicKey()
            .compose(pubKey -> {
                String pub = pubKey.asBech32();
                String filePath = pub + ".nip46";
                return store
                    .write(filePath)
                    .then(out -> {
                        ObjectOutputStream oos = null;
                        try {
                            oos = new ObjectOutputStream(out);
                            oos.writeObject(signer);
                            return null;
                        } catch (Exception e) {
                            throw new RuntimeException("Failed to save key", e);
                        } finally {
                            try {
                                if (oos != null) oos.close();
                            } catch (IOException e) {
                                logger.warning("Failed to close output stream: " + e.getMessage());
                            }
                        }
                    });
            });
    }

    @Override
    public boolean isEnabled() {
        return getOptions().getStrategy().isNip46RemoteIdentityEnabled();
    }
}
