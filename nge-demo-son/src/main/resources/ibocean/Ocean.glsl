#ifndef OCEAN_GLSL
#define OCEAN_GLSL
#import "ibocean/FlipbookTexture.glsl"

vec2 getCoords(in vec3 wpos, in vec3 size){
    return wpos.xz / size.xz;
}


vec4 sampleOcean(
    in sampler2DArray tex, 
    in vec2 data, 
    in vec3 wpos,
    in vec3 size,
    in vec3 scale
){
    vec4 v =  textureFlipBook(
        tex, 
        data, 
        getCoords(wpos,size)*scale.xz
    );
    v.w *= scale.y;
   

    return v;
}
#endif