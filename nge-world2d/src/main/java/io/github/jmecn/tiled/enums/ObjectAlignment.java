package io.github.jmecn.tiled.enums;

public enum ObjectAlignment {
    UNSPECIFIED("unspecified"),
    TOP_LEFT("topleft"),
    TOP("top"),
    TOP_RIGHT("topright"),
    LEFT("left"),
    CENTER("center"),
    RIGHT("right"),
    BOTTOM_LEFT("bottomleft"),
    BOTTOM("bottom"),
    BOTTOM_RIGHT("bottomright");
    final String value;
    ObjectAlignment(String value) {
        this.value = value;
    }
    public String getValue() {
        return value;
    }
    public static ObjectAlignment fromString(String value) {
        for (ObjectAlignment alignment : ObjectAlignment.values()) {
            if (alignment.value.equals(value)) {
                return alignment;
            }
        }
        return UNSPECIFIED;
    }
}