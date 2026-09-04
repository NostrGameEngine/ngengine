/**
 * Copyright (c) 2025-2026, Nostr Game Engine
 * All rights reserved.
 */
package org.ngengine.web;

import java.util.Set;
import org.teavm.dependency.AbstractDependencyListener;
import org.teavm.dependency.DependencyAgent;

/**
 * Makes metadata-only application types visible to TeaVM dependency analysis.
 *
 * <p>A reflection supplier describes members after TeaVM reaches a class, but
 * Tiled components and similar extension points can be referenced only by a
 * string in an asset. Linking the generated extension roots keeps those types
 * alive even when no Java call site references them. Other reflected classes
 * retain normal reachability, matching Native Image metadata semantics.</p>
 */
final class NgeReflectionReachabilityListener extends AbstractDependencyListener {
    private final ClassLoader pluginClassLoader;

    NgeReflectionReachabilityListener(ClassLoader pluginClassLoader) {
        this.pluginClassLoader = pluginClassLoader;
    }

    @Override
    public void started(DependencyAgent agent) {
        Set<String> classNames = NgeReflectionMetadata.loadRoots(
                pluginClassLoader,
                agent.getClassLoader());
        for (String className : classNames) {
            if (agent.getClassSource().get(className) != null) {
                agent.linkClass(className);
            }
        }
    }
}
