package org.ngengine.network.protocol;

/**
 * Marker interface for messages that can be diff-compressed by the protocol.
 */
public interface DiffableMessage {

    /**
     * Returns a stable logical group identifier for diff streams.
     */
    long getDiffGroup();

    boolean equals(Object obj);
    int hashCode();
}
