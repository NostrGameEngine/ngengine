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

import com.jme3.math.Vector3f;
import com.simsilica.lemur.Button;
import com.simsilica.lemur.Checkbox;
import com.simsilica.lemur.HAlignment;
import com.simsilica.lemur.VAlignment;
import com.simsilica.lemur.component.DynamicInsetsComponent;
import java.util.logging.Logger;
import org.ngengine.auth.AuthConfig;
import org.ngengine.auth.AuthStrategy;
import org.ngengine.gui.components.NTextInput;
import org.ngengine.gui.components.NVSpacer;
import org.ngengine.gui.components.containers.NColumn;
import org.ngengine.gui.win.NWindow;
import org.ngengine.nostr4j.keypair.NostrKeyPair;
import org.ngengine.nostr4j.keypair.NostrPrivateKey;
import org.ngengine.nostr4j.nip49.Nip49;
import org.ngengine.nostr4j.signer.NostrKeyPairSigner;
import org.ngengine.nostr4j.signer.NostrSigner;

public class NsecAuthWindow extends NWindow<AuthConfig> {

    private static final Logger log = Logger.getLogger(NsecAuthWindow.class.getName());

    @Override
    protected void compose(Vector3f size, AuthConfig opt) throws Exception {
        if (opt.getForNpub() != null) {
            loadAndAuthStage(opt, opt.getForNpub(), false);
        } else {
            renderStage1(opt);
        }
    }

    public void authFailed(AuthConfig opt, Throwable e) {
        AuthStrategy strategy = opt.getStrategy();
        getManager().showToast(e);
        renderStage1(opt);
    }

    protected void renderStage1(AuthConfig opt) {
        AuthStrategy strategy = opt.getStrategy();
        getContent().clearChildren();
        NColumn windowContent = getContent().addCol();

        setTitle("Private Key Authentication");

        setBackAction(win -> {
            close();
        });

        NTextInput nsecInput = new NTextInput();
        {
            nsecInput.setLabel("Enter your nsec or ncryptsec private key or create a new one");
            nsecInput.setPreferredWidth(this.getPreferredSize().x * 0.6f);
            nsecInput.setIsSecretInput(true);
            nsecInput.setGenerateAction(() -> {
                return NostrPrivateKey.generate().asBech32();
            });
            windowContent.addChild(nsecInput);
        }

        Checkbox rememberMe = new Checkbox("Remember me");
        if (opt.getAuth().isStoreEnabled()) {
            rememberMe.setTextHAlignment(HAlignment.Left);
            rememberMe.setTextVAlignment(VAlignment.Center);
            rememberMe.setInsetsComponent(new DynamicInsetsComponent(0, 1, 0, 0.03f));
            nsecInput.getLeft().addChild(rememberMe);
        }

        {
            windowContent.addChild(new NVSpacer());
            windowContent.addChild(new NVSpacer());
        }

        {
            Button nsecLoginBtn = new Button("Authenticate");
            nsecLoginBtn.setTextHAlignment(HAlignment.Center);
            windowContent.addChild(nsecLoginBtn);

            nsecLoginBtn.addClickCommands(src -> {
                try {
                    if (nsecInput.getText().isEmpty()) {
                        authFailed(opt, new Exception("Nsec is empty"));
                    }

                    if (!rememberMe.isChecked()) {
                        if (nsecInput.getText().startsWith("ncryptsec")) {
                            // if the key is encrypted, decrypt and load
                            loadAndAuthStage(opt, nsecInput.getText(), false);
                        } else {
                            // if the key is not encrypted, just skip to the auth confirmation
                            NostrPrivateKey key = NostrPrivateKey.fromBech32(nsecInput.getText());
                            auth(opt, new NostrKeyPairSigner(new NostrKeyPair(key)), null);
                        }
                    } else {
                        if (nsecInput.getText().startsWith("ncryptsec")) {
                            // if the key is encrypted, and remind me is toggled, decrypt and store
                            loadAndAuthStage(opt, nsecInput.getText(), true);
                        } else {
                            // if the key is not encrypted and remind me is toggled, render the encryption stage to encrypt and store the key
                            NostrPrivateKey key = NostrPrivateKey.fromBech32(nsecInput.getText());
                            renderEncryptAndStoreStage(opt, new NostrKeyPairSigner(new NostrKeyPair(key)));
                        }
                    }
                } catch (Exception e) {
                    authFailed(opt, e);
                }
            });
        }
    }

    protected void auth(AuthConfig opt, NostrSigner signer, String saveWithPassword) {
        AuthStrategy strategy = opt.getStrategy();
        if (saveWithPassword != null) {
            try {
                opt.getAuth().save(signer, saveWithPassword).await();
            } catch (Exception e) {
                getManager().showToast(e);
            }
        }
        getManager()
            .runInThread(() -> {
                strategy.getCallback().accept(signer);
                close();
            });
    }

    /**
     * Load a signer either from the store or from a given ncryptsec
     *
     * @param strategy
     * @param inputKey
     *            the given ncryptsec or the pubkey to load from the store
     * @param save
     */
    protected void loadAndAuthStage(AuthConfig opt, String inputKey, boolean save) {
        getContent().clearChildren();
        NColumn windowContent = getContent().addCol();

        AuthStrategy strategy = opt.getStrategy();
        setTitle("Passphrase required");
        setBackAction(win -> {
            renderStage1(opt);
        });

        NTextInput passphraseInput = new NTextInput();
        passphraseInput.setLabel("Please enter the passphrase to decrypt the private key");
        passphraseInput.setIsSecretInput(true);
        windowContent.addChild(passphraseInput);

        Button nsecLoginBtn = new Button("Continue");
        nsecLoginBtn.setTextHAlignment(HAlignment.Center);
        windowContent.addChild(nsecLoginBtn);

        nsecLoginBtn.addClickCommands(src -> {
            try {
                if (!inputKey.startsWith("ncryptsec")) { // is npub
                    if (passphraseInput.getText().isEmpty()) {
                        throw new Exception("Passphrase is empty");
                    }
                    String npub = inputKey;
                    opt
                        .getAuth()
                        .load(npub, passphraseInput.getText())
                        .catchException(e -> {
                            getManager().showToast(e);
                        })
                        .then(signer -> {
                            auth(opt, signer, null);
                            return null;
                        });
                } else { // is ncryptsec
                    String ncryptsec = inputKey;
                    Nip49
                        .decrypt(ncryptsec, passphraseInput.getText())
                        .catchException(e -> {
                            getManager().showToast(e);
                        })
                        .then(key -> {
                            NostrKeyPairSigner signer = new NostrKeyPairSigner(new NostrKeyPair(key));

                            String saveWithPassword = null;
                            if (save) {
                                if (passphraseInput.getText().isEmpty()) {
                                    throw new RuntimeException("Passphrase is empty");
                                }
                                saveWithPassword = passphraseInput.getText();
                            }

                            auth(opt, signer, saveWithPassword);
                            return null;
                        })
                        .catchException(e -> {
                            getManager().showToast(e);
                        });
                }
            } catch (Throwable e) {
                getManager().showToast(e);
            }
        });
    }

    protected void renderEncryptAndStoreStage(AuthConfig opt, NostrKeyPairSigner signer) {
        getContent().clearChildren();
        NColumn windowContent = getContent().addCol();

        AuthStrategy strategy = opt.getStrategy();
        setTitle("Encrypt and store");
        setBackAction(win -> {
            renderStage1(opt);
        });

        NTextInput passphraseInput = new NTextInput();
        passphraseInput.setLabel("Please enter a passphrase that will be used to encrypt the private key before storing it");
        passphraseInput.setIsSecretInput(true);
        windowContent.addChild(passphraseInput);

        Button nsecLoginBtn = new Button("Continue");
        nsecLoginBtn.setTextHAlignment(HAlignment.Center);
        windowContent.addChild(nsecLoginBtn);

        nsecLoginBtn.addClickCommands(src -> {
            try {
                if (passphraseInput.getText().isEmpty()) {
                    throw new Exception("Passphrase is empty");
                }
                auth(opt, signer, passphraseInput.getText());
            } catch (Exception e) {
                authFailed(opt, e);
            }
        });
    }
}
