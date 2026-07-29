/*
 * Copyright (c) 2026, Nostr Game Engine
 * All rights reserved.
 */
package org.ngengine.gui.guix.containers;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.jme3.math.Vector3f;
import com.jme3.scene.Node;
import org.junit.jupiter.api.Test;
import org.ngengine.gui.Insets3f;
import org.ngengine.gui.Panel;
import org.ngengine.gui.component.AbstractGuiComponent;
import org.ngengine.gui.component.BorderLayout;

public class NPanelTest {

    @Test
    public void contentInsetsKeepBackgroundFullSizeAndMoveLayoutInsideIt() {
        NPanel panel = new NPanel();
        TrackingBackground background = new TrackingBackground();
        panel.setBackground(background);
        panel.setContentInsets(new Insets3f(3f, 5f, 7f, 11f));

        Panel child = new Panel();
        child.setPreferredSize(20f, 10f);
        panel.addChild(child);
        panel.setSize(new Vector3f(100f, 50f, 1f));

        assertEquals(new Vector3f(100f, 50f, 1f), background.size);

        Node content = ((BorderLayout) panel.getLayout()).getChild(BorderLayout.Position.Center);
        assertEquals(new Vector3f(5f, -3f, 0f), content.getLocalTranslation());
        assertEquals(new Vector3f(84f, 40f, 1f), content.getControl(
            org.ngengine.gui.core.GuiControl.class
        ).getSize());
        assertEquals(36f, panel.getPreferredSize().x);
        assertEquals(20f, panel.getPreferredSize().y);
    }

    @Test
    public void externalAndContentInsetsRemainIndependent() {
        NPanel panel = new NPanel();
        TrackingBackground background = new TrackingBackground();
        panel.setBackground(background);
        panel.setInsets(new Insets3f(2f, 3f, 4f, 5f));
        panel.setContentInsets(new Insets3f(3f, 5f, 7f, 11f));

        Panel child = new Panel();
        child.setPreferredSize(20f, 10f);
        panel.addChild(child);
        panel.setSize(new Vector3f(100f, 50f, 1f));

        assertEquals(new Vector3f(3f, -2f, 0f), background.position);
        assertEquals(new Vector3f(92f, 44f, 1f), background.size);

        Node content = ((BorderLayout) panel.getLayout()).getChild(BorderLayout.Position.Center);
        assertEquals(new Vector3f(8f, -5f, 0f), content.getLocalTranslation());
        assertEquals(new Vector3f(76f, 34f, 1f), content.getControl(
            org.ngengine.gui.core.GuiControl.class
        ).getSize());
        assertEquals(44f, panel.getPreferredSize().x);
        assertEquals(26f, panel.getPreferredSize().y);
    }

    private static final class TrackingBackground extends AbstractGuiComponent {
        private final Vector3f position = new Vector3f();
        private final Vector3f size = new Vector3f();

        @Override
        public void calculatePreferredSize(Vector3f size) {
        }

        @Override
        public void reshape(Vector3f pos, Vector3f size) {
            this.position.set(pos);
            this.size.set(size);
        }
    }
}
