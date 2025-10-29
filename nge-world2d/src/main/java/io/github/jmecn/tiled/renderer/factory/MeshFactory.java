package io.github.jmecn.tiled.renderer.factory;

import com.jme3.math.Matrix3f;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.jme3.scene.Mesh;
import com.jme3.scene.VertexBuffer;
import io.github.jmecn.tiled.core.MapObject;
import io.github.jmecn.tiled.core.Tile;

import java.nio.FloatBuffer;
import java.util.List;

/**
 * Mesh factory for creating tile mesh and object mesh.
 *
 * @author yanmaoyuan
 */
public interface MeshFactory {

    default void toIsometric(Mesh mesh, float ratio) {
        Matrix3f mat3 = new Matrix3f(
                1f, 0f, -1f,
                0f, 1f, 0f,
                ratio, 0f, ratio);

        VertexBuffer vb = mesh.getBuffer(VertexBuffer.Type.Position);
        FloatBuffer fb = (FloatBuffer) vb.getData();
        for (int i = 0; i < fb.capacity(); i += 3) {
            Vector3f v = new Vector3f(fb.get(i), 0f, fb.get(i + 2));
            mat3.multLocal(v);
            fb.put(i, v.x);
            fb.put(i + 2, v.z);
        }
        mesh.updateBound();
    }

    /**
     * Create a new mesh for a tile by global id. The mesh will be flipped if necessary.
     * @param tileId global id. The flag bits will be used.
     * @return the mesh
     */
    Mesh newTileMesh(int tileId);

    /**
     * Create a new mesh for a tile. The mesh will be flipped if necessary.
     * @param tile the tile. The tile.getGid() flag bits will be used.
     * @return the mesh
     */
    Mesh newTileMesh(Tile tile);

    /**
     * Get the mesh for a tile by global id. The mesh will be cached by its gid once created.
     * @param tileId global id with flag bits.
     * @return the mesh
     */
    Mesh getTileMesh(int tileId);

    /**
     * Get the mesh for a tile. The mesh will be cached by its gid once created.
     * @param tile the tile. The tile.getGid() flag bits will be used.
     * @return the mesh
     */
    Mesh getTileMesh(Tile tile);

    /**
     * Create a new mesh for a map object.
     * @param object the map object
     * @return the mesh
     */
    Mesh newObjectMesh(MapObject object);

    Mesh rectangle(MapObject object);

    Mesh rectangle(float width, float height, boolean fill);

    Mesh ellipse(MapObject object);

    Mesh ellipse(float width, float height, boolean fill);

    Mesh polygon(MapObject object);

    Mesh polygon(List<Vector2f> points, boolean fill);

    Mesh polyline(MapObject object);

    Mesh polyline(List<Vector2f> points, boolean closePath);

    Mesh marker();

    Mesh marker(float radius, boolean fill);

    Mesh image(MapObject object);

    Mesh image(float width, float height);
}