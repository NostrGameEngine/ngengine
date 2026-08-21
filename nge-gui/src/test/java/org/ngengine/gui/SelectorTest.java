package org.ngengine.gui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.jme3.asset.AssetManager;
import com.jme3.audio.AudioRenderer;
import com.jme3.system.AppSettings;
import com.jme3.system.JmeContext;
import com.jme3.system.JmeSystem;
import com.jme3.system.JmeSystemDelegate;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URL;
import java.nio.ByteBuffer;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.ngengine.gui.ListBox.ListAction;
import org.ngengine.gui.core.VersionedList;
import org.ngengine.gui.core.VersionedReference;

public class SelectorTest {

    @Test
    @SuppressWarnings({"rawtypes", "unchecked"})
    public void popupClickCommitsSelectedValueBeforeCollapse() {
        JmeSystem.setSystemDelegate(new TestSystemDelegate());
        AssetManager assets = JmeSystem.newAssetManager(
            SelectorTest.class.getResource("/com/jme3/asset/Desktop.cfg")
        );
        NGEGui.initialize(assets);

        TestSelector selector = new TestSelector(
            new VersionedList<>(List.of("1280 x 720", "1600 x 900"))
        );
        VersionedReference<String> selected = selector.createSelectedItemReference();

        selector.getSelectionModel().setSelection(1);
        for (Object command : selector.getListBox().getCommands(ListAction.Click)) {
            ((Command) command).execute(selector.getListBox());
        }

        assertTrue(selected.update());
        assertEquals("1600 x 900", selected.get());
    }

    @Test
    @SuppressWarnings({"rawtypes", "unchecked"})
    public void popupClickCommitsEvenWhenSelectionVersionWasAlreadyObserved() {
        JmeSystem.setSystemDelegate(new TestSystemDelegate());
        AssetManager assets = JmeSystem.newAssetManager(
            SelectorTest.class.getResource("/com/jme3/asset/Desktop.cfg")
        );
        NGEGui.initialize(assets);

        TestSelector selector = new TestSelector(
            new VersionedList<>(List.of("1280 x 720", "1600 x 900"))
        );
        VersionedReference<String> selected = selector.createSelectedItemReference();

        selector.getSelectionModel().setSelection(1);
        // Reproduce the detached-popup ordering: another observer sees the
        // selection model change before the popup click command is dispatched.
        assertEquals("1600 x 900", selector.getSelectedItem());
        for (Object command : selector.getListBox().getCommands(ListAction.Click)) {
            ((Command) command).execute(selector.getListBox());
        }

        assertTrue(selected.update());
        assertEquals("1600 x 900", selected.get());
    }

    @Test
    public void selectablePredicateRejectsUnavailableValues() {
        JmeSystem.setSystemDelegate(new TestSystemDelegate());
        AssetManager assets = JmeSystem.newAssetManager(
            SelectorTest.class.getResource("/com/jme3/asset/Desktop.cfg")
        );
        NGEGui.initialize(assets);

        Selector<String> selector = new Selector<>(
            new VersionedList<>(List.of("Available", "Locked"))
        );
        selector.setSelectableItemPredicate(value -> !"Locked".equals(value));

        assertTrue(selector.getListBox().isSelectableItem("Available"));
        assertFalse(selector.getListBox().isSelectableItem("Locked"));
        selector.setSelectedItem("Locked");
        assertEquals("Available", selector.getSelectedItem());
    }

    @Test
    public void hoverPreviewDoesNotPublishUntilTheRowIsActivated() {
        JmeSystem.setSystemDelegate(new TestSystemDelegate());
        AssetManager assets = JmeSystem.newAssetManager(
            SelectorTest.class.getResource("/com/jme3/asset/Desktop.cfg")
        );
        NGEGui.initialize(assets);

        TestSelector selector = new TestSelector(
            new VersionedList<>(List.of("Windowed", "Fullscreen"))
        );
        VersionedReference<String> selected = selector.createSelectedItemReference();
        selector.previewing = true;
        selector.getSelectionModel().setSelection(1);
        selector.updateLogicalState(0f);

        assertFalse(selected.update());
        assertEquals("Windowed", selected.get());

        for (Object command : selector.getListBox().getCommands(ListAction.Click)) {
            ((Command) command).execute(selector.getListBox());
        }

        assertTrue(selected.update());
        assertEquals("Fullscreen", selected.get());
    }

    @Test
    public void listBoxAutoHidesItsScrollBarOnlyWhenEveryRowFits() {
        JmeSystem.setSystemDelegate(new TestSystemDelegate());
        AssetManager assets = JmeSystem.newAssetManager(
            SelectorTest.class.getResource("/com/jme3/asset/Desktop.cfg")
        );
        NGEGui.initialize(assets);

        ListBox<String> list = new ListBox<>(new VersionedList<>(List.of("One", "Two")));
        list.setVisibleItems(2);

        assertTrue(list.isAutoHideScrollBar());
        assertNull(list.getSlider().getParent());

        list.setVisibleItems(1);
        assertEquals(list, list.getSlider().getParent());

        list.setVisibleItems(2);
        list.setAutoHideScrollBar(false);
        assertEquals(list, list.getSlider().getParent());
    }

    private static final class TestSelector extends Selector<String> {
        private boolean previewing;

        private TestSelector(VersionedList<String> model) {
            super(model);
        }

        @Override
        protected void collapse() {
        }

        @Override
        public boolean isExpanded() {
            return previewing || super.isExpanded();
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
            return SelectorTest.class.getResource("/com/jme3/asset/Desktop.cfg");
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
