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
package com.jme3.material;

import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.jme3.math.Vector4f;
import com.jme3.shader.Shader;
import com.jme3.shader.ShaderBufferBlock;
import com.jme3.shader.ShaderParamsBlock;
import com.jme3.shader.VarType;
import com.jme3.shader.bufferobject.BufferBindingPoints;
import com.jme3.shader.bufferobject.BufferRegion;
import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class MatParamUniformBufferTest {

    @Test
    public void writesBlockMembersToBufferObject() {
        Shader shader = shaderWithBlock();
        MatParamUniformBuffer buffer = new MatParamUniformBuffer(shader);

        buffer.begin();
        assertTrue(buffer.set(new MatParam(VarType.Vector4, "Color", new ColorRGBA(1f, 0.5f, 0.25f, 1f)), false));
        assertTrue(buffer.set(new MatParam(VarType.Float, "Roughness", 0.75f), false));
        assertFalse(buffer.set(new MatParam(VarType.Float, "Outside", 1f), false));
        buffer.finish();

        ShaderBufferBlock block = shader.getBufferBlock(BufferBindingPoints.MAT_PARAMS_BLOCK_NAME);
        assertSame(buffer.getBufferObject(), block.getBufferObject());
        assertEquals(ShaderBufferBlock.BufferType.UniformBufferObject, block.getType());

        ByteBuffer data = buffer.getBufferObject().getByteData();
        assertEquals(1f, data.getFloat(0), 0.0001f);
        assertEquals(0.5f, data.getFloat(4), 0.0001f);
        assertEquals(0.25f, data.getFloat(8), 0.0001f);
        assertEquals(1f, data.getFloat(12), 0.0001f);
        assertEquals(0.75f, data.getFloat(16), 0.0001f);
    }

    @Test
    public void laysOutMembersWithStd140Offsets() {
        MatParamUniformBuffer buffer = new MatParamUniformBuffer(shaderWithBlock());

        assertEquals(0, regionStart(buffer, 0));
        assertEquals(15, regionEnd(buffer, 0));
        assertEquals(16, regionStart(buffer, 1));
        assertEquals(19, regionEnd(buffer, 1));
        // float[4] has a std140 stride of 16 bytes.
        assertEquals(32, regionStart(buffer, 2));
        assertEquals(95, regionEnd(buffer, 2));
    }

    @Test
    public void acceptsInstanceNamedBlock() {
        Shader shader = new Shader();
        shader.addSource(Shader.ShaderType.Fragment, "mat-param-instance-test.frag",
                "#version 330\n"
                        + "layout(std140) uniform MatParams {\n"
                        + "    vec4 Color;\n"
                        + "} m_MatParams;\n",
                null, "GLSL330");

        MatParamUniformBuffer buffer = new MatParamUniformBuffer(shader);
        buffer.begin();
        assertTrue(buffer.set(new MatParam(VarType.Vector4, "Color", new ColorRGBA(1f, 0f, 0f, 1f)), false));
        buffer.finish();

        ShaderBufferBlock block = shader.getBufferBlock("MatParams");
        assertSame(buffer.getBufferObject(), block.getBufferObject());
        assertEquals(BufferBindingPoints.MAT_PARAMS_BLOCK_NAME, block.getBufferObject().getName());
    }

    @Test
    public void staysInactiveWithoutASupportedBlock() {
        Shader shader = new Shader();
        shader.addSource(Shader.ShaderType.Fragment, "no-mat-param-test.frag",
                "#version 330\nvoid main() {}\n", null, "GLSL330");

        MatParamUniformBuffer buffer = new MatParamUniformBuffer(shader);
        assertFalse(buffer.isActive());
        buffer.begin();
        assertFalse(buffer.set(new MatParam(VarType.Float, "Roughness", 1f), false));
        buffer.finish();
    }

    @Test
    public void sharesTheParsedBlockBetweenHolders() {
        Shader shader = shaderWithBlock();
        MatParamUniformBuffer first = new MatParamUniformBuffer(shader);
        MatParamUniformBuffer second = new MatParamUniformBuffer(shader);

        assertNotNull(first.getBlock());
        assertSame(first.getBlock(), second.getBlock());
        assertSame(shader.getParamsBlock(), first.getBlock());
    }

    @Test
    public void aReparsedBlockMakesTheHolderStale() {
        Shader shader = shaderWithBlock();
        MatParamUniformBuffer buffer = new MatParamUniformBuffer(shader);
        assertTrue(buffer.isCurrent(shader));

        // Adding a source invalidates the parsed block, so the holder goes stale.
        shader.addSource(Shader.ShaderType.Fragment, "another.frag",
                "#version 330\nvoid main() {}\n", null, "GLSL330");

        assertFalse(buffer.isCurrent(shader));
        assertTrue(new MatParamUniformBuffer(shader).isCurrent(shader));
    }

    @Test
    public void rejectsParametersWithAMismatchedType() {
        MatParamUniformBuffer buffer = new MatParamUniformBuffer(shaderWithBlock());
        buffer.begin();

        assertFalse(buffer.set(new MatParam(VarType.Int, "Roughness", 1), false));
        assertFalse(buffer.set(new MatParam(VarType.FloatArray, "Roughness", new float[]{1f}), false));
        assertTrue(buffer.set(new MatParam(VarType.Float, "Roughness", 1f), false));
    }

    @Test
    public void unchangedValuesDoNotMarkTheBufferForUpdate() {
        Shader shader = shaderWithBlock();
        MatParamUniformBuffer buffer = new MatParamUniformBuffer(shader);

        buffer.begin();
        assertTrue(buffer.set(new MatParam(VarType.Float, "Roughness", 0.75f), false));
        buffer.finish();
        // Simulate the renderer uploading and clearing the flag.
        buffer.getBufferObject().clearUpdateNeeded();

        buffer.begin();
        assertTrue(buffer.set(new MatParam(VarType.Float, "Roughness", 0.75f), false));
        buffer.finish();

        assertFalse(buffer.getBufferObject().isUpdateNeeded());
    }

    @Test
    public void changedValuesMarkTheBufferForUpdate() {
        Shader shader = shaderWithBlock();
        MatParamUniformBuffer buffer = new MatParamUniformBuffer(shader);

        buffer.begin();
        buffer.set(new MatParam(VarType.Float, "Roughness", 0.75f), false);
        buffer.finish();
        buffer.getBufferObject().clearUpdateNeeded();

        buffer.begin();
        buffer.set(new MatParam(VarType.Float, "Roughness", 0.5f), false);
        buffer.finish();

        assertTrue(buffer.getBufferObject().isUpdateNeeded());
        assertEquals(0.5f, buffer.getBufferObject().getByteData().getFloat(16), 0.0001f);
    }

    @Test
    public void overridesKeepOwnershipOfAMember() {
        Shader shader = shaderWithBlock();
        MatParamUniformBuffer buffer = new MatParamUniformBuffer(shader);

        buffer.begin();
        assertTrue(buffer.set(new MatParam(VarType.Float, "Roughness", 0.25f), true));
        assertTrue(buffer.set(new MatParam(VarType.Float, "Roughness", 0.75f), false));
        buffer.finish();

        assertEquals(0.25f, buffer.getBufferObject().getByteData().getFloat(16), 0.0001f);

        // The override releases the member on the next pass.
        buffer.begin();
        assertTrue(buffer.set(new MatParam(VarType.Float, "Roughness", 0.75f), false));
        buffer.finish();

        assertEquals(0.75f, buffer.getBufferObject().getByteData().getFloat(16), 0.0001f);
    }

    @Test
    public void clearingAMemberWritesZero() {
        Shader shader = shaderWithBlock();
        MatParamUniformBuffer buffer = new MatParamUniformBuffer(shader);

        buffer.begin();
        buffer.set(new MatParam(VarType.Float, "Roughness", 0.75f), false);
        buffer.finish();
        assertEquals(0.75f, buffer.getBufferObject().getByteData().getFloat(16), 0.0001f);

        assertTrue(buffer.clear("Roughness", VarType.Float));
        buffer.finish();
        assertEquals(0f, buffer.getBufferObject().getByteData().getFloat(16), 0.0001f);
    }

    @Test
    public void theFirstPassAttachesTheBufferEvenWhenNothingChanges() {
        Shader shader = shaderWithBlock();
        MatParamUniformBuffer buffer = new MatParamUniformBuffer(shader);

        buffer.begin();
        // Roughness already holds zero, so no member is written.
        buffer.set(new MatParam(VarType.Float, "Roughness", 0f), false);
        buffer.finish();

        ShaderBufferBlock block = shader.getBufferBlock(BufferBindingPoints.MAT_PARAMS_BLOCK_NAME);
        assertSame(buffer.getBufferObject(), block.getBufferObject());
    }

    @Test
    public void aClearOutsideAPassIsFlushedByTheNextOne() {
        Shader shader = shaderWithBlock();
        MatParamUniformBuffer buffer = new MatParamUniformBuffer(shader);

        buffer.begin();
        buffer.set(new MatParam(VarType.Float, "Roughness", 0.75f), false);
        buffer.finish();
        assertEquals(0.75f, buffer.getBufferObject().getByteData().getFloat(16), 0.0001f);

        // Material.clearParam() clears the members outside of a pass.
        assertTrue(buffer.clear("Roughness", VarType.Float));

        // The pass that follows must still flush the zero that was asked for.
        buffer.begin();
        buffer.finish();

        assertEquals(0f, buffer.getBufferObject().getByteData().getFloat(16), 0.0001f);
    }

    @Test
    public void clearingIgnoresParametersOutsideTheBlock() {
        MatParamUniformBuffer buffer = new MatParamUniformBuffer(shaderWithBlock());

        assertFalse(buffer.clear("Outside", VarType.Float));
        assertFalse(buffer.clear("Roughness", VarType.Int));
        assertTrue(buffer.clear("Roughness", VarType.Float));
    }

    @Test
    public void clearingAnArrayMemberWritesZero() {
        Shader shader = shaderWithBlock();
        MatParamUniformBuffer buffer = new MatParamUniformBuffer(shader);

        buffer.begin();
        buffer.set(new MatParam(VarType.FloatArray, "Weights", new float[]{1f, 2f, 3f, 4f}), false);
        buffer.finish();
        assertEquals(1f, buffer.getBufferObject().getByteData().getFloat(32), 0.0001f);

        buffer.begin();
        assertTrue(buffer.clear("Weights", VarType.FloatArray));
        buffer.finish();

        ByteBuffer data = buffer.getBufferObject().getByteData();
        assertEquals(0f, data.getFloat(32), 0.0001f);
        assertEquals(0f, data.getFloat(48), 0.0001f);
        assertEquals(0f, data.getFloat(64), 0.0001f);
        assertEquals(0f, data.getFloat(80), 0.0001f);
    }

    @Test
    public void clearingAnAlreadyZeroArrayDoesNotMarkTheBufferForUpdate() {
        Shader shader = shaderWithBlock();
        MatParamUniformBuffer buffer = new MatParamUniformBuffer(shader);

        buffer.begin();
        assertTrue(buffer.clear("Weights", VarType.FloatArray));
        buffer.finish();
        buffer.getBufferObject().clearUpdateNeeded();

        buffer.begin();
        assertTrue(buffer.clear("Weights", VarType.FloatArray));
        buffer.finish();

        assertFalse(buffer.getBufferObject().isUpdateNeeded());
    }

    @Test
    public void shorterSourceIsRewrittenEveryFrame() {
        Shader shader = shaderWithBlock();
        MatParamUniformBuffer buffer = new MatParamUniformBuffer(shader);

        // Only the prefix is copied, as the uniform path does, so the tail keeps
        // its previous value. A source whose length differs from the declared one
        // never compares equal, so it is written again on every update.
        buffer.begin();
        buffer.set(new MatParam(VarType.FloatArray, "Weights", new Float[]{1f, 2f}), false);
        buffer.finish();

        ByteBuffer data = buffer.getBufferObject().getByteData();
        assertEquals(1f, data.getFloat(32), 0.0001f);
        assertEquals(2f, data.getFloat(48), 0.0001f);
        assertEquals(0f, data.getFloat(64), 0.0001f);
        assertEquals(0f, data.getFloat(80), 0.0001f);

        buffer.getBufferObject().clearUpdateNeeded();
        buffer.begin();
        buffer.set(new MatParam(VarType.FloatArray, "Weights", new Float[]{1f, 2f}), false);
        buffer.finish();

        assertTrue(buffer.getBufferObject().isUpdateNeeded());
    }

    @Test
    public void writesTheWholeDeclaredArrayWhenTheSourceMatchesIt() {
        Shader shader = shaderWithBlock();
        MatParamUniformBuffer buffer = new MatParamUniformBuffer(shader);

        buffer.begin();
        assertTrue(buffer.set(new MatParam(VarType.FloatArray, "Weights",
                new Float[]{1f, 2f, 3f, 4f}), false));
        buffer.finish();

        ByteBuffer data = buffer.getBufferObject().getByteData();
        assertEquals(1f, data.getFloat(32), 0.0001f);
        assertEquals(2f, data.getFloat(48), 0.0001f);
        assertEquals(3f, data.getFloat(64), 0.0001f);
        assertEquals(4f, data.getFloat(80), 0.0001f);
    }

    @Test
    public void longerSourceDoesNotOverflowTheDeclaredArray() {
        Shader shader = shaderWithBlock();
        MatParamUniformBuffer buffer = new MatParamUniformBuffer(shader);

        buffer.begin();
        // Extra elements are dropped, the declared backing array must not overflow.
        assertTrue(buffer.set(new MatParam(VarType.FloatArray, "Weights",
                new Float[]{1f, 2f, 3f, 4f, 5f, 6f}), false));
        buffer.finish();

        ByteBuffer data = buffer.getBufferObject().getByteData();
        assertEquals(1f, data.getFloat(32), 0.0001f);
        assertEquals(4f, data.getFloat(80), 0.0001f);
    }

    @Test
    public void acceptsPrimitiveArrayValues() {
        Shader shader = shaderWithBlock();
        MatParamUniformBuffer buffer = new MatParamUniformBuffer(shader);

        buffer.begin();
        assertTrue(buffer.set(new MatParam(VarType.FloatArray, "Weights", new float[]{3f}), false));
        buffer.finish();

        ByteBuffer data = buffer.getBufferObject().getByteData();
        assertEquals(3f, data.getFloat(32), 0.0001f);
        assertEquals(0f, data.getFloat(48), 0.0001f);
    }

    @Test
    public void unchangedArraysDoNotMarkTheBufferForUpdate() {
        Shader shader = shaderWithBlock();
        MatParamUniformBuffer buffer = new MatParamUniformBuffer(shader);

        buffer.begin();
        buffer.set(new MatParam(VarType.FloatArray, "Weights", new float[]{1f, 2f, 3f, 4f}), false);
        buffer.finish();
        buffer.getBufferObject().clearUpdateNeeded();

        buffer.begin();
        assertTrue(buffer.set(new MatParam(VarType.FloatArray, "Weights", new float[]{1f, 2f, 3f, 4f}), false));
        buffer.finish();

        assertFalse(buffer.getBufferObject().isUpdateNeeded());
    }

    @Test
    public void wrapperAndPrimitiveArraysCompareEqual() {
        Shader shader = shaderWithBlock();
        MatParamUniformBuffer buffer = new MatParamUniformBuffer(shader);

        buffer.begin();
        buffer.set(new MatParam(VarType.FloatArray, "Weights", new Float[]{1f, 2f, 3f, 4f}), false);
        buffer.finish();
        buffer.getBufferObject().clearUpdateNeeded();

        // Same content supplied as a primitive array: not a change.
        buffer.begin();
        assertTrue(buffer.set(new MatParam(VarType.FloatArray, "Weights", new float[]{1f, 2f, 3f, 4f}), false));
        buffer.finish();

        assertFalse(buffer.getBufferObject().isUpdateNeeded());
    }

    @Test
    public void changedArraysMarkTheBufferForUpdate() {
        Shader shader = shaderWithBlock();
        MatParamUniformBuffer buffer = new MatParamUniformBuffer(shader);

        buffer.begin();
        buffer.set(new MatParam(VarType.FloatArray, "Weights", new float[]{1f, 2f, 3f, 4f}), false);
        buffer.finish();
        buffer.getBufferObject().clearUpdateNeeded();

        buffer.begin();
        buffer.set(new MatParam(VarType.FloatArray, "Weights", new float[]{1f, 9f, 3f, 4f}), false);
        buffer.finish();

        assertTrue(buffer.getBufferObject().isUpdateNeeded());
        assertEquals(9f, buffer.getBufferObject().getByteData().getFloat(48), 0.0001f);
    }

    @Test
    public void writesVectorAndBooleanMembers() {
        Shader shader = new Shader();
        shader.addSource(Shader.ShaderType.Fragment, "mat-param-kinds.frag",
                "#version 330\n"
                        + "layout(std140) uniform m_MatParams {\n"
                        + "    vec2 Offset;\n"
                        + "    vec3 Scale;\n"
                        + "    bool Enabled;\n"
                        + "};\n",
                null, "GLSL330");

        MatParamUniformBuffer buffer = new MatParamUniformBuffer(shader);
        buffer.begin();
        assertTrue(buffer.set(new MatParam(VarType.Vector2, "Offset", new Vector2f(1f, 2f)), false));
        assertTrue(buffer.set(new MatParam(VarType.Vector3, "Scale", new Vector3f(3f, 4f, 5f)), false));
        assertTrue(buffer.set(new MatParam(VarType.Boolean, "Enabled", Boolean.TRUE), false));
        buffer.finish();

        ByteBuffer data = buffer.getBufferObject().getByteData();
        assertEquals(1f, data.getFloat(0), 0.0001f);
        assertEquals(2f, data.getFloat(4), 0.0001f);
        assertEquals(3f, data.getFloat(16), 0.0001f);
        assertEquals(4f, data.getFloat(20), 0.0001f);
        assertEquals(5f, data.getFloat(24), 0.0001f);
        // A scalar only aligns to its own size, so bool follows the vec3 at 28.
        assertEquals(1, data.getInt(28));
    }

    @Test
    public void colorValuesFillVec4MembersWithoutRewriting() {
        Shader shader = shaderWithBlock();
        MatParamUniformBuffer buffer = new MatParamUniformBuffer(shader);

        buffer.begin();
        assertTrue(buffer.set(new MatParam(VarType.Vector4, "Color", new ColorRGBA(1f, 0.5f, 0.25f, 1f)), false));
        buffer.finish();
        assertEquals(0.5f, buffer.getBufferObject().getByteData().getFloat(4), 0.0001f);
        buffer.getBufferObject().clearUpdateNeeded();

        // ColorRGBA is normalized to Vector4f, so the comparison converges.
        buffer.begin();
        assertTrue(buffer.set(new MatParam(VarType.Vector4, "Color", new ColorRGBA(1f, 0.5f, 0.25f, 1f)), false));
        buffer.finish();

        assertFalse(buffer.getBufferObject().isUpdateNeeded());
    }

    @Test
    public void acceptsVector4Values() {
        Shader shader = new Shader();
        shader.addSource(Shader.ShaderType.Fragment, "mat-param-vector4.frag",
                "#version 330\n"
                        + "layout(std140) uniform m_MatParams {\n"
                        + "    vec4 Value;\n"
                        + "};\n",
                null, "GLSL330");

        MatParamUniformBuffer buffer = new MatParamUniformBuffer(shader);
        buffer.begin();
        assertTrue(buffer.set(new MatParam(VarType.Vector4, "Value", new Vector4f(1f, 2f, 3f, 4f)), false));
        buffer.finish();

        ByteBuffer data = buffer.getBufferObject().getByteData();
        assertEquals(1f, data.getFloat(0), 0.0001f);
        assertEquals(4f, data.getFloat(12), 0.0001f);
    }

    @Test
    public void ignoresBoolArrayMembers() {
        Shader shader = new Shader();
        shader.addSource(Shader.ShaderType.Fragment, "mat-param-bool-array.frag",
                "#version 330\n"
                        + "layout(std140) uniform m_MatParams {\n"
                        + "    bool Flags[2];\n"
                        + "    float Roughness;\n"
                        + "};\n",
                null, "GLSL330");

        MatParamUniformBuffer buffer = new MatParamUniformBuffer(shader);
        assertTrue(buffer.isActive());

        buffer.begin();
        // The block parses, but bool arrays have no matching VarType.
        assertFalse(buffer.set(new MatParam(VarType.Boolean, "Flags", Boolean.TRUE), false));
        assertTrue(buffer.set(new MatParam(VarType.Float, "Roughness", 0.5f), false));
    }

    @Test
    public void exposesTheParsedBlock() {
        MatParamUniformBuffer buffer = new MatParamUniformBuffer(shaderWithBlock());

        ShaderParamsBlock block = buffer.getBlock();
        assertNotNull(block);
        assertEquals(3, block.members.length);
        assertEquals("Weights", block.members[2].paramName);
    }

    private static int regionStart(MatParamUniformBuffer buffer, int index) {
        BufferRegion region = buffer.getBufferObject().getRegion(index);
        return region.getStart();
    }

    private static int regionEnd(MatParamUniformBuffer buffer, int index) {
        BufferRegion region = buffer.getBufferObject().getRegion(index);
        return region.getEnd();
    }

    private static Shader shaderWithBlock() {
        Shader shader = new Shader();
        shader.addSource(Shader.ShaderType.Fragment, "mat-param-test.frag", shaderSource(), null, "GLSL330");
        return shader;
    }

    private static String shaderSource() {
        return "#version 330\n"
                + "layout(std140) uniform m_MatParams {\n"
                + "    vec4 Color;\n"
                + "    float Roughness;\n"
                + "    float Weights[4];\n"
                + "};\n"
                + "void main() {}\n";
    }
}
