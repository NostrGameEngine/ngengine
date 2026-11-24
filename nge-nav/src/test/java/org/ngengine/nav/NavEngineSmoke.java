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

import com.jme3.app.SimpleApplication;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.shape.Box;
import com.jme3.system.AppSettings;
import com.jme3.system.JmeContext;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Runs the real engine loop without a graphics device. Game code consumes motion intents.
 */
public final class NavEngineSmoke extends SimpleApplication {

    private final CountDownLatch finished = new CountDownLatch(1);
    private final NavMotion motion = new NavMotion();
    private final NavStep step = new NavStep();
    private final Vector3f movement = new Vector3f();
    private volatile Throwable failure;
    private volatile boolean arrived;
    private NavNavigator navigator;

    public NavEngineSmoke() {
        super(new NavAppState(new NavWorld(NavSettings.builder().chunkSize(4).build())));
    }

    public static void main(String[] args) throws Exception {
        NavEngineSmoke app = new NavEngineSmoke();
        AppSettings settings = new AppSettings(true);
        settings.setAudioRenderer(null);
        settings.setFrameRate(240);
        app.setSettings(settings);
        app.setShowSettings(false);
        app.start(JmeContext.Type.Headless);
        boolean done = app.finished.await(30, TimeUnit.SECONDS);
        app.stop(true);
        Thread.setDefaultUncaughtExceptionHandler(null);
        if (!done || !app.arrived || app.failure != null) throw new AssertionError(
            "Engine navigation did not arrive: " +
            app.step.getStatus() +
            "/" +
            app.step.getReason() +
            " at " +
            app.motion.position,
            app.failure
        );
        System.out.println("ENGINE_NAV_OK");
    }

    @Override
    protected com.jme3.font.BitmapFont loadGuiFont() {
        return null;
    }

    @Override
    public void simpleInitApp() {
        NavWorld world = stateManager.getState(NavAppState.class).getWorld();
        Geometry floor = new Geometry("floor", new Box(3, 0.5f, 3));
        floor.setLocalTranslation(0, -0.5f, 0);
        floor.updateGeometricState();
        world.layer("floor").add(floor, true, 0, NavGeometry.DEFAULT_FLAGS);
        motion.position.set(-1.25f, 0.5f, 0.25f);
        navigator = world.view().radius(0.15f).height(1).build().navigator();
        navigator.target(new Vector3f(1.25f, 0.5f, 0.25f));
    }

    @Override
    public void simpleUpdate(float tpf) {
        motion.time += tpf;
        navigator.update(motion, tpf, step);
        if (step.getStatus() == NavStatus.MOVE) {
            step.getDesiredVelocity(motion.velocity);
            movement.set(motion.velocity).multLocal(step.getHorizon());
            motion.position.addLocal(movement);
        } else motion.velocity.set(0, 0, 0);
        if (step.getStatus() == NavStatus.ARRIVED) {
            arrived = true;
            finished.countDown();
        }
    }

    @Override
    public void handleError(String message, Throwable exception) {
        exception.printStackTrace(System.out);
        failure = exception;
        finished.countDown();
    }
}
