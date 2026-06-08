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
import com.jme3.math.Vector3f;
import com.jme3.scene.Mesh;
import com.jme3.scene.VertexBuffer;
import com.jme3.util.BufferUtils;

import java.util.List;

/**
 * desc:
 *
 * @author yanmaoyuan
 */
public class Polyline extends Mesh {

    public Polyline() {
    }

    public Polyline(List<Vector2f> points, boolean closePath) {
        if (points.size() < 2) {
            throw new IllegalArgumentException("An polygon must have 2 points at least.");
        }
        polyline(points, closePath);
    }

    protected void polyline(List<Vector2f> points, boolean closePath) {
        int len = points.size();
        Vector3f[] vertex = new Vector3f[len];
        Vector3f[] normal = new Vector3f[len];
        short[] index = new short[closePath?len+1:len];

        // first one
        Vector2f point = new Vector2f();
        for(int i=0; i<len; i++) {
            point.set(points.get(i));
            vertex[i] = new Vector3f(point.x, 0, point.y);
            normal[i] = new Vector3f(0f, 1f, 0f);
            index[i] = (short) i;
        }

        if (closePath) {
            index[len] = 0;
        }

        this.setMode(Mode.LineStrip);
        this.setBuffer(VertexBuffer.Type.Position, 3, BufferUtils.createFloatBuffer(vertex));
        this.setBuffer(VertexBuffer.Type.Normal, 3, BufferUtils.createFloatBuffer(normal));
        this.setBuffer(VertexBuffer.Type.Index, 2, index);
        this.updateBound();
        this.updateCounts();
        this.setStatic();
    }
}