package org.ngengine.world2d;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.jme3.math.Vector3f;
import com.jme3.renderer.Camera;

class TiledViewRenderComponentTest {

    @Test
    void cameraSettlesOnlyWhenTargetIsStableAndErrorIsVisuallyNegligible() {
        Camera camera = new Camera(1280, 720);
        camera.setParallelProjection(true);
        camera.setFrustum(-100f, 10f, -320f, 320f, 180f, -180f);

        float settleDistance = TiledViewRenderComponent.cameraSettleDistance(camera);

        assertEquals(0.005f, settleDistance, 0.0001f);
        assertTrue(TiledViewRenderComponent.shouldSnapCamera(12f, true, 0.004f, 1000f, settleDistance, true));
        assertFalse(TiledViewRenderComponent.shouldSnapCamera(12f, true, 0.004f, 1000f, settleDistance, false));
        assertFalse(TiledViewRenderComponent.shouldSnapCamera(12f, true, 0.006f, 1000f, settleDistance, true));
    }

    @Test
    void existingImmediateAndLongDistanceSnapRulesRemainIntact() {
        assertTrue(TiledViewRenderComponent.shouldSnapCamera(12f, false, 10f, 1000f, 0.005f, false));
        assertTrue(TiledViewRenderComponent.shouldSnapCamera(0f, true, 10f, 1000f, 0.005f, false));
        assertTrue(TiledViewRenderComponent.shouldSnapCamera(12f, true, 1001f, 1000f, 0.005f, false));
        assertFalse(TiledViewRenderComponent.shouldSnapCamera(12f, true, 10f, 1000f, 0.005f, true));
    }

    @Test
    void targetMustRemainStillForThreeFramesBeforeSettling() {
        int stableFrames = 0;

        stableFrames = TiledViewRenderComponent.updateStableTargetFrames(stableFrames, 0f, 0.0001f);
        stableFrames = TiledViewRenderComponent.updateStableTargetFrames(stableFrames, 0f, 0.0001f);
        assertEquals(2, stableFrames);

        stableFrames = TiledViewRenderComponent.updateStableTargetFrames(stableFrames, 0f, 0.0001f);
        assertEquals(3, stableFrames);

        stableFrames = TiledViewRenderComponent.updateStableTargetFrames(stableFrames, 0.001f, 0.0001f);
        assertEquals(0, stableFrames);
    }

    @Test
    void settleDistanceIgnoresMovementAlongCameraViewAxis() {
        Vector3f first = new Vector3f(10f, 100f, -5f);
        Vector3f second = new Vector3f(10f, -100f, -5f);

        assertEquals(0f, TiledViewRenderComponent.cameraPlaneDistance(first, second), 0f);
    }

    @Test
    void renderLocationSnapsToLogicalPixelWithoutChangingCameraScale() {
        Camera camera = new Camera(1280, 720);
        camera.setParallelProjection(true);
        camera.setFrustum(-100f, 10f, -320f, 320f, 180f, -180f);
        Vector3f location = new Vector3f(10.24f, 0f, -3.74f);

        TiledViewRenderComponent.snapCameraLocation(location, camera);

        assertEquals(10f, location.x, 0.0001f);
        assertEquals(-3.5f, location.z, 0.0001f);
        assertEquals(1280, camera.getWidth());
        assertEquals(720, camera.getHeight());
    }
}
