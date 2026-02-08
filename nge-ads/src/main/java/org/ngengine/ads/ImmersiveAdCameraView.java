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
 * the BSD 3-Clause License. 
 */

package org.ngengine.ads;

import com.jme3.collision.Collidable;
import com.jme3.collision.CollisionResult;
import com.jme3.math.Ray;
import com.jme3.renderer.Camera;
import com.jme3.renderer.Camera.FrustumIntersect;
import java.time.Duration;
import org.ngengine.components.ComponentManager;
import org.ngengine.gui.win.NToast;
import org.ngengine.gui.win.NToast.ToastType;
import org.ngengine.gui.win.NWindowManagerComponent;
import org.ngengine.nostrads.protocol.AdBidEvent;
import org.ngengine.nostrads.protocol.types.AdActionType;
import org.ngengine.picker.CameraPicker;
import org.ngengine.picker.CameraPickerBehavior;
import org.ngengine.picker.CloserVisibleCameraPickerBehavior;

public class ImmersiveAdCameraView implements ImmersiveAdViewer {

    private final Camera cam;
    private float maxDistance = 2100.0f;
    private ComponentManager mng;
    private NToast currentToast;
    private CameraPickerBehavior pickerBehavior;
    private ImmersiveAdSpace lastSpace;
    private String calloutPrefix = "Press action to";

    public ImmersiveAdCameraView(ComponentManager mng, Camera cam) {
        this.cam = cam;
        this.mng = mng;
    }

    public void setMaxDistance(float maxDistance) {
        this.maxDistance = maxDistance;
        this.pickerBehavior = null;
    }

    public void setCalloutPrefix(String calloutPrefix) {
        this.calloutPrefix = calloutPrefix;
    }

    @Override
    public boolean isVisible(ImmersiveAdSpace space) {
        FrustumIntersect frustumState = cam.contains(space.getBounds());
        if (frustumState == FrustumIntersect.Outside) return false;
        return true;
    }

    @Override
    public void showInfo(ImmersiveAdSpace space, String description, String callToAction) {
        NWindowManagerComponent windowManager = mng.getComponent(NWindowManagerComponent.class);
        if (windowManager != null && lastSpace != space) {
            if (currentToast != null) {
                currentToast.removeFromParent();
            }
            if (space != null) {
                AdActionType actionType = space.get().getActionType();
                if(actionType==AdActionType.ATTENTION){
                    space.confirm();
                }
                NToast toast = windowManager.showToast(
                    ToastType.INFO,
                    description + "\n    " + calloutPrefix + " " + callToAction,
                    Duration.ofMinutes(1));

                toast.addActionListener(id -> {
                    space.openLink();
                    if(actionType==AdActionType.ACTION){
                        space.confirm();
                    }
                });
                currentToast = toast;
                    
            }
        }
        lastSpace = space;
    }

    @Override
    public void beginUpdate() {}

    @Override
    public void endUpdate() {}

    private ImmersiveAdSpace bestSpace;
    private float bestDistance = Float.MAX_VALUE;

    @Override
    public void beginSelection() {
        bestSpace = null;
        bestDistance = Float.MAX_VALUE;
    }

    @Override
    public void select(ImmersiveAdSpace space) {
        if (pickerBehavior == null) {
            pickerBehavior =
                new CloserVisibleCameraPickerBehavior() {
                    @Override
                    public Ray tweakRay(Collidable rootNode, Camera cam, Ray ray) {
                        ray.setLimit(maxDistance);
                        return ray;
                    }
                };
        }

        AdBidEvent bid = space.get();
        if(bid.getActionType()==AdActionType.VIEW){
            space.confirm();
        }

        CollisionResult res = CameraPicker.pick(space.getBounds(), cam, pickerBehavior);
        if (res != null && (bestSpace == null || res.getDistance() < bestDistance)) {
            bestDistance = res.getDistance();
            bestSpace = space;
        }
    }

    @Override
    public ImmersiveAdSpace endSelection() {
        return bestSpace;
    }
}
