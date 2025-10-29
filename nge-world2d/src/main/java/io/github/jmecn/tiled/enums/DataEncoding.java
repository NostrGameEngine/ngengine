package io.github.jmecn.tiled.enums;

/**
 * Tile data encoding
 * @author yanmaoyuan
 */
public enum DataEncoding {
    BASE64("base64"), CSV("csv"), NONE("");
    private final String value;
    DataEncoding(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static DataEncoding fromValue(String value) {
        for (DataEncoding c : DataEncoding.values()) {
            if (c.value.equals(value)) {
                return c;
            }
        }
        return null;
    }
}
