#import "Common/ShaderLib/GLSLCompat.glsllib"
#import "Common/ShaderLib/Instancing.glsllib"
#import "ibocean/FlipbookTexture.glsl"

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

uniform sampler2DArray m_Crest;
uniform vec2 m_CrestData;

uniform vec3 m_Scale;
uniform vec3 m_Scale2;
void main(){      
    vec4 normalHeight = textureFlipBook(m_NormalHeight, m_NormalHeightData, inTexCoord*m_Scale.xz) * vec4(1.0,1.0,1.0, m_Scale.y);
    normalHeight += textureFlipBook(m_NormalHeight, m_NormalHeightData, inTexCoord*m_Scale2.xz) * vec4(1.0,1.0,1.0, m_Scale2.y);
    normalHeight /= vec4(2.0);


    vec4 modelSpacePos = vec4(inPosition, 1.0);
    modelSpacePos.y += normalHeight.a;

    vec3 modelSpaceNorm = inNormal;
    vec3 modelSpaceTan  = inTangent.xyz;
    
    texCoord = inTexCoord;

    wPosition = TransformWorld(modelSpacePos).xyz;
    wNormal  = TransformWorldNormal(modelSpaceNorm);
    wTangent = vec4(TransformWorldNormal(modelSpaceTan),inTangent.w);

    gl_Position = TransformWorldViewProjection(modelSpacePos);
}
