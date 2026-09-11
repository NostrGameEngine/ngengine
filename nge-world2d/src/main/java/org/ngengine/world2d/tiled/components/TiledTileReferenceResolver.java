package org.ngengine.world2d.tiled.components;

import com.jme3.asset.AssetManager;

import org.ngengine.world2d.tiled.core.TiledMap;
import org.ngengine.world2d.tiled.core.tileset.Tile;
import org.ngengine.world2d.tiled.core.tileset.Tileset;

/**
 * Resolves tile references carried by Tiled object snapshots.
 *
 * <p>A snapshot may contain both a map GID and metadata for a tile in an external
 * tileset. The GID is used first, unless its tile does not match the supplied
 * tileset source and tile class or local tile ID. Referenced tilesets are then
 * searched among the map's loaded tilesets and loaded through the asset manager
 * when necessary. A tile class is preferred to a local tile ID when both are
 * supplied.
 *
 * <p>Flip flags encoded in the GID are preserved on the returned tile. If the
 * reference cannot be resolved, the tile found from the GID is returned;
 * {@code null} is returned when neither lookup succeeds.
 */
final class TiledTileReferenceResolver {
    private TiledTileReferenceResolver() {}

    /**
     * Resolves a tile from its map GID and optional external tileset reference.
     *
     * @param assets asset manager used to load an external tileset; may be {@code null}
     * @param map map used for the GID lookup and loaded-tileset search; may be {@code null}
     * @param gid map GID, including any encoded flip flags
     * @param source external tileset source path; blank values disable reference lookup
     * @param tileClass optional tileset tile class, preferred over {@code tileId}
     * @param tileId optional tileset-local tile ID; negative values mean unspecified
     * @return the resolved tile with the GID's flip flags, the GID tile when the
     *         reference cannot be resolved, or {@code null} when neither lookup succeeds
     */
    static Tile resolve(
            AssetManager assets,
            TiledMap map,
            int gid,
            String source,
            String tileClass,
            int tileId) {
        Tile gidTile = tileForGid(map, gid);
        if (!hasReference(source, tileClass, tileId) || matches(gidTile, source, tileClass, tileId)) {
            return gidTile;
        }

        if (map != null) {
            for (Tileset tileset : map.getTileSets()) {
                if (sameSource(tileset.getSource(), source)) {
                    Tile referenced = tileFrom(tileset, tileClass, tileId);
                    if (referenced != null) {
                        return withFlippedMask(referenced, gid);
                    }
                }
            }
        }

        Tileset loaded = loadTileset(assets, source);
        Tile referenced = tileFrom(loaded, tileClass, tileId);
        return referenced != null ? withFlippedMask(referenced, gid) : gidTile;
    }

    private static Tile tileForGid(TiledMap map, int gid) {
        int baseGid = gid & ~Tile.FLIPPED_MASK;
        if (map == null || baseGid <= 0) {
            return null;
        }
        try {
            return withFlippedMask(map.getTileForTileGID(baseGid), gid);
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    private static Tile withFlippedMask(Tile tile, int gid) {
        if (tile == null) {
            return null;
        }
        int mask = gid & Tile.FLIPPED_MASK;
        if (tile.getFlippedMask() == mask) {
            return tile;
        }
        Tile transformed = tile.copy();
        transformed.setFlippedMask(mask);
        return transformed;
    }

    private static boolean hasReference(String source, String tileClass, int tileId) {
        return source != null && !source.trim().isEmpty()
            && ((tileClass != null && !tileClass.trim().isEmpty()) || tileId >= 0);
    }

    private static boolean matches(Tile tile, String source, String tileClass, int tileId) {
        if (tile == null || tile.getTileset() == null
                || !sameSource(tile.getTileset().getSource(), source)) {
            return false;
        }
        if (tileClass != null && !tileClass.trim().isEmpty()) {
            return tileClass.equals(tile.getClazz());
        }
        return tileId < 0 || tileId == tile.getId();
    }

    private static Tile tileFrom(Tileset tileset, String tileClass, int tileId) {
        if (tileset == null) {
            return null;
        }
        if (tileClass != null && !tileClass.trim().isEmpty()) {
            Tile byClass = tileset.findByClass(tileClass);
            if (byClass != null) {
                return byClass;
            }
        }
        return tileId >= 0 ? tileset.getTile(tileId) : null;
    }

    private static Tileset loadTileset(AssetManager assets, String source) {
        if (assets == null || source == null || source.trim().isEmpty()) {
            return null;
        }
        String normalized = normalize(source);
        try {
            Object loaded = assets.loadAsset(normalized);
            return loaded instanceof Tileset ? (Tileset) loaded : null;
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    private static boolean sameSource(String left, String right) {
        return left != null && right != null && normalize(left).equals(normalize(right));
    }

    private static String normalize(String source) {
        String normalized = source.replace('\\', '/').trim();
        while (normalized.startsWith("../")) {
            normalized = normalized.substring(3);
        }
        while (normalized.startsWith("./")) {
            normalized = normalized.substring(2);
        }
        return normalized;
    }
}
