#ifndef FLIPBOOKTEXTURE_GLSL
#define FLIPBOOKTEXTURE_GLSL
vec4 textureFlipBook(
    in sampler2DArray tex, 
    in vec2 texData, 
    in vec2 texCoord
){
    float nFrames = floor(texData.y);
    float currentFrame = floor(texData.x);

    vec4 color = texture(tex, vec3(texCoord, currentFrame));
    return color;
}
#endif