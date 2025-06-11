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
package org.ngengine.gui.components;

import com.jme3.material.RenderState.BlendMode;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.shape.Quad;
import com.jme3.texture.Texture;
import com.jme3.texture.Texture.MagFilter;
import com.jme3.texture.Texture.MinFilter;
import com.jme3.texture.plugins.SVGTextureKey;
import com.simsilica.lemur.GuiGlobals;
import com.simsilica.lemur.Panel;
import com.simsilica.lemur.core.GuiControl;
import com.simsilica.lemur.core.GuiControlListener;
import com.simsilica.lemur.core.GuiMaterial;
import com.simsilica.lemur.core.GuiUpdateListener;
import com.simsilica.lemur.style.Attributes;
import com.simsilica.lemur.style.ElementId;
import com.simsilica.lemur.style.StyleAttribute;
import com.simsilica.lemur.style.StyleDefaults;

public class NLoadingSpinner extends Panel implements GuiUpdateListener, GuiControlListener {

    public static final String ELEMENT_ID = "loading-spinner";

    protected String spinnerPath = "icons/outline/loader-2.svg";
    protected float speed = -1f;
    protected GuiMaterial material;
    protected Node spinnerNode;
    protected boolean invalidated = false;
    protected ColorRGBA color;

    public NLoadingSpinner() {
        super(new ElementId(ELEMENT_ID));
        material = createMaterial();

        Quad quad = new Quad(1, 1);
        Geometry geom = new Geometry("LoadingSpinnerGeom", quad);
        geom.setMaterial(material.getMaterial());
        geom.setLocalTranslation(-0.5f, -0.5f, 0);

        spinnerNode = new Node("LoadingSpinner");
        spinnerNode.setLocalTranslation(0, 0, 0);
        spinnerNode.attachChild(geom);

        attachChild(spinnerNode);

        loadImage(32, 32);
        getControl(GuiControl.class).addUpdateListener(this);
        getControl(GuiControl.class).addListener(this);
    }

    @StyleDefaults("loading-spinner")
    public static void initializeDefaultStyles(Attributes attrs) {
        ColorRGBA gray = GuiGlobals.getInstance().srgbaColor(ColorRGBA.Gray);
        attrs.set("color", gray, false);
    }

    protected GuiMaterial createMaterial() {
        GuiMaterial mat = GuiGlobals.getInstance().createMaterial(color, false);
        mat.getMaterial().getAdditionalRenderState().setBlendMode(BlendMode.Alpha);
        return mat;
    }

    protected void loadImage(float w, float h) {
        float min = Math.min(w, h);
        w = min;
        h = min;
        Texture tx = GuiGlobals.getInstance().loadTexture(new SVGTextureKey(spinnerPath, (int) w, (int) h), false);
        tx.setMinFilter(MinFilter.BilinearNoMipMaps);
        tx.setMagFilter(MagFilter.Bilinear);
        material.setTexture(tx);
    }

    @Override
    public void guiUpdate(GuiControl source, float tpf) {
        spinnerNode.rotate(0, 0, speed * tpf);
        Vector3f spinnerSize = spinnerNode.getLocalScale();
        Vector3f containerSize = getSize();
        spinnerNode.setLocalTranslation(containerSize.x / 2, containerSize.y / 2 - spinnerSize.y, 1);

        if (invalidated) {
            invalidated = false;
            loadImage(getSize().x, getSize().y);
        }
    }

    @Override
    public void reshape(GuiControl source, Vector3f pos, Vector3f size) {
        float minDim = Math.min(size.x, size.y);
        spinnerNode.setLocalScale(minDim);
        invalidated = true;
    }

    @Override
    public void focusGained(GuiControl source) {}

    @Override
    public void focusLost(GuiControl source) {}

    @StyleAttribute(value = "color", lookupDefault = false)
    public void setColor(ColorRGBA color) {
        if (this.color == null) {
            this.color = new ColorRGBA();
        }
        this.color.set(color);
    }

    @StyleAttribute(value = "speed", lookupDefault = false)
    public void setSpeed(float speed) {
        this.speed = speed;
    }

    @StyleAttribute(value = "icon", lookupDefault = false)
    public void setIcon(String icon) {
        this.spinnerPath = icon;
        invalidated = true;
    }
}
