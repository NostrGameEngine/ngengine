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

import com.jme3.material.Material;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;

import org.ngengine.world2d.tiled.core.TiledMap;
import org.ngengine.world2d.tiled.renderer.shape.IsoGrid;
import org.ngengine.world2d.tiled.renderer.shape.IsoRect;
import org.ngengine.world2d.tiled.util.TiledCoordinateSystem;

/**
 * Isometric render
 * 
 * @author yanmaoyuan
 *
 */
public class IsometricRenderer extends MapRenderer {

    public IsometricRenderer(TiledMap tiledMap, int PPM, Node rootNode) {
        super(tiledMap, PPM, rootNode);
    }

    public IsometricRenderer(TiledMap tiledMap, int PPM, Node rootNode, TiledCoordinateSystem coordinateSystem) {
        super(tiledMap, PPM, rootNode, coordinateSystem);
    }

    @Override
    public Spatial createTileGrid(Material material) {
        // create a grid
        IsoRect mesh = new IsoRect(tileWidth, tileHeight, true);
        Geometry geom = new Geometry("TileGrid", mesh);
        geom.setMaterial(material);
        return geom;
    }

    @Override
    public void visitTiles(TileVisitor visitor) {
        int count = 0;
        for (int p = 0; p < height + width - 1; p++) {
            for (int y = 0; y <= p; y++) {
                int x = p - y;
                if (y < height && x < width) {
                    visitor.visit(x, y, count);
                    count++;
                }
            }
        }
    }

    @Override
    public void renderGrid(Node gridVisual, Material gridMaterial) {
        // add boundary
        IsoGrid grid = new IsoGrid(width, height, tileWidth, tileHeight);
        Geometry geom = new Geometry("Grid#Boundary", grid);
        geom.setMaterial(gridMaterial);
        gridVisual.attachChild(geom);
    }

}
