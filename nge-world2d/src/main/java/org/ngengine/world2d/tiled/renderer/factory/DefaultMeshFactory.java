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

package org.ngengine.world2d.tiled.renderer.factory;

import com.jme3.math.Vector2f;
import com.jme3.scene.Mesh;
import com.jme3.util.IntMap;

import org.ngengine.world2d.tiled.core.TiledMap;
import org.ngengine.world2d.tiled.core.entity.TiledObjectEntity;
import org.ngengine.world2d.tiled.core.tileset.Tile;
import org.ngengine.world2d.tiled.core.tileset.Tileset;
import org.ngengine.world2d.tiled.enums.FillMode;
import org.ngengine.world2d.tiled.enums.Orientation;
import org.ngengine.world2d.tiled.renderer.shape.*;

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
    public Mesh newObjectMesh(TiledObjectEntity obj) {
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
    public Rect rectangle(TiledObjectEntity object) {
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
    public Ellipse ellipse(TiledObjectEntity object) {
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
    public Polygon polygon(TiledObjectEntity object) {
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
    public Polyline polyline(TiledObjectEntity object) {
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
    public Rect image(TiledObjectEntity object) {
        return image((float) object.getWidth(), (float) object.getHeight());
    }

    @Override
    public Rect image(float width, float height) {
        return new Rect(width, height, true);
    }

    public Rect text(TiledObjectEntity object) {
        return text((float) object.getWidth(), (float) object.getHeight());
    }

    public Rect text(float width, float height) {
        return new Rect(width, height, true);
    }

public TileMesh tile(TiledObjectEntity obj) {
    Tile tile = obj.getTile();
    if (tile == null) {
        float width = Math.max(1f, (float) obj.getWidth());
        float height = Math.max(1f, (float) obj.getHeight());
        logger.warning("Tile object has null tile data for object '" + obj.getName()
            + "' (gid=" + obj.getGid() + "), using fallback mesh.");
        return new TileMesh(
            new Vector2f(0f, 0f),
            new Vector2f(width, height),
            new Vector2f(0f, 0f),
            new Vector2f(0f, 0f),
            obj.getGid(),
            orientation
        );
    }

    Vector2f coord = new Vector2f(tile.getX(), tile.getY());

    Vector2f size = new Vector2f(tile.getWidth(), tile.getHeight());
    Vector2f offset = (tile.getTileset() != null)
            ? tile.getTileset().getTileOffset().clone()
            : new Vector2f(0, 0);

    if (tile.getTileset() != null && tile.getTileset().getFillMode() == FillMode.STRETCH) {
        float wObj = (float) obj.getWidth();
        float hObj = (float) obj.getHeight();

        float sx = wObj / size.x;
        float sy = hObj / size.y;

        size.set(wObj, hObj);
        offset.multLocal(sx, sy);
    }


    Vector2f origin = new Vector2f();
    if (orientation == Orientation.ISOMETRIC) {
        origin.set(-size.x * 0.5f, 0f);
    } else {
        origin.set(0f, 0f);
    }

    return new TileMesh(coord, size, offset, origin, tile.getGid(), orientation);
}


}
