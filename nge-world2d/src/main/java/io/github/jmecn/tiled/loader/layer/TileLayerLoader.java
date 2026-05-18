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

import com.jme3.asset.AssetKey;
import com.jme3.asset.AssetManager;
import io.github.jmecn.tiled.core.*;
import io.github.jmecn.tiled.core.entity.TiledTileEntity;
import io.github.jmecn.tiled.core.tileset.Tile;
import io.github.jmecn.tiled.enums.DataCompression;
import io.github.jmecn.tiled.enums.DataEncoding;
import io.github.jmecn.tiled.loader.LayerLoader;
import io.github.jmecn.tiled.xml.XmlNode;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Base64;
import java.util.Map;
import java.util.logging.Logger;
import java.util.zip.GZIPInputStream;
import java.util.zip.InflaterInputStream;

import static io.github.jmecn.tiled.TiledConst.*;
import static io.github.jmecn.tiled.loader.Utils.getAttribute;
import static io.github.jmecn.tiled.loader.Utils.getAttributeValue;

/**
 * The image loader.
 *
 * @author yanmaoyuan
 */
public class TileLayerLoader extends LayerLoader {

    private static final Logger logger = Logger.getLogger(TileLayerLoader.class.getName());
    private final TiledMap map;

    public TileLayerLoader(AssetManager assetManager, AssetKey<?> key, TiledMap map) {
        super(assetManager, key);
        this.map = map;
    }

    /**
     * Loads a map layer from a layer node.
     *
     * @param node the node representing the "layer" element
     * @return the loaded map layer
     * @throws IOException if an I/O error occurs
     */
    @Override
    public TiledTileLayer load(XmlNode node) throws IOException {
        final int layerWidth = getAttribute(node, WIDTH, map.getWidth());
        final int layerHeight = getAttribute(node, HEIGHT, map.getHeight());

        TiledTileLayer layer = new TiledTileLayer(layerWidth, layerHeight);

        readLayerBase(node, layer);

        XmlNode child = node.getFirstChild();
        while (child != null) {
            String nodeName = child.getNodeName();
            if (DATA.equals(nodeName)) {
                readData(layer, child);
            }  
            child = child.getNextSibling();
        }

        child = node.getFirstChild();
        while (child != null) {
            String nodeName = child.getNodeName();
            if ("tileproperties".equals(nodeName)) {
                readTileProperties(layer, child);
            }
            child = child.getNextSibling();
        }

        return layer;
    }

    private void readData(TiledTileLayer layer, XmlNode node) throws IOException {
        String enc = getAttributeValue(node, "encoding");
        String comp = getAttributeValue(node, "compression");

        DataEncoding encoding = DataEncoding.NONE;
        if (enc != null) {
            encoding = DataEncoding.fromValue(enc);
            if (encoding == null) {
                logger.warning("Unsupported encoding:" + enc + ", layer:" + layer.getName());
                throw new IllegalArgumentException("Unsupported encoding:" + enc);
            }
        }

        DataCompression compression = DataCompression.NONE;
        if (comp != null) {
            compression = DataCompression.fromValue(comp);
            if (compression == null) {
                logger.warning("Unsupported compression: "+ comp+" , layer: " + layer.getName());
                throw new IllegalArgumentException("Unsupported compression:" + comp);
            }
        }

        if (map.isInfinite()) {
            // read chunks
            for (XmlNode child : node.getChildNodes()) {
                String nodeName = child.getNodeName();
                if ("chunk".equals(nodeName)) {
                    TiledTileChunk chunk = readChunk(layer, child, encoding, compression);
                    layer.addChunk(chunk);
                    mergeChunk(layer, chunk);// FIXME experimental
                }
            }
        } else {
            switch (encoding) {
                case BASE64:
                    decodeBase64Data(layer, node, compression);
                    break;
                case CSV:
                    decodeCsvData(layer, node);
                    break;
                default:
                    decodeTileData(layer, node);
                    break;
            }
        }
    }

    private void mergeChunk(TiledTileLayer layer, TiledTileChunk chunk) {
        int x = chunk.getX();
        int y = chunk.getY();
        double width = chunk.getWidth();
        double height = chunk.getHeight();

        // set chunk to layer
        for (int cy = 0; cy < height; cy++) {
            for (int cx = 0; cx < width; cx++) {
                Tile tile = chunk.getTileAt(cx, cy).getTile();
                layer.placeTileAt(x + cx, y + cy, tile);
            }
        }
    }
    /**
     * Get the InputStream for the data element.
     *
     * @param len the length of the data
     * @param compression the compression method
     * @param decode the decoded data
     * @return the InputStream
     * @throws IOException if an I/O error occurs
     */
    private InputStream getInputStream(int len, DataCompression compression, byte[] decode) throws IOException {
        InputStream is;
        switch (compression) {
            case GZIP: {
                is = new GZIPInputStream(new ByteArrayInputStream(decode), len);
                break;
            }
            case ZLIB: {
                is = new InflaterInputStream(new ByteArrayInputStream(decode));
                break;
            }
            case ZSTANDARD: {
                String zstdClassName = System.getProperty("tmx-loader.zstd.class", "com.github.luben.zstd.ZstdInputStream");
                Class<?> zstdClass;
                try {
                    zstdClass = Class.forName(zstdClassName);
                    is = (InputStream) zstdClass.getConstructor(InputStream.class).newInstance(new ByteArrayInputStream(decode));
                } catch (Exception e) {
                    throw new IOException("Unable to use Zstandard compression, please add the com.github.luben.zstd.ZstdInputStream dependency or specify another ZstdInputStream implementation with the system property 'tmx-loader.zstd.class'.", e);
                }
                break;
            }
            default: {
                is = new ByteArrayInputStream(decode);
                break;
            }
        }
        return is;
    }

    private void decodeBase64Data(TiledTileContainer tileContainer, XmlNode node, DataCompression compression) throws IOException {
        // With XmlNode, text is stored on the node itself (no separate text child)
        String text = node.getTextContent();
        if ((text == null || text.trim().isEmpty()) && node.getFirstChild() != null) {
            // Fallback for any unexpected structure
            text = node.getFirstChild().getTextContent();
        }
        if (text == null || text.trim().isEmpty()) {
            return;
        }

        byte[] decodeData = Base64.getDecoder().decode(text.trim());
        int width = (int)tileContainer.getWidth();
        int height = (int)tileContainer.getHeight();
        int len = width * height * 4;
        InputStream is = getInputStream(len, compression, decodeData);

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int tileId = 0;
                tileId |= is.read();
                tileId |= is.read() << 8;
                tileId |= is.read() << 16;
                tileId |= is.read() << 24;

                map.setTileAtFromTileId(tileContainer, x, y, tileId);
            }
        }
    }

    private void decodeCsvData(TiledTileContainer tileContainer, XmlNode node) throws IOException {
        String csvText = node.getTextContent();

        /*
         * trim 'space', 'tab', 'newline'. pay attention to
         * additional unicode chars like \u2028, \u2029, \u0085 if
         * necessary
         */
        String[] csvTileIds = csvText.trim().split("[\\s]*,[\\s]*");

        int width = (int)tileContainer.getWidth();
        int height = (int)tileContainer.getHeight();
        int len = width * height;

        if (csvTileIds.length != len) {
            throw new IOException("Number of tiles does not match the layer's width and height");
        }

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                String sTileId = csvTileIds[x + y * width];
                int tileId = (int) Long.parseLong(sTileId);
                map.setTileAtFromTileId(tileContainer, x, y, tileId);
            }
        }
    }

    private void decodeTileData(TiledTileContainer tileContainer, XmlNode node) {
        int x = 0;
        int y = 0;
        int width = (int)tileContainer.getWidth();
        int height = (int)tileContainer.getHeight();
        XmlNode child = node.getFirstChild();
        while (child != null) {
            if (TILE.equals(child.getNodeName())) {
                int tileId = getAttribute(child, GID, -1);
                map.setTileAtFromTileId(tileContainer, x, y, tileId);

                x++;
                if (x == width) {
                    x = 0;
                    y++;
                }
                if (y == height) {
                    break;
                }
            }
            child = child.getNextSibling();
        }
    }

    /**
     * <p>This is currently added only for infinite maps. The contents of a chunk element is
     * same as that of the data element, except it stores the data of the area specified
     * in the attributes.</p>
     *
     * <p>Can contain any number: &lt;tile&gt;</p>
     *
     * @param layer the layer
     * @param node the chunk node
     * @param encoding the encoding
     * @param compression the compression
     * @return the chunk
     */
    private TiledTileChunk readChunk(TiledTileLayer layer, XmlNode node, DataEncoding encoding, DataCompression compression) throws IOException {
        int x = getAttribute(node, X, 0);
        int y = getAttribute(node, Y, 0);
        int width = getAttribute(node, WIDTH, 0);
        int height = getAttribute(node, HEIGHT, 0);

        TiledTileChunk chunk = new TiledTileChunk(x, y, width, height);

        if (node.hasChildNodes()) {
            switch (encoding) {
                case BASE64:
                    decodeBase64Data(chunk, node, compression);
                    break;
                case CSV:
                    decodeCsvData(chunk, node);
                    break;
                default:
                    decodeTileData(chunk, node);
                    break;
            }
        } else {
            logger.warning("Chunk has no child nodes, layer: " + layer.getName());
            throw new IllegalArgumentException("Chunk has no child nodes");
        }

        return chunk;
    }

    private void readTileProperties(TiledTileLayer layer, XmlNode node) {
        XmlNode child = node.getFirstChild();
        while (child != null) {
            if (TILE.equalsIgnoreCase(child.getNodeName())) {
                int x = getAttribute(child, X, -1);
                int y = getAttribute(child, Y, -1);

                Map<String, Object> tip = propertiesLoader.readProperties(child);
                TiledTileEntity e = layer.getTileAt(x, y);
                e.setProperties(tip);
            }
            child = child.getNextSibling();
        }
    }
}
