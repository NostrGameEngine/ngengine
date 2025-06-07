/**
 * Copyright (c) 2025, Nostr Game Engine
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
 * the BSD 3-Clause License. The original jMonkeyEngine license is as follows:
 */
package org.ngengine.demo.son.controls;

import com.jme3.anim.AnimComposer;
import com.jme3.anim.AnimLayer;
import com.jme3.anim.tween.action.Action;
import com.jme3.anim.tween.action.BaseAction;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.ViewPort;
import com.jme3.scene.Spatial;
import com.jme3.scene.control.AbstractControl;
import com.jme3.util.clone.Cloner;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.ngengine.demo.son.anim.AnimGroupController;
import org.ngengine.demo.son.anim.AnimTweenController;

public class BoatAnimationControl extends AbstractControl {

    private static final Logger logger = Logger.getLogger(BoatAnimationControl.class.getName());
    private Map<BaseAction, AnimTweenController> tweenControllers = new WeakHashMap<>();
    protected float sailFactor, windFactor, flagFactor;

    @Override
    public void cloneFields(Cloner cloner, Object original) {
        super.cloneFields(cloner, original);
    }

    protected AnimGroupController setAnimation(String anim, String layer) {
        AnimGroupController controller = new AnimGroupController();
        Spatial sp = getSpatial();
        sp.depthFirstTraversal(s -> {
            try {
                AnimComposer composer = s.getControl(AnimComposer.class);
                if (composer == null) return;
                if (!composer.getLayerNames().contains(layer)) {
                    composer.makeLayer(layer, null);
                }
                AnimLayer animLayer = composer.getLayer(layer);
                String currentAction = animLayer.getCurrentActionName();
                if (composer.getAnimClipsNames().contains(anim)) {
                    if (currentAction == null || !currentAction.equals(anim)) {
                        Action act = composer.action(anim);
                        AnimTweenController stepper = new AnimTweenController(act);
                        BaseAction action = new BaseAction(stepper);
                        animLayer.setCurrentAction(anim, action, false);
                        tweenControllers.put(action, stepper);
                        controller.add(stepper);
                    } else {
                        Action act = animLayer.getCurrentAction();
                        AnimTweenController stepper = tweenControllers.get(act);
                        if (stepper != null) controller.add(stepper);
                    }
                }
            } catch (Exception e) {
                logger.log(Level.WARNING, "Error setting animation: ", e);
            }
        });
        return controller;
    }

    public void setFlagAnim(float rot) {
        AnimGroupController flag = setAnimation("rotateFlag", "flag");
        flag.setStep(rot);
        flagFactor = rot;
    }

    public void setSailAnim(float fold, float force) {
        if (fold > 0.9f) {
            fold = 1f;
        }
        if (fold == 0f) {
            AnimGroupController sail = setAnimation("strongWind", "sail");
            sail.setStep(force);
        } else {
            AnimGroupController sail = setAnimation("sailScale", "sail");
            sail.setStep(fold);
        }
        sailFactor = fold;
        windFactor = force;
    }

    public float getSailFactor() {
        return sailFactor;
    }

    public float getWindFactor() {
        return windFactor;
    }

    public float getFlagFactor() {
        return flagFactor;
    }

    @Override
    protected void controlUpdate(float tpf) {
        // c.unsetStep();

        // setAnimation("foldFlag", "flag2");
        // setAnimation("sailScale", "sail");
        // setAnimation("strongWind", "wind");
        // Spatial sp = getSpatial();
        // sp.depthFirstTraversal(s -> {

        //     SkinningControl sc = s.getControl(SkinningControl.class);
        //     if (sc != null) {
        //         if (!sc.isHardwareSkinningPreferred()) {
        //              sc.setHardwareSkinningPreferred(true);
        //         }
        //     }

        //     AnimComposer composer = s.getControl(AnimComposer.class);
        //     if (composer != null) {
        //         AnimLayer flagLayer = composer.getLayer("flag");
        //         if(flagLayer == null) {
        //             composer.makeLayer("flag");
        //         }

        //         AnimLayer flagLayer2 = composer.getLayer("flag2");
        //         if (flagLayer == null) {
        //             composer.makeLayer("flag2");
        //         }

        //         AnimLayer sailLayer = composer.getLayer("sail");
        //         if (sailLayer == null) {
        //             composer.makeLayer("sail");
        //         }

        //         AnimLayer windLayer = composer.getLayer("wind");
        //         if (windLayer == null) {
        //             composer.makeLayer("wind");
        //         }

        //         if(!composer.getCurrentAction("flag").equals("rotateFlag")) {
        //             composer.setCurrentAction("rotateFlag", "flag");
        //         }
        //     }

        // });

    }

    @Override
    protected void controlRender(RenderManager rm, ViewPort vp) {}
}
