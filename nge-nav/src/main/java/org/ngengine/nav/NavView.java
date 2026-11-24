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

import com.jme3.math.FastMath;
import com.jme3.math.Vector3f;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Immutable actor policy over shared navigation geometry.
 */
public final class NavView {

    private static final AtomicLong NEXT_REVISION = new AtomicLong();
    final NavWorld world;
    final long revision;
    final float radius;
    final float height;
    final float cutoff;
    final float maxSlope;
    final float maxBend;
    final float arrivalDistance;
    final float nearestDistance;
    final float stepHeight;
    final boolean extendedSteps;
    final float contactSkin;
    final float supportMargin;
    final long ignoredMovableObstacle;
    final Vector3f up;
    final boolean planeSupport;
    final int modes;
    final float[] costs;
    final float[] speeds;
    final float[] slopes;
    final float[] areas;
    final NavKinematics kinematics;
    final NavCostPolicy policy;

    private NavView(Builder builder) {
        world = builder.world;
        revision = NEXT_REVISION.incrementAndGet();
        radius = builder.radius;
        height = builder.height;
        cutoff = builder.cutoff;
        maxSlope = builder.maxSlope * FastMath.DEG_TO_RAD;
        maxBend = builder.maxBend * FastMath.DEG_TO_RAD;
        arrivalDistance = builder.arrivalDistance;
        nearestDistance = builder.nearestDistance;
        stepHeight = builder.stepHeight;
        extendedSteps = builder.extendedSteps;
        contactSkin = builder.contactSkin;
        supportMargin = builder.supportMargin;
        ignoredMovableObstacle = builder.ignoredMovableObstacle;
        up = builder.up.clone();
        if (world.settings.planar) {
            float perpendicular = up.dot(world.settings.axisZ);
            up.addLocal(
                -world.settings.axisZ.x * perpendicular,
                -world.settings.axisZ.y * perpendicular,
                -world.settings.axisZ.z * perpendicular
            );
            if (up.lengthSquared() > 0.000001f) up.normalizeLocal(); else up.set(builder.up);
        }
        planeSupport = world.settings.planar && Math.abs(up.dot(world.settings.axisZ)) > 0.999f;
        modes = builder.modes;
        costs = builder.costs.clone();
        for (int i = 0; i < costs.length; i++) costs[i] = Math.max(0, costs[i] + builder.preferences[i]);
        speeds = builder.speeds.clone();
        slopes = builder.slopes.clone();
        areas = builder.areas.clone();
        kinematics = builder.kinematics;
        policy = builder.policy;
    }

    public NavNavigator navigator() {
        return new NavNavigator(this);
    }

    /**
     * Creates guidance with continuous local steering. The follower belongs to this navigator.
     */
    public NavNavigator navigator(NavPathFollower follower) {
        if (follower == null) throw new IllegalArgumentException("Missing path follower");
        return new NavNavigator(this, follower);
    }

    public Builder copy() {
        return new Builder(this);
    }

    public long getRevision() {
        return revision;
    }

    public float getCostCutoff() {
        return cutoff;
    }

    public float getRadius() {
        return radius;
    }

    public float getHeight() {
        return height;
    }

    public float getContactSkin() {
        return contactSkin;
    }

    public NavKinematics getKinematics() {
        return kinematics;
    }

    public boolean allows(NavMode mode) {
        return (modes & mode.mask()) != 0;
    }

    public float evaluate(NavMode mode, int area, float inclination, float ascent) {
        if (
            !allows(mode) ||
            !Float.isFinite(inclination) ||
            !Float.isFinite(ascent) ||
            (mode == NavMode.WALK && (inclination > maxSlope || inclination >= FastMath.HALF_PI))
        ) return Float.POSITIVE_INFINITY;
        int m = mode.ordinal();
        float value = costs[m] + slopes[m] * inclination + areas[area & 255];
        if (policy != null) value = policy.cost(mode, area, inclination, ascent, value);
        return Float.isFinite(value) && value >= 0 && value <= cutoff ? value : Float.POSITIVE_INFINITY;
    }

    public float getSpeed(NavMode mode) {
        return speeds[mode.ordinal()];
    }

    public NavPath findPath(Vector3f start, Vector3f target) {
        return world.findPath(this, start, target);
    }

    /** A failed search may retain a safe corridor to the closest reachable point. Check isComplete before claiming arrival. */
    public NavPath findClosestPath(Vector3f start, Vector3f target) {
        return world.findClosestPath(this, start, target);
    }

    public boolean nearestPoint(Vector3f position, Vector3f store) {
        return world.nearestPoint(this, position, store);
    }

    public int nearestArea(Vector3f position) {
        return world.nearestArea(this, position);
    }

    /**
     * Checks a straight capsule sweep against published geometry, using this view's up axis.
     * Missing pages and checks exceeding the local query budget return false. This checks
     * clearance only; callers must separately validate support and movement permissions.
     */
    public boolean isSegmentClear(Vector3f from, Vector3f to) {
        return world.isSegmentClear(this, from, to);
    }

    /** Checks continuous floor support and capsule clearance, within the local query budget. */
    public boolean isWalkableSegment(Vector3f from, Vector3f to) {
        return world.isWalkableSegment(this, from, to);
    }

    public boolean randomPoint(long seed, Vector3f store) {
        return world.randomPoint(this, seed, store);
    }

    public static final class Builder {

        private final NavWorld world;
        private float radius = 0.2f;
        private float height = 1.8f;
        private float cutoff = Float.POSITIVE_INFINITY;
        private float maxSlope = 50;
        private float maxBend = 100;
        private float arrivalDistance = 0.15f;
        private float nearestDistance = 3;
        private float stepHeight = 0.5f;
        private boolean extendedSteps;
        private float contactSkin;
        private float supportMargin;
        private long ignoredMovableObstacle;
        private final Vector3f up = new Vector3f(0, 1, 0);
        private int modes = NavMode.WALK.mask();
        private final float[] costs = new float[NavMode.ALL.length];
        private final float[] speeds = new float[NavMode.ALL.length];
        private final float[] preferences = new float[NavMode.ALL.length];
        private final float[] slopes = new float[NavMode.ALL.length];
        private final float[] areas = new float[256];
        private NavKinematics kinematics;
        private NavCostPolicy policy;

        Builder(NavWorld world) {
            this.world = world;
            Arrays.fill(costs, 1);
            Arrays.fill(speeds, 3);
            if (world.settings.planar) up.set(world.settings.axisZ);
        }

        private Builder(NavView view) {
            world = view.world;
            radius = view.radius;
            height = view.height;
            cutoff = view.cutoff;
            maxSlope = view.maxSlope * FastMath.RAD_TO_DEG;
            maxBend = view.maxBend * FastMath.RAD_TO_DEG;
            arrivalDistance = view.arrivalDistance;
            nearestDistance = view.nearestDistance;
            stepHeight = view.stepHeight;
            extendedSteps = view.extendedSteps;
            contactSkin = view.contactSkin;
            supportMargin = view.supportMargin;
            ignoredMovableObstacle = view.ignoredMovableObstacle;
            up.set(view.up);
            modes = view.modes;
            System.arraycopy(view.costs, 0, costs, 0, costs.length);
            System.arraycopy(view.speeds, 0, speeds, 0, speeds.length);
            System.arraycopy(view.slopes, 0, slopes, 0, slopes.length);
            System.arraycopy(view.areas, 0, areas, 0, areas.length);
            kinematics = view.kinematics;
            policy = view.policy;
        }

        /** Excludes only the named manipulation target from clearance checks. Zero restores ordinary collision. */
        public Builder ignoreMovableObstacle(long identity) {
            if (identity < 0) throw new IllegalArgumentException("Invalid obstacle identity");
            ignoredMovableObstacle = identity; return this;
        }

        public Builder radius(float value) {
            radius = nonnegative(value);
            return this;
        }

        public Builder height(float value) {
            height = nonnegative(value);
            return this;
        }

        public Builder costCutoff(float value) {
            if (Float.isNaN(value) || value < 0) throw new IllegalArgumentException("Invalid cutoff");
            cutoff = value;
            return this;
        }

        public Builder allow(NavMode... values) {
            modes = 0;
            for (NavMode mode : values) modes |= mode.mask();
            return this;
        }

        public Builder cost(NavMode mode, float value) {
            costs[mode.ordinal()] = nonnegative(value);
            return this;
        }

        /**
         * Signed bias applied to the base cost; the resulting cost is clamped to zero.
         */
        public Builder preference(NavMode mode, float bias) {
            if (!Float.isFinite(bias)) throw new IllegalArgumentException("Non-finite preference");
            preferences[mode.ordinal()] = bias;
            return this;
        }

        public Builder speed(NavMode mode, float value) {
            speeds[mode.ordinal()] = nonnegative(value);
            return this;
        }

        public Builder slopeCost(NavMode mode, float costPerRadian) {
            slopes[mode.ordinal()] = nonnegative(costPerRadian);
            return this;
        }

        public Builder areaCost(int area, float value) {
            if (area < 0 || area > 255) throw new IllegalArgumentException("Area must be in [0, 255]");
            areas[area] = nonnegative(value);
            return this;
        }

        public Builder maxSlope(float degrees) {
            maxSlope = angle(degrees);
            return this;
        }

        public Builder maxBend(float degrees) {
            maxBend = angle(degrees);
            return this;
        }

        public Builder stepHeight(float value) {
            stepHeight = nonnegative(value);
            return this;
        }

        /** Allow a stepping controller to cross clearance gaps wider than one voxel. */
        public Builder extendedSteps(boolean enabled) {
            extendedSteps = enabled;
            return this;
        }

        /**
         * Permitted physics contact penetration in world units. Zero retains
         * strict geometry tests. Keep this small relative to the agent radius.
         */
        public Builder contactSkin(float value) {
            contactSkin = nonnegative(value);
            return this;
        }

        public Builder arrivalDistance(float value) {
            arrivalDistance = nonnegative(value);
            return this;
        }

        public Builder nearestDistance(float value) {
            nearestDistance = nonnegative(value);
            return this;
        }

        /** Minimum supported margin around the walking center, independent of collision clearance. */
        public Builder supportMargin(float value) {
            supportMargin = nonnegative(value);
            return this;
        }

        public Builder up(Vector3f value) {
            if (!Vector3f.isValidVector(value) || value.lengthSquared() < 0.000001f) throw new IllegalArgumentException(
                "Invalid up direction"
            );
            up.set(value).normalizeLocal();
            return this;
        }

        public Builder kinematics(NavKinematics value) {
            kinematics = value;
            return this;
        }

        public Builder policy(NavCostPolicy value) {
            policy = value;
            return this;
        }

        public NavView build() {
            if (height < 2 * radius) throw new IllegalArgumentException("Height must cover body diameter");
            if (contactSkin > radius) throw new IllegalArgumentException("Contact skin must not exceed body radius");
            return new NavView(this);
        }

        private static float nonnegative(float value) {
            if (!Float.isFinite(value) || value < 0) throw new IllegalArgumentException("Expected a finite nonnegative value");
            return value;
        }

        private static float angle(float value) {
            if (!Float.isFinite(value) || value < 0 || value > 180) throw new IllegalArgumentException(
                "Angle must be in [0, 180]"
            );
            return value;
        }
    }
}
