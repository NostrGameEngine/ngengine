/**
 * Copyright (c) 2026, Nostr Game Engine
 * All rights reserved.
 */
package ngetests.tests.gui;

import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import org.ngengine.Components;
import org.ngengine.NGEApplication;
import org.ngengine.NGEApplication.NGEAppRunner;
import org.ngengine.ViewPortManager;
import org.ngengine.components.AbstractComponent;
import org.ngengine.components.ComponentManager;
import org.ngengine.gui.ListBox;
import org.ngengine.gui.core.GuiControl;
import org.ngengine.gui.guix.NLabel;
import org.ngengine.gui.guix.containers.NPanel;
import org.ngengine.gui.guix.win.NWindow;
import org.ngengine.gui.guix.win.NWindowManagerComponent;
import org.ngengine.platform.NGEPlatform;
import org.ngengine.platform.jvm.JVMAsyncPlatform;

public class TestNWindowListBoxScrollbarDrag {

    public static void main(String[] args) {
        NGEPlatform.set(new JVMAsyncPlatform());
        NGEAppRunner app = NGEApplication.createApp(a -> {
            NWindowManagerComponent windows = new NWindowManagerComponent(true);
            Components.mount(a, windows).enable();
            windows.setInteractionEnabled(true);
            Components.mount(a, new Launcher()).enable();
        });
        app.run();
    }

    public static class Launcher extends AbstractComponent {
        @Override
        protected void onEnable(ComponentManager mng, boolean firstTime) {
            getInstanceOf(ViewPortManager.class).getMainSceneViewPort()
                    .setBackgroundColor(new ColorRGBA(0.34f, 0.37f, 0.41f, 1f));
            getInstanceOf(NWindowManagerComponent.class).showWindow(ListWindow.class);
        }

        @Override
        protected void onDisable(ComponentManager mng) {
        }
    }

    public static class ListWindow extends NWindow<Void> {
        private ListBox<String> listBox;
        private NLabel status;
        private boolean updateListenerRegistered;

        @Override
        protected void compose(Vector3f size, Void args) throws Throwable {
            setTitle("Relative ListBox Scrollbar Drag");
            setFitContent(false);

            NPanel content = getContent();
            content.addChild(new NLabel("Relative ListBox scrollbar drag"));

            listBox = content.addChild(new ListBox<>());
            listBox.setVisibleItems(10);
            listBox.setPreferredSize(0.42f, 0.36f);
            for (int i = 1; i <= 80; i++) {
                listBox.getModel().add(String.format("Item %02d", i));
            }

            status = content.addChild(new NLabel(""));
            if (!updateListenerRegistered) {
                getControl(GuiControl.class).addUpdateListener((source, tpf) -> updateStatus());
                updateListenerRegistered = true;
            }
            updateStatus();
        }

        private void updateStatus() {
            if (listBox == null || status == null) {
                return;
            }
            status.setText("first visible: " + listBox.getGridPanel().getRow()
                    + " / slider: " + String.format("%.2f", listBox.getSlider().getModel().getValue()));
        }
    }
}
