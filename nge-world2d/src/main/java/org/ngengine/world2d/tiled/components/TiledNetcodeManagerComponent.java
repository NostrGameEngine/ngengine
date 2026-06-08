package org.ngengine.world2d.tiled.components;

import org.ngengine.network.components.NetcodeManagerComponent;
import org.ngengine.network.components.NetcodeSpawner;
import org.ngengine.nostr4j.signer.NostrSigner;

import jakarta.annotation.Nullable;

public class TiledNetcodeManagerComponent extends NetcodeManagerComponent {
    public TiledNetcodeManagerComponent() {
        super(new TiledNetcodeSpawner());
    }

    public TiledNetcodeManagerComponent(@Nullable NetcodeSpawner spawner) {
        super(spawner != null ? spawner : new TiledNetcodeSpawner());
    }

    public TiledNetcodeManagerComponent(@Nullable NetcodeSpawner spawner, @Nullable NostrSigner signer) {
        super(spawner != null ? spawner : new TiledNetcodeSpawner(), signer);
    }
}
