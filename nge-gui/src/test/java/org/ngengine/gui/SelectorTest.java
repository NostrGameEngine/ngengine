package org.ngengine.gui;

import static org.junit.jupiter.api.Assertions.assertEquals;
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

    private static final class TestSelector extends Selector<String> {
        private TestSelector(VersionedList<String> model) {
            super(model);
        }

        @Override
        protected void collapse() {
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
