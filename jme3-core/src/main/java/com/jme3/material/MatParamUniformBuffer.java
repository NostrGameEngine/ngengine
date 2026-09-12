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
import com.jme3.math.Matrix3f;
import com.jme3.math.Matrix4f;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.jme3.math.Vector4f;
import com.jme3.shader.Shader;
import com.jme3.shader.ShaderBufferBlock;
import com.jme3.shader.ShaderParamsBlock;
import com.jme3.shader.VarType;
import com.jme3.shader.bufferobject.BufferBindingPoints;
import com.jme3.shader.bufferobject.BufferObject;
import com.jme3.shader.bufferobject.layout.Std140Layout;
import com.jme3.util.struct.MutableStructField;
import com.jme3.util.struct.StructField;
import com.jme3.util.struct.StructUtils;
import com.jme3.util.struct.ValueStructField;
import com.jme3.util.struct.fields.BooleanArrayField;
import com.jme3.util.struct.fields.BooleanField;
import com.jme3.util.struct.fields.FloatArrayField;
import com.jme3.util.struct.fields.FloatField;
import com.jme3.util.struct.fields.IntArrayField;
import com.jme3.util.struct.fields.IntField;
import com.jme3.util.struct.fields.Matrix3fArrayField;
import com.jme3.util.struct.fields.Matrix3fField;
import com.jme3.util.struct.fields.Matrix4fArrayField;
import com.jme3.util.struct.fields.Matrix4fField;
import com.jme3.util.struct.fields.Vector2fArrayField;
import com.jme3.util.struct.fields.Vector2fField;
import com.jme3.util.struct.fields.Vector3fArrayField;
import com.jme3.util.struct.fields.Vector3fField;
import com.jme3.util.struct.fields.Vector4fArrayField;
import com.jme3.util.struct.fields.Vector4fField;

import java.util.ArrayList;
import java.util.List;

/**
 * Packs the material parameters of one material into the {@code m_MatParams}
 * UBO declared by one shader.
 */
final class MatParamUniformBuffer {

    private final ShaderParamsBlock block;
    private final List<StructField<?>> fields;
    /** Pass id in which an override owns the member; a stale id means unclaimed. */
    private final int[] claimedPass;
    /**
     * Current update pass, bumped by {@link #begin()}. Starts at 1 so the zeroed
     * {@link #claimedPass} never matches before the first pass.
     */
    private int pass = 1;
    /**
     * True while a field change is waiting to be flushed. Set by any write, also
     * outside a pass, and cleared only once {@link #finish} flushed it.
     */
    private boolean dirty;
    private final BufferObject bufferObject = new BufferObject();
    private final Std140Layout layout;
    private final ShaderBufferBlock shaderBlock;

    /**
     * Creates the holder for one material and one shader.
     *
     * @param shader the shader whose block is bound
     */
    MatParamUniformBuffer(Shader shader) {
        bufferObject.setName(BufferBindingPoints.MAT_PARAMS_BLOCK_NAME);
        bufferObject.setAccessHint(BufferObject.AccessHint.Dynamic);
        bufferObject.setNatureHint(BufferObject.NatureHint.Draw);

        block = shader.getParamsBlock();
        if (block == null) {
            fields = null;
            claimedPass = null;
            layout = null;
            shaderBlock = null;
            return;
        }

        layout = new Std140Layout();
        shaderBlock = shader.getBufferBlock(block.blockName);
        fields = new ArrayList<>(block.members.length);
        claimedPass = new int[block.members.length];
        for (int i = 0; i < block.members.length; i++) {
            fields.add(createField(i, block.members[i]));
        }
        StructUtils.setBufferLayout(fields, layout, bufferObject);
        // The first pass must flush even if every value already matches its field,
        // so that the block gets its buffer object attached.
        dirty = true;
    }

    /**
     * Returns the parsed block backing this holder, or null when the shader
     * declares no supported block.
     *
     * @return the parsed block, or null
     */
    ShaderParamsBlock getBlock() {
        return block;
    }

    /**
     * Tests whether the shader declares a supported material parameter block.
     *
     * @return true if matching parameters are written into the UBO
     */
    boolean isActive() {
        return block != null;
    }

    /**
     * Tests whether this holder still describes the block the shader declares
     * now. Adding a source reparses the block, so a cached holder goes stale and
     * has to be rebuilt.
     *
     * @param shader the shader this holder was built for
     * @return true if this holder matches the shader's current block
     */
    boolean isCurrent(Shader shader) {
        return block == shader.getParamsBlock();
    }

    /**
     * Starts a new update pass, releasing the members claimed by overrides in the
     * previous pass. Claims are tracked by pass id, so this is O(1).
     */
    void begin() {
        if (block == null) {
            return;
        }
        pass++;
    }

    /**
     * Attempts to store a material parameter. The value is written only when it
     * differs from the stored one, so an unchanged parameter does not mark the
     * buffer for upload.
     *
     * @param param the material parameter or override to store
     * @param override true when {@code param} comes from an override list
     * @return true if the parameter belongs to the UBO and was consumed
     */
    boolean set(MatParam param, boolean override) {
        if (block == null || param.getValue() == null) {
            return false;
        }

        // A null member varType never matches, e.g. bool arrays.
        int index = block.indexOf(param.getName());
        if (index < 0 || block.members[index].varType != param.getVarType()) {
            return false;
        }

        if (override) {
            claimedPass[index] = pass;
        } else if (claimedPass[index] == pass) {
            // Overrides win when both target the same member.
            return true;
        }

        // The field compares the raw value itself, so an unchanged parameter
        // costs no conversion and no allocation.
        StructField<?> field = fields.get(index);
        ShaderParamsBlock.Member member = block.members[index];
        Object raw = param.getValue();
        if (!field.valueEquals(raw)) {
            writeField(field, member, toFieldType(member, raw));
            dirty = true;
        }
        return true;
    }

    /**
     * Clears a UBO-backed parameter, resetting its member to zero.
     *
     * @param paramName unprefixed material parameter name
     * @param varType material parameter type
     * @return true if the parameter belongs to the UBO and was consumed
     */
    boolean clear(String paramName, VarType varType) {
        if (block == null) {
            return false;
        }

        // A null member varType never matches, e.g. bool arrays.
        int index = block.indexOf(paramName);
        if (index < 0 || block.members[index].varType != varType) {
            return false;
        }

        claimedPass[index] = pass;
        StructField<?> field = fields.get(index);
        field.setToZero();
        // setToZero only marks a field whose value actually changed.
        dirty |= field.isUpdateNeeded();
        return true;
    }

    /**
     * Writes the pending updates into the backing buffer object and attaches it
     * to the shader block. Skipped while nothing is pending, so a pass that
     * changes no parameter costs a single flag check.
     */
    void finish() {
        if (block == null || !dirty) {
            return;
        }

        StructUtils.updateBufferData(fields, false, layout, bufferObject);
        shaderBlock.setBufferObject(ShaderBufferBlock.BufferType.UniformBufferObject, bufferObject);
        dirty = false;
    }

    /**
     * Returns the backing buffer object.
     *
     * @return the UBO populated by this holder
     */
    BufferObject getBufferObject() {
        return bufferObject;
    }

    private static StructField<?> createField(int position, ShaderParamsBlock.Member member) {
        boolean array = member.arrayLength > 0;
        switch (member.kind) {
            case FLOAT:
                return array ? new FloatArrayField(position, member.glslName, member.arrayLength)
                        : new FloatField(position, member.glslName, 0f);
            case INT:
                return array ? new IntArrayField(position, member.glslName, member.arrayLength)
                        : new IntField(position, member.glslName, 0);
            case BOOL:
                return array ? new BooleanArrayField(position, member.glslName, member.arrayLength)
                        : new BooleanField(position, member.glslName, Boolean.FALSE);
            case VEC2:
                return array ? new Vector2fArrayField(position, member.glslName, member.arrayLength)
                        : new Vector2fField(position, member.glslName, new Vector2f());
            case VEC3:
                return array ? new Vector3fArrayField(position, member.glslName, member.arrayLength)
                        : new Vector3fField(position, member.glslName, new Vector3f());
            case VEC4:
                return array ? new Vector4fArrayField(position, member.glslName, member.arrayLength)
                        : new Vector4fField(position, member.glslName, new Vector4f());
            case MAT3:
                return array ? new Matrix3fArrayField(position, member.glslName, member.arrayLength)
                        : new Matrix3fField(position, member.glslName, new Matrix3f());
            case MAT4:
                return array ? new Matrix4fArrayField(position, member.glslName, member.arrayLength)
                        : new Matrix4fField(position, member.glslName, new Matrix4f());
            default:
                throw new IllegalArgumentException("Unsupported member kind " + member.kind);
        }
    }

    /**
     * Converts a material value to the type stored by the matching field.
     * <p>
     * {@link VarType#FloatArray}, {@link VarType#IntArray} and bool arrays accept
     * both a primitive and a wrapper array while the fields hold a wrapper array,
     * so primitive sources are boxed here. {@link VarType#Vector4} also accepts a
     * {@link ColorRGBA} while the field holds a {@link Vector4f}.
     * <p>
     * This allocates only when the value is actually written, see {@link #set}.
     *
     * @param member the member the value belongs to
     * @param value the value supplied by the material
     * @return the value expressed in the field's own type
     */
    private static Object toFieldType(ShaderParamsBlock.Member member, Object value) {
        if (member.arrayLength == 0) {
            if (member.kind == ShaderParamsBlock.Kind.VEC4 && value instanceof ColorRGBA) {
                ColorRGBA color = (ColorRGBA) value;
                return new Vector4f(color.r, color.g, color.b, color.a);
            }
            return value;
        }
        if (value instanceof float[]) {
            float[] source = (float[]) value;
            Float[] boxed = new Float[source.length];
            for (int i = 0; i < source.length; i++) {
                boxed[i] = source[i];
            }
            return boxed;
        }
        if (value instanceof int[]) {
            int[] source = (int[]) value;
            Integer[] boxed = new Integer[source.length];
            for (int i = 0; i < source.length; i++) {
                boxed[i] = source[i];
            }
            return boxed;
        }
        if (value instanceof boolean[]) {
            boolean[] source = (boolean[]) value;
            Boolean[] boxed = new Boolean[source.length];
            for (int i = 0; i < source.length; i++) {
                boxed[i] = source[i];
            }
            return boxed;
        }
        return value;
    }

    /**
     * Writes a value into the field. The value must already be in the field's own
     * type, see {@link #toFieldType}.
     * <p>
     * Array fields keep a backing array as long as the declared GLSL array, so
     * the value is copied into it. Scalars either replace their value or are
     * mutated in place, depending on the field type.
     */
    @SuppressWarnings("unchecked")
    private static void writeField(StructField<?> field, ShaderParamsBlock.Member member, Object value) {
        if (member.arrayLength > 0) {
            Object[] target = (Object[]) ((MutableStructField<Object[]>) field).getValueForUpdate();
            Object[] source = (Object[]) value;
            System.arraycopy(source, 0, target, 0, Math.min(source.length, target.length));
            return;
        }
        switch (member.kind) {
            case FLOAT:
            case INT:
            case BOOL:
                ((ValueStructField<Object>) field).setValue(value);
                return;
            case VEC2:
                ((MutableStructField<Vector2f>) field).getValueForUpdate().set((Vector2f) value);
                return;
            case VEC3:
                ((MutableStructField<Vector3f>) field).getValueForUpdate().set((Vector3f) value);
                return;
            case VEC4:
                ((MutableStructField<Vector4f>) field).getValueForUpdate().set((Vector4f) value);
                return;
            case MAT3:
                ((MutableStructField<Matrix3f>) field).getValueForUpdate().set((Matrix3f) value);
                return;
            case MAT4:
                ((MutableStructField<Matrix4f>) field).getValueForUpdate().set((Matrix4f) value);
                return;
            default:
                throw new IllegalArgumentException("Unsupported member kind " + member.kind);
        }
    }
}
