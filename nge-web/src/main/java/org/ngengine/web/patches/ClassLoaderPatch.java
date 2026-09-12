/*
 * Copyright (c) 2025-2026, Nostr Game Engine
 * All rights reserved.
 */
package org.ngengine.web.patches;

import java.io.IOException;
import java.net.URL;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Objects;

/** Compatibility for optional class-path scanners in web builds. */
public class ClassLoaderPatch {

    /**
     * No JVM class path is available in the browser. Engine assets are resolved
     * separately by WebResourceLoader, not by this class-loader enumeration.
     */
    public Enumeration<URL> getResources(String name) throws IOException {
        Objects.requireNonNull(name, "name");
        return Collections.emptyEnumeration();
    }
}
