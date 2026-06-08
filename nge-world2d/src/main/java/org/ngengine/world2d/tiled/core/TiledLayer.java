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


import java.util.List;

import org.ngengine.components.ComponentManagerProvider;

import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector2f;

import org.ngengine.world2d.tiled.components.TiledComponentManager;
import org.ngengine.world2d.tiled.components.TiledComponentReflectionMounting;
import org.ngengine.world2d.tiled.util.CoordinateSystem;

/**
 * A layer of a map.
 * @author yanmaoyuan
 *
 */
public abstract class TiledLayer extends TiledBase implements ComponentManagerProvider {
    protected TiledMap map;

    protected TiledLayer parent;

    protected int index = 0;

    /**
     * Unique ID of the layer (defaults to 0, with valid IDs being at least 1).
     * Each layer that added to a map gets a unique id. Even if a layer is deleted,
     * no layer ever gets the same ID. Can not be changed in Tiled. (since Tiled 1.2)
     */
    protected int id;


    /**
     * The class of the layer (since 1.9, defaults to “”).
     */
    protected String clazz;

    /**
     * The x,y coordinate of the layer in tiles. Defaults to 0 and can no longer
     * be changed in Tiled.
     */
    protected int x = 0;

    protected int y = 0;

    /**
     * The width and height of the layer in tiles. Traditionally required, but
     * as of Tiled always the same as the map width and height for fixed-size maps.
     */
    protected int width = 0;
    protected int height = 0;
    protected double opacity = 1.0;
    protected boolean visible = true;
    protected boolean locked = false;
    protected ColorRGBA tintColor = new ColorRGBA(1f, 1f, 1f, 1f);

    protected int offsetX = 0;
    protected int offsetY = 0;
    protected int renderOffsetX = 0;
    protected int renderOffsetY = 0;
    private boolean isRenderOffsetUpdated = true;

    protected double parallaxX = 1;
    protected double parallaxY = 1;
    protected double renderParallaxX = 1;
    protected double renderParallaxY = 1;
    private boolean isRenderParallaxUpdated = true;
 
    public TiledLayer() {
        // for serialization
    }

 
    private TiledComponentManager componentManager;

    @Override
    public TiledComponentManager getComponentManager() {
        if(componentManager==null){
            componentManager = new TiledComponentManager();
            TiledComponentReflectionMounting.mountFromProperty(this, componentManager);
        }
        return componentManager;
    }



    /**
     * Constructor for TMXLayer.
     * 
     * @param width width in tiles
     * @param height height in tiles
     */
    public TiledLayer(int width, int height) {
        this.width = width;
        this.height = height;
    }

    /**
     * @return The map this layer is part of.
     */
    public TiledMap getMap() {
        return map;
    }

    /**
     * @param map The map this layer is part of.
     */
    public void setMap(TiledMap map) {
        this.map = map;
        setUpdateNeeded();
    }

    /**
     * @return The parent of this layer.
     */
    public TiledLayer getParent() {
        return parent;
    }

    /**
     * Set the parent of this layer.
     * @param parent The parent of this layer.
     */
    public void setParent(TiledLayer parent) {
        if (parent == this) {
            throw new IllegalArgumentException("Can't set parent to itself!");
        }
        this.parent = parent;
        setUpdateNeeded();
    }

    public void setIndex(int index) {
        this.index = index;
        setUpdateNeeded();
    }

    public int getIndex() {
        return index;
    }

    /**
     * @return The unique ID of the layer.
     */
    public int getId() {
        return id;
    }

    /**
     * @param id The unique ID of the layer.
     */
    public void setId(int id) {
        this.id = id;
        setUpdateNeeded();
    }



    /**
     * @return The class of the layer.
     */
    public String getClazz() {
        return clazz;
    }

    /**
     * @param clazz The class of the layer.
     */
    public void setClazz(String clazz) {
        this.clazz = clazz;
        setUpdateNeeded();
    }

    /**
     * @return The x coordinate of the layer in tiles.
     */
    public int getX() {
        return x;
    }

    /**
     * @param x The x coordinate of the layer in tiles.
     */
    public void setX(int x) {
        this.x = x;
        setUpdateNeeded();
    }

    /**
     * @return The y coordinate of the layer in tiles.
     */
    public int getY() {
        return y;
    }

    /**
     * @param y The y coordinate of the layer in tiles.
     */
    public void setY(int y) {
        this.y = y;
        setUpdateNeeded();
    }

    /**
     * @return The width of the layer in tiles.
     */
    public int getWidth() {
        return width;
    }

    /**
     * @param width The width of the layer in tiles.
     */
    public void setWidth(int width) {
        this.width = width;
        setUpdateNeeded();
    }

    /**
     * @return The height of the layer in tiles.
     */
    public int getHeight() {
        return height;
    }

    /**
     * @param height The height of the layer in tiles.
     */
    public void setHeight(int height) {
        this.height = height;
        setUpdateNeeded();
    }

    /**
     * @return The opacity of the layer as a value from 0 to 1. Defaults to 1.
     */
    public double getOpacity() {
        return opacity;
    }

    /**
     * Set the opacity of the layer as a value from 0 to 1. Defaults to 1.
     * @param opacity The opacity of the layer as a value from 0 to 1.
     */
    public void setOpacity(double opacity) {
        this.opacity = opacity;
        setUpdateNeeded();
    }

    /**
     * @return Whether the layer is shown or hidden. Defaults to true.
     */
    public boolean isVisible() {
        if (parent != null) {
            return parent.isVisible();
        } else {
            return visible;
        }
    }

    /**
     * @param visible Whether the layer is shown or hidden.
     */
    public void setVisible(boolean visible) {
        this.visible = visible;
        setUpdateNeeded();
    }

    public boolean isLocked() {
        return locked;
    }

    public void setLocked(boolean locked) {
        this.locked = locked;
        setUpdateNeeded();
    }

    /**
     * @return A tint color that is multiplied with any tiles drawn by this layer.
     */
    public ColorRGBA getTintColor() {
        if (parent != null) {
            return parent.getTintColor();
        } else {
            return tintColor;
        }
    }

    /**
     * @param tintColor A tint color that is multiplied with any tiles drawn by this layer.
     */
    public void setTintColor(ColorRGBA tintColor) {
        this.tintColor = tintColor;
        setUpdateNeeded();
    }

    /**
     * @return Horizontal offset for this layer in pixels. Defaults to 0.
     */
    public int getOffsetX() {
        return offsetX;
    }

    /**
     * @param offsetX Horizontal offset for this layer in pixels.
     */
    public void setOffsetX(int offsetX) {
        this.offsetX = offsetX;
        invalidRenderOffset();
    }

    /**
     * @return Vertical offset for this layer in pixels. Defaults to 0. (since 0.14)
     */
    public int getOffsetY() {
        return offsetY;
    }

    /**
     * @param offsetY Vertical offset for this layer in pixels.
     */
    public void setOffsetY(int offsetY) {
        this.offsetY = offsetY;
        invalidRenderOffset();
    }

    /**
     * Set the offset for this layer in pixels.
     * @param offsetX Horizontal offset for this layer in pixels.
     * @param offsetY Vertical offset for this layer in pixels.
     */
    public void setOffset(int offsetX, int offsetY) {
        this.offsetX = offsetX;
        this.offsetY = offsetY;
        invalidRenderOffset();
    }

    /**
     * @return Horizontal offset for this layer in pixels, including the offset of all parent layers.
     */
    public int getRenderOffsetX() {
        if (isRenderOffsetUpdated) {
            calculateRenderOffset();
        }
        return renderOffsetX;
    }

    /**
     * @return Vertical offset for this layer in pixels, including the offset of all parent layers.
     */
    public int getRenderOffsetY() {
        if (isRenderOffsetUpdated) {
            calculateRenderOffset();
        }
        return renderOffsetY;
    }

    /**
     * @return Horizontal parallax factor for this layer. Defaults to 1. (since 1.5)
     */
    public double getParallaxX() {
        return parallaxX;
    }

    /**
     * @param parallaxX Horizontal parallax factor for this layer.
     */
    public void setParallaxX(double parallaxX) {
        this.parallaxX = parallaxX;
        invalidRenderParallax();
    }

    /**
     * @return Vertical parallax factor for this layer. Defaults to 1. (since 1.5)
     */
    public double getParallaxY() {
        return parallaxY;
    }

    /**
     * @param parallaxY Vertical parallax factor for this layer.
     */
    public void setParallaxY(double parallaxY) {
        this.parallaxY = parallaxY;
        invalidRenderParallax();
    }

    /**
     * Set the parallax factor for this layer.
     * @param parallaxX Horizontal parallax factor for this layer.
     * @param parallaxY Vertical parallax factor for this layer.
     */
    public void setParallaxFactor(double parallaxX, double parallaxY) {
        this.parallaxX = parallaxX;
        this.parallaxY = parallaxY;
        invalidRenderParallax();
    }

    public double getRenderParallaxX() {
        if (isRenderParallaxUpdated) {
            calculateRenderParallax();
        }
        return renderParallaxX;
    }

    public double getRenderParallaxY() {
        if (isRenderParallaxUpdated) {
            calculateRenderParallax();
        }
        return renderParallaxY;
    }

    /**
     * Mark the render offset as invalid.
     */
    public void invalidRenderOffset() {
        isRenderOffsetUpdated = true;
        setUpdateNeeded();
    }

    /**
     * Mark the render parallax as invalid.
     */
    public void invalidRenderParallax() {
        isRenderParallaxUpdated = true;
        setUpdateNeeded();
    }

    /**
     * Calculate the render offset for this layer.
     *
     * <p>When the offset is set on a group layer, it applies to all its child layers.</p>
     */
    protected void calculateRenderOffset() {
        if (parent != null) {
            parent.calculateRenderOffset();
            renderOffsetX = offsetX + parent.getRenderOffsetX();
            renderOffsetY = offsetY + parent.getRenderOffsetY();
        } else {
            renderOffsetX = offsetX;
            renderOffsetY = offsetY;
        }
        isRenderOffsetUpdated = false;
    }

    /**
     * Calculate the render parallax factor for this layer.
     *
     * <p>When the parallax scrolling factor is set on a group layer, it applies to all its child layers.</p>
     * <p>The effective parallax scrolling factor of a layer is determined by multiplying the parallax scrolling
     * factor by the scrolling factors of all parent layers.</p>
     */
    protected void calculateRenderParallax() {
        if (parent != null) {
            parent.calculateRenderParallax();
            renderParallaxX = parallaxX * parent.getRenderParallaxX();
            renderParallaxY = parallaxY * parent.getRenderParallaxY();
        } else {
            renderParallaxX = parallaxX;
            renderParallaxY = parallaxY;
        }
        isRenderParallaxUpdated = false;
    }

    public abstract void getNearby(
            CoordinateSystem coords,
            Vector2f worldPos,
            float radius,   
            List<TiledBase> out
    );
  
}
