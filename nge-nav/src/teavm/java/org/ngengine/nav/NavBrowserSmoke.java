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

import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.util.BufferAllocatorFactory;
import com.jme3.util.PrimitiveAllocator;

public final class NavBrowserSmoke {

    public static void main(String[] args) {
        BufferAllocatorFactory.setBufferAllocatorSupplier(PrimitiveAllocator::new);
        try (NavWorld world = NavWorld.manual(NavSettings.builder().chunkSize(4).build())) {
            world.layer("floor").add(NavGeometry.box(new Vector3f(-2, -1, -2), new Vector3f(2, 0, 2)));
            for (int i = 0; i < 1000 && !world.isReady(); i++) world.pump(32);
            if (!world.isReady()) throw new AssertionError("Browser generation did not complete");
            NavView view = world.view().radius(0.1f).height(1).cost(NavMode.WALK, 0).build();
            NavPath path = view.findPath(new Vector3f(-1.25f, 0.5f, 0.25f), new Vector3f(1.25f, 0.5f, 0.25f));
            if (!path.isComplete() || path.getCost() != 0) throw new AssertionError("Browser route failed");
            NavNavigator navigator = view.navigator(new NavPathFollower());
            navigator.target(new Vector3f(1.25f, 0.5f, 0.25f));
            NavMotion motion = new NavMotion();
            motion.position.set(-1.25f, 0.5f, 0.25f);
            NavStep step = new NavStep();
            if (navigator.update(motion, 0.016f, step).getStatus() != NavStatus.YIELD) throw new AssertionError(
                "Inline planning"
            );
            world.pump(1);
            if (navigator.update(motion, 0.016f, step).getStatus() != NavStatus.MOVE) throw new AssertionError(
                "Browser guidance failed"
            );
            Vector3f move = new Vector3f();
            for (int tick = 0; tick < 600 && step.getStatus() != NavStatus.ARRIVED; tick++) {
                if (step.getStatus() == NavStatus.MOVE) {
                    step.getDesiredVelocity(motion.velocity);
                    motion.position.addLocal(motion.velocity.mult(step.getHorizon(), move));
                }
                navigator.update(motion, 0.016f, step);
                world.pump(1);
            }
            if (step.getStatus() != NavStatus.ARRIVED || motion.velocity.length() > 0.1f) throw new AssertionError(
                "Browser continuous following failed"
            );
            motion.position.set(-1.25f, 0.5f, 0.25f);
            if (
                !view.isSegmentClear(motion.position, new Vector3f(1.25f, 0.5f, 0.25f)) ||
                view.isSegmentClear(motion.position, new Vector3f(-1.25f, 0.3f, 0.25f))
            ) throw new AssertionError("Browser external sweep failed");
            NavFrame frame = new NavFrame(world);
            long revision = world.getRevision();
            frame.updatePose(new Vector3f(4, 2, 1), new Quaternion().fromAngleAxis(0.5f, Vector3f.UNIT_Y), 1);
            NavMotion global = new NavMotion(), recovered = new NavMotion();
            frame.toWorld(motion, global);
            frame.toLocal(global, recovered);
            if (
                recovered.position.distanceSquared(motion.position) > 0.000001f ||
                recovered.velocity.distanceSquared(motion.velocity) > 0.000001f ||
                world.getRevision() != revision
            ) throw new AssertionError("Browser moving frame failed");
            System.out.println("NAV_TEAVM_OK");
        }
    }
}
