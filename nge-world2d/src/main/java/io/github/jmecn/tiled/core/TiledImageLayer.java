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

package io.github.jmecn.tiled.core;

import java.util.List;

import com.jme3.math.Vector2f;

import io.github.jmecn.tiled.core.entity.TiledImageEntity;
import io.github.jmecn.tiled.util.CoordinateSystem;

/**
 * A layer consisting of a single image.
 */
public class TiledImageLayer extends TiledLayer {

    private TiledImageEntity image;

    private boolean repeatX;
    private boolean repeatY;

    /**
     * Default constructor
     */
    public TiledImageLayer() {
        // for serialization
    }
    
    public TiledImageLayer(int width, int height) {
        super(width, height);
    }

    /**
     * @return Whether the image drawn by this layer is repeated along the X axis.
     */
    public boolean isRepeatX() {
        return repeatX;
    }

    /**
     * @param repeatX Whether the image drawn by this layer is repeated along the X axis.
     */
    public void setRepeatX(boolean repeatX) {
        this.repeatX = repeatX;
        setUpdateNeeded();
    }

    /**
     * @return Whether the image drawn by this layer is repeated along the Y axis.
     */
    public boolean isRepeatY() {
        return repeatY;
    }

    /**
     * @param repeatY Whether the image drawn by this layer is repeated along the Y axis.
     */
    public void setRepeatY(boolean repeatY) {
        this.repeatY = repeatY;
        setUpdateNeeded();
    }

    /**
     * @return The image of this layer.
     */
    public TiledImageEntity getImage() {
        return image;
    }

    /**
     * @param image The image of this layer.
     */
    public void setImage(TiledImageEntity image) {
        this.image = image;
        // tell map renderer to update it
        setUpdateNeeded();
    }

    @Override
    public void getNearby(CoordinateSystem coords, Vector2f worldPos, float radius, List<TiledBase> out) {
       out.add(this);
    }

}
