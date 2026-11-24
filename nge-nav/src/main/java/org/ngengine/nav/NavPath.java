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

import com.jme3.math.Vector3f;
import java.nio.FloatBuffer;
import java.nio.LongBuffer;

/**
 * Immutable search result. Points describe body centers, including surface clearance.
 */
public final class NavPath {

    final NavWorld world;
    final long policyRevision;
    final long revision, surfaceRevision;
    final LongBuffer references;
    final LongBuffer links;
    final FloatBuffer points;
    final NavReason reason;
    final double cost;

    NavPath(
        NavWorld world,
        long policyRevision,
        long revision,
        long surfaceRevision,
        LongBuffer references,
        LongBuffer links,
        FloatBuffer points,
        NavReason reason,
        double cost
    ) {
        this.world = world;
        this.policyRevision = policyRevision;
        this.revision = revision; this.surfaceRevision = surfaceRevision;
        this.references = references;
        this.links = links;
        this.points = points;
        this.reason = reason;
        this.cost = cost;
    }

    public boolean isComplete() {
        return reason == NavReason.NONE;
    }

    public NavReason getReason() {
        return reason;
    }

    public long getSurfaceRevision() { return surfaceRevision; }

    public long getRevision() {
        return revision;
    }

    public double getCost() {
        return cost;
    }

    public int size() {
        return references == null ? 0 : references.capacity();
    }

    public NavMode getMode(int index) {
        return NavSnapshot.mode(references.get(index));
    }

    public long getTraversalId(int index) {
        return links.get(index);
    }

    public Vector3f getPoint(int index, Vector3f store) {
        return store.set(points.get(index * 3), points.get(index * 3 + 1), points.get(index * 3 + 2));
    }
}
