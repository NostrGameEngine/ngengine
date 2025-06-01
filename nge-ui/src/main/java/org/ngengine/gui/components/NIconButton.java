package org.ngengine.gui.components;

import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector2f;
import com.simsilica.lemur.Button;
import com.simsilica.lemur.HAlignment;
import com.simsilica.lemur.VAlignment;
import com.simsilica.lemur.component.IconComponent;
import com.simsilica.lemur.core.GuiComponent;
import com.simsilica.lemur.style.ElementId;
import com.simsilica.lemur.style.StyleAttribute;

public class NIconButton extends Button {
    public static final String ELEMENT_ID = "iconButton";
    protected float iconSize;

    public NIconButton(String iconPath) {
        this(iconPath, ELEMENT_ID);
        
        

    }

    public NIconButton(String iconPath, String elementId) {
        super("", new ElementId(elementId));
        // super("", new ElementId(Button.ELEMENT_ID).child("iconButton"));
        int iconSize = (int) (this.iconSize > 0? this.iconSize:getFontSize() * 1.5f);
        if(iconSize<2) iconSize = 2;
        IconComponent icon = new NSVGIcon(iconPath, iconSize, iconSize);
        icon.setColor(getColor());
        if(getIcon()==null)setIcon(icon);
        // setInsets(new Insets3f(margin, margin, margin, margin));
        setTextHAlignment(HAlignment.Left);
        setTextVAlignment(VAlignment.Center);

    }
    @StyleAttribute("color")
    @Override
    public void setColor(ColorRGBA color) {
        super.setColor(color);
        if (getIcon() instanceof IconComponent icon) {
            icon.setColor(color);
        }
    }
    
    @StyleAttribute("svgIcon")
    public void setSVGIcon(String iconPath) {
        int iconSize = (int) (this.iconSize > 0 ? this.iconSize : getFontSize() * 1.5f);
        if (iconSize < 2) iconSize = 2;
        IconComponent icon = new NSVGIcon(iconPath, iconSize, iconSize);
        super.setIcon(icon);      
    }

    @StyleAttribute("iconSize")
    public void setIconSize(float iconSize) {
        if (iconSize < 2) iconSize = 2;
        this.iconSize = iconSize;
        GuiComponent icon =  getIcon();
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
