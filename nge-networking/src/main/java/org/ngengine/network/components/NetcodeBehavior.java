package org.ngengine.network.components;

import java.time.Duration;

public final class NetcodeBehavior {
    public static final NetcodeBehavior DEFAULT = new NetcodeBehavior(Duration.ofMillis(1000 / 25));

    private final Duration snapshotInterval;

    public NetcodeBehavior(Duration snapshotInterval) {
        this.snapshotInterval = snapshotInterval;
    }

    public Duration getSnapshotInterval() {
        return snapshotInterval;
    }
}
