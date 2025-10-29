package io.github.jmecn.tiled.enums;

public enum FillMode {
    STRETCH("stretch"),
    PRESERVE_ASPECT_FIT("preserve-aspect-fit");
    final String value;
    FillMode(String value) {
        this.value = value;
    }
    public String getValue() {
        return value;
    }
    public static FillMode fromString(String value) {
        for (FillMode mode : FillMode.values()) {
            if (mode.value.equals(value)) {
                return mode;
            }
        }
        return STRETCH;
    }
}