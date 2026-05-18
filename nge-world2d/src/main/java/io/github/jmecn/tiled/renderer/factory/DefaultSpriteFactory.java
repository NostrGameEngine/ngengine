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

package io.github.jmecn.tiled.renderer.factory;

import java.util.Map.Entry;
import java.util.Set;
import java.util.logging.Logger;

import org.ngengine.platform.NGEUtils;

import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.scene.*;
import io.github.jmecn.tiled.animation.AnimatedTileControl;
import io.github.jmecn.tiled.core.*;
import io.github.jmecn.tiled.core.entity.TiledObjectEntity;
import io.github.jmecn.tiled.core.tileset.Tile;
import io.github.jmecn.tiled.enums.ObjectShape;


/**
 * desc:
 *
 * @author yanmaoyuan
 */
public class DefaultSpriteFactory implements SpriteFactory {

    private final static Logger logger = Logger.getLogger(DefaultSpriteFactory.class.getName());

    private MeshFactory meshFactory;
    private MaterialFactory materialFactory;

    public DefaultSpriteFactory() {
        this(null, null);
    }

    public DefaultSpriteFactory(MeshFactory meshFactory, MaterialFactory materialFactory) {
        this.meshFactory = meshFactory;
        this.materialFactory = materialFactory;
    }

    /**
     * Get the MaterialFactory for creating tile material.
     * @return the MaterialFactory
     */
    public MaterialFactory getMaterialFactory() {
        return materialFactory;
    }

    /**
     * Set the MaterialFactory for creating tile material.
     * @param materialFactory the MaterialFactory
     */
    public void setMaterialFactory(MaterialFactory materialFactory) {
        this.materialFactory = materialFactory;
    }

    /**
     * Get the MeshFactory for creating tile mesh.
     * @return the MeshFactory
     */
    public MeshFactory getMeshFactory() {
        return meshFactory;
    }

    /**
     * Set the MeshFactory for creating tile mesh.
     * @param meshFactory the MeshFactory
     */
    public void setMeshFactory(MeshFactory meshFactory) {
        this.meshFactory = meshFactory;
    }

  
    public void applyProperties(TiledBase tile, Spatial spatial){
         Set<String> keys = tile.listPropertyKeys();
        // for(Entry<Object, Object> entry : props.entrySet()) {
        for(String key : keys){
            key = NGEUtils.safeString(key);
            Object value =  tile.getProperty(key);
            if(!UserData.isSupportedType(value)) continue;
            spatial.setUserData(key, value);                   
        }
    }
  
 
    @Override
    public Geometry newTileSprite(Tile tile, Material material) {
        Mesh mesh = meshFactory.getTileMesh(tile);
        String name = "tile#" + tile.getGid();
        Geometry geometry = new Geometry(name, mesh);
        geometry.setMaterial(material);
        if (tile.isAnimated()) {
            geometry.addControl(new AnimatedTileControl(tile));
        }
        return geometry;
    }

    public Geometry newObjectSprite(TiledObjectEntity obj) {
        Mesh mesh = meshFactory.newObjectMesh(obj);
        if (mesh == null) {
            return null;
        }

        Geometry geometry = new Geometry(obj.getName(), mesh);
        return geometry;
    }

    private Geometry text(TiledObjectEntity obj) {
        // TODO render text
        TiledObjectText objectText = obj.getTextData();
        return null;
    }

    @Override
    public void setAnimation(Spatial visual, Tile tile) {
        AnimatedTileControl anim = visual.getControl(AnimatedTileControl.class);
        if (tile == null) {
            if (anim != null) {
                visual.removeControl(anim);
            }
            return;
        }
        if(anim == null && tile.isAnimated()){
            visual.addControl(new AnimatedTileControl(tile));
        } else if(anim != null){
            if(!tile.isAnimated()){
                visual.removeControl(anim);
            } else {
                anim.setTile(tile);
            }                    
        }
    }

    @Override
    public void setAnimation(Spatial spatial, TiledObjectEntity object) {
        if(object.getShape()==ObjectShape.TILE){
            Tile tile = object.getTile();
            setAnimation(spatial, tile);
        }
    }

}
