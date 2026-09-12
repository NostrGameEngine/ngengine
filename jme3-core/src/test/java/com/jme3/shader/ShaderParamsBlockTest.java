/*
 * Copyright (c) 2009-2026 jMonkeyEngine
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 * * Redistributions of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 *
 * * Redistributions in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer in the
 *   documentation and/or other materials provided with the distribution.
 *
 * * Neither the name of 'jMonkeyEngine' nor the names of its contributors
 *   may be used to endorse or promote products derived from this software
 *   without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
 * TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.jme3.shader;

import com.jme3.shader.bufferobject.BufferBindingPoints;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

public class ShaderParamsBlockTest {

    @Test
    public void parsesStd140MatParamBlock() {
        ShaderParamsBlock block = ShaderParamsBlock.parse(shaderSource());

        assertNotNull(block);
        assertEquals(BufferBindingPoints.MAT_PARAMS_BLOCK_NAME, block.blockName);
        assertEquals(3, block.members.length);

        assertEquals("m_Color", block.members[0].glslName);
        assertEquals("Color", block.members[0].paramName);
        assertEquals(ShaderParamsBlock.Kind.VEC4, block.members[0].kind);
        assertEquals(0, block.members[0].arrayLength);
        assertEquals(VarType.Vector4, block.members[0].varType);

        assertEquals(ShaderParamsBlock.Kind.FLOAT, block.members[1].kind);
        assertEquals(VarType.Float, block.members[1].varType);

        assertEquals("Weights", block.members[2].paramName);
        assertEquals(ShaderParamsBlock.Kind.FLOAT, block.members[2].kind);
        assertEquals(1, block.members[2].arrayLength);
        assertEquals(VarType.FloatArray, block.members[2].varType);

        assertEquals(0, block.indexOf("Color"));
        assertEquals(1, block.indexOf("Roughness"));
        assertEquals(2, block.indexOf("Weights"));
        assertEquals(-1, block.indexOf("Missing"));
    }

    @Test
    public void parsesMemberWithoutMaterialPrefix() {
        ShaderParamsBlock block = ShaderParamsBlock.parse("#version 330\n"
                + "layout(std140) uniform m_MatParams {\n"
                + "    float Roughness;\n"
                + "};\n");

        assertNotNull(block);
        assertEquals("Roughness", block.members[0].paramName);
        assertEquals("Roughness", block.members[0].glslName);
    }

    @Test
    public void parsesInstanceNamedMatParamBlock() {
        ShaderParamsBlock block = ShaderParamsBlock.parse("#version 330\n"
                + "layout(std140) uniform MatParams {\n"
                + "    vec4 Color;\n"
                + "} m_MatParams;\n");

        assertNotNull(block);
        assertEquals("MatParams", block.blockName);
        assertEquals(1, block.members.length);
    }

    @Test
    public void parsesAllSupportedMemberKinds() {
        ShaderParamsBlock block = ShaderParamsBlock.parse("#version 330\n"
                + "layout(std140) uniform m_MatParams {\n"
                + "    float A;\n"
                + "    int B;\n"
                + "    bool C;\n"
                + "    vec2 D;\n"
                + "    vec3 E;\n"
                + "    vec4 F;\n"
                + "    mat3 G;\n"
                + "    mat4 H;\n"
                + "};\n");

        assertNotNull(block);
        assertEquals(VarType.Float, block.members[0].varType);
        assertEquals(VarType.Int, block.members[1].varType);
        assertEquals(VarType.Boolean, block.members[2].varType);
        assertEquals(VarType.Vector2, block.members[3].varType);
        assertEquals(VarType.Vector3, block.members[4].varType);
        assertEquals(VarType.Vector4, block.members[5].varType);
        assertEquals(VarType.Matrix3, block.members[6].varType);
        assertEquals(VarType.Matrix4, block.members[7].varType);
    }

    @Test
    public void parsesArrayMemberTypes() {
        ShaderParamsBlock block = ShaderParamsBlock.parse("#version 330\n"
                + "layout(std140) uniform m_MatParams {\n"
                + "    float A[2];\n"
                + "    int B[2];\n"
                + "    vec2 C[2];\n"
                + "    vec3 D[2];\n"
                + "    vec4 E[2];\n"
                + "    mat3 F[2];\n"
                + "    mat4 G[2];\n"
                + "};\n");

        assertNotNull(block);
        assertEquals(VarType.FloatArray, block.members[0].varType);
        assertEquals(VarType.IntArray, block.members[1].varType);
        assertEquals(VarType.Vector2Array, block.members[2].varType);
        assertEquals(VarType.Vector3Array, block.members[3].varType);
        assertEquals(VarType.Vector4Array, block.members[4].varType);
        assertEquals(VarType.Matrix3Array, block.members[5].varType);
        assertEquals(VarType.Matrix4Array, block.members[6].varType);
        for (ShaderParamsBlock.Member member : block.members) {
            assertEquals(2, member.arrayLength);
        }
    }

    @Test
    public void boolArraysHaveNoBindableVarType() {
        ShaderParamsBlock block = ShaderParamsBlock.parse("#version 330\n"
                + "layout(std140) uniform m_MatParams {\n"
                + "    bool Flags[4];\n"
                + "};\n");

        assertNotNull(block);
        assertEquals(4, block.members[0].arrayLength);
        assertNull(block.members[0].varType);
    }

    @Test
    public void parsesMultipleDeclaratorsInOneStatement() {
        ShaderParamsBlock block = ShaderParamsBlock.parse("#version 330\n"
                + "layout(std140) uniform m_MatParams {\n"
                + "    float A, B;\n"
                + "};\n");

        assertNotNull(block);
        assertEquals(2, block.members.length);
        assertEquals("A", block.members[0].paramName);
        assertEquals("B", block.members[1].paramName);
    }

    @Test
    public void ignoresBlocksWithUnsupportedMembers() {
        assertNull(ShaderParamsBlock.parse("#version 330\n"
                + "layout(std140) uniform m_MatParams {\n"
                + "    vec4 Color;\n"
                + "    sampler2D Unsupported;\n"
                + "    float Roughness;\n"
                + "};\n"));
    }

    @Test
    public void ignoresBlocksWithUnsupportedArrayDeclarators() {
        assertNull(ShaderParamsBlock.parse("#version 330\n"
                + "#define WEIGHT_COUNT 4\n"
                + "layout(std140) uniform m_MatParams {\n"
                + "    float Weights[WEIGHT_COUNT];\n"
                + "};\n"));
    }

    @Test
    public void ignoresBlocksWithExplicitMemberLayout() {
        assertNull(ShaderParamsBlock.parse("#version 330\n"
                + "layout(std140) uniform m_MatParams {\n"
                + "    layout(offset = 32) vec4 Color;\n"
                + "};\n"));
    }

    @Test
    public void ignoresBlocksWithoutStd140Qualifier() {
        assertNull(ShaderParamsBlock.parse("#version 330\n"
                + "layout(shared) uniform m_MatParams {\n"
                + "    vec4 Color;\n"
                + "};\n"));
    }

    @Test
    public void ignoresUnrelatedUniformBlocks() {
        assertNull(ShaderParamsBlock.parse("#version 330\n"
                + "layout(std140) uniform m_Lights {\n"
                + "    vec4 Color;\n"
                + "};\n"));
    }

    @Test
    public void ignoresCommentsWhenParsing() {
        ShaderParamsBlock block = ShaderParamsBlock.parse("#version 330\n"
                + "// layout(std140) uniform m_MatParams { float Decoy; };\r\n"
                + "/* layout(std140) uniform m_MatParams { float Decoy; }; */\n"
                + "layout(std140) uniform m_MatParams {\n"
                + "    vec4 Color; // real member\n"
                + "};\n");

        assertNotNull(block);
        assertEquals(1, block.members.length);
        assertEquals("Color", block.members[0].paramName);
    }

    @Test
    public void parsesTheBlockFromAShaderSource() {
        Shader shader = new Shader();
        shader.addSource(Shader.ShaderType.Fragment, "params-block.frag", shaderSource(), null, "GLSL330");

        assertNotNull(shader.getParamsBlock());
        assertSame(shader.getParamsBlock(), shader.getParamsBlock());
    }

    @Test
    public void cachesShadersWithoutSupportedMaterialBlocks() {
        Shader shader = new Shader();
        shader.addSource(Shader.ShaderType.Fragment, "no-params-block.frag",
                "#version 330\nvoid main() {}\n", null, "GLSL330");

        assertNull(shader.getParamsBlock());
        assertNull(shader.getParamsBlock());
    }

    @Test
    public void addingASourceInvalidatesTheCachedBlock() {
        Shader shader = new Shader();
        shader.addSource(Shader.ShaderType.Vertex, "params-block.vert",
                "#version 330\nvoid main() {}\n", null, "GLSL330");
        assertNull(shader.getParamsBlock());

        shader.addSource(Shader.ShaderType.Fragment, "params-block.frag", shaderSource(), null, "GLSL330");
        assertNotNull(shader.getParamsBlock());
    }

    private static String shaderSource() {
        return "#version 330\n"
                + "layout(std140) uniform m_MatParams {\n"
                + "    vec4 m_Color;\n"
                + "    float m_Roughness;\n"
                + "    float m_Weights[1];\n"
                + "};\n"
                + "void main() {}\n";
    }
}
