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

package org.ngengine.world2d.tiled.core;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector2f;
import com.jme3.util.SafeArrayList;
import com.jme3.util.TempVars;

import org.ngengine.world2d.tiled.core.entity.TiledObjectEntity;
import org.ngengine.world2d.tiled.enums.DrawOrder;
import org.ngengine.world2d.tiled.math2d.Bound2D;
import org.ngengine.world2d.tiled.util.CoordinateSystem;

/**
 * The object group is in fact a map layer, and is hence called "object layer"
 * in Tiled Qt.
 * 
 * @author yanmaoyuan
 * 
 */
public class TiledObjectLayer extends TiledLayer {

    /**
     * The color used to display the objects in this group.
     */
    private ColorRGBA color;

    /**
     * Whether the objects are drawn according to the order of appearance
     * ("index") or sorted by their y-coordinate ("topdown"). Defaults to
     * "topdown".
     */
    private DrawOrder drawOrder = DrawOrder.TOPDOWN;

    private final List<TiledObjectEntity> objects = new SafeArrayList<>(TiledObjectEntity.class);

    public TiledObjectLayer() {
        // for serialization
    }

    public TiledObjectLayer(int width, int height) {
        super(width, height);
    }

    public ColorRGBA getColor() {
        return color;
    }

    public void setColor(ColorRGBA color) {
        this.color = color;
    }
    
    public DrawOrder getDrawOrder() {
        return drawOrder;
    }

    public void setDrawOrder(DrawOrder drawOrder) {
        this.drawOrder = drawOrder;
    }

    public List<TiledObjectEntity> getObjects() {
        return objects;
    }

    public TiledObjectEntity get(int id) {
        return get(BigInteger.valueOf(id));
    }

    public TiledObjectEntity get(BigInteger id) {
        if (id == null) {
            return null;
        }
        for (TiledObjectEntity obj : objects) {
            if (id.equals(obj.getId())) {
                return obj;
            }
        }
        return null;
    }
    
    public void add(TiledObjectEntity obj) {
        obj.setObjectGroup(this);
        objects.add(obj);
        obj.attached();
    }

    public void remove(TiledObjectEntity o) {
        if (!objects.remove(o)) {
            return;
        }
        // Keep the layer/map scope available while lifecycle cleanup runs.
        // Netcode cleanup uses that scope to address the same remote handler
        // that was registered while the object was attached.
        o.detached();
        o.setObjectGroup(null);
    }

    /**
     * <p>
     * getObjectAt.
     * </p>
     * 
     * @param x a double.
     * @param y a double.
     * @return a {@link TiledObjectEntity} object.
     */
    public TiledObjectEntity getObjectAt(double x, double y) {
        for (TiledObjectEntity obj : objects) {
            // Attempt to get an object bordering the point that has no width
            if (obj.getWidth() == 0 && obj.getX() == x) {
                return obj;
            }

            // Attempt to get an object bordering the point that has no height
            if (obj.getHeight() == 0 && obj.getY() == y) {
                return obj;
            }

            // TODO check if the object is a polygon or polyline, handle rotation
            Bound2D rect = new Bound2D(obj.getX(), obj.getY(), obj.getWidth(), obj.getHeight());
            if (rect.contains(x, y)) {
                return obj;
            }
        }
        return null;
    }

    public void getObjectsAt(double x, double y, List<TiledObjectEntity> out) {
        for (TiledObjectEntity obj : objects) {
            boolean contains = false;
            // Attempt to get an object bordering the point that has no width
            if (obj.getWidth() == 0 && obj.getX() == x) {
                contains |= true;
            }

            // Attempt to get an object bordering the point that has no height
            if (obj.getHeight() == 0 && obj.getY() == y) {
                contains |= true;
            }

            // TODO check if the object is a polygon or polyline, handle rotation
            Bound2D rect = new Bound2D(obj.getX(), obj.getY(), obj.getWidth(), obj.getHeight());
            if (rect.contains(x, y)) {
                contains |= true;
            }

            if (contains) {
                out.add(obj);
            }
        }
    }


    @Override
    public void getNearby(CoordinateSystem coords, Vector2f worldPos, float radius, List<TiledBase> out) {
        try (TempVars temp = TempVars.get()) {

            for (TiledObjectEntity obj : getObjects()) {
                Vector2f wpos = temp.vect2d;
                coords.getCenterInGridSpace(obj, wpos);
                 
                float dx = wpos.x - worldPos.x;
                float dy = wpos.y - worldPos.y;
                float dist2 = dx * dx + dy * dy;
                if (dist2 <= radius * radius) {
                    out.add(obj);
                }
            }
        }

    }

}
