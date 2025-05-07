package com.simsilica.lemur.style.base.styles;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.text.Style;

import com.jme3.asset.AssetManager;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.simsilica.lemur.Button;
import com.simsilica.lemur.Button.ButtonAction;
import com.simsilica.lemur.Command;
import com.simsilica.lemur.GuiGlobals;
import com.simsilica.lemur.Insets3f;
import com.simsilica.lemur.component.IconComponent;
import com.simsilica.lemur.component.QuadBackgroundComponent;
import com.simsilica.lemur.component.TbtQuadBackgroundComponent;
import com.simsilica.lemur.style.Attributes;
import com.simsilica.lemur.style.Styles;


public class GlassStyle {
    private static final String NAME = "glass";
   

    public static void installAndUse(){
        install();
        use();
    }
    
    public static void use(){
        GuiGlobals globals = GuiGlobals.getInstance();
        globals.getStyles().setDefaultStyle(NAME);
    }
    
    public static void install() {
        GuiGlobals gui = GuiGlobals.getInstance();
        Styles styles = GuiGlobals.getInstance().getStyles();

        TbtQuadBackgroundComponent gradient = TbtQuadBackgroundComponent.create(
            "/com/simsilica/lemur/icons/bordered-gradient.png",
            1,1,1,126,126,1f,false);

        TbtQuadBackgroundComponent bevel = TbtQuadBackgroundComponent.create(
                "/com/simsilica/lemur/icons/bevel-quad.png", 
                0.125f, 8, 8, 119, 119, 1f, false);

        TbtQuadBackgroundComponent border = TbtQuadBackgroundComponent.create(
               "/com/simsilica/lemur/icons/border.png",
                1, 1, 1, 6, 6, 1f, false);

        TbtQuadBackgroundComponent border2 = TbtQuadBackgroundComponent.create(
               "/com/simsilica/lemur/icons/border.png", 
                1, 2, 2, 6, 6, 1f, false);

        QuadBackgroundComponent doubleGradient = new QuadBackgroundComponent(new ColorRGBA(0.5f, 0.75f, 0.85f, 0.5f));
        doubleGradient.setTexture(
                gui.loadTexture("/com/simsilica/lemur/icons/double-gradient-128.png",true, false));


        Command<Button> pressedCommand = (source)->{
            if( source.isPressed() ) {
                source.move(1, -1, 0);
            } else {
                source.move(-1, 1, 0);
            }
        };
   

        Command<Button> repeatCommand = new Command<Button>() {
            private long startTime;
            private long lastClick;

            public void execute(Button source) {
                // Only do the repeating click while the mouse is
                // over the button (and pressed of course)
                if (source.isPressed() && source.isHighlightOn()) {
                    long elapsedTime = System.currentTimeMillis() - startTime;
                    // After half a second pause, click 8 times a second
                    if (elapsedTime > 500) {
                        if (elapsedTime - lastClick > 125) {
                            source.click();

                            // Try to quantize the last click time to prevent drift
                            lastClick = ((elapsedTime - 500) / 125) * 125 + 500;
                        }
                    }
                } else {
                    startTime = System.currentTimeMillis();
                    lastClick = 0;
                }
            }
        };

        Map<ButtonAction, List<Command<? super Button>>> sliderButtonCommands = Map.of(
            ButtonAction.Hover, List.of(repeatCommand)
        );

        Map<ButtonAction, List<Command<? super Button>>> stdButtonCommands = Map.of(ButtonAction.Down,
                List.of(pressedCommand), ButtonAction.Up, List.of(pressedCommand));

  
        Attributes glob = styles.getSelector(NAME);
        glob.set("fontSize", 14);

        Attributes label = styles.getSelector( "label", NAME);
        label.set("insets", new Insets3f(2, 2, 0, 2));
        label.set("color", new ColorRGBA(.5f, .75f,.75f,.85f));


        Attributes container = styles.getSelector( "container", NAME);
        TbtQuadBackgroundComponent containerBackground = gradient.clone();
        containerBackground.setColor(new ColorRGBA(.25f,.5f,.5f,.5f));
        container.set("background", containerBackground);

  

        Attributes title = styles.getSelector( "title", NAME);
        title.set("color", new ColorRGBA(0.8f, 0.9f, 1f, 0.85f));
        title.set("highlightColor", new ColorRGBA(1, 0.8f, 1, 0.85f));
        title.set("shadowColor", new ColorRGBA(0, 0, 0, 0.75f));
        title.set("shadowOffset", new Vector3f(2, -2, -1));
        QuadBackgroundComponent titleBackground = new QuadBackgroundComponent(new ColorRGBA(0.5f, 0.75f, 0.85f, 0.5f));
        titleBackground.setTexture(gui.loadTexture("/com/simsilica/lemur/icons/double-gradient-128.png", true, false));
        title.set("background", titleBackground);
        title.set("insets", new Insets3f( 2, 2, 2, 2 ));
        title.set("buttonCommands", stdButtonCommands);


        Attributes button = styles.getSelector( "button", NAME);
        TbtQuadBackgroundComponent buttonBackground = gradient.clone();
        buttonBackground.setColor(new ColorRGBA(0, 0.75f, 0.75f, 0.5f));
        button.set("background", buttonBackground);
        button.set("color", new ColorRGBA(0.8f, 0.9f, 1f, 0.85f));
        button.set("insets", new Insets3f( 2, 2, 2, 2 ));
        button.set("buttonCommands", stdButtonCommands);
           


        Attributes slider = styles.getSelector( "slider", NAME);
        TbtQuadBackgroundComponent sliderBackground = gradient.clone();
        sliderBackground.setColor(new ColorRGBA(.25f,.5f,.5f,.5f));
        slider.set("background", sliderBackground);
        slider.set("insets", new Insets3f( 1, 3, 1, 2 ));

        Attributes sliderButton = styles.getSelector( "sliderButton", NAME);
        QuadBackgroundComponent sliderButtonBackground = doubleGradient.clone();
        sliderButtonBackground.setColor(new ColorRGBA(0.5f, 0.75f, 0.75f, 0.5f));
        sliderButton.set("background", sliderButtonBackground);
        sliderButton.set("insets", new Insets3f( 0, 0, 0, 0 ));


        Attributes sliderThumb = styles.getSelector( "slider.thumb.button", NAME);
        sliderThumb.set("text", "[]");
        sliderThumb.set("color", new ColorRGBA(0.6f, 0.8f, 0.8f, 0.85f));

        Attributes sliderLeftButton = styles.getSelector( "slider.left.button", NAME);
        sliderLeftButton.set("text", "-");
        QuadBackgroundComponent sliderLeftButtonBackground = doubleGradient.clone();
        sliderLeftButtonBackground.setColor(new ColorRGBA(0.5f, 0.75f, 0.75f, 0.5f));
        sliderLeftButtonBackground.setMargin(5,0);
        sliderLeftButton.set("background", sliderLeftButtonBackground);
        sliderLeftButton.set("color", new ColorRGBA(0.6f, 0.8f, 0.8f, 0.85f));
        sliderLeftButton.set("buttonCommands", sliderButtonCommands);


        Attributes sliderRightButton = styles.getSelector("slider.right.button", NAME);
        sliderRightButton.set("text", "+");
        QuadBackgroundComponent sliderRightButtonBackground = doubleGradient.clone();
        sliderRightButtonBackground.setColor(new ColorRGBA(0.5f, 0.75f, 0.75f, 0.5f));
        sliderRightButtonBackground.setMargin(4, 0);
        sliderRightButton.set("background", sliderRightButtonBackground);
        sliderRightButton.set("color", new ColorRGBA(0.6f, 0.8f, 0.8f, 0.85f));
        sliderRightButton.set("buttonCommands", sliderButtonCommands);
  

        Attributes sliderUpButton = styles.getSelector("slider.up.button", NAME);
        sliderUpButton.set("buttonCommands", sliderButtonCommands);


        Attributes sliderDownButton = styles.getSelector("slider.down.button", NAME);
        sliderDownButton.set("buttonCommands", sliderButtonCommands);


        Attributes checkbox = styles.getSelector( "checkbox", NAME);
        IconComponent on = new IconComponent("/com/simsilica/lemur/icons/Glass-check-on.png", 1f, 0, 0, 1f,
                false);
        on.setColor(new ColorRGBA(0.5f, 0.9f, 0.9f, 0.9f));
        on.setMargin(5,0);

        IconComponent off = new IconComponent("/com/simsilica/lemur/icons/Glass-check-off.png", 1f, 0, 0, 1f,
                false);
        off.setColor(new ColorRGBA(0.6f, 0.8f, 0.8f, 0.8f));
        off.setMargin(5, 0);

        checkbox.set("onView", on);
        checkbox.set("offView", off);
        checkbox.set("color", new ColorRGBA(0.8f, 0.9f, 1, 0.85f));

        Attributes rollup = styles.getSelector("rollup", NAME);
        TbtQuadBackgroundComponent rollupBackground = gradient.clone();
        rollupBackground.setColor(new ColorRGBA(0.25f, 0.5f, 0.5f, 0.5f));
        rollup.set("background", rollupBackground);


        Attributes tabbedPanel = styles.getSelector("tabbedPanel", NAME);
        TbtQuadBackgroundComponent tabbedPanelBackground = gradient.clone();
        tabbedPanelBackground.setColor(new ColorRGBA(0.8f, 0.9f, 1f, 0.85f));
        tabbedPanel.set("background", tabbedPanelBackground);

        Attributes tabbedPanelContainer = styles.getSelector("tabbedPanel.container", NAME);
        tabbedPanelContainer.set("background", null);

        Attributes tabButton = styles.getSelector("tab.button", NAME);
        TbtQuadBackgroundComponent tabButtonBackground = gradient.clone();
        tabButtonBackground.setColor(new ColorRGBA(0.25f, 0.5f, 0.5f, 0.5f));
        tabButton.set("background", tabButtonBackground);
        tabButton.set("color", new ColorRGBA(0.4f, 0.45f, 0.5f, 0.85f));
        tabButton.set("insets", new Insets3f(4, 2, 0, 2));
        tabButton.set("buttonCommands", stdButtonCommands);

    }
}