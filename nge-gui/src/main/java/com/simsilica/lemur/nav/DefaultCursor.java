/**
 * Copyright (c) 2026, Nostr Game Engine
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
 * 
 * #########################################
 * 
 * nge-gui is built and based on Lemur, which is licensed under the BSD 3-Clause License.
 * - Copyright (c) 2012-2026 jMonkeyEngine All rights reserved. 
 * - Copyright (c) 2016-2026, Simsilica, LLC All rights reserved.
 * 
 * https://github.com/jMonkeyEngine-Contributions/Lemur
 */

package com.simsilica.lemur.nav;

import com.jme3.asset.AssetManager;
import com.jme3.collision.Collidable;
import com.jme3.collision.CollisionResults;
import com.jme3.material.Material;
import com.jme3.material.RenderState.BlendMode;
import com.jme3.material.RenderState.FaceCullMode;
import com.jme3.math.ColorRGBA;
import com.jme3.renderer.queue.RenderQueue.Bucket;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import com.jme3.scene.Node;
import com.jme3.scene.VertexBuffer.Type;
import com.jme3.util.BufferUtils;
import com.simsilica.lemur.NGEGui;

/**
 * Simple symmetric "dart" cursor (border + fill only).
 *
 * @author Riccardo Balbo, GPT-5.2
 */
public class DefaultCursor extends Node {

    private static ColorRGBA fromHex(String hex) {
        return NGEGui.srgbaColor(Integer.valueOf(hex.substring(1, 3), 16) / 255f,
                Integer.valueOf(hex.substring(3, 5), 16) / 255f,
                Integer.valueOf(hex.substring(5, 7), 16) / 255f,
                hex.length() > 7 ? Integer.valueOf(hex.substring(7, 9), 16) / 255f : 1f, true);
    }

    private final ColorRGBA fillColor = fromHex("#3E1E68");
    private final ColorRGBA borderColor = fromHex("#9681b6");

    public float borderScale = 1.12f;
    public float fillScale = 0.90f;

    private float sizePx;

    private Geometry border;
    private Geometry fill;

    private final Material matAlpha;

    public DefaultCursor(AssetManager assets, float sizePx) {
        super("cursor");
        this.sizePx = sizePx;

        setCullHint(CullHint.Never);

        Material m = new Material(assets, "Common/MatDefs/Misc/Unshaded.j3md");
        m.getAdditionalRenderState().setBlendMode(BlendMode.Off);
        m.getAdditionalRenderState().setDepthTest(false);
        m.getAdditionalRenderState().setDepthWrite(true);
        m.getAdditionalRenderState().setFaceCullMode(FaceCullMode.Off);
        setQueueBucket(Bucket.Translucent);
        this.matAlpha = m;

        rebuild();
    }

    public void setSize(float sizePx) {
        this.sizePx = Math.max(8f, sizePx);
        rebuild();
    }

    /** Call this after changing fillColor/borderColor. */
    public void refreshColors() {
        if (border != null) border.getMaterial().setColor("Color", borderColor);
        if (fill != null) fill.getMaterial().setColor("Color", fillColor);
    }

    private void rebuild() {
        detachAllChildren();

        // Arrow axis points down-right from the tip.
        final float invSqrt2 = 0.70710677f;
        final float dx = invSqrt2, dy = -invSqrt2; // along arrow
        final float nx = invSqrt2, ny = invSqrt2; // perpendicular

        // Symmetric dart (convex quad), CCW:
        // tip -> left shoulder -> tail -> right shoulder
        float L1 = sizePx * 0.78f; // shoulder distance
        float L2 = sizePx * 1.32f; // tail distance
        float B = sizePx * 0.34f; // half width at shoulders

        float tipX = 0f, tipY = 0f;

        float sLeftX = dx * L1 - nx * B;
        float sLeftY = dy * L1 - ny * B;

        float tailX = dx * L2;
        float tailY = dy * L2;

        float sRightX = dx * L1 + nx * B;
        float sRightY = dy * L1 + ny * B;

        float[] xBase = new float[] { tipX, sLeftX, tailX, sRightX };
        float[] yBase = new float[] { tipY, sLeftY, tailY, sRightY };

        Mesh borderMesh = buildQuadMesh(scale(xBase, borderScale), scale(yBase, borderScale));
        Mesh fillMesh = buildQuadMesh(scale(xBase, fillScale), scale(yBase, fillScale));

        border = geomAlpha("border", borderMesh, borderColor);
        border.setLocalTranslation(0, 0, 0.02f);

        fill = geomAlpha("fill", fillMesh, fillColor);
        fill.setLocalTranslation(0, 0, 0.04f);

        attachChild(border);
        attachChild(fill);

        refreshColors();
    }

    private Geometry geomAlpha(String name, Mesh mesh, ColorRGBA color) {
        Geometry g = new Geometry(name, mesh);
        Material m = matAlpha.clone();
        m.setColor("Color", color);
        g.setMaterial(m);
        g.setQueueBucket(Bucket.Gui);
        return g;
    }

 

    private static float[] scale(float[] a, float s) {
        float[] out = new float[a.length];
        for (int i = 0; i < a.length; i++) out[i] = a[i] * s;
        return out;
    }

    /** Convex 4-vertex mesh in CCW order: indices fixed. */
    private static Mesh buildQuadMesh(float[] x, float[] y) {
        float[] pos = new float[4 * 3];
        for (int i = 0; i < 4; i++) {
            pos[i * 3] = x[i];
            pos[i * 3 + 1] = y[i];
            pos[i * 3 + 2] = 0f;
        }

        int[] idx = new int[] { 0, 1, 2, 0, 2, 3 };

        Mesh m = new Mesh();
        m.setBuffer(Type.Position, 3, BufferUtils.createFloatBuffer(pos));
        m.setBuffer(Type.Index, 3, BufferUtils.createIntBuffer(idx));
        m.updateBound();
        m.updateCounts();
        return m;
    }
}
