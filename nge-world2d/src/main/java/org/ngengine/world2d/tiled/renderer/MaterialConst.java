/**
 * Copyright (c) 2025-2026, Nostr Game Engine
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 * 
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 
 * 3. Neither the name of the copyright holder nor the names of its
 *    contributors may be used to endorse or promote products derived from
 *    this software without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 * 
 * Nostr Game Engine is a fork of the jMonkeyEngine, which is licensed under
 * the BSD 3-Clause License. 
 */

package org.ngengine.world2d.tiled.renderer;

import com.jme3.math.ColorRGBA;

/**
 * desc:
 *
 * @author yanmaoyuan
 */
public final class MaterialConst {

    private MaterialConst() {
    }

    public static final String TILED_J3MD = "Shader/Tiled.j3md";

    // uniforms
    public static final String COLOR_MAP = "ColorMap";
    public static final String COLOR_MAP_0 = "ColorMap0";
    public static final String COLOR_MAP_1 = "ColorMap1";
    public static final String COLOR_MAP_2 = "ColorMap2";
    public static final String COLOR_MAP_3 = "ColorMap3";
    public static final String COLOR_ARRAY_0 = "ColorArray0";
    public static final String COLOR_ARRAY_1 = "ColorArray1";
    public static final String COLOR_ARRAY_2 = "ColorArray2";
    public static final String COLOR_ARRAY_3 = "ColorArray3";
    public static final String DECAL_MAP = "DecalMap";
    public static final String DECAL_IMAGE_SIZE = "DecalImageSize";
    public static final String DECAL_TILE_SIZE = "DecalTileSize";
    public static final String DECAL_0 = "Decal0";
    public static final String DECAL_1 = "Decal1";
    public static final String DECAL_2 = "Decal2";
    public static final String DECAL_3 = "Decal3";
    public static final String COLOR = "Color";
    public static final String TRANS_COLOR = "TransColor";
    public static final String USE_TINT_COLOR = "UseTintColor";
    public static final String TINT_COLOR = "TintColor";

    public static final String IMAGE_SIZE = "ImageSize";
    public static final String TILE_SIZE = "TileSize";
    public static final String USE_TILESET_IMAGE = "UseTilesetImage";
    public static final String USE_INSTANCING = "UseInstancing";
    public static final String TILE_POSITION = "TilePosition";
    public static final String USE_TILE_POSITION = "UseTilePosition";

    public static final String OPACITY = "Opacity";
    public static final String LAYER_OPACITY = "LayerOpacity";

    public static final ColorRGBA CURSOR_AVAILABLE_COLOR = new ColorRGBA(0.7f, 0.7f, 0.9f, 0.5f);
    public static final ColorRGBA CURSOR_UNAVAILABLE_COLOR = new ColorRGBA(0.8f, 0.2f, 0.2f, 0.5f);
}
