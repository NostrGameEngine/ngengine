package org.ngengine.network.components;

import org.ngengine.network.protocol.NetworkSafe;

@NetworkSafe
public class NetcodeDespawnActionMessage extends ActionMessage {
    /**
     * Entity removal is a terminal state transition and must not be lost.
     * Replicas do not independently age authoritative entities, so an
     * unreliable despawn could otherwise leave a permanent ghost behind.
     */
    public NetcodeDespawnActionMessage() {
        setReliable(true);
    }
}
