package io.github.jmecn.tiled.enums;

import io.github.jmecn.tiled.core.MapObject;

import java.util.Comparator;

/**
 * Whether the objects are drawn according to the order of appearance
 * ("index") or sorted by their y-coordinate ("topdown"). Defaults to
 * "topdown".
 */
public enum DrawOrder implements Comparator<MapObject> {
    INDEX("index") {
        @Override
        public int compare(MapObject o1, MapObject o2) {
            return Integer.compare(o1.getId(), o2.getId());
        }
    },
    TOPDOWN("topdown") {
        @Override
        public int compare(MapObject o1, MapObject o2) {
            return Double.compare(o1.getY(), o2.getY());
        }
    };

    private final String value;
    DrawOrder(String value) {
        this.value = value;
    }

    public static DrawOrder fromValue(String value) {
        for (DrawOrder c : DrawOrder.values()) {
            if (c.value.equals(value)) {
                return c;
            }
        }
        return TOPDOWN;
    }
}