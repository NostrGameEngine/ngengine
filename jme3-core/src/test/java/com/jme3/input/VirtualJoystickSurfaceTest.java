package com.jme3.input;

import com.jme3.asset.DesktopAssetManager;
import com.jme3.input.dummy.DummyKeyInput;
import com.jme3.input.dummy.DummyMouseInput;
import com.jme3.input.virtual.VirtualJoystick;
import com.jme3.input.virtual.VirtualJoystickXboxLayout;
import com.jme3.scene.Node;
import com.jme3.texture.plugins.AWTLoader;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VirtualJoystickSurfaceTest {
    @Test
    void normalizedPointerHitsControlAtDifferentGuiScale() {
        InputManager inputManager = new InputManager(new DummyMouseInput(), new DummyKeyInput(), null, null);
        VirtualJoystick joystick = new VirtualJoystick(inputManager, null, 0);
        joystick.setLayout(new VirtualJoystickXboxLayout());
        joystick.setEnabled(true);
        assertFalse(joystick.onNormalizedPointerDown(1, 0.56f, 0.06f, 0));

        // START sits at (0.56, 0.06) whether the GUI uses logical pixels or a relative height.
        Node gui = new Node("gui");
        DesktopAssetManager assets = new DesktopAssetManager(true);
        assets.registerLoader(AWTLoader.class, "png");
        joystick.updateVisuals(gui, assets, 2224, 1000, 0);
        assertFalse(joystick.onPointerDown(1, 0.56f * 914, 0.06f * 411, 1));
        assertTrue(joystick.onNormalizedPointerDown(1, 0.56f, 0.06f, 2));
        assertEquals(1L << 1, joystick.getCapturedPointerMask());
        assertTrue(joystick.onPointerUp(1, 3));
        assertEquals(0L, joystick.getCapturedPointerMask());

        joystick.updateVisuals(gui, assets, 914, 411, 0);
        assertTrue(joystick.onNormalizedPointerDown(1, 0.56f, 0.06f, 4));
        assertTrue(joystick.onPointerUp(1, 5));
    }
}
