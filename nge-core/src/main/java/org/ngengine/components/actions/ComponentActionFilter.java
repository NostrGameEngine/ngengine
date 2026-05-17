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
    /** Handler requires authority on the target entry. */
    public static final int WITH_AUTHORITY = 1 << 2;
    /** Handler requires no authority on the target entry. */
    public static final int WITHOUT_AUTHORITY = 1 << 3;
}
