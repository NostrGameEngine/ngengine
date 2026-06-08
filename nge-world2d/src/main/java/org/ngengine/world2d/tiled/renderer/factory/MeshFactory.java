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

package org.ngengine.world2d.tiled.renderer.factory;

import com.jme3.math.Matrix3f;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.jme3.scene.Mesh;
import com.jme3.scene.VertexBuffer;

import org.ngengine.world2d.tiled.core.entity.TiledObjectEntity;
import org.ngengine.world2d.tiled.core.tileset.Tile;

import java.nio.FloatBuffer;
import java.util.List;

/**
 * Mesh factory for creating tile mesh and object mesh.
 *
 * @author yanmaoyuan
 */
public interface MeshFactory {

    default void toIsometric(Mesh mesh, float ratio) {
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

    /**
     * Create a new mesh for a tile by global id. The mesh will be flipped if necessary.
     * @param tileId global id. The flag bits will be used.
     * @return the mesh
     */
    Mesh newTileMesh(int tileId);

    /**
     * Create a new mesh for a tile. The mesh will be flipped if necessary.
     * @param tile the tile. The tile.getGid() flag bits will be used.
     * @return the mesh
     */
    Mesh newTileMesh(Tile tile);

    /**
     * Get the mesh for a tile by global id. The mesh will be cached by its gid once created.
     * @param tileId global id with flag bits.
     * @return the mesh
     */
    Mesh getTileMesh(int tileId);

    /**
     * Get the mesh for a tile. The mesh will be cached by its gid once created.
     * @param tile the tile. The tile.getGid() flag bits will be used.
     * @return the mesh
     */
    Mesh getTileMesh(Tile tile);

    /**
     * Create a new mesh for a map object.
     * @param object the map object
     * @return the mesh
     */
    Mesh newObjectMesh(TiledObjectEntity object);

    Mesh rectangle(TiledObjectEntity object);

    Mesh rectangle(float width, float height, boolean fill);

    Mesh ellipse(TiledObjectEntity object);

    Mesh ellipse(float width, float height, boolean fill);

    Mesh polygon(TiledObjectEntity object);

    Mesh polygon(List<Vector2f> points, boolean fill);

    Mesh polyline(TiledObjectEntity object);

    Mesh polyline(List<Vector2f> points, boolean closePath);

    Mesh marker();

    Mesh marker(float radius, boolean fill);

    Mesh image(TiledObjectEntity object);

    Mesh image(float width, float height);
}