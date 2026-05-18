package org.ngengine.network.components;

import org.ngengine.network.protocol.DiffableMessage;

public abstract class SnapshotMessage extends ActionMessage implements DiffableMessage {

    public SnapshotMessage() {
        this.setChannel(2);
    }

    @Override
    public long getDiffGroup() {
        long hash = 0xcbf29ce484222325L;
        hash = hashString(hash, getClass().getName());
        hash = hashString(hash, getComponentId());
        hash = hashString(hash, getNetworkId() != null ? getNetworkId().toString() : null);
        return hash;
    }

    private static long hashString(long seed, String value) {
        if (value == null || value.isEmpty()) {
            return (seed ^ 0x9e3779b97f4a7c15L) * 0x100000001b3L;
        }
        long hash = seed;
        for (int i = 0; i < value.length(); i++) {
            hash ^= value.charAt(i);
            hash *= 0x100000001b3L;
        }
        return hash;
    }
}
