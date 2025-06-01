package org.ngengine.demo.son.utils;

import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.jme3.scene.Mesh;
import com.jme3.scene.VertexBuffer;
import com.jme3.util.BufferUtils;

public class GridMesh extends Mesh {
    
    public GridMesh(int centerRes, float centerSize, float outerSize) {     
        super();
        final int outerRes = 32;
        final int totalRes = centerRes + outerRes * 2; 
         int vertCount = totalRes * totalRes;
        Vector3f[] positions = new Vector3f[vertCount];
        Vector2f[] uvs = new Vector2f[vertCount];
        int[] indices = new int[(totalRes - 1) * (totalRes - 1) * 6];
        Vector3f[] normals = new Vector3f[vertCount];
        float outerOffset = centerSize / 2f;
        float outerExtent = (outerSize - centerSize) / 2f;

        int idx = 0;
        for (int z = 0; z < totalRes; z++) {
            for (int x = 0; x < totalRes; x++) {
                boolean inCenterX = x >= outerRes && x < centerRes + outerRes;
                boolean inCenterZ = z >= outerRes && z < centerRes + outerRes;

                float posX, posZ;
                float uvX = (float) x / (totalRes - 1);
                float uvZ = (float) z / (totalRes - 1);

                if (inCenterX) {
                    float centerX = (float) (x - outerRes) / centerRes;
                    posX = (centerX - 0.5f) * centerSize;
                } else if (x < outerRes) {
                    float t = (float) x / outerRes;
                    posX = -outerOffset - outerExtent * (1f - t);
                } else {
                    float t = (float) (x - centerRes - outerRes) / outerRes;
                    posX = outerOffset + outerExtent * t;
                }

                if (inCenterZ) {
                    float centerZ = (float) (z - outerRes) / centerRes;
                    posZ = (centerZ - 0.5f) * centerSize;
                } else if (z < outerRes) {
                    float t = (float) z / outerRes;
                    posZ = -outerOffset - outerExtent * (1f - t);
                } else {
                    float t = (float) (z - centerRes - outerRes) / outerRes;
                    posZ = outerOffset + outerExtent * t;
                }

                positions[idx] = new Vector3f(posX, 0, posZ);
                uvs[idx] = new Vector2f(uvX, uvZ);
                normals[idx] = new Vector3f(0, 1, 0);
                idx++;
            }
        }

        int tri = 0;
        for (int z = 0; z < totalRes - 1; z++) {
            for (int x = 0; x < totalRes - 1; x++) {
                int i0 = z * totalRes + x;
                int i1 = i0 + 1;
                int i2 = i0 + totalRes;
                int i3 = i2 + 1;
                indices[tri++] = i0;
                indices[tri++] = i2;
                indices[tri++] = i1;
                indices[tri++] = i1;
                indices[tri++] = i2;
                indices[tri++] = i3;
            }
        }

        setBuffer(VertexBuffer.Type.Position, 3, BufferUtils.createFloatBuffer(positions));
        setBuffer(VertexBuffer.Type.TexCoord, 2, BufferUtils.createFloatBuffer(uvs));
        setBuffer(VertexBuffer.Type.Index, 3, BufferUtils.createIntBuffer(indices));
        setBuffer(VertexBuffer.Type.Normal, 3, BufferUtils.createFloatBuffer(normals));
        updateBound();
     }

}
