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

import com.jme3.asset.AssetManager;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector4f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.texture.Texture;

import org.ngengine.world2d.tiled.core.entity.TiledImageEntity;
import org.ngengine.world2d.tiled.core.entity.TiledObjectEntity;
import org.ngengine.world2d.tiled.core.tileset.Tile;
import org.ngengine.world2d.tiled.core.tileset.Tileset;
import org.ngengine.world2d.tiled.util.ColorUtil;

import static org.ngengine.world2d.tiled.renderer.MaterialConst.*;

import java.util.logging.Logger;

/**
 * desc:
 *
 * @author yanmaoyuan
 */
public class DefaultMaterialFactory implements MaterialFactory {
    private static final Logger logger = Logger.getLogger(DefaultMaterialFactory.class.getName());
    private final AssetManager assetManager;

    public DefaultMaterialFactory(AssetManager assetManager) {
        this.assetManager = assetManager;

    }

    @Override
    public Material newMaterial() {
        return new Material(assetManager, TILED_J3MD);
    }

    @Override
    public void setTile(Material material, Tile tile) {
        Tileset tileset = tile.getTileset();
        if (tileset != null) {
            if (tileset.isImageBased()) {
                // this tile comes from a collection of images.
                setTileset(material, tileset);
            } else {
                // this tile comes from an imaged based tileset.
                setTiledImage(material, tile.getImage());
                material.setBoolean(USE_TILESET_IMAGE, true);
                material.setVector4(TILE_SIZE, new Vector4f(tile.getWidth(), tile.getHeight(), 0f, 0f));
            }
        } else {
            if (tile.getImage() != null) {
                setTiledImage(material, tile.getImage());
                material.setBoolean(USE_TILESET_IMAGE, true);
                material.setVector4(TILE_SIZE, new Vector4f(tile.getWidth(), tile.getHeight(), 0f, 0f));
            } else {
                throw new IllegalArgumentException("Tileset or Image is required!");
            }
        }
    }

    @Override
    public void setTileset(Material material, Tileset tileset) {
        if (!tileset.isImageBased()) {
            throw new IllegalArgumentException("Tileset must be image based!");
        }
        TiledImageEntity image = tileset.getImage();
        setTiledImage(material, image);

        int tileWidth = tileset.getTileWidth();
        int tileHeight = tileset.getTileHeight();
        int tileMargin = tileset.getMargin();
        int tileSpacing = tileset.getSpacing();

        material.setBoolean(USE_TILESET_IMAGE, true);
        material.setVector4(TILE_SIZE, new Vector4f(tileWidth, tileHeight, tileMargin, tileSpacing));
    }

    @Override
    public void setTiledImage(Material mat, TiledImageEntity image) {
        Texture texture = image.getTexture();

        // create material
        mat.setTexture(COLOR_MAP, texture);
        if (image.getTrans() != null) {
            ColorRGBA transparentColor = ColorUtil.toColorRGBA(image.getTrans());
            mat.setColor(TRANS_COLOR, transparentColor);
        }
        mat.setVector2(IMAGE_SIZE, new Vector2f(image.getWidth(), image.getHeight()));
    }

    @Override
    public void setColor(Material mat, ColorRGBA color) {
        mat.setColor(COLOR, color);
    }


   

    
 

    @Override
    public void setTintColor(Material material, ColorRGBA tintColor) {
        if (tintColor != null) {
            material.setBoolean(USE_TINT_COLOR, true);
            material.setColor(TINT_COLOR, tintColor);
        } else {
            material.setBoolean(USE_TINT_COLOR, false);
        }
    }

    @Override
    public void setTintColor(Spatial spatial, ColorRGBA tintColor) {
        if (spatial instanceof Geometry) {
            Geometry geometry = (Geometry) spatial;
            setTintColor(geometry.getMaterial(), tintColor);
        } else {
            Node node = (Node) spatial;
            for (Spatial child : node.getChildren()) {
                if (child instanceof Geometry) {
                    Geometry geometry = (Geometry) child;
                    setTintColor(geometry.getMaterial(), tintColor);
                }
            }
        }
    }

    @Override
    public void setLayerOpacity(Material material, float opacity) {
        material.setFloat(LAYER_OPACITY, opacity);
    }

    @Override
    public void setLayerOpacity(Spatial spatial, float opacity) {
        if (spatial instanceof Geometry) {
            Geometry geometry = (Geometry) spatial;
            setLayerOpacity(geometry.getMaterial(), opacity);
        } else {
            Node node = (Node) spatial;
            for (Spatial child : node.getChildren()) {
                if (child instanceof Geometry) {
                    Geometry geometry = (Geometry) child;
                    setLayerOpacity(geometry.getMaterial(), opacity);
                }
            }
        }
    }

    @Override
    public void setOpacity(Material material, float opacity) {
        material.setFloat(OPACITY, opacity);
    }

    @Override
    public void setOpacity(Spatial spatial, float opacity) {
        if (spatial instanceof Geometry) {
            Geometry geometry = (Geometry) spatial;
            setOpacity(geometry.getMaterial(), opacity);
        } else {
            Node node = (Node) spatial;
            for (Spatial child : node.getChildren()) {
                if (child instanceof Geometry) {
                    Geometry geometry = (Geometry) child;
                    setOpacity(geometry.getMaterial(), opacity);
                }
            }
        }
    }

    public void setMapObject(Material mat, TiledObjectEntity obj) {
         
        switch (obj.getShape()) {
            case IMAGE: {
                setTiledImage(mat, obj.getImage());
                break;
            }
            case TILE: {
                if (obj.getTile() != null) {
                    setTile(mat, obj.getTile());
                }
                break;
            }
            case TEXT: {
                // TODO not supported yet
                logger.warning("Text object is not supported yet.");
                break;
            }
            default: {
                break;
            }
        }

    }
}
