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

package org.ngengine.world2d.tiled.core;

import java.math.BigInteger;

import org.ngengine.components.ComponentManagerProvider;

import org.ngengine.world2d.tiled.components.TiledComponentManager;
import org.ngengine.world2d.tiled.components.TiledObjectSyncComponent;
import org.ngengine.world2d.tiled.components.TiledComponentReflectionMounting;
import org.ngengine.world2d.tiled.core.entity.TiledObjectEntity;
import org.ngengine.world2d.tiled.animation.Animation;
import org.ngengine.world2d.tiled.animation.Frame;
import org.ngengine.world2d.tiled.core.tileset.Tile;

/**
 * An entry that can be placed on a map
 * @author Riccardo Balbo
 */
public abstract class TiledEntity extends TiledBase implements ComponentManagerProvider {
    private TiledComponentManager componentManager;
    private Tile renderedTileOverride;
    private Tile animatedRenderedTile;
    private Tile animationTile;
    private Animation tileAnimation;
    private int animationFrameIndex;
    private float unusedAnimationTime;
    private int renderedTileUpdateNeeded = nextUpdateId();

    @Override
    public TiledComponentManager getComponentManager() {
        if(componentManager==null){
            componentManager = new TiledComponentManager();
            if (this instanceof TiledObjectEntity) {
                String syncComponentClass = null;
                Object syncComponentProp = getProperty("net.sync.component");
                if (syncComponentProp != null) {
                    syncComponentClass = String.valueOf(syncComponentProp).trim();
                }
                if (syncComponentClass != null && !syncComponentClass.isEmpty()) {
                    if (TiledComponentReflectionMounting.mountByClassName(componentManager, syncComponentClass, this) == null) {
                        componentManager.addComponent(new TiledObjectSyncComponent());
                        componentManager.enableComponent(TiledObjectSyncComponent.class);
                    }
                } else {
                    componentManager.addComponent(new TiledObjectSyncComponent());
                    componentManager.enableComponent(TiledObjectSyncComponent.class);
                }
            }
            TiledComponentReflectionMounting.mountFromProperty(this, componentManager);
        }
        return componentManager;
    }


    public abstract void removeFromLayer();
    protected void detached(){
        if(componentManager!=null){
            componentManager.notifyEntityDetached(this);
            componentManager.setEnabled(false);
        }
    }

    protected void attached(){
        if(componentManager!=null){
            componentManager.setEnabled(true);
        }
    }

  
    public abstract double getHeight();
    public abstract double getWidth();
    public abstract double getY();
    public abstract double getX();
    public abstract String getClazz();
    public abstract BigInteger getId();
    public abstract Tile getTile();

    /**
     * Returns the tile currently selected for rendering. This is independent
     * from the logical tile used for properties, components, and physics.
     *
     * @return the explicit visual override, current animation frame, or logical
     *         tile, in that order
     */
    public final Tile getRenderedTile() {
        if (renderedTileOverride != null) {
            return renderedTileOverride;
        }
        return animatedRenderedTile != null ? animatedRenderedTile : getTile();
    }

    /**
     * Overrides only the tile used for rendering. Passing the logical tile or
     * {@code null} clears the explicit override and reveals the current
     * animation frame, without changing the entity GID, inherited properties,
     * components, or collision source.
     *
     * @param tile tile to display, or {@code null} to display the logical tile
     */
    public final void setRenderedTile(Tile tile) {
        Tile previous = getRenderedTile();
        Tile logicalTile = getTile();
        Tile override = tile == null || tile == logicalTile ? null : tile;
        if (renderedTileOverride == override) {
            return;
        }
        renderedTileOverride = override;
        if (previous != getRenderedTile()) {
            renderedTileUpdateNeeded = nextUpdateId();
        }
    }

    /** @return update ID for render-tile-only changes */
    public final int getRenderedTileUpdateId() {
        return renderedTileUpdateNeeded;
    }

    /**
     * Advances the logical tile's default Tiled animation without changing the
     * logical tile. World management calls this once per simulation tick so
     * every render target observes the same frame.
     *
     * @param tpf elapsed seconds
     */
    public final void updateTileAnimation(float tpf) {
        Tile logicalTile = getTile();
        if (animationTile != logicalTile) {
            animationTile = logicalTile;
            tileAnimation = logicalTile != null && !logicalTile.getAnimations().isEmpty()
                    ? logicalTile.getAnimations().get(0) : null;
            animationFrameIndex = 0;
            unusedAnimationTime = 0f;
            applyAnimationFrame();
        }
        if (tileAnimation == null || tileAnimation.getTotalFrames() == 0) {
            setAnimatedRenderedTile(null);
            return;
        }

        Frame frame = tileAnimation.getFrame(animationFrameIndex);
        if (frame == null) {
            setAnimatedRenderedTile(null);
            return;
        }
        unusedAnimationTime += Math.max(0f, tpf) * 1000f;
        int remainingFrames = tileAnimation.getTotalFrames();
        while (frame.getDuration() > 0 && unusedAnimationTime >= frame.getDuration()
                && remainingFrames-- > 0) {
            unusedAnimationTime -= frame.getDuration();
            animationFrameIndex = (animationFrameIndex + 1) % tileAnimation.getTotalFrames();
            frame = tileAnimation.getFrame(animationFrameIndex);
            if (frame == null) {
                break;
            }
        }
        applyAnimationFrame();
    }

    /** Resets visual animation state after an actual logical tile swap. */
    protected final void logicalTileChanged() {
        renderedTileOverride = null;
        animatedRenderedTile = null;
        animationTile = null;
        tileAnimation = null;
        animationFrameIndex = 0;
        unusedAnimationTime = 0f;
        renderedTileUpdateNeeded = nextUpdateId();
    }

    private void applyAnimationFrame() {
        if (tileAnimation == null || animationTile == null || animationTile.getTileset() == null) {
            setAnimatedRenderedTile(null);
            return;
        }
        Frame frame = tileAnimation.getFrame(animationFrameIndex);
        Tile frameTile = frame != null ? animationTile.getTileset().getTile(frame.getTileId()) : null;
        setAnimatedRenderedTile(frameTile);
    }

    private void setAnimatedRenderedTile(Tile tile) {
        Tile previous = getRenderedTile();
        animatedRenderedTile = tile == getTile() ? null : tile;
        if (previous != getRenderedTile()) {
            renderedTileUpdateNeeded = nextUpdateId();
        }
    }
}
