package org.ngengine.world2d;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.jme3.renderer.Camera;

class TiledViewRenderComponentTest {

    @Test
    void cameraSettlesWithinHalfAVisiblePixel() {
        Camera camera = new Camera(1280, 720);
        camera.setParallelProjection(true);
        camera.setFrustum(-100f, 10f, -320f, 320f, 180f, -180f);

        float settleDistance = TiledViewRenderComponent.cameraSettleDistance(camera);

        assertEquals(0.25f, settleDistance, 0.0001f);
        assertTrue(TiledViewRenderComponent.shouldSnapCamera(12f, true, 0.2f, 1000f, settleDistance));
        assertFalse(TiledViewRenderComponent.shouldSnapCamera(12f, true, 0.3f, 1000f, settleDistance));
    }

    @Test
    void existingImmediateAndLongDistanceSnapRulesRemainIntact() {
        assertTrue(TiledViewRenderComponent.shouldSnapCamera(12f, false, 10f, 1000f, 0.25f));
        assertTrue(TiledViewRenderComponent.shouldSnapCamera(0f, true, 10f, 1000f, 0.25f));
        assertTrue(TiledViewRenderComponent.shouldSnapCamera(12f, true, 1001f, 1000f, 0.25f));
        assertFalse(TiledViewRenderComponent.shouldSnapCamera(12f, true, 10f, 1000f, 0.25f));
    }
}
