package io.github.jmecn.tiled.renderer.factory;

import com.jme3.asset.AssetManager;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector4f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.texture.Texture;
import io.github.jmecn.tiled.core.MapObject;
import io.github.jmecn.tiled.core.Tile;
import io.github.jmecn.tiled.core.TiledImage;
import io.github.jmecn.tiled.core.Tileset;
import io.github.jmecn.tiled.util.ColorUtil;

import static io.github.jmecn.tiled.renderer.MaterialConst.*;

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
        TiledImage image = tileset.getImage();
        setTiledImage(material, image);

        int tileWidth = tileset.getTileWidth();
        int tileHeight = tileset.getTileHeight();
        int tileMargin = tileset.getMargin();
        int tileSpacing = tileset.getSpacing();

        material.setBoolean(USE_TILESET_IMAGE, true);
        material.setVector4(TILE_SIZE, new Vector4f(tileWidth, tileHeight, tileMargin, tileSpacing));
    }

    @Override
    public void setTiledImage(Material mat, TiledImage image) {
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

    public void setMapObject(Material mat, MapObject obj) {
         
        switch (obj.getShape()) {
            case IMAGE: {
                setTiledImage(mat, obj.getImage());
                break;
            }
            case TILE: {
                setTile(mat, obj.getTile());
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
