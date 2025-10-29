package io.github.jmecn.tiled.enums;

/**
 * The order in which tiles are rendered on screen. since Tiled 0.10, but
 * only supported for orthogonal maps at the moment
 */
public enum RenderOrder {
    RIGHT_DOWN("right-down"),
    RIGHT_UP("right-up"),
    LEFT_DOWN("left-down"),
    LEFT_UP("left-up");
    final String value;
    RenderOrder(String value) {
        this.value = value;
    }
    public String getValue() {
        return value;
    }
    public static RenderOrder fromString(String value) {
        for (RenderOrder o : values()) {
            if (o.value.equals(value)) {
                return o;
            }
        }
        return RIGHT_DOWN;
    }
}