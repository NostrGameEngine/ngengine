package com.jme3.util.mikktspace;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class MikkTSpaceContextTest {
    @Test
    void basicAndFullCallbacksDescribeTheSameAnalyticBasis() {
        for (float uvDirection : new float[]{1f, -1f}) {
            float[][] positions = {{0, 0, 0}, {1, 0, 0}, {0, 1, 0}};
            float[][] uv = {{0, 0}, {uvDirection, 0}, {0, 1}};
            float[] signs = new float[3];
            float[] expectedSigns = new float[3];
            MikkTSpaceContext context = new MikkTSpaceContext() {
                @Override public int getNumFaces() { return 1; }
                @Override public int getNumVerticesOfFace(int face) { return 3; }
                @Override public void getPosition(float[] out, int face, int vertex) {
                    System.arraycopy(positions[vertex], 0, out, 0, 3);
                }
                @Override public void getNormal(float[] out, int face, int vertex) {
                    out[0] = 0; out[1] = 0; out[2] = 1;
                }
                @Override public void getTexCoord(float[] out, int face, int vertex) {
                    System.arraycopy(uv[vertex], 0, out, 0, 2);
                }
                @Override public void setTSpaceBasic(float[] tangent, float sign, int face, int vertex) {
                    assertArrayEquals(new float[]{uvDirection, 0, 0}, tangent, 1e-6f);
                    signs[vertex] = sign;
                }
                @Override public void setTSpace(float[] tangent, float[] bitangent, float magS,
                        float magT, boolean orientation, int face, int vertex) {
                    assertArrayEquals(new float[]{0, 1, 0}, bitangent, 1e-6f);
                    assertEquals(1f, magS, 1e-6f);
                    assertEquals(1f, magT, 1e-6f);
                    expectedSigns[vertex] = orientation ? 1f : -1f;
                }
            };
            assertTrue(MikktspaceTangentGenerator.genTangSpaceDefault(context));
            assertArrayEquals(expectedSigns, signs, 0f);
            for (float sign : signs) {
                // cross(+Z, tangent) * sign must be the analytic +Y bitangent.
                assertEquals(1f, uvDirection * sign, 1e-6f);
            }
        }
    }
}
