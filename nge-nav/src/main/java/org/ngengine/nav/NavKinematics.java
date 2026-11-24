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

public final class NavKinematics {

    final float acceleration;
    final float braking;
    final float turnRate;
    final float timeWeight;
    final float terminalSpeed;

    private NavKinematics(Builder builder) {
        acceleration = builder.acceleration;
        braking = builder.braking;
        turnRate = builder.turnRate;
        timeWeight = builder.timeWeight;
        terminalSpeed = builder.terminalSpeed;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {

        private float acceleration = Float.POSITIVE_INFINITY;
        private float braking = Float.POSITIVE_INFINITY;
        private float turnRate = Float.POSITIVE_INFINITY;
        private float timeWeight;
        private float terminalSpeed = Float.POSITIVE_INFINITY;

        public Builder acceleration(float value) {
            acceleration = positive(value);
            return this;
        }

        public Builder braking(float value) {
            braking = positive(value);
            return this;
        }

        public Builder turnRate(float radiansPerSecond) {
            turnRate = positive(radiansPerSecond);
            return this;
        }

        public Builder timeWeight(float value) {
            if (!Float.isFinite(value) || value < 0) throw new IllegalArgumentException("Invalid time weight");
            timeWeight = value;
            return this;
        }

        public Builder terminalSpeed(float value) {
            if (!Float.isFinite(value) || value < 0) throw new IllegalArgumentException("Invalid terminal speed");
            terminalSpeed = value;
            return this;
        }

        public NavKinematics build() {
            return new NavKinematics(this);
        }

        private static float positive(float value) {
            if (Float.isNaN(value) || value <= 0) throw new IllegalArgumentException("Motion limit must be positive");
            return value;
        }
    }
}
