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

package org.ngengine.world2d.tiled.animation;

import com.jme3.math.Vector2f;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.ViewPort;
import com.jme3.scene.Geometry;
import com.jme3.scene.control.AbstractControl;

import org.ngengine.world2d.tiled.core.tileset.Tile;
import org.ngengine.world2d.tiled.renderer.MaterialConst;

/**
 * This control used to play animation of a tile.
 * 
 * @author yanmaoyuan
 * 
 */
public class AnimatedTileControl extends AbstractControl {

    private Tile tile;
    private Animation anim;

    private int previousTileId;
    private int currentFrameIndex;
    private float unusedTime;
    private final Vector2f tilePosition = new Vector2f();
    private Float[] textureArrayLayers;

    public AnimatedTileControl(Tile tile) {
        this.tile = tile;
        resetAnimation();
        setAnim(0);
    }

    public void setTile(Tile tile) {
        if (this.tile != tile) {
            this.tile = tile;
            resetAnimation();
            setAnim(0);
        }
    }

    public void setAnim(String name) {
        Animation animation = tile.getAnimation(name);
        if (animation != null) {
            if (this.anim != animation) {
                this.anim = animation;
                resetAnimation();
            }
        } else {
            if (this.anim != null) {
                this.anim = null;
                resetAnimation();
            }
        }
    }

    public void setAnim(int index) {
        Animation animation = tile.getAnimations().get(index);
        if (animation != null) {
            if (this.anim != animation) {
                this.anim = animation;
                resetAnimation();
            }
        } else {
            if (this.anim != null) {
                this.anim = null;
                resetAnimation();
            }
        }
    }

    /**
     * Resets the tile animation.
     */
    public void resetAnimation() {
        previousTileId = -1;
        currentFrameIndex = 0;
        unusedTime = 0f;
    }

    /**
     * Supplies the immutable tile-id to texture-array-layer lookup prepared by
     * the map renderer. The lookup is shared by all controls for the tileset.
     *
     * @param layers tile-id indexed, pre-boxed layer lookup, or {@code null}
     */
    public void setTextureArrayLayers(Float[] layers) {
        textureArrayLayers = layers;
    }

    public Tile getCurrentTile() {
        if (anim == null || anim.getTotalFrames() == 0 || tile.getTileset() == null) {
            return tile;
        }
        Frame frame = anim.getFrame(currentFrameIndex);
        if (frame == null) {
            return tile;
        }
        Tile current = tile.getTileset().getTile(frame.getTileId());
        return current != null ? current : tile;
    }

    @Override
    protected void controlUpdate(float tpf) {
        // no animation
        if (anim == null) {
            return;
        }

        float ms = tpf * 1000;
        unusedTime += ms;
        Frame frame = anim.getFrame(currentFrameIndex);

        while (frame.getDuration() > 0 && unusedTime > frame.getDuration()) {
            unusedTime -= frame.getDuration();
            currentFrameIndex = (currentFrameIndex + 1) % anim.getTotalFrames();

            frame = anim.getFrame(currentFrameIndex);
        }

        /*
         * whether this caused the current tileId to change.
         */
        if (previousTileId != frame.getTileId()) {
            previousTileId = frame.getTileId();
            Geometry geom = (Geometry) spatial;

            Tile t = getCurrentTile();
            Float layer = textureArrayLayers != null && t.getId() >= 0
                    && t.getId() < textureArrayLayers.length
                    ? textureArrayLayers[t.getId()] : null;
            if (layer != null && geom.getMaterial().getParam(MaterialConst.COLOR_ARRAY) != null) {
                geom.getMaterial().setFloat(MaterialConst.TILE_LAYER, layer);
            } else {
                geom.getMaterial().setVector2(MaterialConst.TILE_POSITION,
                        tilePosition.set(t.getX(), t.getY()));
            }
        }
    }

    @Override
    protected void controlRender(RenderManager rm, ViewPort vp) {
        // ignore
    }

}
