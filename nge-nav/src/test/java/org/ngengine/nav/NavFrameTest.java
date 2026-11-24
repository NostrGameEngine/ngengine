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

import static org.junit.jupiter.api.Assertions.*;
import static org.ngengine.nav.NavWorldTest.*;

import com.jme3.math.FastMath;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import org.junit.jupiter.api.Test;

class NavFrameTest {

    @Test
    void translatingAndRotatingFrameCarriesMotionWithoutRebuildingPages() {
        try (NavWorld world = floor()) {
            NavFrame frame = new NavFrame(world);
            NavView view = world.view().radius(0.15f).height(1).build();
            NavChunk page = world.getChunk(new NavChunkKey(0, 0, 0));
            long revision = world.getRevision();
            Vector3f from = v(-1.25f, 0.5f, -0.25f), to = v(1.25f, 0.5f, -0.25f);
            assertTrue(view.findPath(from, to).isComplete());
            frame.setPose(v(10, 0, 0), new Quaternion());
            frame.updatePose(v(12, 0, 0), new Quaternion().fromAngleAxis(FastMath.HALF_PI, Vector3f.UNIT_Y), 1);
            NavMotion local = new NavMotion(), global = new NavMotion(), roundTrip = new NavMotion();
            local.position.set(2, 0, 0);
            local.forward.set(Vector3f.UNIT_X);
            local.velocity.set(1, 0, 0);
            frame.toWorld(local, global);
            assertEquals(0, global.position.distance(v(12, 0, -2)), 0.0001f);
            assertEquals(0, global.forward.distance(v(0, 0, -1)), 0.0001f);
            assertEquals(0, global.velocity.distance(v(2 - FastMath.PI, 0, -1)), 0.0001f);
            frame.toLocal(global, roundTrip);
            assertEquals(0, roundTrip.position.distance(local.position), 0.0001f);
            assertEquals(0, roundTrip.velocity.distance(local.velocity), 0.0001f);
            assertEquals(0, roundTrip.forward.distance(local.forward), 0.0001f);
            frame.toWorld(local, local);
            frame.toLocal(local, local);
            assertEquals(0, local.position.distance(v(2, 0, 0)), 0.0001f);
            assertEquals(0, local.velocity.distance(v(1, 0, 0)), 0.0001f);
            assertTrue(view.findPath(from, to).isComplete());
            assertEquals(revision, world.getRevision());
            assertEquals(0, world.getPendingBuildCount());
            assertSame(page, world.getChunk(new NavChunkKey(0, 0, 0)));
        }
    }

    @Test
    void smallRotationsRetainAngularVelocityPrecisionAndRotateBodyAxes() {
        try (NavWorld world = floor()) {
            NavFrame frame = new NavFrame(world);
            frame.updatePose(Vector3f.ZERO, new Quaternion().fromAngleAxis(0.0001f, Vector3f.UNIT_Z), 0.01f);
            NavMotion local = new NavMotion(), global = new NavMotion();
            local.position.set(1, 0, 0);
            frame.toWorld(local, global);
            assertEquals(0.01f, global.velocity.y, 0.000001f);
            assertEquals(-0.000001f, global.velocity.x, 0.0000001f);
            assertEquals(-0.0001f, global.up.x, 0.000001f);
            frame.toLocal(global, global);
            assertEquals(0, global.up.distance(Vector3f.UNIT_Y), 0.000001f);
            assertEquals(0, global.velocity.length(), 0.000001f);
        }
    }

    @Test
    void oppositeQuaternionSignsDoNotIntroduceAngularVelocityAndTeleportsResetMotion() {
        try (NavWorld world = floor()) {
            NavFrame frame = new NavFrame(world);
            Quaternion rotation = new Quaternion().fromAngleAxis(0.7f, Vector3f.UNIT_Y);
            frame.setPose(v(4, 2, 1), rotation);
            frame.updatePose(v(4, 2, 1), rotation.clone().negateLocal(), 0.1f);
            NavMotion local = new NavMotion(), global = new NavMotion();
            local.position.set(1, 0, 0);
            frame.toWorld(local, global);
            assertEquals(0, global.velocity.length(), 0.0001f);
            frame.updatePose(v(5, 2, 1), rotation, 0.1f);
            frame.toWorld(local, global);
            assertEquals(10, global.velocity.x, 0.0001f);
            frame.setPose(v(-100, 30, 7), rotation);
            frame.toWorld(local, global);
            assertEquals(0, global.velocity.length(), 0.0001f);
            assertThrows(IllegalArgumentException.class, () -> frame.updatePose(v(0, 0, 0), rotation, 0));
            assertThrows(IllegalArgumentException.class, () -> frame.setPose(v(0, 0, 0), new Quaternion(0, 0, 0, 0)));
        }
    }
}
