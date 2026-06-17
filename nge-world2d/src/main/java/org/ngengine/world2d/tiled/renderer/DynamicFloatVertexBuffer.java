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

package org.ngengine.world2d.tiled.renderer;

import com.jme3.scene.Mesh;
import com.jme3.scene.VertexBuffer;
import com.jme3.util.BufferUtils;
import java.nio.FloatBuffer;

/**
 * Growable instanced float buffer that keeps a single JME vertex buffer alive
 * while the batch grows and shrinks.
 */
class DynamicFloatVertexBuffer {
    private final VertexBuffer.Type type;
    private final int components;
    private final int minCapacity;
    private final int shrinkAfterUpdates;

    private Mesh mesh;
    private VertexBuffer vertexBuffer;
    private FloatBuffer data;
    private int capacity;
    private int underusedUpdates;
    private boolean resized;

    DynamicFloatVertexBuffer(VertexBuffer.Type type, int components, int minCapacity, int shrinkAfterUpdates) {
        this.type = type;
        this.components = components;
        this.minCapacity = minCapacity;
        this.shrinkAfterUpdates = shrinkAfterUpdates;
    }

    void beginUpdate() {
        resized = false;
    }

    void ensureCapacity(int needed) {
        if (needed <= capacity) {
            return;
        }
        int newCapacity = capacity == 0 ? minCapacity : capacity;
        while (newCapacity < needed) {
            newCapacity *= 2;
        }
        resize(newCapacity);
    }

    
    void maybeShrink(int used) {
        // shrink only when really underused
        if (capacity <= minCapacity || used * 4 >= capacity) {
            underusedUpdates = 0;
            return;
        }
        // lazy shrink
        underusedUpdates++;
        if (underusedUpdates >= shrinkAfterUpdates) {
            int newCapacity = minCapacity;
            while (newCapacity < Math.max(used * 2, minCapacity)) {
                newCapacity *= 2;
            }
            resize(newCapacity);
            underusedUpdates = 0;
        }
    }

    void attach(Mesh mesh) {
        this.mesh = mesh;
        vertexBuffer = new VertexBuffer(type);
        vertexBuffer.setupData(VertexBuffer.Usage.Dynamic, components, VertexBuffer.Format.Float, data);
        vertexBuffer.setInstanced(true);
        mesh.setBuffer(vertexBuffer);
    }

    void put(int element, int component, float value) {
        data.put(element * components + component, value);
    }

    void markElementDirty(int element) {
        if (!resized && vertexBuffer != null) {
            vertexBuffer.markElementsDirty(element, 1);
        }
    }

    private void resize(int newCapacity) {
        FloatBuffer oldData = data;
        int oldCapacity = capacity;
        capacity = newCapacity;
        data = BufferUtils.createFloatBuffer(capacity * components);
        if (oldData != null) {
            int floats = Math.min(oldCapacity * components, data.capacity());
            for (int i = 0; i < floats; i++) {
                data.put(i, oldData.get(i));
            }
        }
        if (vertexBuffer != null) {
            vertexBuffer.updateData(data);
            mesh.updateCounts();
        }
        resized = true;
    }
}
