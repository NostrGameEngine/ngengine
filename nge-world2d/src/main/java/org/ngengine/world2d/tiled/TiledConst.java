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

package org.ngengine.world2d.tiled;

/**
 * Constants
 *
 * @author yanmaoyuan
 */
public final class TiledConst {
    private TiledConst() {
    }

    public static final String EMPTY = "";
    public static final String TMX_EXTENSION = "tmx";// tiled map xml file
    public static final String TSX_EXTENSION = "tsx";// tileset xml file
    public static final String TX_EXTENSION = "tx";// object template xml file
    public static final String MAP = "map";
    public static final String NAME = "name";
    public static final String VALUE = "value";
    public static final String ORIENTATION = "orientation";
    public static final String RENDER_ORDER = "renderorder";
    public static final String COMPRESSION_LEVEL = "compressionlevel";
    public static final String TILE_WIDTH = "tilewidth";
    public static final String TILE_HEIGHT = "tileheight";
    public static final String MARGIN = "margin";
    public static final String SPACING = "spacing";
    public static final String TILE_COUNT = "tilecount";
    public static final String COLUMNS = "columns";
    public static final String HEX_SIDE_LENGTH = "hexsidelength";
    public static final String STAGGER_AXIS = "staggeraxis";
    public static final String STAGGER_INDEX = "staggerindex";
    public static final String PARALLAX_ORIGIN_X = "parallaxoriginx";
    public static final String PARALLAX_ORIGIN_Y = "parallaxoriginy";
    public static final String BACKGROUND_COLOR = "backgroundcolor";
    public static final String NEXT_LAYER_ID = "nextlayerid";
    public static final String NEXT_OBJECT_ID = "nextobjectid";
    public static final String INFINITE = "infinite";
    public static final String VERSION = "version";
    public static final String TILED_VERSION = "tiledversion";
    public static final String CLASS = "class";
    public static final String TYPE = "type";
    public static final String COLOR = "color";
    // tilesets
    public static final String TILESET = "tileset";
    public static final String SOURCE = "source";
    public static final String FIRST_GID = "firstgid";
    public static final String OBJECT_ALIGNMENT = "objectalignment";
    public static final String TILE_RENDER_SIZE = "tilerendersize";
    public static final String FILL_MODE = "fillmode";
    public static final String IMAGE = "image";
    public static final String TRANS = "trans";
    public static final String FORMAT = "format";
    public static final String TILE_OFFSET = "tileoffset";
    public static final String GRID = "grid";
    // tileset-transformations
    public static final String TRANSFORMATIONS = "transformations";
    public static final String H_FLIP = "hvlip";
    public static final String V_FLIP = "vflip";
    public static final String ROTATE = "rotate";
    public static final String PREFER_UNTRANSFORMED = "preferuntransformed";
    public static final String TEMPLATE = "template";
    public static final String GID = "gid";
    public static final String TILE = "tile";
    public static final String ID = "id";
    public static final String X = "x";
    public static final String Y = "y";
    public static final String WIDTH = "width";
    public static final String HEIGHT = "height";
    public static final String OPACITY = "opacity";
    public static final String LOCKED = "locked";
    public static final String TINT_COLOR = "tintcolor";
    public static final String PROBABILITY = "probability";
    public static final String VISIBLE = "visible";
    public static final String OFFSET_X = "offsetx";
    public static final String OFFSET_Y = "offsety";
    public static final String PARALLAX_X = "parallaxx";
    public static final String PARALLAX_Y = "parallaxy";
    public static final String ROTATION = "rotation";
    public static final String DATA = "data";
    public static final String TEXT_EMPTY = "#text";
    // properties
    public static final String PROPERTIES = "properties";
    public static final String PROPERTY = "property";
    public static final String PROPERTY_TYPE = "propertytype";
    public static final String LAYER = "layer";
    public static final String IMAGELAYER = "imagelayer";
    public static final String REPEAT_X = "repeatx";
    public static final String REPEAT_Y = "repeaty";
    // objects
    public static final String OBJECTGROUP = "objectgroup";
    public static final String DRAW_ORDER = "draworder";
    public static final String POINT = "point";
    public static final String OBJECT = "object";
    public static final String POLYLINE = "polyline";
    public static final String POLYGON = "polygon";
    public static final String ELLIPSE = "ellipse";
    public static final String TEXT = "text";
    public static final String GROUP = "group";
    // wangsets
    public static final String WANGSETS = "wangsets";
    public static final String WANGSET = "wangset";
    public static final String WANGCOLOR = "wangcolor";
    public static final String WANGTILE = "wangtile";
    public static final String WANGID = "wangid";
    // terrain types
    public static final String TERRAIN_TYPES = "terraintypes";
    public static final String TERRAIN = "terrain";
    // tile animation
    public static final String ANIMATION = "animation";
    public static final String FRAME = "frame";
    public static final String TILE_ID = "tileid";
    public static final String DURATION = "duration";
    public static final String OBJECT_GROUP = "objectgroup";
}
