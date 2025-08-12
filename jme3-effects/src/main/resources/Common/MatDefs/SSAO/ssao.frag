#import "Common/ShaderLib/GLSLCompat.glsllib"
#import "Common/ShaderLib/MultiSample.glsllib"

uniform vec2 g_Resolution;
uniform vec2 g_ResolutionInverse;
uniform vec2 m_FrustumNearFar;
uniform COLORTEXTURE m_Texture;
uniform sampler2D m_Normals;
uniform sampler2D m_RandomMap;
uniform DEPTHTEXTURE m_DepthTexture;
uniform vec3 m_FrustumCorner;
uniform float m_SampleRadius;
uniform float m_Intensity;
uniform float m_Scale;
uniform float m_Bias;
uniform bool m_UseOnlyAo;
uniform bool m_UseAo;
uniform vec2[4] m_Samples;

varying vec2 texCoord;

vec3 getPosition(float depthv, in vec2 uv){
  //Reconstruction from depth
  float depth = (2.0 * m_FrustumNearFar.x) / (m_FrustumNearFar.y + m_FrustumNearFar.x - depthv * (m_FrustumNearFar.y-m_FrustumNearFar.x));

  //one frustum corner method
  float x = mix(-m_FrustumCorner.x, m_FrustumCorner.x, uv.x);
  float y = mix(-m_FrustumCorner.y, m_FrustumCorner.y, uv.y);

  return depth * vec3(x, y, m_FrustumCorner.z);
}

vec3 approximateNormal(in vec3 pos,in vec2 texCoord){
    float step = g_ResolutionInverse.x;
    float stepy = g_ResolutionInverse.y;
    float depth2 = getDepth(m_DepthTexture,texCoord + vec2(step,-stepy)).r;
    float depth3 = getDepth(m_DepthTexture,texCoord + vec2(-step,-stepy)).r;
    vec3 pos2 = vec3(getPosition(depth2,texCoord + vec2(step,-stepy)));
    vec3 pos3 = vec3(getPosition(depth3,texCoord + vec2(-step,-stepy)));

    vec3 v1 = (pos - pos2).xyz;
    vec3 v2 = (pos3 - pos2).xyz;
    return normalize(cross(-v1, v2));
}

vec3 getNormal(in vec2 uv){
  return normalize(texture2D(m_Normals, uv).xyz * 2.0 - 1.0);
}

vec2 getRandom(in vec2 uv){  
   vec4 rand = texture2D(m_RandomMap,g_Resolution * uv / 128.0 * 3.0)*2.0 -1.0;
   return normalize(rand.xy);
}

// NEW: Detect depth edges to reduce halos
float getDepthEdge(in vec2 uv) {
    // Sample depth at current pixel and neighbors
    float center = getDepth(m_DepthTexture, uv).r;
    float right = getDepth(m_DepthTexture, uv + vec2(g_ResolutionInverse.x, 0.0)).r;
    float bottom = getDepth(m_DepthTexture, uv + vec2(0.0, g_ResolutionInverse.y)).r;
    
    // Convert depths to linear space for better comparison
    float centerPos = (2.0 * m_FrustumNearFar.x) / (m_FrustumNearFar.y + m_FrustumNearFar.x - center * (m_FrustumNearFar.y-m_FrustumNearFar.x));
    float rightPos = (2.0 * m_FrustumNearFar.x) / (m_FrustumNearFar.y + m_FrustumNearFar.x - right * (m_FrustumNearFar.y-m_FrustumNearFar.x));
    float bottomPos = (2.0 * m_FrustumNearFar.x) / (m_FrustumNearFar.y + m_FrustumNearFar.x - bottom * (m_FrustumNearFar.y-m_FrustumNearFar.x));
    
    // Compute depth differences - adjust threshold based on scene scale
    float depthThreshold = centerPos * 0.1; // 10% of current depth is a good starting point
    float edgeFactorX = abs(centerPos - rightPos) / depthThreshold;
    float edgeFactorY = abs(centerPos - bottomPos) / depthThreshold;
    
    // Edge factor: 0 at strong depth edges, 1 elsewhere
    float edge = 1.0 - clamp(max(edgeFactorX, edgeFactorY), 0.0, 1.0);
    
    // Smooth the transition
    return smoothstep(0.0, 0.5, edge);
}

// IMPROVED: Depth-aware ambient occlusion calculation
float doAmbientOcclusion(in vec2 tc, in vec3 pos, in vec3 norm) {
    vec2 clampedTexCoord = clamp(tc, 0.0, 1.0); // Clamp texture coordinates
    float depthv = getDepth(m_DepthTexture, clampedTexCoord).r;

    if (depthv == 1.0) {
        return 0.0; // Skip invalid depth
    }

    vec3 samplePos = getPosition(depthv, clampedTexCoord);
    vec3 diff = samplePos - pos;
    
    // IMPROVED: Reduce influence of samples with large depth differences
    float depthDiff = abs(samplePos.z - pos.z);
    float depthScale = pos.z * 0.2; // Scale depth threshold by current depth
    float depthWeight = 1.0 - clamp(depthDiff / depthScale, 0.0, 1.0);
    depthWeight = depthWeight * depthWeight; // Square for sharper falloff
    
    vec3 v = normalize(diff);
    float d = length(diff) * m_Scale;
    
    // IMPROVED: Only consider samples that are in front of tangent plane
    float NdotV = dot(norm, v);
    float occlusion = max(0.0, NdotV - m_Bias) * (1.0 / (1.0 + d)) * depthWeight;
    
    return occlusion * m_Intensity;
}

vec2 reflection(in vec2 v1,in vec2 v2){
    vec2 result= 2.0 * dot(v2, v1) * v2;
    result = v1-result;
    return result;
}

// NEW: Calculate adaptive radius that reduces near depth edges
float getAdaptiveRadius(in vec2 uv, float baseRadius) {
    float depthEdge = getDepthEdge(uv);
    // Scale radius to 20-100% based on depth edge factor
    return baseRadius * (0.2 + 0.8 * depthEdge);
}

void main() {
    float depthv = getDepth(m_DepthTexture, texCoord).r;

    if (depthv == 1.0) {
        gl_FragColor = vec4(1.0); // Skip SSAO for invalid depth
        return;
    }

    vec3 position = getPosition(depthv, texCoord);

    #ifdef APPROXIMATE_NORMALS
        vec3 normal = approximateNormal(position, texCoord);
    #else
        vec3 normal = getNormal(texCoord);
    #endif

    vec2 rand = getRandom(texCoord);

    float ao = 0.0;
    
    // IMPROVED: Calculate base and adaptive radius
    float baseRad = clamp(m_SampleRadius / position.z, 0.0, 0.1);
    float rad = getAdaptiveRadius(texCoord, baseRad);
    
    // NEW: Detect depth edges for final AO weighting
    float depthEdgeFactor = getDepthEdge(texCoord);

    int iterations = 4;
    for (int j = 0; j < iterations; ++j) {
        vec2 coord1 = reflection(vec2(m_Samples[j]), vec2(rand)) * vec2(rad, rad);
        vec2 coord2 = vec2(coord1.x * 0.707 - coord1.y * 0.707, coord1.x * 0.707 + coord1.y * 0.707);

        // Use 4 samples per iteration at different distances
        ao += doAmbientOcclusion(texCoord + coord1.xy * 0.25, position, normal);
        ao += doAmbientOcclusion(texCoord + coord2 * 0.50, position, normal);
        ao += doAmbientOcclusion(texCoord + coord1.xy * 0.75, position, normal);
        ao += doAmbientOcclusion(texCoord + coord2 * 1.00, position, normal);
    }
    ao /= float(iterations) * 4.0;

    // IMPROVED: Wider edge fade (15% from edges instead of 5%)
    float edgeFade = smoothstep(0.0, 0.15, texCoord.x) *
                     smoothstep(0.0, 0.15, texCoord.y) *
                     smoothstep(0.0, 0.15, 1.0 - texCoord.x) *
                     smoothstep(0.0, 0.15, 1.0 - texCoord.y);
    
    // IMPROVED: Combine screen edge fade with depth edge fade
    float finalFade = edgeFade * (0.3 + 0.7 * depthEdgeFactor);
    ao *= finalFade;
    
    // AO is subtracted from 1.0 to get the final result
    // Higher values mean less occlusion
    gl_FragColor = vec4(1.0 - ao);
}