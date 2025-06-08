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
package org.ngengine.auth.nsec;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.logging.Logger;
import org.ngengine.auth.Auth;
import org.ngengine.auth.AuthStrategy;
import org.ngengine.nostr4j.keypair.NostrKeyPair;
import org.ngengine.nostr4j.nip49.Nip49;
import org.ngengine.nostr4j.signer.NostrKeyPairSigner;
import org.ngengine.nostr4j.signer.NostrSigner;
import org.ngengine.platform.AsyncTask;
import org.ngengine.platform.VStore;

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
    public AsyncTask<NostrSigner> load(VStore store, String pub, String password) {
        String filePath = pub + ".nsecAuth";
        return store
            .exists(filePath)
            .compose(v -> {
                if (v) {
                    return store
                        .read(filePath)
                        .compose(in -> {
                            try {
                                byte[] data = in.readAllBytes();
                                String encrypted = new String(data, StandardCharsets.UTF_8);
                                return Nip49
                                    .decrypt(encrypted, password)
                                    .catchException(ex -> {
                                        System.err.println("Failed to decrypt key: " + ex.getMessage());
                                    })
                                    .then(key -> {
                                        return new NostrKeyPairSigner(new NostrKeyPair(key));
                                    });
                            } catch (Exception e) {
                                throw new RuntimeException("Failed to load key", e);
                            } finally {
                                try {
                                    if (in != null) in.close();
                                } catch (IOException e) {
                                    log.warning("Failed to close input stream: " + e.getMessage());
                                }
                            }
                        });
                } else {
                    throw new RuntimeException("User does not exist");
                }
            });
    }

    @Override
    public AsyncTask<Void> delete(VStore store, String pub) {
        String filePath = pub + ".nsecAuth";
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
    public AsyncTask<List<String>> listSaved(VStore store) {
        return store
            .listAll()
            .then(files -> {
                List<String> users = new ArrayList<>();
                for (String file : files) {
                    if (file.endsWith(".nsecAuth")) {
                        String username = file.substring(0, file.length() - ".nsecAuth".length());
                        users.add(username);
                    }
                }
                return users;
            });
    }

    @Override
    public AsyncTask<Void> save(VStore store, NostrSigner sn, String password) {
        // we won't save cleartext keys
        NostrKeyPairSigner signer = (NostrKeyPairSigner) sn;
        return signer
            .getPublicKey()
            .compose(pubKey -> {
                try {
                    Objects.requireNonNull(password, "Password cannot be null");
                    if (password.isEmpty()) {
                        throw new RuntimeException("Password cannot be empty");
                    }
                    String pub = pubKey.asBech32();
                    String filePath = pub + ".nsecAuth";
                    return Nip49
                        .encrypt(signer.getKeyPair().getPrivateKey(), password)
                        .compose(encrypted -> {
                            return store
                                .write(filePath)
                                .then(out -> {
                                    try {
                                        out.write(encrypted.getBytes(StandardCharsets.UTF_8));
                                        return null;
                                    } catch (Exception e) {
                                        throw new RuntimeException("Failed to save key", e);
                                    } finally {
                                        try {
                                            out.close();
                                        } catch (IOException e) {
                                            log.warning("Failed to close output stream: " + e.getMessage());
                                        }
                                    }
                                });
                        });
                } catch (Exception e) {
                    throw new RuntimeException("Failed to save key", e);
                }
            });
    }

    @Override
    public boolean isEnabled() {
        return getOptions().getStrategy().isLocalIdentityEnabled();
    }
}
