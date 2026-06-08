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

package org.ngengine.world2d.tiled.loader;

import com.jme3.asset.AssetInfo;
import com.jme3.asset.AssetKey;
import com.jme3.asset.AssetLoadException;
import com.jme3.asset.AssetManager;
import org.ngengine.world2d.tiled.animation.Animation;
import org.ngengine.world2d.tiled.animation.Frame;
import org.ngengine.world2d.tiled.core.*;
import org.ngengine.world2d.tiled.core.entity.TiledImageEntity;
import org.ngengine.world2d.tiled.core.tileset.Tile;
import org.ngengine.world2d.tiled.core.tileset.Tileset;
import org.ngengine.world2d.tiled.core.tileset.TilesetGrid;
import org.ngengine.world2d.tiled.core.tileset.WangColor;
import org.ngengine.world2d.tiled.core.tileset.WangSet;
import org.ngengine.world2d.tiled.core.tileset.WangTile;
import org.ngengine.world2d.tiled.enums.FillMode;
import org.ngengine.world2d.tiled.enums.ObjectAlignment;
import org.ngengine.world2d.tiled.enums.Orientation;
import org.ngengine.world2d.tiled.enums.TileRenderSize;
import org.ngengine.world2d.tiled.loader.layer.ObjectLayerLoader;
import org.ngengine.world2d.tiled.util.ColorUtil;
import org.ngengine.world2d.tiled.util.TileCutter;
import org.ngengine.world2d.tiled.xml.XmlNode;
import org.ngengine.world2d.tiled.xml.XmlParser;
import java.util.logging.Logger;

import java.io.InputStream;
import java.util.Map;

import static org.ngengine.world2d.tiled.TiledConst.*;
import static org.ngengine.world2d.tiled.loader.Utils.*;
import static org.ngengine.world2d.tiled.loader.Utils.getAttribute;

/**
 * desc:
 *
 * @author yanmaoyuan
 */
public final class TilesetLoader {

    private static final Logger logger = Logger.getLogger(TilesetLoader.class.getName());

    private final AssetManager assetManager;
    private final AssetKey<?> assetKey;

    private final ImageLoader imageLoader;
    private final PropertyLoader propertiesLoader;

    public TilesetLoader(AssetManager assetManager, AssetKey<?> assetKey) {
        this.assetManager = assetManager;
        this.assetKey = assetKey;

        this.imageLoader = new ImageLoader(assetManager, assetKey);
        this.propertiesLoader = new PropertyLoader();
    }

    /**
     * Load a TileSet from .tsx file.
     *
     * @param inputStream the input stream of the tileset file
     * @return the loaded tileset
     */
    public Tileset load(final InputStream inputStream) {
        try {
            XmlNode root = XmlParser.parse(inputStream);
            if (root == null || !TILESET.equals(root.getNodeName())) {
                logger.warning("No tileset found in file.");
                throw new AssetLoadException("No tileset found in file.");
            }

            // parse tileset xml
            Tileset tileset = readTileset(root);
            if (tileset.getSource() != null) {
                logger.warning("Recursive external tilesets are not supported. " + tileset.getSource());
            } else {
                tileset.setSource(assetKey.getName());
            }
            return tileset;
        } catch (Exception e) {
            throw new AssetLoadException("Failed loading tileset", e);
        }
    }

    /**
     * Load TileSet from a ".tsx" file.
     *
     * @param source the path to the tileset file
     * @return the loaded tileset
     */
    public Tileset load(String source) {
        String assetPath = toJmeAssetPath(assetManager, assetKey, source);

        // load it with assetManager
        Tileset tileset;
        try {
            AssetInfo assetInfo = assetManager.locateAsset(new AssetKey<>(assetPath));
            tileset = load(assetInfo.openStream());
            return tileset;
        } catch (Exception e) {
            throw new IllegalArgumentException("Tileset " + source + " was not loaded correctly!", e);
        }
    }

    /**
     * <p>read tileset</p>
     *
     * Can contain at most one:
     *   &lt;image&gt;, &lt;tileoffset&gt;, &lt;grid&gt; (since 1.0),
     *   &lt;properties&gt;, &lt;terraintypes&gt;, &lt;wangsets&gt; (since 1.1),
     *   &lt;transformations&gt; (since 1.5)
     * Can contain any number :
     *   &lt;tile&gt;
     *
     * @param node node
     * @return Tileset
     */
    public Tileset readTileset(XmlNode node) {
        int firstGid = getAttribute(node, FIRST_GID, 1);
        String source = getAttributeValue(node, SOURCE);
        String name = getAttributeValue(node, NAME);
        String clazz = getAttribute(node, CLASS, EMPTY);
        int tileWidth = getAttribute(node, TILE_WIDTH, 0);
        int tileHeight = getAttribute(node, TILE_HEIGHT, 0);
        int tileSpacing = getAttribute(node, SPACING, 0);
        int tileMargin = getAttribute(node, MARGIN, 0);
        int tileCount = getAttribute(node, TILE_COUNT, 0);
        int columns = getAttribute(node, COLUMNS, 0);
        String objectAlignment = getAttribute(node, OBJECT_ALIGNMENT, ObjectAlignment.UNSPECIFIED.getValue());
        String tileRenderSize = getAttribute(node, TILE_RENDER_SIZE, TileRenderSize.TILE.getValue());
        String fillMode = getAttribute(node, FILL_MODE, FillMode.STRETCH.getValue());

        Tileset tileset = new Tileset(tileWidth, tileHeight, tileSpacing, tileMargin);
        tileset.setFirstGid(firstGid);
        tileset.setSource(source);
        tileset.setName(name);
        tileset.setClazz(clazz);
        tileset.setTileCount(tileCount);
        tileset.setColumns(columns);
        tileset.setObjectAlignment(objectAlignment);
        tileset.setTileRenderSize(tileRenderSize);
        tileset.setFillMode(fillMode);

        for (XmlNode child : node.getChildNodes()) {

            String nodeName = child.getNodeName();
            switch (nodeName) {
                case IMAGE: {
                    readImage(tileset, child);
                    break;
                }
                case GRID: {
                    tileset.setGrid(readGrid(child));
                    break;
                }
                case TERRAIN_TYPES: {
                    getChildrenByTag(child, TERRAIN).forEach(n -> {
                        TiledTerrain terrain = readTerrain(n);
                        tileset.addTerrain(terrain);
                    });
                    break;
                }
                case TILE:
                    readTile(tileset, child);
                    break;
                case TILE_OFFSET: {
                    int tileOffsetX = getAttribute(child, X, 0);
                    int tileOffsetY = getAttribute(child, Y, 0);
                    tileset.setTileOffset(tileOffsetX, tileOffsetY);
                    break;
                }
                case TRANSFORMATIONS: {
                    tileset.setTransformations(readTransformation(child));
                    break;
                }
                case WANGSETS: {
                    // read wangsets
                    getChildrenByTag(child, WANGSET).forEach(n -> {
                        WangSet wangSet = readWangSet(n);
                        tileset.addWangSet(wangSet);
                    });
                    break;
                }
                default: {
                    if (!PROPERTIES.equals(nodeName) && !TEXT_EMPTY.equals(nodeName)) {
                        logger.warning("Unsupported tileset element: " + nodeName);
                    }
                    break;
                }
            }
        }

        return tileset;
    }

    private void readImage(Tileset tileset, XmlNode node) {
        if (tileset.isImageBased()) {
            logger.warning("Ignoring illegal image element after tileset image.");
            return;
        }

        TiledImageEntity image = imageLoader.load(node);
        tileset.setImage(image);
        tileset.setImageSource(image.getSource());

        int tileWidth = tileset.getTileWidth();
        int tileHeight = tileset.getTileHeight();
        int tileMargin = tileset.getMargin();
        int tileSpacing = tileset.getSpacing();

        TileCutter cutter = new TileCutter(image.getWidth(), image.getHeight(), tileWidth, tileHeight, tileMargin, tileSpacing);
        tileset.setColumns(cutter.getColumns());
        tileset.setTileCount(cutter.getTileCount());

        Tile tile = cutter.getNextTile();
        while (tile != null) {
            tileset.addNewTile(tile);
            tile = cutter.getNextTile();
        }
    }

    private TilesetGrid readGrid(XmlNode node) {
        String orientation = getAttribute(node, ORIENTATION, Orientation.ORTHOGONAL.getValue());
        int width = getAttribute(node, WIDTH, 0);
        int height = getAttribute(node, HEIGHT, 0);
        return new TilesetGrid(Orientation.fromString(orientation), width, height);
    }

    /**
     * read terrain.
     *
     * @param node the node
     * @return the terrain
     */
    private TiledTerrain readTerrain(XmlNode node) {
        final String name = getAttributeValue(node, NAME);
        final int tile = getAttribute(node, TILE, -1);

        TiledTerrain terrain = new TiledTerrain(name);
        terrain.setTile(tile);

        // read properties
        Map<String, Object> props = propertiesLoader.readProperties(node);
        terrain.setProperties(props);

        return terrain;
    }

    private TiledTransformations readTransformation(XmlNode node) {
        // This element is used to describe which transformations can be applied to the tiles
        // (e.g. to extend a Wang set by transforming existing tiles).
        // Whether the tiles in this set can be flipped horizontally (default 0)
        int hFlip = getAttribute(node, H_FLIP, 0);
        // Whether the tiles in this set can be flipped vertically (default 0)
        int vFlip = getAttribute(node, V_FLIP, 0);
        // Whether the tiles in this set can be rotated in 90 degree increments (default 0)
        int rotate = getAttribute(node, ROTATE, 0);
        // Whether untransformed tiles remain preferred, otherwise transformed tiles are used to produce more variations (default 0)
        int preferUntransformed = getAttribute(node, PREFER_UNTRANSFORMED, 0);
        return new TiledTransformations(hFlip, vFlip, rotate, preferUntransformed);
    }

    /**
     * read wangset
     *
     * @param node the node
     * @return the wangset
     */
    private WangSet readWangSet(XmlNode node) {
        String name = getAttributeValue(node, NAME);
        String clazz = getAttribute(node, CLASS, EMPTY);
        int tile = getAttribute(node, TILE, -1);

        WangSet wangSet = new WangSet(name);
        wangSet.setClazz(clazz);
        wangSet.setTile(tile);

        // read properties
        Map<String, Object> props = propertiesLoader.readProperties(node);
        wangSet.setProperties(props);

        for (XmlNode child : node.getChildNodes()) {
            String nodeName = child.getNodeName();
            if (WANGCOLOR.equals(nodeName)) {
                wangSet.addWangColor(readWangColor(child));
            } else if (WANGTILE.equals(nodeName)) {
                wangSet.addWangTile(readWangTile(child));
            }
        }
        return wangSet;
    }

    private WangColor readWangColor(XmlNode node) {
        String name = getAttributeValue(node, NAME);
        String clazz = getAttribute(node, CLASS, EMPTY);
        String color = getAttributeValue(node, COLOR);
        int tile = getAttribute(node, TILE, -1);
        float probability = (float) getDoubleAttribute(node, PROBABILITY, 0.0);

        WangColor wangColor = new WangColor();
        wangColor.setName(name);
        wangColor.setClazz(clazz);
        wangColor.setColor(ColorUtil.toColorRGBA(color));
        wangColor.setTile(tile);
        wangColor.setProbability(probability);

        // read properties
        Map<String, Object> props = propertiesLoader.readProperties(node);
        wangColor.setProperties(props);

        return wangColor;
    }

    private WangTile readWangTile(XmlNode node) {
        int tileId = getAttribute(node, TILE_ID, -1);
        String wangId = getAttribute(node, WANGID, EMPTY);
        return new WangTile(tileId, wangId);
    }

    /**
     * Read Tile for tileset
     * <p>
     * Can contain: properties, image (since 0.9), objectgroup (since 0.10),
     * animation (since 0.10)
     * </p>
     *
     * @param tileset the Tileset
     * @param node the node representing the "tile" element
     */
    private void readTile(Tileset tileset, XmlNode node) {

        Tile tile;

        int id = getAttribute(node, ID, -1);

        if (!tileset.isImageBased() || id > tileset.getMaxTileId()) {
            tile = new Tile();
            tile.setId(id);
            tile.setGid(id + tileset.getFirstGid());

            tileset.addTile(tile);
        } else {
            tile = tileset.getTile(id);
        }

        // since 1.9
        int x = getAttribute(node, X, -1);
        int y = getAttribute(node, Y, -1);
        int width = getAttribute(node, WIDTH, -1);
        int height = getAttribute(node, HEIGHT, -1);
        String clazz = getAttribute(node, TYPE, getAttribute(node, CLASS, EMPTY));// compatibility with 1.8 or earlier
        tile.setClazz(clazz);

        if (x > -1) {
            tile.setX(x);
        }
        if (y > -1) {
            tile.setY(y);
        }
        if (width > -1) {
            tile.setWidth(width);
        }
        if (height > -1) {
            tile.setHeight(height);
        }

        // in <tileset> we need id, terrain, probability
        String terrainStr = getAttributeValue(node, TERRAIN);
        if (terrainStr != null) {
            String[] tileIds = terrainStr.split("[\\s]*,[\\s]*");

            assert tileIds.length == 4;

            int terrain = 0;
            for (int i = 0; i < 4; i++) {
                int tid = Integer.parseInt(tileIds[i]);
                terrain |= tid << 8 * (3 - i);
            }

            tile.setTerrain(terrain);
        }

        float probability = (float) getDoubleAttribute(node, PROBABILITY, 0.0);
        tile.setProbability(probability);

        Map<String, Object> props = propertiesLoader.readProperties(node);
        tile.setProperties(props);

        readTileImage(tile, node);
        readAnimation(tile, node);
        readCollision(tile, node);
    }

    private void readTileImage(Tile tile, XmlNode parent) {
        XmlNode node = getChildByTag(parent, IMAGE);
        if (node == null) {
            return;
        }

        TiledImageEntity image = imageLoader.load(node);
        tile.setImage(image);
        // use the tile image size as tile size by default
        if (tile.getWidth() <= 0 && tile.getHeight() <= 0) {
            tile.setWidth(image.getWidth());
            tile.setHeight(image.getHeight());
        }
    }

    private void readAnimation(Tile tile, XmlNode parent) {
        XmlNode node = getChildByTag(parent, ANIMATION);
        if (node == null) {
            return;
        }

        String name = getAttribute(node, NAME, "Default");

        Animation animation = new Animation(name);
        for (XmlNode frameNode : node.getChildNodes()) {
            if (frameNode.getNodeName().equals(FRAME)) {
                int tileId = getAttribute(frameNode, TILE_ID, 0);
                int duration = getAttribute(frameNode, DURATION, 0);
                animation.addFrame(new Frame(tileId, duration));
            }
        }
        tile.addAnimation(animation);
    }

    /**
     * Tile collision is stored in the objectgroup of the tile.
     * @param tile the tile
     * @param parent the parent node
     */
    private void readCollision(Tile tile, XmlNode parent) {
        XmlNode node = getChildByTag(parent, OBJECT_GROUP);
        if (node == null) {
            return;
        }

    ObjectLayerLoader loader = new ObjectLayerLoader(assetManager, assetKey);
    TiledObjectLayer objectGroup = loader.load(node);
        if (objectGroup != null && !objectGroup.getObjects().isEmpty()) {
            tile.setCollisions(objectGroup);
        }
    }

}
