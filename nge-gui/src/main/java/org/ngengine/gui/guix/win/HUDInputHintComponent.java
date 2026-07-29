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

package org.ngengine.gui.guix.win;

import com.jme3.input.InputDevice;
import com.jme3.input.InputManager;
import com.jme3.input.Joystick;
import com.jme3.input.JoystickButton;
import com.jme3.input.Keyboard;
import com.jme3.input.Mouse;
import com.jme3.input.MouseInput;
import com.jme3.input.event.InputEvent;
import com.jme3.input.event.JoyButtonEvent;
import com.jme3.input.event.KeyInputEvent;
import com.jme3.input.event.MouseButtonEvent;
import com.jme3.input.event.MouseMotionEvent;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.ngengine.components.AbstractComponent;
import org.ngengine.components.ComponentManager;
import org.ngengine.components.fragments.InputHandlerFragment;
import org.ngengine.components.fragments.LogicFragment;
import org.ngengine.components.jme3.AppComponentInitializer.InputActions;
import org.ngengine.gui.Container;
import org.ngengine.gui.FillMode;
import org.ngengine.gui.HAlignment;
import org.ngengine.gui.Insets3f;
import org.ngengine.gui.NGEGui;
import org.ngengine.gui.NGEStyle;
import org.ngengine.gui.VAlignment;
import org.ngengine.gui.component.BorderLayout;
import org.ngengine.gui.component.QuadBackgroundComponent;
import org.ngengine.gui.guix.NChip;
import org.ngengine.gui.guix.NLabel;
import org.ngengine.gui.guix.containers.NPanel;
import org.ngengine.gui.guix.containers.NRow;

public class HUDInputHintComponent extends AbstractComponent implements LogicFragment, InputHandlerFragment {
    private final LinkedHashMap<InputHintKey, String> hints = new LinkedHashMap<>();
    private NHud attachedHud;
    private NPanel inputHintRoot;
    private boolean inputHintAttached;
    private String renderedInputHintSignature = "";
    private InputDevice activeDevice;
    private Integer activeInput;
    private boolean inputHintsDirty = true;

    public void setInputHint(InputDevice device, int input, String hint) {
        InputHintKey key = InputHintKey.of(device, input);
        if (key == null) {
            return;
        }
        if (hint == null || hint.isBlank()) {
            if (hints.remove(key) != null) {
                renderedInputHintSignature = "";
                inputHintsDirty = true;
            }
            if (key.matches(activeDevice, activeInput)) {
                activeInput = null;
                inputHintsDirty = true;
            }
            updateHud();
            return;
        }
        String trimmed = hint.trim();
        if (!trimmed.equals(hints.get(key))) {
            hints.put(key, trimmed);
            renderedInputHintSignature = "";
            inputHintsDirty = true;
            updateHud();
        }
    }

    public void clearInputHints() {
        if (hints.isEmpty()) {
            hideInputHints();
            return;
        }
        hints.clear();
        activeDevice = null;
        activeInput = null;
        inputHintsDirty = true;
        hideInputHints();
    }

    public void showInputHints(InputDevice device, int input) {
        InputHintKey key = InputHintKey.of(device, input);
        if (key == null || !hints.containsKey(key)) {
            return;
        }
        activeDevice = device;
        activeInput = input;
        inputHintsDirty = true;
        updateHud();
    }

    public void showInputHintsForDevice(InputDevice device) {
        if (device == null) {
            return;
        }
        activeDevice = device;
        activeInput = null;
        inputHintsDirty = true;
        updateHud();
    }

    public void hideInputHints() {
        if (inputHintRoot != null && inputHintAttached) {
            inputHintRoot.removeFromParent();
            inputHintRoot.clearChildren();
        }
        inputHintAttached = false;
        attachedHud = null;
        renderedInputHintSignature = "";
        activeDevice = null;
        activeInput = null;
        inputHintsDirty = false;
    }

    @Override
    protected void onEnable(ComponentManager mng, boolean firstTime) {
    }

    @Override
    protected void onDisable(ComponentManager mng) {
        hideInputHints();
        hints.clear();
        activeDevice = null;
        activeInput = null;
        inputHintsDirty = true;
    }

    @Override
    public void updateAppLogic(ComponentManager mng, float tpf) {
        NHud hud = currentHud();
        if (!inputHintsDirty && inputHintAttached && hud == attachedHud) {
            return;
        }
        updateHud(hud);
    }

    @Override
    public void onJoyButtonEvent(ComponentManager mng, JoyButtonEvent evt) {
        showInputHints(evt.getDevice(), evt.getButtonIndex());
    }

    @Override
    public void onMouseMotionEvent(ComponentManager mng, MouseMotionEvent evt) {
        if (evt.getDeltaWheel() != 0) {
            showInputHints(evt.getDevice(), MouseInput.BUTTON_MIDDLE);
        }
    }

    @Override
    public void onMouseButtonEvent(ComponentManager mng, MouseButtonEvent evt) {
        showInputHints(evt.getDevice(), evt.getButtonIndex());
    }

    @Override
    public void onKeyEvent(ComponentManager mng, KeyInputEvent evt) {
        showInputHints(evt.getDevice(), evt.getKeyCode());
    }

    @Override
    public void onInputAction(ComponentManager mng, String action, boolean toggled, float value, InputEvent<?> event,
            float tpf) {
    }

    @Override
    public void onInputDeviceConnected(ComponentManager mng, InputManager inputManager, InputActions inputActions,
            InputDevice device) {
    }

    @Override
    public void onInputDeviceDisconnected(ComponentManager mng, InputManager inputManager, InputActions inputActions,
            InputDevice device) {
        if (device == activeDevice) {
            hideInputHints();
        }
    }

    @Override
    public boolean controlsOnScreenJoystick(ComponentManager mng, Joystick[] joysticks) {
        return false;
    }

    private void updateHud() {
        updateHud(currentHud());
    }

    private void updateHud(NHud hud) {
        if (hud == null) {
            return;
        }
        if (activeDevice == null) {
            inputHintsDirty = false;
            return;
        }
        if (activeInput != null) {
            InputHintKey activeKey = InputHintKey.of(activeDevice, activeInput);
            if (activeKey == null || !hints.containsKey(activeKey)) {
                inputHintsDirty = false;
                return;
            }
        }

        LinkedHashMap<String, ArrayList<InputHintKey>> grouped = new LinkedHashMap<>();
        String owner = InputHintKey.ownerFor(activeDevice);
        for (Map.Entry<InputHintKey, String> entry : hints.entrySet()) {
            if (!entry.getKey().owner.equals(owner)) {
                continue;
            }
            grouped.computeIfAbsent(entry.getValue(), k -> new ArrayList<>()).add(entry.getKey());
        }
        if (grouped.isEmpty()) {
            hideInputHints();
            return;
        }

        ArrayList<InputHintView> views = new ArrayList<>(grouped.size());
        for (Map.Entry<String, ArrayList<InputHintKey>> entry : grouped.entrySet()) {
            views.add(new InputHintView(entry.getValue(), entry.getKey()));
        }
        renderInputHints(hud, views);
    }

    private NHud currentHud() {
        NWindowManagerComponent wm = getInstanceOf(NWindowManagerComponent.class);
        return wm != null ? wm.getWindow(NHud.class) : null;
    }

    private void ensureInputHintRoot() {
        if (inputHintRoot != null) {
            return;
        }
        inputHintRoot = new NPanel();
        inputHintRoot.setBackground(null);
        inputHintRoot.setInsets(new Insets3f(0f, 0f, 0f, 0f));
        inputHintRoot.setPreferredSize(NGEStyle.px(420f), NGEStyle.px(34f));
    }

    private void renderInputHints(NHud hud, List<InputHintView> hints) {
        ensureInputHintRoot();
        if (hints == null || hints.isEmpty()) {
            hideInputHints();
            return;
        }
        String signature = inputHintSignature(hints);
        if (signature.equals(renderedInputHintSignature) && hud == attachedHud) {
            inputHintsDirty = false;
            return;
        }
        if (attachedHud != null && attachedHud != hud) {
            hideInputHints();
            ensureInputHintRoot();
        }

        inputHintRoot.clearChildren();
        float edgeInset = NGEStyle.px(14f);
        inputHintRoot.setInsets(new Insets3f(0f, edgeInset, edgeInset, 0f));
        renderedInputHintSignature = signature;
        NRow row = inputHintRoot.addRow(BorderLayout.Position.South);
        row.setFillMode(FillMode.None, FillMode.None);
        float totalWidth = 0f;
        for (InputHintView hint : hints) {
            if (totalWidth > 0f) {
                NPanel gap = spacer(NGEStyle.px(8f), NGEStyle.px(1f));
                row.addChild(gap);
                totalWidth += NGEStyle.px(8f);
            }
            NChip chip = createInputHintChip(hint);
            row.addChild(chip);
            totalWidth += chip.getPreferredSize().x;
        }
        inputHintRoot.setPreferredSize(Math.max(NGEStyle.px(1f), totalWidth + edgeInset), NGEStyle.px(118f));
        if (!inputHintAttached) {
            hud.getBottomLeft().setFillMode(FillMode.None, FillMode.None);
            hud.getBottomLeft().addChild(inputHintRoot);
            inputHintAttached = true;
            attachedHud = hud;
        }
        inputHintRoot.setLocalTranslation(0f, 0f, 4f);
        inputHintsDirty = false;
    }

    private NChip createInputHintChip(InputHintView hint) {
        float chipHeight = NGEStyle.px(40f);
        float paddingX = NChip.defaultPaddingX();
        float paddingY = NChip.defaultPaddingY();
        float keyHeight = NGEStyle.px(25f);
        Container keys = createInputHintKeys(hint.keys, inputHintKeysWidth(hint.keys, keyHeight), chipHeight - paddingY * 2f, keyHeight);
        NChip chip = new NChip(hint.hint, keys);
        chip.setMetrics(chipHeight, paddingX, paddingY, NGEStyle.px(8f));
        chip.setMinTextWidth(NGEStyle.px(42f));
        chip.setTextHAlignment(HAlignment.Left);
        return chip;
    }

    private Container createInputHintKeys(List<InputHintKey> keys, float width, float height, float iconSize) {
        Container box = new Container(new BorderLayout());
        box.setBackground(null);
        box.setInsets(new Insets3f(0f, 0f, 0f, 0f));
        box.setPreferredSize(width, height);
        NRow row = new NRow();
        row.setFillMode(FillMode.None, FillMode.None);
        row.setPreferredSize(Math.max(NGEStyle.px(1f), width), height);
        for (int i = 0; i < keys.size(); i++) {
            if (i > 0) {
                row.addChild(spacer(NGEStyle.px(3f), NGEStyle.px(1f)));
            }
            row.addChild(createInputHintKey(keys.get(i), height, iconSize));
        }
        box.addChild(row, BorderLayout.Position.Center);
        return box;
    }

    private NPanel createInputHintKey(InputHintKey key, float height, float iconSize) {
        float width = inputHintKeyWidth(key, iconSize);
        NPanel slot = new NPanel();
        slot.setBackground(null);
        slot.setInsets(new Insets3f(0f, 0f, 0f, 0f));
        slot.setPreferredSize(width, height);

        Container centered = new Container(new BorderLayout());
        centered.setBackground(null);
        centered.setInsets(new Insets3f(0f, 0f, 0f, 0f));
        centered.setPreferredSize(width, height);
        float keySize = Math.min(iconSize, width);

        NPanel panel = key.iconPath == null ? new NPanel(NChip.ICON_ID) : new NPanel();
        panel.setInsets(new Insets3f(0f, 0f, 0f, 0f));
        panel.setPreferredSize(keySize, keySize);
        if (key.iconPath != null) {
            panel.setBackground(new QuadBackgroundComponent(NGEGui.loadTexture(key.iconPath, false, false)));
        } else {
            NLabel label = new NLabel(key.fallbackLabel, NChip.ICON_ID);
            label.setPreferredSize(keySize, keySize);
            label.setTextHAlignment(HAlignment.Center);
            label.setTextVAlignment(VAlignment.Center);
            panel.getLayout().addChild(label, BorderLayout.Position.Center);
        }
        centered.addChild(panel, BorderLayout.Position.Center);
        slot.getLayout().addChild(centered, BorderLayout.Position.Center);
        return slot;
    }

    private float inputHintKeysWidth(List<InputHintKey> keys, float height) {
        float width = 0f;
        for (int i = 0; i < keys.size(); i++) {
            width += inputHintKeyWidth(keys.get(i), height);
            if (i > 0) {
                width += NGEStyle.px(3f);
            }
        }
        return width;
    }

    private float inputHintKeyWidth(InputHintKey key, float height) {
        if (key.iconPath != null) {
            return height;
        }
        return Math.max(NGEStyle.px(34f), NGEStyle.px(16f + key.fallbackLabel.length() * 7f));
    }

    private NPanel spacer(float width, float height) {
        NPanel spacer = new NPanel();
        spacer.setBackground(null);
        spacer.setInsets(new Insets3f(0f, 0f, 0f, 0f));
        spacer.setPreferredSize(width, height);
        return spacer;
    }

    private String inputHintSignature(List<InputHintView> hints) {
        StringBuilder out = new StringBuilder();
        for (InputHintView hint : hints) {
            if (out.length() > 0) {
                out.append('|');
            }
            for (InputHintKey key : hint.keys) {
                out.append(key.owner).append(':').append(key.input).append(':')
                    .append(key.iconPath).append(':').append(key.fallbackLabel).append('+');
            }
            out.append('=').append(hint.hint);
        }
        return out.toString();
    }

    private static final class InputHintView {
        private final List<InputHintKey> keys;
        private final String hint;

        private InputHintView(List<InputHintKey> keys, String hint) {
            this.keys = keys;
            this.hint = hint;
        }
    }

    private static final class InputHintKey {
        private final String owner;
        private final int input;
        private final String iconPath;
        private final String fallbackLabel;

        private InputHintKey(String owner, int input, String iconPath, String fallbackLabel) {
            this.owner = owner;
            this.input = input;
            this.iconPath = iconPath == null || iconPath.isBlank() ? null : iconPath;
            this.fallbackLabel = fallbackLabel == null || fallbackLabel.isBlank() ? "?" : fallbackLabel;
        }

        private static InputHintKey of(InputDevice device, int input) {
            String owner = ownerFor(device);
            if (owner == null) {
                return null;
            }
            if (device instanceof Keyboard) {
                Keyboard keyboard = (Keyboard) device;
                return new InputHintKey(owner, input, keyboard.getButtonIconPath(input), keyboard.getButtonLabel(input));
            }
            if (device instanceof Mouse) {
                Mouse mouse = (Mouse) device;
                return new InputHintKey(owner, input, mouse.getButtonIconPath(input), mouse.getButtonLabel(input));
            }
            if (device instanceof Joystick) {
                Joystick joystick = (Joystick) device;
                for (JoystickButton button : joystick.getButtons()) {
                    if (button.getButtonId() == input) {
                        return new InputHintKey(owner, input, button.getIconPath(), button.getName());
                    }
                }
                return new InputHintKey(owner, input, null, String.valueOf(input));
            }
            return null;
        }

        private static String ownerFor(InputDevice device) {
            if (device instanceof Keyboard || device instanceof Mouse) {
                return "desktop";
            }
            if (device instanceof Joystick) {
                return "joystick:" + device.getId();
            }
            return null;
        }

        private boolean matches(InputDevice device, Integer input) {
            return input != null && this.input == input && owner.equals(ownerFor(device));
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (!(obj instanceof InputHintKey)) {
                return false;
            }
            InputHintKey other = (InputHintKey) obj;
            return input == other.input && owner.equals(other.owner);
        }

        @Override
        public int hashCode() {
            return 31 * owner.hashCode() + input;
        }
    }
}
