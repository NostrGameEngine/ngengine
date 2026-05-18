#import "Common/ShaderLib/GLSLCompat.glsllib"

// enable apis and import PBRLightingUtils
#define ENABLE_PBRLightingUtils_getWorldPosition 1
//#define ENABLE_PBRLightingUtils_getLocalPosition 1
#define ENABLE_PBRLightingUtils_getWorldNormal 1
#define ENABLE_PBRLightingUtils_getWorldTangent 1
#define ENABLE_PBRLightingUtils_getTexCoord 1
#define ENABLE_PBRLightingUtils_readPBRSurface 1
#define ENABLE_PBRLightingUtils_computeDirectLightContribution 1
#define ENABLE_PBRLightingUtils_computeProbesContribution 1

#import "Common/ShaderLib/module/pbrlighting/PBRLightingUtils.glsllib"

#ifdef DEBUG_VALUES_MODE
    uniform int m_DebugValuesMode;
#endif

uniform vec4 g_LightData[NB_LIGHTS];
uniform int g_LightCount;
uniform vec3 g_CameraPosition;

#ifdef USE_FOG
    #import "Common/ShaderLib/MaterialFog.glsllib"
#endif

#if defined(MATCAP_MAP) || defined(MATCAP_METAL_MAP)
    #if defined(MATCAP_MAP) 
        uniform sampler2D m_MatCapMap;
    #endif
    #if defined(MATCAP_METAL_MAP) 
        uniform sampler2D m_MatCapMetalMap;
    #endif
    uniform float m_MatCapIntensity; 
    uniform mat4 g_ViewMatrix;
    #define USE_MATCAP
#endif

void main(){
    vec3 wpos = PBRLightingUtils_getWorldPosition();
    vec3 worldViewDir = normalize(g_CameraPosition - wpos);
    
    // Create a blank PBRSurface.
    PBRSurface surface = PBRLightingUtils_createPBRSurface(worldViewDir);
    
    // Read surface data from standard PBR matParams. (note: matParams are declared in 'PBRLighting.j3md' and initialized as uniforms in 'PBRLightingUtils.glsllib')
    PBRLightingUtils_readPBRSurface(surface);          

    //Calculate necessary variables from pbr surface prior to applying lighting. Ensure all texture/param reading and blending occurrs prior to this being called!
    PBRLightingUtils_calculatePreLightingValues(surface);
    
    // Calculate direct lights
    for(int i = 0;i < NB_LIGHTS; i+=3){
        #if (defined(GL_ES) && __VERSION__ >= 300) || (!defined(GL_ES) && __VERSION__ >= 150)
        if(i >= g_LightCount * 3){
            break;
        }
        #endif
        vec4 lightData0 = g_LightData[i];
        vec4 lightData1 = g_LightData[i+1];
        vec4 lightData2 = g_LightData[i+2];    
        PBRLightingUtils_computeDirectLightContribution(
          lightData0, lightData1, lightData2, 
          surface
        );
    }


    // Calculate env probes
    PBRLightingUtils_computeProbesContribution(surface);

    // Apply MatCap if enabled
    #ifdef USE_MATCAP
        float matcapIntensity = clamp(m_MatCapIntensity, 0.0, 1.0);
        #if NB_PROBES == 0
            matcapIntensity = 1.0; // if no env probes, matcap fully replaces env lighting
        #endif
        if(matcapIntensity > 0.0){
            vec3 vN = normalize(mat3(g_ViewMatrix)*surface.normal); // view space
            vec2 uv = vN.xy * 0.5 + 0.5;
            uv = clamp(uv, 0.001, 0.999);
            
            vec3 matCapColor = vec3(0.0);
            #ifdef MATCAP_MAP
                vec3 c = texture2D(m_MatCapMap, uv).rgb;
                c*=surface.albedo.rgb;
                #if defined(MATCAP_METAL_MAP)
                    c*=(1.0 - surface.metallic); // mask non-metal areas
                #endif
                matCapColor += c;
            #endif

            #ifdef MATCAP_METAL_MAP
                vec3 cMetal = texture2D(m_MatCapMetalMap, uv).rgb;
                cMetal*=surface.albedo.rgb;
                #if defined(MATCAP_MAP)
                    cMetal*=surface.metallic; // mask metal areas
                #endif
                matCapColor += cMetal;
            #endif
            
            surface.envLightContribution.rgb = mix(
                surface.envLightContribution.rgb,
                matCapColor,
                matcapIntensity
            );
        }
    #endif

    // Put it all together
    gl_FragColor.rgb = vec3(0.0);
    gl_FragColor.rgb += surface.bakedLightContribution;
    gl_FragColor.rgb += surface.directLightContribution;
    gl_FragColor.rgb += surface.envLightContribution;
    gl_FragColor.rgb += surface.emission;
    gl_FragColor.a = surface.alpha;    

    #ifdef USE_FOG
        gl_FragColor = MaterialFog_calculateFogColor(vec4(gl_FragColor));
    #endif
    
   //outputs the final value of the selected layer as a color for debug purposes. 
    #ifdef DEBUG_VALUES_MODE
        gl_FragColor = PBRLightingUtils_getColorOutputForDebugMode(m_DebugValuesMode, vec4(gl_FragColor.rgba), surface);
    #endif   
}
