package org.ngengine.gradle.basis;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.imageio.ImageIO;
import org.gradle.testkit.runner.GradleRunner;
import org.gradle.testkit.runner.TaskOutcome;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class NgeBasisTextureEncoderPluginFunctionalTest {

    @TempDir
    Path projectDirectory;

    @Test
    void ordinaryImagesAreAlignedToGpuCompressionBlocksByDefault() throws IOException {
        Path commandLog = projectDirectory.resolve("basisu-commands.txt");
        Path fakeBasisu = fakeBasisu(commandLog);
        Files.writeString(projectDirectory.resolve("settings.gradle"),
                "rootProject.name = 'fixture'\n", StandardCharsets.UTF_8);
        Files.writeString(projectDirectory.resolve("build.gradle"), ""
                + "plugins {\n"
                + "    id 'java'\n"
                + "    id 'org.ngengine.tiled-basis-texture-encoder'\n"
                + "}\n"
                + "basisTextures {\n"
                + "    imageExtensions = ['png']\n"
                + "    basisuArguments = ['-ktx2', '-uastc', '-quiet']\n"
                + "    basisuExecutable = '"
                + fakeBasisu.toString().replace("\\", "\\\\") + "'\n"
                + "}\n",
                StandardCharsets.UTF_8);

        Path image = projectDirectory.resolve("src/main/resources/ui/unaligned.png");
        Files.createDirectories(image.getParent());
        writeImage(image, 6, 5);

        var result = GradleRunner.create()
                .withProjectDir(projectDirectory.toFile())
                .withArguments("encodeBasisTextures", "--stacktrace")
                .withPluginClasspath()
                .build();

        assertEquals(TaskOutcome.SUCCESS, result.task(":encodeBasisTextures").getOutcome());
        String command = Files.readString(commandLog, StandardCharsets.UTF_8);
        assertTrue(command.contains("-resample 8 8"));
    }

    @Test
    void tiledAtlasIsEncodedOnceAsMipmappedTextureArray() throws IOException {
        Path commandLog = projectDirectory.resolve("basisu-commands.txt");
        Path fakeBasisu = fakeBasisu(commandLog);
        Files.writeString(projectDirectory.resolve("settings.gradle"),
                "rootProject.name = 'fixture'\n", StandardCharsets.UTF_8);
        Files.writeString(projectDirectory.resolve("build.gradle"), ""
                + "plugins {\n"
                + "    id 'java'\n"
                + "    id 'org.ngengine.tiled-basis-texture-encoder'\n"
                + "}\n"
                + "basisTextures {\n"
                + "    imageExtensions = ['png']\n"
                + "    basisuArguments = ['-ktx2', '-uastc', '-quiet']\n"
                + "    basisuExecutable = '"
                + fakeBasisu.toString().replace("\\", "\\\\") + "'\n"
                + "}\n",
                StandardCharsets.UTF_8);

        Path tilesetDirectory = projectDirectory.resolve("src/main/resources/tilesets");
        Files.createDirectories(tilesetDirectory.resolve("atlas"));
        Files.writeString(tilesetDirectory.resolve("fixture.tsx"), ""
                + "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                + "<tileset name=\"fixture\" tilewidth=\"4\" tileheight=\"4\" "
                + "spacing=\"1\" margin=\"1\" tilecount=\"4\" columns=\"2\">\n"
                + "  <image source=\"atlas/fixture.png\" width=\"11\" height=\"11\"/>\n"
                + "</tileset>\n",
                StandardCharsets.UTF_8);
        writeAtlas(tilesetDirectory.resolve("atlas/fixture.png"));

        var result = GradleRunner.create()
                .withProjectDir(projectDirectory.toFile())
                .withArguments("processResources", "--stacktrace")
                .withPluginClasspath()
                .build();

        assertEquals(TaskOutcome.SUCCESS, result.task(":encodeBasisTextures").getOutcome());
        assertEquals(TaskOutcome.SUCCESS, result.task(":encodeTiledBasisTextureArrays").getOutcome());
        assertTrue(Files.isRegularFile(projectDirectory.resolve(
                "build/resources/main/tilesets/atlas/fixture.png.basis")));

        String commands = Files.readString(commandLog, StandardCharsets.UTF_8);
        assertEquals(1, commands.lines().count());
        assertTrue(commands.contains("-mipmap"));
        assertTrue(commands.contains("-tex_type 2darray"));
        assertEquals(4, countOccurrences(commands, "layer-"));
    }

    private static void writeAtlas(Path path) throws IOException {
        writeImage(path, 11, 11);
    }

    private static void writeImage(Path path, int width, int height) throws IOException {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                image.setRGB(x, y, 0xff000000 | x * 17 << 8 | y * 17);
            }
        }
        ImageIO.write(image, "PNG", path.toFile());
    }

    private Path fakeBasisu(Path commandLog) throws IOException {
        Path executable = projectDirectory.resolve("basisu-fake");
        Files.writeString(executable, ""
                + "#!/bin/sh\n"
                + "printf '%s\\n' \"$*\" >> '" + commandLog + "'\n"
                + "out=''\n"
                + "while [ \"$#\" -gt 0 ]; do\n"
                + "  if [ \"$1\" = '-output_file' ]; then\n"
                + "    shift\n"
                + "    out=\"$1\"\n"
                + "  fi\n"
                + "  shift\n"
                + "done\n"
                + "printf 'fake-basisu\\n' > \"$out\"\n",
                StandardCharsets.UTF_8);
        executable.toFile().setExecutable(true);
        return executable;
    }

    private static int countOccurrences(String value, String needle) {
        int count = 0;
        int offset = 0;
        while ((offset = value.indexOf(needle, offset)) >= 0) {
            count++;
            offset += needle.length();
        }
        return count;
    }
}
