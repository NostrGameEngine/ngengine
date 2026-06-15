/*
 * Copyright (c) 2026 jMonkeyEngine
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 * * Redistributions of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 *
 * * Redistributions in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer in the
 *   documentation and/or other materials provided with the distribution.
 *
 * * Neither the name of 'jMonkeyEngine' nor the names of its contributors
 *   may be used to endorse or promote products derived from this software
 *   without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
 * TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package com.jme3.renderer;

import com.jme3.scene.shape.Quad;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class StatisticsTest {

    @Test
    public void meshInstancesCountExtraDrawnInstances() {
        Statistics statistics = new Statistics();
        statistics.setEnabled(true);

        Quad quad = new Quad(1f, 1f);
        statistics.onMeshDrawn(quad, 0);
        assertEquals(0, statistics.getNumMeshInstances());

        statistics.onMeshDrawn(quad, 0, 5);
        assertEquals(4, statistics.getNumMeshInstances());

        statistics.clearFrame();
        assertEquals(0, statistics.getNumMeshInstances());
    }

    @Test
    public void uploadBytesAreSummedAndClearedPerFrame() {
        Statistics statistics = new Statistics();
        statistics.setEnabled(true);

        statistics.onTextureUpload(100);
        statistics.onTextureSubUpload(10);
        statistics.onVertexBufferUpload(200);
        statistics.onVertexBufferSubUpload(20);
        statistics.onBufferObjectUpload(300);
        statistics.onBufferObjectSubUpload(30);

        assertEquals(100, statistics.getTextureUploadBytes());
        assertEquals(10, statistics.getTextureSubUploadBytes());
        assertEquals(200, statistics.getVertexBufferUploadBytes());
        assertEquals(20, statistics.getVertexBufferSubUploadBytes());
        assertEquals(300, statistics.getBufferObjectUploadBytes());
        assertEquals(30, statistics.getBufferObjectSubUploadBytes());
        assertEquals(660, statistics.getCpuToGpuUploadBytes());
        assertEquals(300, statistics.getLargestUploadBytes());

        statistics.clearFrame();
        assertEquals(0, statistics.getCpuToGpuUploadBytes());
        assertEquals(0, statistics.getLargestUploadBytes());
    }
}
