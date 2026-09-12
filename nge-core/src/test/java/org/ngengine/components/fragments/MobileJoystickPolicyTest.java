package org.ngengine.components.fragments;

import com.jme3.input.Joystick;
import com.jme3.system.JmeSystem;
import com.jme3.system.Platform;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class MobileJoystickPolicyTest {
    private final InputHandlerFragment fragment = mock(InputHandlerFragment.class, CALLS_REAL_METHODS);

    @Test
    void backendClassificationWinsOverOperatingSystem() {
        try (MockedStatic<JmeSystem> system = mockStatic(JmeSystem.class)) {
            for (Platform platform : Platform.values()) {
                system.when(JmeSystem::getPlatform).thenReturn(platform);
                system.when(JmeSystem::isMobileDevice).thenReturn(false);
                assertFalse(fragment.showOnScreenJoystick(null, null), platform.name());
                system.when(JmeSystem::isMobileDevice).thenReturn(true);
                assertTrue(fragment.showOnScreenJoystick(null, null), platform.name());
            }
        }
    }

    @Test
    void physicalGamepadStillSuppressesAutomaticJoystick() {
        try (MockedStatic<JmeSystem> system = mockStatic(JmeSystem.class)) {
            system.when(JmeSystem::isMobileDevice).thenReturn(true);
            assertFalse(fragment.showOnScreenJoystick(null, new Joystick[] {mock(Joystick.class)}));
        }
    }
}
