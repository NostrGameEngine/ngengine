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

package org.ngengine.world2d.tiled.renderer;

import com.jme3.bounding.BoundingBox;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import com.jme3.scene.Node;
import com.jme3.scene.VertexBuffer;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import org.ngengine.platform.NGEUtils;
import org.ngengine.world2d.tiled.core.TiledBase;
import org.ngengine.world2d.tiled.core.TiledLayer;
import org.ngengine.world2d.tiled.core.entity.TiledObjectEntity;
import org.ngengine.world2d.tiled.core.tileset.Tile;
import org.ngengine.world2d.tiled.core.tileset.Tileset;
import org.ngengine.world2d.tiled.enums.FillMode;
import org.ngengine.world2d.tiled.enums.Orientation;
import org.ngengine.world2d.tiled.renderer.queue.YAxisComparator;

/**
 * One instanced geometry plus its CPU-side record list for a single draw group.
 */
final class InstancedTileBatch {
    private static final int INSTANCE_COMPONENTS = 16;
    private static final int TILE_COMPONENTS = 4;
    private static final int SIZE_COMPONENTS = 4;
    private static final int ORIGIN_COMPONENTS = 4;
    private static final int UV_SIZE_COMPONENTS = 2;
    private static final int DECAL_COMPONENTS = 4;
    private static final String DECAL_TILE_PROPERTY = "decal.tile";
    private static final String DECAL_TILESET_PROPERTY = "decal.tileset";
    private static final String DECAL_SCALE_PROPERTY = "decal.scale";
    private static final String DECAL_SIZE_PROPERTY = "decal.size";
    private static final String DECAL_OFFSET_X_PROPERTY = "decal.offsetX";
    private static final String DECAL_OFFSET_Y_PROPERTY = "decal.offsetY";
    private static final String DECAL_HIDDEN_PROPERTY = "decal.hidden";
    private static final String SOCKET_HIDE_DECAL_PROPERTY = "socket.hideDecal";
    private static final String DEFAULT_DECAL_TILESET = "tilesetDECALS";
    private static final int MIN_CAPACITY = 16;
    private static final int SHRINK_AFTER_UPDATES = 300;

    private final MapRenderer renderer;
    final String layerName;
    final ArrayList<InstancedTilesetSource> sources = new ArrayList<>();
    final ArrayList<InstancedTileRecord> records = new ArrayList<>();
    private final IdentityHashMap<TiledBase, Integer> slots = new IdentityHashMap<>();
    private final DynamicFloatVertexBuffer instanceData = new DynamicFloatVertexBuffer(
            VertexBuffer.Type.InstanceData, INSTANCE_COMPONENTS, MIN_CAPACITY, SHRINK_AFTER_UPDATES);
    private final DynamicFloatVertexBuffer tileData = new DynamicFloatVertexBuffer(
            VertexBuffer.Type.TexCoord2, TILE_COMPONENTS, MIN_CAPACITY, SHRINK_AFTER_UPDATES);
    private final DynamicFloatVertexBuffer sizeData = new DynamicFloatVertexBuffer(
            VertexBuffer.Type.TexCoord3, SIZE_COMPONENTS, MIN_CAPACITY, SHRINK_AFTER_UPDATES);
    private final DynamicFloatVertexBuffer originData = new DynamicFloatVertexBuffer(
            VertexBuffer.Type.TexCoord4, ORIGIN_COMPONENTS, MIN_CAPACITY, SHRINK_AFTER_UPDATES);
    private final DynamicFloatVertexBuffer uvSizeData = new DynamicFloatVertexBuffer(
            VertexBuffer.Type.TexCoord5, UV_SIZE_COMPONENTS, MIN_CAPACITY, SHRINK_AFTER_UPDATES);
    private final DynamicFloatVertexBuffer decal0Data = new DynamicFloatVertexBuffer(
            VertexBuffer.Type.TexCoord6, DECAL_COMPONENTS, MIN_CAPACITY, SHRINK_AFTER_UPDATES);
    private final DynamicFloatVertexBuffer decal1Data = new DynamicFloatVertexBuffer(
            VertexBuffer.Type.TexCoord7, DECAL_COMPONENTS, MIN_CAPACITY, SHRINK_AFTER_UPDATES);
    private final DynamicFloatVertexBuffer decal2Data = new DynamicFloatVertexBuffer(
            VertexBuffer.Type.TexCoord8, DECAL_COMPONENTS, MIN_CAPACITY, SHRINK_AFTER_UPDATES);
    private final DynamicFloatVertexBuffer decal3Data = new DynamicFloatVertexBuffer(
            VertexBuffer.Type.Color, DECAL_COMPONENTS, MIN_CAPACITY, SHRINK_AFTER_UPDATES);
    private final boolean delayedCompaction;
    private final float[] tmpDecalTile = new float[InstancedTileRecord.DECAL_LAYERS];
    private final float[] tmpDecalX = new float[InstancedTileRecord.DECAL_LAYERS];
    private final float[] tmpDecalY = new float[InstancedTileRecord.DECAL_LAYERS];
    private final float[] tmpDecalScale = new float[InstancedTileRecord.DECAL_LAYERS];
    private InstancedTilesetSource decalSource;
    Geometry geometry;
    private boolean materialDirty;
    float minX = Float.POSITIVE_INFINITY;
    float maxX = Float.NEGATIVE_INFINITY;
    float minY = Float.POSITIVE_INFINITY;
    float maxY = Float.NEGATIVE_INFINITY;
    float minZ = Float.POSITIVE_INFINITY;
    float maxZ = Float.NEGATIVE_INFINITY;
    final int drawGroup;
    int cooldownFrames;
    private int rebatchCooldownFrames;
    int pendingInsertCount;
    int pendingRemoveCount;
    int tombstoneCount;
    float fragmentation;
    int lastDirtyFrame = -1;
    String lastDirtyReason;

    InstancedTileBatch(MapRenderer renderer, String layerName, int drawGroup, boolean delayedCompaction) {
        this.renderer = renderer;
        this.layerName = layerName;
        this.drawGroup = drawGroup;
        this.delayedCompaction = delayedCompaction;
    }

    boolean contains(InstancedTilesetSource source) {
        return sources.contains(source);
    }

    boolean canAdd(InstancedTilesetSource source) {
        return sources.contains(source) || sources.size() < renderer.maxInstancedTilesetSlots;
    }

    void beginUpdate() {
        for (InstancedTileRecord record : records) {
            record.seen = false;
        }
        minX = Float.POSITIVE_INFINITY;
        maxX = Float.NEGATIVE_INFINITY;
        minY = Float.POSITIVE_INFINITY;
        maxY = Float.NEGATIVE_INFINITY;
        minZ = Float.POSITIVE_INFINITY;
        maxZ = Float.NEGATIVE_INFINITY;
        instanceData.beginUpdate();
        tileData.beginUpdate();
        sizeData.beginUpdate();
        originData.beginUpdate();
        uvSizeData.beginUpdate();
        decal0Data.beginUpdate();
        decal1Data.beginUpdate();
        decal2Data.beginUpdate();
        decal3Data.beginUpdate();
    }

    void tickFrame() {
        if (cooldownFrames > 0) {
            cooldownFrames--;
        }
        if (rebatchCooldownFrames > 0) {
            rebatchCooldownFrames--;
        }
    }

    void putTile(TiledBase entry, Tile tile, InstancedTilesetSource source, float x, float y, float z) {
        InstancedTileRecord record = getRecord(entry);
        int textureSlot = getTextureSlot(source);
        float tileDataX;
        float tileDataY;
        if (source.imageBased) {
            tileDataX = (float) tile.getX();
            tileDataY = (float) tile.getY();
        } else {
            tileDataX = (float) source.collectionLayerByTileId.get(tile.getId());
            tileDataY = 0f;
        }
        int flipFlags = getInstancedFlipFlags(tile);
        float imageWidth = (float) source.imageWidth;
        float imageHeight = (float) source.imageHeight;
        float uvWidth = (float) tile.getWidth();
        float uvHeight = (float) tile.getHeight();
        Vector2f offset = tile.getTileset().getTileOffset();
        float tileWidth = (float) tile.getWidth();
        float tileHeight = (float) tile.getHeight();
        float offsetX = offset.x;
        float offsetY = offset.y;
        float originX;
        float originY;
        if (renderer.tiledMap.getOrientation() == Orientation.ISOMETRIC) {
            originX = -tileWidth * 0.5f;
            originY = renderer.tileHeight;
        } else {
            originX = 0f;
            originY = renderer.tileHeight;
        }
        boolean changed = record.update(tile, source, textureSlot, tileDataX, tileDataY, flipFlags,
                x, y, z, 0f, tileWidth, tileHeight, offsetX, offsetY,
                originX, originY, imageWidth, imageHeight, uvWidth, uvHeight);
        boolean decalChanged = record.updateDecals(null, null, null, null);

        int index = slots.get(record.entry);
        if (changed || decalChanged || record.writeNeeded) {
            writeRecord(index);
            record.writeNeeded = false;
        } else {
            includeRecordBounds(record);
        }
    }

    void putObject(TiledObjectEntity entry, Tile tile, InstancedTilesetSource source, float x, float y, float z) {
        InstancedTileRecord record = getRecord(entry);
        int textureSlot = getTextureSlot(source);
        float tileDataX;
        float tileDataY;
        if (source.imageBased) {
            tileDataX = (float) tile.getX();
            tileDataY = (float) tile.getY();
        } else {
            tileDataX = (float) source.collectionLayerByTileId.get(tile.getId());
            tileDataY = 0f;
        }
        int flipFlags = getInstancedFlipFlags(tile);
        float imageWidth = (float) source.imageWidth;
        float imageHeight = (float) source.imageHeight;
        float uvWidth = (float) tile.getWidth();
        float uvHeight = (float) tile.getHeight();
        float rotation = (float) (-entry.getRotation() * FastMath.DEG_TO_RAD);
        Vector2f offset = tile.getTileset().getTileOffset();
        float tileWidth = (float) tile.getWidth();
        float tileHeight = (float) tile.getHeight();
        float offsetX = offset.x;
        float offsetY = offset.y;
        if (tile.getTileset().getFillMode() == FillMode.STRETCH) {
            float sx = (float) entry.getWidth() / tileWidth;
            float sy = (float) entry.getHeight() / tileHeight;
            tileWidth = (float) entry.getWidth();
            tileHeight = (float) entry.getHeight();
            offsetX *= sx;
            offsetY *= sy;
        }
        Vector2f origin = TileObjectAlignment.origin(renderer.tiledMap.getOrientation(), tile, tileWidth, tileHeight);
        float originX = origin.x;
        float originY = origin.y;
        boolean changed = record.update(tile, source, textureSlot, tileDataX, tileDataY, flipFlags,
                x, y, z, rotation, tileWidth, tileHeight, offsetX, offsetY,
                originX, originY, imageWidth, imageHeight, uvWidth, uvHeight);
        fillObjectDecals(entry, tile);
        boolean decalChanged = record.updateDecals(tmpDecalTile, tmpDecalX, tmpDecalY, tmpDecalScale);

        int index = slots.get(record.entry);
        if (changed || decalChanged || record.writeNeeded) {
            writeRecord(index);
            record.writeNeeded = false;
        } else {
            includeRecordBounds(record);
        }
    }

    private void fillObjectDecals(TiledObjectEntity entry, Tile tile) {
        for (int i = 0; i < InstancedTileRecord.DECAL_LAYERS; i++) {
            tmpDecalTile[i] = -1f;
            tmpDecalX[i] = 0f;
            tmpDecalY[i] = 0f;
            tmpDecalScale[i] = 0f;
        }
        if (entry == null || tile == null || tile.getCollisions() == null) {
            return;
        }

        int layer = 0;
        for (TiledObjectEntity decalObject : tile.getCollisions().getObjects()) {
            if (layer >= InstancedTileRecord.DECAL_LAYERS) {
                return;
            }
            if (isDecalHidden(decalObject)) {
                continue;
            }
            Object decalTileValue = decalObject.getProperty(DECAL_TILE_PROPERTY);
            if (decalTileValue == null) {
                continue;
            }

            String tilesetName = String.valueOf(decalObject.getPropertyOrDefault(
                    DECAL_TILESET_PROPERTY, DEFAULT_DECAL_TILESET)).trim();
            Tileset tileset = renderer.tiledMap.getTileset(tilesetName);
            if (tileset == null || !tileset.isImageBased()) {
                continue;
            }
            int decalTileId = NGEUtils.safeInt(decalTileValue);
            Tile decalTile = tileset.getTile(decalTileId);
            if (decalTile == null) {
                continue;
            }
            InstancedTilesetSource source = renderer.getInstancedTilesetSource(tileset);
            if (decalSource == null) {
                decalSource = source;
                materialDirty = true;
            } else if (decalSource != source) {
                continue;
            }

            float tileWidth = Math.max((float) tile.getWidth(), 1f);
            float tileHeight = Math.max((float) tile.getHeight(), 1f);
            tmpDecalTile[layer] = decalTile.getId();
            float centerX = (float) ((decalObject.getX() + decalObject.getWidth() * 0.5) / tileWidth)
                    + NGEUtils.safeFloat(decalObject.getProperty(DECAL_OFFSET_X_PROPERTY));
            float centerY = (float) ((decalObject.getY() + decalObject.getHeight() * 0.5) / tileHeight)
                    + NGEUtils.safeFloat(decalObject.getProperty(DECAL_OFFSET_Y_PROPERTY));
            Vector2f decalCenter = ObjectDecalPlacement.transformCenterForTileFlip(centerX, centerY, tile);
            tmpDecalX[layer] = decalCenter.x;
            tmpDecalY[layer] = decalCenter.y;
            float defaultScale = (float) (Math.max(decalObject.getWidth(), decalObject.getHeight()) / tileWidth);
            Object decalSizeValue = decalObject.getProperty(DECAL_SIZE_PROPERTY);
            float decalSize = decalSizeValue != null ? NGEUtils.safeFloat(decalSizeValue) : defaultScale * tileWidth;
            Object decalScaleValue = decalObject.getProperty(DECAL_SCALE_PROPERTY);
            tmpDecalScale[layer] = decalScaleValue != null ? NGEUtils.safeFloat(decalScaleValue) : decalSize / tileWidth;
            layer++;
        }
    }

    private boolean isDecalHidden(TiledObjectEntity decalObject) {
        return isTruthy(decalObject.getProperty(DECAL_HIDDEN_PROPERTY))
                || isTruthy(decalObject.getProperty(SOCKET_HIDE_DECAL_PROPERTY));
    }

    private boolean isTruthy(Object value) {
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        return value != null && Boolean.parseBoolean(String.valueOf(value));
    }

    private InstancedTileRecord getRecord(TiledBase entry) {
        Integer oldSlot = slots.get(entry);
        if (oldSlot != null) {
            InstancedTileRecord record = records.get(oldSlot);
            if (record.tombstone) {
                record.tombstone = false;
                record.writeNeeded = true;
                tombstoneCount--;
                if (pendingRemoveCount > 0) {
                    pendingRemoveCount--;
                }
                pendingInsertCount++;
                updateFragmentation();
                markBatchChanged("revive");
            }
            return record;
        }

        ensureCapacity(records.size() + 1);
        InstancedTileRecord record = new InstancedTileRecord();
        record.entry = entry;
        record.writeNeeded = true;
        slots.put(entry, records.size());
        records.add(record);
        pendingInsertCount++;
        updateFragmentation();
        markBatchChanged("pendingInsert");
        return record;
    }

    private int getTextureSlot(InstancedTilesetSource source) {
        int slot = sources.indexOf(source);
        if (slot >= 0) {
            return slot;
        }
        slot = sources.size();
        sources.add(source);
        materialDirty = true;
        return slot;
    }

    boolean remove(TiledBase entry) {
        Integer slot = slots.get(entry);
        if (slot == null) {
            return false;
        }
        removeSlot(slot);
        return true;
    }

    void endUpdate(Node layerNode, TiledLayer layer) {
        for (int i = records.size() - 1; i >= 0; i--) {
            InstancedTileRecord record = records.get(i);
            if (!record.seen && !record.tombstone) {
                removeSlot(i);
            }
        }

        if (delayedCompaction && shouldRebatch() && renderer.tryConsumeInstancedRebatch()) {
            compactTombstones();
        }

        if (records.isEmpty() || tombstoneCount == records.size()) {
            if (geometry != null) {
                geometry.removeFromParent();
            }
            return;
        }

        maybeShrink();
        if (geometry == null) {
            createGeometry();
            Material material = renderer.spriteFactory.getMaterialFactory().newMaterial();
            geometry.setMaterial(material);
            materialDirty = true;
            renderer.entryMap.put(geometry, layer);
        }
        if (geometry.getParent() != layerNode) {
            layerNode.attachChild(geometry);
        }
        if (materialDirty) {
            applyMaterial(geometry.getMaterial());
            materialDirty = false;
        }
        renderer.spriteFactory.getMaterialFactory().setTintColor(geometry.getMaterial(), layer.getTintColor());
        renderer.spriteFactory.getMaterialFactory().setLayerOpacity(geometry.getMaterial(), (float) layer.getOpacity());
        renderer.spriteFactory.getMaterialFactory().setBlendMode(geometry.getMaterial(), layer.getBlendMode());
        geometry.setUserData(YAxisComparator.SORT_ORDER_USER_DATA,
                renderer.getWorldSortOrder(layer, drawGroup));
        geometry.setNumInstances(records.size());
        updateBound();
    }

    private void removeSlot(int slot) {
        if (delayedCompaction) {
            tombstoneSlot(slot);
        } else {
            swapRemoveSlot(slot);
        }
    }

    private void swapRemoveSlot(int slot) {
        int last = records.size() - 1;
        InstancedTileRecord removed = records.get(slot);
        slots.remove(removed.entry);
        markBatchChanged("remove");
        if (slot != last) {
            InstancedTileRecord moved = records.get(last);
            records.set(slot, moved);
            slots.put(moved.entry, slot);
            writeRecord(slot);
        }
        records.remove(last);
        updateFragmentation();
    }

    private void tombstoneSlot(int slot) {
        InstancedTileRecord record = records.get(slot);
        if (record.tombstone) {
            return;
        }
        record.tombstone = true;
        record.seen = false;
        record.writeNeeded = false;
        tombstoneCount++;
        pendingRemoveCount++;
        updateFragmentation();
        markBatchChanged("tombstone");
        writeRecord(slot);
    }

    boolean hasPendingWork() {
        return shouldRebatch();
    }

    private boolean shouldRebatch() {
        if (!delayedCompaction || tombstoneCount == 0 || rebatchCooldownFrames > 0) {
            return false;
        }
        return tombstoneCount >= 16 || tombstoneCount * 4 >= records.size();
    }

    private void compactTombstones() {
        int slot = 0;
        int tail = records.size() - 1;
        while (slot <= tail) {
            InstancedTileRecord record = records.get(slot);
            if (!record.tombstone) {
                slot++;
                continue;
            }

            slots.remove(record.entry);
            while (tail > slot && records.get(tail).tombstone) {
                slots.remove(records.get(tail).entry);
                records.remove(tail);
                tombstoneCount--;
                tail--;
            }

            if (tail <= slot) {
                records.remove(slot);
                tombstoneCount--;
                tail--;
                continue;
            }

            InstancedTileRecord moved = records.get(tail);
            records.set(slot, moved);
            slots.put(moved.entry, slot);
            records.remove(tail);
            tombstoneCount--;
            tail--;
            writeRecord(slot);
            slot++;
        }
        tombstoneCount = 0;
        pendingInsertCount = 0;
        pendingRemoveCount = 0;
        updateFragmentation();
        markBatchChanged("rebatch");
    }

    private void markBatchChanged(String reason) {
        cooldownFrames = MapRenderer.BATCH_COOLDOWN_FRAMES;
        if (delayedCompaction) {
            rebatchCooldownFrames = MapRenderer.BATCH_COOLDOWN_FRAMES;
        }
        lastDirtyFrame = renderer.renderFrame;
        lastDirtyReason = reason;
        renderer.logInstancedBatchChange(this, reason);
    }

    private void updateFragmentation() {
        fragmentation = records.isEmpty() ? 0f : tombstoneCount / (float) records.size();
    }

    private void writeRecord(int index) {
        InstancedTileRecord record = records.get(index);
        float cos = FastMath.cos(record.rotation);
        float sin = FastMath.sin(record.rotation);

        instanceData.put(index, 0, cos);
        instanceData.put(index, 1, 0f);
        instanceData.put(index, 2, -sin);
        instanceData.put(index, 3, 0f);
        instanceData.put(index, 4, 0f);
        instanceData.put(index, 5, 1f);
        instanceData.put(index, 6, 0f);
        instanceData.put(index, 7, 0f);
        instanceData.put(index, 8, sin);
        instanceData.put(index, 9, 0f);
        instanceData.put(index, 10, cos);
        instanceData.put(index, 11, 0f);
        instanceData.put(index, 12, record.x);
        instanceData.put(index, 13, record.y);
        instanceData.put(index, 14, record.z);
        instanceData.put(index, 15, 1f);

        tileData.put(index, 0, record.tileDataX);
        tileData.put(index, 1, record.tileDataY);
        tileData.put(index, 2, record.tombstone ? -1f : (float) record.textureSlot);
        tileData.put(index, 3, (float) record.flipFlags);

        sizeData.put(index, 0, record.tileWidth);
        sizeData.put(index, 1, record.tileHeight);
        sizeData.put(index, 2, record.offsetX);
        sizeData.put(index, 3, record.offsetY);

        originData.put(index, 0, record.originX);
        originData.put(index, 1, record.originY);
        originData.put(index, 2, record.imageWidth);
        originData.put(index, 3, record.imageHeight);

        uvSizeData.put(index, 0, record.uvWidth);
        uvSizeData.put(index, 1, record.uvHeight);
        writeDecalLayer(decal0Data, index, record, 0);
        writeDecalLayer(decal1Data, index, record, 1);
        writeDecalLayer(decal2Data, index, record, 2);
        writeDecalLayer(decal3Data, index, record, 3);

        if (!record.tombstone) {
            includeRecordBounds(record);
        }
        record.writeNeeded = false;

        if (geometry != null) {
            instanceData.markElementDirty(index);
            tileData.markElementDirty(index);
            sizeData.markElementDirty(index);
            originData.markElementDirty(index);
            uvSizeData.markElementDirty(index);
            decal0Data.markElementDirty(index);
            decal1Data.markElementDirty(index);
            decal2Data.markElementDirty(index);
            decal3Data.markElementDirty(index);
        }
    }

    private void writeDecalLayer(DynamicFloatVertexBuffer buffer, int index, InstancedTileRecord record, int layer) {
        boolean hidden = record.tombstone || record.decalTile[layer] < -0.5f;
        buffer.put(index, 0, hidden ? -1f : record.decalTile[layer]);
        buffer.put(index, 1, hidden ? 0f : record.decalX[layer]);
        buffer.put(index, 2, hidden ? 0f : record.decalY[layer]);
        buffer.put(index, 3, hidden ? 0f : record.decalScale[layer]);
    }

    private void includeRecordBounds(InstancedTileRecord record) {
        if (record.tombstone) {
            return;
        }
        minX = Math.min(minX, record.x + record.originX + record.offsetX);
        maxX = Math.max(maxX, record.x + record.originX + record.offsetX + record.tileWidth);
        minY = Math.min(minY, record.y);
        maxY = Math.max(maxY, record.y);
        minZ = Math.min(minZ, record.z + record.originY + record.offsetY - record.tileHeight);
        maxZ = Math.max(maxZ, record.z + record.originY + record.offsetY);
    }

    void addTombstoneDebugRects() {
        if (!renderer.batchDebugEnabled || tombstoneCount == 0) {
            return;
        }
        for (InstancedTileRecord record : records) {
            if (!record.tombstone) {
                continue;
            }
            renderer.addDebugRect("tombstone#" + drawGroup,
                    record.x + record.originX + record.offsetX,
                    record.z + record.originY + record.offsetY - record.tileHeight,
                    Math.max(record.tileWidth, 1f),
                    Math.max(record.tileHeight, 1f),
                    record.y + 0.35f,
                    new ColorRGBA(1f, 0.05f, 0.05f, 0.45f));
        }
    }

    private void ensureCapacity(int needed) {
        instanceData.ensureCapacity(needed);
        tileData.ensureCapacity(needed);
        sizeData.ensureCapacity(needed);
        originData.ensureCapacity(needed);
        uvSizeData.ensureCapacity(needed);
        decal0Data.ensureCapacity(needed);
        decal1Data.ensureCapacity(needed);
        decal2Data.ensureCapacity(needed);
        decal3Data.ensureCapacity(needed);
    }

    private void maybeShrink() {
        instanceData.maybeShrink(records.size());
        tileData.maybeShrink(records.size());
        sizeData.maybeShrink(records.size());
        originData.maybeShrink(records.size());
        uvSizeData.maybeShrink(records.size());
        decal0Data.maybeShrink(records.size());
        decal1Data.maybeShrink(records.size());
        decal2Data.maybeShrink(records.size());
        decal3Data.maybeShrink(records.size());
    }

    private int getInstancedFlipFlags(Tile tile) {
        int flags = 0;
        if (tile.isFlippedHorizontally()) {
            flags |= 1;
        }
        if (tile.isFlippedVertically()) {
            flags |= 2;
        }
        if (tile.isFlippedAntiDiagonally()) {
            flags |= 4;
        }
        return flags;
    }

    void applyMaterial(Material material) {
        material.clearParam(MaterialConst.COLOR_MAP_0);
        material.clearParam(MaterialConst.COLOR_MAP_1);
        material.clearParam(MaterialConst.COLOR_MAP_2);
        material.clearParam(MaterialConst.COLOR_MAP_3);
        material.clearParam(MaterialConst.DECAL_MAP);
        material.clearParam(MaterialConst.COLOR_ARRAY_0);
        material.clearParam(MaterialConst.COLOR_ARRAY_1);
        material.clearParam(MaterialConst.COLOR_ARRAY_2);
        material.clearParam(MaterialConst.COLOR_ARRAY_3);
        for (int i = 0; i < sources.size(); i++) {
            InstancedTilesetSource source = sources.get(i);
            if (source.imageBased) {
                material.setTexture(getColorMapParam(i), source.texture);
            } else {
                material.setTexture(getColorArrayParam(i), source.textureArray);
            }
        }
        if (decalSource != null && decalSource.imageBased) {
            material.setTexture(MaterialConst.DECAL_MAP, decalSource.texture);
            material.setVector2(MaterialConst.DECAL_IMAGE_SIZE,
                    new Vector2f(decalSource.imageWidth, decalSource.imageHeight));
            material.setVector4(MaterialConst.DECAL_TILE_SIZE,
                    new com.jme3.math.Vector4f(
                            decalSource.tileset.getTileWidth(),
                            decalSource.tileset.getTileHeight(),
                            decalSource.tileset.getMargin(),
                            decalSource.tileset.getSpacing()));
        }
        material.setBoolean(MaterialConst.USE_INSTANCING, true);
        material.setBoolean(MaterialConst.USE_TILESET_IMAGE, true);
    }

    private String getColorMapParam(int slot) {
        switch (slot) {
            case 0: return MaterialConst.COLOR_MAP_0;
            case 1: return MaterialConst.COLOR_MAP_1;
            case 2: return MaterialConst.COLOR_MAP_2;
            case 3: return MaterialConst.COLOR_MAP_3;
            default: throw new IllegalArgumentException("Unsupported tile texture slot: " + slot);
        }
    }

    private String getColorArrayParam(int slot) {
        switch (slot) {
            case 0: return MaterialConst.COLOR_ARRAY_0;
            case 1: return MaterialConst.COLOR_ARRAY_1;
            case 2: return MaterialConst.COLOR_ARRAY_2;
            case 3: return MaterialConst.COLOR_ARRAY_3;
            default: throw new IllegalArgumentException("Unsupported tile texture slot: " + slot);
        }
    }

    void createGeometry() {
        ensureCapacity(records.size());
        Mesh mesh = new Mesh();
        mesh.setBuffer(VertexBuffer.Type.Position, 3, new float[] {
            0, 0, -1,
            1, 0, -1,
            1, 0, 0,
            0, 0, 0
        });
        mesh.setBuffer(VertexBuffer.Type.TexCoord, 2, new float[] {
            0f, 0f,
            1f, 0f,
            1f, 1f,
            0f, 1f
        });
        mesh.setBuffer(VertexBuffer.Type.Normal, 3, new float[] {
            0, 1, 0,
            0, 1, 0,
            0, 1, 0,
            0, 1, 0
        });
        mesh.setBuffer(VertexBuffer.Type.Index, 3, new short[] { 3, 2, 1, 3, 1, 0 });
        instanceData.attach(mesh);
        tileData.attach(mesh);
        sizeData.attach(mesh);
        originData.attach(mesh);
        uvSizeData.attach(mesh);
        decal0Data.attach(mesh);
        decal1Data.attach(mesh);
        decal2Data.attach(mesh);
        decal3Data.attach(mesh);
        mesh.updateBound();
        mesh.updateCounts();
        geometry = new InstancedTileGeometry("tiles#" + layerName, mesh);
        geometry.setIgnoreTransform(true);
        geometry.setNumInstances(records.size());
    }

    private void updateBound() {
        if (geometry == null) {
            return;
        }
        if (records.isEmpty() || minX == Float.POSITIVE_INFINITY) {
            geometry.setLocalTranslation(Vector3f.ZERO);
            geometry.setModelBound(new BoundingBox(Vector3f.ZERO, 1f, 1f, 1f));
        } else {
            geometry.setLocalTranslation(Vector3f.ZERO);
            geometry.setModelBound(new BoundingBox(
                    new Vector3f((minX + maxX) * 0.5f,
                            (minY + maxY) * 0.5f,
                            (minZ + maxZ) * 0.5f),
                    Math.max((maxX - minX) * 0.5f, 1f),
                    (maxY - minY) * 0.5f,
                    Math.max((maxZ - minZ) * 0.5f, 1f)));
        }
    }
}
