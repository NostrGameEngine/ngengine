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

package org.ngengine.world2d.box2d;

import java.util.List;

import org.box2d4j.b2WorldId;
import org.box2d4j.b2BodyId;
import org.box2d4j.b2JointId;
import org.box2d4j.b2ShapeId;
import org.ngengine.Components;

import org.ngengine.world2d.tiled.core.TiledLayer;
import org.ngengine.world2d.tiled.core.TiledObjectLayer;
import org.ngengine.world2d.tiled.core.TiledTileLayer;
import org.ngengine.world2d.tiled.core.TiledBase;
import org.ngengine.world2d.tiled.core.TiledEntity;
import org.ngengine.world2d.tiled.core.TiledMap;
import org.ngengine.world2d.tiled.core.entity.TiledObjectEntity;
import org.ngengine.world2d.tiled.core.entity.TiledTileEntity;
import org.ngengine.world2d.tiled.core.tileset.Tile;
import org.ngengine.world2d.tiled.enums.ObjectShape;

import static org.box2d4j.B2.b2World_IsValid;

public class Box2dHelper {
    public static boolean sameBody(b2BodyId first, b2BodyId second) {
        return first == second || first != null && second != null
                && first.index1 == second.index1
                && first.world0 == second.world0
                && first.generation == second.generation;
    }

    public static boolean sameShape(b2ShapeId first, b2ShapeId second) {
        return first == second || first != null && second != null
                && first.index1 == second.index1
                && first.world0 == second.world0
                && first.generation == second.generation;
    }

    public static boolean sameJoint(b2JointId first, b2JointId second) {
        return first == second || first != null && second != null
                && first.index1 == second.index1
                && first.world0 == second.world0
                && first.generation == second.generation;
    }

    public static boolean isPhysicsEnabled(TiledObjectEntity object) {
        Object physics = object.getProperty("physics");
        if (physics instanceof Boolean) {
            return (Boolean) physics;
        }
        if (physics != null) {
            return Boolean.parseBoolean(String.valueOf(physics));
        }
        return true;
    }

    public static boolean hasPhysicalCollisions(Tile tile) {
        if (tile == null || tile.getCollisions() == null) {
            return false;
        }
        List<TiledObjectEntity> collisions = tile.getCollisions().getObjects();
        for (int i = 0; i < collisions.size(); i++) {
            TiledObjectEntity collision = collisions.get(i);
            if (isPhysicsEnabled(collision)) {
                return true;
            }
        }
        return false;
    }

    public static void applyControl(TiledEntity entity, b2WorldId phy) {
        if (!Components.has(entity, TiledPhysicsComponent.class)) {
            TiledPhysicsComponent phyComp = new TiledPhysicsComponent();
            Components.mount(entity, phyComp).enable();
        }
    }

    public static void applyToLayer(TiledLayer entity, b2WorldId phy) {
        if (entity instanceof TiledTileLayer) {
            TiledTileLayer layer = (TiledTileLayer) entity;
            for (int y = 0; y < layer.getHeight(); y++) {
                for (int x = 0; x < layer.getWidth(); x++) {
                    TiledTileEntity tile = layer.getTileAt(x, y);
                    if(
                        tile==null
                        ||tile.getTile()==null
                        || !hasPhysicalCollisions(tile.getTile())
                    ) {
                        continue;
                    }
                    applyControl(tile,  phy);
                }
            }
        } else if (entity instanceof TiledObjectLayer) {
            TiledObjectLayer group = (TiledObjectLayer) entity;
            List<TiledObjectEntity> objects = group.getObjects();
            for (int i = 0; i < objects.size(); i++) {
                TiledObjectEntity obj = objects.get(i);
                if(obj.getShape()==ObjectShape.TILE && !hasPhysicalCollisions(obj.getTile())) {
                    continue;
                }
                if(obj.getShape()==ObjectShape.POINT || !isPhysicsEnabled(obj)){
                    continue;
                }
                applyControl(obj,  phy);
            }
        }
    }

    public static void apply(b2WorldId phy, TiledBase entity) {

        if (phy != null && b2World_IsValid(phy)) {
            if (entity instanceof TiledMap) {
                List<TiledLayer> layers = ((TiledMap) entity).getLayers();
                for (int i = 0; i < layers.size(); i++) {
                    TiledLayer layer = layers.get(i);
                    // if (layer.getName().equalsIgnoreCase("collisions")) {
                        applyToLayer(layer, phy);
                    // }
                }
            } else if (entity instanceof TiledLayer) {
                applyToLayer((TiledLayer) entity, phy);
            }
        }

    }

}
