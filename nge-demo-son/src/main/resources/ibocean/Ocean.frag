#import "Common/ShaderLib/GLSLCompat.glsllib"
#define HORIZON_FADE 1
// enable apis and import PBRLightingUtils
#define ENABLE_PBRLightingUtils_getWorldPosition 1
//#define ENABLE_PBRLightingUtils_getLocalPosition 1
#define ENABLE_PBRLightingUtils_getWorldNormal 1
#define ENABLE_PBRLightingUtils_getWorldTangent 1
#define ENABLE_PBRLightingUtils_getTexCoord 1
// #define ENABLE_PBRLightingUtils_readPBRSurface 1
#define ENABLE_PBRLightingUtils_computeDirectLightContribution 1
#define ENABLE_PBRLightingUtils_computeProbesContribution 1

#import "Common/ShaderLib/module/pbrlighting/PBRLightingUtils.glsllib"
#import "ibocean/FlipbookTexture.glsl"

#ifdef DEBUG_VALUES_MODE
    uniform int m_DebugValuesMode;
#endif

uniform vec4 g_LightData[NB_LIGHTS];
uniform vec3 g_CameraPosition;

uniform sampler2DArray m_NormalHeight;
uniform vec2 m_NormalHeightData;

 
uniform vec3 m_Scale;
uniform vec3 m_Scale2;

uniform sampler2D m_FoamTexture;
uniform float g_Time;

 
float getCrestIntensity(vec4 normalHeight, vec2 texCoord, float time) {
    vec3 normalVector = normalize(normalHeight.xyz * 2.0 - 1.0);
    float waveHeight = normalHeight.w;
    float wavePeak = smoothstep(0.68, 0.97, waveHeight);

    // Steepness and intersection
    float steepness = 1.0 - abs(dot(normalVector, vec3(0,0,1)));
    vec3 ddx = dFdx(normalVector);
    vec3 ddy = dFdy(normalVector);
    float intersection = length(ddx) + length(ddy);

    // Multi-layer animated foam breakup
    float foamNoise1 = 0.5 + 0.5 * sin(texCoord.x * 40.0 + texCoord.y * 60.0 + waveHeight * 12.0 + time * 0.7);
    float foamNoise2 = 0.5 + 0.5 * sin(texCoord.x * 80.0 - texCoord.y * 40.0 + waveHeight * 24.0 - time * 0.5);
    float foamNoise = mix(foamNoise1, foamNoise2, 0.5);

    // Foam only on crests and intersections, with breakup
    float foam = (steepness * 2.5 + intersection * 3.0) * wavePeak * foamNoise;

    // Add foam texture detail (animated)
    float foamTex = texture(m_FoamTexture, texCoord * 8.0 + vec2(waveHeight * 2.0, time * 0.15)).r;
    foam = mix(foam, foam * foamTex, 0.8);

    // Sharpen and clamp
    foam = pow(clamp(foam, 0.0, 1.0), 1.4);

    // Fade foam in deep water
    float waterDepth = pow(waveHeight, 2.0);
    foam *= smoothstep(0.95, 0.15, waterDepth);

    return foam;
}

void main() {
    float time = g_Time;

    // --- TROPICAL COLOR GRADIENT ---
    vec3 deepColor = vec3(0.01, 0.18, 0.32);      // Deep blue
    vec3 midColor = vec3(0.07, 0.45, 0.65);       // Mid turquoise
    vec3 shallowColor = vec3(0.18, 0.78, 0.82);   // Shallow, almost white-turquoise
    vec3 foamColor = vec3(1.15, 1.13, 1.10);      // Slightly warm foam

    vec3 wpos = PBRLightingUtils_getWorldPosition();
    vec3 wViewDir = normalize(g_CameraPosition - wpos);
    vec3 tan = normalize(wTangent.xyz);

    vec4 normalHeight = textureFlipBook(m_NormalHeight, m_NormalHeightData, texCoord*m_Scale.xz) * vec4(1.0,1.0,1.0, m_Scale.y);
    normalHeight += textureFlipBook(m_NormalHeight, m_NormalHeightData, texCoord*m_Scale2.xz) * vec4(1.0,1.0,1.0, m_Scale2.y);
    normalHeight /= vec4(2.0);

    float waterDepth = pow(normalHeight.w, 2.2);

    // Color gradient by depth
    vec3 waterColor;
    waterColor = deepColor ;//mix(deepColor, midColor, waterDepth / 0.25);
   
    // Fresnel effect for glancing angles
    
    float fresnel = pow(1.0 - max(dot(normalize(normalHeight.xyz * 2.0 - 1.0), wViewDir), 0.0), 3.0);
    waterColor = mix(waterColor, vec3(0.45, 0.75, 1.0), fresnel * 0.45);

    // --- FOAM ---
    float crestIntensity = getCrestIntensity(normalHeight, texCoord * m_Scale.xz, time);

    // View-dependent foam: more visible at glancing angles
    float viewFoam = pow(1.0 - max(0.0, dot(normalize(normalHeight.xyz * 2.0 - 1.0), wViewDir)), 2.0) * 0.45;
    crestIntensity = clamp(crestIntensity + viewFoam, 0.0, 1.0);

    // --- NORMALS ---
    vec3 normalVector = normalize(normalHeight.xyz * 2.0 - 1.0);
    mat3 tbn = mat3(tan, wTangent.w * cross(normalize(wNormal), tan), normalize(wNormal));
    int normalType  = 1;
    vec3 finalNormal = normalize(tbn * normalize((normalHeight.xyz * vec3(2.0, normalType*2.0, 2.0) - vec3(1.0, normalType*1.0, 0.0))));

    // --- SURFACE ---
    PBRSurface surface;
    surface.position = wPosition;
    surface.viewDir = wViewDir;
    surface.geometryNormal = normalize(wNormal);
    surface.tbnMat = tbn;
    surface.normal = finalNormal;
    surface.frontFacing = gl_FrontFacing;
    surface.depth = gl_FragCoord.z;
    surface.hasTangents = true;

    // --- BLEND FOAM AND WATER ---
    // vec3 foamTint = mix(foamColor, waterColor, 0.22);
    // surface.albedo = mix(waterColor, foamTint, crestIntensity);

    // Water is smooth and reflective, foam is rough and diffuse
    surface.metallic = mix(0.98, 0.0, crestIntensity);
    surface.roughness = mix(0.04, 0.93, crestIntensity);
    surface.alpha = 1.0;

    // Subsurface scattering for shallow water
    surface.emission = waterColor * (1.0 - waterDepth) * 0.13;

    // Horizon fade
    float horizonFactor = 1.0 - pow(1.0 - abs(dot(wViewDir, surface.geometryNormal)), 4.0);
    surface.albedo = mix(surface.albedo, vec3(0.7, 0.85, 0.95) * 0.5, horizonFactor * 0.6);

    // --- LIGHTING ---
    surface.lightMapColor = vec3(1.0);    
    surface.hasBasicLightMap = false; 
    surface.exposure = 1.0;        
    surface.ao = vec3(1.0);
    surface.brightestNonGlobalLightStrength = 0.0;

    PBRLightingUtils_calculatePreLightingValues(surface);
    for(int i = 0; i < NB_LIGHTS; i+=3) {
        vec4 lightData0 = g_LightData[i];
        vec4 lightData1 = g_LightData[i+1];
        vec4 lightData2 = g_LightData[i+2];    
        PBRLightingUtils_computeDirectLightContribution(
          lightData0, lightData1, lightData2, 
          surface
        );
    }
    PBRLightingUtils_computeProbesContribution(surface);

    outFragColor.rgb = vec3(0.0);
    outFragColor.rgb += surface.bakedLightContribution;
    outFragColor.rgb += surface.directLightContribution;
    outFragColor.rgb += surface.envLightContribution;
    outFragColor.rgb += surface.emission;
    outFragColor.a = surface.alpha;

    #ifdef DEBUG_VALUES_MODE
        outFragColor = PBRLightingUtils_getColorOutputForDebugMode(m_DebugValuesMode, vec4(outFragColor.rgba), surface);
    #endif   
}