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

package org.ngengine.world2d.tiled.renderer.shape;

import com.jme3.math.Vector2f;
import com.jme3.scene.VertexBuffer;

import java.util.ArrayList;
import java.util.List;

/**
 * desc:
 *
 * @author yanmaoyuan
 */
public class Rect extends Polyline {

    public Rect(float width, float height, boolean fill) {
        this(width, height, fill, 1f, 1f, DEFAULT_STROKE_WIDTH);
    }

    public Rect(float width, float height, boolean fill, float maxU, float maxV) {
        this(width, height, fill, maxU, maxV, DEFAULT_STROKE_WIDTH);
    }

    public Rect(float width, float height, boolean fill, float maxU, float maxV, float strokeWidth) {
        if (fill) {
            fill(width, height, maxU, maxV);
        } else {
            border(width, height, strokeWidth);
        }
    }

    private void border(float w, float h, float strokeWidth) {
        List<Vector2f> points = new ArrayList<>();
        points.add(new Vector2f(0,0));
        points.add(new Vector2f(w,0));
        points.add(new Vector2f(w, h));
        points.add(new Vector2f(0, h));
        polyline(points, true, strokeWidth);
    }

    private void fill(float w, float h, float maxU, float maxV) {
        this.setBuffer(VertexBuffer.Type.Position, 3, new float[] {
                0, 0, h,
                w, 0, h,
                w, 0, 0,
                0, 0, 0
        });
        this.setBuffer(VertexBuffer.Type.Normal, 3, new float[] {
                0, 1, 0,
                0, 1, 0,
                0, 1, 0,
                0, 1, 0
        });
        this.setBuffer(VertexBuffer.Type.TexCoord, 2, new float[] {
                0, 0,
                maxU, 0,
                maxU, maxV,
                0, maxV
        });
        this.setBuffer(VertexBuffer.Type.Index, 3, new short[] {
                0, 1, 2,
                0, 2, 3
        });
        this.updateBound();
        this.updateCounts();
        this.setStatic();
    }
}
