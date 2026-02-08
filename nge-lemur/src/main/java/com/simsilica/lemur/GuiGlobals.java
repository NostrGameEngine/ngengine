/*
 * $Id$
 *
 * Copyright (c) 2012-2012 jMonkeyEngine
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 * * Redistributions of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 *
 * * Redistributions in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer in the
 *   documentation and/or other materials provided with the distribution.
 *
 * * Neither the name of 'jMonkeyEngine' nor the names of its contributors
 *   may be used to endorse or promote products derived from this software
 *   without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
 * TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

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

package com.simsilica.lemur;

import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.jme3.app.Application;
import com.jme3.asset.AssetManager;
import com.jme3.asset.TextureKey;
import com.jme3.font.BitmapFont;
import com.jme3.material.MatParam;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.renderer.ViewPort;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.renderer.queue.RenderQueue.Bucket;
import com.jme3.system.JmeContext.Type;
import com.jme3.texture.Texture;
import com.simsilica.lemur.anim.AnimationState;
import com.simsilica.lemur.component.Text2d;
import com.simsilica.lemur.component.TextComponent;
import com.simsilica.lemur.core.GuiMaterial;
import com.simsilica.lemur.core.UnshadedMaterialAdapter;
import com.simsilica.lemur.core.LightingMaterialAdapter;

import com.simsilica.lemur.event.PopupState;

import com.simsilica.lemur.input.InputMapper;
import com.simsilica.lemur.style.Styles;


/**
 *  A utility class that sets up some default global behavior for
 *  the default GUI elements and provides some common access to
 *  things like the AssetManager.
 *
 *  <p>When initialized, GuiGlobals will keep a reference to the
 *  AssetManager for use in creating materials, loading fonts, and so
 *  on.  It will also:
 *  <ul>
 *  <li>Setup the KeyInterceptState for allowing edit fields to intercept
 *      key events ahead of the regular input processing.</li>
 *  <li>Initialize InputMapper to provide advanced controller input processing.</li>
 *  <li>Setup the MouseAppState to provide default mouse listener and picking
 *      support for registered pick roots.</li>
 *  <li>Setup the FocusManagerState that keeps track of the currently
 *      focused component and makes sure transition methods are properly called.</li>
 *  <li>Setup the default styles.</li>
 *  <li>Sets up the layer based geometry comparators for the default app
 *      viewport.</li>
 *  </ul>
 *
 *  <p>For applications that wish to customize the behavior of GuiGlobals,
 *  it is possible to set a custom subclass instead of initializing the
 *  default implemenation.  Examples of reasons do do this might include
 *  using custom materials instead of the default JME materials or otherwise
 *  customizing the initialization setup.</p>
 *
 *  @author    Paul Speed
 */
public class GuiGlobals {

    static Logger log = Logger.getLogger(GuiGlobals.class.getName());

    private static GuiGlobals instance;

    private AssetManager assets;
    private InputMapper  inputMapper;
    private AnimationState animationState;
    private PopupState popupState;
    private String iconBase;

    private Function<String, Text2d> textFactory = new DefaultTextFactory();

    private Styles styles;

    private boolean gammaEnabled;

    public static void initialize( Application app ) {
        setInstance(new GuiGlobals(app));
    }

    public static void setInstance( GuiGlobals globals ) {
        instance = globals;
        log.info( "Initializing GuiGlobals with:" + globals );
    }

    public static GuiGlobals getInstance() {
        return instance;
    }

    protected boolean isHeadless( Application app ) {
        Type type = app.getContext().getType();
        return type == Type.Headless; // || type == Type.OffscreenSurface;
    }

    protected GuiGlobals( Application app ) {
        this.assets = app.getAssetManager();

        if( isHeadless(app) ) {
            // Do only minimal initialization... and nothing requiring
            // input, a screen, etc.
            styles = new Styles();
            setDefaultStyles();

            iconBase = getClass().getPackage().getName().replace( '.', '/' ) + "/icons";
            return;
        }


        this.inputMapper = new InputMapper();
 
        this.animationState = new AnimationState();
        this.popupState = new PopupState();





        app.getStateManager().attach(animationState);
        app.getStateManager().attach(popupState);

        styles = new Styles();
        setDefaultStyles();

        iconBase = getClass().getPackage().getName().replace( '.', '/' ) + "/icons";

        ViewPort main = app.getViewPort();
        setupGuiComparators(main);

        

        gammaEnabled = app.getContext().getSettings().isGammaCorrection();
    }

    protected AssetManager getAssetManager() {
        return assets;
    }

    protected String getIconBase() {
        return iconBase;
    }

    public void setupGuiComparators( ViewPort view ) {
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

    protected void setDefaultStyles() {
        styles.setDefault(loadFont("Interface/Fonts/Default.fnt"));
        styles.setDefault(ColorRGBA.LightGray);

        // Setup some default styles for the "DEFAULT" Style
        styles.getSelector(null).set("color", ColorRGBA.White);
        styles.getSelector(null).set("fontName", "Interface/Fonts/Default.fnt");
    }

    public Styles getStyles() {
        return styles;
    }

    public InputMapper getInputMapper() {
        return inputMapper;
    }

    public AnimationState getAnimationState() {
        return animationState;
    }

    public PopupState getPopupState() {
        return popupState;
    }
 

    /**
     *  Goes through all of the font page materials and sets
     *  alpha test and alpha fall-off.
     */
    public void fixFont( BitmapFont font ) {
        for( int i = 0; i < font.getPageSize(); i++ ) {
            Material m = font.getPage(i);
            // AlphaTest and AlphaFalloff are deprecated in favor of the material
            // parameter... in fact in current JME there are no-ops.
            //m.getAdditionalRenderState().setAlphaTest(true);
            //m.getAdditionalRenderState().setAlphaFallOff(0.1f);
            m.setFloat("AlphaDiscardThreshold", 0.1f);
        }
    }

    private Texture getTexture( Material mat, String name ) {
        MatParam mp = mat.getParam(name);
        if( mp == null ) {
            return null;
        }
        return (Texture)mp.getValue();
    }

    public AssetManager getAssets() {
        return assets;
    }
    public void lightFont( BitmapFont font ) {
        Material[] pages = new Material[font.getPageSize()];
        for( int i = 0; i < pages.length; i++ ) {
            Material original = font.getPage(i);
            Material m = new Material(assets, "Common/MatDefs/Light/Lighting.j3md");
            m.setTexture("DiffuseMap", getTexture(original, "ColorMap"));
            pages[i] = m;
        }
        font.setPages(pages);
    }

    public BitmapFont loadFont( String path ) {
        BitmapFont result = assets.loadFont(path);
        fixFont(result);
        return result;
    }

    public Text2d createText2d( String fontName ) {
        if( textFactory == null ) {
            throw new UnsupportedOperationException("No text2D factory is configured.");
        }
        return textFactory.apply(fontName);
    }

    public void setTextFactory( Function<String, Text2d> textFactory ) {
        this.textFactory = textFactory;
    }

    public Function<String, Text2d> getTextFactory() {
        return textFactory;
    }

    public GuiMaterial createMaterial( boolean lit ) {
        if( lit ) {
            return new LightingMaterialAdapter(new Material(assets, "Common/MatDefs/Light/Lighting.j3md"));
        } else {
            return new UnshadedMaterialAdapter(new Material(assets, "Common/MatDefs/Misc/Unshaded.j3md"));
        }
    }

    public GuiMaterial createMaterial( ColorRGBA color, boolean lit ) {
        GuiMaterial mat = createMaterial(lit);
        mat.setColor(color);
        return mat;
    }

    public GuiMaterial createMaterial( Texture texture, boolean lit ) {
        GuiMaterial mat = createMaterial(lit);
        mat.setTexture(texture);
        return mat;
    }

    public Texture loadDefaultIcon( String name ) {
        return loadTexture(iconBase + "/" + name, false, false);
    }

    public Texture loadTexture( String path, boolean repeat, boolean generateMips ) {
        TextureKey key = new TextureKey(path);
        key.setGenerateMips(generateMips);

        return loadTexture(key, repeat);
    }

    public Texture loadTexture(TextureKey key, boolean repeat) {
        Texture t = assets.loadTexture(key);
        if (t == null) {
            throw new RuntimeException("Error loading texture:" + key.getName());
        }
        if (repeat) {
            t.setWrap(Texture.WrapMode.Repeat);
        } else {
            // JME has deprecated Clamp and defaults to EdgeClamp.
            // I think the WrapMode.EdgeClamp javadoc is totally bonkers, though.
            t.setWrap(Texture.WrapMode.EdgeClamp);
        }
        return t;
    }

    static final float GAMMA = 2.2f;

    /**
     *  Creates a color from the specified RGBA values as if they were in SRGB space,
     *  depending on whether gamma correction is enabled or disabled.  If there is no
     *  gamma correction then the RGBA values are interpretted literally.  If gamma
     *  correction is enabled then the values are converted to linear space before
     *  returning.
     */
    public ColorRGBA srgbaColor( float r, float g, float b, float a ) {
        if( gammaEnabled ) {
            // Note: unlike JME's seAsSrgb() method, when converting from SRGB
            //   space this method will also convert the alpha as it seems to matter in color
            //   matching.
            // ...except it didn't always work.
            //return new ColorRGBA().setAsSrgb(r, g, b, (float)Math.pow(a, GAMMA));
            return new ColorRGBA().setAsSrgb(r, g, b, a);
        } else {
            return new ColorRGBA(r, g, b, a);
        }
    }

    /**
     *  Creates a color from the specified RGBA values as if they were in SRGB space,
     *  depending on whether gamma correction is enabled or disabled.  If there is no
     *  gamma correction then the RGBA values are interpretted literally.  If gamma
     *  correction is enabled then the values are converted to linear space before
     *  returning.
     */
    public ColorRGBA srgbaColor( ColorRGBA srgbColor ) {
        return srgbaColor(srgbColor.r, srgbColor.g, srgbColor.b, srgbColor.a);
    }

  
 
 
    private static class DefaultTextFactory implements Function<String, Text2d> {
        public Text2d apply( String fontName ) {
            if( log.isLoggable(Level.FINE) ) {
                log.fine("createText2d(" + fontName + ")");
            }
            BitmapFont font = getInstance().loadFont(fontName);
            return new TextComponent("", font);
        }
    }
}
