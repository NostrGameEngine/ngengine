package com.jme3.input.controls;

import com.jme3.input.event.InputEvent;

public interface UnifiedInputListener extends InputListener {
 
    public void onUnifiedInput(String name, boolean toggled, float value, InputEvent<?> event, float tpf);
}
