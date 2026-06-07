package com.jme3.input;

import com.jme3.asset.AssetManager;
import com.jme3.input.icons.InputButtonIcons;
import com.jme3.texture.Texture;

public class Mouse implements InputDevice {
    @Override
    public void rumble(float amount) {
        // Mice do not support rumble
    }

    @Override
    public String getDeviceName() {
        return "Mouse";
    }

    @Override
    public int getId() {
        return -2;
    }

    public String getButtonLabel(int buttonId) {
        return InputButtonIcons.getMouseButtonLabel(buttonId);
    }

    public String getButtonIconPath(int buttonId) {
        return InputButtonIcons.getMouseButtonIconPath(buttonId);
    }

    public Texture getButtonIcon(AssetManager assetManager, int buttonId) {
        return InputButtonIcons.getMouseButtonIcon(assetManager, buttonId);
    }
    
}
