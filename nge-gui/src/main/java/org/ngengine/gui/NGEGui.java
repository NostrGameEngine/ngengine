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

package org.ngengine.gui;

import java.util.IdentityHashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.jme3.asset.AssetManager;
import com.jme3.asset.TextureKey;
import com.jme3.font.BitmapFont;
import com.jme3.input.InputManager;
import com.jme3.input.KeyInput;
import com.jme3.input.MouseInput;
import com.jme3.input.controls.KeyTrigger;
import com.jme3.input.controls.MouseAxisTrigger;
import com.jme3.input.controls.MouseButtonTrigger;
import com.jme3.material.MatParam;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.renderer.ViewPort;
import com.jme3.scene.Spatial;
import com.jme3.scene.control.Control;
import com.jme3.texture.Texture;
import org.ngengine.gui.component.Text2d;
import org.ngengine.gui.component.TextComponent;
import org.ngengine.gui.core.GuiMaterial;
import org.ngengine.gui.core.LightingMaterialAdapter;
import org.ngengine.gui.core.UnshadedMaterialAdapter;
import org.ngengine.gui.nav.FocusTarget;
import org.ngengine.gui.style.Styles;

public class NGEGui {
    private final static Logger log = Logger.getLogger(NGEGui.class.getName());

    private static final ThreadLocal<Map<ViewPort, GuiContext>> contextsThreadLocal = ThreadLocal
            .withInitial(IdentityHashMap::new);
    private static final ThreadLocal<Styles> stylesThreadLocal = ThreadLocal.withInitial(Styles::new);
    private static final ThreadLocal<Function<String, Text2d>> textFactoryThreadLocal = new ThreadLocal<>();
    private static String iconBase;
    private static final ThreadLocal<AssetManager> assetManagerThreadLocal = new ThreadLocal<>();

    public static void initialize(AssetManager assetManager) {
        if(assetManagerThreadLocal.get()!=null) return; // already initialized
        assetManagerThreadLocal.set(assetManager);
        textFactoryThreadLocal.set(new DefaultTextFactory());
        iconBase = NGEGui.class.getPackage().getName().replace('.', '/') + "/icons";
        Styles styles = stylesThreadLocal.get();

        styles.setDefault(loadFont("Interface/Fonts/Default.fnt"));
        styles.setDefault(ColorRGBA.LightGray);

        // Setup some default styles for the "DEFAULT" Style
        styles.getSelector(null).set("color", ColorRGBA.White);
        styles.getSelector(null).set("fontName", "Interface/Fonts/Default.fnt");
    }

    public static AssetManager getAssetManager() {
        return assetManagerThreadLocal.get();
    }

    public static Styles getStyles() {
        return stylesThreadLocal.get();
    }
    
    public static boolean isFocusable(Spatial s) {
        return isFocusable(s, FocusTarget.FOCUS_ALL);
    }

    public static boolean isFocusable(Spatial s, int focusMask) {
        if (s.getCullHint() == Spatial.CullHint.Always) return false;
        FocusTarget tg = findFocusTarget(s);
        return tg != null && tg.isFocusable(focusMask);
    }

    public static FocusTarget findFocusTarget(Spatial s) {
        if (s == null) {
            return null;
        }
        for (int i = 0; i < s.getNumControls(); i++) {
            Control c = s.getControl(i);
            if (c instanceof FocusTarget) {
                return (FocusTarget) c;
            }
        }
        return null;
    }

    public static BitmapFont loadFont(String path) {
        AssetManager assetManager = getAssetManager();
        BitmapFont result = assetManager.loadFont(path);
        fixFont(result);
        return result;
    }

    private static Texture getTexture(Material mat, String name) {
        MatParam mp = mat.getParam(name);
        if (mp == null) {
            return null;
        }
        return (Texture) mp.getValue();
    }

    public static Text2d createText2d(String fontName) {
        Function<String, Text2d> textFactory = textFactoryThreadLocal.get();
        if (textFactory == null) {
            throw new UnsupportedOperationException("No text2D factory is configured.");
        }
        return textFactory.apply(fontName);
    }

    public static void setTextFactory(Function<String, Text2d> textFactory) {
        textFactoryThreadLocal.set(textFactory);
    }

    public static Function<String, Text2d> getTextFactory() {
        return textFactoryThreadLocal.get();
    }

    public static void lightFont(BitmapFont font) {
        AssetManager assetManager = getAssetManager();
        Material[] pages = new Material[font.getPageSize()];
        for (int i = 0; i < pages.length; i++) {
            Material original = font.getPage(i);
            Material m = new Material(assetManager, "Common/MatDefs/Light/Lighting.j3md");
            m.setTexture("DiffuseMap", getTexture(original, "ColorMap"));
            pages[i] = m;
        }
        font.setPages(pages);
    }

    public static GuiMaterial createMaterial(boolean lit) {
        AssetManager assetManager = getAssetManager();
        if (lit) {
            return new LightingMaterialAdapter(
                    new Material(assetManager, "Common/MatDefs/Light/Lighting.j3md"));
        } else {
            return new UnshadedMaterialAdapter(
                    new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md"));
        }
    }

    public static GuiMaterial createMaterial(ColorRGBA color, boolean lit) {

        GuiMaterial mat = createMaterial(lit);
        mat.setColor(color);
        return mat;
    }

    public static GuiMaterial createMaterial(Texture texture, boolean lit) {
        GuiMaterial mat = createMaterial(lit);
        mat.setTexture(texture);
        return mat;
    }

    public static Texture loadDefaultIcon(String name) {
        return loadTexture(iconBase + "/" + name, false, false);
    }

    public static Texture loadTexture(String path, boolean repeat, boolean generateMips) {
        TextureKey key = new TextureKey(path);
        key.setGenerateMips(generateMips);

        return loadTexture(key, repeat);
    }

    public static Texture loadTexture(TextureKey key, boolean repeat) {
        AssetManager assetManager = getAssetManager();
        Texture t = assetManager.loadTexture(key);
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

    /**
     * Creates a color from the specified RGBA values as if they were in SRGB space, depending on whether
     * gamma correction is enabled or disabled. If there is no gamma correction then the RGBA values are
     * interpretted literally. If gamma correction is enabled then the values are converted to linear space
     * before returning.
     */
    public static ColorRGBA srgbaColor(float r, float g, float b, float a, boolean gammaEnabled) {
        if (gammaEnabled) {
            // Note: unlike JME's seAsSrgb() method, when converting from SRGB
            // space this method will also convert the alpha as it seems to matter in color
            // matching.
            // ...except it didn't always work.
            // return new ColorRGBA().setAsSrgb(r, g, b, (float)Math.pow(a, GAMMA));
            return new ColorRGBA().setAsSrgb(r, g, b, a);
        } else {
            return new ColorRGBA(r, g, b, a);
        }
    }

    /**
     * Creates a color from the specified RGBA values as if they were in SRGB space, depending on whether
     * gamma correction is enabled or disabled. If there is no gamma correction then the RGBA values are
     * interpretted literally. If gamma correction is enabled then the values are converted to linear space
     * before returning.
     */
    public static ColorRGBA srgbaColor(ColorRGBA srgbColor, boolean gammaEnabled) {
        return srgbaColor(srgbColor.r, srgbColor.g, srgbColor.b, srgbColor.a, gammaEnabled);
    }

    private static class DefaultTextFactory implements Function<String, Text2d> {

        public Text2d apply(String fontName) {
            if (log.isLoggable(Level.FINE)) {
                log.fine("createText2d(" + fontName + ")");
            }

            BitmapFont font = loadFont(fontName);
            return new TextComponent("", font);
        }
    }

    /**
     * Goes through all of the font page materials and sets alpha test and alpha fall-off.
     */
    public static void fixFont(BitmapFont font) {
        for (int i = 0; i < font.getPageSize(); i++) {
            Material m = font.getPage(i);
            // AlphaTest and AlphaFalloff are deprecated in favor of the material
            // parameter... in fact in current JME there are no-ops.
            // m.getAdditionalRenderState().setAlphaTest(true);
            // m.getAdditionalRenderState().setAlphaFallOff(0.1f);
            m.setFloat("AlphaDiscardThreshold", 0.1f);
        }
    }

    public static GuiContext register(ViewPort vp, boolean sRGB) {
        Map<ViewPort, GuiContext> contexts = contextsThreadLocal.get();
        if (vp == null) throw new IllegalArgumentException("ViewPort cannot be null");
        GuiContext viewState = contexts.get(vp);
        if (viewState != null) return viewState;

        viewState = new GuiContext(vp,  sRGB);
        contexts.put(vp, viewState);
        return viewState;
    }

    public static boolean isRegistered(ViewPort vp) {
        Map<ViewPort, GuiContext> contexts = contextsThreadLocal.get();
        return vp != null && contexts.containsKey(vp);
    }

    public static void unregister(ViewPort vp) {
        if (vp == null) return;
        Map<ViewPort, GuiContext> contexts = contextsThreadLocal.get();
        GuiContext s = contexts.remove(vp);
        if (s != null) s.onDisabled();
    }

    public static GuiContext get(Spatial sp) {
        if (sp == null) throw new IllegalArgumentException("Spatial cannot be null");
        Map<ViewPort, GuiContext> contexts = contextsThreadLocal.get();

        Spatial root = sp;
        while (root.getParent() != null) root = root.getParent();

        for (Map.Entry<ViewPort, GuiContext> e : contexts.entrySet()) {
            ViewPort vp = e.getKey();
            for (Spatial sceneRoot : vp.getScenes()) {
                if (sceneRoot == root) {
                    return e.getValue();
                }
            }
        }

        return null;
    }

    public static GuiContext get(ViewPort vp) {
        if (vp == null) throw new IllegalArgumentException("ViewPort cannot be null");
        Map<ViewPort, GuiContext> contexts = contextsThreadLocal.get();
        GuiContext s = contexts.get(vp);
        if (s == null) {
            throw new IllegalStateException("ViewPort not registered: " + vp + ". Call register(vp) first.");
        }
        return s;
    }

    public static void update(ViewPort vp, float tpf) {
        Map<ViewPort, GuiContext> contexts = contextsThreadLocal.get();
        GuiContext s = contexts.get(vp);
        if (s != null) {
            s.update(tpf);
        }
    }
 
 
}
