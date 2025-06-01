package org.ngengine.demo.son;

import java.util.List;
import java.util.function.Consumer;

import org.ngengine.auth.AuthStrategy;
import org.ngengine.auth.Nip46AuthStrategy;
import org.ngengine.nostr4j.nip46.Nip46AppMetadata;
import org.ngengine.nostr4j.signer.NostrSigner;
import org.ngengine.platform.NGEPlatform;
import org.ngengine.platform.VStore;
import org.ngengine.player.PlayerManagerAppState;

import com.jme3.system.AppSettings;

public class Defaults {
  
    public static final List<String> AUTH_RELAYS = List.of(
    "wss://relay.nsec.app",
    "wss://relay.ngengine.org"    
    );
    public static final List<String> ID_RELAYS = List.of(
        "wss://relay.ngengine.org",
        "wss://relay.snort.social",
        "wss://relay.damus.io",
        "wss://relay.primal.net"
    );
    public static final Nip46AppMetadata NIP46_METADATA = new Nip46AppMetadata().setName("ngengine.org - Unnamed App");

    public static final AuthStrategy authStrategy(AppSettings settings, Consumer<NostrSigner> callback) {
        return authStrategy(settings, callback, null);
    }

    public static final AuthStrategy authStrategy(AppSettings settings, Consumer<NostrSigner> callback, PlayerManagerAppState playerManager) {
        VStore authStore = NGEPlatform.get().getDataStore(settings.getTitle(), "auth");
        return new AuthStrategy(callback)
        .enableStore(authStore)
        .enableNip46(new Nip46AuthStrategy().setMetadata(NIP46_METADATA))
        .enableNsec()
        .setPlayerManager(playerManager);
    }
}
