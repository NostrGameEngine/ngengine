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

package org.ngengine.gui.guix;

import org.ngengine.gui.Button;
import org.ngengine.gui.HAlignment;
import org.ngengine.gui.VAlignment;

import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector2f;
import org.ngengine.gui.component.IconComponent;
import org.ngengine.gui.core.GuiComponent;
import org.ngengine.gui.style.ElementId;
import org.ngengine.gui.style.StyleAttribute;

public class NIconButton extends Button {

    public static final String ELEMENT_ID = "iconButton";
    protected float iconSize;

    /**
     * @deprecated   use NIconButton(NSVGIcon icon) instead
     * @param iconPath
     */
    @Deprecated
    public NIconButton(String iconPath) {
        this(iconPath, ELEMENT_ID);
    }

    /**
     * @deprecated   use NIconButton(NSVGIcon icon, String elementId) instead
     */
    @Deprecated
    public NIconButton(String iconPath, String elementId) {
        super("", new ElementId(elementId));
        applyStyles(NIconButton.class);

        // super("", new ElementId(Button.ELEMENT_ID).child("iconButton"));
        int iconSize = (int) (this.iconSize > 0 ? this.iconSize : getFontSize() * 1.5f);
        if (iconSize < 2) iconSize = 2;
        IconComponent icon = new NSVGIcon(iconPath, iconSize, iconSize);
        icon.setColor(getColor());
        if (getIcon() == null) setIcon(icon);
        // setInsets(new Insets3f(margin, margin, margin, margin));
        setTextHAlignment(HAlignment.Left);
        setTextVAlignment(VAlignment.Center);
    }

    public NIconButton(NSVGIcon icon) {
        this(icon, ELEMENT_ID);
    }

    public NIconButton(NSVGIcon icon, String elementId) {
        super("", new ElementId(elementId));
        applyStyles(NIconButton.class);

        // super("", new ElementId(Button.ELEMENT_ID).child("iconButton"));
        int iconSize = (int) (this.iconSize > 0 ? this.iconSize : getFontSize() * 1.5f);
        if (iconSize < 2) iconSize = 2;
        icon.setIconSize(new Vector2f(iconSize, iconSize));
        icon.setColor(getColor());
        if (getIcon() == null) setIcon(icon);
        // setInsets(new Insets3f(margin, margin, margin, margin));
        setTextHAlignment(HAlignment.Left);
        setTextVAlignment(VAlignment.Center);
    }

    @StyleAttribute("color")
    @Override
    public void setColor(ColorRGBA color) {
        super.setColor(color);
        if (getIcon() instanceof IconComponent) {
            IconComponent icon = (IconComponent) getIcon();
            icon.setColor(color);
        }
    }

    /**
     * @deprecated causes deadlocks when compiling to teavm, use setSVGIconComponent ("svgIconComponent") instead
     * @param iconPath
     */
    @StyleAttribute("svgIcon")
    @Deprecated
    public void setSVGIcon(String iconPath) {
        int iconSize = (int) (this.iconSize > 0 ? this.iconSize : getFontSize() * 1.5f);
        if (iconSize < 2) iconSize = 2;
        IconComponent icon = new NSVGIcon(iconPath, iconSize, iconSize);
        super.setIcon(icon);
    }

    @StyleAttribute("svgIconComponent")
    public void setSVGIconComponent(NSVGIcon icon) {
        int iconSize = (int) (this.iconSize > 0 ? this.iconSize : getFontSize() * 1.5f);
        if (iconSize < 2) iconSize = 2;
        icon.setIconSize(new Vector2f(iconSize, iconSize));
        super.setIcon(icon);
    }

    @StyleAttribute("iconSize")
    public void setIconSize(float iconSize) {
        if (iconSize < 2) iconSize = 2;
        this.iconSize = iconSize;
        GuiComponent icon = getIcon();
        if (icon != null && icon instanceof IconComponent) {
            IconComponent iconComponent = (IconComponent) icon;
            iconComponent.setIconSize(new Vector2f(iconSize, iconSize));
        }
    }

    @Override
    public NIconButton clone() {
        return (NIconButton) super.clone();
    }
}
