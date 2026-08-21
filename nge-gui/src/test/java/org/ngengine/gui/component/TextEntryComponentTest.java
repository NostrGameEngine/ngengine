package org.ngengine.gui.component;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.jme3.asset.AssetManager;
import com.jme3.audio.AudioRenderer;
import com.jme3.math.Vector3f;
import com.jme3.system.AppSettings;
import com.jme3.system.JmeContext;
import com.jme3.system.JmeSystem;
import com.jme3.system.JmeSystemDelegate;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URL;
import java.nio.ByteBuffer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.ngengine.gui.NGEGui;
import org.ngengine.gui.VAlignment;

class TextEntryComponentTest {
    private TextEntryComponent entry;

    @BeforeEach
    void createEntry() {
        JmeSystem.setSystemDelegate(new TestSystemDelegate());
        AssetManager assets = JmeSystem.newAssetManager(
            TextEntryComponentTest.class.getResource("/com/jme3/asset/Desktop.cfg")
        );
        NGEGui.initialize(assets);
        entry = new TextEntryComponent(assets.loadFont("Interface/Fonts/Default.fnt"));
        entry.setSingleLine(true);
        entry.reshape(new Vector3f(), new Vector3f(200f, 52f, 1f));
    }

    @Test
    void centeredSingleLineCursorUsesTheSameVerticalAlignmentAsText() {
        entry.setVAlignment(VAlignment.Top);
        float topAlignedY = entry.cursorY(0);
        entry.setVAlignment(VAlignment.Center);

        assertEquals(topAlignedY - Math.max(0f, 52f + topAlignedY) * 0.5f, entry.cursorY(0), 0.0001f);
    }

    @Test
    void topAlignedCursorRetainsItsOriginalPosition() {
        entry.setSingleLine(false);
        float originalY = entry.cursorY(0);
        entry.setSingleLine(true);
        entry.setVAlignment(VAlignment.Top);

        assertEquals(originalY, entry.cursorY(0), 0.0001f);
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
            return TextEntryComponentTest.class.getResource("/com/jme3/asset/Desktop.cfg");
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
