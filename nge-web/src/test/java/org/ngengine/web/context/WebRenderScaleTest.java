package org.ngengine.web.context;

import com.jme3.system.AppSettings;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

class WebRenderScaleTest {

    @Test
    void fractionalScaleReducesRenderTargetWithoutChangingLogicalSize() {
        assertArrayEquals(
                new int[] {1280, 720, 1280, 720, 960, 540},
                WebContext.resolveCanvasSizes(0.75f, 1280, 720, 1f));
    }

    @Test
    void fractionalScaleUsesTheNativeDensityFramebufferAsItsBase() {
        assertArrayEquals(
                new int[] {1280, 720, 2560, 1440, 1920, 1080},
                WebContext.resolveCanvasSizes(0.75f, 1280, 720, 2f));
    }

    @Test
    void builtInModesKeepTheirLogicalAndPhysicalSemantics() {
        assertArrayEquals(
                new int[] {1280, 720, 1280, 720, 1280, 720},
                WebContext.resolveCanvasSizes(AppSettings.DISPLAY_SCALE_DISABLED, 1280, 720, 2f));
        assertArrayEquals(
                new int[] {2560, 1440, 2560, 1440, 2560, 1440},
                WebContext.resolveCanvasSizes(AppSettings.DISPLAY_SCALE_NATIVE_PIXELS, 1280, 720, 2f));
        assertArrayEquals(
                new int[] {1280, 720, 2560, 1440, 2560, 1440},
                WebContext.resolveCanvasSizes(AppSettings.DISPLAY_SCALE_DPI_AWARE, 1280, 720, 2f));
    }
}
