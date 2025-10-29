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
public class OrthoGrid extends Mesh {

    public OrthoGrid(int width, int height, int tileWidth, int tileHeight) {
        int lineCount = height + width + 2;
        FloatBuffer fpb = BufferUtils.createFloatBuffer(6 * lineCount);
        ShortBuffer sib = BufferUtils.createShortBuffer(2 * lineCount);
        float xLineLen = (float)(width) * tileWidth;
        float yLineLen = (float)(height) * tileHeight;
        int curIndex = 0;

        int i;
        float x;
        for(i = 0; i < height + 1; ++i) {
            x = (float)i * tileHeight;
            fpb.put(0.0F).put(0.0F).put(x);
            fpb.put(xLineLen).put(0.0F).put(x);
            sib.put((short)(curIndex++));
            sib.put((short)(curIndex++));
        }

        for(i = 0; i < width + 1; ++i) {
            x = (float)i * tileWidth;
            fpb.put(x).put(0.0F).put(0.0F);
            fpb.put(x).put(0.0F).put(yLineLen);
            sib.put((short)(curIndex++));
            sib.put((short)(curIndex++));
        }

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
