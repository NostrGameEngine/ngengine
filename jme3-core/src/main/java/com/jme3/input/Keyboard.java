package com.jme3.input;

import com.jme3.asset.AssetManager;
import com.jme3.input.icons.InputButtonIcons;
import com.jme3.texture.Texture;

public class Keyboard implements InputDevice {
    @Override
    public void rumble(float amount) {
        // Keyboards do not support rumble
    }

    @Override
    public String getDeviceName() {
        return "Keyboard";
    }

    @Override
    public int getId() {
        return -1;
    }

    public String getButtonLabel(int keyId) {
        return InputButtonIcons.getKeyLabel(keyId);
    }

    public String getButtonIconPath(int keyId) {
        return InputButtonIcons.getKeyIconPath(keyId);
    }

    public Texture getButtonIcon(AssetManager assetManager, int keyId) {
        return InputButtonIcons.getKeyIcon(assetManager, keyId);
    }
    
}
