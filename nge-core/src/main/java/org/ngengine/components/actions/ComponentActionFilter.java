package org.ngengine.components.actions;

/**
 * Bit flags used by {@link ComponentAction#filter()}.
 */
public final class ComponentActionFilter {
    private ComponentActionFilter() {}

    /** Handler accepts locally-originated actions. */
    public static final int LOCAL = 1;
    /** Handler accepts remotely-originated actions. */
    public static final int REMOTE = 1 << 1;
    /** Handler requires authority on the local target entry. */
    public static final int LOCAL_PEER_HAS_AUTHORITY = 1 << 2;
    /** Remotely-originated actions are accepted only from an authoritative peer. */
    public static final int REMOTE_PEER_HAS_AUTHORITY = 1 << 3;
}
