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

package org.ngengine.world2d.tiled.renderer.shape;

import com.jme3.app.SimpleApplication;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import com.jme3.system.AppSettings;
import org.ngengine.world2d.tiled.renderer.MaterialConst;
import org.ngengine.world2d.tiled.util.ObjectMesh;

public class TestIsoRect extends SimpleApplication {

    @Override
    public void simpleInitApp() {
        int tileWidth = 130;
        int tileHeight = 66;
        Mesh mesh = new Rect(66, 66, true);
        ObjectMesh.toIsometric(mesh, tileWidth, tileHeight);

        Material mat = new Material(assetManager, MaterialConst.TILED_J3MD);
        mat.setColor(MaterialConst.COLOR, ColorRGBA.Red);

        Geometry geom = new Geometry("rectangle", mesh);
        geom.move(tileWidth * 0.5f, 0, 0f);
        geom.setMaterial(mat);

        // grid
        IsoGrid grid = new IsoGrid(1, 1, tileWidth, tileHeight);
        Geometry gridGeom = new Geometry("grid", grid);
        Material gridMat = new Material(assetManager, MaterialConst.TILED_J3MD);
        gridMat.setColor(MaterialConst.COLOR, ColorRGBA.White);
        gridMat.getAdditionalRenderState().setPolyOffset(1f, 1f);
        gridGeom.setMaterial(gridMat);
        gridGeom.move(0f, 0f, 0f);
        rootNode.attachChild(gridGeom);

        viewPort.setBackgroundColor(ColorRGBA.DarkGray);
        rootNode.attachChild(geom);
        rootNode.scale(1/32f);
        
        flyCam.setMoveSpeed(10);
    }

    public static void main(String[] args) {
        TestIsoRect app = new TestIsoRect();
        app.setSettings(new AppSettings(true));
        app.start();
    }

}
