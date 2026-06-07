/*
 * Copyright (c) 2026, Nostr Game Engine
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
package org.ngengine.gui.nav;

import com.jme3.asset.AssetManager;
import com.jme3.input.InputManager;
import com.jme3.input.JoyInput;
import com.jme3.input.Joystick;
import com.jme3.input.JoystickAxis;
import com.jme3.input.JoystickButton;
import com.jme3.input.KeyInput;
import com.jme3.input.Mouse;
import com.jme3.input.MouseInput;
import com.jme3.input.RawInputListener;
import com.jme3.cursors.plugins.JmeCursor;
import com.jme3.input.dummy.DummyKeyInput;
import com.jme3.input.dummy.DummyMouseInput;
import com.jme3.input.event.InputEvent;
import com.jme3.input.event.JoyAxisEvent;
import com.jme3.input.event.JoyButtonEvent;
import com.jme3.input.event.KeyInputEvent;
import com.jme3.input.event.MouseButtonEvent;
import com.jme3.input.event.MouseMotionEvent;
import com.jme3.math.Vector3f;
import com.jme3.renderer.Camera;
import com.jme3.renderer.ViewPort;
import com.jme3.renderer.queue.RenderQueue.Bucket;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.shape.Quad;
import com.jme3.audio.AudioRenderer;
import com.jme3.system.AppSettings;
import com.jme3.system.JmeContext;
import com.jme3.system.JmeSystem;
import com.jme3.system.JmeSystemDelegate;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Function;
import java.net.URL;
import java.nio.ByteBuffer;
import org.junit.jupiter.api.Test;
import org.ngengine.gui.Axis;
import org.ngengine.gui.DefaultRangedValueModel;
import org.ngengine.gui.GuiContext;
import org.ngengine.gui.NGEGui;
import org.ngengine.gui.Slider;
import org.ngengine.gui.core.GuiControl;
import org.ngengine.gui.ime.ImeComposer;
import org.ngengine.gui.ime.ImeCompositionEvent;
import org.ngengine.gui.ime.PhysicalKeyboardImeComposer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class NavigatorLayerTest {

    @Test
    public void autofocusSelectsTopLeftFocusableElementAndActionsIt() {
        Node root = new Node("root");
        Node right = focusable("right", 20f, 40f);
        Node topLeft = focusable("topLeft", 5f, 40f);
        Node lowerLeft = focusable("lowerLeft", 0f, 10f);
        List<String> events = new ArrayList<>();
        addRecorder(topLeft, events);

        root.attachChild(right);
        root.attachChild(topLeft);
        root.attachChild(lowerLeft);

        NavigatorLayer layer = new NavigatorLayer(root, null, null);
        layer.setEnabled(true);
        layer.updateFocus(true);
        layer.action(true);
        layer.action(false);

        assertSame(topLeft, layer.getFocus());
        assertEquals(List.of(
            "gained:topLeft",
            "action:topLeft:true",
            "action:topLeft:false"
        ), events);
    }

    @Test
    public void directionalNavigationSkipsNonFocusableElements() {
        Node root = new Node("root");
        Node start = focusable("start", 0f, 0f);
        Node nonFocusable = new Node("nonFocusable");
        nonFocusable.setLocalTranslation(new Vector3f(20f, 0f, 0f));
        Node target = focusable("target", 40f, 0f);

        root.attachChild(start);
        root.attachChild(nonFocusable);
        root.attachChild(target);

        NavigatorLayer layer = new NavigatorLayer(root, null, null);
        layer.setEnabled(true);
        layer.focus(start);

        assertSame(target, layer.navigate(TraversalDirection.Right));
        assertSame(target, layer.getFocus());
    }

    @Test
    public void pointerOnlyTargetsArePickableButSkippedByNavigation() {
        Node root = new Node("root");
        Node start = focusable("start", 0f, 0f);
        Node pointerOnly = focusable("pointerOnly", 20f, 0f);
        pointerOnly.getControl(GuiControl.class).setFocusable(FocusTarget.FOCUS_POINTER);
        Node target = focusable("target", 40f, 0f);

        root.attachChild(start);
        root.attachChild(pointerOnly);
        root.attachChild(target);

        NavigatorLayer layer = new NavigatorLayer(root, null, null);
        layer.setEnabled(true);
        layer.focus(start);

        assertEquals(true, NGEGui.isFocusable(pointerOnly, FocusTarget.FOCUS_POINTER));
        assertEquals(false, NGEGui.isFocusable(pointerOnly, FocusTarget.FOCUS_NAVIGATION));
        assertSame(target, layer.navigate(TraversalDirection.Right));
    }

    @Test
    public void pointerCanClearFocusWithoutAutofocusFallback() {
        Node root = new Node("root");
        Node target = focusable("target", 0f, 0f);
        root.attachChild(target);

        NavigatorLayer layer = new NavigatorLayer(root, null, null);
        layer.setEnabled(true);
        layer.focus(target);
        layer.clearPointerFocus();
        layer.updateFocus(false);

        assertNull(layer.getFocus());
    }

    @Test
    public void directionalNavigationPrefersAlignedRowCandidateOverCloserLowerCandidate() {
        Node root = new Node("root");
        Node start = focusable("start", 0f, 0f);
        Node right = focusable("right", 40f, 0f);
        Node lowerRight = focusable("lowerRight", 10f, -20f);

        root.attachChild(start);
        root.attachChild(lowerRight);
        root.attachChild(right);

        NavigatorLayer layer = new NavigatorLayer(root, null, null);
        layer.setEnabled(true);
        layer.focus(start);

        assertSame(right, layer.navigate(TraversalDirection.Right));
        assertSame(right, layer.getFocus());
    }

    @Test
    public void verticalNavigationPrefersNearestRowBeforeHorizontalAlignment() {
        Node root = new Node("root");
        Node start = focusable("start", 100f, 0f);
        Node nearRowLeft = focusable("nearRowLeft", 0f, -20f);
        Node nearRowMiddle = focusable("nearRowMiddle", 35f, -20f);
        Node nearRowRight = focusable("nearRowRight", 70f, -20f);
        Node farAligned = focusable("farAligned", 100f, -80f);

        root.attachChild(start);
        root.attachChild(nearRowLeft);
        root.attachChild(nearRowMiddle);
        root.attachChild(nearRowRight);
        root.attachChild(farAligned);

        NavigatorLayer layer = new NavigatorLayer(root, null, null);
        layer.setEnabled(true);
        layer.focus(start);

        assertSame(nearRowRight, layer.navigate(TraversalDirection.Down));
        assertSame(nearRowRight, layer.getFocus());
    }

    @Test
    public void joystickHandlerConfirmsFocusedElementAndBacksOutWithFocusPresent() {
        initializeGui();

        ViewPort vp = new ViewPort("gui", new Camera(800, 600));
        Node guiNode = new Node("GuiNode");
        guiNode.setQueueBucket(Bucket.Gui);
        vp.attachScene(guiNode);
        GuiContext context = NGEGui.register(vp, true);

        Node root = new Node("root");
        Node target = focusable("target", 0f, 0f);
        List<String> events = new ArrayList<>();
        addRecorder(target, events);
        root.attachChild(target);
        guiNode.attachChild(root);
        context.getNavigator().pushLayer(root);

        TestJoystick joystick = new TestJoystick();
        TestJoyInput joyInput = new TestJoyInput(joystick);
        InputManager inputManager = newInputManager(joyInput);
        DefaultNavigatorInputHandler handler = new DefaultNavigatorInputHandler(vp);
        AtomicInteger backActions = new AtomicInteger();
        AtomicInteger primaryActions = new AtomicInteger();
        handler.setBackAction(backActions::incrementAndGet);
        handler.setPrimaryAction(primaryActions::incrementAndGet);
        handler.registerListener(inputManager);
        handler.setInputDevice(inputManager, joystick);

        assertSame(target, context.getNavigator().autofocus());

        joyInput.queue(new JoyButtonEvent(new TestJoystickButton(joystick, 2), true));
        inputManager.update(0.016f);
        joyInput.queue(new JoyButtonEvent(new TestJoystickButton(joystick, 2), false));
        inputManager.update(0.016f);
        joyInput.queue(new JoyButtonEvent(new TestJoystickButton(joystick, 1), true));
        inputManager.update(0.016f);

        assertEquals(List.of(
            "gained:target",
            "action:target:true",
            "action:target:false"
        ), events);
        assertEquals(0, primaryActions.get());
        assertEquals(1, backActions.get());
    }

    @Test
    public void joystickBackClosesOpenImeBeforeRunningBackActionWithoutClearingFocus() {
        initializeGui();

        ViewPort vp = new ViewPort("gui-ime-back", new Camera(800, 600));
        Node guiNode = new Node("GuiNode");
        guiNode.setQueueBucket(Bucket.Gui);
        vp.attachScene(guiNode);
        GuiContext context = NGEGui.register(vp, true);
        TestImeComposer ime = new TestImeComposer();
        context.setImeComposer(ime);
        ime.open(context, ev -> {}, new ImeCompositionEvent("typing"), c -> c, s -> 0f);
        Node root = new Node("root");
        Node target = focusable("target", 0f, 0f);
        root.attachChild(target);
        guiNode.attachChild(root);
        context.getNavigator().pushLayer(root);
        context.getNavigator().focus(target);

        TestJoystick joystick = new TestJoystick();
        TestJoyInput joyInput = new TestJoyInput(joystick);
        InputManager inputManager = newInputManager(joyInput);
        DefaultNavigatorInputHandler handler = new DefaultNavigatorInputHandler(vp);
        AtomicInteger backActions = new AtomicInteger();
        handler.setBackAction(backActions::incrementAndGet);
        handler.registerListener(inputManager);
        handler.setInputDevice(inputManager, joystick);

        joyInput.queue(new JoyButtonEvent(new TestJoystickButton(joystick, 1), true));
        inputManager.update(0.016f);

        assertEquals(1, ime.closeCount.get());
        assertEquals(false, ime.isOpen());
        assertSame(target, context.getNavigator().getFocus());
        assertEquals(0, backActions.get());

        joyInput.queue(new JoyButtonEvent(new TestJoystickButton(joystick, 1), false));
        inputManager.update(0.016f);
        joyInput.queue(new JoyButtonEvent(new TestJoystickButton(joystick, 1), true));
        inputManager.update(0.016f);

        assertEquals(1, backActions.get());
    }

    @Test
    public void openImeMovesCaretWithKeyboardAndJoystickHorizontalInput() {
        initializeGui();

        ViewPort vp = new ViewPort("gui-ime-caret", new Camera(800, 600));
        NGEGui.register(vp, true);
        TestJoystick joystick = new TestJoystick();
        InputManager inputManager = newInputManager(new TestJoyInput(joystick));
        PhysicalKeyboardImeComposer composer = new PhysicalKeyboardImeComposer(inputManager);
        ImeCompositionEvent event = new ImeCompositionEvent("abcd");
        event.setCursor(2);

        composer.open(NGEGui.get(vp), ev -> {}, event, c -> c, s -> (float) s.length());

        composer.onKeyEvent(new KeyInputEvent(KeyInput.KEY_LEFT, '\0', true, false));
        assertEquals(1, event.getCursorStart());

        composer.onJoyButtonEvent(new JoyButtonEvent(
                new TestJoystickButton(joystick, Integer.parseInt(JoystickButton.BUTTON_XBOX_DPAD_RIGHT)), true));
        assertEquals(2, event.getCursorStart());

        JoystickAxis axis = joystick.getAxis(JoystickAxis.AXIS_XBOX_RIGHT_THUMB_STICK_X);
        composer.onJoyAxisEvent(new JoyAxisEvent(axis, 0.8f));
        assertEquals(3, event.getCursorStart());

        composer.onJoyAxisEvent(new JoyAxisEvent(axis, 0f));
        composer.onJoyAxisEvent(new JoyAxisEvent(axis, -0.8f));
        assertEquals(2, event.getCursorStart());
    }

    @Test
    public void openImeInsertsPrintableUnknownKeyCharacters() {
        initializeGui();

        ViewPort vp = new ViewPort("gui-ime-printable", new Camera(800, 600));
        NGEGui.register(vp, true);
        InputManager inputManager = newInputManager((JoyInput) null);
        PhysicalKeyboardImeComposer composer = new PhysicalKeyboardImeComposer(inputManager);
        ImeCompositionEvent event = new ImeCompositionEvent("");

        composer.open(NGEGui.get(vp), ev -> {}, event, c -> c, s -> (float) s.length());
        composer.onKeyEvent(new KeyInputEvent(KeyInput.KEY_UNKNOWN, '!', true, false));

        assertEquals("!", event.getText());
        assertEquals(1, event.getCursorStart());
    }

    @Test
    public void joystickAxisNavigationRequiresPressThresholdBeforeMovingFocus() {
        initializeGui();

        ViewPort vp = new ViewPort("gui-axis", new Camera(800, 600));
        Node guiNode = new Node("GuiNode");
        guiNode.setQueueBucket(Bucket.Gui);
        vp.attachScene(guiNode);
        GuiContext context = NGEGui.register(vp, true);
        context.getNavigator().setSimulateCursor(false);

        Node root = new Node("root");
        Node start = focusable("start", 0f, 0f);
        Node down = focusable("down", 0f, -20f);
        root.attachChild(start);
        root.attachChild(down);
        guiNode.attachChild(root);
        context.getNavigator().pushLayer(root);

        TestJoystick joystick = new TestJoystick();
        TestJoyInput joyInput = new TestJoyInput(joystick);
        InputManager inputManager = newInputManager(joyInput);
        DefaultNavigatorInputHandler handler = new DefaultNavigatorInputHandler(vp);
        handler.registerListener(inputManager);
        handler.setInputDevice(inputManager, joystick);

        context.getNavigator().focus(start);

        joyInput.queue(new JoyAxisEvent(joystick.getYAxis(), 0.20f));
        inputManager.update(0.016f);
        assertSame(start, context.getNavigator().getFocus());

        joyInput.queue(new JoyAxisEvent(joystick.getYAxis(), 0.60f));
        inputManager.update(0.016f);
        assertSame(down, context.getNavigator().getFocus());
    }

    @Test
    public void firstJoystickPressAfterMouseDeviceSwitchOnlyAutofocuses() {
        initializeGui();

        ViewPort vp = new ViewPort("gui-device-switch", new Camera(800, 600));
        Node guiNode = new Node("GuiNode");
        guiNode.setQueueBucket(Bucket.Gui);
        vp.attachScene(guiNode);
        GuiContext context = NGEGui.register(vp, true);
        context.getNavigator().setSimulateCursor(false);

        Node root = new Node("root");
        Node start = focusable("start", 0f, 0f);
        Node down = focusable("down", 0f, -20f);
        root.attachChild(start);
        root.attachChild(down);
        guiNode.attachChild(root);
        context.getNavigator().pushLayer(root);
        context.getNavigator().clearPointerFocus();

        TestJoystick joystick = new TestJoystick();
        TestJoyInput joyInput = new TestJoyInput(joystick);
        InputManager inputManager = newInputManager(joyInput);
        DefaultNavigatorInputHandler handler = new DefaultNavigatorInputHandler(vp);
        handler.registerListener(inputManager);
        handler.setInputDevice(inputManager, new Mouse());
        handler.setInputDevice(inputManager, joystick);

        joyInput.queue(new JoyButtonEvent(new TestJoystickButton(joystick, 13), true));
        inputManager.update(0.016f);
        assertSame(start, context.getNavigator().getFocus());

        joyInput.queue(new JoyButtonEvent(new TestJoystickButton(joystick, 13), false));
        inputManager.update(0.016f);
        joyInput.queue(new JoyButtonEvent(new TestJoystickButton(joystick, 13), true));
        inputManager.update(0.016f);
        assertSame(down, context.getNavigator().getFocus());
    }

    @Test
    public void firstJoystickPressAfterMouseDeviceSwitchActsWhenFocusAlreadyExists() {
        initializeGui();

        ViewPort vp = new ViewPort("gui-device-switch-focused", new Camera(800, 600));
        Node guiNode = new Node("GuiNode");
        guiNode.setQueueBucket(Bucket.Gui);
        vp.attachScene(guiNode);
        GuiContext context = NGEGui.register(vp, true);

        Node root = new Node("root");
        Node start = focusable("start", 0f, 0f);
        Node down = focusable("down", 0f, -20f);
        root.attachChild(start);
        root.attachChild(down);
        guiNode.attachChild(root);
        context.getNavigator().pushLayer(root);
        context.getNavigator().focus(start);

        TestJoystick joystick = new TestJoystick();
        TestJoyInput joyInput = new TestJoyInput(joystick);
        InputManager inputManager = newInputManager(joyInput);
        DefaultNavigatorInputHandler handler = new DefaultNavigatorInputHandler(vp);
        handler.registerListener(inputManager);
        handler.setInputDevice(inputManager, new Mouse());
        handler.setInputDevice(inputManager, joystick);

        joyInput.queue(new JoyButtonEvent(new TestJoystickButton(joystick, 13), true));
        inputManager.update(0.016f);

        assertSame(down, context.getNavigator().getFocus());
    }

    @Test
    public void focusedSliderConsumesDirectionalNavigationAsValueChange() {
        initializeGui();

        ViewPort vp = new ViewPort("gui-slider-direction", new Camera(800, 600));
        Node guiNode = new Node("GuiNode");
        guiNode.setQueueBucket(Bucket.Gui);
        vp.attachScene(guiNode);
        GuiContext context = NGEGui.register(vp, true);

        Node root = new Node("root");
        DefaultRangedValueModel model = new DefaultRangedValueModel(0, 100, 50);
        Slider slider = new Slider(model, Axis.X);
        slider.setDelta(10);
        root.attachChild(slider);
        guiNode.attachChild(root);
        context.getNavigator().pushLayer(root);
        context.getNavigator().focus(slider);

        TestJoystick joystick = new TestJoystick();
        TestJoyInput joyInput = new TestJoyInput(joystick);
        InputManager inputManager = newInputManager(joyInput);
        DefaultNavigatorInputHandler handler = new DefaultNavigatorInputHandler(vp);
        handler.registerListener(inputManager);
        handler.setInputDevice(inputManager, joystick);

        joyInput.queue(new JoyButtonEvent(new TestJoystickButton(
                joystick, Integer.parseInt(JoystickButton.BUTTON_XBOX_DPAD_RIGHT)), true));
        inputManager.update(0.016f);

        assertEquals(60, model.getValue());
        assertSame(slider, context.getNavigator().getFocus());
    }

    @Test
    public void joystickAxisNavigationIgnoresVerticalNoiseWhenHorizontalAxisDominates() {
        initializeGui();

        ViewPort vp = new ViewPort("gui-axis-dominance", new Camera(800, 600));
        Node guiNode = new Node("GuiNode");
        guiNode.setQueueBucket(Bucket.Gui);
        vp.attachScene(guiNode);
        GuiContext context = NGEGui.register(vp, true);
        context.getNavigator().setSimulateCursor(false);

        Node root = new Node("root");
        Node start = focusable("start", 0f, 0f);
        Node right = focusable("right", 40f, 0f);
        Node down = focusable("down", 0f, -40f);
        root.attachChild(start);
        root.attachChild(right);
        root.attachChild(down);
        guiNode.attachChild(root);
        context.getNavigator().pushLayer(root);

        TestJoystick joystick = new TestJoystick();
        TestJoyInput joyInput = new TestJoyInput(joystick);
        InputManager inputManager = newInputManager(joyInput);
        DefaultNavigatorInputHandler handler = new DefaultNavigatorInputHandler(vp);
        handler.registerListener(inputManager);
        handler.setInputDevice(inputManager, joystick);

        context.getNavigator().focus(start);

        joyInput.queue(new JoyAxisEvent(joystick.getXAxis(), 0.80f));
        inputManager.update(0.016f);
        assertSame(right, context.getNavigator().getFocus());

        joyInput.queue(new JoyAxisEvent(joystick.getYAxis(), 0.55f));
        inputManager.update(0.016f);
        assertSame(right, context.getNavigator().getFocus());
    }

    @Test
    public void joystickShoulderButtonsScrollFocusedElementHorizontally() {
        initializeGui();

        ViewPort vp = new ViewPort("gui-shoulders", new Camera(800, 600));
        Node guiNode = new Node("GuiNode");
        guiNode.setQueueBucket(Bucket.Gui);
        vp.attachScene(guiNode);
        GuiContext context = NGEGui.register(vp, true);

        Node root = new Node("root");
        Node target = focusable("target", 0f, 0f);
        List<String> events = new ArrayList<>();
        addRecorder(target, events);
        root.attachChild(target);
        guiNode.attachChild(root);
        context.getNavigator().pushLayer(root);

        TestJoystick joystick = new TestJoystick();
        TestJoyInput joyInput = new TestJoyInput(joystick);
        InputManager inputManager = newInputManager(joyInput);
        DefaultNavigatorInputHandler handler = new DefaultNavigatorInputHandler(vp);
        handler.registerListener(inputManager);
        handler.setInputDevice(inputManager, joystick);

        context.getNavigator().focus(target);

        joyInput.queue(new JoyButtonEvent(new TestJoystickButton(joystick, 5), true));
        inputManager.update(0.016f);
        joyInput.queue(new JoyButtonEvent(new TestJoystickButton(joystick, 4), true));
        inputManager.update(0.016f);

        assertEquals(List.of(
            "gained:target",
            "scroll:target:Right:1.0",
            "scroll:target:Left:1.0"
        ), events);
    }

    @Test
    public void simulatedCursorUsesLeftStickAndRightTriggerForPointerClick() {
        initializeGui();

        ViewPort vp = new ViewPort("gui-simulated-cursor", new Camera(800, 600));
        Node guiNode = new Node("GuiNode");
        guiNode.setQueueBucket(Bucket.Gui);
        vp.attachScene(guiNode);
        GuiContext context = NGEGui.register(vp, true);
        context.getNavigator().setSimulateCursor(true);

        Node root = new Node("root");
        Node target = pickable("target", 405f, 275f);
        List<String> events = new ArrayList<>();
        addRecorder(target, events);
        root.attachChild(focusable("start", 0f, 0f));
        root.attachChild(focusable("right", 40f, 0f));
        root.attachChild(target);
        guiNode.attachChild(root);
        guiNode.updateGeometricState();
        context.getNavigator().pushLayer(root);
        context.getNavigator().focus(root.getChild("start"));

        TestJoystick joystick = new TestJoystick();
        TestJoyInput joyInput = new TestJoyInput(joystick);
        InputManager inputManager = newInputManager(joyInput);
        DefaultNavigatorInputHandler handler = new DefaultNavigatorInputHandler(vp);
        handler.registerListener(inputManager);
        handler.setInputDevice(inputManager, joystick);

        joyInput.queue(new JoyAxisEvent(joystick.getXAxis(), 0.80f));
        inputManager.update(0.016f);
        joyInput.queue(new JoyButtonEvent(new TestJoystickButton(
                joystick, Integer.parseInt(JoystickButton.BUTTON_XBOX_RT)), true));
        inputManager.update(0.016f);
        joyInput.queue(new JoyButtonEvent(new TestJoystickButton(
                joystick, Integer.parseInt(JoystickButton.BUTTON_XBOX_RT)), false));
        inputManager.update(0.016f);

        assertSame(target, context.getNavigator().getFocus());
        assertEquals(List.of(
            "action:target:true",
            "action:target:false"
        ), events.stream().filter(e -> e.startsWith("action:")).toList());
    }

    @Test
    public void simulatedCursorContinuesMovingWhileLeftStickIsHeld() {
        initializeGui();

        ViewPort vp = new ViewPort("gui-simulated-cursor-held", new Camera(800, 600));
        Node guiNode = new Node("GuiNode");
        guiNode.setQueueBucket(Bucket.Gui);
        vp.attachScene(guiNode);
        GuiContext context = NGEGui.register(vp, true);
        context.getNavigator().setSimulateCursor(true);

        TestJoystick joystick = new TestJoystick();
        TestJoyInput joyInput = new TestJoyInput(joystick);
        InputManager inputManager = newInputManager(joyInput);
        DefaultNavigatorInputHandler handler = new DefaultNavigatorInputHandler(vp);
        handler.registerListener(inputManager);
        handler.setInputDevice(inputManager, joystick);

        joyInput.queue(new JoyAxisEvent(joystick.getXAxis(), 0.80f));
        inputManager.update(0.016f);
        context.getNavigator().update(0.016f);
        float firstX = context.getNavigator().getCursor().getLocalTranslation().x;

        handler.update(0.016f);
        context.getNavigator().update(0.016f);
        float secondX = context.getNavigator().getCursor().getLocalTranslation().x;

        handler.update(0.016f);
        context.getNavigator().update(0.016f);
        float thirdX = context.getNavigator().getCursor().getLocalTranslation().x;

        assertTrue(secondX > firstX);
        assertTrue(thirdX > secondX);
    }

    @Test
    public void simulatedCursorSpeedIsConfigurable() {
        initializeGui();

        ViewPort vp = new ViewPort("gui-simulated-cursor-speed", new Camera(800, 600));
        Node guiNode = new Node("GuiNode");
        guiNode.setQueueBucket(Bucket.Gui);
        vp.attachScene(guiNode);
        GuiContext context = NGEGui.register(vp, true);
        context.getNavigator().setSimulateCursor(true);
        context.getNavigator().setSimulatedCursorSpeed(100f);

        TestJoystick joystick = new TestJoystick();
        TestJoyInput joyInput = new TestJoyInput(joystick);
        InputManager inputManager = newInputManager(joyInput);
        DefaultNavigatorInputHandler handler = new DefaultNavigatorInputHandler(vp);
        handler.registerListener(inputManager);
        handler.setInputDevice(inputManager, joystick);

        joyInput.queue(new JoyAxisEvent(joystick.getXAxis(), 1f));
        inputManager.update(0.1f);
        context.getNavigator().update(0.1f);
        float slowX = context.getNavigator().getCursor().getLocalTranslation().x;

        context.getNavigator().setSimulatedCursorSpeed(300f);
        handler.update(0.1f);
        context.getNavigator().update(0.1f);
        float fastX = context.getNavigator().getCursor().getLocalTranslation().x;

        assertEquals(10f, slowX - 400f, 0.001f);
        assertEquals(30f, fastX - slowX, 0.001f);
    }

    @Test
    public void hardwareCursorKeepsSoftwareCursorOffUntilSimulatedCursorMoves() {
        initializeGui();

        ViewPort vp = new ViewPort("gui-hardware-cursor", new Camera(800, 600));
        vp.attachScene(new Node("GuiNode"));
        Navigator navigator = NGEGui.register(vp, true).getNavigator();

        assertNotNull(navigator.getCursor());

        navigator.setHardwareCursor(true);

        assertEquals(true, navigator.isHardwareCursor());
        assertEquals(true, navigator.isCursorVisible());
        assertNull(navigator.getCursor());

        navigator.setSimulateCursor(true);
        assertEquals(true, navigator.updateSimulatedCursorPosition(10, 20));

        assertNotNull(navigator.getCursor());
        assertEquals(true, navigator.isCursorActive());
    }

    @Test
    public void hiddenSystemCursorClicksUseVirtualCursorPosition() {
        initializeGui();

        ViewPort vp = new ViewPort("gui-mouse", new Camera(800, 600));
        Node guiNode = new Node("GuiNode");
        guiNode.setQueueBucket(Bucket.Gui);
        vp.attachScene(guiNode);
        GuiContext context = NGEGui.register(vp, true);

        Node root = new Node("root");
        Node physicalTarget = pickable("physical", 100f, 100f);
        Node virtualTarget = pickable("virtual", 300f, 100f);
        List<String> physicalEvents = new ArrayList<>();
        List<String> virtualEvents = new ArrayList<>();
        addRecorder(physicalTarget, physicalEvents);
        addRecorder(virtualTarget, virtualEvents);
        root.attachChild(physicalTarget);
        root.attachChild(virtualTarget);
        guiNode.attachChild(root);
        guiNode.updateGeometricState();
        context.getNavigator().pushLayer(root);

        TestMouseInput mouseInput = new TestMouseInput();
        InputManager inputManager = newInputManager(mouseInput, null);
        DefaultNavigatorInputHandler handler = new DefaultNavigatorInputHandler(vp);
        handler.registerListener(inputManager);
        handler.setInputDevice(inputManager, new Mouse());
        inputManager.setCursorVisible(false);

        mouseInput.queue(new MouseMotionEvent(300, 100, -400, 400, 0, 0));
        inputManager.update(0.016f);
        mouseInput.queue(new MouseButtonEvent(MouseInput.BUTTON_LEFT, true, 110, 110));
        inputManager.update(0.016f);
        mouseInput.queue(new MouseButtonEvent(MouseInput.BUTTON_LEFT, false, 110, 110));
        inputManager.update(0.016f);

        assertEquals(false, physicalEvents.stream().anyMatch(e -> e.startsWith("action:")));
        assertEquals(List.of(
            "gained:virtual",
            "action:virtual:true",
            "action:virtual:false"
        ), virtualEvents);
    }

    @Test
    public void hiddenSystemCursorFallsBackToRelativeMotionWhenAbsolutePositionIsFixed() {
        initializeGui();

        ViewPort vp = new ViewPort("gui-mouse-relative", new Camera(800, 600));
        Node guiNode = new Node("GuiNode");
        guiNode.setQueueBucket(Bucket.Gui);
        vp.attachScene(guiNode);
        GuiContext context = NGEGui.register(vp, true);

        Node root = new Node("root");
        Node target = pickable("target", 425f, 275f);
        List<String> events = new ArrayList<>();
        addRecorder(target, events);
        root.attachChild(target);
        guiNode.attachChild(root);
        guiNode.updateGeometricState();
        context.getNavigator().pushLayer(root);

        TestMouseInput mouseInput = new TestMouseInput();
        InputManager inputManager = newInputManager(mouseInput, null);
        DefaultNavigatorInputHandler handler = new DefaultNavigatorInputHandler(vp);
        handler.registerListener(inputManager);
        handler.setInputDevice(inputManager, new Mouse());
        inputManager.setCursorVisible(false);

        mouseInput.queue(new MouseMotionEvent(400, 300, 50, 0, 0, 0));
        inputManager.update(0.016f);
        mouseInput.queue(new MouseButtonEvent(MouseInput.BUTTON_LEFT, true, 0, 0));
        inputManager.update(0.016f);
        mouseInput.queue(new MouseButtonEvent(MouseInput.BUTTON_LEFT, false, 0, 0));
        inputManager.update(0.016f);

        assertEquals(List.of(
            "gained:target",
            "action:target:true",
            "action:target:false"
        ), events);
    }

    @Test
    public void disabledCursorDoesNotBecomeActiveWhenPositionUpdates() {
        initializeGui();

        ViewPort vp = new ViewPort("gui-cursor", new Camera(800, 600));
        Node guiNode = new Node("GuiNode");
        guiNode.setQueueBucket(Bucket.Gui);
        vp.attachScene(guiNode);
        Navigator navigator = NGEGui.register(vp, true).getNavigator();

        assertEquals(true, navigator.updateCursorPosition(10, 20));
        assertEquals(true, navigator.isCursorVisible());
        assertEquals(true, navigator.isCursorActive());

        navigator.setCursor(false);

        assertEquals(false, navigator.updateCursorPosition(30, 40));
        assertEquals(false, navigator.isCursorVisible());
        assertEquals(false, navigator.isCursorActive());
    }

    @Test
    public void cursorAutoHideDefaultsToFiveSeconds() {
        initializeGui();

        ViewPort vp = new ViewPort("gui-cursor-autohide", new Camera(800, 600));
        Node guiNode = new Node("GuiNode");
        guiNode.setQueueBucket(Bucket.Gui);
        vp.attachScene(guiNode);
        Navigator navigator = NGEGui.register(vp, true).getNavigator();

        assertEquals(5f, navigator.getCursorAutoHideDelay(), 0.001f);
        assertEquals(0.75f, navigator.getCursorActivityThreshold(), 0.001f);
        assertEquals(0.25f, navigator.getSimulatedCursorDeadZone(), 0.001f);

        assertEquals(true, navigator.updateCursorPosition(10, 20));
        navigator.update(4.9f);
        assertEquals(true, navigator.isCursorActive());
        assertEquals(true, navigator.updateCursorPosition(10.2, 20.2));

        navigator.update(0.2f);
        assertEquals(false, navigator.isCursorActive());

        assertEquals(true, navigator.updateCursorPosition(12, 20));
        navigator.update(4.9f);
        assertEquals(true, navigator.isCursorActive());

        navigator.setCursorActivityThreshold(3f);
        assertEquals(3f, navigator.getCursorActivityThreshold(), 0.001f);
        assertEquals(true, navigator.updateCursorPosition(14, 20));

        navigator.update(0.2f);
        assertEquals(false, navigator.isCursorActive());
    }

    @Test
    public void simulatedCursorDeadZoneDoesNotKeepAutoHideAlive() {
        initializeGui();

        ViewPort vp = new ViewPort("gui-simulated-cursor-deadzone", new Camera(800, 600));
        Node guiNode = new Node("GuiNode");
        guiNode.setQueueBucket(Bucket.Gui);
        vp.attachScene(guiNode);
        GuiContext context = NGEGui.register(vp, true);
        Navigator navigator = context.getNavigator();
        navigator.setSimulateCursor(true);

        assertEquals(true, navigator.updateSimulatedCursorPosition(400, 300));
        navigator.update(4.9f);
        assertEquals(true, navigator.isCursorActive());

        TestJoystick joystick = new TestJoystick();
        TestJoyInput joyInput = new TestJoyInput(joystick);
        InputManager inputManager = newInputManager(joyInput);
        DefaultNavigatorInputHandler handler = new DefaultNavigatorInputHandler(vp);
        handler.registerListener(inputManager);
        handler.setInputDevice(inputManager, joystick);

        joyInput.queue(new JoyAxisEvent(joystick.getXAxis(), 0.20f));
        inputManager.update(0.016f);
        handler.update(0.016f);

        navigator.update(0.2f);
        assertEquals(false, navigator.isCursorActive());
    }

    @Test
    public void simulatedCursorIsEnabledByDefault() {
        initializeGui();

        ViewPort vp = new ViewPort("gui-simulated-cursor-default", new Camera(800, 600));
        Node guiNode = new Node("GuiNode");
        guiNode.setQueueBucket(Bucket.Gui);
        vp.attachScene(guiNode);
        Navigator navigator = NGEGui.register(vp, true).getNavigator();

        assertEquals(true, navigator.isSimulateCursor());
    }

    private static void initializeGui() {
        JmeSystem.setSystemDelegate(new TestSystemDelegate());
        AssetManager assets = JmeSystem.newAssetManager(
                NavigatorLayerTest.class.getResource("/com/jme3/asset/Desktop.cfg"));
        NGEGui.initialize(assets);
    }

    private static Node focusable(String name, float x, float y) {
        Node node = new Node(name);
        node.setLocalTranslation(new Vector3f(x, y, 0f));
        GuiControl control = new GuiControl("focus");
        control.setFocusable(true);
        node.addControl(control);
        return node;
    }

    private static Node pickable(String name, float x, float y) {
        Node node = focusable(name, x, y);
        node.attachChild(new Geometry(name + ".quad", new Quad(50f, 50f)));
        return node;
    }

    private static void addRecorder(Node node, List<String> events) {
        node.getControl(GuiControl.class).addFocusChangeListener(new FocusListener() {
            @Override
            public void focusGained(Spatial target) {
                events.add("gained:" + target.getName());
            }

            @Override
            public void focusLost(Spatial target) {
                events.add("lost:" + target.getName());
            }

            @Override
            public void focusAction(Spatial target, boolean pressed) {
                events.add("action:" + target.getName() + ":" + pressed);
            }

            @Override
            public void focusScrollUpdate(Spatial target, ScrollDirection dir, double value) {
                events.add("scroll:" + target.getName() + ":" + dir + ":" + value);
            }
        });
    }

    private static InputManager newInputManager(JoyInput joyInput) {
        return newInputManager(new DummyMouseInput(), joyInput);
    }

    private static InputManager newInputManager(MouseInput mouseInput, JoyInput joyInput) {
        DummyKeyInput keys = new DummyKeyInput();
        mouseInput.initialize();
        keys.initialize();
        if (joyInput != null) {
            joyInput.initialize();
        }
        return new InputManager(mouseInput, keys, joyInput, null);
    }

    private static final class TestMouseInput implements MouseInput {
        private final Queue<InputEvent<?>> events = new ArrayDeque<>();
        private RawInputListener listener;
        private boolean initialized;

        private void queue(InputEvent<?> event) {
            events.add(event);
        }

        @Override
        public void initialize() {
            initialized = true;
        }

        @Override
        public void update() {
            while (!events.isEmpty()) {
                InputEvent<?> event = events.remove();
                if (event instanceof MouseMotionEvent) {
                    listener.onMouseMotionEvent((MouseMotionEvent) event);
                } else if (event instanceof MouseButtonEvent) {
                    listener.onMouseButtonEvent((MouseButtonEvent) event);
                } else {
                    throw new IllegalArgumentException("Unsupported event: " + event);
                }
            }
        }

        @Override
        public void destroy() {
            initialized = false;
        }

        @Override
        public boolean isInitialized() {
            return initialized;
        }

        @Override
        public void setInputListener(RawInputListener listener) {
            this.listener = listener;
        }

        @Override
        public long getInputTimeNanos() {
            return System.nanoTime();
        }

        @Override
        public void setCursorVisible(boolean visible) {
        }

        @Override
        public int getButtonCount() {
            return 3;
        }

        @Override
        public void setNativeCursor(JmeCursor cursor) {
        }
    }

    private static final class TestImeComposer implements ImeComposer {
        private final AtomicInteger closeCount = new AtomicInteger();
        private boolean open;

        @Override
        public void open(GuiContext ctx, Consumer<ImeCompositionEvent> listener, ImeCompositionEvent event,
                Function<Character, Character> inputFilter, Function<String, Float> getLineWidth) {
            open = true;
        }

        @Override
        public void close() {
            closeCount.incrementAndGet();
            open = false;
        }

        @Override
        public boolean isOpen() {
            return open;
        }

        @Override
        public void copyAll() {
        }

        @Override
        public void pasteReplace() {
        }
    }

    private static final class TestJoyInput implements JoyInput {
        private final Queue<InputEvent<?>> events = new ArrayDeque<>();
        private final Joystick joystick;
        private RawInputListener listener;
        private boolean initialized;

        private TestJoyInput(Joystick joystick) {
            this.joystick = joystick;
        }

        private void queue(InputEvent<?> event) {
            events.add(event);
        }

        @Override
        public void initialize() {
            initialized = true;
        }

        @Override
        public void update() {
            while (!events.isEmpty()) {
                InputEvent<?> event = events.remove();
                if (event instanceof JoyButtonEvent) {
                    listener.onJoyButtonEvent((JoyButtonEvent) event);
                } else if (event instanceof JoyAxisEvent) {
                    listener.onJoyAxisEvent((JoyAxisEvent) event);
                } else {
                    throw new IllegalArgumentException("Unsupported event: " + event);
                }
            }
        }

        @Override
        public void destroy() {
            initialized = false;
        }

        @Override
        public boolean isInitialized() {
            return initialized;
        }

        @Override
        public void setInputListener(RawInputListener listener) {
            this.listener = listener;
        }

        @Override
        public long getInputTimeNanos() {
            return System.nanoTime();
        }

        @Override
        public void setJoyRumble(int joyId, float amountHigh, float amountLow, float duration) {
        }

        @Override
        public Joystick[] loadJoysticks(InputManager inputManager) {
            return new Joystick[] { joystick };
        }
    }

    private static final class TestJoystick implements Joystick {
        @Override
        public int getJoyId() {
            return 0;
        }

        @Override
        public String getName() {
            return "test";
        }

        @Override
        public List<JoystickAxis> getAxes() {
            return List.of();
        }

        @Override
        public List<JoystickButton> getButtons() {
            return List.of();
        }

        @Override
        public JoystickAxis getAxis(String logicalId) {
            return new TestJoystickAxis(this, axisId(logicalId), logicalId);
        }

        @Override
        public JoystickButton getButton(String logicalId) {
            return new TestJoystickButton(this, Integer.parseInt(logicalId));
        }

        @Override
        public JoystickAxis getXAxis() {
            return getAxis(JoystickAxis.AXIS_XBOX_LEFT_THUMB_STICK_X);
        }

        @Override
        public JoystickAxis getYAxis() {
            return getAxis(JoystickAxis.AXIS_XBOX_LEFT_THUMB_STICK_Y);
        }

        @Override
        public JoystickAxis getPovXAxis() {
            return getAxis(JoystickAxis.AXIS_XBOX_RIGHT_THUMB_STICK_X);
        }

        @Override
        public JoystickAxis getPovYAxis() {
            return getAxis(JoystickAxis.AXIS_XBOX_RIGHT_THUMB_STICK_Y);
        }

        @Override
        public int getXAxisIndex() {
            return 0;
        }

        @Override
        public int getYAxisIndex() {
            return 1;
        }

        @Override
        public int getAxisCount() {
            return 4;
        }

        @Override
        public int getButtonCount() {
            return 16;
        }

        @Override
        public void assignButton(String mappingName, int buttonId) {
        }

        @Override
        public void assignAxis(String positiveMapping, String negativeMapping, int axisId) {
        }

        @Override
        public void rumble(float amountHigh, float amountLow, float duration) {
        }

        private int axisId(String logicalId) {
            if (JoystickAxis.AXIS_XBOX_LEFT_THUMB_STICK_X.equals(logicalId)) return 0;
            if (JoystickAxis.AXIS_XBOX_LEFT_THUMB_STICK_Y.equals(logicalId)) return 1;
            if (JoystickAxis.AXIS_XBOX_RIGHT_THUMB_STICK_X.equals(logicalId)) return 2;
            if (JoystickAxis.AXIS_XBOX_RIGHT_THUMB_STICK_Y.equals(logicalId)) return 3;
            return 0;
        }
    }

    private static final class TestJoystickAxis implements JoystickAxis {
        private final Joystick joystick;
        private final int axisId;
        private final String logicalId;

        private TestJoystickAxis(Joystick joystick, int axisId, String logicalId) {
            this.joystick = joystick;
            this.axisId = axisId;
            this.logicalId = logicalId;
        }

        @Override
        public void assignAxis(String positiveMapping, String negativeMapping) {
        }

        @Override
        public Joystick getJoystick() {
            return joystick;
        }

        @Override
        public String getName() {
            return logicalId;
        }

        @Override
        public String getLogicalId() {
            return logicalId;
        }

        @Override
        public int getAxisId() {
            return axisId;
        }

        @Override
        public boolean isAnalog() {
            return true;
        }

        @Override
        public boolean isRelative() {
            return false;
        }

        @Override
        public float getDeadZone() {
            return 0f;
        }
    }

    private static final class TestJoystickButton implements JoystickButton {
        private final Joystick joystick;
        private final int buttonId;

        private TestJoystickButton(Joystick joystick, int buttonId) {
            this.joystick = joystick;
            this.buttonId = buttonId;
        }

        @Override
        public void assignButton(String mappingName) {
        }

        @Override
        public Joystick getJoystick() {
            return joystick;
        }

        @Override
        public String getName() {
            return String.valueOf(buttonId);
        }

        @Override
        public String getLogicalId() {
            return String.valueOf(buttonId);
        }

        @Override
        public int getButtonId() {
            return buttonId;
        }
    }

    private static final class TestSystemDelegate extends JmeSystemDelegate {
        @Override
        public void writeImageFile(OutputStream outStream, String format, ByteBuffer imageData, int width, int height)
                throws IOException {
        }

        @Override
        public URL getPlatformAssetConfigURL() {
            return NavigatorLayerTest.class.getResource("/com/jme3/asset/Desktop.cfg");
        }

        @Override
        public JmeContext newContext(AppSettings settings, JmeContext.Type contextType) {
            return null;
        }

        @Override
        public AudioRenderer newAudioRenderer(AppSettings settings) {
            return null;
        }

        @Override
        public void initialize(AppSettings settings) {
        }

        @Override
        public void showSoftKeyboard(boolean show) {
        }
    }
}
