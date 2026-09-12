package com.jme3.system.android;

import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.view.View;
import android.view.Display;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AndroidMobileDeviceTest {
    private final JmeAndroidSystem system = new JmeAndroidSystem();
    private final PackageManager packages = mock(PackageManager.class);
    private final Configuration configuration = mock(Configuration.class);
    private Resources resources;

    @BeforeEach
    void attachTouchDevice() {
        Context context = mock(Context.class);
        resources = mock(Resources.class);
        View view = mock(View.class);
        when(view.getContext()).thenReturn(context);
        when(context.getPackageManager()).thenReturn(packages);
        when(context.getResources()).thenReturn(resources);
        when(resources.getConfiguration()).thenReturn(configuration);
        when(packages.hasSystemFeature(PackageManager.FEATURE_TOUCHSCREEN)).thenReturn(true);
        configuration.uiMode = Configuration.UI_MODE_TYPE_NORMAL;
        JmeAndroidSystem.setView(view);
    }

    @AfterEach
    void detachView() {
        JmeAndroidSystem.setView(null);
    }

    @Test
    void phoneAndTabletAreMobileRegardlessOfLayoutSize() {
        configuration.smallestScreenWidthDp = 360;
        assertTrue(system.isMobileDevice());
        configuration.smallestScreenWidthDp = 800;
        assertTrue(system.isMobileDevice());
    }

    @Test
    void touchEmulationIsNotATouchscreen() {
        when(packages.hasSystemFeature(PackageManager.FEATURE_TOUCHSCREEN)).thenReturn(false);
        when(packages.hasSystemFeature(PackageManager.FEATURE_FAKETOUCH)).thenReturn(true);
        assertFalse(system.isMobileDevice());
    }

    @Test
    void nonHandheldFormFactorsAreExcludedEvenWithTouch() {
        for (String feature : new String[] {PackageManager.FEATURE_PC,
                PackageManager.FEATURE_TELEVISION, PackageManager.FEATURE_LEANBACK_ONLY,
                PackageManager.FEATURE_AUTOMOTIVE, PackageManager.FEATURE_WATCH,
                "org.chromium.arc", "org.chromium.arc.device_management"}) {
            when(packages.hasSystemFeature(feature)).thenReturn(true);
            assertFalse(new JmeAndroidSystem().isMobileDevice(), feature);
            when(packages.hasSystemFeature(feature)).thenReturn(false);
        }
    }

    @Test
    void modeChangesAreObservedWithoutRepeatingHardwareQueries() {
        assertTrue(system.isMobileDevice());
        for (int mode : new int[] {Configuration.UI_MODE_TYPE_TELEVISION,
                Configuration.UI_MODE_TYPE_DESK, Configuration.UI_MODE_TYPE_CAR,
                Configuration.UI_MODE_TYPE_WATCH, Configuration.UI_MODE_TYPE_APPLIANCE,
                Configuration.UI_MODE_TYPE_VR_HEADSET}) {
            configuration.uiMode = mode;
            assertFalse(system.isMobileDevice());
        }
        configuration.uiMode = Configuration.UI_MODE_TYPE_NORMAL | Configuration.UI_MODE_NIGHT_YES;
        assertTrue(system.isMobileDevice());
        verify(packages, times(1)).hasSystemFeature(PackageManager.FEATURE_TOUCHSCREEN);
    }

    @Test
    void optionalDexFieldIsReadAgainWithoutRepeatingItsLookup() {
        SamsungConfiguration dex = new SamsungConfiguration();
        assertFalse(system.isDesktopMode(dex));
        dex.semDesktopModeEnabled = SamsungConfiguration.SEM_DESKTOP_MODE_ENABLED;
        assertTrue(system.isDesktopMode(dex));
        dex.semDesktopModeEnabled = 0;
        assertFalse(system.isDesktopMode(dex));
        assertFalse(system.isDesktopMode(configuration));
        assertFalse(system.isDesktopMode(configuration));
    }

    public static class SamsungConfiguration {
        public static final int SEM_DESKTOP_MODE_ENABLED = 1;
        public int semDesktopModeEnabled;
    }

    @Test
    void secondaryDisplaysAndUnattachedViewsAreNotHandheld() {
        View view = mock(View.class);
        Display display = mock(Display.class);
        assertFalse(JmeAndroidSystem.DisplayApi17.isDefaultDisplay(view));
        when(view.getDisplay()).thenReturn(display);
        when(display.getDisplayId()).thenReturn(Display.DEFAULT_DISPLAY);
        assertTrue(JmeAndroidSystem.DisplayApi17.isDefaultDisplay(view));
        when(display.getDisplayId()).thenReturn(2);
        assertFalse(JmeAndroidSystem.DisplayApi17.isDefaultDisplay(view));
        when(display.getDisplayId()).thenReturn(Display.DEFAULT_DISPLAY);
        assertTrue(JmeAndroidSystem.DisplayApi17.isDefaultDisplay(view));
    }

    @Test
    void recreatedActivityRefreshesTheHardwareCache() {
        assertTrue(system.isMobileDevice());
        attachTouchDevice();
        when(packages.hasSystemFeature(PackageManager.FEATURE_PC)).thenReturn(true);
        assertFalse(system.isMobileDevice());
        verify(packages, times(2)).hasSystemFeature(PackageManager.FEATURE_TOUCHSCREEN);
    }

    @Test
    void missingViewIsUnknownNotMobile() {
        JmeAndroidSystem.setView(null);
        assertFalse(system.isMobileDevice());
    }
}
