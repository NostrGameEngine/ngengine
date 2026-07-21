package org.ngengine.world2d;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.jme3.math.Vector3f;
import com.jme3.renderer.Camera;

class TiledViewRenderComponentTest {

    @Test
    void cameraHoldsItsContinuousPositionInsideAStableSubPixelDeadband() {
        Camera camera = new Camera(1280, 720);
        camera.setParallelProjection(true);
        camera.setFrustum(-100f, 10f, -320f, 320f, 180f, -180f);

        float holdDistance = TiledViewRenderComponent.cameraHoldDistance(camera);

        assertEquals(0.125f, holdDistance, 0.0001f);
        assertTrue(TiledViewRenderComponent.nextCameraHoldState(
            false, 12f, true, 0.12f, holdDistance, holdDistance * 3f, true, 0f, 0.5f, 1f / 60f
        ));
        assertFalse(TiledViewRenderComponent.nextCameraHoldState(
            false, 12f, true, 0.12f, holdDistance, holdDistance * 3f, false, 0f, 0.5f, 1f / 60f
        ));
        assertFalse(TiledViewRenderComponent.nextCameraHoldState(
            false, 12f, true, 0.13f, holdDistance, holdDistance * 3f, true, 0f, 0.5f, 1f / 60f
        ));
        assertFalse(TiledViewRenderComponent.nextCameraHoldState(
            false, 0f, true, 0.12f, holdDistance, holdDistance * 3f, true, 0f, 0.5f, 1f / 60f
        ));
    }

    @Test
    void cameraDoesNotHoldWhileItsNextStepIsStillVisible() {
        assertFalse(TiledViewRenderComponent.nextCameraHoldState(
            false, 12f, true, 0.12f, 0.125f, 0.375f, true, 0f, 0.5f, 1f / 30f
        ));
        assertTrue(TiledViewRenderComponent.nextCameraHoldState(
            false, 12f, true, 0.07f, 0.125f, 0.375f, true, 0f, 0.5f, 1f / 30f
        ));
    }

    @Test
    void activeHoldUsesHysteresisInsteadOfChatteringAtEntryThreshold() {
        assertTrue(TiledViewRenderComponent.nextCameraHoldState(
            true, 12f, true, 0.13f, 0.125f, 0.375f, false, 5f, 0.5f, 1f / 60f
        ));
        assertTrue(TiledViewRenderComponent.nextCameraHoldState(
            true, 12f, true, 0.30f, 0.125f, 0.375f, false, 7.9f, 0.5f, 1f / 60f
        ));
    }

    @Test
    void activeHoldReleasesForRealMovementOrLargeDisplacement() {
        assertFalse(TiledViewRenderComponent.nextCameraHoldState(
            true, 12f, true, 0.30f, 0.125f, 0.375f, false, 8.1f, 0.5f, 1f / 60f
        ));
        assertFalse(TiledViewRenderComponent.nextCameraHoldState(
            true, 12f, true, 0.38f, 0.125f, 0.375f, true, 0f, 0.5f, 1f / 60f
        ));
    }

    @Test
    void existingImmediateAndLongDistanceSnapRulesRemainIntact() {
        assertTrue(TiledViewRenderComponent.shouldSnapCamera(12f, false, 10f, 1000f));
        assertTrue(TiledViewRenderComponent.shouldSnapCamera(0f, true, 10f, 1000f));
        assertTrue(TiledViewRenderComponent.shouldSnapCamera(12f, true, 1001f, 1000f));
        assertFalse(TiledViewRenderComponent.shouldSnapCamera(12f, true, 10f, 1000f));
    }

    @Test
    void targetMustRemainStillForThreeFramesBeforeSettling() {
        int stableFrames = 0;

        stableFrames = TiledViewRenderComponent.updateStableTargetFrames(stableFrames, 0f, 2f);
        stableFrames = TiledViewRenderComponent.updateStableTargetFrames(stableFrames, 1f, 2f);
        assertEquals(2, stableFrames);

        stableFrames = TiledViewRenderComponent.updateStableTargetFrames(stableFrames, 2f, 2f);
        assertEquals(3, stableFrames);

        stableFrames = TiledViewRenderComponent.updateStableTargetFrames(stableFrames, 2.1f, 2f);
        assertEquals(0, stableFrames);
    }

    @Test
    void targetSpeedInPixelsIsFrameRateIndependent() {
        assertEquals(8f, TiledViewRenderComponent.targetSpeedInPixels(0.2f, 0.5f, 0.05f), 0.0001f);
        assertEquals(8f, TiledViewRenderComponent.targetSpeedInPixels(0.1f, 0.5f, 0.025f), 0.0001f);
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
