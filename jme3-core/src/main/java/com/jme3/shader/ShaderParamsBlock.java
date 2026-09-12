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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.jme3.shader.bufferobject.BufferBindingPoints;

/**
 * Description of the {@code m_MatParams} std140 block declared by a material shader.
 */
public final class ShaderParamsBlock {

    /**
     * GLSL member types that can be packed into the material parameter block.
     */
    public enum Kind {
        FLOAT, INT, BOOL, VEC2, VEC3, VEC4, MAT3, MAT4;

        static Kind fromGlslType(String glslType) {
            switch (glslType) {
                case "float":
                    return FLOAT;
                case "int":
                    return INT;
                case "bool":
                    return BOOL;
                case "vec2":
                    return VEC2;
                case "vec3":
                    return VEC3;
                case "vec4":
                    return VEC4;
                case "mat3":
                    return MAT3;
                case "mat4":
                    return MAT4;
                default:
                    return null;
            }
        }
    }

    /**
     * One declared member of the block.
     */
    public static final class Member {
        /** Declared GLSL name, e.g. {@code m_Diffuse}. */
        public final String glslName;
        /** Material parameter name, i.e. {@link #glslName} without the {@code m_} prefix. */
        public final String paramName;
        /** Declared GLSL type. */
        public final Kind kind;
        /** Declared array length, or 0 for scalar members. */
        public final int arrayLength;
        /**
         * Material parameter type this member accepts, or {@code null} when the
         * member cannot be bound to a material parameter, such as bool arrays.
         */
        public final VarType varType;

        private Member(String glslName, Kind kind, int arrayLength, VarType varType) {
            this.glslName = glslName;
            this.paramName = glslName.startsWith("m_") ? glslName.substring(2) : glslName;
            this.kind = kind;
            this.arrayLength = arrayLength;
            this.varType = varType;
        }
    }

    private static final Pattern COMMENT_PATTERN = Pattern.compile(
            "(?s)/\\*.*?\\*/|//[^\\r\\n\\u0085\\u2028\\u2029]*");
    private static final Pattern BLOCK_PATTERN = Pattern.compile(
            "(?s)(?:layout\\s*\\(([^)]*)\\)\\s*)?uniform\\s+(\\w+)\\s*\\{(.*?)\\}\\s*(\\w+)?\\s*;");
    private static final Pattern MEMBER_PATTERN = Pattern.compile(
            "(?s)(?:layout\\s*\\([^)]*\\)\\s*)?(?:highp\\s+|mediump\\s+|lowp\\s+)?"
                    + "(float|int|bool|vec2|vec3|vec4|mat3|mat4)\\s+(.+)");
    private static final Pattern DECLARATOR_PATTERN = Pattern.compile("(\\w+)\\s*(?:\\[\\s*(\\d+)\\s*\\])?\\s*");

    /** Actual GLSL block name to bind. */
    public final String blockName;
    /** Declared members in declaration order. */
    public final Member[] members;

    private final Map<String, Integer> indexByParamName = new HashMap<>();

    private ShaderParamsBlock(String blockName, ArrayList<Member> members) {
        this.blockName = blockName;
        this.members = members.toArray(new Member[0]);
        for (int i = 0; i < this.members.length; i++) {
            indexByParamName.putIfAbsent(this.members[i].paramName, i);
        }
    }

    /**
     * Finds the position of a member by material parameter name.
     *
     * @param paramName unprefixed material parameter name
     * @return the member index, or -1 if no member matches
     */
    public int indexOf(String paramName) {
        Integer index = indexByParamName.get(paramName);
        return index == null ? -1 : index;
    }

    /**
     * Parses the material parameter block declared by any of the shader
     * sources.
     *
     * @param shader the shader to inspect
     * @return the parsed block, or null if no source declares a supported block
     */
    public static ShaderParamsBlock parse(Shader shader) {
        for (Shader.ShaderSource source : shader.getSources()) {
            ShaderParamsBlock block = parse(source.getSource());
            if (block != null) {
                return block;
            }
        }
        return null;
    }

    /**
     * Parses a GLSL source string for a supported material parameter block.
     *
     * @param source GLSL source code (may be null)
     * @return the parsed block, or null if the source does not declare a
     * supported block
     */
    public static ShaderParamsBlock parse(String source) {
        if (source == null) {
            return null;
        }

        Matcher blockMatcher = BLOCK_PATTERN.matcher(COMMENT_PATTERN.matcher(source).replaceAll(""));
        while (blockMatcher.find()) {
            String layoutQualifier = blockMatcher.group(1);
            String blockName = blockMatcher.group(2);
            String body = blockMatcher.group(3);
            String instanceName = blockMatcher.group(4);

            if (!BufferBindingPoints.MAT_PARAMS_BLOCK_NAME.equals(blockName)
                    && !BufferBindingPoints.MAT_PARAMS_BLOCK_NAME.equals(instanceName)) {
                continue;
            }
            if (layoutQualifier == null || !layoutQualifier.toLowerCase(Locale.ROOT).contains("std140")) {
                continue;
            }

            return parseMembers(blockName, body);
        }
        return null;
    }

    private static ShaderParamsBlock parseMembers(String blockName, String body) {
        if (body.indexOf('#') >= 0) {
            // Preprocessor-dependent declarations cannot be sized reliably from
            // the raw source available here.
            return null;
        }

        ArrayList<Member> members = new ArrayList<>();
        for (String statement : body.split(";")) {
            statement = statement.trim();
            if (statement.isEmpty()) {
                continue;
            }
            if (statement.startsWith("layout")) {
                // Explicit member layouts, such as layout(offset = N), are not
                // interpreted here. Falling back avoids silently using the
                // wrong offset.
                return null;
            }

            Matcher memberMatcher = MEMBER_PATTERN.matcher(statement);
            if (!memberMatcher.matches()) {
                return null;
            }

            Kind kind = Kind.fromGlslType(memberMatcher.group(1));
            if (kind == null) {
                return null;
            }

            String declarations = memberMatcher.group(2);
            for (String declaration : declarations.split(",")) {
                Matcher declaratorMatcher = DECLARATOR_PATTERN.matcher(declaration.trim());
                if (!declaratorMatcher.matches()) {
                    return null;
                }

                String memberName = declaratorMatcher.group(1);
                String arrayLength = declaratorMatcher.group(2);
                int length = arrayLength == null ? 0 : Integer.parseInt(arrayLength);
                members.add(new Member(memberName, kind, length, varTypeOf(kind, length > 0)));
            }
        }

        if (members.isEmpty()) {
            return null;
        }
        return new ShaderParamsBlock(blockName, members);
    }

    private static VarType varTypeOf(Kind kind, boolean array) {
        switch (kind) {
            case FLOAT:
                return array ? VarType.FloatArray : VarType.Float;
            case INT:
                return array ? VarType.IntArray : VarType.Int;
            case BOOL:
                // bool arrays have no matching VarType and stay unbound.
                return array ? null : VarType.Boolean;
            case VEC2:
                return array ? VarType.Vector2Array : VarType.Vector2;
            case VEC3:
                return array ? VarType.Vector3Array : VarType.Vector3;
            case VEC4:
                return array ? VarType.Vector4Array : VarType.Vector4;
            case MAT3:
                return array ? VarType.Matrix3Array : VarType.Matrix3;
            case MAT4:
                return array ? VarType.Matrix4Array : VarType.Matrix4;
            default:
                return null;
        }
    }
}
