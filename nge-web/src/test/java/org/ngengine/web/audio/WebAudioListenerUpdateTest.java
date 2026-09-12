/*
 * Copyright (c) 2025-2026, Nostr Game Engine
 * All rights reserved.
 */
package org.ngengine.web.audio;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.jme3.audio.Listener;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import org.junit.jupiter.api.Test;

class WebAudioListenerUpdateTest {
    private static class RecordingRenderer extends WebAudioRenderer {
        int updates;
        final Vector3f position = new Vector3f();
        final Vector3f direction = new Vector3f();
        final Vector3f up = new Vector3f();

        @Override
        protected void sendListenerTransform(Vector3f position, Vector3f direction, Vector3f up) {
            updates++;
            this.position.set(position);
            this.direction.set(direction);
            this.up.set(up);
        }
    }

    @Test
    void coalescesChangesAndSendsTheFinalTransform() {
        RecordingRenderer renderer = new RecordingRenderer();
        Listener listener = new Listener();
        renderer.setListener(listener);
        listener.setLocation(new Vector3f(1, 2, 3));
        listener.setRotation(new Quaternion().fromAngles(0, 1, 0));
        listener.setVelocity(new Vector3f(4, 5, 6));
        assertEquals(0, renderer.updates);
        renderer.update(0.016f);
        assertEquals(1, renderer.updates);
        assertEquals(listener.getLocation(), renderer.position);
        assertEquals(listener.getDirection(), renderer.direction);
        assertEquals(listener.getUp(), renderer.up);
    }

    @Test
    void unchangedFrameAssignmentsDoNotGenerateMessages() {
        RecordingRenderer renderer = new RecordingRenderer();
        Listener listener = new Listener();
        renderer.setListener(listener);
        renderer.update(0.016f);
        for (int i = 0; i < 1000; i++) {
            listener.setLocation(Vector3f.ZERO);
            listener.setRotation(Quaternion.IDENTITY);
            listener.setVelocity(Vector3f.ZERO);
            renderer.update(0.016f);
        }
        assertEquals(1, renderer.updates);
        listener.setLocation(Vector3f.UNIT_X);
        renderer.update(0.016f);
        assertEquals(2, renderer.updates);
    }

    @Test
    void replacingListenerInvalidatesCacheAndDetachesTheOldListener() {
        RecordingRenderer renderer = new RecordingRenderer();
        Listener old = new Listener();
        renderer.setListener(old);
        renderer.update(0.016f);
        Listener replacement = new Listener();
        renderer.setListener(replacement);
        old.setLocation(Vector3f.UNIT_X);
        renderer.update(0.016f);
        assertEquals(2, renderer.updates);
        assertEquals(Vector3f.ZERO, renderer.position);
    }

    @Test
    void cleanupDiscardsPendingListenerUpdates() {
        RecordingRenderer renderer = new RecordingRenderer();
        Listener listener = new Listener();
        renderer.setListener(listener);
        renderer.cleanup();
        listener.setLocation(Vector3f.UNIT_X);
        renderer.update(0.016f);
        assertEquals(0, renderer.updates);
    }
}
