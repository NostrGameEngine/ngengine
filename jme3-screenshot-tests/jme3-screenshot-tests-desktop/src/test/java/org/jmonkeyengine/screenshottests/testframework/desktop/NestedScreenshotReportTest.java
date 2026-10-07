package org.jmonkeyengine.screenshottests.testframework.desktop;

import com.aventstack.extentreports.ExtentReports;
import com.jme3.texture.Image;
import com.jme3.texture.image.ColorSpace;
import com.jme3.util.BufferUtils;
import java.io.File;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

class NestedScreenshotReportTest {
    @TempDir Path directory;

    @Test
    void createsNestedImageDirectories() throws Exception {
        ExtentReportExtensionJunitJupiter report = new ExtentReportExtensionJunitJupiter() {
            @Override
            public File reportFolderPath() {
                return directory.toFile();
            }
        };
        Field currentTest = ExtentReportExtensionJunitJupiter.class.getDeclaredField("currentTest");
        currentTest.setAccessible(true);
        Object previous = currentTest.get(null);
        currentTest.set(null, new ExtentReports().createTest("nested image"));
        try {
            Image image = new Image(Image.Format.RGBA8, 1, 1,
                BufferUtils.createByteBuffer(new byte[] {-1, 0, 0, -1}), ColorSpace.sRGB);
            report.attachImageInner("capture", "scene/variant/capture.png", image);
            Path capture = directory.resolve("scene/variant/capture.png");
            assertTrue(Files.isRegularFile(capture));
            assertEquals(1, ImageIO.read(capture.toFile()).getWidth());
        } finally {
            currentTest.set(null, previous);
        }
    }
}
