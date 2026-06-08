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

import org.ngengine.world2d.tiled.core.entity.TiledObjectEntity;
import org.ngengine.world2d.tiled.core.tileset.Tileset;

/**
 * desc:
 *
 * @author yanmaoyuan
 */
public class TiledObjectTemplate {
    private String source;
    private Tileset tileset;
    private TiledObjectEntity object;

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public Tileset getTileset() {
        return tileset;
    }

    public void setTileset(Tileset tileset) {
        this.tileset = tileset;
    }

    public TiledObjectEntity getObject() {
        return object;
    }

    public void setObject(TiledObjectEntity object) {
        this.object = object;
    }

    @Override
    public String toString() {
        return "ObjectTemplate{" +
                "source='" + source + '\'' +
                ", tileset=" + tileset +
                ", object=" + object +
                '}';
    }

    public void copyTo(TiledObjectEntity obj) {
        obj.setName(object.getName());
        obj.setClazz(object.getClazz());
        obj.setWidth(object.getWidth());
        obj.setHeight(object.getHeight());
        obj.setShape(object.getShape());
        obj.setGid(object.getGid());// for Shape.TILE
        obj.setTile(object.getTile());// for Shape.TILE
        obj.setPoints(object.getPoints());// for Shape.POLYLINE, Shape.POLYGON
        obj.setImage(object.getImage());// for Shape.IMAGE
        obj.setTextData(object.getTextData());// for Shape.TEXT
        obj.setVisible(object.isVisible());
        obj.setRotation(object.getRotation());
//        obj.setProperties(object.getProperties());
        obj.clearProperties();
        object.copyPropertiesTo(obj);
    }
}