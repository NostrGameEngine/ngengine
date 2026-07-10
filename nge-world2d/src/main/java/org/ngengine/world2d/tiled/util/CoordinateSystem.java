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

import org.jbox2d.common.Vec2;

import com.jme3.math.Vector2f;
import com.jme3.util.TempVars;

import org.ngengine.world2d.tiled.core.TiledBase;
import org.ngengine.world2d.tiled.core.entity.TiledObjectEntity;
import org.ngengine.world2d.tiled.core.entity.TiledTileEntity;
import org.ngengine.world2d.tiled.core.tileset.Tile;
import org.ngengine.world2d.tiled.math2d.Point;

/**
 * Convert between different coordinate spaces.
 * World space = the effective world space of the scenegraph (as used in jME3)
 * Grid space = the coordinate system used by the map plane (same as world space for orthogonal maps, but different for isometric maps as the map plane is rotated)
 * Physics space = the coordinate system used by the physics engine (usually scaled down by a factor that remaps the world coordinates to meters)
 * Tile = the tile indices in the tile map (eg. first tile is at (0,0))
 * 
 * 
 */
public interface CoordinateSystem {

    void gridToWorldSpace(float x, float y, Vector2f out);


    void gridToTile(float x, float y, Point out);


    void tileToGridSpace(float x, float y, Vector2f out);

    void tileToWorldSpace(float x, float y, Vector2f out);

    void worldToGridSpace(float x, float y, Vector2f out);


    void worldToTile(float x, float y, Point out);

    void worldToPhysicsSpace(float x, float y, Vector2f out);

    default void worldToPhysicsSpace(Vector2f c, Vec2 out){
        try(TempVars vars = TempVars.get()){
            Vector2f tmp = vars.vect2d;
            worldToPhysicsSpace(c.x,c.y, tmp);
            out.x = tmp.x;
            out.y = tmp.y;
        }
    }
   
    void physicsToWorldSpace(float x, float y, Vector2f out);

    void physicsToWorldSpace(Vec2 physicsWorldCoords, Vector2f out);
    public default void physicsToWorldSpace(Vector2f physicsWorldCoords, Vector2f out){
        physicsToWorldSpace(physicsWorldCoords.x, physicsWorldCoords.y, out);
    }

    default Vector2f gridToWorldSpace(float x, float y) {
        Vector2f out = new Vector2f();
        gridToWorldSpace(x, y, out);
        return out;
    }

    default Point gridToTile(float x, float y) {
        Point out = new Point();
        gridToTile(x, y, out);
        return out;
    }

    default Vector2f tileToGridSpace(float x, float y) {
        Vector2f out = new Vector2f();
        tileToGridSpace(x, y, out);
        return out;
    }

    default Vector2f tileToWorldSpace(float x, float y) {
        Vector2f out = new Vector2f();
        tileToWorldSpace(x, y, out);
        return out;
    }

    default Vector2f worldToGridSpace(float x, float y) {
        Vector2f out = new Vector2f();
        worldToGridSpace(x, y, out);
        return out;
    }

    default Point worldToTile(float x, float y) {
        Point out = new Point();
        worldToTile(x, y, out);
        return out;
    }

    default Vec2 worldToPhysicsSpace(Vector2f worldCoords) {
        Vec2 out = new Vec2();
        worldToPhysicsSpace(worldCoords, out);
        return out;
    }

    default Vector2f physicsToWorldSpace(Vec2 physicsWorldCoords) {
        Vector2f out = new Vector2f();
        physicsToWorldSpace(physicsWorldCoords, out);
        return out;
    }
    
    
    float getTopDownYIndex(TiledObjectEntity o) ;


    float getTopDownYIndex(float x, float y);

    default void getPositionInGridSpace(TiledBase entry, Vector2f out){
        if(entry instanceof TiledTileEntity){
            TiledTileEntity tile = (TiledTileEntity)entry;
            tileToGridSpace((float)tile.getX(), (float)tile.getY(), out);
        } else if(entry instanceof TiledObjectEntity){
            TiledObjectEntity obj = (TiledObjectEntity)entry;
            out.x = (float) obj.getX();
            out.y = (float) obj.getY();
        } else {
            out.set(0,0);
        }
    }
    default Vector2f getPositionInGridSpace(TiledBase entry){
        Vector2f out = new Vector2f();
        getPositionInGridSpace(entry, out);
        return out;
    }

    void getCollisionCenterInGridSpace(
        TiledObjectEntity parentTileObject,
        TiledObjectEntity collisionObject,
        Vector2f out
    );

    default void getTileObjectCenterInGridSpace(
        TiledObjectEntity parentTileObject,
        Tile tile,
        TiledObjectEntity tileObject,
        Vector2f out
    ) {
        getCollisionCenterInGridSpace(parentTileObject, tileObject, out);
    }
    void getCenterInGridSpace(TiledBase obj, Vector2f out);

    default Vector2f getCenterInGridSpace(TiledBase entry){
        Vector2f out = new Vector2f();
        getCenterInGridSpace(entry, out);
        return out;
    }

}
