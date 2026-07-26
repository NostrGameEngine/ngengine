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

import com.jme3.material.Material;
import com.jme3.material.RenderState.BlendMode;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import org.ngengine.gui.component.IconComponent;
import org.ngengine.gui.component.QuadBackgroundComponent;
import org.ngengine.gui.component.SpringGridLayout;
import org.ngengine.gui.component.TbtQuadBackgroundComponent;
import org.ngengine.gui.guix.NChip;
import org.ngengine.gui.guix.NSVGIcon;
import org.ngengine.gui.guix.containers.NPanel;
import org.ngengine.gui.style.Attributes;
import org.ngengine.gui.style.ElementId;
import org.ngengine.gui.style.Styles;
import java.util.List;
import java.util.Map;

import org.ngengine.DevMode;
import org.ngengine.platform.NGEUtils;

public class NGEStyle {

    private static final String NAME = "nge";

    public static ColorRGBA fromHex(String hex) {
        ColorRGBA c = NGEGui.srgbaColor(
            Integer.valueOf(hex.substring(1, 3), 16) / 255f,
            Integer.valueOf(hex.substring(3, 5), 16) / 255f,
            Integer.valueOf(hex.substring(5, 7), 16) / 255f,
            hex.length() > 7 ? Integer.valueOf(hex.substring(7, 9), 16) / 255f : 1f,
            true
        );

        return c;
    }

    static ColorRGBA secondary = fromHex("#15091a");

    static {
        DevMode.registerReloadCallback(NGEStyle.class, NGEStyle::install);
    }

    public static void installAndUse(int width, int height) {
        installAndUse((float) width, (float) height);
    }

    public static void installAndUse(float width, float height) {
        install(width, height);
        use();
    }

    public static void  installAndUse() {
        installAndUse(-1, -1);
    }

    public static void use() {
    
        NGEGui.getStyles().setDefaultStyle(NAME);
    }

    private static float width = 1280;
    private static float height = 720;
    private static final float REFERENCE_MIN_AXIS = 720f;

    public static float vmin(float f) {
        return Math.min(width, height) / 100f * f;
    }

    public static float vmax(float f) {
        return Math.max(width, height) / 100f * f;
    }

    public static float vw(float f) {
        return width / 100f * f;
    }

    public static float vh(float f) {
        return height / 100f * f;
    }

    public static boolean isRelativeSize() {
        return height <= 1.0001f;
    }

    public static float px(float f) {
        return vmin(100f) / REFERENCE_MIN_AXIS * f;
    }

    public static void install() {
        install(-1, -1);
    }

    

    public static void install(int width, int height) {
        install((float) width, (float) height);
    }

    public static void install(float width, float height) {
        NGEStyle.width = width > 0 ? width : 1280f;
        NGEStyle.height = height > 0 ? height : 720f;

        System.out.println("Installing NGEStyle");
        Styles styles = NGEGui.getStyles();

        ColorRGBA darkPurple = fromHex("#050609fb");
        ColorRGBA mediumPurple = fromHex("#151922f8");
        ColorRGBA lightPurple = fromHex("#d7d9df");
        ColorRGBA accentPurple = fromHex("#f1b84b");
        ColorRGBA glowPurple = fromHex("#79b7ff");
        ColorRGBA glowPurpleA = fromHex("#79b7ff88");
        ColorRGBA neonBlue = fromHex("#79b7ff");
        ColorRGBA neonBlueA = fromHex("#79b7ff65");
        ColorRGBA neontoshi = fromHex("#f1b84b");
        ColorRGBA transparent = new ColorRGBA(0, 0, 0, 0);

        ColorRGBA errorColor = fromHex("#4a1116f8");
        ColorRGBA warningColor = fromHex("#4a3511f8");
        ColorRGBA infoColor = fromHex("#102a40f8");

        darkPurple.a = 0.98f;
        ColorRGBA backgroundColor = darkPurple;
        ColorRGBA seconBackground = mediumPurple;
        mediumPurple.a = 0.95f;

        Attributes glob = styles.getSelector(NAME);
        glob.set("fontSize", vmin(2.1f));
        glob.set("shadowOffset", new Vector3f(px(1), -px(1), -px(1)));
        
        {
            Material highlightMat = new Material(
                NGEGui.getAssetManager(),
                "Common/MatDefs/Misc/Unshaded.j3md"
            );
            highlightMat.getAdditionalRenderState().setBlendMode(BlendMode.AlphaAdditive);
            ColorRGBA colorRGBA = neonBlue.clone().multLocal(0.38f);
            highlightMat.setColor("Color", colorRGBA);

            glob.set("effects", Map.of(
                "focus", new FocusEffect(true,highlightMat),
                "unfocus", new FocusEffect(false,highlightMat)
            ));
        

        }

        {
            Attributes attrs = styles.getSelector(TextField.ELEMENT_ID, NAME);
            attrs.set("background", new QuadBackgroundComponent(new ColorRGBA(0,0,0,0.94f)), false);
            attrs.set("singleLine", true);
        }

        {
            // ElementId parnet = new ElementId(Spinner.ELEMENT_ID);  
            // styles.getSelector(parent.child(Spinner.UP_ID), NAME).set("text", "+", false);
            // styles.getSelector(parent.child(Spinner.UP_ID), NAME).set("insets", new Insets3f(0, 0, 0, 0), false);
            // styles.getSelector(parent.child(Spinner.DOWN_ID), NAME).set("text", "-", false);
            // styles.getSelector(parent.child(Spinner.DOWN_ID), NAME).set("insets", new Insets3f(0, 0, 0, 0), false);
            // styles.getSelector(parent.child(Spinner.VALUE_ID), NAME).set("textVAlignment", VAlignment.Center, false);
        }

        {
            ElementId parent = new ElementId(Slider.ELEMENT_ID);
            styles.getSelector(parent.child(Slider.UP_ID), NAME).set("text", "^", false);
            styles.getSelector(parent.child(Slider.DOWN_ID), NAME).set("text", "v", false);
            styles.getSelector(parent.child(Slider.LEFT_ID), NAME).set("text", "<", false);
            styles.getSelector(parent.child(Slider.RIGHT_ID), NAME).set("text", ">", false);
            styles.getSelector(parent.child(Slider.THUMB_ID), NAME).set("text", "#", false);

        }

        {
            ElementId parent = new ElementId(Selector.ELEMENT_ID);
            styles.getSelector(parent.child(Selector.EXPANDER_ID), NAME).set("text", "v", false);
            Attributes optionList  = styles.getSelector(parent.child(Selector.CONTAINER_ID), NAME);
            QuadBackgroundComponent quad = new QuadBackgroundComponent(darkPurple);
            quad.getMaterial().getMaterial().getAdditionalRenderState().setBlendMode(BlendMode.Exclusion);
            optionList.set("background", quad, false);       

        }

      {        
            ElementId parent = new ElementId(ProgressBar.ELEMENT_ID);        
            styles.getSelector(parent.child(ProgressBar.CONTAINER_ID), NAME).set("background", 
                                                    new QuadBackgroundComponent(NGEGui.srgbaColor(new ColorRGBA(0.2f, 0.2f, 0.2f, 0.5f), true)
                                                   , px(2), px(2)) );
            styles.getSelector(parent.child(ProgressBar.VALUE_ID), NAME).set("background", 
                                                    new QuadBackgroundComponent(NGEGui.srgbaColor(new ColorRGBA(0.1f, 0.7f, 0.3f, 1), true))); 
            styles.getSelector(parent.child(ProgressBar.LABEL_ID), NAME).set("textHAlignment", HAlignment.Center, false);
        }

        {        
            Attributes attrs = styles.getSelector(Panel.ELEMENT_ID, NAME);
            attrs.set( "background", new QuadBackgroundComponent(mediumPurple) , false );
        }

        {
            Attributes attrs = styles.getSelector(NPanel.ELEMENT_ID, NAME);
            attrs.set("insets", new Insets3f(px(5f), px(8f), px(5f), px(8f)), false);
        }

        {
            Attributes chip = styles.getSelector(NChip.PANEL_ID, NAME);
            QuadBackgroundComponent background = new QuadBackgroundComponent(mediumPurple);
            chip.set("background", background, false);

            Attributes chipLabel = styles.getSelector(NChip.LABEL_ID, NAME);
            chipLabel.set("fontSize", px(14f), false);
            chipLabel.set("color", lightPurple, false);
            chipLabel.set("textHAlignment", HAlignment.Left, false);
            chipLabel.set("textVAlignment", VAlignment.Center, false);

            Attributes chipIcon = styles.getSelector(NChip.ICON_ID, NAME);
            QuadBackgroundComponent iconBackground = new QuadBackgroundComponent(accentPurple);
            iconBackground.setMargin(px(3f), px(3f));
            chipIcon.set("background", iconBackground, false);
            chipIcon.set("fontSize", px(12.5f), false);
            chipIcon.set("color", darkPurple, false);
            chipIcon.set("textHAlignment", HAlignment.Center, false);
            chipIcon.set("textVAlignment", VAlignment.Center, false);
        }

        {
            ElementId parent = new ElementId(ListBox.ELEMENT_ID);
            ColorRGBA selectorColor = accentPurple.clone();
            selectorColor.a = 0.72f;
            QuadBackgroundComponent quad = new QuadBackgroundComponent(selectorColor);
            styles.getSelector(parent.child(ListBox.SELECTOR_ID), NAME).set("background", quad, false);        
        }


        {
            Attributes attrs = styles.getSelector(Container.ELEMENT_ID, NAME);
            attrs.set("layout", new SpringGridLayout(), false);
        }


        {
            Attributes attrs = styles.getSelector(Checkbox.ELEMENT_ID, NAME);
            IconComponent on = new IconComponent("/org/ngengine/gui/icons/Check.png", px(1.2f),
                                   px(2), px(2), 0.01f, false);
            IconComponent off = new IconComponent("/org/ngengine/gui/icons/Check.png", px(1.2f),
                                    px(2), px(2), 0.01f, false);
            off.setColor(new ColorRGBA(0,0,0,0));

            attrs.set("background", new QuadBackgroundComponent( new ColorRGBA(0,0,0,0) ), false);
            attrs.set("onView", on, false);
            attrs.set("offView", off, false);
            attrs.set("textVAlignment", VAlignment.Center, false);
                        // attrs.set("focusColor", ColorRGBA.Green, false);       // green should not need srgb conversion

        }

        {
            Attributes attrs = styles.getSelector(Button.ELEMENT_ID, NAME);
            attrs.set("background", new QuadBackgroundComponent(new ColorRGBA(0,0,0,0)), false);
            attrs.set("highlightColor", ColorRGBA.Yellow, false);  // yellow should not need srgb conversion
            // attrs.set("focusColor", ColorRGBA.Green, false);       // green should not need srgb conversion
            attrs.set("shadowColor", NGEGui.srgbaColor(new ColorRGBA(0, 0, 0, 0.5f), true), false);
            attrs.set("clickSound", "org/ngengine/gui/sounds/button-click.ogg", false);
            attrs.set("clickSoundVolume", 0.25f, false);
        }

         {
            Attributes attrs = styles.getSelector("loading-spinner", NAME);
             ColorRGBA gray = NGEGui.srgbaColor(ColorRGBA.Gray, true);
            attrs.set("color", gray, false);
         }
        {
            Attributes container = styles.getSelector("container", NAME);
            QuadBackgroundComponent containerBackground = new QuadBackgroundComponent(transparent);
            container.set("background", containerBackground);
            container.set("insets", new Insets3f(vmin(1), vmin(1), vmin(1), vmin(1)));
        }

        {
            Attributes container = styles.getSelector("window", NAME);
            int x1 = 15;
            int x2 = 48-15;
            int y1 = 15;
            int y2 = 48-15;
            float scale = px(1f);

            TbtQuadBackgroundComponent background = TbtQuadBackgroundComponent.create(
                "org/ngengine/gui/border/bg.png",
                scale,
                x1,
                y1,
                x2,
                y2,
                1f,
                false
            );
            background.setMargin(new Vector2f(px(10), px(10)));
            ColorRGBA windowBackground = mediumPurple.clone();
            windowBackground.a = 0.98f;
            background.setColor(windowBackground);
            container.set("background", background);
            container.set("selectionBackground", new QuadBackgroundComponent(mediumPurple));
        }

        {

            Attributes title = styles.getSelector("window.title", NAME);
            title.set("fontSize", vmin(4));
            title.set("background", new QuadBackgroundComponent(transparent));
            title.set("color", lightPurple);
            title.set("insets", new Insets3f(vh(0.4f), vmin(2.0f), vh(0.4f), vmin(2.0f)));
        }

        {
            Attributes title = styles.getSelector("window.titleBar", NAME);

            title.set("insets", new Insets3f(vh(1.2f), vh(1.2f), vh(3), vh(1.2f)));
        }

        {
            Attributes label = styles.getSelector("label", NAME);
            label.set("color", lightPurple);
            label.set("insets", new Insets3f(vmin(1), vmin(1), vmin(1), vmin(1)));
        }

        {
            Attributes warnLabel = styles.getSelector("label.warning", NAME);
            warnLabel.set("color", neontoshi);
            warnLabel.set("fontSize", vmin(1.9f));
            warnLabel.set("insets", new Insets3f(vmin(2), vmin(2), vmin(2), vmin(2)));
            TbtQuadBackgroundComponent border = TbtQuadBackgroundComponent.create(
                "/org/ngengine/gui/icons/border.png",
                px(1),
                6,
                6,
                6,
                6,
                1f,
                false
            );
            border.setColor(neontoshi);
            border.setMargin(vh(2), vh(2));
            warnLabel.set("background", border);
        }
        {
            Attributes highlightedLabel = styles.getSelector("label.highlighted", NAME);
            highlightedLabel.set("color", lightPurple);
        }
        {
            Attributes button = styles.getSelector("button.default", NAME);
            button.set("color", lightPurple);
        }

        {
            Attributes button = styles.getSelector("button.default.selected", NAME);
            button.set("color", lightPurple);
        }

        {
            Attributes button = styles.getSelector("button.default.hovered", NAME);
            button.set("color", lightPurple);
        }

        {
            Attributes button = styles.getSelector("button.default.pressed", NAME);
            button.set("color", lightPurple);
        }

        {
            Attributes button = styles.getSelector("button", NAME);
            int x1 = 23;
            int x2 = 48-23;
            int y1 = 23;
            int y2 = 48-23;
            float scale = px(1f);
            TbtQuadBackgroundComponent bg = TbtQuadBackgroundComponent.create(
                "org/ngengine/gui/border/btn0.png",
                scale,
                x1,
                y1,
                x2,
                y2,
                1f,
                false
            );
            bg.setColor(mediumPurple);
            // QuadBackgroundComponent bg = new QuadBackgroundComponent(mediumPurple);
            bg.setMargin(vmin(2.2f), vmin(2.2f));

            button.set("background", bg);
            button.set("color", lightPurple);
            // button.set("focusColor", lightPurple);
            button.set("focusShadowColor", transparent);
            button.set("highlightColor", glowPurple);
            button.set("highlightShadowColor", transparent);
            button.set("insets", new Insets3f(vmin(0.4f), vmin(0.4f), vmin(0.4f), vmin(0.4f)));
            button.set("textHAlignment", HAlignment.Center, false);
            button.set("textVAlignment", VAlignment.Center, false);
            // button.set("insets", new Insets3f(vmin(1), vmin(1), vmin(2), vmin(2)));
            // TbtQuadBackgroundComponent background = TbtQuadBackgroundComponent.create("ui/button.png",
            // 0.1f,
            // 2, 2, 2, 2

            // , 1f, false);
            // bg.setMargin(vmin(1.2f), vmin(1.2f));
            // background.setColor(mediumPurple);
            // button.set("background", background);
        }

        {
            Attributes iconButton = styles.getSelector("iconButton", NAME);
            iconButton.set("insets", new Insets3f(vmin(1), vmin(1), vmin(1), vmin(1)));
            iconButton.set("color", lightPurple);
            QuadBackgroundComponent buttonBg = new QuadBackgroundComponent(new ColorRGBA(0, 0.75f, 0.75f, 0f));
            iconButton.set("background", buttonBg);
        }

        {
            Attributes textField = styles.getSelector("textField", NAME);
            textField.set("color", lightPurple);

            QuadBackgroundComponent bg = new QuadBackgroundComponent(mediumPurple);
            bg.setMargin(vmin(1.2f), vmin(1.2f));
            textField.set("background", bg);
        }

        {
            float squareSize = vmin(2.8f);

            Attributes checkbox = styles.getSelector("checkbox", NAME);
            IconComponent on = new NSVGIcon("org/ngengine/gui/icons/outline/square-check.svg", squareSize, squareSize);
            on.setColor(new ColorRGBA(0.5f, 0.9f, 0.9f, 0.9f));
            on.setMargin(px(5), 0);
            on.setColor(lightPurple);

            IconComponent off = new NSVGIcon("org/ngengine/gui/icons/outline/square.svg", squareSize, squareSize);
            off.setColor(new ColorRGBA(0.6f, 0.8f, 0.8f, 0.8f));
            off.setMargin(px(5), 0);
            off.setColor(lightPurple);

            checkbox.set("onView", on);
            checkbox.set("offView", off);
            checkbox.set("color", lightPurple);
        }

        {
            Attributes qr = styles.getSelector("qr", NAME);
            qr.set("insets", new Insets3f(vmin(1), vmin(1), vmin(1), vmin(1)));
            qr.set("lightPixelsColor", lightPurple);
            qr.set("darkPixelsColor", transparent);
            // QuadBackgroundComponent bg = qr.get("background");
            // bg.setColor(mediumPurple);

        }

        {
            Attributes spinner = styles.getSelector("loading-spinner", NAME);
            spinner.set("color", lightPurple);
        }

        {
            Attributes slider = styles.getSelector("slider", NAME);
            ColorRGBA sliderBgColor = fromHex("#202733aa");
            ColorRGBA sliderButtonColor = fromHex("#303846cc");
            ColorRGBA sliderThumbColor = fromHex("#b9c3d6cc");
            QuadBackgroundComponent bg = new QuadBackgroundComponent(sliderBgColor);
            QuadBackgroundComponent buttonBg = new QuadBackgroundComponent(sliderButtonColor);
            buttonBg.setMargin(vmin(0.8f), 0);
            QuadBackgroundComponent thumbBg = new QuadBackgroundComponent(sliderThumbColor);
            thumbBg.setMargin(vmin(0.8f), 0);

            slider.set("background", bg);
            slider.set("insets", new Insets3f(0, vmin(0.6f), 0, 0));

            for (String sliderButton : List.of("left", "right", "up", "down")) {
                Attributes sliderButtonAttr = styles.getSelector("slider." + sliderButton + ".button", NAME);
                sliderButtonAttr.set("color", lightPurple);
                sliderButtonAttr.set("insets", new Insets3f(0, 0, 0, 0));
                sliderButtonAttr.set("text", "");
                sliderButtonAttr.set("background", buttonBg.clone());
            }

            Attributes sliderThumb = styles.getSelector("slider.thumb.button", NAME);
            sliderThumb.set("text", "");
            sliderThumb.set("background", thumbBg);

            Attributes sliderButton = styles.getSelector("sliderButton", NAME);
            sliderButton.set("background", buttonBg);
            sliderButton.set("insets", new Insets3f(0, 0, 0, 0));
        }

        {
            Attributes listItems = styles.getSelector("list.items", NAME);

            listItems.set("insets", new Insets3f(0, 0, 0, 0));
        }

        {
            Attributes listSelector = styles.getSelector("list.selector", NAME);

            ColorRGBA selectorColor = mediumPurple.clone();
            selectorColor.a = 0.72f;
            QuadBackgroundComponent bg = new QuadBackgroundComponent(selectorColor);
            bg.setMargin(0, 0);
            listSelector.set("background", bg);
        }

        {
            {
                Attributes toast = styles.getSelector("toast", NAME);
                toast.set("insets", new Insets3f(vmin(1.4f), vmin(1.8f), vmin(1.4f), vmin(1.8f)));
            }

            {
                Attributes errorToast = styles.getSelector("error.toast", NAME);
                QuadBackgroundComponent bg = new QuadBackgroundComponent(errorColor);
                bg.setMargin(vmin(.8f), vmin(1.8f));
                errorToast.set("background", bg);
            }

            {
                Attributes infoToast = styles.getSelector("info.toast", NAME);
                QuadBackgroundComponent bg = new QuadBackgroundComponent(infoColor);
                bg.setMargin(vmin(.8f), vmin(1.8f));
                infoToast.set("background", bg);
            }

            {
                Attributes warnToast = styles.getSelector("warning.toast", NAME);
                QuadBackgroundComponent bg = new QuadBackgroundComponent(warningColor);
                bg.setMargin(vmin(.8f), vmin(1.8f));
                warnToast.set("background", bg);
            }

            {
                float iconSize = vmin(2.1f);
                NSVGIcon on = new NSVGIcon("org/ngengine/gui/icons/outline/x.svg", iconSize, iconSize);

                Attributes closeBtn = styles.getSelector("toast.close.iconButton", NAME);
                closeBtn.set("iconSize", vmin(2.1f));
                closeBtn.set("fontSize", vmin(2.1f));
                closeBtn.set("color", lightPurple);
                closeBtn.set("svgIconComponent", on);
                closeBtn.set("preferredSize", new Vector3f(vmin(4.4f), vmin(4.4f), 0f));
                closeBtn.set("insets", new Insets3f(0f, 0f, 0f, 0f));
            }

            {
                float iconSize = vmin(2.1f);
                Attributes toastIcon = styles.getSelector("toast.iconButton", NAME);
                NSVGIcon on = new NSVGIcon("org/ngengine/gui/icons/outline/info-square-rounded.svg", iconSize, iconSize);
                toastIcon.set("color", lightPurple);
                toastIcon.set("svgIconComponent",on );
                toastIcon.set("preferredSize", new Vector3f(vmin(4.4f), vmin(4.4f), 0f));
                toastIcon.set("insets", new Insets3f(0f, 0f, 0f, 0f));
            }

            {
                Attributes errorToastIcon = styles.getSelector("error.toast.iconButton", NAME);
                float iconSize = vmin(2.1f);
                NSVGIcon on = new NSVGIcon("org/ngengine/gui/icons/outline/alert-triangle.svg", iconSize, iconSize);
                errorToastIcon.set("svgIconComponent",on );
            }

            {
                Attributes warningToastIcon = styles.getSelector("warning.toast.iconButton", NAME);
                float iconSize = vmin(2.1f);
                NSVGIcon on = new NSVGIcon("org/ngengine/gui/icons/outline/alert-triangle.svg", iconSize, iconSize);
                warningToastIcon.set("svgIconComponent",on );
            }

            {
                Attributes toastLabel = styles.getSelector("toast.label", NAME);
                toastLabel.set("textHAlignment", HAlignment.Left);
                toastLabel.set("textVAlignment", VAlignment.Center);
                toastLabel.set("insets", new Insets3f(0f, vmin(0.8f), 0f, vmin(0.8f)));
            }
        }
    }

    public static QuadBackgroundComponent solidBackground(ColorRGBA color) {
        QuadBackgroundComponent bg = new QuadBackgroundComponent(color);
        bg.setMargin(vmin(1.2f), vmin(1.2f));
        return bg;
    }
}
