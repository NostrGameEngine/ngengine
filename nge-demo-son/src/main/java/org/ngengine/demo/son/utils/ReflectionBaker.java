/**
 * Copyright (c) 2025, Nostr Game Engine
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
 * the BSD 3-Clause License. The original jMonkeyEngine license is as follows:
 */
package org.ngengine.demo.son.utils;

import com.jme3.material.MatParamOverride;
import com.jme3.math.Plane;
import com.jme3.renderer.Camera;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.ViewPort;
import com.jme3.renderer.queue.RenderQueue.Bucket;
import com.jme3.scene.Geometry;
import com.jme3.shader.VarType;
import com.jme3.texture.FrameBuffer;
import com.jme3.water.ReflectionProcessor;
import java.util.function.Predicate;

public class ReflectionBaker extends ReflectionProcessor {

    private final Geometry oceanGeom;
    private RenderManager rm;
    private Predicate<Geometry> lastFilter = null;
    private final MatParamOverride renderRef = new MatParamOverride(VarType.Boolean, "RenderRef", true);

    public ReflectionBaker(Geometry oceanGeom, Camera reflectionCam, FrameBuffer reflectionBuffer, Plane reflectionClipPlane) {
        super(reflectionCam, reflectionBuffer, reflectionClipPlane);
        this.oceanGeom = oceanGeom;
    }

    @Override
    public void initialize(RenderManager rm, ViewPort vp) {
        this.rm = rm;
        super.initialize(rm, vp);
    }

    @Override
    public void preFrame(float tpf) {
        this.lastFilter = rm.getRenderFilter();
        rm.setRenderFilter(g -> {
            if (g.equals(oceanGeom)) return false;
            if (g.getQueueBucket() == Bucket.Sky) return false;
            return true;
        });
        rm.addForcedMatParam(renderRef);
        super.preFrame(tpf);
    }

    @Override
    public void postFrame(FrameBuffer out) {
        super.postFrame(out);
        rm.setRenderFilter(lastFilter);
        rm.removeForcedMatParam(renderRef);
        lastFilter = null;
    }
}
