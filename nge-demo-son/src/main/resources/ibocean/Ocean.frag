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
 uniform vec3 g_CameraPosition;

#import "ibocean/Ocean.glsl"
#import "ibocean/OceanRef.glsl"
#import "Common/ShaderLib/Instancing.glsllib"

#ifdef DEBUG_VALUES_MODE
    uniform int m_DebugValuesMode;
#endif

uniform vec4 g_LightData[NB_LIGHTS];

uniform vec3 m_BaseScale;
uniform vec3 m_Scale;
uniform vec3 m_Wind;
uniform sampler2DArray m_OceanMap;
uniform vec2 m_TileSize;
uniform vec2 m_Offsets[NUM_LAYERS];

uniform sampler2D m_FoamTexture;
uniform sampler2D m_RefMap;
uniform mat4 m_ReflViewProj;
uniform float g_Time;

void main() {
    
    vec3 wpos = PBRLightingUtils_getWorldPosition();  
    vec2 uv = wpos.xz/100.0;

    
    vec4 normalHeight = sampleIBOcean(
        wpos,
        m_Offsets,
        m_BaseScale,
        m_Scale,
        m_TileSize,
        m_OceanMap
    );
    normalHeight.xyz = normalize(mix(vec3(0.0, 1.0, 0.0), normalHeight.xyz, 0.2)); // 0.3 = 30% strength
  
     

    float time = g_Time;
    vec3 deepColor = vec3(35.0/255.0,0.0,110.0/255.0)*0.6; // Deep ocean color
    
    vec3 shallowColor = vec3(75.0/255.0,0.3,130.0/255.0)*3.0;  
    vec3 foamColor = vec3(1.15, 1.13, 1.10);    

    vec3 wViewDir = normalize(g_CameraPosition - wpos);
    vec3 tan = normalize(wTangent.xyz);

    float toYScale = m_BaseScale.y  * m_Scale.y;    
    float minHighWave = toYScale * 0.75;
    float maxHighWave = toYScale * 0.86;
    float minLowTide = m_BaseScale.y * 6.0 * 0.90;
    float maxLowTide = m_BaseScale.y * 6.0 * 0.95;
    float lowTideFactor = smoothstep(minLowTide,maxLowTide,normalHeight.w);

    float waterHeight = smoothstep(minHighWave,maxHighWave,normalHeight.w) * lowTideFactor;

    float foam = texture(m_FoamTexture, vec2(-time*0.002)*m_Wind.xz+uv*m_Scale.xz).r;    
    float foamMask = smoothstep(0.5,0.8,waterHeight*1.4)*foam;    


    vec3 waterColor =  mix(deepColor, shallowColor,  waterHeight);
    waterColor = mix(waterColor, foamColor, foamMask);

 
    

   
    mat3 tbn = mat3(tan, wTangent.w * cross(normalize(wNormal), tan), normalize(wNormal));
    vec3 finalNormal=normalize(normalHeight.xyz);
        
    PBRSurface surface;
    surface.directLightContribution = vec3(0.0);
    surface.bakedLightContribution = vec3(0.0);
    surface.envLightContribution = vec3(0.0);
    
    surface.position = wPosition;
    surface.viewDir = wViewDir;
    surface.geometryNormal = normalize(wNormal);
    surface.tbnMat = tbn;
    surface.normal = finalNormal;
    surface.frontFacing = gl_FrontFacing;
    surface.depth = gl_FragCoord.z;
    surface.hasTangents = true;

       
    surface.metallic = mix(0.7, 0.1, foamMask);
    surface.roughness =0.02;
    surface.alpha = 1.0;
    surface.albedo = waterColor;
         

    surface.emission= vec3(0.0);


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

    vec4 reflection = sampleReflection(
        m_RefMap, 
        m_ReflViewProj,
        wpos,
        finalNormal,
            0.01

    );
    float reflectionFactor = reflection.a;
      
    surface.envLightContribution = mix(surface.envLightContribution, reflection.rgb,  reflectionFactor*0.3);

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