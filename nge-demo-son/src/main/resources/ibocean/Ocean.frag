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

uniform vec3 m_Offset;

uniform sampler2D m_FoamTexture;
uniform float g_Time;

 
 

void main() {
    float time = g_Time;

    // --- TROPICAL COLOR GRADIENT ---
    vec3 deepColor = vec3(0.01, 0.18, 0.32);      // Deep blue
    vec3 shallowColor = vec3(0.47, 0.85, 1.05);       // Mid turquoise
    vec3 foamColor = vec3(1.15, 1.13, 1.10);      // Slightly warm foam

    vec3 wpos = PBRLightingUtils_getWorldPosition();
    vec3 wViewDir = normalize(g_CameraPosition - wpos);
    vec3 tan = normalize(wTangent.xyz);




    vec4 normalHeight1 = textureFlipBook(m_NormalHeight, m_NormalHeightData, (m_Offset.xz)+texCoord*m_Scale.xz) * vec4(1.0,1.0,1.0, 1.0);
    // vec4 normalHeight2  = textureFlipBook(m_NormalHeight, m_NormalHeightData, (m_Offset2.xz)+texCoord*m_Scale2.xz) * vec4(1.0,1.0,1.0, m_Scale2.y);
    vec4 normalHeight = normalHeight1;

    // interpolate between two normal maps (rgb)

    // // Decode normals from [0,1] to [-1,1] range
    // vec3 normal1 = normalHeight1.xyz * 2.0 - 1.0;
    // vec3 normal2 = normalHeight2.xyz * 2.0 - 1.0;

    // // Normalize the normals
    // normal1 = normalize(normal1);
    // normal2 = normalize(normal2);

    // // Interpolate between normalized vectors
    // vec3 mixedNormal = normalize(mix(normal1, normal2, 0.5));

    // // Pack back to [0,1] range for storage
    // normalHeight.xyz = mixedNormal * 0.5 + 0.5;
    

    // // interpolate height (a)
    // normalHeight.w = (normalHeight1.w + normalHeight2.w) * 0.5;
    

 

    // Color gradient by depth
    vec3 waterColor;
    
    float foam = texture(m_FoamTexture, g_Time*0.03+(m_Offset.xz)+texCoord*m_Scale.xz).r;    
    float foamMask = smoothstep(0.5,0.8,normalHeight.w)*foam;    
    float waterHeight = smoothstep(0.3,0.8,normalHeight.w);

    waterColor =  mix(deepColor, shallowColor,  waterHeight);
    waterColor = mix(waterColor, foamColor, foamMask);

    


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
    surface.albedo = waterColor;

    // Water is smooth and reflective, foam is rough and diffuse
    surface.metallic = mix(0.9, 0.4, foamMask);
    surface.roughness = mix(0.04, 0.3, waterHeight);
    surface.alpha = 0.8;

    // Subsurface scattering for shallow water
    // surface.emission = mix(vec3(0.0), foamColor, waterHeight);
     surface.emission= vec3(0.0);


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