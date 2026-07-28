/*
 * Copyright (c) 2026, Nostr Game Engine
 * All rights reserved.
 */
package org.ngengine.gui.nav;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.jme3.asset.AssetManager;
import com.jme3.audio.AudioRenderer;
import com.jme3.bounding.BoundingVolume;
import com.jme3.renderer.Camera;
import com.jme3.renderer.ViewPort;
import com.jme3.renderer.queue.RenderQueue.Bucket;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.shape.Quad;
import com.jme3.system.AppSettings;
import com.jme3.system.JmeContext;
import com.jme3.system.JmeSystem;
import com.jme3.system.JmeSystemDelegate;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URL;
import java.nio.ByteBuffer;
import org.junit.jupiter.api.Test;
import org.ngengine.gui.GuiContext;
import org.ngengine.gui.LayerComparator;
import org.ngengine.gui.NGEGui;
import org.ngengine.gui.Panel;

public class PopupHandlerTest {

    @Test
    public void missingPopupBoundsHaveNeutralDepth() {
        TestPopupHandler handler = new TestPopupHandler();

        assertEquals(0f, handler.minZ(null));
    }

    @Test
    public void popupAndBlockerUseEngineLayersAboveWindowsAndBelowCursor() {
        GuiContext context = newContext("popup-layers");
        PopupHandler handler = context.getPopupHandler();
        Node guiNode = context.getGuiNode();
        Panel popup = new Panel();
        popup.setLocalTranslation(15, 25, 3);
        LayerComparator.resetLayer(popup, 7);

        handler.showPopup(popup);

        Geometry blocker = findLastBlocker(guiNode);
        assertEquals(PopupHandler.POPUP_BASE_LAYER, LayerComparator.getLayer((Spatial) blocker));
        assertEquals(PopupHandler.POPUP_BASE_LAYER + 1, LayerComparator.getLayer(popup));
        assertTrue(LayerComparator.getLayer((Spatial) blocker) > 100);
        assertTrue(LayerComparator.getLayer(popup) < PopupHandler.CURSOR_LAYER);
        assertTrue(popup.getLocalTranslation().z > blocker.getLocalTranslation().z);

        handler.closePopup(popup);

        assertNull(blocker.getParent());
        assertNull(popup.getParent());
        assertEquals(3f, popup.getLocalTranslation().z, 0.001f);
        assertEquals(7, LayerComparator.getLayer(popup));
    }

    @Test
    public void nestedPopupLayersKeepEachNewBlockerAboveThePreviousPopup() {
        GuiContext context = newContext("popup-stack");
        PopupHandler handler = context.getPopupHandler();
        Node guiNode = context.getGuiNode();
        Panel first = new Panel();
        Panel second = new Panel();

        handler.showPopup(first);
        Geometry firstBlocker = findLastBlocker(guiNode);
        handler.showPopup(second);
        Geometry secondBlocker = findLastBlocker(guiNode);

        assertEquals(200, LayerComparator.getLayer((Spatial) firstBlocker));
        assertEquals(201, LayerComparator.getLayer(first));
        assertEquals(202, LayerComparator.getLayer((Spatial) secondBlocker));
        assertEquals(203, LayerComparator.getLayer(second));
        assertTrue(LayerComparator.getLayer((Spatial) secondBlocker) > LayerComparator.getLayer(first));
        assertTrue(LayerComparator.getLayer(second) > LayerComparator.getLayer((Spatial) secondBlocker));
        assertTrue(LayerComparator.getLayer(second) < PopupHandler.CURSOR_LAYER);

        handler.closePopup(first);

        assertNull(firstBlocker.getParent());
        assertSame(guiNode, secondBlocker.getParent());
        assertEquals(200, LayerComparator.getLayer((Spatial) secondBlocker));
        assertEquals(201, LayerComparator.getLayer(second));
        handler.closePopup(second);
    }

    @Test
    public void popupBlockerTracksLogicalGuiResize() {
        GuiContext context = newContext("popup-resize");
        PopupHandler handler = context.getPopupHandler();
        Panel popup = new Panel();

        handler.showPopup(popup);
        Geometry blocker = findLastBlocker(context.getGuiNode());
        Quad quad = (Quad) blocker.getMesh();
        assertEquals(800f, quad.getWidth(), 0.001f);
        assertEquals(600f, quad.getHeight(), 0.001f);

        context.getGuiCamera().resize(1200, 700, true);
        handler.update(0f);

        assertEquals(1200f, quad.getWidth(), 0.001f);
        assertEquals(700f, quad.getHeight(), 0.001f);
        handler.closePopup(popup);
    }

    @Test
    public void deepestSupportedPopupLayerStillStaysBelowCursor() {
        int deepestBlocker = PopupHandler.POPUP_BASE_LAYER
            + (PopupHandler.MAX_POPUP_STACK_SIZE - 1) * 2;

        assertEquals(PopupHandler.CURSOR_LAYER - 2, deepestBlocker);
        assertTrue(deepestBlocker + 1 < PopupHandler.CURSOR_LAYER);
    }

    private static GuiContext newContext(String name) {
        JmeSystem.setSystemDelegate(new TestSystemDelegate());
        AssetManager assets = JmeSystem.newAssetManager(
            PopupHandlerTest.class.getResource("/com/jme3/asset/Desktop.cfg")
        );
        NGEGui.initialize(assets);

        ViewPort viewPort = new ViewPort(name, new Camera(800, 600));
        Node guiNode = new Node("GuiNode");
        guiNode.setQueueBucket(Bucket.Gui);
        viewPort.attachScene(guiNode);
        return NGEGui.register(viewPort, true);
    }

    private static Geometry findLastBlocker(Node guiNode) {
        Geometry result = null;
        for (com.jme3.scene.Spatial child : guiNode.getChildren()) {
            if (child instanceof Geometry && "blocker".equals(child.getName())) {
                result = (Geometry) child;
            }
        }
        return result;
    }

    private static class TestPopupHandler extends PopupHandler {
        TestPopupHandler() {
            super(new Node("GuiNode"), new Camera(800, 600));
        }

        float minZ(BoundingVolume bounds) {
            return getMinZ(bounds);
        }
    }

    private static final class TestSystemDelegate extends JmeSystemDelegate {
        @Override
        public void writeImageFile(
            OutputStream outStream,
            String format,
            ByteBuffer imageData,
            int width,
            int height
        ) throws IOException {
        }

        @Override
        public URL getPlatformAssetConfigURL() {
            return PopupHandlerTest.class.getResource("/com/jme3/asset/Desktop.cfg");
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
