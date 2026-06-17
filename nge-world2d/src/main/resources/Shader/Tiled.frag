#import "Common/ShaderLib/GLSLCompat.glsllib"

#if defined(HAS_COLOR_ARRAY0) || defined(HAS_COLOR_ARRAY1) || defined(HAS_COLOR_ARRAY2) || defined(HAS_COLOR_ARRAY3)
#extension GL_EXT_texture_array : enable
#endif

#ifdef HAS_TRANS_COLOR
uniform vec4 m_TransColor;
#endif

#if defined(USE_TINT_COLOR) && defined(HAS_TINT_COLOR)
uniform vec4 m_TintColor;
#endif

#ifdef HAS_COLOR
uniform vec4 m_Color;
#endif

#ifdef HAS_OPACITY
uniform float m_Opacity;
#endif

#ifdef HAS_LAYER_OPACITY
uniform float m_LayerOpacity;
#endif

#ifdef HAS_COLOR_MAP
uniform sampler2D m_ColorMap;
#endif

#ifdef HAS_COLOR_MAP0
uniform sampler2D m_ColorMap0;
#endif
#ifdef HAS_COLOR_MAP1
uniform sampler2D m_ColorMap1;
#endif
#ifdef HAS_COLOR_MAP2
uniform sampler2D m_ColorMap2;
#endif
#ifdef HAS_COLOR_MAP3
uniform sampler2D m_ColorMap3;
#endif

#ifdef HAS_COLOR_ARRAY0
#if !defined(GL_EXT_texture_array) && __VERSION__ < 130
#error Texture arrays are not supported, but required for instanced Tiled image collections.
#endif
uniform sampler2DArray m_ColorArray0;
#endif
#ifdef HAS_COLOR_ARRAY1
#if !defined(GL_EXT_texture_array) && __VERSION__ < 130
#error Texture arrays are not supported, but required for instanced Tiled image collections.
#endif
uniform sampler2DArray m_ColorArray1;
#endif
#ifdef HAS_COLOR_ARRAY2
#if !defined(GL_EXT_texture_array) && __VERSION__ < 130
#error Texture arrays are not supported, but required for instanced Tiled image collections.
#endif
uniform sampler2DArray m_ColorArray2;
#endif
#ifdef HAS_COLOR_ARRAY3
#if !defined(GL_EXT_texture_array) && __VERSION__ < 130
#error Texture arrays are not supported, but required for instanced Tiled image collections.
#endif
uniform sampler2DArray m_ColorArray3;
#endif

#ifdef USE_TILESET_IMAGE
#ifdef INSTANCING
varying vec4 v_TileData;
varying vec2 v_UvSize;
varying vec2 v_ImageSize;
#else
uniform vec2 m_ImageSize;
uniform vec4 m_TileSize;//(width, height, margin, space)
varying vec2 v_TilePos;
#endif
#endif

varying vec2 v_TexCoord;

vec2 getTileUVClamped(vec2 tilePos, vec2 tileSize, vec2 imageSize) {
    vec2 pixel = v_TexCoord * tileSize + tilePos;
    vec2 min = vec2(tilePos + 0.5);
    vec2 max = vec2(tilePos + tileSize - 0.5);
    vec2 uv = clamp(pixel, min, max) / imageSize;
    uv.y = 1.0 - uv.y;
    return uv;
}

#ifdef INSTANCING
vec4 sampleInstancedTile(vec2 tileUv) {
    float slot = v_TileData.z;
    if (slot < -0.5) {
        discard;
    }
    if (false) {
        return vec4(1.0);
    }
#ifdef HAS_COLOR_MAP0
    else if (slot < 0.5) {
        return texture2D(m_ColorMap0, getTileUVClamped(v_TileData.xy, v_UvSize, v_ImageSize));
    }
#endif
#ifdef HAS_COLOR_ARRAY0
    else if (slot < 0.5) {
        return texture2DArray(m_ColorArray0, vec3(getTileUVClamped(vec2(0.0), v_UvSize, v_ImageSize), v_TileData.x));
    }
#endif
#ifdef HAS_COLOR_MAP1
    else if (slot >= 0.5 && slot < 1.5) {
        return texture2D(m_ColorMap1, getTileUVClamped(v_TileData.xy, v_UvSize, v_ImageSize));
    }
#endif
#ifdef HAS_COLOR_ARRAY1
    else if (slot >= 0.5 && slot < 1.5) {
        return texture2DArray(m_ColorArray1, vec3(getTileUVClamped(vec2(0.0), v_UvSize, v_ImageSize), v_TileData.x));
    }
#endif
#ifdef HAS_COLOR_MAP2
    else if (slot >= 1.5 && slot < 2.5) {
        return texture2D(m_ColorMap2, getTileUVClamped(v_TileData.xy, v_UvSize, v_ImageSize));
    }
#endif
#ifdef HAS_COLOR_ARRAY2
    else if (slot >= 1.5 && slot < 2.5) {
        return texture2DArray(m_ColorArray2, vec3(getTileUVClamped(vec2(0.0), v_UvSize, v_ImageSize), v_TileData.x));
    }
#endif
#ifdef HAS_COLOR_MAP3
    else if (slot >= 2.5 && slot < 3.5) {
        return texture2D(m_ColorMap3, getTileUVClamped(v_TileData.xy, v_UvSize, v_ImageSize));
    }
#endif
#ifdef HAS_COLOR_ARRAY3
    else if (slot >= 2.5 && slot < 3.5) {
        return texture2DArray(m_ColorArray3, vec3(getTileUVClamped(vec2(0.0), v_UvSize, v_ImageSize), v_TileData.x));
    }
#endif
    return vec4(1.0);
}
#endif

void main(){
    vec4 color = vec4(1.0);

#if defined(HAS_COLOR_MAP) || defined(INSTANCING)
    vec2 uv = v_TexCoord;

    #ifdef USE_TILESET_IMAGE
    #ifdef INSTANCING
    color = sampleInstancedTile(uv);
    #else
    uv = getTileUVClamped(v_TilePos, m_TileSize.xy, m_ImageSize.xy);
    color = texture2D(m_ColorMap, uv);
    #endif
    #else
    color = texture2D(m_ColorMap, uv);
    #endif

    #ifdef HAS_TRANS_COLOR
    if(color.rgb == m_TransColor.rgb) {
        discard;
    }
    #endif

    if (color.a < 0.5) {
        discard;
    }
#endif

    #ifdef HAS_COLOR
    color *= m_Color;
    #endif

    #if defined(USE_TINT_COLOR) && defined(HAS_TINT_COLOR)
    color *= m_TintColor;
    #endif

    #ifdef HAS_LAYER_OPACITY
    color.a *= m_LayerOpacity;
    #endif

    #ifdef HAS_OPACITY
    color.a *= m_Opacity;
    #endif

    gl_FragColor = color;
}
