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

package io.github.jmecn.tiled.renderer;

import com.jme3.material.Material;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;

import io.github.jmecn.tiled.core.TiledMap;
import io.github.jmecn.tiled.core.entity.TiledObjectEntity;
import io.github.jmecn.tiled.math2d.Point;
import io.github.jmecn.tiled.renderer.shape.Diamond;

/**
 * Staggered render
 * 
 * @author yanmaoyuan
 * 
 */
public class StaggeredRenderer extends HexagonalRenderer {
 
    public StaggeredRenderer(TiledMap tiledMap, int PPM, Node rootNode) {
        super(tiledMap, PPM, rootNode);
    }

    @Override
    public Spatial createTileGrid(Material material) {
        // create a grid
        Diamond mesh = new Diamond(tiledMap.getTileWidth(), tiledMap.getTileHeight(), true);
        Geometry geom = new Geometry("TileGrid", mesh);
        geom.setMaterial(material);
        return geom;
    }

    /**
     * Converts screen to tile coordinates. Sub-tile return values are not
     * supported by this renderer.
     * This override exists because the method used by the HexagonalRenderer
     * does not produce nice results for isometric shapes in the tile corners.
     */
    @Override
    public void worldToTile(float x, float y, Point out) {
        float alignedX = x;
        float alignedY = y;
        if (staggerX) {
            alignedX -= staggerEven ? sideOffsetX : 0;
        } else {
            alignedY -= staggerEven ? sideOffsetY : 0;
        }

        // Start with the coordinates of a grid-aligned tile
        Point referencePoint = new Point(alignedX / tileWidth, alignedY / tileHeight);

        // Relative x and y position on the base square of the grid-aligned tile
        Point rel = new Point(
                alignedX - referencePoint.getX() * tileWidth,
                alignedY - referencePoint.getY() * tileHeight);

        // Adjust the reference point to the correct tile coordinates
        adjustReferencePoint(referencePoint);

        float yPos = rel.getX() * ((float) tileHeight / tileWidth);

        // Check whether the cursor is in any of the corners (neighboring tiles)
        if (sideOffsetY - yPos > rel.getY()) {
             topLeft(referencePoint.getX(), referencePoint.getY(), out);             
        } else if (-sideOffsetY + yPos > rel.getY()) {
             topRight(referencePoint.getX(), referencePoint.getY(), out);
        } else if (sideOffsetY + yPos < rel.getY()) {
             bottomLeft(referencePoint.getX(), referencePoint.getY(), out);
        } else if (sideOffsetY * 3 - yPos < rel.getY()) {
             bottomRight(referencePoint.getX(), referencePoint.getY(), out);
        } else {
            out.set(referencePoint.getX(), referencePoint.getY());
        }
    }

    @Override
    public float getTopDownYIndex(TiledObjectEntity obj) {
        float y = (float)obj.getY();
        float tileY = y / mapSize.getY();
        return (float) (tileY * layerDistance);
    }
}
