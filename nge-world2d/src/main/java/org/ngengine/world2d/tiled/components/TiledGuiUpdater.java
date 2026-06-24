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

package org.ngengine.world2d.tiled.components;


import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.function.Supplier;
import java.util.logging.Logger;

import org.ngengine.components.Component;
import org.ngengine.components.ComponentManager;
import org.ngengine.components.runners.ComponentInitializer;
import org.ngengine.components.runners.ComponentUpdater;
import org.ngengine.gui.GuiContext;
import org.ngengine.gui.NGEGui;
import org.ngengine.gui.guix.containers.NPanel;
import org.ngengine.gui.guix.win.NWindowManager;
import org.ngengine.gui.guix.win.NWindowManagerComponent;
import org.ngengine.world2d.PovRenderer;
import org.ngengine.world2d.TiledWorld2d;

import com.jme3.bounding.BoundingBox;
import com.jme3.bounding.BoundingSphere;
import com.jme3.bounding.BoundingVolume;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.jme3.renderer.Camera;
import com.jme3.renderer.ViewPort;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.util.TempVars;
import org.ngengine.world2d.tiled.components.fragments.TiledGuiFragment;
import org.ngengine.world2d.tiled.core.TiledBase;
import org.ngengine.world2d.tiled.core.TiledLayer;
import org.ngengine.world2d.tiled.core.TiledTileContainer;
import org.ngengine.world2d.tiled.core.entity.TiledObjectEntity;
import org.ngengine.world2d.tiled.core.entity.TiledTileEntity;

/**
 * Updates GUI fragments anchored to tiled world entries for every registered POV.
 */
public class TiledGuiUpdater implements ComponentUpdater, ComponentInitializer {
    private final Logger logger = Logger.getLogger(getClass().getName());
    private final Supplier<TiledBase> entrySupplier;
  
 
    /**
     * Per-POV state passed to a {@link TiledGuiFragment}.
     */
    public static class GuiFragmentContext {
        long updateId = 0;
        private final NWindowManager wmng;
        private final ViewPort vp;
        private final NPanel panel;
        private Vector2f padding = new Vector2f();
        private Vector2f size = new Vector2f();

        // private float bbX0, bbY0, bbX1, bbY1;
        GuiFragmentContext(NWindowManager wmng, ViewPort vp, NPanel panel){
            this.wmng = wmng;
            this.vp = vp;
            this.panel = panel;
        }

        

        /**
         * Returns the extra screen-space offset applied after automatic anchoring.
         *
         * @return mutable padding vector in GUI pixels
         */
        public Vector2f getPadding(){
            return padding;
        }

        /**
         * Returns the projected world-space size of the owner spatial in GUI pixels.
         *
         * @return mutable size vector in GUI pixels
         */
        public Vector2f getSize(){
            return size;
        }

        /**
         * Sets the screen-space offset applied after automatic anchoring.
         *
         * @param x horizontal offset in GUI pixels
         * @param y vertical offset in GUI pixels
         */
        public void setPadding(float x, float y){
            this.padding.set(x, y);
        }

        /**
         * Sets the horizontal screen-space offset.
         *
         * @param x horizontal offset in GUI pixels
         */
        public void setPaddingX(float x){
            this.padding.x = x;
        }

        /**
         * Sets the vertical screen-space offset.
         *
         * @param y vertical offset in GUI pixels
         */
        public void setPaddingY(float y){
            this.padding.y = y;
        }

        /**
         * Returns the root panel owned by this GUI fragment.
         *
         * @return the fragment content panel
         */
        public NPanel getContent(){
            return panel;
        }

        /**
         * Returns the window manager associated with this context's GUI viewport.
         *
         * @return the window manager
         */
        public NWindowManager getWindowManager(){
            return wmng;
        }

        /**
         * Returns the GUI viewport that displays this context.
         *
         * @return the GUI viewport
         */
        public ViewPort getViewPort(){
            return vp;
        }

    }

    /**
     * Creates an updater for GUI fragments anchored to the supplied tiled entry.
     *
     * @param entrySupplier supplies the current layer, tile, or object that owns the
     *        GUI fragment
     */
    public TiledGuiUpdater(
        Supplier<TiledBase> entrySupplier
    ){
        this.entrySupplier = entrySupplier;
    }

 

    @Override
    public boolean canUpdate(ComponentManager fragmentManager, Component component) {
        return component instanceof TiledGuiFragment;
    }



    private final Map<TiledGuiFragment, Map<PovRenderer, GuiFragmentContext>> worldGuiFragmentCache = new HashMap<>();

    private void updateForPov(  Map<PovRenderer, GuiFragmentContext> cache , ComponentManager mng,  PovRenderer renderer, TiledGuiFragment frag,  long id) {
        if (!(frag instanceof TiledGuiFragment)) return;
        TiledBase entry = entrySupplier.get();
        if (entry == null) {
            return;
        }

        ViewPort guiVp = renderer.getGuiViewPort();
        ViewPort sceneVp = renderer.getSceneViewPort();
        if (guiVp == null || sceneVp == null) return;

        TiledWorld2d world = mng.getInstanceOf(TiledWorld2d.class);
        if (world == null) {
            return;
        }

        
        GuiFragmentContext fd = cache.get(renderer);

        // init if needed
        if (fd == null) {

            NPanel content = new NPanel();
            Node guiNode = world.getRenderTarget(renderer).getWorldGuiNode();
            if (guiNode != null) {
                guiNode.attachChild(content);
                NWindowManagerComponent wmgc = mng.getInstanceOf(NWindowManagerComponent.class);
                if (wmgc == null) {
                    content.removeFromParent();
                    return;
                }
                NWindowManager wm = wmgc.getManager(guiVp);
                fd = new GuiFragmentContext(wm, guiVp, content);
                cache.put(renderer, fd);
                frag.rebuildGuiFragment(mng, renderer, fd);
            }
        }
        if (fd == null) {
            return;
        }

        fd.updateId = id;

    
        Camera sceneCam = sceneVp.getCamera();

        NPanel panel = fd.getContent();

        

        try (TempVars vars = TempVars.get()) {
            Camera guiCamera = fd.getViewPort().getCamera();

            Vector3f pos = vars.vect1;
            pos.set(0, 0, 0);

            TiledLayer layer = null;
            if (entry instanceof TiledLayer) {
                layer = (TiledLayer) entry;
            } else if (entry instanceof TiledObjectEntity) {
                layer = ((TiledObjectEntity) entry).getObjectGroup();
            } else if (entry instanceof TiledTileEntity) {
                TiledTileContainer cnt = ((TiledTileEntity) entry).getContainer();
                if (cnt instanceof TiledLayer) {
                    layer = (TiledLayer) cnt;
                }
            }

            if (layer != null) {
                Spatial sp = world.getRenderTarget(renderer).getRenderer().getSpatial(layer, entry);
                if (sp != null) {

                    BoundingVolume bv = sp.getWorldBound();
                    Vector3f renderWp;

                    if (bv != null) {
                        renderWp = bv.getCenter();
                        if (bv instanceof BoundingSphere) {
                            float radius = ((BoundingSphere) bv).getRadius();
                            fd.size.x= radius*2;
                            fd.size.y= radius*2;
                        } else if (bv instanceof BoundingBox) {
                            Vector3f extent = vars.vect2;
                            ((BoundingBox) bv).getExtent(extent);
                            fd.size.x= extent.x*2;
                            fd.size.y= extent.z*2;
                        }
                    } else {
                        renderWp = sp.getWorldTranslation();
                    }

                    sceneCam.getScreenCoordinates(renderWp, pos);

                    GuiContext guiContext = NGEGui.isRegistered(guiVp) ? NGEGui.get(guiVp) : null;
                    if (guiContext != null && guiContext.isRelativeSize()) {
                        fd.size.x = guiContext.toGuiDeltaX(fd.size.x);
                        fd.size.y = guiContext.toGuiDeltaY(fd.size.y);
                        pos.x = guiContext.toGuiX(pos.x);
                        pos.y = guiContext.toGuiY(pos.y);
                    } else {
                        float scaleX = guiCamera.getWidth() / (float) sceneCam.getWidth();
                        float scaleY = guiCamera.getHeight() / (float) sceneCam.getHeight();

                        fd.size.x *= scaleX;
                        fd.size.y *= scaleY;

                        pos.x *= scaleX;
                        pos.y *= scaleY;
                    }
                }
            }

            Vector3f panelSize = panel.getSize();
            if (panelSize == null || panelSize.length() == 0) {
                panelSize = panel.getPreferredSize();
            }
            float targetX = pos.x - panelSize.x * 0.5f;
            float targetY = pos.y - panelSize.y * 0.5f;
            panel.setLocalTranslation(targetX, targetY, 0);
        }

        frag.renderGuiFragmentData(mng, renderer, fd);
        Vector3f pos = panel.getLocalTranslation();
        pos.y+=fd.padding.y;
        pos.x+=fd.padding.x;
        panel.setLocalTranslation(pos);
    }
 

    @Override
    public int initialize(ComponentManager mng, Component fragment, Runnable markReady) {
        return 0;
    } 


    long updateCounter = 0;

   

    @Override
    public void update(ComponentManager mng, Component component, float tpf) {
       
    }

    @Override
    public void cleanup(ComponentManager mng, Component fragment) {
        if(fragment instanceof TiledGuiFragment){
            Map<PovRenderer, GuiFragmentContext> fd = worldGuiFragmentCache.remove((TiledGuiFragment) fragment);
            if(fd!=null){
                for(GuiFragmentContext data : fd.values()){
                    data.getContent().removeFromParent();
                }
            }
        }
        
    }


    @Override
    public void render(ComponentManager mng, Component component) {
        TiledBase entry = entrySupplier.get();
        if(entry==null){
            return;
        }
        

        if (component instanceof TiledGuiFragment) {
            updateCounter++;
            TiledWorld2d world = component.getComponentManager().getInstanceOf(TiledWorld2d.class);
            if (world == null) {
                return;
            }
    
            Map<PovRenderer, GuiFragmentContext> cache = worldGuiFragmentCache.computeIfAbsent((TiledGuiFragment)component, f -> new HashMap<>());

            Iterator<PovRenderer> it = world.getPovRenderers().iterator();
            while(it.hasNext()){
                PovRenderer pov = it.next();
                if(pov == null){
                    continue;
                }
                updateForPov(cache, mng, pov, (TiledGuiFragment) component, updateCounter);
            }
            
            Iterator<Map.Entry<PovRenderer, GuiFragmentContext>> cacheIt = cache.entrySet().iterator();
            while(cacheIt.hasNext()){
                Map.Entry<PovRenderer, GuiFragmentContext> e = cacheIt.next();
                GuiFragmentContext fd = e.getValue();
                if(fd.updateId != updateCounter){
                    logger.fine("Removing fragment for pov " + e.getKey()
                            + " due to missing update. Last update id: " + fd.updateId
                            + ", current update id: " + updateCounter);
                    fd.getContent().removeFromParent();
                    cacheIt.remove();
                }
            }
        }
        
    }

  
    @Override
    public void afterRender(ComponentManager componentManager, Component component) {
       
    }

    @Override
    public void afterUpdate(ComponentManager componentManager, Component component) {
   
    }





    @Override
    public boolean canInitialize(ComponentManager mng, Component fragment) {
        NWindowManagerComponent wmng = mng.getInstanceOf(NWindowManagerComponent.class);
        if(wmng == null ) return false;
        return fragment instanceof TiledGuiFragment;
    }
}
