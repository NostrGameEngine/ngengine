/**
 * Copyright (c) 2026, Nostr Game Engine
 * All rights reserved.
 */
package ngetests.tests.gui;

import com.jme3.app.DebugKeysAppState;
import com.jme3.app.SimpleApplication;
import com.jme3.app.StatsAppState;
import com.jme3.input.InputDevice;
import com.jme3.input.RawInputListener;
import com.jme3.input.event.JoyAxisEvent;
import com.jme3.input.event.JoyButtonEvent;
import com.jme3.input.event.KeyInputEvent;
import com.jme3.input.event.MouseButtonEvent;
import com.jme3.input.event.MouseMotionEvent;
import com.jme3.input.event.TouchEvent;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.jme3.system.AppSettings;
import org.ngengine.gui.Container;
import org.ngengine.gui.Label;
import org.ngengine.gui.ListBox;
import org.ngengine.gui.NGEGui;
import org.ngengine.gui.NGEStyle;
import org.ngengine.gui.core.GuiControl;
import org.ngengine.gui.ime.PhysicalKeyboardImeComposer;
import org.ngengine.gui.nav.DefaultNavigatorInputHandler;

public class TestListBoxScrollbarDrag extends SimpleApplication implements RawInputListener {

    private DefaultNavigatorInputHandler inputHandler;
    private ListBox<String> listBox;
    private Label status;

    public static void main(String[] args) {
        TestListBoxScrollbarDrag app = new TestListBoxScrollbarDrag();
        AppSettings settings = new AppSettings(true);
        settings.setRenderer(AppSettings.LWJGL_OPENGL40);
        settings.setWidth(1280);
        settings.setHeight(720);
        settings.setResizable(true);
        settings.setVSync(true);
        settings.setTitle("ListBox Scrollbar Drag");
        app.setSettings(settings);
        app.start();
    }

    public TestListBoxScrollbarDrag() {
        super(new StatsAppState(), new DebugKeysAppState());
    }

    @Override
    public void simpleInitApp() {
        viewPort.setBackgroundColor(ColorRGBA.DarkGray);
        setPauseOnLostFocus(false);
        setDisplayFps(false);
        setDisplayStatView(false);

        NGEGui.initialize(assetManager);
        NGEStyle.installAndUse();
        NGEGui.register(guiViewPort, true);
        NGEGui.get(guiViewPort).setImeComposer(new PhysicalKeyboardImeComposer(inputManager));

        inputHandler = new DefaultNavigatorInputHandler(guiViewPort);
        inputHandler.registerListener(inputManager);
        inputManager.addRawInputListener(this);

        Container window = new Container();
        window.addChild(new Label("ListBox scrollbar drag"));

        listBox = window.addChild(new ListBox<>());
        listBox.setVisibleItems(10);
        listBox.setPreferredSize(420f, 360f);
        for (int i = 1; i <= 80; i++) {
            listBox.getModel().add(String.format("Item %02d", i));
        }

        status = window.addChild(new Label(""));
        window.setLocalTranslation(420f, 610f, 100f);
        guiNode.attachChild(window);
    }

    @Override
    public void simpleUpdate(float tpf) {
        NGEGui.update(guiViewPort, tpf);
        if (listBox != null && status != null) {
            status.setText("first visible: " + listBox.getGridPanel().getRow()
                    + " / slider: " + String.format("%.2f", listBox.getSlider().getModel().getValue()));
        }
    }

    @Override
    public void beginInput() {
    }

    @Override
    public void endInput() {
    }

    @Override
    public void onJoyAxisEvent(JoyAxisEvent evt) {
        setInputDevice(evt.getDevice());
    }

    @Override
    public void onJoyButtonEvent(JoyButtonEvent evt) {
        setInputDevice(evt.getDevice());
    }

    @Override
    public void onMouseMotionEvent(MouseMotionEvent evt) {
        setInputDevice(evt.getDevice());
    }

    @Override
    public void onMouseButtonEvent(MouseButtonEvent evt) {
        setInputDevice(evt.getDevice());
    }

    @Override
    public void onKeyEvent(KeyInputEvent evt) {
        setInputDevice(evt.getDevice());
    }

    @Override
    public void onTouchEvent(TouchEvent evt) {
        setInputDevice(evt.getDevice());
    }

    private void setInputDevice(InputDevice device) {
        inputHandler.setInputDevice(inputManager, device);
    }
}
