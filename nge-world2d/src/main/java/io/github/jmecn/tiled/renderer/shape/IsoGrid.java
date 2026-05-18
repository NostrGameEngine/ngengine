/**
 * Copyright (c) 2025-2026, Nostr Game Engine
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 * 
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 
 * 3. Neither the name of the copyright holder nor the names of its
 *    contributors may be used to endorse or promote products derived from
 *    this software without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 * 
 * Nostr Game Engine is a fork of the jMonkeyEngine, which is licensed under
 * the BSD 3-Clause License. 
 */

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
