/*
 * $Id$
 *
 * Copyright (c) 2015, Simsilica, LLC
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 *
 * 3. Neither the name of the copyright holder nor the names of its
 *    contributors may be used to endorse or promote products derived
 *    from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 * FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 * COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT,
 * INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
 * HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT,
 * STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED
 * OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package org.ngengine.gui;

import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.texture.Image;
import com.jme3.texture.Texture2D;
import com.jme3.texture.image.ImageRaster;
import com.jme3.util.BufferUtils;
import org.ngengine.gui.component.BorderLayout;
import org.ngengine.gui.component.IconComponent;
import org.ngengine.gui.component.QuadBackgroundComponent;
import org.ngengine.gui.component.SpringGridLayout;
import org.ngengine.gui.core.GuiControl;
import org.ngengine.gui.core.VersionedHolder;
import org.ngengine.gui.core.VersionedObject;
import org.ngengine.gui.core.VersionedReference;
import org.ngengine.gui.nav.FocusListener;
import org.ngengine.gui.nav.ScrollDirection;
import org.ngengine.gui.style.ElementId;
import org.ngengine.gui.style.Styles;


/**
 *
 *
 *  @author    Paul Speed
 */
public class ColorChooser extends Panel {

    public static final String ELEMENT_ID = "colorChooser";
    public static final String CONTAINER_ID = "container";
    public static final String COLORS_ID = "colors";
    public static final String BRIGHTNESS_ID = "brightness.slider";
    public static final String VALUE_ID = "value";
    public static final String CROSSHAIR_ID = "crosshair";
    
    public static final String DEFAULT_CROSSHAIR = "/org/ngengine/gui/icons/tiny-crosshair.png";

    public static final Texture2D defaultTexture = new Texture2D(256, 256, Image.Format.RGBA8);
    static {
        defaultTexture.getImage().setData(BufferUtils.createByteBuffer(256 * 256 * 4));
        ImageRaster raster = ImageRaster.create(defaultTexture.getImage());
        ColorRGBA color = new ColorRGBA();
        for( int i = 0; i < 256; i++ ) {
            for( int j = 0; j < 256; j++ ) {
                raster.setPixel(i, j, hsbToRgb(i/255f, j/255f, 0.5f, color));
            }
        }
    }

    private VersionedObject<ColorRGBA> model;
    private VersionedReference<ColorRGBA> modelRef;
    private Texture2D swatchTexture;

    private Panel value;
    private Container colorPanel;
    private Panel colors;
    private Panel crosshair;
    private Vector3f crosshairOffset;    
    private QuadBackgroundComponent valueColor = new QuadBackgroundComponent();
    private QuadBackgroundComponent swatchComponent;
    private Slider brightness;
    private VersionedReference brightnessRef;

    private float hIndex = 0;
    private float sIndex = 0;
    private float bIndex = 0.5f;


    public ColorChooser() {
        this((VersionedObject<ColorRGBA>)null);
    }
   
    public ColorChooser( VersionedObject<ColorRGBA> model){
        this(model, new ElementId(ELEMENT_ID));
    }
  
    public ColorChooser(  ElementId elementId ) {
        this( null, elementId);

    }

    public ColorChooser(  VersionedObject<ColorRGBA> model, ElementId elementId ) {
        super(elementId.child(CONTAINER_ID));

        this.swatchTexture = defaultTexture;

        SpringGridLayout layout = new SpringGridLayout();
        getControl(GuiControl.class).setLayout(layout);

        colorPanel = new Container(elementId.child(COLORS_ID));
        colorPanel.setLayout(new SpringGridLayout());
        colors = new Panel(elementId.child(COLORS_ID));
        colorPanel.addChild(colors);
        this.swatchComponent = new QuadBackgroundComponent(swatchTexture);
        getControl(GuiControl.class).addFocusChangeListener( new SwatchListener());
        
        colors.setBackground(swatchComponent);
        colors.setPreferredSize(new Vector3f(NGEStyle.px(256), NGEStyle.px(64), 0));
        layout.addChild(colorPanel, 2);

        brightness = new Slider( Axis.Y, elementId.child(BRIGHTNESS_ID));
        layout.addChild(brightness, 1);
        brightnessRef = brightness.getModel().createReference();

        value = new Panel( elementId.child(VALUE_ID));
        value.setPreferredSize(new Vector3f(NGEStyle.px(64), NGEStyle.px(64), 0));
        value.setBackground(valueColor);
        layout.addChild(value, 0);

        crosshair = new Panel( elementId.child(CROSSHAIR_ID));
        if( crosshair.getBackground() == null ) {
            IconComponent icon = new IconComponent(DEFAULT_CROSSHAIR);            
            crosshair.setBackground(icon);
        }       
        Vector3f pref = crosshair.getPreferredSize();
        crosshairOffset = new Vector3f(pref.x * 0.5f, pref.y * 0.5f, pref.z);
        
        // Insert a node between the color chooser and the crosshair so that
        // the layout will ignore it
        Node standoff = new Node("crosshair-standoff");
        standoff.attachChild(crosshair);
        colors.attachChild(standoff);
     

        setModel(model);
        applyStyles(ColorChooser.class);
    }

    public void setModel( VersionedObject<ColorRGBA> model ) {
        if( this.model != null ) {
            // clean up whatever
        }
        if( model == null ) {
            // Create a default model
            model = new VersionedHolder<ColorRGBA>(new ColorRGBA(0.5f, 0.5f, 0.5f, 1));
        }
        this.model = model;
        modelRef = model.createReference();
        updateColorView();
    }

    public VersionedObject<ColorRGBA> getModel() {
        return model;
    }

    public void setColor( ColorRGBA color ) {
        if( !(model instanceof VersionedHolder) ) {
            throw new UnsupportedOperationException("Current model does not support externally setting the color");
        }
        ((VersionedHolder<ColorRGBA>)model).setObject(color);
    }
    
    public ColorRGBA getColor() {
        return model == null ? null : model.getObject();
    }

    @Override
    public void updateLogicalState( float tpf ) {
        super.updateLogicalState(tpf);
        if( modelRef.update() ) {
            updateColorView();
        }
        if( brightnessRef.update() ) {
            updateBrightness();
        }
    }

    protected void updateModelValue( float h, float s, float b ) {
        if( h == hIndex && s == sIndex && b == bIndex ) {
            return;
        }
        this.hIndex = h;
        this.sIndex = s;
        this.bIndex = b;

        ((VersionedHolder<ColorRGBA>)model).setObject(
                hsbToRgb(hIndex, sIndex, bIndex, new ColorRGBA()));
    }

    protected void updateBrightness() {
        float v = (float)(brightness.getModel().getValue()/100);
        updateModelValue(hIndex, sIndex, v);
    }

    protected void updateColorView() {

        ColorRGBA c = model.getObject();
        Vector3f hsb = rgbToHsb(c, new Vector3f());

        this.hIndex = hsb.x;
        this.sIndex = hsb.y;
        this.bIndex = hsb.z;

        updateColorView(hsb.x, hsb.y, hsb.z);
    }

    protected void updateColorView( float h, float s, float v ) {

        valueColor.setColor(hsbToRgb(h, s, v, new ColorRGBA()));

        // Now we need to get the B of the HSB to set that one
        brightness.getModel().setValue(v * 100);
 
        Vector3f range = colors.getSize();       
        crosshair.setLocalTranslation(h * range.x - crosshairOffset.x, s * range.y - range.y + crosshairOffset.y, crosshairOffset.z);
    }

    static ColorRGBA hsbToRgb(float hue, float saturation, float brightness, ColorRGBA store) {
        if (saturation == 0) {
            return store.set(brightness, brightness, brightness, 1);
        }

        float normalizedHue = hue - (float)Math.floor(hue);
        float scaledHue = normalizedHue * 6;
        int sector = (int)scaledHue;
        float fraction = scaledHue - sector;
        float p = brightness * (1 - saturation);
        float q = brightness * (1 - saturation * fraction);
        float t = brightness * (1 - saturation * (1 - fraction));

        switch (sector) {
            case 0:
                return store.set(brightness, t, p, 1);
            case 1:
                return store.set(q, brightness, p, 1);
            case 2:
                return store.set(p, brightness, t, 1);
            case 3:
                return store.set(p, q, brightness, 1);
            case 4:
                return store.set(t, p, brightness, 1);
            default:
                return store.set(brightness, p, q, 1);
        }
    }

    static Vector3f rgbToHsb(ColorRGBA color, Vector3f store) {
        float red = color.r;
        float green = color.g;
        float blue = color.b;
        float maximum = Math.max(red, Math.max(green, blue));
        float minimum = Math.min(red, Math.min(green, blue));
        float brightness = maximum;
        float saturation = maximum == 0 ? 0 : (maximum - minimum) / maximum;
        float hue = 0;

        if (saturation != 0) {
            float range = maximum - minimum;
            float redDistance = (maximum - red) / range;
            float greenDistance = (maximum - green) / range;
            float blueDistance = (maximum - blue) / range;
            if (red == maximum) {
                hue = blueDistance - greenDistance;
            } else if (green == maximum) {
                hue = 2 + redDistance - blueDistance;
            } else {
                hue = 4 + greenDistance - redDistance;
            }
            hue /= 6;
            if (hue < 0) {
                hue += 1;
            }
        }

        return store.set(hue, saturation, brightness);
    }

    private class SwatchListener implements FocusListener {

    
        @Override
        public void focusGained(Spatial target) {
          
        }

        @Override
        public void focusLost(Spatial target) {
            
        }

        @Override
        public void focusAction(Spatial target, boolean pressed) {
            // Vector3f world = new Vector3f(event.getX(), event.getY(), 0);
            // Vector3f local = colors.worldToLocal(world, null);
            // Vector3f size = colors.getSize();
            // float h = (local.x / size.x);
            // float s = (size.y + local.y) / size.y;
            // updateModelValue(h, s, bIndex);
        }

        @Override
        public void focusScrollUpdate(Spatial target, ScrollDirection dir,  double value) {
 
        }
    }
}
