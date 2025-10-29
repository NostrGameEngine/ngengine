package io.github.jmecn.tiled.renderer;

/**
 * @author yanmaoyuan
 */
public interface TileVisitor {
    void visit(int x, int y, int z);
}