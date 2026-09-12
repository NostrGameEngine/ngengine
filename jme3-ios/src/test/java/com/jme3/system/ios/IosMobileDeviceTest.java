package com.jme3.system.ios;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class IosMobileDeviceTest {
    @Test
    void queriesTheImmutableDeviceFamilyOnlyOnce() {
        for (boolean mobile : new boolean[] {false, true}) {
            int[] calls = {0};
            JmeIosSystem system = new JmeIosSystem() {
                @Override
                boolean queryMobileDevice() {
                    calls[0]++;
                    return mobile;
                }
            };
            for (int frame = 0; frame < 100; frame++) {
                assertEquals(mobile, system.isMobileDevice());
            }
            assertEquals(1, calls[0]);
        }
    }

    @Test
    void missingNativeBridgeDoesNotAssumeIosMeansHandheld() {
        JmeIosSystem system = new JmeIosSystem();
        assertFalse(system.isMobileDevice());
        assertFalse(system.isMobileDevice());
    }
}
