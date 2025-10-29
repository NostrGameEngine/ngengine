package io.github.jmecn.tiled.renderer.shape;

import com.jme3.scene.Mesh;
import com.jme3.scene.VertexBuffer;
import com.jme3.util.BufferUtils;

import java.nio.FloatBuffer;
import java.nio.ShortBuffer;

/**
 * desc:
 *
 * @author yanmaoyuan
 */
public class IsoGrid extends Mesh {

    public IsoGrid(int width, int height, int tileWidth, int tileHeight) {
        int lineCount = height + width + 6;
        FloatBuffer fpb = BufferUtils.createFloatBuffer(6 * lineCount);
        ShortBuffer sib = BufferUtils.createShortBuffer(2 * lineCount);

        float halfTileWidth = tileWidth * 0.5f;
        float halfTileHeight = tileHeight * 0.5f;

        float dhw = height * halfTileWidth;
        float dww = width * halfTileWidth;
        float dwh = width * halfTileHeight;
        float dhh = height * halfTileHeight;

        int curIndex = 0;

        int i;
        float x;
        float y;
        for(i = 0; i < height + 1; ++i) {
            x = dhw - i * halfTileWidth;
            y = i * halfTileHeight;
            fpb.put(x).put(0.0F).put(y);
            fpb.put(x + dww).put(0.0F).put(y + dwh);
            sib.put((short)(curIndex++));
            sib.put((short)(curIndex++));
        }

        for(i = 0; i < width + 1; ++i) {
            x = dhw + i * halfTileWidth;
            y = i * halfTileHeight;
            fpb.put(x).put(0.0F).put(y);
            fpb.put(x - dhw).put(0.0F).put(y + dhh);
            sib.put((short)(curIndex++));
            sib.put((short)(curIndex++));
        }

        // make a rectangle
        float mapWidth = (width + height) * halfTileWidth;
        float mapHeight = (width + height) * halfTileHeight;
        fpb.put(0).put(0).put(0);
        fpb.put(mapWidth).put(0).put(0);
        sib.put((short)(curIndex++));
        sib.put((short)(curIndex++));

        fpb.put(mapWidth).put(0).put(0);
        fpb.put(mapWidth).put(0).put(mapHeight);
        sib.put((short)(curIndex++));
        sib.put((short)(curIndex++));

        fpb.put(mapWidth).put(0).put(mapHeight);
        fpb.put(0).put(0).put(mapHeight);
        sib.put((short)(curIndex++));
        sib.put((short)(curIndex++));

        fpb.put(0).put(0).put(mapHeight);
        fpb.put(0).put(0).put(0);
        sib.put((short)(curIndex++));
        sib.put((short)(curIndex++));

        fpb.flip();
        sib.flip();
        this.setBuffer(VertexBuffer.Type.Position, 3, fpb);
        this.setBuffer(VertexBuffer.Type.Index, 2, sib);
        this.setMode(Mode.Lines);
        this.updateBound();
        this.updateCounts();
        this.setStatic();
    }
}
