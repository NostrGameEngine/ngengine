package io.github.jmecn.tiled.util;

import com.jme3.math.ColorRGBA;

public final class ColorUtil {

    private ColorUtil() {}

    public static ColorRGBA toColorRGBA(String str) {
        float scalar = 1f / 255f;

        // #AARRGGBB || #AARRGGBB
        if (str.startsWith("#")) {
            str = str.substring(1);
        }

        int argb = (int) Long.parseLong(str.toUpperCase(), 16);
        int alpha = 0xFF;
        if (str.length() >= 8) {// use #AARRGGBB
            alpha = (argb >> 24) & 0xFF;
        }
        int red = (argb >> 16) & 0xFF;
        int green = (argb >> 8) & 0xFF;
        int blue = (argb) & 0xFF;

        return new ColorRGBA(red * scalar, green * scalar, blue * scalar, alpha * scalar);
    }

    public static String toHex(ColorRGBA color) {
        int alpha = (int) (color.a * 255);
        int red = (int) (color.r * 255);
        int green = (int) (color.g * 255);
        int blue = (int) (color.b * 255);

        return String.format("#%02X%02X%02X%02X", alpha, red, green, blue);
    }
}
