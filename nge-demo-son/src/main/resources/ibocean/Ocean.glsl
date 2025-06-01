#ifndef OCEAN_GLSL
#define OCEAN_GLSL

// #define DRAG_MULT 0.38
// #define WAVE_ITERATIONS 12
// #define NORMAL_ITERATIONS 24

// Enhanced wave function for sharper peaks
// vec2 wavedx(vec2 position, vec2 direction, float frequency, float timeshift, float sharpness) {
//     float x = dot(direction, position) * frequency + timeshift;
//     // Add asymmetry to create sharper peaks but smoother troughs
//     float wave = exp(sin(x) - 1.0);
//     // Enhance the peaks by adding the cube of the wave (creates sharper peaks)
//     wave = mix(wave, pow(wave, 3.0), sharpness);
//     float dx = wave * cos(x);
//     return vec2(wave, -dx);
// }

// // Generate waves using octave-based summation with position warping
// float getwaves(vec2 position, int iterations, float time, float peakiness) {
//     // Position-dependent phase shift prevents octave alignment
//     float wavePhaseShift = length(position) * 0.1;
    
//     // Wave parameters that evolve with each iteration
//     float iter = 0.0;
//     float frequency = 1.0;
//     float timeMultiplier = 2.0;
//     float weight = 1.0;
    
//     // Accumulators for weighted sum
//     float sumOfValues = 0.0;
//     float sumOfWeights = 0.0;
    
//     for(int i=0; i < iterations; i++) {
//         // Create semi-random wave direction based on iteration
//         vec2 direction = vec2(sin(iter), cos(iter));
        
//         // Calculate wave height and derivative - higher peakiness for first octaves
//         float octaveSharpness = peakiness * max(0.0, 1.0 - float(i) / 4.0);
//         vec2 res = wavedx(position, direction, frequency, time * timeMultiplier + wavePhaseShift, octaveSharpness);
        
//         // Critical: Shift position based on wave's derivative (creates fluid interaction)
//         position += direction * res.y * weight * DRAG_MULT;
        
//         // Add to weighted sum
//         sumOfValues += res.x * weight;
//         sumOfWeights += weight;
        
//         // Prepare for next octave with reduced influence
//         weight = mix(weight, 0.0, 0.2);
//         frequency *= 1.18;
//         timeMultiplier *= 1.07;
        
//         // Add a large pseudo-random value to create variation in octave directions
//         iter += 1232.399963;
//     }
    
//     // Return normalized sum
//     return sumOfValues / sumOfWeights;
// }

// // Calculate normal with enhanced contrast
// vec3 calculate_normal(vec2 pos, float epsilon, float time, vec2 wind, float peakiness) {
//     float normalStrength = mix(0.3, 0.7, peakiness);
    
//     // Sample heights using central difference approach
//     float height_x0 = getwaves(pos + vec2(-epsilon, 0.0), NORMAL_ITERATIONS, time, peakiness);
//     float height_x1 = getwaves(pos + vec2(epsilon, 0.0), NORMAL_ITERATIONS, time, peakiness);
//     float height_z0 = getwaves(pos + vec2(0.0, -epsilon), NORMAL_ITERATIONS, time, peakiness);
//     float height_z1 = getwaves(pos + vec2(0.0, epsilon), NORMAL_ITERATIONS, time, peakiness);
    
//     // Calculate slopes with central differences for better accuracy
//     float slopeX = (height_x1 - height_x0) * normalStrength / (2.0 * epsilon);
//     float slopeZ = (height_z1 - height_z0) * normalStrength / (2.0 * epsilon);
    
//     // Properly construct the normal vector (tangent plane perpendicular)
//     vec3 normal = normalize(vec3(-slopeX, 1.0, -slopeZ));
    
//     return normal;
// }

// // Helper function to get basic ocean sample
// vec4 sampleOceanBasic(vec3 wpos, vec3 inWind, vec3 scale, float time) {
//     // Get wind magnitude and direction
//     vec2 wind = vec2(inWind.x, inWind.z);
//     float windMagnitude = length(wind);
    
//     // Create a stable coordinate system that doesn't degenerate far from origin
//     const float DOMAIN_SIZE = 512.0;
//     vec2 stablePos = mod(wpos.xz, DOMAIN_SIZE);
    
//     // Calculate scaled position for wave generation
//     vec2 wavePos = stablePos * scale.xz * 0.1;
    
//     // Apply spatial variation
//     float spatialVar = sin(stablePos.x * 0.004 + stablePos.y * 0.005) * 0.1 + 0.9;
//     float scaleVariation = 1.0 + 
//                           sin(stablePos.x * 0.016 + stablePos.y * 0.02) * sin(stablePos.y * 0.018) * 0.05 + 
//                           sin(stablePos.x * 0.04 + stablePos.y * 0.03) * 0.02;
                          
//     // Create position sets for different wave scales
//     vec2 largeWavePos = wavePos * 0.3 * spatialVar;
//     vec2 mediumWavePos = wavePos * 1.1 * scaleVariation;
//     vec2 smallWavePos = wavePos * 3.7 * scaleVariation;
    
//     // Apply wind influence
//     float windFactor = clamp(windMagnitude * 0.01, 0.3, 1.0);
//     float timeScale = 0.6 * windFactor;
//     float peakiness = clamp(windMagnitude * 0.005, 0.1, 0.5);
    
//     // Generate waves
//     float largeWaves = getwaves(largeWavePos, WAVE_ITERATIONS, time * timeScale * 0.3, peakiness);
//     float medWaves = getwaves(mediumWavePos, WAVE_ITERATIONS, time * timeScale * 0.7, peakiness * 0.7);
//     float smallWaves = getwaves(smallWavePos, min(WAVE_ITERATIONS, 5), time * timeScale * 1.2, peakiness * 0.4);
    
//     // Calculate weights
//     float largeWeight = 0.6 + windFactor * 0.2;
//     float medWeight = 0.3 - windFactor * 0.1;
//     float smallWeight = 0.1 - windFactor * 0.05;
    
//     // Normalize weights
//     float totalWeight = largeWeight + medWeight + smallWeight;
//     largeWeight /= totalWeight;
//     medWeight /= totalWeight;
//     smallWeight /= totalWeight;
    
//     // Combine wave heights
//     float finalHeight = 
//         largeWaves * largeWeight + 
//         medWaves * medWeight + 
//         smallWaves * smallWeight;
    
//     // Apply wave height scaling
//     finalHeight = pow(finalHeight, 0.8) * 0.5 + 0.5;
//     finalHeight = clamp(finalHeight, 0.0, 1.0);
    
//     // Apply wind-based height scaling
//     float heightScale = 0.7 + clamp(windMagnitude * 0.015, 0.0, 0.8);
    
//     // Calculate normal
//     vec3 normal = calculate_normal(wavePos, 0.2, time * timeScale, wind, peakiness);
    
//     // Return height and normal
//     return vec4(normal, finalHeight * scale.y * heightScale);
// }

// // Main entry point with your specified signature
// vec4 sampleOcean(
//     in vec3 wpos,
//     in vec3 inWind,
//     in vec3 scale,
//     in float time
// ) {
//     // Get distance to domain boundaries to detect seam areas
//     const float DOMAIN_SIZE = 512.0;
//     vec2 normalizedPos = mod(wpos.xz, DOMAIN_SIZE) / DOMAIN_SIZE;
//     vec2 distToBoundary = min(normalizedPos, 1.0 - normalizedPos);
//     float boundaryDist = min(distToBoundary.x, distToBoundary.y);
    
//     // Multi-sampling parameters (more samples near boundaries)
//     int numSamples = boundaryDist < 0.1 ? 9 : 1;
//     float sampleRadius = boundaryDist < 0.1 ? (0.1 - boundaryDist) * 60.0 : 0.0;
    
//     // Sample multiple times near boundaries and average the results
//     vec3 totalNormal = vec3(0.0);
//     float totalHeight = 0.0;
    
//     if (numSamples == 1) {
//         // Far from boundary - just use the regular sample
//         vec4 sample = sampleOceanBasic(wpos, inWind, scale, time);
//         return sample;
//     } else {
//         // Near boundary - use multi-sampling to blur across the seam
//         for (int i = 0; i < numSamples; i++) {
//             // Generate sample offset using golden ratio for good distribution
//             float angle = float(i) * 2.399963; // Golden angle in radians
//             float radius = sqrt(float(i) / float(numSamples-1)) * sampleRadius;
//             vec2 offset = vec2(cos(angle), sin(angle)) * radius;
            
//             // Sample with offset
//             vec4 sample = sampleOceanBasic(
//                 wpos + vec3(offset.x, 0.0, offset.y),
//                 inWind, 
//                 scale, 
//                 time
//             );
            
//             // Accumulate results
//             totalNormal += sample.xyz;
//             totalHeight += sample.w;
//         }
        
//         // Average the results
//         totalNormal = normalize(totalNormal);
//         totalHeight /= float(numSamples);
        
//         return vec4(totalNormal, totalHeight);
//     }
// }








vec4 sampleIBOcean(
    in vec3 wpos,
    in vec2 offsets[NUM_LAYERS],
    in vec3 baseScale,
    in vec3 scale,
    in vec2 tileSize,
    in sampler2DArray oceanMap
) {

  
    float height = 0.0;
    vec3 wNormal = vec3(0.0);
    for(int i=0;i<NUM_LAYERS;i++){
        vec2 uv = wpos.xz;
        uv += offsets[i].xy;
        uv *= scale.xz * baseScale.xz;
        uv.x = mod(uv.x, tileSize.x);
        uv.y = mod(uv.y, tileSize.y);
        uv.x /= tileSize.x;
        uv.y /= tileSize.y;
        vec4 band = texture(oceanMap, vec3(uv, i));
        float bandHeight = band.a;
        vec3 bandNormal = band.xyz * 2.0 - 1.0; 

        height += bandHeight * scale.y * baseScale.y ;
        wNormal += bandNormal ;
    }
    return vec4(normalize(wNormal), height/NUM_LAYERS);
}
#endif