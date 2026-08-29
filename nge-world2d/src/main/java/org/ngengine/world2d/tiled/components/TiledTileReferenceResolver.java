package org.ngengine.world2d.tiled.components;

import com.jme3.asset.AssetManager;

import org.ngengine.world2d.tiled.core.TiledMap;
import org.ngengine.world2d.tiled.core.tileset.Tile;
import org.ngengine.world2d.tiled.core.tileset.Tileset;

/** Resolves a tile snapshot without assuming that runtime tileset gids are map gids. */
final class TiledTileReferenceResolver {
    private TiledTileReferenceResolver() {}

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
                        return referenced;
                    }
                }
            }
        }

        Tileset loaded = loadTileset(assets, source);
        Tile referenced = tileFrom(loaded, tileClass, tileId);
        return referenced != null ? referenced : gidTile;
    }

    private static Tile tileForGid(TiledMap map, int gid) {
        if (map == null || gid <= 0) {
            return null;
        }
        try {
            return map.getTileForTileGID(gid);
        } catch (RuntimeException ignored) {
            return null;
        }
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
