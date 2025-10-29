package io.github.jmecn.tiled.enums;

/**
 * For staggered and hexagonal maps, determines which axis ("x" or "y") is
 * staggered. (since 0.11)
 */
public enum StaggerAxis {
    X("x"), Y("y");
    final String value;
    StaggerAxis(String value) {
        this.value = value;
    }
    public String getValue() {
        return value;
    }
}