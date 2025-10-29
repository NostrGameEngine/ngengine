package io.github.jmecn.tiled.loader;

import com.jme3.asset.AssetKey;
import com.jme3.asset.AssetManager;
import com.jme3.math.ColorRGBA;
import io.github.jmecn.tiled.core.*;
import io.github.jmecn.tiled.enums.Orientation;
import io.github.jmecn.tiled.enums.RenderOrder;
import io.github.jmecn.tiled.enums.StaggerAxis;
import io.github.jmecn.tiled.enums.StaggerIndex;
import io.github.jmecn.tiled.loader.layer.LayerLoaders;
import io.github.jmecn.tiled.util.ColorUtil;
import io.github.jmecn.tiled.xml.XmlNode;
import io.github.jmecn.tiled.xml.XmlParser;

import java.util.logging.Logger;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Properties;

import static io.github.jmecn.tiled.TiledConst.*;
import static io.github.jmecn.tiled.loader.Utils.*;

/**
 * desc:
 *
 * @author yanmaoyuan
 */
public final class MapLoader {

    private static final Logger logger = Logger.getLogger(MapLoader.class.getName());

    private final AssetManager assetManager;

    private final AssetKey<?> assetKey;

    private TiledMap map;

    private final TilesetLoader tilesetLoader;
    private final PropertyLoader propertiesLoader;

    public MapLoader(AssetManager assetManager, AssetKey<?> key) {
        this.assetManager = assetManager;
        this.assetKey = key;

        this.tilesetLoader = new TilesetLoader(assetManager, key);
        this.propertiesLoader = new PropertyLoader();
    }

    /**
     * Load a Map from .tmx file
     *
     * @param inputStream InputStream
     * @return the TiledMap
     * @throws IOException if an error occurs while reading the map
     */
    public TiledMap load(InputStream inputStream) throws IOException {
        XmlNode root = XmlParser.parse(inputStream);
        return readMap(root);
    }

    public TiledMap readMap(XmlNode mapNode) throws IOException {

        if (!MAP.equals(mapNode.getNodeName())) {
            throw new IllegalArgumentException("Not a valid tmx map file.");
        }

        // Load other map attributes
        String version = getAttributeValue(mapNode, VERSION);
        String tiledVersion = getAttributeValue(mapNode, TILED_VERSION);
        String clazz = getAttribute(mapNode, CLASS, EMPTY);
        String orientation = getAttribute(mapNode, ORIENTATION, Orientation.ORTHOGONAL.getValue());
        String renderOrder = getAttribute(mapNode, RENDER_ORDER, RenderOrder.RIGHT_DOWN.getValue());
        int compressionLevel = getAttribute(mapNode, COMPRESSION_LEVEL, -1);
        int width = getAttribute(mapNode, WIDTH, 0);
        int height = getAttribute(mapNode, HEIGHT, 0);
        int tileWidth = getAttribute(mapNode, TILE_WIDTH, 0);
        int tileHeight = getAttribute(mapNode, TILE_HEIGHT, 0);
        int hexSideLength = getAttribute(mapNode, HEX_SIDE_LENGTH, 0);
        String staggerAxis = getAttribute(mapNode, STAGGER_AXIS, StaggerAxis.Y.getValue());
        String staggerIndex = getAttribute(mapNode, STAGGER_INDEX, StaggerIndex.ODD.getValue());
        int parallaxOriginX = getAttribute(mapNode, PARALLAX_ORIGIN_X, 0);
        int parallaxOriginY = getAttribute(mapNode, PARALLAX_ORIGIN_Y, 0);
        String backgroundColorStr = getAttributeValue(mapNode, BACKGROUND_COLOR);
        int nextLayerId = getAttribute(mapNode, NEXT_LAYER_ID, 0);
        int nextObjectId = getAttribute(mapNode, NEXT_OBJECT_ID, 0);
        boolean infinite = getAttribute(mapNode, INFINITE, 0) == 1;

        if (width <= 0 || height <= 0) {
            logger.warning("Couldn't locate map dimensions.");
            throw new IllegalArgumentException("Couldn't locate map dimensions.");
        }

        map = new TiledMap(width, height);
        map.setVersion(version);
        map.setTiledVersion(tiledVersion);
        map.setClazz(clazz);
        map.setOrientation(orientation);
        map.setRenderOrder(RenderOrder.fromString(renderOrder));
        map.setCompressionLevel(compressionLevel);
        map.setTileWidth(tileWidth);
        map.setTileHeight(tileHeight);
        map.setHexSideLength(hexSideLength);
        map.setStaggerAxis(staggerAxis);
        map.setStaggerIndex(staggerIndex);
        map.setParallaxOriginX(parallaxOriginX);
        map.setParallaxOriginY(parallaxOriginY);

        ColorRGBA backgroundColor;
        if (backgroundColorStr != null) {
            backgroundColor = ColorUtil.toColorRGBA(backgroundColorStr);
            map.setBackgroundColor(backgroundColor);
        }

        map.setNextLayerId(nextLayerId);
        map.setNextObjectId(nextObjectId);
        map.setInfinite(infinite);

        // Load properties
        Properties props = propertiesLoader.readProperties(mapNode);
        map.setProperties(props);

    // read tilesets
    readTilesets(mapNode);

    // read layers
    readLayers(mapNode);

        return map;
    }

    public void readTilesets(XmlNode mapNode) {
        List<XmlNode> tileSets = XmlParser.getElementsByTagName(mapNode, TILESET);
        for (int i = 0; i < tileSets.size(); i++) {
            XmlNode node = tileSets.get(i);

            String source = getAttributeValue(node, SOURCE);
            int firstGid = getAttribute(node, FIRST_GID, 1);

            Tileset tileset;
            if (source != null) {
                logger.info("Load tileset: " + source);
                tileset = (Tileset) assetManager.loadAsset(assetKey.getFolder() + source);
                // as first gid is a map related property, we need to update it.
                tileset.updateFirstGid(firstGid);
                tileset.setSource(source);
            } else {
                tileset = tilesetLoader.readTileset(node);
            }

            // Set tile width and height if not set
            if (tileset.getTileWidth() <= 0) {
                tileset.setTileWidth(map.getTileWidth());
                logger.fine("Tileset "+ tileset.getName() +" has no tile width. Using map tile width: " + map.getTileWidth());
            }
            if (tileset.getTileHeight() <= 0) {
                tileset.setTileHeight(map.getTileHeight());
                logger.fine("Tileset " + tileset.getName() + " has no tile height. Using map tile height: " + map.getTileHeight());
            }

            map.addTileset(tileset);
        }
    }

    private void readLayers(XmlNode mapNode) throws IOException {
        LayerLoaders layerLoaders = new LayerLoaders(assetManager, assetKey, map);

        XmlNode child = mapNode.getFirstChild();
        while (child != null) {
            String childName = child.getNodeName();
            // ignore tileset and properties
            if (!TILESET.equals(childName) && !PROPERTIES.equals(childName) && !TEXT_EMPTY.equals(childName)) {
                LayerLoader layerLoader = layerLoaders.create(childName);
                if (layerLoader != null) {
                    Layer layer = layerLoader.load(child);
                    // in case the layer has no dimensions, set the map dimensions
                    if (layer.getWidth() == 0 && layer.getHeight() == 0) {
                        layer.setWidth(map.getWidth());
                        layer.setHeight(map.getHeight());
                    }
                    map.addLayer(layer);
                }
            }
            child = child.getNextSibling();
        }
    }
}
