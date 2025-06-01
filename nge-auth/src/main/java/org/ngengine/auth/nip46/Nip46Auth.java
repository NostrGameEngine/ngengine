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

    public Nip46Auth(  AuthStrategy strategy) {
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

        return store.exists(filePath).compose((v) -> {
            if (v) {
                return store.read(filePath).then((in) -> {
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
        return store.exists(filePath).compose(v -> {
            if (v) {
                return store.delete(filePath);
            } else {
                throw new RuntimeException("User does not exist");
            }

        });
    }

    @Override
    protected AsyncTask<List<String>> listSaved(VStore store) {
        return store.listAll().then((files) -> {
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
       return signer.getPublicKey().compose(pubKey -> {
            String pub = pubKey.asBech32();
            String filePath = pub + ".nip46";
            return store.write(filePath).then((out) -> {
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
        return getOptions().getStrategy().isNip46Enabled();
    }


    
}
