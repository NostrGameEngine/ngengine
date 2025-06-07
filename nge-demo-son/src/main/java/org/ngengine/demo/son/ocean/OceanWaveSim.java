/**
 * Copyright (c) 2025, Nostr Game Engine
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
 * the BSD 3-Clause License. The original jMonkeyEngine license is as follows:
 */
package org.ngengine.demo.son.ocean;

import com.jme3.math.FastMath;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.jme3.math.Vector4f;

/**
 * Ocean wave simulation that matches the GLSL shader implementation. Generates realistic ocean wave heights
 * and normals with multi-sampling approach to prevent tiling artifacts at domain boundaries.
 */
public class OceanWaveSim {

    private static final float DRAG_MULT = 0.38f;
    private static final int WAVE_ITERATIONS = 12;
    private static final int NORMAL_ITERATIONS = 24;

    /**
     * Enhanced wave function for sharper peaks.
     *
     * @param position
     *            Position in wave space
     * @param direction
     *            Wave direction vector
     * @param frequency
     *            Wave frequency
     * @param timeshift
     *            Time offset
     * @param sharpness
     *            Controls peak sharpness
     * @return Vector2f with (wave height, wave derivative)
     */
    private static Vector2f wavedx(Vector2f position, Vector2f direction, float frequency, float timeshift, float sharpness) {
        float x = direction.dot(position) * frequency + timeshift;
        // Add asymmetry to create sharper peaks but smoother troughs
        float wave = (float) Math.exp(FastMath.sin(x) - 1.0f);
        // Enhance the peaks by adding the cube of the wave (creates sharper peaks)
        wave = FastMath.interpolateLinear(sharpness, wave, wave * wave * wave);
        float dx = wave * FastMath.cos(x);
        return new Vector2f(wave, -dx);
    }

    /**
     * Generate waves using octave-based summation with position warping.
     *
     * @param position
     *            Position in wave space
     * @param iterations
     *            Number of wave iterations to compute
     * @param time
     *            Current animation time
     * @param peakiness
     *            Controls the sharpness of wave peaks
     * @return Normalized wave height at the given position
     */
    private static float getWaves(Vector2f position, int iterations, float time, float peakiness) {
        // Position-dependent phase shift prevents octave alignment
        float wavePhaseShift = position.length() * 0.1f;

        // Wave parameters that evolve with each iteration
        float iter = 0.0f;
        float frequency = 1.0f;
        float timeMultiplier = 2.0f;
        float weight = 1.0f;

        // Accumulators for weighted sum
        float sumOfValues = 0.0f;
        float sumOfWeights = 0.0f;

        Vector2f pos = new Vector2f(position);

        for (int i = 0; i < iterations; i++) {
            // Create semi-random wave direction based on iteration
            Vector2f direction = new Vector2f(FastMath.sin(iter), FastMath.cos(iter));

            // Calculate wave height and derivative - higher peakiness for first octaves
            float octaveSharpness = peakiness * Math.max(0.0f, 1.0f - (float) i / 4.0f);
            Vector2f res = wavedx(pos, direction, frequency, time * timeMultiplier + wavePhaseShift, octaveSharpness);

            // Critical: Shift position based on wave's derivative (creates fluid interaction)
            pos.addLocal(direction.mult(res.y * weight * DRAG_MULT));

            // Add to weighted sum
            sumOfValues += res.x * weight;
            sumOfWeights += weight;

            // Prepare for next octave with reduced influence
            weight = FastMath.interpolateLinear(0.2f, weight, 0.0f);
            frequency *= 1.18f;
            timeMultiplier *= 1.07f;

            // Add a large pseudo-random value to create variation in octave directions
            iter += 1232.399963f;
        }

        // Return normalized sum
        return sumOfValues / sumOfWeights;
    }

    /**
     * Calculate normal
     *
     * @param pos
     *            Position in wave space
     * @param epsilon
     *            Distance for derivative sampling
     * @param time
     *            Current animation time
     * @param wind
     *            Wind vector (direction and magnitude)
     * @param peakiness
     *            Controls the sharpness of wave peaks
     * @return Normal vector at the given position
     */
    private static Vector3f calculateNormal(
        Vector2f pos,
        float epsilon,
        float time,
        Vector2f wind,
        float peakiness,
        Vector3f scale
    ) {
        float normalStrength = FastMath.interpolateLinear(peakiness, 0.3f, 0.7f);

        // Calculate world-space epsilon based on actual world scale
        float worldEpsilon = epsilon / Math.max(scale.x, scale.z);

        // Sample heights using central difference approach
        Vector2f xMinusPos = new Vector2f(pos).addLocal(-worldEpsilon, 0.0f);
        Vector2f xPlusPos = new Vector2f(pos).addLocal(worldEpsilon, 0.0f);
        Vector2f zMinusPos = new Vector2f(pos).addLocal(0.0f, -worldEpsilon);
        Vector2f zPlusPos = new Vector2f(pos).addLocal(0.0f, worldEpsilon);

        float height_x0 = getWaves(xMinusPos, NORMAL_ITERATIONS, time, peakiness);
        float height_x1 = getWaves(xPlusPos, NORMAL_ITERATIONS, time, peakiness);
        float height_z0 = getWaves(zMinusPos, NORMAL_ITERATIONS, time, peakiness);
        float height_z1 = getWaves(zPlusPos, NORMAL_ITERATIONS, time, peakiness);

        // Calculate gradients in world space
        float worldSpacing = 2.0f * worldEpsilon * scale.x; // Convert back to world units
        float slopeX = (height_x1 - height_x0) * normalStrength * scale.y / worldSpacing;
        float slopeZ = (height_z1 - height_z0) * normalStrength * scale.y / worldSpacing;

        // Construct world space normal
        Vector3f normal = new Vector3f(-slopeX, 1.0f, -slopeZ);
        normal.normalizeLocal();

        return normal;
    }

    /**
     * Helper function to get basic ocean sample without boundary handling.
     *
     * @param wpos
     *            World position to sample
     * @param inWind
     *            Wind vector (direction and magnitude in world space)
     * @param scale
     *            Scale factors for X, Y, Z dimensions
     * @param time
     *            Current animation time
     * @return Vector4f with normal (xyz) and height (w)
     */
    private static Vector4f sampleOceanBasic(float DOMAIN_SIZE, Vector3f wpos, Vector3f inWind, Vector3f scale, float time) {
        // Get wind magnitude and direction
        Vector2f wind = new Vector2f(inWind.x, inWind.z);
        float windMagnitude = wind.length();

        // Create a stable coordinate system that doesn't degenerate far from origin
        Vector2f stablePos = new Vector2f(wpos.x % DOMAIN_SIZE, wpos.z % DOMAIN_SIZE);
        if (stablePos.x < 0) stablePos.x += DOMAIN_SIZE;
        if (stablePos.y < 0) stablePos.y += DOMAIN_SIZE;

        // Calculate scaled position for wave generation
        Vector2f wavePos = new Vector2f(stablePos).multLocal(scale.x, scale.z).multLocal(0.1f);

        // Apply spatial variation
        float spatialVar = FastMath.sin(stablePos.x * 0.004f + stablePos.y * 0.005f) * 0.1f + 0.9f;
        float scaleVariation =
            1.0f +
            FastMath.sin(stablePos.x * 0.016f + stablePos.y * 0.02f) *
            FastMath.sin(stablePos.y * 0.018f) *
            0.05f +
            FastMath.sin(stablePos.x * 0.04f + stablePos.y * 0.03f) *
            0.02f;

        // Create position sets for different wave scales
        Vector2f largeWavePos = new Vector2f(wavePos).multLocal(0.3f * spatialVar);
        Vector2f mediumWavePos = new Vector2f(wavePos).multLocal(1.1f * scaleVariation);
        Vector2f smallWavePos = new Vector2f(wavePos).multLocal(3.7f * scaleVariation);

        // Apply wind influence
        float windFactor = FastMath.clamp(windMagnitude * 0.01f, 0.3f, 1.0f);
        float timeScale = 0.6f * windFactor;
        float peakiness = FastMath.clamp(windMagnitude * 0.005f, 0.1f, 0.5f);

        // Generate waves
        float largeWaves = getWaves(largeWavePos, WAVE_ITERATIONS, time * timeScale * 0.3f, peakiness);
        float medWaves = getWaves(mediumWavePos, WAVE_ITERATIONS, time * timeScale * 0.7f, peakiness * 0.7f);
        float smallWaves = getWaves(smallWavePos, Math.min(WAVE_ITERATIONS, 5), time * timeScale * 1.2f, peakiness * 0.4f);

        // Calculate weights
        float largeWeight = 0.6f + windFactor * 0.2f;
        float medWeight = 0.3f - windFactor * 0.1f;
        float smallWeight = 0.1f - windFactor * 0.05f;

        // Normalize weights
        float totalWeight = largeWeight + medWeight + smallWeight;
        largeWeight /= totalWeight;
        medWeight /= totalWeight;
        smallWeight /= totalWeight;

        // Combine wave heights
        float finalHeight = largeWaves * largeWeight + medWaves * medWeight + smallWaves * smallWeight;

        // Apply wave height scaling
        finalHeight = FastMath.pow(finalHeight, 0.8f) * 0.5f + 0.5f;
        finalHeight = FastMath.clamp(finalHeight, 0.0f, 1.0f);

        // Calculate normal
        Vector3f normal = calculateNormal(wavePos, 0.2f, time * timeScale, wind, peakiness, scale);

        // Return height and normal
        return new Vector4f(normal.x, normal.y, normal.z, finalHeight);
    }

    /**
     * Sample ocean height and normal at a given world position using multi-sampling approach at domain
     * boundaries.
     *
     * @param wpos
     *            World position to sample
     * @param inWind
     *            Wind vector (direction and magnitude in world space)
     * @param scale
     *            Scale factors for X, Y, Z dimensions
     * @param time
     *            Current animation time
     * @return Vector4f with normal (xyz) and height (w)
     */
    public static Vector4f sampleOcean(float DOMAIN_SIZE, Vector3f wpos, Vector3f inWind, Vector3f scale, float time) {
        // Get distance to domain boundaries to detect seam areas
        Vector2f normalizedPos = new Vector2f(wpos.x % DOMAIN_SIZE, wpos.z % DOMAIN_SIZE);
        if (normalizedPos.x < 0) normalizedPos.x += DOMAIN_SIZE;
        if (normalizedPos.y < 0) normalizedPos.y += DOMAIN_SIZE;
        normalizedPos.divideLocal(DOMAIN_SIZE);

        Vector2f distToBoundary = new Vector2f(
            Math.min(normalizedPos.x, 1.0f - normalizedPos.x),
            Math.min(normalizedPos.y, 1.0f - normalizedPos.y)
        );
        float boundaryDist = Math.min(distToBoundary.x, distToBoundary.y);

        // Multi-sampling parameters (more samples near boundaries)
        int numSamples = boundaryDist < 0.1f ? 9 : 1;
        float sampleRadius = boundaryDist < 0.1f ? (0.1f - boundaryDist) * 60.0f : 0.0f;

        // Far from boundary - just use the regular sample
        if (numSamples == 1) {
            return sampleOceanBasic(DOMAIN_SIZE, wpos, inWind, scale, time);
        }
        // Near boundary - use multi-sampling to blur across the seam
        else {
            Vector3f totalNormal = new Vector3f();
            float totalHeight = 0.0f;

            for (int i = 0; i < numSamples; i++) {
                // Generate sample offset using golden ratio for good distribution
                float angle = (float) i * 2.399963f; // Golden angle in radians
                float radius = FastMath.sqrt((float) i / (float) (numSamples - 1)) * sampleRadius;
                Vector2f offset = new Vector2f(FastMath.cos(angle) * radius, FastMath.sin(angle) * radius);

                // Sample with offset
                Vector3f offsetPos = new Vector3f(wpos).addLocal(offset.x, 0.0f, offset.y);
                Vector4f sample = sampleOceanBasic(DOMAIN_SIZE, offsetPos, inWind, scale, time);

                // Accumulate results
                totalNormal.addLocal(sample.x, sample.y, sample.z);
                totalHeight += sample.w;
            }

            // Average the results
            totalNormal.normalizeLocal();
            totalHeight /= (float) numSamples;

            return new Vector4f(totalNormal.x, totalNormal.y, totalNormal.z, totalHeight);
        }
    }
}
