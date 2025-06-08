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
package org.ngengine.demo.son;

import com.jme3.system.AppSettings;
import java.util.List;
import java.util.function.Consumer;
import org.ngengine.auth.AuthStrategy;
import org.ngengine.auth.Nip46AuthStrategy;
import org.ngengine.nostr4j.nip46.Nip46AppMetadata;
import org.ngengine.nostr4j.signer.NostrSigner;
import org.ngengine.platform.NGEPlatform;
import org.ngengine.platform.VStore;
import org.ngengine.player.PlayerManagerComponent;

public class Defaults {

    public static final List<String> AUTH_RELAYS = List.of("wss://relay.nsec.app", "wss://relay.ngengine.org");
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

    public static final AuthStrategy authStrategy(
        AppSettings settings,
        Consumer<NostrSigner> callback,
        PlayerManagerComponent playerManager
    ) {
        VStore authStore = NGEPlatform.get().getDataStore(settings.getTitle(), "auth");
        return new AuthStrategy(callback)
            .enableStore(authStore)
            .enableNip46RemoteIdentity(new Nip46AuthStrategy().setMetadata(NIP46_METADATA))
            .enableLocalIdentity()
            .setPlayerManager(playerManager);
    }
}
