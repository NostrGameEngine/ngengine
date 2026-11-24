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
 * Reusable, clearance-checked STEP trajectory, parameterized by travelled distance.
 * The game owns time, speed, body movement and action acknowledgement.
 */
public final class NavStepTrajectory {

    private static final int SEGMENTS = 16;
    private final float[] points = new float[(SEGMENTS + 1) * 3];
    private final float[] distances = new float[SEGMENTS + 1];
    private final Vector3f from = new Vector3f();
    private final Vector3f to = new Vector3f();
    private final Vector3f a = new Vector3f();
    private final Vector3f b = new Vector3f();
    private NavView view;
    private int count;

    /**
     * Tries a direct crossing, then a shallow raised arc. Tight geometry retains the
     * navigator's validated raised crossing instead of forcing a curve through an obstacle.
     */
    public boolean begin(NavView view, Vector3f position, NavStep step) {
        this.view = null;
        count = 0;
        if (
            view == null ||
            !Vector3f.isValidVector(position) ||
            step.getStatus() != NavStatus.TRANSITION ||
            step.getAction() != NavAction.STEP
        ) return false;
        NavSnapshot snapshot = view.world.snapshot;
        from.set(position);
        step.getNextPosition(to);
        float height = (to.x - from.x) * view.up.x + (to.y - from.y) * view.up.y + (to.z - from.z) * view.up.z;
        for (int attempt = 0; attempt < 3; attempt++) {
            count = attempt == 2 ? 4 : SEGMENTS + 1;
            boolean clear = true;
            distances[0] = 0;
            a.set(from);
            for (int i = 0; i < count; i++) {
                float t = (float) i / (count - 1);
                if (attempt < 2) {
                    b.interpolateLocal(from, to, t);
                    if (attempt == 1) {
                        float lift = Math.abs(height) + step.getStepLift() * 2 + view.radius * 0.25f;
                        b.addLocal(
                            view.up.x * lift * 4 * t * (1 - t),
                            view.up.y * lift * 4 * t * (1 - t),
                            view.up.z * lift * 4 * t * (1 - t)
                        );
                    }
                } else {
                    b.set(i < 2 ? from : to);
                    float lift = i == 1
                        ? Math.max(0, height) + step.getStepLift()
                        : i == 2 ? Math.max(0, -height) + step.getStepLift() : 0;
                    b.addLocal(view.up.x * lift, view.up.y * lift, view.up.z * lift);
                }
                if (i > 0 && !NavCollision.sweep(snapshot, view, a, b, view.up, view.up, view.world.settings.localChecks)) {
                    clear = false;
                    break;
                }
                points[i * 3] = b.x;
                points[i * 3 + 1] = b.y;
                points[i * 3 + 2] = b.z;
                if (i > 0) distances[i] = distances[i - 1] + a.distance(b);
                a.set(b);
            }
            if (clear && snapshot == view.world.snapshot) {
                this.view = view;
                return true;
            }
        }
        count = 0;
        return false;
    }

    public float getLength() {
        return count == 0 ? 0 : distances[count - 1];
    }

    /**
     * Samples a body-center target and checks the actual move against the current snapshot.
     * On failure store is unchanged; the controller should fail the pending action and replan.
     */
    public boolean sample(float distance, Vector3f position, Vector3f store) {
        if (view == null || !Float.isFinite(distance) || distance < 0 || !Vector3f.isValidVector(position)) return false;
        int i = 1;
        while (i < count - 1 && distances[i] < distance) i++;
        float length = distances[i] - distances[i - 1];
        float t = length <= 1e-8f ? 1 : Math.min(1, (distance - distances[i - 1]) / length);
        int p = (i - 1) * 3;
        a.set(points[p], points[p + 1], points[p + 2]);
        b.set(points[p + 3], points[p + 4], points[p + 5]);
        a.interpolateLocal(b, t);
        NavSnapshot snapshot = view.world.snapshot;
        if (
            !NavCollision.sweep(snapshot, view, position, a, view.up, view.up, view.world.settings.localChecks) ||
            view.world.snapshot != snapshot
        ) return false;
        store.set(a);
        return true;
    }
}
