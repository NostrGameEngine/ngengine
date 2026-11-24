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

/**
 * Caller-owned measured motion. Positions are body centers in world coordinates.
 */
public final class NavMotion {

    public final Vector3f position = new Vector3f();
    public final Vector3f forward = new Vector3f(0, 0, 1);
    public final Vector3f up = new Vector3f(0, 1, 0);
    public final Vector3f velocity = new Vector3f();
    public NavMode mode = NavMode.WALK;
    public double time;

    public NavMotion set(Vector3f position, Vector3f forward, Vector3f velocity, double time) {
        this.position.set(position);
        this.forward.set(forward);
        this.velocity.set(velocity);
        this.time = time;
        return this;
    }

    boolean isValid() {
        return (
            Vector3f.isValidVector(up) &&
            Math.abs(up.lengthSquared() - 1) < 0.001f &&
            Vector3f.isValidVector(position) &&
            Vector3f.isValidVector(forward) &&
            Vector3f.isValidVector(velocity) &&
            Double.isFinite(time) &&
            mode != null
        );
    }
}
