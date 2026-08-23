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
 * 3. Neither the name of the copyright holder nor the names of its contributors
 *    may be used to endorse or promote products derived from this software without
 *    specific prior written permission.
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
 * Nostr Game Engine is a fork of the jMonkeyEngine, which is licensed under the
 * BSD 3-Clause License.
 */
package org.ngengine.world2d.tiled.renderer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.jme3.renderer.Caps;
import com.jme3.scene.Node;
import com.jme3.texture.Image;
import com.jme3.texture.Texture;
import com.jme3.texture.Texture2D;
import com.jme3.texture.image.ColorSpace;
import com.jme3.util.BufferUtils;
import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.EnumSet;
import org.junit.jupiter.api.Test;
import org.ngengine.world2d.tiled.core.TiledMap;
import org.ngengine.world2d.tiled.core.entity.TiledImageEntity;
import org.ngengine.world2d.tiled.core.tileset.Tile;
import org.ngengine.world2d.tiled.core.tileset.Tileset;

public class TilesetTexturePreparationTest {

    @Test
    public void atlasIsSplitIntoMipmappedLayersByDefault() {
        Tileset tileset = atlasTileset();
        MapRenderer renderer = renderer();

        InstancedTilesetSource source = renderer.getInstancedTilesetSource(tileset);

        assertTrue(renderer.isPreferTextureArraysForTilesets());
        assertTrue(source.arrayBased);
        assertEquals(0, source.getLayer(0));
        assertEquals(1, source.getLayer(1));
        assertEquals(4, source.imageWidth);
        assertEquals(4, source.imageHeight);
        assertEquals(2, source.textureArray.getImage().getData().size());
        assertEquals(64, source.textureArray.getImage().getData(0).get(0) & 0xff,
                "Tiled top-left atlas coordinates must be converted to raw bottom-up rows");
        assertEquals(0, source.textureArray.getImage().getData(1).get(0) & 0xff);
        assertEquals(Texture.MinFilter.Trilinear, source.textureArray.getMinFilter());
        assertEquals(Texture.MagFilter.Bilinear, source.textureArray.getMagFilter());
    }

    @Test
    public void disabledPreferenceKeepsAtlasOnBleedSafeLegacyFiltering() {
        Tileset tileset = atlasTileset();
        MapRenderer renderer = renderer();
        renderer.setPreferTextureArraysForTilesets(false);

        InstancedTilesetSource source = renderer.getInstancedTilesetSource(tileset);

        assertFalse(source.arrayBased);
        assertEquals(Texture.MinFilter.BilinearNoMipMaps, source.texture.getMinFilter());
        assertEquals(Texture.MagFilter.Bilinear, source.texture.getMagFilter());
    }

    @Test
    public void collectionWithDifferentLayoutsIsNormalizedOnce() {
        Tileset tileset = new Tileset(4, 4, 0, 0);
        tileset.addTile(collectionTile(2, 2, Image.Format.RGB8, 2 * 2 * 3));
        tileset.addTile(collectionTile(4, 4, Image.Format.RGBA8, 4 * 4 * 4));
        MapRenderer renderer = renderer();

        InstancedTilesetSource preferred = renderer.getPreferredTilesetSource(tileset);
        assertFalse(preferred.arrayBased,
                "preference alone must not normalize heterogeneous collection images");

        InstancedTilesetSource source = renderer.getInstancedTilesetSource(tileset);

        assertEquals(preferred, source);
        assertTrue(source.arrayBased);
        assertEquals(Image.Format.RGBA8, source.textureArray.getImage().getFormat());
        assertEquals(4, source.imageWidth);
        assertEquals(4, source.imageHeight);
        assertEquals(Texture.MinFilter.Trilinear, source.textureArray.getMinFilter());
    }

    @Test
    public void compatibleCompressedCollectionUsesOriginalBlocksAndMips() {
        Tileset tileset = new Tileset(4, 4, 0, 0);
        tileset.addTile(compressedCollectionTile((byte) 1));
        tileset.addTile(compressedCollectionTile((byte) 2));
        MapRenderer renderer = renderer();
        renderer.setRendererCapabilities(EnumSet.of(Caps.TextureArray, Caps.TextureCompressionS3TC));

        InstancedTilesetSource source = renderer.getInstancedTilesetSource(tileset);

        assertTrue(source.arrayBased);
        assertEquals(Image.Format.DXT1, source.textureArray.getImage().getFormat());
        assertEquals(2, source.textureArray.getImage().getData().size());
        assertEquals(Texture.MinFilter.Trilinear, source.textureArray.getMinFilter());
    }

    @Test
    public void unsupportedBackendLeavesCollectionAvailableForMultidraw() {
        Tileset tileset = new Tileset(4, 4, 0, 0);
        Tile tile = collectionTile(4, 4, Image.Format.RGBA8, 4 * 4 * 4);
        tileset.addTile(tile);
        MapRenderer renderer = renderer();
        renderer.setRendererCapabilities(Collections.<Caps>emptySet());

        InstancedTilesetSource source = renderer.getInstancedTilesetSource(tileset);

        assertFalse(source.arrayBased);
        assertNotNull(source.arrayFailureReason);
        assertEquals(Texture.MinFilter.Trilinear, tile.getImage().getTexture().getMinFilter());
    }

    private static MapRenderer renderer() {
        TiledMap map = new TiledMap(1, 1);
        map.setTileWidth(4);
        map.setTileHeight(4);
        return new OrthogonalRenderer(map, 4, new Node("test"));
    }

    private static Tileset atlasTileset() {
        Tileset tileset = new Tileset(4, 4, 0, 0);
        ByteBuffer data = BufferUtils.createByteBuffer(4 * 8 * 4);
        for (int i = 0; i < data.capacity(); i++) {
            data.put((byte) i);
        }
        data.flip();
        Texture2D texture = new Texture2D(new Image(
                Image.Format.RGBA8, 4, 8, data, ColorSpace.sRGB));
        TiledImageEntity image = new TiledImageEntity("atlas", null, "png", 4, 8);
        image.setTexture(texture);
        tileset.setImage(image);
        tileset.addTile(new Tile(0, 0, 4, 4));
        tileset.addTile(new Tile(0, 4, 4, 4));
        return tileset;
    }

    private static Tile collectionTile(int width, int height, Image.Format format, int bytes) {
        ByteBuffer data = BufferUtils.createByteBuffer(bytes);
        while (data.hasRemaining()) {
            data.put((byte) 0x7f);
        }
        data.flip();
        TiledImageEntity tiledImage = new TiledImageEntity(
                "collection", null, "generated", width, height);
        tiledImage.setTexture(new Texture2D(new Image(format, width, height, data, ColorSpace.sRGB)));
        Tile tile = new Tile(0, 0, width, height);
        tile.setImage(tiledImage);
        return tile;
    }

    private static Tile compressedCollectionTile(byte value) {
        int[] mipSizes = { 8, 8, 8 };
        ByteBuffer data = BufferUtils.createByteBuffer(24);
        while (data.hasRemaining()) {
            data.put(value);
        }
        data.flip();
        Image image = new Image(Image.Format.DXT1, 4, 4, data, mipSizes, ColorSpace.Linear);
        TiledImageEntity tiledImage = new TiledImageEntity(
                "compressed", null, "dds", 4, 4);
        tiledImage.setTexture(new Texture2D(image));
        Tile tile = new Tile(0, 0, 4, 4);
        tile.setImage(tiledImage);
        return tile;
    }
}
