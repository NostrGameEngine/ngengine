package io.github.jmecn.tiled.renderer.shape;

import com.jme3.math.FastMath;
import com.jme3.math.Matrix3f;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.jme3.scene.Mesh;
import com.jme3.scene.VertexBuffer;
import com.jme3.util.TempVars;
import io.github.jmecn.tiled.core.Tile;
import io.github.jmecn.tiled.enums.Orientation;

import java.nio.FloatBuffer;

/**
 * This is the mesh of a tile.
 *
 * @author yanmaoyuan
 */
public class TileMesh extends Mesh {

    private final Vector2f coord;
    private final Vector2f size;
    private final Vector2f offset;
    private final Vector2f origin;

    /**
     * Create a tile mesh.
     *
     * @param coord the coordinate of the tile
     * @param size the size of the tile
     * @param offset the offset of the tile
     * @param origin the origin of the tile
     */
    public TileMesh(Vector2f coord, Vector2f size, Vector2f offset, Vector2f origin) {
        this.coord = coord;
        this.size = size;
        this.offset = offset;
        this.origin = origin;

        float x = coord.getX();
        float y = coord.getY();

        float[] vertices = getPositions(size, offset, origin);

        // use TexCoord2 to store the tile's position in the image.
        // this is useful when we want to get the tile's position in the image.
        float[] texCoord2 = new float[]{x, y, 0, x, y, 0, x, y, 0, x, y, 0};

        float[] texCoords = new float[]{0f, 0f, 1f, 0f, 1f, 1f, 0f, 1f};
        short[] indexes = new short[]{3, 2, 1, 3, 1, 0};
        float[] normals = new float[]{0, 1, 0, 0, 1, 0, 0, 1, 0, 0, 1, 0};

        this.setBuffer(VertexBuffer.Type.Position, 3, vertices);
        this.setBuffer(VertexBuffer.Type.TexCoord, 2, texCoords);
        this.setBuffer(VertexBuffer.Type.TexCoord2, 3, texCoord2);
        this.setBuffer(VertexBuffer.Type.Normal, 3, normals);
        this.setBuffer(VertexBuffer.Type.Index, 3, indexes);
        this.updateBound();
        this.updateCounts();
        this.setStatic();
    }

    /**
     * Calculate the vertices' position of this tile.
     *
     * <pre>
     * 0          1
     * *----------*
     * |        * |
     * |      *   |
     * |    *     |
     * |  *       |
     * *----------*
     * 3          2
     * </pre>
     * @param size the size of the tile
     * @param offset the offset of the tile
     * @param origin the origin of the tile
     */
    private float[] getPositions(Vector2f size, Vector2f offset, Vector2f origin) {
        float[] vertices = new float[]{
                0, 0, -1,
                1, 0, -1,
                1, 0, 0,
                0, 0, 0};

        // I know this can be down in vertex shader, but it will cause view frustum culling problem.
        for (int i = 0; i < vertices.length; i += 3) {
            vertices[i] = vertices[i] * size.getX() + origin.x + offset.getX();
            vertices[i + 2] = vertices[i + 2] * size.getY() + origin.y + offset.getY();
        }
        return vertices;
    }

    public TileMesh(Vector2f coord, Vector2f size, Vector2f offset, Vector2f origin, int gid, Orientation orientation) {
        this(coord, size, offset, origin);

        boolean isFlipHorizontally = (gid & Tile.FLIPPED_HORIZONTALLY_FLAG) != 0;
        boolean isFlipVertically = (gid & Tile.FLIPPED_VERTICALLY_FLAG) != 0;
        boolean isFlipAntiDiagonally = (gid & Tile.FLIPPED_DIAGONALLY_FLAG) != 0;
        boolean isRotatedHexagonal120 = (gid & Tile.ROTATED_HEXAGONAL_120_FLAG) != 0;

        float[] texCoords = new float[]{0f, 0f, 1f, 0f, 1f, 1f, 0f, 1f};

        if (isFlipHorizontally) {
            swapUV(texCoords, 0, 1);
            swapUV(texCoords, 2, 3);
        }
        if (isFlipVertically) {
            swapUV(texCoords, 0, 3);
            swapUV(texCoords, 1, 2);
        }

        if (orientation == Orientation.HEXAGONAL) {
            float rotate = 0f;
            if (isFlipAntiDiagonally) {
                rotate += FastMath.PI / 3f;// 60
            }
            if (isRotatedHexagonal120) {
                rotate += FastMath.TWO_PI / 3f;// 120
            }
            if (rotate != 0f) {
                rotate(rotate);
            }
        } else {
            if (isFlipAntiDiagonally) {
                swapUV(texCoords, 0, 2);
            }
        }

        this.setBuffer(VertexBuffer.Type.TexCoord, 2, texCoords);

        this.updateBound();
        this.updateCounts();
        this.setStatic();
    }

    private void swapUV(float[] buf, int i, int j) {
        int component = 2;
        for (int k = 0; k < component; k++) {
            int a = i * component + k;
            int b = j * component + k;
            float tmp = buf[b];
            buf[b] = buf[a];
            buf[a] = tmp;
        }
    }

    private void rotate(float rotate) {
        float[] vertices = new float[12];
        FloatBuffer buffer = (FloatBuffer) getBuffer(VertexBuffer.Type.Position).getData();
        buffer.flip();
        buffer.get(vertices);

        TempVars vars = TempVars.get();
        Matrix3f mat3 = vars.tempMat3;
        mat3.fromAngleAxis(-rotate, Vector3f.UNIT_Y);

        // center
        Vector3f center = vars.vect2;
        center.x = (vertices[0] + vertices[3] + vertices[6] + vertices[9]) / 4f;
        center.y = (vertices[1] + vertices[4] + vertices[7] + vertices[10]) / 4f;
        center.z = (vertices[2] + vertices[5] + vertices[8] + vertices[11]) / 4f;

        for (int i = 0; i < 4; i++) {
            Vector3f pos = vars.vect1;
            pos.set(vertices[i * 3], vertices[i * 3 + 1], vertices[i * 3 + 2]);
            pos.subtractLocal(center);
            mat3.mult(pos, pos);
            pos.addLocal(center);
            vertices[i * 3] = pos.x;
            vertices[i * 3 + 1] = pos.y;
            vertices[i * 3 + 2] = pos.z;
        }
        vars.release();

        this.setBuffer(VertexBuffer.Type.Position, 3, vertices);
    }

    public Vector2f getCoord() {
        return coord;
    }

    public Vector2f getSize() {
        return size;
    }

    public Vector2f getOffset() {
        return offset;
    }

    public Vector2f getOrigin() {
        return origin;
    }
}
