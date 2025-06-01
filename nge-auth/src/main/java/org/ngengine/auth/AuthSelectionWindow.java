package org.ngengine.auth;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.ngengine.CallbackPolicy;
import org.ngengine.auth.nip46.Nip46Auth;
import org.ngengine.auth.nsec.NsecAuth;
import org.ngengine.auth.stored.StoredAuthSelectionOptions;
import org.ngengine.auth.stored.StoredAuthSelectionWindow;
import org.ngengine.gui.components.NIconButton;
import org.ngengine.gui.win.NWindow;
import org.ngengine.nostr4j.keypair.NostrPublicKey;
import org.ngengine.player.Player;

import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.simsilica.lemur.Axis;
import com.simsilica.lemur.Button;
import com.simsilica.lemur.Container;
import com.simsilica.lemur.FillMode;
import com.simsilica.lemur.VAlignment;
import com.simsilica.lemur.component.IconComponent;
import com.simsilica.lemur.component.SpringGridLayout;

public class AuthSelectionWindow extends NWindow<AuthStrategy> {
    private final Logger log = Logger.getLogger(AuthSelectionWindow.class.getName());
    private final Map<Class<? extends Auth>, Auth> initializedAuths = new HashMap<>();

    private List<Class<? extends Auth>> authMethods = new ArrayList<>(
            List.of(NsecAuth.class, Nip46Auth.class));



    public List<Class<? extends Auth>> getAuthMethods() {
        return authMethods;
    }

    public void setAuthMethods(List<Class<? extends Auth>> authMethods) {
        this.authMethods = authMethods;
        invalidate();
    }

    private Button playerButton(String npub, Auth auth

    ) {
        Player player = auth.getOptions().getStrategy().getPlayerManager()
                .getPlayer(NostrPublicKey.fromBech32(npub));

        Button storedIdentityButton = new Button("  " + player.getName());

        player.addUpdateListener(() -> {
            storedIdentityButton.setText("  " + player.getName());
            return CallbackPolicy.REMOVE_AFTER_CALL;
        });
        IconComponent icon = new IconComponent(player.getImage(), new Vector2f(1, 1), 0, 0, 0, false);
        float iconSize = storedIdentityButton.getFontSize() * 3;
        icon.setIconSize(new Vector2f(iconSize, iconSize));
        icon.setMargin(0, 0);
        storedIdentityButton.setIcon(icon);
        storedIdentityButton.setTextVAlignment(VAlignment.Center);

        NIconButton deleteBtn = new NIconButton("icons/outline/activity.svg");
        storedIdentityButton.attachChild(deleteBtn);
        deleteBtn.setLocalTranslation(0, 0, 1);

        storedIdentityButton.addClickCommands(src -> {
            getManager().showWindow(StoredAuthSelectionWindow.class,
                    new StoredAuthSelectionOptions(player.getName(), player.getImage())
                            .setConfirmAction(win -> {
                                auth.open(getManager(), npub, null);

                            }).setRemoveAction(win -> {
                                auth.delete(npub);

                            }));
        });
        return storedIdentityButton;
    }

    @Override
    protected void compose(Vector3f size, AuthStrategy strategy) throws Exception {
        setTitle("Authentication");
        setFitContent(true);

        Container content = getContent().addCol();
        Container stored = new Container(new SpringGridLayout(Axis.Y, Axis.X, FillMode.None, FillMode.None));
        content.addChild(stored);

        for (Class<? extends Auth> authClass : authMethods) {
            try {
                Auth auth = initializedAuths.computeIfAbsent(authClass, k -> {
                    try {
                        return authClass.getDeclaredConstructor(AuthStrategy.class)
                                .newInstance(strategy);
                    } catch (Exception e) {
                        throw new RuntimeException(
                                "Failed to initialize auth method: " + authClass.getSimpleName(), e);
                    }
                });
                if (auth.isEnabled()) {
                    List<String> npubs = auth.listSaved().await();
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
