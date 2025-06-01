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
        return store.exists(filePath).compose((v) -> {
        if (v) {
            return store.read(filePath).compose((in) -> {
                try{
                    byte[] data = in.readAllBytes();
                    String encrypted = new String(data, StandardCharsets.UTF_8);
                     return Nip49.decrypt(encrypted, password)
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
        return store.exists(filePath).compose(v -> {
            if (v) {
                return store.delete(filePath);
            } else {
                throw new RuntimeException("User does not exist");
            }

        });
    }

    @Override
    public AsyncTask<List<String>> listSaved(VStore store) {
        return store.listAll().then((files) -> {
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
        return signer.getPublicKey().compose(pubKey -> {
            try{
                Objects.requireNonNull(password, "Password cannot be null");
                if (password.isEmpty()) {
                    throw new RuntimeException("Password cannot be empty");
                }
                String pub = pubKey.asBech32();
                String filePath = pub + ".nsecAuth";
                return Nip49.encrypt(signer.getKeyPair().getPrivateKey(), password).compose(encrypted->{                    
                    return store.write(filePath).then((out) -> {
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
        return getOptions().getStrategy().isNsecEnabled();
    }



 
    
}
