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
 * the BSD 3-Clause License. 
 */

package com.jme3.bullet.types;

import org.teavm.jso.JSMethod;
import org.teavm.jso.JSObject;

public interface btIndexedMesh extends JSObject {
     @JSMethod("get_m_numTriangles")
    public int getNumTriangles();

    @JSMethod("set_m_numTriangles")
    public void setNumTriangles(int numTriangles);

    @JSMethod("getTriangleIndexBase")
    public int getTriangleIndexBase();

    @JSMethod("setTriangleIndexBase")
    public void setTriangleIndexBase(int triangleIndexBase);

    @JSMethod("get_m_triangleIndexStride")
    public int getTriangleIndexStride();

    @JSMethod("set_m_triangleIndexStride")
    public void setTriangleIndexStride(int triangleIndexStride);

    @JSMethod("get_m_numVertices")
    public int getNumVertices();

    @JSMethod("set_m_numVertices")
    public void setNumVertices(int numVertices);

    @JSMethod("getVertexBase")
    public int getVertexBase();

    @JSMethod("setVertexBase")
    public void setVertexBase(int vertexBase);

    @JSMethod("get_m_vertexStride")
    public int getVertexStride();

    @JSMethod("set_m_vertexStride")
    public void setVertexStride(int vertexStride);
   

    
}
