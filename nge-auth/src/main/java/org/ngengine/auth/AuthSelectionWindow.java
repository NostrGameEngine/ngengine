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

import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import org.ngengine.gui.component.IconComponent;
import org.ngengine.gui.component.SpringGridLayout;
import org.ngengine.gui.guix.NIconButton;
import org.ngengine.gui.guix.win.NWindow;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.ngengine.CallbackPolicy;
import org.ngengine.auth.nip07.Nip07Auth;
import org.ngengine.auth.nip46.Nip46Auth;
import org.ngengine.auth.nsec.NsecAuth;
import org.ngengine.auth.stored.StoredAuthSelectionOptions;
import org.ngengine.auth.stored.StoredAuthSelectionWindow;
import org.ngengine.gui.Axis;
import org.ngengine.gui.Button;
import org.ngengine.gui.Container;
import org.ngengine.gui.FillMode;
import org.ngengine.gui.VAlignment;
import org.ngengine.nostr4j.keypair.NostrPublicKey;
import org.ngengine.player.Player;
import org.ngengine.player.PlayerManagerComponent;

public class AuthSelectionWindow extends NWindow<AuthStrategy> {

    private final Logger log = Logger.getLogger(AuthSelectionWindow.class.getName());
    private final Map<Class<? extends Auth>, Auth> initializedAuths = new HashMap<>();

    private List<Class<? extends Auth>> authMethods = new ArrayList<>(List.of(Nip07Auth.class, NsecAuth.class, Nip46Auth.class));

    public List<Class<? extends Auth>> getAuthMethods() {
        return authMethods;
    }

    public void setAuthMethods(List<Class<? extends Auth>> authMethods) {
        this.authMethods = authMethods;
        invalidate();
    }

    private Button playerButton(String npub, Auth auth) {
        PlayerManagerComponent playerManager = auth.getOptions().getStrategy().getPlayerManager();
        Player player = playerManager != null ? playerManager.getPlayer(NostrPublicKey.fromBech32(npub)) : null;

        Button storedIdentityButton = new Button("  " + (player != null ? player.getName() : npub));

        if (player != null) {
            player.addUpdateListener(() -> {
                storedIdentityButton.setText("  " + player.getName());
                return CallbackPolicy.REMOVE_AFTER_CALL;
            });

            IconComponent icon = new IconComponent(player.getImage(), new Vector2f(1, 1), 0, 0, 0, false);
            float iconSize = storedIdentityButton.getFontSize() * 3;
            icon.setIconSize(new Vector2f(iconSize, iconSize));
            icon.setMargin(0, 0);
            storedIdentityButton.setIcon(icon);
        }
        storedIdentityButton.setTextVAlignment(VAlignment.Center);

        NIconButton deleteBtn = new NIconButton("org/ngengine/gui/icons/outline/activity.svg");
        storedIdentityButton.attachChild(deleteBtn);
        deleteBtn.setLocalTranslation(0, 0, 1);

        storedIdentityButton.addClickCommands(src -> {
            getManager()
                .showWindow(
                    StoredAuthSelectionWindow.class,
                    new StoredAuthSelectionOptions(
                        player != null ? player.getName() : npub,
                        player != null ? player.getImage() : null
                    )
                        .setConfirmAction(win -> {
                            auth.open(getManager(), npub);
                        })
                        .setRemoveAction(win -> {
                            try {
                                auth.delete(npub);
                            } catch (IOException e) {
                                log.log(Level.WARNING, "Failed to delete stored identity: " + npub, e);
                            }
                        })
                );
        });
        return storedIdentityButton;
    }

    @Override
    protected void compose(Vector3f size, AuthStrategy strategy) throws Exception {
        setTitle("Authentication");
        setFitContent(true);

        if (strategy.isAutoStore()) {
            strategy.store = this.getManager().getDataStoreProvider().getDataStore("auth");
        }

        Container content = getContent().addCol();
        Container stored = new Container(new SpringGridLayout(Axis.Y, Axis.X, FillMode.None, FillMode.None));
        content.addChild(stored);

        for (Class<? extends Auth> authClass : authMethods) {
            try {
                Auth auth = initializedAuths.computeIfAbsent(
                    authClass,
                    k -> {
                        try {
                            return authClass.getDeclaredConstructor(AuthStrategy.class).newInstance(strategy);
                        } catch (Exception e) {
                            throw new RuntimeException("Failed to initialize auth method: " + authClass.getSimpleName(), e);
                        }
                    }
                );
                if (auth.isEnabled()) {
                    List<String> npubs = auth.listSaved();
                    for (String npub : npubs) {
                        Button storedIdentityButton = playerButton(npub, auth);
                        stored.addChild(storedIdentityButton);
                    }
                    Button newIdentity = new Button(auth.getNewIdentityText());
                    newIdentity.addClickCommands(src -> {
                        auth.open(getManager(), null);
                    });
                    content.addChild(newIdentity);
                }
            } catch (Exception e) {
                log.log(Level.WARNING, "Failed to initialize auth method: " + authClass.getSimpleName(), e);
                continue;
            }
        }
    }
}
