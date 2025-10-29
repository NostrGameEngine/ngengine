package io.github.jmecn.tiled.renderer.factory;

import com.jme3.math.Vector2f;
import com.jme3.scene.Mesh;
import com.jme3.util.IntMap;
import io.github.jmecn.tiled.core.MapObject;
import io.github.jmecn.tiled.core.Tile;
import io.github.jmecn.tiled.core.TiledMap;
import io.github.jmecn.tiled.core.Tileset;
import io.github.jmecn.tiled.enums.FillMode;
import io.github.jmecn.tiled.enums.Orientation;
import io.github.jmecn.tiled.renderer.shape.*;


import java.util.List;
import java.util.logging.Logger;

/**
 * desc:
 *
 * @author yanmaoyuan
 */
public class DefaultMeshFactory implements MeshFactory {

    private static final int ELLIPSE_POINTS = 36;// This value used to generate ellipse mesh.
    public static final float MARKER_RADIUS = 16f;

    private final static Logger logger = Logger.getLogger(DefaultMeshFactory.class.getName());

    private final TiledMap tiledMap;
    private final Orientation orientation;
    private final float ratio;
    private final IntMap<TileMesh> cache;

    public DefaultMeshFactory() {
        this(null);
    }

    public DefaultMeshFactory(Orientation orientation, float ratio) {
        this.tiledMap = null;
        this.orientation = orientation;
        this.ratio = ratio;
        this.cache = new IntMap<>();
    }

    public DefaultMeshFactory(TiledMap tiledMap) {
        this.tiledMap = tiledMap;
        if (tiledMap != null) {
            this.orientation = tiledMap.getOrientation();
            this.ratio = (float) tiledMap.getTileHeight() / tiledMap.getTileWidth();
        } else {
            this.orientation = Orientation.ORTHOGONAL;
            this.ratio = 1f;
        }
        this.cache = new IntMap<>();
    }

    @Override
    public TileMesh newTileMesh(int tileId) {
        // clear the flag
        int gid = tileId & ~Tile.FLIPPED_MASK;

        Tile tile = tiledMap.getTileForTileGID(gid);
        if (tile == null) {
            throw new IllegalArgumentException("Tile not found, id: " + tileId);
        }

        if (tile.getGid() != tileId) {
            Tile t = tile.copy();
            t.setGid(tileId);
            tile = t;
        }

        return newTileMesh(tile);
    }

    @Override
    public TileMesh newTileMesh(Tile tile) {
        Tileset tileset = tile.getTileset();
        Vector2f offset = tileset.getTileOffset();

        Vector2f origin;
        if (orientation == Orientation.ISOMETRIC) {
            // isometric map tile origin is on top-center the tile
            origin = new Vector2f(-tile.getWidth() * 0.5f, tiledMap.getTileHeight());
        } else {
            // the tile origin is on top-left corner
            origin = new Vector2f(0, tiledMap.getTileHeight());
        }

        Vector2f coord = new Vector2f(tile.getX(), tile.getY());
        Vector2f size = new Vector2f(tile.getWidth(), tile.getHeight());

        return new TileMesh(coord, size, offset, origin, tile.getGid(), orientation);
    }

    @Override
    public TileMesh getTileMesh(int tileId) {
        if (cache.containsKey(tileId)) {
            return cache.get(tileId);
        } else {
            TileMesh mesh = newTileMesh(tileId);
            cache.put(tileId, mesh);
            return mesh;
        }
    }

    @Override
    public TileMesh getTileMesh(Tile tile) {
        if (cache.containsKey(tile.getGid())) {
            return cache.get(tile.getGid());
        } else {
            TileMesh mesh = newTileMesh(tile);
            cache.put(tile.getGid(), mesh);
            return mesh;
        }
    }

    @Override
    public Mesh newObjectMesh(MapObject obj) {
        Mesh mesh;
        switch (obj.getShape()) {
            case RECTANGLE: {
                mesh = rectangle(obj);
                break;
            }
            case ELLIPSE: {
                mesh = ellipse(obj);
                break;
            }
            case POLYGON: {
                mesh = polygon(obj);
                break;
            }
            case POLYLINE: {
                mesh = polyline(obj);
                break;
            }
            case POINT: {
                mesh = marker();
                break;
            }
            case IMAGE: {
                mesh = image(obj);
                break;
            }
            case TEXT: {
                mesh = text(obj);
                break;
            }
            case TILE: {
                mesh = tile(obj);
                break;
            }
            default: {
                mesh = null;
                logger.warning("Unsupported object type: " + obj.getShape());
                break;
            }
        }
        return mesh;
    }

    @Override
    public Rect rectangle(MapObject object) {
        return rectangle((float) object.getWidth(), (float) object.getHeight(), false);
    }

    @Override
    public Rect rectangle(float width, float height, boolean fill) {
        Rect mesh = new Rect(width, height, fill);
        if (orientation == Orientation.ISOMETRIC) {
            toIsometric(mesh, ratio);
        }
        return mesh;
    }

    @Override
    public Ellipse ellipse(MapObject object) {
        return ellipse((float) object.getWidth(), (float) object.getHeight(), false);
    }

    @Override
    public Ellipse ellipse(float width, float height, boolean fill) {
        Ellipse mesh = new Ellipse(width, height, ELLIPSE_POINTS, fill);
        if (orientation == Orientation.ISOMETRIC) {
            toIsometric(mesh, ratio);
        }
        return mesh;
    }

    @Override
    public Polygon polygon(MapObject object) {
        return polygon(object.getPoints(), false);
    }

    @Override
    public Polygon polygon(List<Vector2f> points, boolean fill) {
        Polygon mesh = new Polygon(points, fill);
        if (orientation == Orientation.ISOMETRIC) {
            toIsometric(mesh, ratio);
        }
        return mesh;
    }

    @Override
    public Polyline polyline(MapObject object) {
        return polyline(object.getPoints(), false);
    }

    @Override
    public Polyline polyline(List<Vector2f> points, boolean closePath) {
        Polyline mesh = new Polyline(points, closePath);
        if (orientation == Orientation.ISOMETRIC) {
            toIsometric(mesh, ratio);
        }
        return mesh;
    }

    @Override
    public Marker marker() {
        return marker(MARKER_RADIUS, false);
    }

    @Override
    public Marker marker(float radius, boolean fill) {
        return new Marker(radius, ELLIPSE_POINTS, fill);
    }

    @Override
    public Rect image(MapObject object) {
        return image((float) object.getWidth(), (float) object.getHeight());
    }

    @Override
    public Rect image(float width, float height) {
        return new Rect(width, height, true);
    }

    public Rect text(MapObject object) {
        return text((float) object.getWidth(), (float) object.getHeight());
    }

    public Rect text(float width, float height) {
        return new Rect(width, height, true);
    }

    public TileMesh tile(MapObject obj) {
        Tile tile = obj.getTile();
        float tw = tile.getWidth();

        Vector2f coord = new Vector2f(tile.getX(), tile.getY());
        Vector2f size = new Vector2f(tile.getWidth(), tile.getHeight());
        Vector2f offset;
        if (tile.getTileset() != null) {
            Tileset tileset = tile.getTileset();
            offset = tileset.getTileOffset();
            // scale the tile
            if (tileset.getFillMode() == FillMode.STRETCH) {
                size.set((int) obj.getWidth(), (int) obj.getHeight());
            }
        } else {
            offset = new Vector2f(0, 0);
        }

        Vector2f origin;
        if (orientation == Orientation.ISOMETRIC) {
            // In isometric, it's aligned to the bottom-center.
            origin = new Vector2f(-tw * 0.5f, 0);
        } else {
            // In orthogonal, it's aligned to the bottom-left
            origin = new Vector2f(0, 0);
        }

        // When the object has a gid set, then it is represented by
        // the image of the tile with that global ID. The image
        // alignment currently depends on the map orientation.
        return new TileMesh(coord, size, offset, origin, tile.getGid(), orientation);
    }
}
