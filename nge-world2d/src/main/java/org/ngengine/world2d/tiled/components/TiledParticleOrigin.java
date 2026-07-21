package org.ngengine.world2d.tiled.components;

import com.jme3.math.FastMath;
import com.jme3.math.Vector2f;

import org.ngengine.world2d.tiled.core.TiledObjectLayer;
import org.ngengine.world2d.tiled.core.entity.TiledObjectEntity;
import org.ngengine.world2d.tiled.core.tileset.Tile;
import org.ngengine.world2d.tiled.core.tileset.Tileset;
import org.ngengine.world2d.tiled.enums.FillMode;
import org.ngengine.world2d.tiled.enums.Orientation;
import org.ngengine.world2d.tiled.renderer.TileObjectAlignment;
import org.ngengine.world2d.tiled.util.CoordinateSystem;

/**
 * Resolves an optional, arbitrary visual origin declared inside a particle tile.
 *
 * <p>The center of a collision object carrying {@code particles.origin=true}
 * is aligned with the requested emitter position. When no marker is declared,
 * callers keep Tiled's normal {@code objectalignment} behavior.</p>
 */
public final class TiledParticleOrigin {
    public static final String PROPERTY_ORIGIN = "particles.origin";

    private TiledParticleOrigin() {
    }

    public static TiledObjectEntity findOrigin(Tile tile) {
        TiledObjectLayer objects = tile != null ? tile.getCollisions() : null;
        if (objects == null) {
            return null;
        }
        for (TiledObjectEntity object : objects.getObjects()) {
            Object value = object.getProperty(PROPERTY_ORIGIN);
            if (value instanceof Boolean && ((Boolean) value).booleanValue()) {
                return object;
            }
            if (value != null && Boolean.parseBoolean(String.valueOf(value).trim())) {
                return object;
            }
        }
        return null;
    }

    /**
     * Moves a particle object so its declared visual origin lands on a grid-space anchor.
     *
     * @return true when a custom origin was found and applied
     */
    public static boolean alignToGridAnchor(TiledObjectEntity particle, Vector2f anchorGrid,
            CoordinateSystem coordinates, Orientation orientation) {
        if (particle == null || anchorGrid == null || coordinates == null) {
            return false;
        }
        Vector2f originOffset = new Vector2f();
        if (!resolveOriginOffset(particle, orientation, originOffset)) {
            return false;
        }

        Vector2f anchorWorld = new Vector2f();
        coordinates.gridToWorldSpace(anchorGrid.x, anchorGrid.y, anchorWorld);
        Vector2f particleGrid = new Vector2f();
        coordinates.worldToGridSpace(
                anchorWorld.x - originOffset.x,
                anchorWorld.y - originOffset.y,
                particleGrid
        );
        particle.setX(particleGrid.x);
        particle.setY(particleGrid.y);
        return true;
    }

    /**
     * Resolves the grid-space anchor represented by a particle's custom origin.
     *
     * <p>This is the inverse of {@link #alignToGridAnchor}: it allows spatial
     * lookups to compare the requested emitter position instead of the shifted
     * top-left coordinate of the rendered particle object.</p>
     *
     * @param particle the particle object to inspect
     * @param coordinates the map coordinate system
     * @param orientation the map orientation, or null for orthogonal
     * @param store storage for the resolved grid-space anchor
     * @return true when the particle declares and resolves a custom origin
     */
    public static boolean getGridAnchor(TiledObjectEntity particle, CoordinateSystem coordinates,
            Orientation orientation, Vector2f store) {
        if (particle == null || coordinates == null || store == null) {
            return false;
        }
        if (!resolveOriginOffset(particle, orientation, store)) {
            return false;
        }
        float originOffsetX = store.x;
        float originOffsetY = store.y;
        coordinates.gridToWorldSpace((float) particle.getX(), (float) particle.getY(), store);
        store.addLocal(originOffsetX, originOffsetY);
        coordinates.worldToGridSpace(store.x, store.y, store);
        return true;
    }

    private static boolean resolveOriginOffset(TiledObjectEntity particle, Orientation orientation, Vector2f store) {
        Tile tile = particle != null ? particle.getTile() : null;
        TiledObjectEntity marker = findOrigin(tile);
        if (tile == null || marker == null || tile.getWidth() <= 0 || tile.getHeight() <= 0) {
            return false;
        }

        Tileset tileset = tile.getTileset();
        boolean stretch = tileset == null || tileset.getFillMode() == null
                || tileset.getFillMode() == FillMode.STRETCH;
        float renderedWidth = stretch ? (float) particle.getWidth() : tile.getWidth();
        float renderedHeight = stretch ? (float) particle.getHeight() : tile.getHeight();
        float scaleX = renderedWidth / tile.getWidth();
        float scaleY = renderedHeight / tile.getHeight();

        float markerX = ((float) marker.getX() + (float) marker.getWidth() * 0.5f) * scaleX;
        float markerY = ((float) marker.getY() + (float) marker.getHeight() * 0.5f) * scaleY;
        if (tile.isFlippedHorizontally()) {
            markerX = renderedWidth - markerX;
        }
        if (tile.isFlippedVertically()) {
            markerY = renderedHeight - markerY;
        }

        Orientation mapOrientation = orientation != null ? orientation : Orientation.ORTHOGONAL;
        Vector2f objectOrigin = TileObjectAlignment.origin(mapOrientation, tile, renderedWidth, renderedHeight);
        float offsetX = 0f;
        float offsetY = 0f;
        if (tileset != null && tileset.getTileOffset() != null) {
            offsetX = tileset.getTileOffset().x * (stretch ? scaleX : 1f);
            offsetY = tileset.getTileOffset().y * (stretch ? scaleY : 1f);
        }

        float localX = objectOrigin.x + offsetX + markerX;
        float localZ = objectOrigin.y + offsetY - renderedHeight + markerY;
        float angle = (float) (-particle.getRotation() * FastMath.DEG_TO_RAD);
        float cos = FastMath.cos(angle);
        float sin = FastMath.sin(angle);
        store.set(
                cos * localX + sin * localZ,
                -sin * localX + cos * localZ
        );
        return true;
    }
}
