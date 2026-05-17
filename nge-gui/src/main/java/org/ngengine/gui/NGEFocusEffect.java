/**
 * Copyright (c) 2026, Nostr Game Engine
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
 * 
 * #########################################
 * 
 * nge-gui is built and based on Lemur, which is licensed under the BSD 3-Clause License.
 * - Copyright (c) 2012-2026 jMonkeyEngine All rights reserved. 
 * - Copyright (c) 2016-2026, Simsilica, LLC All rights reserved.
 * 
 * https://github.com/jMonkeyEngine-Contributions/Lemur
 */

package org.ngengine.gui;

import java.util.WeakHashMap;

import com.jme3.bounding.BoundingBox;
import com.jme3.material.Material;
import com.jme3.math.Vector3f;
import com.jme3.renderer.ViewPort;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.shape.Quad;
import com.jme3.util.TempVars;
import org.ngengine.gui.anim.Animation;
import org.ngengine.gui.effect.AbstractEffect;
import org.ngengine.gui.effect.EffectInfo;

public class NGEFocusEffect extends AbstractEffect<Panel> {
    private final boolean onFocus;
    private final static ThreadLocal<WeakHashMap<Panel,State>> states = ThreadLocal.withInitial(WeakHashMap::new);
    private final Material highlightMat;
    private final static class State{
        Geometry overlay;
        boolean removed = false;
    }

    protected State getState(Panel target){
        WeakHashMap<Panel,State> map = states.get();
        return map.compute(target,(k,v)->{
            if(v==null||v.removed){
                v = new State();
            }
            return v;
        });
    }

    protected void clearState(State state){
        if(state.overlay != null) {
            state.overlay.removeFromParent();
            state.removed = true;
            state.overlay = null;
        }
    }

    public NGEFocusEffect(boolean onFocus, Material highlightMat){ 
        super("focus");
        this.onFocus = onFocus;
        this.highlightMat = highlightMat;
    }   

    @Override
    public Animation create(Panel target, EffectInfo existing) {
        return new Animation(){
     
            @Override
            public boolean animate(double tpf) {
                if(onFocus){
                    Node overlay = getOverlay(target);
                    if(overlay==null) return true;
                    State state = getState(target);
                    update(state, overlay, target);
                } else {
                    State state = getState(target);
                    clearState(state);
                }
                return true;
            }

            @Override
            public void cancel() {
              
            }
        };
    }

    protected Node getOverlay(Spatial target){
        GuiContext vs = NGEGui.get(target);
        if (vs==null) return null;
        ViewPort vp = vs.getViewPort();
        Node overlayScene = (Node) vp.getScenes().get(0);
        return overlayScene;
    }


    protected void update(State state, Node overlayScene, Spatial target){
 
        if(state.overlay == null){
            Quad quad = new Quad(1,1);
            Geometry geo = new Geometry("focusOverlay", quad);
            geo.setMaterial(highlightMat);
            overlayScene.attachChild(geo);
            state.overlay = geo;            
        }           
        
        if(state.overlay!=null){
            BoundingBox box = (BoundingBox) target.getWorldBound();
            try(TempVars vars = TempVars.get()){
                Vector3f scale = vars.vect1;
                Vector3f translation = vars.vect2;

                scale.set(box.getXExtent()*2, box.getYExtent()*2, 1);
                translation.set( box.getCenter().x - box.getXExtent(), box.getCenter().y - box.getYExtent(), box.getCenter().z + 0.1f);
                
                overlayScene.worldToLocal(translation, translation);
                scale.divideLocal(overlayScene.getWorldScale());

                state.overlay.setLocalScale(scale);
                state.overlay.setLocalTranslation(translation);
            }
        }
        
    }



  
}
