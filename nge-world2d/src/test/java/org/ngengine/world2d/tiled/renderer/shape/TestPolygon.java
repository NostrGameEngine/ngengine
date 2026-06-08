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

import java.util.ArrayList;
import java.util.List;

import com.jme3.app.SimpleApplication;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector2f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import com.jme3.system.AppSettings;
import org.ngengine.world2d.tiled.renderer.MaterialConst;

public class TestPolygon extends SimpleApplication {

    @Override
    public void simpleInitApp() {

        // points
        List<Vector2f> points = new ArrayList<Vector2f>();
        points.add(new Vector2f(0,0));
        points.add(new Vector2f(55,-23));
        points.add(new Vector2f(96,-117));
        points.add(new Vector2f(110,-61));
        points.add(new Vector2f(104,-42));
        points.add(new Vector2f(119,-33));
        points.add(new Vector2f(116,6));
        points.add(new Vector2f(104,9));
        points.add(new Vector2f(100,36));
        points.add(new Vector2f(60,43));
        points.add(new Vector2f(53,58));
        points.add(new Vector2f(43,58));
        points.add(new Vector2f(34,74));
        points.add(new Vector2f(21,69));
        points.add(new Vector2f(18,90));
        points.add(new Vector2f(0,89));
        
        Mesh mesh = new Polygon(points, false);
        
        Material mat = new Material(assetManager, MaterialConst.TILED_J3MD);
        mat.setColor(MaterialConst.COLOR, ColorRGBA.Red);

        Geometry geom = new Geometry("polygon", mesh);
        geom.setMaterial(mat);
        
        viewPort.setBackgroundColor(ColorRGBA.DarkGray);
        rootNode.attachChild(geom.scale(1/32f));
        
        flyCam.setMoveSpeed(10);
    }

    public static void main(String[] args) {
        TestPolygon app = new TestPolygon();
        app.setSettings(new AppSettings(true));
        app.start();
    }

}
