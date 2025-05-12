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

uniform sampler2DArray m_NormalHeight;
uniform vec2 m_NormalHeightData;
uniform vec3 g_CameraPosition;

uniform vec3 m_Scale;
uniform vec3 m_Size;
void main(){      
   
    texCoord = inTexCoord;

    #ifdef RENDER_BUOYANCY_MAP
        vec2 pos = inPosition.xy * 2.0 - 1.0;
        gl_Position = vec4(pos, 0.0, 1.0);   
    #else
    

        vec4 modelSpacePos = vec4(inPosition, 1.0);
        vec3 modelSpaceNorm = inNormal;
        vec3 modelSpaceTan  = inTangent.xyz;

        vec3 wpos = TransformWorld(modelSpacePos).xyz;

        vec4 normalHeight = sampleOcean(
            m_NormalHeight, 
            m_NormalHeightData, 
            wpos,
            m_Size,
            m_Scale
        );

   
        #ifndef RENDER_BUOYANCY_MAP
            wpos.y += normalHeight.a;
        #endif 
        
        wPosition = wpos;
        wNormal  = TransformWorldNormal(modelSpaceNorm);
        wTangent = vec4(TransformWorldNormal(modelSpaceTan),inTangent.w);
        
   
        gl_Position = g_ViewProjectionMatrix * vec4(wpos,1.0);
    #endif

}
