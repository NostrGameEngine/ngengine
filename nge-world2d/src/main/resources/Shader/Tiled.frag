#import "Common/ShaderLib/GLSLCompat.glsllib"

#if defined(HAS_COLOR_ARRAY) || defined(HAS_COLOR_ARRAY0) || defined(HAS_COLOR_ARRAY1) || defined(HAS_COLOR_ARRAY2) || defined(HAS_COLOR_ARRAY3) || defined(HAS_DECAL_ARRAY)
#extension GL_EXT_texture_array : enable
#endif

#ifdef HAS_TRANS_COLOR
uniform vec4 m_TransColor;
#endif

#if defined(USE_TINT_COLOR) && defined(HAS_TINT_COLOR)
uniform vec4 m_TintColor;
#endif

#ifdef HAS_HUE_SHIFT
uniform float m_HueShift;
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

#ifdef USE_TILE_ALPHA_OCCLUSION
uniform float m_TileAlphaOcclusionStrength;
uniform float m_TileAlphaOcclusionRadius;
#endif

#ifdef HAS_COLOR_MAP
uniform sampler2D m_ColorMap;
#endif
#ifdef HAS_COLOR_ARRAY
#if !defined(GL_EXT_texture_array) && __VERSION__ < 130
#error Texture arrays are not supported, but required for this Tiled tileset.
#endif
uniform sampler2DArray m_ColorArray;
uniform float m_TileLayer;
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
#endif
#ifdef HAS_DECAL_ARRAY
#if !defined(GL_EXT_texture_array) && __VERSION__ < 130
#error Texture arrays are not supported, but required for Tiled decals.
#endif
uniform sampler2DArray m_DecalArray;
#endif
#if defined(HAS_DECAL_MAP) || defined(HAS_DECAL_ARRAY)
uniform vec2 m_DecalImageSize;
uniform vec4 m_DecalTileSize;
uniform vec4 m_Decal0;
uniform vec4 m_Decal1;
uniform vec4 m_Decal2;
uniform vec4 m_Decal3;
uniform float m_DecalFlipFlags;
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

#if defined(HAS_DECAL_MAP) || defined(HAS_DECAL_ARRAY)
varying vec2 v_DecalTexCoord;
#endif

#ifdef USE_TILESET_IMAGE
#ifdef INSTANCING
varying vec4 v_TileData;
varying vec2 v_UvSize;
varying vec2 v_ImageSize;
#if defined(HAS_DECAL_MAP) || defined(HAS_DECAL_ARRAY)
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

vec2 getTileUVClampedAt(vec2 texCoord, vec2 tilePos, vec2 tileSize, vec2 imageSize) {
    vec2 pixel = texCoord * tileSize + tilePos;
    vec2 min = vec2(tilePos + 0.5);
    vec2 max = vec2(tilePos + tileSize - 0.5);
    vec2 uv = clamp(pixel, min, max) / imageSize;
    uv.y = 1.0 - uv.y;
    return uv;
}

vec2 getTileUVClamped(vec2 tilePos, vec2 tileSize, vec2 imageSize) {
    return getTileUVClampedAt(v_TexCoord, tilePos, tileSize, imageSize);
}

#if defined(HAS_DECAL_MAP) || defined(HAS_DECAL_ARRAY)
vec2 unflipDecalUv(vec2 decalUv) {
#ifndef INSTANCING
    float flags = m_DecalFlipFlags;
    if (mod(floor(flags / 2.0), 2.0) >= 1.0) {
        decalUv.y = 1.0 - decalUv.y;
    }
    if (mod(floor(flags), 2.0) >= 1.0) {
        decalUv.x = 1.0 - decalUv.x;
    }
    if (mod(floor(flags / 4.0), 2.0) >= 1.0) {
        decalUv = vec2(1.0 - decalUv.y, 1.0 - decalUv.x);
    }
#endif
    return decalUv;
}

vec4 sampleInstancedDecal(vec4 decal) {
    if (decal.x < -0.5 || decal.w <= 0.0) {
        return vec4(0.0);
    }

    vec2 decalUv = (v_DecalTexCoord - decal.yz + decal.ww * 0.5) / decal.ww;
    decalUv = unflipDecalUv(decalUv);
    if (decalUv.x < 0.0 || decalUv.x > 1.0 || decalUv.y < 0.0 || decalUv.y > 1.0) {
        return vec4(0.0);
    }

    float tileId = floor(decal.x + 0.5);
#ifdef HAS_DECAL_ARRAY
    vec2 uv = getTileUVClampedAt(decalUv, vec2(0.0), m_DecalTileSize.xy, m_DecalImageSize);
    return texture2DArray(m_DecalArray, vec3(uv, tileId));
#else
    float strideX = m_DecalTileSize.x + m_DecalTileSize.w;
    float strideY = m_DecalTileSize.y + m_DecalTileSize.w;
    float columns = max(1.0, floor((m_DecalImageSize.x - m_DecalTileSize.z * 2.0 + m_DecalTileSize.w) / strideX));
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
#endif
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
        return texture2D(m_ColorMap0, getTileUVClampedAt(tileUv, v_TileData.xy, v_UvSize, v_ImageSize));
    }
#endif
#ifdef HAS_COLOR_ARRAY0
    else if (slot < 0.5) {
        return texture2DArray(m_ColorArray0, vec3(getTileUVClampedAt(tileUv, vec2(0.0), v_UvSize, v_ImageSize), v_TileData.x));
    }
#endif
#ifdef HAS_COLOR_MAP1
    else if (slot >= 0.5 && slot < 1.5) {
        return texture2D(m_ColorMap1, getTileUVClampedAt(tileUv, v_TileData.xy, v_UvSize, v_ImageSize));
    }
#endif
#ifdef HAS_COLOR_ARRAY1
    else if (slot >= 0.5 && slot < 1.5) {
        return texture2DArray(m_ColorArray1, vec3(getTileUVClampedAt(tileUv, vec2(0.0), v_UvSize, v_ImageSize), v_TileData.x));
    }
#endif
#ifdef HAS_COLOR_MAP2
    else if (slot >= 1.5 && slot < 2.5) {
        return texture2D(m_ColorMap2, getTileUVClampedAt(tileUv, v_TileData.xy, v_UvSize, v_ImageSize));
    }
#endif
#ifdef HAS_COLOR_ARRAY2
    else if (slot >= 1.5 && slot < 2.5) {
        return texture2DArray(m_ColorArray2, vec3(getTileUVClampedAt(tileUv, vec2(0.0), v_UvSize, v_ImageSize), v_TileData.x));
    }
#endif
#ifdef HAS_COLOR_MAP3
    else if (slot >= 2.5 && slot < 3.5) {
        return texture2D(m_ColorMap3, getTileUVClampedAt(tileUv, v_TileData.xy, v_UvSize, v_ImageSize));
    }
#endif
#ifdef HAS_COLOR_ARRAY3
    else if (slot >= 2.5 && slot < 3.5) {
        return texture2DArray(m_ColorArray3, vec3(getTileUVClampedAt(tileUv, vec2(0.0), v_UvSize, v_ImageSize), v_TileData.x));
    }
#endif
    return vec4(1.0);
}
#endif

#if defined(USE_TILE_ALPHA_OCCLUSION) && (defined(HAS_COLOR_MAP) || defined(HAS_COLOR_ARRAY) || defined(INSTANCING))
float alphaFromSample(vec4 sampleColor) {
#ifdef HAS_TRANS_COLOR
    if (sampleColor.rgb == m_TransColor.rgb) {
        return 0.0;
    }
#endif
    return sampleColor.a;
}

vec4 sampleBaseColor(vec2 texCoord) {
#ifdef INSTANCING
    return sampleInstancedTile(texCoord);
#else
#ifdef USE_TILESET_IMAGE
#ifdef HAS_COLOR_ARRAY
    return texture2DArray(m_ColorArray, vec3(getTileUVClampedAt(texCoord, vec2(0.0), m_TileSize.xy, m_ImageSize.xy), m_TileLayer));
#else
    return texture2D(m_ColorMap, getTileUVClampedAt(texCoord, v_TilePos, m_TileSize.xy, m_ImageSize.xy));
#endif
#else
    return texture2D(m_ColorMap, texCoord);
#endif
#endif
}

vec2 alphaOcclusionTexel() {
#ifdef INSTANCING
    return m_TileAlphaOcclusionRadius / max(v_UvSize, vec2(1.0));
#else
#ifdef USE_TILESET_IMAGE
    return m_TileAlphaOcclusionRadius / max(m_TileSize.xy, vec2(1.0));
#else
    return m_TileAlphaOcclusionRadius / max(m_ImageSize.xy, vec2(1.0));
#endif
#endif
}

vec4 applyTileAlphaOcclusion(vec4 color, vec2 texCoord) {
    float strength = clamp(m_TileAlphaOcclusionStrength, 0.0, 1.0);
    if (strength <= 0.0) {
        return color;
    }

    vec2 stepUv = alphaOcclusionTexel();
    float a = alphaFromSample(color);
    float minNeighbor = 1.0;
    float maxNeighbor = 0.0;

    float neighbor;
    neighbor = alphaFromSample(sampleBaseColor(texCoord + vec2(stepUv.x, 0.0)));
    minNeighbor = min(minNeighbor, neighbor);
    maxNeighbor = max(maxNeighbor, neighbor);
    neighbor = alphaFromSample(sampleBaseColor(texCoord + vec2(-stepUv.x, 0.0)));
    minNeighbor = min(minNeighbor, neighbor);
    maxNeighbor = max(maxNeighbor, neighbor);
    neighbor = alphaFromSample(sampleBaseColor(texCoord + vec2(0.0, stepUv.y)));
    minNeighbor = min(minNeighbor, neighbor);
    maxNeighbor = max(maxNeighbor, neighbor);
    neighbor = alphaFromSample(sampleBaseColor(texCoord + vec2(0.0, -stepUv.y)));
    minNeighbor = min(minNeighbor, neighbor);
    maxNeighbor = max(maxNeighbor, neighbor);
    neighbor = alphaFromSample(sampleBaseColor(texCoord + stepUv));
    minNeighbor = min(minNeighbor, neighbor);
    maxNeighbor = max(maxNeighbor, neighbor);
    neighbor = alphaFromSample(sampleBaseColor(texCoord + vec2(-stepUv.x, stepUv.y)));
    minNeighbor = min(minNeighbor, neighbor);
    maxNeighbor = max(maxNeighbor, neighbor);
    neighbor = alphaFromSample(sampleBaseColor(texCoord + vec2(stepUv.x, -stepUv.y)));
    minNeighbor = min(minNeighbor, neighbor);
    maxNeighbor = max(maxNeighbor, neighbor);
    neighbor = alphaFromSample(sampleBaseColor(texCoord - stepUv));
    minNeighbor = min(minNeighbor, neighbor);
    maxNeighbor = max(maxNeighbor, neighbor);

    if (a >= 0.5) {
        float edgeShade = (1.0 - minNeighbor) * strength * smoothstep(0.5, 1.0, a);
        color.rgb *= 1.0 - clamp(edgeShade, 0.0, 0.45);
    } else {
        float fringe = smoothstep(0.45, 0.95, maxNeighbor) * (1.0 - a) * strength * 0.65;
        color = vec4(0.0, 0.0, 0.0, fringe);
    }

    return color;
}
#endif

vec3 hueShift(vec3 color, float shift) {
    float angle = shift * 6.28318530718;
    float s = sin(angle);
    float c = cos(angle);
    mat3 weights = mat3(
        vec3(0.299, 0.587, 0.114),
        vec3(0.299, 0.587, 0.114),
        vec3(0.299, 0.587, 0.114)
    ) + mat3(
        vec3(0.701, -0.587, -0.114),
        vec3(-0.299, 0.413, -0.114),
        vec3(-0.300, -0.588, 0.886)
    ) * c + mat3(
        vec3(0.168, 0.330, -0.497),
        vec3(-0.328, 0.035, 0.292),
        vec3(1.250, -1.050, -0.203)
    ) * s;
    return clamp(color * weights, 0.0, 1.0);
}

void main(){
    vec4 color = vec4(1.0);

#if defined(HAS_COLOR_MAP) || defined(HAS_COLOR_ARRAY) || defined(INSTANCING)
    vec2 uv = v_TexCoord;

    #ifdef USE_TILESET_IMAGE
    #ifdef INSTANCING
    color = sampleInstancedTile(uv);
    #if defined(HAS_DECAL_MAP) || defined(HAS_DECAL_ARRAY)
    color = alphaOver(color, sampleInstancedDecal(v_Decal0));
    color = alphaOver(color, sampleInstancedDecal(v_Decal1));
    color = alphaOver(color, sampleInstancedDecal(v_Decal2));
    color = alphaOver(color, sampleInstancedDecal(v_Decal3));
    #endif
    #else
#ifdef HAS_COLOR_ARRAY
    uv = getTileUVClampedAt(v_TexCoord, vec2(0.0), m_TileSize.xy, m_ImageSize.xy);
    color = texture2DArray(m_ColorArray, vec3(uv, m_TileLayer));
#else
    uv = getTileUVClamped(v_TilePos, m_TileSize.xy, m_ImageSize.xy);
    color = texture2D(m_ColorMap, uv);
#endif
    #if defined(HAS_DECAL_MAP) || defined(HAS_DECAL_ARRAY)
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
    vec3 diff = color.rgb - m_TransColor.rgb;
    if (dot(diff, diff) < 0.0001) { // 0.01 * 0.01
        discard;
    }
    #endif

    #if defined(USE_TILE_ALPHA_OCCLUSION) && (defined(HAS_COLOR_MAP) || defined(HAS_COLOR_ARRAY) || defined(INSTANCING))
    color = applyTileAlphaOcclusion(color, v_TexCoord);
    #endif

    #ifdef USE_TILE_ALPHA_OCCLUSION
    if (color.a <= 0.01) {
        discard;
    }
    #else
    if (color.a < 0.5) {
        discard;
    }
    #endif
#endif

    #ifdef HAS_COLOR
    color *= m_Color;
    #endif

    #if defined(USE_TINT_COLOR) && defined(HAS_TINT_COLOR)
    color *= m_TintColor;
    #endif

    #ifdef HAS_HUE_SHIFT
    if (abs(m_HueShift) > 0.0001) {
        color.rgb = hueShift(color.rgb, m_HueShift);
    }
    #endif

    #ifdef HAS_LAYER_OPACITY
    color.a *= m_LayerOpacity;
    #endif

    #ifdef HAS_OPACITY
    color.a *= m_Opacity;
    #endif

    gl_FragColor = color;
}
