package io.github.jmecn.tiled.enums;

/**
 * For staggered and hexagonal maps, determines whether the "even" or "odd"
 * indexes along the staggered axis are shifted. (since 0.11)
 */
public enum StaggerIndex {
    ODD("odd", 0),
    EVEN("even", 1)
    ;
    final String value;
    final int index;

    StaggerIndex(String value, int index) {
        this.value = value;
        this.index = index;
    }
    public String getValue() {
        return value;
    }
    public int getIndex() {
        return index;
    }

}