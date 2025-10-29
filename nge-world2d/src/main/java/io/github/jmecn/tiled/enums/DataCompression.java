package io.github.jmecn.tiled.enums;

/**
 * Tile data compression
 * @author yanmaoyuan
 */
public enum DataCompression {

    ZLIB("zlib"), GZIP("gzip"), ZSTANDARD("zstd"), NONE("");
    private final String value;
    DataCompression(String value) {
        this.value = value;
    }
    public String getValue() {
        return value;
    }

    public static DataCompression fromValue(String value) {
        for (DataCompression c : DataCompression.values()) {
            if (c.value.equals(value)) {
                return c;
            }
        }
        return null;
    }
}
