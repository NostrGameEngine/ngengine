package org.ngengine.network.components;

import org.ngengine.network.RemotePeer;

import jakarta.annotation.Nullable;

/**
 * Strategy interface for runtime spawn/despawn resolution of netcode targets.
 */
public interface NetcodeSpawner {
    /**
     * Resolves (or creates) a {@link NetcodeFragment} target for an inbound snapshot.
     */
    @Nullable NetcodeFragment spawn(NetcodeManagerComponent manager, SnapshotMessage snapshot);

    /**
     * Handles an inbound despawn request.
     */
    void despawn(
        NetcodeManagerComponent manager,
        @Nullable NetcodeFragment target
    );
}
