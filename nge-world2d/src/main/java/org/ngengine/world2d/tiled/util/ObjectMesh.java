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

package org.ngengine.world2d.tiled.util;

import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.List;

import org.ngengine.world2d.tiled.renderer.shape.Polyline;

import com.jme3.math.Matrix3f;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.jme3.scene.Mesh;
import com.jme3.scene.VertexBuffer;

/**
 * Create mesh for the visual part of an ObjectNode.
 * Bound2D, Ellipse, Polygon, Polyline
 * 
 * @author yanmaoyuan
 *
 */
public final class ObjectMesh {

    private ObjectMesh() {}

    public static void toIsometric(Mesh mesh, int tileWidth, int tileHeight) {
        float ratio = (float) tileHeight / tileWidth;
        Matrix3f mat3 = new Matrix3f(
                1f, 0f, -1f,
                0f, 1f, 0f,
                ratio, 0f, ratio);

        VertexBuffer vb = mesh.getBuffer(VertexBuffer.Type.Position);
        FloatBuffer fb = (FloatBuffer) vb.getData();
        for (int i = 0; i < fb.capacity(); i += 3) {
            Vector3f v = new Vector3f(fb.get(i), 0f, fb.get(i + 2));
            mat3.multLocal(v);
            fb.put(i, v.x);
            fb.put(i + 2, v.z);
        }
        mesh.updateBound();
    }

    public static Mesh makeRectangleBorder(double width, double height) {
        List<Vector2f> points = new ArrayList<>();
        points.add(new Vector2f(0,0));
        points.add(new Vector2f((float) width,0));
        points.add(new Vector2f((float)width, (float)height));
        points.add(new Vector2f(0, (float) height));
        
        return makePolyline(points, true);
    }

    public static Mesh makePolyline(List<Vector2f> points, boolean closePath) {
        return new Polyline(points, closePath);
    }
}
