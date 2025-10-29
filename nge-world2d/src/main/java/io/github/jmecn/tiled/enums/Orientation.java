package io.github.jmecn.tiled.enums;

/**
 * The orientation of the map determines how it should be rendered. An
 * Orthogonal map is using rectangular tiles that are aligned on a straight
 * grid. An Isometric map uses diamond shaped tiles that are aligned on an
 * isometric projected grid. A Hexagonal map uses hexagon shaped tiles that
 * fit into each other by shifting every other row.
 */
public enum Orientation {
    ORTHOGONAL("orthogonal"),
    ISOMETRIC("isometric"),
    /**
     * Hexagonal.
     *
     * @since 0.11
     */
    HEXAGONAL("hexagonal"),
    /**
     * Staggered (used for iso and hex).
     */
    STAGGERED("staggered");
    final String value;
    Orientation(String value) {
        this.value = value;
    }
    public String getValue() {
        return value;
    }
    public static Orientation fromString(String value) {
        for (Orientation o : values()) {
            if (o.value.equals(value)) {
                return o;
            }
        }
        return ORTHOGONAL;
    }
}