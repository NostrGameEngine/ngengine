/*
 * Copyright (c) 2026, Nostr Game Engine
 * All rights reserved.
 */
package org.ngengine.gui;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import org.junit.jupiter.api.Test;

public class ColorChooserTest {

    @Test
    public void convertsHsbWithoutAwt() {
        ColorRGBA color = ColorChooser.hsbToRgb(0.5f, 1f, 0.75f, new ColorRGBA());

        assertEquals(0f, color.r, 0.000001f);
        assertEquals(0.75f, color.g, 0.000001f);
        assertEquals(0.75f, color.b, 0.000001f);
        assertEquals(1f, color.a, 0.000001f);
    }

    @Test
    public void roundTripsRgbThroughHsb() {
        ColorRGBA expected = new ColorRGBA(0.2f, 0.65f, 0.4f, 1f);
        Vector3f hsb = ColorChooser.rgbToHsb(expected, new Vector3f());
        ColorRGBA actual = ColorChooser.hsbToRgb(hsb.x, hsb.y, hsb.z, new ColorRGBA());

        assertEquals(expected.r, actual.r, 0.000001f);
        assertEquals(expected.g, actual.g, 0.000001f);
        assertEquals(expected.b, actual.b, 0.000001f);
    }
}
