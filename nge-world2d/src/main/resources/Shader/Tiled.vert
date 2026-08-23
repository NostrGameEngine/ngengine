#import "Common/ShaderLib/GLSLCompat.glsllib"
#import "Common/ShaderLib/Instancing.glsllib"

#ifdef USE_TILE_POSITION
uniform vec2 m_TilePosition;
#endif
attribute vec3 inPosition;
attribute vec2 inTexCoord;

#ifdef USE_TILESET_IMAGE
// use texcoord2 as tile position
#ifdef INSTANCING
attribute vec4 inTexCoord2;
attribute vec4 inTexCoord3;
attribute vec4 inTexCoord4;
attribute vec2 inTexCoord5;
#if defined(HAS_DECAL_MAP) || defined(HAS_DECAL_ARRAY)
attribute vec4 inTexCoord6;
attribute vec4 inTexCoord7;
attribute vec4 inTexCoord8;
attribute vec4 inColor;
#endif
#else
attribute vec3 inTexCoord2;
#endif
// pass it to fragment shader
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
varying vec2 v_TilePos;
#endif
#endif

varying vec2 v_TexCoord;
#if defined(HAS_DECAL_MAP) || defined(HAS_DECAL_ARRAY)
varying vec2 v_DecalTexCoord;
#endif

void main() {
    v_TexCoord = inTexCoord;
#if defined(HAS_DECAL_MAP) || defined(HAS_DECAL_ARRAY)
    v_DecalTexCoord = inTexCoord;
#endif

#ifdef USE_TILESET_IMAGE
    #ifdef INSTANCING
    v_TileData = inTexCoord2;
    v_UvSize = inTexCoord5.xy;
    v_ImageSize = inTexCoord4.zw;
#if defined(HAS_DECAL_MAP) || defined(HAS_DECAL_ARRAY)
    v_Decal0 = inTexCoord6;
    v_Decal1 = inTexCoord7;
    v_Decal2 = inTexCoord8;
    v_Decal3 = inColor;
    #endif
    float flags = v_TileData.w;
    if (mod(floor(flags / 4.0), 2.0) >= 1.0) {
        v_TexCoord = vec2(1.0 - v_TexCoord.y, 1.0 - v_TexCoord.x);
    }
    if (mod(floor(flags), 2.0) >= 1.0) {
        v_TexCoord.x = 1.0 - v_TexCoord.x;
    }
    if (mod(floor(flags / 2.0), 2.0) >= 1.0) {
        v_TexCoord.y = 1.0 - v_TexCoord.y;
    }
    #elif defined(USE_TILE_POSITION)
    v_TilePos = m_TilePosition;
    #else
    v_TilePos = inTexCoord2.xy;
    #endif
#endif

    vec3 position = inPosition;
#if defined(INSTANCING) && defined(USE_TILESET_IMAGE)
    position.x = position.x * inTexCoord3.x + inTexCoord4.x + inTexCoord3.z;
    position.z = position.z * inTexCoord3.y + inTexCoord4.y + inTexCoord3.w;
#endif
    vec4 modelSpacePos = vec4(position, 1.0);

    gl_Position = TransformWorldViewProjection(modelSpacePos);
}
