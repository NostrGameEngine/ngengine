package com.jme3.system;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MobileDeviceTest {
    @Test
    void operatingSystemAloneDoesNotIdentifyAHandheldDevice() {
        for (Platform platform : Platform.values()) {
            JmeSystemDelegate delegate = new MockJmeSystemDelegate() {
                @Override
                public Platform getPlatform() {
                    return platform;
                }
            };
            assertEquals(false, delegate.isMobileDevice(), platform.name());
        }
    }

    @Test
    void backendCanReportMobileWithoutChangingItsPlatform() {
        JmeSystemDelegate delegate = new MockJmeSystemDelegate() {
            @Override
            public Platform getPlatform() {
                return Platform.Web;
            }

            @Override
            public boolean isMobileDevice() {
                return true;
            }
        };
        assertEquals(Platform.Web, delegate.getPlatform());
        assertTrue(delegate.isMobileDevice());
    }
}
