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

package org.ngengine.gui;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import com.jme3.math.ColorRGBA;
import com.jme3.scene.Spatial;
import org.ngengine.gui.core.CommandMap;
import org.ngengine.gui.core.GuiControl;
import org.ngengine.gui.nav.FocusListener;
import org.ngengine.gui.nav.ScrollDirection;
import org.ngengine.gui.style.ElementId;
import org.ngengine.gui.style.StyleAttribute;
import org.ngengine.gui.style.Styles;

/**
 * A standard Button GUI element that can be clicked to perform an action or set of actions.
 *
 * @author Paul Speed, Riccardo Balbo
 */
public class Button extends Label implements FocusListener {
    private final Logger log = Logger.getLogger(Button.class.getName());

    public static final String ELEMENT_ID = "button";

    public static final String EFFECT_PRESS = "press";
    public static final String EFFECT_RELEASE = "release";
    public static final String EFFECT_CLICK = "click";
    public static final String EFFECT_ACTIVATE = "activate";
    public static final String EFFECT_DEACTIVATE = "deactivate";
    public static final String EFFECT_FOCUS = "focus";
    public static final String EFFECT_UNFOCUS = "unfocus";
    public static final String EFFECT_ENABLE = "enable";
    public static final String EFFECT_DISABLE = "disable";

    public enum ButtonAction {
        Down, Up, Click, HighlightOn, HighlightOff, FocusGained, FocusLost, Hover, Enabled, Disabled
    };

    private boolean enabled = true;
    private ColorRGBA color;
    private ColorRGBA shadowColor;
    private ColorRGBA highlightColor;
    private ColorRGBA highlightShadowColor;
    private ColorRGBA focusColor;
    private ColorRGBA focusShadowColor;
    private boolean highlightOn;
    private boolean focusOn;
    private boolean pressed;
    private CommandMap<Button, ButtonAction> commandMap = new CommandMap<Button, ButtonAction>(this);

  
    public Button() {
        this("");
    }

    public Button(String s) {
        this(s, new ElementId(ELEMENT_ID));
    }
    public Button(ElementId elementId) {
        this("", elementId);
    }
    public Button(String s, ElementId elementId) {
        super(s, elementId);

        // addControl(new MouseEventControl(FocusMouseListener.INSTANCE, new ButtonMouseHandler()));
        getControl(GuiControl.class).addFocusChangeListener(this);
        getControl(GuiControl.class).setFocusable(true);

        applyStyles(Button.class);
    }

    @SuppressWarnings("unchecked") // because Java doesn't like var-arg generics
    public void addCommands(ButtonAction a, Command<? super Button>... commands) {
        commandMap.addCommands(a, commands);
    }

    public List<Command<? super Button>> getCommands(ButtonAction a) {
        return commandMap.get(a, false);
    }

    public void addClickCommands(Command<? super Button> command) {
        commandMap.addCommands(ButtonAction.Click, command);
    }

    @SuppressWarnings("unchecked") // because Java doesn't like var-arg generics
    public void addClickCommands(Command<? super Button>... commands) {
        commandMap.addCommands(ButtonAction.Click, commands);
    }

    @SuppressWarnings("unchecked") // because Java doesn't like var-arg generics
    public void removeClickCommands(Command<? super Button>... commands) {
        getClickCommands().removeAll(Arrays.asList(commands));
    }

    public List<Command<? super Button>> getClickCommands() {
        return commandMap.get(ButtonAction.Click, false);
    }

    @StyleAttribute("buttonCommands")
    public void setButtonCommands(Map<ButtonAction, List<Command<? super Button>>> map) {
        commandMap.clear();
        // We don't use putAll() because (right now) it would potentially
        // put the wrong list implementations into the command map.
        for (Map.Entry<ButtonAction, List<Command<? super Button>>> e : map.entrySet()) {
            commandMap.addCommands(e.getKey(), e.getValue());
        }
    }

    @StyleAttribute("color")
    @Override
    public void setColor(ColorRGBA color) {
        this.color = color;
        super.setColor(color);
    }

    @Override
    public ColorRGBA getColor() {
        return color;
    }

    @StyleAttribute(value = "shadowColor", lookupDefault = false)
    @Override
    public void setShadowColor(ColorRGBA color) {
        this.shadowColor = color;
        super.setShadowColor(shadowColor);
    }

    @Override
    public ColorRGBA getShadowColor() {
        return shadowColor;
    }

    @StyleAttribute(value = "highlightColor", lookupDefault = false)
    public void setHighlightColor(ColorRGBA color) {
        this.highlightColor = color;
        if (isHighlightOn()) {
            resetColors();
        }
    }

    public ColorRGBA getHighlightColor() {
        return highlightColor;
    }

    @StyleAttribute(value = "highlightShadowColor", lookupDefault = false)
    public void setHighlightShadowColor(ColorRGBA color) {
        this.highlightShadowColor = color;
        if (isHighlightOn()) {
            resetColors();
        }
    }

    public ColorRGBA getHighlightShadowColor() {
        return highlightShadowColor;
    }

    @StyleAttribute(value = "focusColor", lookupDefault = false)
    public void setFocusColor(ColorRGBA color) {
        this.focusColor = color;
        if (isFocusHighlightOn()) {
            resetColors();
        }
    }

    public ColorRGBA getFocusColor() {
        return focusColor;
    }

    @StyleAttribute(value = "focusShadowColor", lookupDefault = false)
    public void setFocusShadowColor(ColorRGBA color) {
        this.focusShadowColor = color;
        if (isFocusHighlightOn()) {
            resetColors();
        }
    }

    public ColorRGBA getFocusShadowColor() {
        return focusShadowColor;
    }

    /**
     * Can be called by application code to simulate a click on a button. Note: this will run the click
     * effects/actions but not the press/release actions.
     */
    public void click() {
        runClick();
    }

    public void setEnabled(boolean b) {
        if (this.enabled == b) return;
        this.enabled = b;
        getControl(GuiControl.class).setFocusable(b);

        if (isEnabled()) {
            commandMap.runCommands(ButtonAction.Enabled);
            runEffect(EFFECT_ENABLE);
        } else {
            if (isPressed()) {
                setPressed(false);
            }
            if (isFocusHighlightOn()) {
                showFocus(false);
            }
            commandMap.runCommands(ButtonAction.Disabled);
            runEffect(EFFECT_DISABLE);
        }
    }

    public boolean isEnabled() {
        return enabled;
    }

    public boolean isPressed() {
        return pressed;
    }

    public boolean isHighlightOn() {
        return highlightOn;
    }

    public boolean isFocusHighlightOn() {
        return focusOn;
    }

    public boolean isFocused() {
        return getControl(GuiControl.class).isFocused();
    }

    protected void showHighlight(boolean f) {
        highlightOn = f;
        resetColors();
    }

    protected void showFocus(boolean f) {
        focusOn = f;
        resetColors();
    }

    private static ColorRGBA mix(ColorRGBA c1, ColorRGBA c2) {
        if (c1 == null && c2 == null) {
            return null;
        }
        if (c1 == null) {
            return c2;
        }
        if (c2 == null) {
            return c1;
        }
        return c1.clone().interpolateLocal(c2, 0.5f);
    }

    protected void resetColors() {
        if (focusOn && highlightOn) {
            // Mix them
            ColorRGBA color = mix(getHighlightColor(), getFocusColor());
            if (color != null) {
                super.setColor(color);
            }
            ColorRGBA shadow = mix(getHighlightShadowColor(), getFocusShadowColor());
            if (shadow != null) {
                super.setShadowColor(shadow);
            }
        } else if (highlightOn) {
            if (getHighlightColor() != null) super.setColor(getHighlightColor());
            if (getHighlightShadowColor() != null) super.setShadowColor(getHighlightShadowColor());
        } else if (focusOn) {
            if (getFocusColor() != null) super.setColor(getFocusColor());
            if (getFocusShadowColor() != null) super.setShadowColor(getFocusShadowColor());
        } else {
            // Just the plain color
            super.setColor(getColor());
            super.setShadowColor(getShadowColor());
        }
    }

    protected void setPressed(boolean f) {
        if (pressed == f) {
            return;
        }
        this.pressed = f;
        if (pressed) {
            commandMap.runCommands(ButtonAction.Down);
            runEffect(EFFECT_PRESS);
        } else {
            commandMap.runCommands(ButtonAction.Up);
            runEffect(EFFECT_RELEASE);
        }
    }

    protected void runClick() {
        if (!isEnabled()) return;
        commandMap.runCommands(ButtonAction.Click);
        runEffect(EFFECT_CLICK);
    }

    @Override
    public String toString() {
        return getClass().getName() + "[text=" + getText() + ", color=" + getColor() + ", elementId="
                + getElementId() + "]";
    }

    @Override
    public void focusGained(Spatial target) {
        log.fine("Button focus gained: " + Button.this);
        if (!isEnabled()) {
            return;
        }
        showFocus(true);
        commandMap.runCommands(ButtonAction.FocusGained);
        runEffect(EFFECT_FOCUS);

    }

    @Override
    public void focusLost(Spatial target) {
        log.fine("Button focus lost: " + Button.this);
        if (!isFocusHighlightOn()) {
            // No reason to run the 'off' effects if we were never on.
            return;
        }

        // If the button is pressed then unpress it
        if (isPressed()) {
            setPressed(false);
        }

        showFocus(false);
        commandMap.runCommands(ButtonAction.FocusLost);
        runEffect(EFFECT_UNFOCUS);
    }

    @Override
    public void focusAction(Spatial target, boolean v) {
        if(v)return;
        log.fine("Button focus activated: " + Button.this);
        if (!isEnabled()) return;
        runClick();
    }

    @Override
    public void focusScrollUpdate(Spatial target, ScrollDirection dir,  double value) {
     
    }

}
