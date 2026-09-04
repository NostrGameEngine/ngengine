/**
 * Copyright (c) 2025-2026, Nostr Game Engine
 * All rights reserved.
 */
package org.ngengine.web.patches;

import org.ngengine.web.context.WebContext;

/** Maps process termination to orderly shutdown of the active browser game context. */
public final class SystemPatch {

    private SystemPatch() {
    }

    public static void exit(int status) {
        WebContext.requestExit();
    }
}
