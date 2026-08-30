package org.ngengine.network.components;

import org.ngengine.network.RemotePeer;

import com.jme3.network.Message;

/** Receives a typed game message on the application logic thread. */
@FunctionalInterface
public interface NetworkMessageHandler<T extends Message> {
    /**
     * Handles a message received from a remote peer.
     *
     * @param source authenticated peer that sent the message
     * @param message decoded message
     */
    void onMessage(RemotePeer source, T message);
}
