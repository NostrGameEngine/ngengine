/**
 * Copyright (c) 2025-2026, Nostr Game Engine
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * 3. Neither the name of the copyright holder nor the names of its
 *    contributors may be used to endorse or promote products derived from
 *    this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 * Nostr Game Engine is a fork of the jMonkeyEngine, which is licensed under
 * the BSD 3-Clause License.
 */

package org.ngengine.nav;

import com.jme3.scene.Geometry;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

public final class NavLayer implements AutoCloseable {

    final NavWorld world;
    final String name;
    final ArrayList<NavSource> sources = new ArrayList<>();
    final Set<NavChunkKey> importedPages = new HashSet<>();
    boolean attached = true;

    NavLayer(NavWorld world, String name) {
        this.world = world;
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void attach(NavChunk page) {
        world.attach(this, page);
    }

    public void unload(NavChunkKey key) {
        world.unload(key);
    }

    public NavSource add(NavGeometry geometry) {
        return world.add(this, geometry);
    }

    public NavSource add(Geometry geometry, boolean closedSolid, int area, int flags) {
        return add(NavGeometry.capture(geometry, closedSolid, area, flags));
    }

    void requireAttached() {
        if (!attached) throw new IllegalStateException("Layer is detached");
    }

    @Override
    public void close() {
        world.detach(this);
    }
}
