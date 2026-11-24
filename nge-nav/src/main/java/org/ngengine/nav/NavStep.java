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
 * Reusable guidance output. A YIELD result contains no current movement command.
 */
public final class NavStep {

    private NavStatus status = NavStatus.YIELD;
    private NavReason reason = NavReason.NO_TARGET;
    private final Vector3f desiredVelocity = new Vector3f();
    private final Vector3f supportNormal = new Vector3f(0, 1, 0);
    private final Vector3f nextPosition = new Vector3f();
    private NavMode mode = NavMode.WALK;
    private long revision;
    private long policyRevision;
    private long actionId;
    private long traversalId;
    private float horizon;
    private float stepLift;
    private boolean positionCorrection;
    private NavAction action = NavAction.NONE;

    public NavStatus getStatus() {
        return status;
    }

    public NavReason getReason() {
        return reason;
    }

    public NavMode getMode() {
        return mode;
    }

    public long getRevision() {
        return revision;
    }

    public long getPolicyRevision() {
        return policyRevision;
    }

    public long getActionId() {
        return actionId;
    }

    public long getTraversalId() {
        return traversalId;
    }

    public NavAction getAction() {
        return action;
    }

    /**
     * Extra clearance above the higher endpoint for an upright STEP crossing, along the view up axis.
     */
    public float getStepLift() {
        return stepLift;
    }

    public float getHorizon() {
        return horizon;
    }

    /**
     * Whether this MOVE aligns the body with the route's initial sample.
     * Controllers can preserve facing and their previous travel vector during this correction.
     */
    public boolean isPositionCorrection() {
        return positionCorrection;
    }

    public Vector3f getDesiredVelocity(Vector3f store) {
        return store.set(desiredVelocity);
    }

    public Vector3f getSupportNormal(Vector3f store) {
        return store.set(supportNormal);
    }

    public Vector3f getNextPosition(Vector3f store) {
        return store.set(nextPosition);
    }

    NavStep reset(NavStatus status, NavReason reason, long revision, long policyRevision) {
        this.status = status;
        this.reason = reason;
        this.revision = revision;
        this.policyRevision = policyRevision;
        desiredVelocity.set(0, 0, 0);
        nextPosition.set(0, 0, 0);
        supportNormal.set(0, 1, 0);
        actionId = traversalId = 0;
        horizon = stepLift = 0;
        action = NavAction.NONE;
        positionCorrection = false;
        return this;
    }

    void move(Vector3f velocity, Vector3f next, Vector3f normal, NavMode mode, float horizon, boolean positionCorrection) {
        status = NavStatus.MOVE;
        reason = NavReason.NONE;
        desiredVelocity.set(velocity);
        nextPosition.set(next);
        supportNormal.set(normal);
        this.mode = mode;
        this.horizon = horizon;
        this.positionCorrection = positionCorrection;
    }

    void transition(
        NavMode mode,
        NavAction action,
        long actionId,
        long traversalId,
        Vector3f next,
        Vector3f normal,
        float lift
    ) {
        status = NavStatus.TRANSITION;
        this.mode = mode;
        this.action = action;
        stepLift = lift;
        this.actionId = actionId;
        this.traversalId = traversalId;
        nextPosition.set(next);
        supportNormal.set(normal);
    }
}
