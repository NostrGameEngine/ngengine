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

package io.github.jmecn.tiled.loader.layer;

import com.jme3.asset.*;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector2f;
import io.github.jmecn.tiled.core.*;
import io.github.jmecn.tiled.core.entity.TiledImageEntity;
import io.github.jmecn.tiled.core.entity.TiledObjectEntity;
import io.github.jmecn.tiled.core.tileset.Tile;
import io.github.jmecn.tiled.core.tileset.Tileset;
import io.github.jmecn.tiled.enums.DrawOrder;
import io.github.jmecn.tiled.enums.ObjectShape;
import io.github.jmecn.tiled.loader.LayerLoader;
import io.github.jmecn.tiled.loader.TiledMapKey;
import io.github.jmecn.tiled.loader.Utils;
import io.github.jmecn.tiled.util.ColorUtil;
import java.io.InputStream;
 import java.lang.ref.WeakReference;

import io.github.jmecn.tiled.xml.XmlNode;
import io.github.jmecn.tiled.xml.XmlParser;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.ngengine.platform.NGEPlatform;
import org.ngengine.platform.NGEUtils;

import static io.github.jmecn.tiled.TiledConst.*;
import static io.github.jmecn.tiled.loader.Utils.*;
import static io.github.jmecn.tiled.loader.Utils.getAttribute;

/**
 * desc:
 *
 * @author yanmaoyuan
 */
public class ObjectLayerLoader extends LayerLoader {
    private static final Logger logger = Logger.getLogger(ObjectLayerLoader.class.getName());
    private final TiledMap map;
    private Map<Integer,WeakReference<Tile>> cache = new HashMap<>();

    public ObjectLayerLoader(AssetManager assetManager, AssetKey<?> key) {
        this(assetManager, key, null);
    }

    public ObjectLayerLoader(AssetManager assetManager, AssetKey<?> key, TiledMap map) {
        super(assetManager, key);

        if (map != null) {// use the map from the constructor
            this.map = map;
        } else {
            if (key instanceof TiledMapKey) {// use the map from the key
                this.map = ((TiledMapKey<?>) key).getTiledMap();
            } else {
                this.map = null;
            }
        }
    }

    @Override
    public TiledObjectLayer load(XmlNode node) {
        int defWidth = map == null ? 0 : map.getWidth();
        int defHeight = map == null ? 0 : map.getHeight();
        final int width = getAttribute(node, WIDTH, defWidth);
        final int height = getAttribute(node, HEIGHT, defHeight);

        TiledObjectLayer layer = new TiledObjectLayer(width, height);
        readLayerBase(node, layer);

        final String color = getAttributeValue(node, COLOR);
        final ColorRGBA borderColor;
        if (color != null) {
            borderColor = ColorUtil.toColorRGBA(color);
        } else {
            borderColor = ColorRGBA.LightGray.clone();
        }
        layer.setColor(borderColor);

        final String drawOrder = getAttributeValue(node, DRAW_ORDER);
        if (drawOrder != null) {
            layer.setDrawOrder(DrawOrder.fromValue(drawOrder));
        }

        // Add all objects from the objects group
        for (XmlNode child : node.getChildNodes()) {
            if (OBJECT.equals(child.getNodeName())) {
                TiledObjectEntity obj = readObjectNode(child);
                layer.add(obj);
            }
        }
        return layer;
    }

    /**
     * Load an object template (.tx) from an input stream.
     * @param inputStream input stream for the template file
     * @return parsed ObjectTemplate
     */
    public TiledObjectTemplate loadObjectTemplate(final InputStream inputStream) {
        TiledObjectTemplate template;
        XmlNode root;

        try {
            root = XmlParser.parse(inputStream);
            if (root == null || !TEMPLATE.equals(root.getNodeName())) {
                logger.warning("Not a valid template file.");
                throw new IllegalArgumentException("Not a valid template file");
            }

            template = readObjectTemplate(root);
            return template;
        } catch (Exception e) {
            throw new AssetLoadException("Failed loading template", e);
        }
    }
    /**
     * Load an object template by source path using the AssetManager.
     * @param source the source of the template
     * @return the loaded template
     */
    private TiledObjectTemplate loadObjectTemplate(String source) {

        TiledObjectTemplate objectTemplate;

        // try to load cached objectTemplate from the map
        if (map != null) {
            objectTemplate = map.getObjectTemplate(source);
            if (objectTemplate != null) {
                return objectTemplate;
            }
        }

        // load it with assetManager
        try {
            logger.info("Loading template: " + source);
            objectTemplate = assetManager.loadAsset(new TiledMapKey<>(assetKey.getFolder() + source, map));
            objectTemplate.setSource(source);
            if (map != null) {// cache the objectTemplate
                map.addObjectTemplate(objectTemplate);
            }
            return objectTemplate;
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Template " + source + " was not loaded correctly!", e);
            throw new AssetLoadException("Template not found: " + source);
        }
    }

    private TiledObjectTemplate readObjectTemplate(XmlNode node) {
        Tileset tileset = null;

        int firstGid = 0;
        String source = null;

        XmlNode tilesetNode = getChildByTag(node, TILESET);// not null if the object is a tile object.
        if (tilesetNode != null) {
            // The readObjectNode method will automatically set the tileset from tiled map,
            // so it doesn't need to load tileset again here.
            firstGid = Utils.getAttribute(tilesetNode, FIRST_GID, 1);
            source = Utils.getAttributeValue(tilesetNode, SOURCE);
        }

        XmlNode objectNode = getChildByTag(node, OBJECT);
        if (objectNode == null) {
            logger.log(Level.SEVERE, "Template must have an object node.");
            throw new IllegalArgumentException("Template must have an object node.");
        }

        TiledObjectEntity obj = readObjectNode(objectNode);
        if (obj.getTile() != null) {
            tileset = obj.getTile().getTileset();
        }

        // notice: the tileset may be null if the object is not a tile object.
        // the obj.getTile() may be null if the template is not loaded from a .tmx file.

        if (obj.getShape() == ObjectShape.TILE && tileset == null) {
            if (source == null) {
                throw new IllegalArgumentException("Template must have a tileset source.");
            }

            // user does not load template from a .tmx, but load it directly from a .tx file.

            // the source may be like "../obj.tsx", should calculate the actual path
            String tilesetPath = AssetKey.reducePath(assetKey.getFolder() + source);
            tileset = (Tileset) assetManager.loadAsset(tilesetPath);
            tileset.updateFirstGid(firstGid);
            tileset.setSource(source);

            // set MapObject tile
            if (obj.getShape() == ObjectShape.TILE) {
                Tile t = tileset.getTile(obj.getGid() - firstGid);
                if (t == null) {
                    logger.warning("Tile not found in tileset: "  + source + ", gid: " + obj.getGid());
                    throw new AssetLoadException("Tile not found in tileset: " + source + ", gid: " + obj.getGid());
                }
                obj.setTile(t);
            }
        }

        TiledObjectTemplate template = new TiledObjectTemplate();
        template.setTileset(tileset);
        template.setObject(obj);
        return template;
    }

    /**
     * Read an object of the ObjectGroup.
     *
     * @param node the node containing the object
     * @return MapObject
     */
    private TiledObjectEntity readObjectNode(XmlNode node) {
        int id = getAttribute(node, ID, 0);
        String name = getAttributeValue(node, NAME);
        String clazz = getAttribute(node, TYPE, getAttribute(node, CLASS, EMPTY));// compatibility with 1.8 or earlier
        double x = getDoubleAttribute(node, X, 0);
        double y = getDoubleAttribute(node, Y, 0);
        double width = getDoubleAttribute(node, WIDTH, 0);
        double height = getDoubleAttribute(node, HEIGHT, 0);
        double rotation = getDoubleAttribute(node, ROTATION, 0);
        String gid = getAttributeValue(node, GID);
        int visible = getAttribute(node, VISIBLE, 1);

        TiledObjectEntity obj = new TiledObjectEntity(id, x, y, width, height);
        
        obj.setRotation(rotation);
        obj.setVisible(visible == 1);
        if (name != null) {
            obj.setName(name);
        }
        if (clazz != null) {
            obj.setClazz(clazz);
        }

        TiledObjectTemplate template;
        String templateSource = getAttributeValue(node, TEMPLATE);
        if (templateSource != null) {
            template = loadObjectTemplate(templateSource);

            obj.setTemplate(templateSource);
            template.copyTo(obj);

            // merge the properties, behavior like inheritance.
            Map<String, Object> props = propertiesLoader.readProperties(node);
            obj.putProperties(props);
            return obj;
        }

        Map<String, Object> props = propertiesLoader.readProperties(node);
        obj.setProperties(props);

        /*
         * if an object have "gid" attribute means it references to a tile.
         */
        if (gid != null) {
            setTileByGid(obj, gid);
        } else {
            readShape(node, obj);
        }

        return obj;
    }

    /**
     * Set tile by gid
     * @param obj the object
     * @param id the gid
     */
    private void setTileByGid(TiledObjectEntity obj, String id) {
        int gid = (int) Long.parseLong(id);
        obj.setShape(ObjectShape.TILE);
        obj.setGid(gid);

        if (map != null) {
            WeakReference<Tile> flippedTileRef = cache.compute(gid, (k,t)->{
                if(t==null||t.get()==null) return null;
                return t;
            });

            Tile tile  = flippedTileRef!=null&&flippedTileRef.get()!=null?flippedTileRef.get():null;
            if(tile==null){
                int tileId = gid & ~Tile.FLIPPED_MASK;
                tile = map.getTileForTileGID(tileId);
                tile = tile.copy();
                tile.setGid(gid);
                cache.put(gid, new WeakReference<>(tile));
                NGEPlatform.get().registerFinalizer(tile, ()->{
                    cache.remove(gid);
                });
            }
          
            obj.setTile(tile);
        }
    }

    /**
     * Read the shape of the object
     * @param node the node containing the shape
     * @param obj the object
     */
    private void readShape(XmlNode node, TiledObjectEntity obj) {
        for (XmlNode child : node.getChildNodes()) {
            String nodeName = child.getNodeName();

            if (!PROPERTIES.equals(nodeName) && !TEXT_EMPTY.equals(nodeName)) {
                switch (nodeName) {
                    case ELLIPSE: {
                        obj.setShape(ObjectShape.ELLIPSE);
                        break;
                    }
                    case POINT: {
                        obj.setShape(ObjectShape.POINT);
                        break;
                    }
                    case POLYGON: {
                        obj.setShape(ObjectShape.POLYGON);
                        obj.setPoints(readPoints(child));
                        break;
                    }
                    case POLYLINE: {
                        obj.setShape(ObjectShape.POLYLINE);
                        obj.setPoints(readPoints(child));
                        break;
                    }
                    case TEXT: {
                        obj.setShape(ObjectShape.TEXT);
                        obj.setTextData(readTextObject(child));
                        break;
                    }
                    case IMAGE: {
                        obj.setShape(ObjectShape.IMAGE);
                        TiledImageEntity image = imageLoader.load(child);
                        obj.setImage(image);
                        break;
                    }
                    default: {
                        logger.warning("unknown object type: " + nodeName);
                        break;
                    }
                }
            }
        }
    }

    /**
     * Read points of a polygon or polyline
     *
     * @param child the node containing the points
     * @return a list of points
     */
    private List<Vector2f> readPoints(XmlNode child) {
        List<Vector2f> points = new ArrayList<>();
        final String pointsAttribute = getAttributeValue(child, "points");
        StringTokenizer st = new StringTokenizer(pointsAttribute, ", ");
        while (st.hasMoreElements()) {
            Vector2f p = new Vector2f();
            p.x = Float.parseFloat(st.nextToken());
            p.y = Float.parseFloat(st.nextToken());

            points.add(p);
        }

        return points;
    }

    private TiledObjectText readTextObject(XmlNode node) {
        String fontFamily = getAttribute(node, "fontfamily", "sans-serif");
        int pixelSize = getAttribute(node, "pixelsize", 16);
        boolean wrap = getAttribute(node, "wrap", 0) == 1;
        String color = getAttributeValue(node, COLOR);
        boolean bold = getAttribute(node, "bold", 0) == 1;
        boolean italic = getAttribute(node, "italic", 0) == 1;
        boolean underline = getAttribute(node, "underline", 0) == 1;
        boolean strikeout = getAttribute(node, "strikeout", 0) == 1;
        boolean kerning = getAttribute(node, "kerning", 1) == 1;
        String horizontalAlignment = getAttribute(node, "halign", "left");// Left, Center, Right, Justify
        String verticalAlignment = getAttribute(node, "valign", "top");// Top, Center, Bottom
        String text = node.getTextContent();

        TiledObjectText objectText = new TiledObjectText(text);
        objectText.setFontFamily(fontFamily);
        objectText.setPixelSize(pixelSize);
        objectText.setWrap(wrap);
        if (color != null) {
            objectText.setColor(ColorUtil.toColorRGBA(color));
        }
        objectText.setBold(bold);
        objectText.setItalic(italic);
        objectText.setUnderline(underline);
        objectText.setStrikeout(strikeout);
        objectText.setKerning(kerning);
        objectText.setHorizontalAlignment(horizontalAlignment);
        objectText.setVerticalAlignment(verticalAlignment);

        return objectText;
    }
}
