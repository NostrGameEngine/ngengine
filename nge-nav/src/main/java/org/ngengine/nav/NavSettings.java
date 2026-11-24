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

public final class NavSettings {

    final float cellSize;
    final int chunkSize;
    final int maxSearchNodes;
    final int maxPathPoints;
    final int maxPendingJobs;
    final int localChecks;
    final int maxNearestNodes;
    final boolean planar;
    final boolean autoLoadPages;
    final Vector3f origin;
    final Vector3f axisX;
    final Vector3f axisY;
    final Vector3f axisZ;

    private NavSettings(Builder builder) {
        cellSize = builder.cellSize;
        chunkSize = builder.chunkSize;
        maxSearchNodes = builder.maxSearchNodes;
        maxPathPoints = builder.maxPathPoints;
        maxPendingJobs = builder.maxPendingJobs;
        localChecks = builder.localChecks;
        maxNearestNodes = builder.maxNearestNodes;
        planar = builder.planar;
        autoLoadPages = builder.autoLoadPages;
        origin = builder.origin.clone();
        axisX = builder.axisX.clone();
        axisY = builder.axisY.clone();
        axisZ = builder.axisZ.clone();
    }

    public static Builder builder() {
        return new Builder();
    }

    public float getCellSize() {
        return cellSize;
    }

    public int getChunkSize() {
        return chunkSize;
    }

    public boolean isAutoLoadPages() {
        return autoLoadPages;
    }

    public boolean isPlanar() {
        return planar;
    }

    Vector3f toLocal(Vector3f point, Vector3f store) {
        float x = point.x - origin.x, y = point.y - origin.y, z = point.z - origin.z;
        return store.set(
            x * axisX.x + y * axisX.y + z * axisX.z,
            x * axisY.x + y * axisY.y + z * axisY.z,
            x * axisZ.x + y * axisZ.y + z * axisZ.z
        );
    }

    Vector3f toWorld(float x, float y, float z, Vector3f store) {
        return store.set(
            origin.x + axisX.x * x + axisY.x * y + axisZ.x * z,
            origin.y + axisX.y * x + axisY.y * y + axisZ.y * z,
            origin.z + axisX.z * x + axisY.z * y + axisZ.z * z
        );
    }

    Vector3f normalToWorld(float x, float y, float z, Vector3f store) {
        return store.set(
            axisX.x * x + axisY.x * y + axisZ.x * z,
            axisX.y * x + axisY.y * y + axisZ.y * z,
            axisX.z * x + axisY.z * y + axisZ.z * z
        );
    }

    public static final class Builder {

        private float cellSize = 0.5f;
        private int chunkSize = 8;
        private int maxSearchNodes = 8192;
        private int maxPathPoints = 2048;
        private int maxPendingJobs = 32;
        private int localChecks = 128;
        private int maxNearestNodes = 4096;
        private boolean planar;
        private boolean autoLoadPages = true;
        private final Vector3f origin = new Vector3f();
        private final Vector3f axisX = new Vector3f(1, 0, 0);
        private final Vector3f axisY = new Vector3f(0, 1, 0);
        private final Vector3f axisZ = new Vector3f(0, 0, 1);

        public Builder cellSize(float value) {
            if (!Float.isFinite(value) || value <= 0) throw new IllegalArgumentException("cellSize must be positive");
            cellSize = value;
            return this;
        }

        public Builder chunkSize(int value) {
            if (value < 2 || value > 16) throw new IllegalArgumentException("chunkSize must be in [2, 16]");
            chunkSize = value;
            return this;
        }

        public Builder searchBudget(int nodes, int pathPoints) {
            if (nodes < 16 || pathPoints < 2) throw new IllegalArgumentException("Invalid search budget");
            maxSearchNodes = nodes;
            maxPathPoints = pathPoints;
            return this;
        }

        public Builder jobCapacity(int value) {
            if (value < 1) throw new IllegalArgumentException("jobCapacity must be positive");
            maxPendingJobs = value;
            return this;
        }

        public Builder localBudget(int checks, int nearestNodes) {
            if (checks < 1 || nearestNodes < 1) throw new IllegalArgumentException("Invalid local budget");
            localChecks = checks;
            maxNearestNodes = nearestNodes;
            return this;
        }

        /**
         * Disables automatic page requests from geometry when the game owns the streaming domain.
         */
        public Builder autoLoadPages(boolean value) {
            autoLoadPages = value;
            return this;
        }

        public Builder plane(Vector3f point, Vector3f normal) {
            if (!Vector3f.isValidVector(point) || !Vector3f.isValidVector(normal) || normal.lengthSquared() < 0.000001f) {
                throw new IllegalArgumentException("Invalid plane");
            }
            planar = true;
            origin.set(point);
            axisZ.set(normal).normalizeLocal();
            axisX.set(Math.abs(axisZ.x) < 0.8f ? Vector3f.UNIT_X : Vector3f.UNIT_Y);
            axisX.subtractLocal(axisZ.mult(axisX.dot(axisZ), axisY)).normalizeLocal();
            axisZ.cross(axisX, axisY).normalizeLocal();
            return this;
        }

        public NavSettings build() {
            return new NavSettings(this);
        }
    }
}
