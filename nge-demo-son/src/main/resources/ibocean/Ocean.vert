#import "Common/ShaderLib/GLSLCompat.glsllib"
#import "Common/ShaderLib/Instancing.glsllib"
#import "ibocean/FlipbookTexture.glsl"
 
#import "ibocean/Ocean.glsl"

in vec3 inPosition;
in vec2 inTexCoord;
in vec3 inNormal;
in vec4 inTangent;

out vec3 wNormal;
out vec3 wPosition;
out vec4 wTangent;
out vec2 texCoord;

uniform vec3 g_CameraPosition;

uniform vec3 m_BaseScale;
uniform vec3 m_Scale;
uniform sampler2DArray m_OceanMap;
uniform vec2 m_TileSize;
uniform vec2 m_Offsets[NUM_LAYERS];


 

void main(){      
   
    texCoord = inTexCoord;

    

    vec4 modelSpacePos = vec4(inPosition, 1.0);
    vec3 modelSpaceNorm = inNormal;
    vec3 modelSpaceTan  = inTangent.xyz;

    vec3 wpos = TransformWorld(modelSpacePos).xyz;

 

    vec4 normalHeight = sampleIBOcean(
        wpos,
        m_Offsets,
        m_BaseScale,
        m_Scale,
        m_TileSize,
        m_OceanMap
    );

    float att = 1.0-smoothstep(0.4, 1.0, length(modelSpacePos.xz) / 2048.0);


    wpos.y += normalHeight.a*att;
    
    
    wPosition = wpos;
    wNormal  =  TransformWorldNormal(modelSpaceNorm);
    wTangent = vec4(TransformWorldNormal(modelSpaceTan),inTangent.w);
    

    gl_Position = g_ViewProjectionMatrix * vec4(wpos,1.0);

}
