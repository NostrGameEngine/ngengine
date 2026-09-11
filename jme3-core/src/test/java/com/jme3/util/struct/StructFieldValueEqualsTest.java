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
package com.jme3.util.struct;

import com.jme3.util.struct.fields.BooleanArrayField;
import com.jme3.util.struct.fields.FloatArrayField;
import com.jme3.util.struct.fields.IntArrayField;
import com.jme3.util.struct.fields.Vector3fArrayField;
import com.jme3.util.struct.fields.Vector3fField;
import com.jme3.util.struct.fields.Vector4fField;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.jme3.math.Vector4f;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class StructFieldValueEqualsTest {

    @Test
    public void floatArrayComparesElementByElement() {
        FloatArrayField field = new FloatArrayField(0, "values", new Float[]{1f, 2f, 3f});

        assertTrue(field.valueEquals(new Float[]{1f, 2f, 3f}));
        assertFalse(field.valueEquals(new Float[]{1f, 2f, 4f}));
    }

    @Test
    public void nullElementCountsAsADifference() {
        FloatArrayField field = new FloatArrayField(0, "values", new Float[]{0f, 2f});

        // Elements are compared as they are, no null coercion.
        assertFalse(field.valueEquals(new Float[]{null, 2f}));
        assertTrue(field.valueEquals(new Float[]{0f, 2f}));
    }

    @Test
    public void intArrayComparesElementByElement() {
        IntArrayField field = new IntArrayField(0, "values", new Integer[]{1, 2, 3});

        assertTrue(field.valueEquals(new Integer[]{1, 2, 3}));
        assertFalse(field.valueEquals(new Integer[]{1, 2, 4}));
    }

    @Test
    public void booleanArrayComparesElementByElement() {
        BooleanArrayField field = new BooleanArrayField(0, "values", new Boolean[]{true, false, true});

        assertTrue(field.valueEquals(new Boolean[]{true, false, true}));
        assertFalse(field.valueEquals(new Boolean[]{true, false, false}));
    }

    @Test
    public void arraysOfDifferentLengthAreNotEqual() {
        FloatArrayField field = new FloatArrayField(0, "values", new Float[]{1f, 2f, 3f});

        assertFalse(field.valueEquals(new Float[]{1f, 2f}));
        assertFalse(field.valueEquals(new Float[]{1f, 2f, 3f, 4f}));
        assertTrue(field.valueEquals(new Float[]{1f, 2f, 3f}));
    }

    @Test
    public void primitiveArrayComparesAgainstTheUnboxedValue() {
        FloatArrayField field = new FloatArrayField(0, "values", new Float[]{1f, 2f, 3f});

        // The raw material value is compared as is, no boxing step needed.
        assertTrue(field.valueEquals(new float[]{1f, 2f, 3f}));
        assertFalse(field.valueEquals(new float[]{1f, 2f, 4f}));
        assertFalse(field.valueEquals(new float[]{1f, 2f}));

        IntArrayField ints = new IntArrayField(0, "values", new Integer[]{1, 2});
        assertTrue(ints.valueEquals(new int[]{1, 2}));
        assertFalse(ints.valueEquals(new int[]{1, 3}));

        BooleanArrayField bools = new BooleanArrayField(0, "values", new Boolean[]{true, false});
        assertTrue(bools.valueEquals(new boolean[]{true, false}));
        assertFalse(bools.valueEquals(new boolean[]{false, false}));
    }

    @Test
    public void colorComparesAgainstAVector4Field() {
        Vector4fField field = new Vector4fField(0, "color", new Vector4f(1f, 0.5f, 0.25f, 1f));

        // ColorRGBA is accepted as is, without building a Vector4f first.
        assertTrue(field.valueEquals(new ColorRGBA(1f, 0.5f, 0.25f, 1f)));
        assertFalse(field.valueEquals(new ColorRGBA(1f, 0.5f, 1f, 1f)));
        assertTrue(field.valueEquals(new Vector4f(1f, 0.5f, 0.25f, 1f)));
    }

    @Test
    public void mismatchedTypesAreNotEqual() {
        FloatArrayField field = new FloatArrayField(0, "values", new Float[]{1f, 2f, 3f});

        assertFalse(field.valueEquals(null));
        assertFalse(field.valueEquals("not an array"));
        assertFalse(field.valueEquals(new int[]{1, 2, 3}));
    }

    @Test
    public void nanComparesEqualToItselfSoTheWriteConverges() {
        // A plain == would report NaN != NaN and rewrite the member on every
        // frame, so both the wrapper and the primitive path use FastMath.compare.
        FloatArrayField floats = new FloatArrayField(0, "values", new Float[]{Float.NaN, 1f});
        assertTrue(floats.valueEquals(new Float[]{Float.NaN, 1f}));
        assertTrue(floats.valueEquals(new float[]{Float.NaN, 1f}));

        Vector4fField color = new Vector4fField(0, "color",
                new Vector4f(Float.NaN, 0f, 0f, 0f));
        assertTrue(color.valueEquals(new ColorRGBA(Float.NaN, 0f, 0f, 0f)));
    }

    @Test
    public void negativeZeroIsDistinctFromZero() {
        // Float.compare orders -0.0f before 0.0f, and the fields follow it so that
        // the stored bits and the comparison agree.
        FloatArrayField field = new FloatArrayField(0, "values", new Float[]{-0f});
        assertFalse(field.valueEquals(new float[]{0f}));
        assertTrue(field.valueEquals(new float[]{-0f}));
    }

    @Test
    public void vectorArrayComparesElementByElement() {
        Vector3fArrayField field = new Vector3fArrayField(0, "points",
                new Vector3f[]{new Vector3f(1f, 2f, 3f), new Vector3f(4f, 5f, 6f)});

        assertTrue(field.valueEquals(new Vector3f[]{new Vector3f(1f, 2f, 3f), new Vector3f(4f, 5f, 6f)}));
        assertFalse(field.valueEquals(new Vector3f[]{new Vector3f(1f, 2f, 3f), new Vector3f(4f, 5f, 7f)}));
    }

    @Test
    public void scalarFieldComparesByValue() {
        Vector3fField field = new Vector3fField(0, "direction", new Vector3f(1f, 2f, 3f));

        assertTrue(field.valueEquals(new Vector3f(1f, 2f, 3f)));
        assertFalse(field.valueEquals(new Vector3f(1f, 2f, 4f)));
        assertFalse(field.valueEquals(null));
    }
}
