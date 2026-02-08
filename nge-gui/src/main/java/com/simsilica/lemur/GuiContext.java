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

package com.simsilica.lemur;

import java.io.Closeable;
import java.lang.ref.WeakReference;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.ngengine.platform.NGEPlatform;

import com.jme3.asset.AssetManager;
import com.jme3.bounding.BoundingBox;
import com.jme3.bounding.BoundingSphere;
import com.jme3.bounding.BoundingVolume;
import com.jme3.collision.CollisionResult;
import com.jme3.collision.CollisionResults;
import com.jme3.math.Ray;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.jme3.renderer.Camera;
import com.jme3.renderer.ViewPort;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.renderer.queue.RenderQueue.Bucket;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.util.TempVars;
import com.simsilica.lemur.anim.AnimationHandler;
import com.simsilica.lemur.ime.DummyImeComposer;
import com.simsilica.lemur.ime.ImeComposer;
import com.simsilica.lemur.ime.ImeCompositionEvent;
import com.simsilica.lemur.nav.Navigator;
import com.simsilica.lemur.nav.PopupHandler;

public class GuiContext {
    private final static Logger log = Logger.getLogger(GuiContext.class.getName());

    private final WeakReference<ViewPort> vpRef;
    private final Set<Object> inputOwners = Collections.newSetFromMap(new IdentityHashMap<>());
    private final Navigator navigator;
    private final AnimationHandler animationHandler;
    private final PopupHandler popupHandler;
    private final OptionPanelState optionPanelHandler;
    private boolean gammaEnabled;
    private ImeComposer imeComposer = new DummyImeComposer();

    public static interface GuiContextHandler extends Closeable{
        public void update( float tpf );

    }

    public ImeComposer getImeComposer() {
        return imeComposer;
    }
    public void setImeComposer(ImeComposer keyboardHandler) {
        if(keyboardHandler == null)  keyboardHandler = new DummyImeComposer();
        this.imeComposer = keyboardHandler;
    }

    public ImeComposer openKeyboard(
        Consumer<ImeCompositionEvent> listener, 
        ImeCompositionEvent event,  
        Function<Character, Character> inputFilter,
        Function<String, Float> getLineWidth
    ) {
        imeComposer.open(this, listener, event, inputFilter, getLineWidth);
        return imeComposer;
    }


    public ImeComposer openKeyboard(        
        Consumer<ImeCompositionEvent> listener, 
        String currentTest, 
        Function<Character, Character> inputTransform, 
        boolean multiline,
        Function<String, Float> getLineWidth
    ) {
        imeComposer.open(this, listener, new ImeCompositionEvent(currentTest, multiline), inputTransform, getLineWidth);
        return imeComposer;
    }


    GuiContext(ViewPort vp,boolean sRGB) {
        this.vpRef = new WeakReference<>(vp);
        this.navigator = new Navigator(this);
        this.animationHandler = new AnimationHandler();
        this.popupHandler = new PopupHandler(getGuiNode(), getGuiCamera());
        this.optionPanelHandler = new OptionPanelState(popupHandler);
        this.gammaEnabled = sRGB;
        setupGuiComparators();
        NGEPlatform.get().registerFinalizer(vp, () -> {
            try{
                this.popupHandler.close();
            }catch(Exception e){
                log.log(Level.WARNING, "Error closing popup handler", e);
            }
            try{
                this.animationHandler.close();
            }catch(Exception e){
                log.log(Level.WARNING, "Error closing animation handler", e);
            }
            try{
                this.navigator.close();
            }catch(Exception e){
                log.log(Level.WARNING, "Error closing navigator", e);
            }
        });

    }

    public OptionPanelState getOptionPanelHandler() {
        return optionPanelHandler;
    }

    public boolean isSrgb(){
        return gammaEnabled;
    }

    public Camera getGuiCamera() {
        ViewPort view = vpRef.get();
        if (view == null) {
            throw new IllegalStateException("ViewPort has been garbage collected");
        }
        return view.getCamera();
    }

    public Node getGuiNode() {
        ViewPort view = vpRef.get();
        if (view == null) {
            throw new IllegalStateException("ViewPort has been garbage collected");
        }
        for (Spatial s : view.getScenes()) {
            if (s instanceof Node) {
                return (Node) s;
            }
        }
        throw new IllegalStateException("No gui node found in ViewPort: " + view);
    }   

    public AnimationHandler getAnimationHandler() {
        return animationHandler;
    }

    public PopupHandler getPopupHandler() {
        return popupHandler;
    }





    public AssetManager getAssetManager() {
        return NGEGui.getAssetManager();
    }

    private void setupGuiComparators() {
        ViewPort view = vpRef.get();
        if (view == null) return;
        RenderQueue rq = view.getQueue();

        rq.setGeometryComparator(Bucket.Opaque,
                new LayerComparator(rq.getGeometryComparator(Bucket.Opaque), -1));
        rq.setGeometryComparator(Bucket.Transparent,
                new LayerComparator(rq.getGeometryComparator(Bucket.Transparent), -1));
        rq.setGeometryComparator(Bucket.Translucent,
                new LayerComparator(rq.getGeometryComparator(Bucket.Translucent), -1));
        rq.setGeometryComparator(Bucket.Gui,
                new LayerComparator(rq.getGeometryComparator(Bucket.Gui), -1));
    }

    public void requestInputEnabled(Object owner) {
        if (owner == null) throw new IllegalArgumentException("owner cannot be null");
        boolean was = isInputEnabled();
        inputOwners.add(owner);
        boolean now = isInputEnabled();
        if (!was && now) onEnabled();
    }

    public void releaseInputEnabled(Object owner) {
        if (owner == null) throw new IllegalArgumentException("owner cannot be null");
        boolean was = isInputEnabled();
        inputOwners.remove(owner);
        boolean now = isInputEnabled();
        if (was && !now) onDisabled();
    }

    public Navigator getNavigator() {
        return navigator;
    }

    public ViewPort getViewPort() {
        return vpRef.get();
    }

    public boolean isInputEnabled() {
        return !inputOwners.isEmpty();
    }

    public void onEnabled() {
    }

    public void onDisabled() {
        navigator.unfocus(navigator.getFocus());
    }

    public void update(float tpf) {
        navigator.update(tpf);
        popupHandler.update(tpf);
        animationHandler.update(tpf);
        optionPanelHandler.update(tpf);
    }

    private CollisionResults pickResults = new CollisionResults();
    private Ray pickeRay = new Ray();

    public Spatial pick(double x, double y) {

        Spatial bestHit = null;
        float bestDistance = Float.POSITIVE_INFINITY;

        for (Spatial root : getViewPort().getScenes()) {
            if (root == null) continue;

            Ray ray = getPickRay(root, x, y, pickeRay);
            if (ray == null) continue;

            pickResults.clear();
            int count = root.collideWith(ray, pickResults);
            if (count <= 0) continue;

            for (CollisionResult cr : pickResults) {
                Spatial hit = cr.getGeometry();

                while (hit != null && !NGEGui.isFocusable(hit)) {
                    hit = hit.getParent();
                }

                if (hit != null && cr.getDistance() < bestDistance) {
                    bestDistance = cr.getDistance();
                    bestHit = hit;
                }
            }
        }

        pickResults.clear();
        return bestHit;
    }

    protected Ray getPickRay(Spatial root, double x, double y, Ray ray) {
        try (TempVars vars = TempVars.get()) {
            Vector2f cursor = vars.vect2d;
            cursor.x = (float) x;
            cursor.y = (float) y;
            Camera cam = getViewPort().getCamera();

            if (root.getQueueBucket() == Bucket.Gui) {
                float[] range = vars.fADdU;
                getZBounds(root, range);

                range[0] -= 1;
                range[1] += 1;
                // return new Ray(new Vector3f(cursor.x, cursor.y, range[1]), new Vector3f(0, 0, -1));
                Vector3f origin = vars.vect1;
                origin.x = cursor.x;
                origin.y = cursor.y;
                origin.z = range[1];
                ray.setOrigin(origin);
                Vector3f direction = vars.vect2;
                direction.x = 0;
                direction.y = 0;
                direction.z = -1;
                ray.setDirection(direction);
                return ray;
            }

            if (!viewContains(cam, cursor)) return null;

            Vector3f clickFar = cam.getWorldCoordinates(cursor, 1, vars.vect1);
            Vector3f clickNear = cam.getWorldCoordinates(cursor, 0, vars.vect2);
            Vector3f dir = vars.vect3.set(clickFar).subtractLocal(clickNear).normalizeLocal();
            if (!dir.isUnitVector()) {
                return null;
            }
            // return new Ray(clickNear, dir);
            ray.setOrigin(clickNear);
            ray.setDirection(dir);
            return ray;
        }
    }

    protected boolean viewContains(Camera cam, Vector2f cursor) {
        float x1 = cam.getViewPortLeft();
        float x2 = cam.getViewPortRight();
        float y1 = cam.getViewPortBottom();
        float y2 = cam.getViewPortTop();
        if (x1 == 0 && x2 == 1 && y1 == 0 && y2 == 1) return true;

        float x = cursor.x / cam.getWidth();
        float y = cursor.y / cam.getHeight();
        return !(x < x1 || x > x2 || y < y1 || y > y2);
    }

    protected void getZBounds(Spatial s, float out[]) {
        BoundingVolume bv = s.getWorldBound();
        if (bv == null) {
            out[0] = 0;
            out[1] = 1;
            return;
        }

        Vector3f c = bv.getCenter();
        if (bv instanceof BoundingBox) {
            BoundingBox bb = (BoundingBox) bv;
            // return new float[] { c.z - bb.getZExtent(), c.z + bb.getZExtent() };
            out[0] = c.z - bb.getZExtent();
            out[1] = c.z + bb.getZExtent();
        } else if (bv instanceof BoundingSphere) {
            BoundingSphere bs = (BoundingSphere) bv;
            // return new float[] { c.z - bs.getRadius(), c.z + bs.getRadius() };
            out[0] = c.z - bs.getRadius();
            out[1] = c.z + bs.getRadius();
        } else {
            throw new UnsupportedOperationException("Unsupported bounding volume: " + bv);
        }
    }

}