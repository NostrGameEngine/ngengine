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

import com.jme3.math.FastMath;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.jme3.scene.VertexBuffer;
import com.jme3.util.BufferUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * desc:
 *
 * @author yanmaoyuan
 */
public class Ellipse extends Polyline {
    public Ellipse(float width, float height, int count, boolean fill) {
        this(width, height, count, fill, DEFAULT_STROKE_WIDTH);
    }

    public Ellipse(float width, float height, int count, boolean fill, float strokeWidth) {
        if (count < 3) {
            throw new IllegalArgumentException("count must be greater than 3");
        }
        if (fill) {
            fill(width, height, count);
        } else {
            border(width, height, count, strokeWidth);
        }
    }


    /**
     * Make an ellipse mesh.
     *
     *
     * @param width the ellipse width
     * @param height the ellipse height
     * @param count how many points you need?
     */
    protected void fill(float width, float height, int count) {
        // the uv center
        float uc = 0.5f;
        float vc = 0.5f;

        // the ellipse center
        float xc = width * uc;
        float yc = height * vc;

        // add two for center vertex and last triangle
        Vector3f[] vertex = new Vector3f[count+2];
        Vector3f[] normal = new Vector3f[count+2];
        Vector2f[] texCoord = new Vector2f[count+2];
        int[] index = new int[count+2];

        // the center
        vertex[0] = new Vector3f(xc, 0, yc);
        normal[0] = new Vector3f(0, 1, 0);
        texCoord[0] = new Vector2f(uc, vc);
        index[0] = 0;

        float radian = FastMath.TWO_PI / count;
        float r = 0;
        for(int i=0; i<count; i++) {
            float x = FastMath.sin(r);
            float y = FastMath.cos(r);
            r += radian;

            vertex[i+1] = new Vector3f(x*xc+xc, 0, y*yc+yc);
            normal[i+1] = new Vector3f(0, 1, 0);
            texCoord[i+1] = new Vector2f(x*uc+uc, y*vc+vc);
            index[i+1] = i+1;
        }
        vertex[count+1] = vertex[1];
        normal[count+1] = normal[1];
        texCoord[count+1] = texCoord[1];
        index[count+1] = count+1;

        setMode(Mode.TriangleFan);
        setBuffer(VertexBuffer.Type.Position, 3, BufferUtils.createFloatBuffer(vertex));
        setBuffer(VertexBuffer.Type.Normal, 3, BufferUtils.createFloatBuffer(normal));
        setBuffer(VertexBuffer.Type.TexCoord, 2, BufferUtils.createFloatBuffer(texCoord));
        setBuffer(VertexBuffer.Type.Index, 3, index);
        setStatic();
        updateBound();
        updateCounts();
    }

    /**
     * Make a border for Ellipse.
     *
     *
     * @param width the ellipse width
     * @param height the ellipse height
     * @param count How many points you need?
     */
    public void border(double width, double height, int count) {
        border(width, height, count, DEFAULT_STROKE_WIDTH);
    }

    public void border(double width, double height, int count, float strokeWidth) {
        float xc = (float) (width * 0.5);
        float yc = (float) (height * 0.5);

        List<Vector2f> points = new ArrayList<>(count);
        float radian = FastMath.TWO_PI / count;
        float r = 0;
        for(int i=0; i<count; i++) {
            float x = FastMath.sin(r) * xc + xc;
            float y = FastMath.cos(r) * yc + yc;
            points.add(new Vector2f(x, y));

            r += radian;
        }

        polyline(points, true, strokeWidth);
    }
}
