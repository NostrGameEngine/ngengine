package org.ngengine.gui;

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
import org.junit.jupiter.api.Test;
import org.ngengine.gui.component.AbstractGuiComponent;

class TextFieldTest {

    @Test
    void contentInsetsPadTextWithoutShrinkingTheBackground() {
        JmeSystem.setSystemDelegate(new TestSystemDelegate());
        AssetManager assets = JmeSystem.newAssetManager(
            TextFieldTest.class.getResource("/com/jme3/asset/Desktop.cfg")
        );
        NGEGui.initialize(assets);

        TextField field = new TextField();
        TrackingBackground background = new TrackingBackground();
        field.setBackground(background);
        field.setInsets(new Insets3f(2f, 3f, 4f, 5f));
        field.setContentInsets(new Insets3f(7f, 11f, 13f, 17f));
        field.setSize(new Vector3f(100f, 50f, 1f));

        assertEquals(new Insets3f(2f, 3f, 4f, 5f), field.getInsets());
        assertEquals(new Insets3f(7f, 11f, 13f, 17f), field.getContentInsets());
        assertEquals(new Vector3f(3f, -2f, 0f), background.position);
        assertEquals(new Vector3f(92f, 44f, 1f), background.size);
    }

    @Test
    void labelContentInsetsPadTextWithoutShrinkingTheBackground() {
        JmeSystem.setSystemDelegate(new TestSystemDelegate());
        AssetManager assets = JmeSystem.newAssetManager(
            TextFieldTest.class.getResource("/com/jme3/asset/Desktop.cfg")
        );
        NGEGui.initialize(assets);

        Label label = new Label("Fullscreen");
        TrackingBackground background = new TrackingBackground();
        label.setBackground(background);
        label.setContentInsets(new Insets3f(6f, 10f, 6f, 10f));
        label.setSize(new Vector3f(200f, 40f, 1f));

        assertEquals(new Insets3f(6f, 10f, 6f, 10f), label.getContentInsets());
        assertEquals(new Vector3f(200f, 40f, 1f), background.size);
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
            return TextFieldTest.class.getResource("/com/jme3/asset/Desktop.cfg");
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
