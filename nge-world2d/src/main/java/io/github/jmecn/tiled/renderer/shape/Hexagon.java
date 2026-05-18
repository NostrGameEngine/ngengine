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

package io.github.jmecn.tiled.renderer.shape;

import com.jme3.math.Vector2f;
import io.github.jmecn.tiled.enums.StaggerAxis;

import java.util.ArrayList;
import java.util.List;

/**
 * This is the basic grid shape in hexagonal map. The origin point is in the top-left corner.
 *
 * <pre>
 * O-/----\---X
 * |/      \
 * |\      /
 * | \____/
 * |
 * Y
 *</pre>
 * @author yanmaoyuan
 */
public class Hexagon extends Polygon {

    public Hexagon(int mapTileWidth, int mapTileHeight, int hexSideLength, StaggerAxis staggerAxis, boolean fill) {
        int sideLengthX = 0;
        int sideLengthY = 0;
        boolean isStaggerX = staggerAxis == StaggerAxis.X;
        if (isStaggerX) {
            sideLengthX = hexSideLength;
        } else {
            sideLengthY = hexSideLength;
        }

        int sideOffsetX = (mapTileWidth - sideLengthX) / 2;
        int sideOffsetY = (mapTileHeight - sideLengthY) / 2;

        int columnWidth = sideOffsetX + sideLengthX;
        int rowHeight = sideOffsetY + sideLengthY;

        int tileWidth = columnWidth + sideOffsetX;
        int tileHeight = rowHeight + sideOffsetY;

        List<Vector2f> polygon = new ArrayList<>(6);
        polygon.add(new Vector2f(0, rowHeight));
        if (!isStaggerX) {
            polygon.add(new Vector2f(0, sideOffsetY));
        }
        polygon.add(new Vector2f(sideOffsetX, 0));
        if (isStaggerX) {
            polygon.add(new Vector2f(columnWidth, 0));
        }
        polygon.add(new Vector2f(tileWidth, sideOffsetY));
        if (!isStaggerX) {
            polygon.add(new Vector2f(tileWidth, rowHeight));
        }
        polygon.add(new Vector2f(columnWidth, tileHeight));
        if (isStaggerX) {
            polygon.add(new Vector2f(sideOffsetX, tileHeight));
        }

        if (fill) {
            fill(polygon);
        } else {
            polyline(polygon, true);
        }
    }
}
