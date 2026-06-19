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
#ifdef HAS_DECAL_MAP
uniform sampler2D m_DecalMap;
uniform vec2 m_DecalImageSize;
uniform vec4 m_DecalTileSize;
uniform vec4 m_Decal0;
uniform vec4 m_Decal1;
uniform vec4 m_Decal2;
uniform vec4 m_Decal3;
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
#ifdef HAS_DECAL_MAP
varying vec4 v_Decal0;
varying vec4 v_Decal1;
varying vec4 v_Decal2;
varying vec4 v_Decal3;
#endif
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

#ifdef HAS_DECAL_MAP
vec4 sampleInstancedDecal(vec4 decal) {
    if (decal.x < -0.5 || decal.w <= 0.0) {
        return vec4(0.0);
    }

    vec2 decalUv = (v_TexCoord - decal.yz + decal.ww * 0.5) / decal.ww;
    if (decalUv.x < 0.0 || decalUv.x > 1.0 || decalUv.y < 0.0 || decalUv.y > 1.0) {
        return vec4(0.0);
    }

    float strideX = m_DecalTileSize.x + m_DecalTileSize.w;
    float strideY = m_DecalTileSize.y + m_DecalTileSize.w;
    float columns = max(1.0, floor((m_DecalImageSize.x - m_DecalTileSize.z * 2.0 + m_DecalTileSize.w) / strideX));
    float tileId = floor(decal.x + 0.5);
    float col = mod(tileId, columns);
    float row = floor(tileId / columns);
    vec2 tilePos = vec2(
        m_DecalTileSize.z + col * strideX,
        m_DecalTileSize.z + row * strideY
    );
    vec2 pixel = decalUv * m_DecalTileSize.xy + tilePos;
    vec2 minPixel = tilePos + vec2(0.5);
    vec2 maxPixel = tilePos + m_DecalTileSize.xy - vec2(0.5);
    vec2 uv = clamp(pixel, minPixel, maxPixel) / m_DecalImageSize;
    uv.y = 1.0 - uv.y;
    return texture2D(m_DecalMap, uv);
}

vec4 alphaOver(vec4 under, vec4 over) {
    return mix(under, over, over.a);
}
#endif

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
    #ifdef HAS_DECAL_MAP
    color = alphaOver(color, sampleInstancedDecal(v_Decal0));
    color = alphaOver(color, sampleInstancedDecal(v_Decal1));
    color = alphaOver(color, sampleInstancedDecal(v_Decal2));
    color = alphaOver(color, sampleInstancedDecal(v_Decal3));
    #endif
    #else
    uv = getTileUVClamped(v_TilePos, m_TileSize.xy, m_ImageSize.xy);
    color = texture2D(m_ColorMap, uv);
    #ifdef HAS_DECAL_MAP
    color = alphaOver(color, sampleInstancedDecal(m_Decal0));
    color = alphaOver(color, sampleInstancedDecal(m_Decal1));
    color = alphaOver(color, sampleInstancedDecal(m_Decal2));
    color = alphaOver(color, sampleInstancedDecal(m_Decal3));
    #endif
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
