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
import java.util.concurrent.atomic.AtomicLong;

/**
 * Authored traversal between body-center positions. The game executes and acknowledges it.
 */
public final class NavLink {

    private static final AtomicLong NEXT_ID = new AtomicLong();
    final long id;
    final Vector3f from;
    final Vector3f to;
    final NavMode mode;
    final float radius;
    final float difficulty;
    final float cost;
    final float duration;
    final float arcHeight;
    final boolean bidirectional;

    public NavLink(
        Vector3f from,
        Vector3f to,
        NavMode mode,
        float radius,
        float difficulty,
        float cost,
        float duration,
        float arcHeight,
        boolean bidirectional
    ) {
        if (
            !Vector3f.isValidVector(from) ||
            !Vector3f.isValidVector(to) ||
            mode == null ||
            !Float.isFinite(radius) ||
            radius <= 0 ||
            !Float.isFinite(difficulty) ||
            difficulty < 0 ||
            !Float.isFinite(cost) ||
            cost < 0 ||
            !Float.isFinite(duration) ||
            duration < 0 ||
            !Float.isFinite(arcHeight) ||
            arcHeight < 0
        ) throw new IllegalArgumentException("Invalid traversal link");
        if (mode == NavMode.WALK || mode == NavMode.CLIMB || mode == NavMode.FLY) {
            throw new IllegalArgumentException("Surface and flight transitions are generated automatically");
        }
        id = NEXT_ID.incrementAndGet();
        this.from = from.clone();
        this.to = to.clone();
        this.mode = mode;
        this.radius = radius;
        this.difficulty = difficulty;
        this.cost = cost;
        this.duration = duration;
        this.arcHeight = arcHeight;
        this.bidirectional = bidirectional;
    }

    public static NavLink teleport(Vector3f from, Vector3f to, float radius, float cost, boolean bidirectional) {
        return new NavLink(from, to, NavMode.TELEPORT, radius, 0, cost, 0, 0, bidirectional);
    }

    public static NavLink jump(Vector3f from, Vector3f to, float radius, float height, float duration) {
        return new NavLink(from, to, NavMode.JUMP, radius, 0, 1, duration, height, false);
    }

    public float getDuration() {
        return duration;
    }

    public float getArcHeight() {
        return arcHeight;
    }

    public float getRadius() {
        return radius;
    }

    public float getDifficulty() {
        return difficulty;
    }

    public float getCost() {
        return cost;
    }

    public boolean isBidirectional() {
        return bidirectional;
    }

    public long getId() {
        return id;
    }

    public NavMode getMode() {
        return mode;
    }

    public Vector3f getFrom(Vector3f store) {
        return store.set(from);
    }

    public Vector3f getTo(Vector3f store) {
        return store.set(to);
    }
}
