package io.github.jmecn.tiled.enums;

public enum TileRenderSize {
    TILE("tile"),
    GRID("grid");
    final String value;
    TileRenderSize(String value) {
        this.value = value;
    }
    public String getValue() {
        return value;
    }
    public static TileRenderSize fromString(String value) {
        for (TileRenderSize size : TileRenderSize.values()) {
            if (size.value.equals(value)) {
                return size;
            }
        }
        return TILE;
    }
}