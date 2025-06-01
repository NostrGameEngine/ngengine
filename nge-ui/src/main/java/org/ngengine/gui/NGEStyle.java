package org.ngengine.gui;

import java.util.List;
import java.util.Map;

import org.ngengine.DevMode;
import org.ngengine.gui.components.NSVGIcon;

import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.jme3.scene.shape.Quad;
import com.jme3.terrain.noise.Color;
import com.simsilica.lemur.Button;
import com.simsilica.lemur.Button.ButtonAction;
import com.simsilica.lemur.Command;
import com.simsilica.lemur.GuiGlobals;
import com.simsilica.lemur.HAlignment;
import com.simsilica.lemur.Insets3f;
import com.simsilica.lemur.VAlignment;
import com.simsilica.lemur.component.IconComponent;
import com.simsilica.lemur.component.QuadBackgroundComponent;
import com.simsilica.lemur.component.TbtQuadBackgroundComponent;
import com.simsilica.lemur.style.Attributes;
import com.simsilica.lemur.style.Styles;

public class NGEStyle {
    private static final String NAME = "nge";

    public static ColorRGBA fromHex(String hex) {
        ColorRGBA linear = new ColorRGBA();
        linear.setAsSrgb(Integer.valueOf(hex.substring(1, 3), 16) / 255f,
                Integer.valueOf(hex.substring(3, 5), 16) / 255f,
                Integer.valueOf(hex.substring(5, 7), 16) / 255f,
                hex.length() > 7 ? Integer.valueOf(hex.substring(7, 9), 16) / 255f : 1f);

        return linear;
    }

    static ColorRGBA secondary = fromHex("#15091a");

    static {
        DevMode.registerReloadCallback(NGEStyle.class, NGEStyle::install);
    }

    public static void installAndUse() {
        install();
        use();
    }

    public static void use() {
        GuiGlobals globals = GuiGlobals.getInstance();
        globals.getStyles().setDefaultStyle(NAME);
    }

    static int width = 1280;
    static int height = 720;

    public static int vmin(float f) {
        return (int) (Math.min((double) width, (double) height) / 100. * f);
    }

    public static int vmax(float f) {
        return (int) (Math.max((double) width, (double) height) / 100. * f);
    }

    public static int vw(float f) {
        return (int) ((double) width / 100. * f);
    }

    public static int vh(float f) {
        return (int) ((double) height / 100. * f);
    }

    public static void install() {
        NGEStyle.width = 1280;
        NGEStyle.height = 720;

        System.out.println("Installing NGEStyle");
        GuiGlobals gui = GuiGlobals.getInstance();
        Styles styles = GuiGlobals.getInstance().getStyles();

        // --dark-purple: #1f0f33;
        // --medium-purple: #3E1E68;
        // --light-purple:#9681b6;
        // --accent-purple: #E75AFF;
        // --glow-purple: #C47AFF;
        // --glow-purpleA: rgba(195, 122, 255, 0.5);
        // --text-color: #E0D5FF;
        // --neon-blue: #00F0FF;
        // --neon-blueA:#062d5a;
        // --neontoshi:#c59d6c;

        ColorRGBA darkPurple = fromHex("#1f0f33");
        ColorRGBA mediumPurple = fromHex("#3E1E68");
        ColorRGBA lightPurple = fromHex("#9681b6");
        ColorRGBA accentPurple = fromHex("#E75AFF");
        ColorRGBA glowPurple = fromHex("#C47AFF");
        ColorRGBA glowPurpleA = fromHex("#C47AFF");
        // ColorRGBA textColor = fromHex("#E0D5FF");
        ColorRGBA neonBlue = fromHex("#00F0FF");
        ColorRGBA neonBlueA = fromHex("#00eeff65");
        ColorRGBA neontoshi = fromHex("#c59d6c");
        ColorRGBA transparent = new ColorRGBA(0, 0, 0, 0);

        ColorRGBA errorColor = fromHex("#5a062d");
        ColorRGBA warningColor = fromHex("#545a06");
        ColorRGBA infoColor = fromHex("#062d5a");

        darkPurple.a = 0.95f;
        ColorRGBA backgroundColor = darkPurple;
        ColorRGBA seconBackground = mediumPurple;
        mediumPurple.a = 0.7f;

        Attributes glob = styles.getSelector(NAME);
        glob.set("fontSize", vmin(2.1f));
        {

        }

        {
            Attributes container = styles.getSelector("container", NAME);
            QuadBackgroundComponent containerBackground = new QuadBackgroundComponent(transparent);
            container.set("background", containerBackground);
            container.set("insets", new Insets3f(vmin(1), vmin(1), vmin(1), vmin(1)));
        }

        {
            Attributes container = styles.getSelector("window", NAME);
            int x1 = 100;
            int x2 = 100;
            int y1 = 100;
            int y2 = 100;
            float scale = 0.24f;

            TbtQuadBackgroundComponent background = TbtQuadBackgroundComponent.create("ui/frame.png", scale,
                    x1, y1, x2, y2

                    , 1f, false);
            background.setMargin(new Vector2f(10, 10));
            background.setColor(darkPurple);
            container.set("background", background);
            container.set("selectionBackground", new QuadBackgroundComponent(mediumPurple));

        }

        {
            Attributes title = styles.getSelector("window.title", NAME);
            title.set("fontSize", vmin(4));
            title.set("background", new QuadBackgroundComponent(transparent));
            title.set("color", lightPurple);

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
            TbtQuadBackgroundComponent border = TbtQuadBackgroundComponent
                    .create("/com/simsilica/lemur/icons/border.png", 1, 6, 6, 6, 6, 1f, false);
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
            QuadBackgroundComponent bg = new QuadBackgroundComponent(mediumPurple);
            bg.setMargin(vmin(1.2f), vmin(1.2f));

            button.set("background", bg);
            button.set("color", lightPurple);
            button.set("focusColor", lightPurple);
            button.set("focusShadowColor", transparent);
            button.set("highlightColor", glowPurple);
            button.set("highlightShadowColor", transparent);
            button.set("insets", new Insets3f(vmin(0.4f), vmin(0.4f), vmin(0.4f), vmin(0.4f)));

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
            QuadBackgroundComponent buttonBg = new QuadBackgroundComponent(
                    new ColorRGBA(0, 0.75f, 0.75f, 0f));
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
            int squareSize = (int) (vmin(2.8f));

            Attributes checkbox = styles.getSelector("checkbox", NAME);
            IconComponent on = new NSVGIcon("icons/outline/square-check.svg", squareSize, squareSize);
            on.setColor(new ColorRGBA(0.5f, 0.9f, 0.9f, 0.9f));
            on.setMargin(5, 0);
            on.setColor(lightPurple);

            IconComponent off = new NSVGIcon("icons/outline/square.svg", squareSize, squareSize);
            off.setColor(new ColorRGBA(0.6f, 0.8f, 0.8f, 0.8f));
            off.setMargin(5, 0);
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
            QuadBackgroundComponent bg = new QuadBackgroundComponent(darkPurple);
            QuadBackgroundComponent bg2 = bg.clone();
            bg2.setColor(mediumPurple);
            bg2.setMargin(vmin(0.8f), 0);

            slider.set("background", bg);
            slider.set("insets", new Insets3f(0, vmin(0.6f), 0, 0));

            for (String sliderButton : List.of("left", "right", "up", "down")) {
                Attributes sliderButtonAttr = styles.getSelector("slider." + sliderButton + ".button", NAME);
                sliderButtonAttr.set("color", lightPurple);
                sliderButtonAttr.set("insets", new Insets3f(0, 0, 0, 0));
                sliderButtonAttr.set("text", "");
                sliderButtonAttr.set("background", bg2);

            }

            Attributes sliderThumb = styles.getSelector("slider.thumb.button", NAME);
            sliderThumb.set("text", "");
            sliderThumb.set("background", bg2);

            Attributes sliderButton = styles.getSelector("sliderButton", NAME);
            sliderButton.set("background", bg2);
            sliderButton.set("insets", new Insets3f(0, 0, 0, 0));

        }

        {
            Attributes listItems = styles.getSelector("list.items", NAME);

            listItems.set("insets", new Insets3f(0, 0, 0, 0));

        }

        {
            Attributes listSelector = styles.getSelector("list.selector", NAME);

            QuadBackgroundComponent bg = new QuadBackgroundComponent(mediumPurple);
            bg.setMargin(0, 0);
            listSelector.set("background", bg);
        }

        {
            {
                Attributes toast = styles.getSelector("toast", NAME);
                toast.set("insets", new Insets3f(vmin(1), vmin(1), vmin(1), vmin(1)));
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
                Attributes closeBtn = styles.getSelector("toast.close.iconButton", NAME);
                closeBtn.set("iconSize", vmin(2.1f));
                closeBtn.set("fontSize", vmin(2.1f));
                closeBtn.set("color", lightPurple);
                closeBtn.set("svgIcon", "icons/outline/x.svg");

            }

            {
                Attributes toastIcon = styles.getSelector("toast.iconButton", NAME);
                toastIcon.set("color", lightPurple);
                toastIcon.set("svgIcon", "icons/outline/info-square-rounded.svg");
            }

            {
                Attributes errorToastIcon = styles.getSelector("error.toast.iconButton", NAME);
                errorToastIcon.set("svgIcon", "icons/outline/alert-octagon.svg");
            }

            {
                Attributes warningToastIcon = styles.getSelector("warning.toast.iconButton", NAME);
                warningToastIcon.set("svgIcon", "icons/outline/alert-triangle.svg");
            }

            {
                Attributes toastLabel = styles.getSelector("toast.label", NAME);
                toastLabel.set("textHAlignment", HAlignment.Left);
                toastLabel.set("textVAlignment", VAlignment.Center);
            }

        }

    }

    public static QuadBackgroundComponent solidBackground(ColorRGBA color) {
        QuadBackgroundComponent bg = new QuadBackgroundComponent(color);
        bg.setMargin(vmin(1.2f), vmin(1.2f));
        return bg;
    }
}
