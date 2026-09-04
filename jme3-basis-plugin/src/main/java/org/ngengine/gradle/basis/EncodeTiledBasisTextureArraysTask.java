package org.ngengine.gradle.basis;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import javax.imageio.ImageIO;
import org.gradle.api.DefaultTask;
import org.gradle.api.GradleException;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputFiles;
import org.gradle.api.tasks.Internal;
import org.gradle.api.tasks.Optional;
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.api.tasks.PathSensitive;
import org.gradle.api.tasks.PathSensitivity;
import org.gradle.api.tasks.TaskAction;
import org.gradle.work.DisableCachingByDefault;
import org.ngengine.basis.gradle.EncodeBasisTexturesTask;

/** Encodes Tiled atlas cells as mipmapped Basis texture-array layers. */
@DisableCachingByDefault(because = "The task invokes a platform-specific external basisu executable.")
public abstract class EncodeTiledBasisTextureArraysTask extends DefaultTask {
    private EncodeBasisTexturesTask ordinaryEncoder;

    @Input
    public abstract ListProperty<String> getResourceDirectories();

    @Input
    public abstract ListProperty<String> getClasspathResourceDirectories();

    @Input
    public abstract ListProperty<String> getBasisuArguments();

    @Input
    @Optional
    public abstract Property<String> getBasisuExecutable();

    @Input
    public abstract Property<Boolean> getHandleTiledTilesets();

    @InputFiles
    @PathSensitive(PathSensitivity.RELATIVE)
    public abstract ConfigurableFileCollection getResourceFiles();

    @OutputDirectory
    public abstract DirectoryProperty getOutputDirectory();

    @Internal
    public EncodeBasisTexturesTask getOrdinaryEncoder() {
        return ordinaryEncoder;
    }

    public void setOrdinaryEncoder(EncodeBasisTexturesTask ordinaryEncoder) {
        this.ordinaryEncoder = ordinaryEncoder;
    }

    /** Performs deterministic atlas discovery, slicing, and encoding. */
    @TaskAction
    public void encode() {
        if (!getHandleTiledTilesets().get()) {
            return;
        }
        if (ordinaryEncoder == null) {
            throw new GradleException("Ordinary Basis encoder task was not configured");
        }

        List<Path> roots = classpathResourceRoots();
        List<TiledAtlas> atlases = TiledAtlasScanner.scan(roots);
        Path outputRoot = getOutputDirectory().get().getAsFile().toPath();
        try {
            Files.createDirectories(outputRoot);
            Path basisu = ordinaryEncoder.resolveBasisuExecutable();
            for (TiledAtlas atlas : atlases) {
                encodeAtlas(basisu, outputRoot, atlas);
            }
        } catch (IOException exception) {
            throw new GradleException("Unable to encode Tiled Basis texture arrays", exception);
        }
    }

    private void encodeAtlas(Path basisu, Path outputRoot, TiledAtlas atlas) throws IOException {
        Path temporaryAtlasDirectory = getTemporaryDir().toPath()
                .resolve(Integer.toUnsignedString(atlas.getResourcePath().hashCode()));
        getProject().delete(temporaryAtlasDirectory);
        Files.createDirectories(temporaryAtlasDirectory);

        List<Path> layers = new ArrayList<>(atlas.getTileCount());
        int[] pixels = new int[Math.multiplyExact(atlas.getTileWidth(), atlas.getTileHeight())];
        for (int tileIndex = 0; tileIndex < atlas.getTileCount(); tileIndex++) {
            int column = tileIndex % atlas.getColumns();
            int row = tileIndex / atlas.getColumns();
            int x = atlas.getMargin() + column * (atlas.getTileWidth() + atlas.getSpacing());
            int y = atlas.getMargin() + row * (atlas.getTileHeight() + atlas.getSpacing());
            atlas.getImage().getRGB(
                    x, y, atlas.getTileWidth(), atlas.getTileHeight(), pixels, 0, atlas.getTileWidth());
            BufferedImage layer = new BufferedImage(
                    atlas.getTileWidth(), atlas.getTileHeight(), BufferedImage.TYPE_INT_ARGB);
            layer.setRGB(
                    0, 0, atlas.getTileWidth(), atlas.getTileHeight(), pixels, 0, atlas.getTileWidth());
            Path layerPath = temporaryAtlasDirectory.resolve(String.format("layer-%06d.png", tileIndex));
            if (!ImageIO.write(layer, "PNG", layerPath.toFile())) {
                throw new IOException("PNG writer is unavailable");
            }
            layers.add(layerPath);
        }

        Path output = outputRoot.resolve(atlas.getResourcePath() + ".basis");
        Files.createDirectories(output.getParent());
        List<String> command = textureArrayCommand(basisu, output, layers);
        Process process = new ProcessBuilder(command).redirectErrorStream(true).start();
        byte[] processOutput = process.getInputStream().readAllBytes();
        try {
            int exitCode = process.waitFor();
            if (exitCode != 0) {
                throw new GradleException("basisu failed for Tiled atlas " + atlas.getImagePath()
                        + "\n" + new String(processOutput, StandardCharsets.UTF_8));
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new GradleException("Interrupted while encoding Tiled atlas "
                    + atlas.getImagePath(), exception);
        }
    }

    private List<String> textureArrayCommand(Path basisu, Path output, List<Path> layers) {
        List<String> command = new ArrayList<>();
        command.add(basisu.toString());
        List<String> configured = getBasisuArguments().get();
        for (int index = 0; index < configured.size(); index++) {
            String argument = configured.get(index);
            if ("-tex_type".equals(argument)) {
                index++;
                continue;
            }
            if (!"-tex_array".equals(argument)) {
                command.add(argument);
            }
        }
        if (!command.contains("-mipmap")) {
            command.add("-mipmap");
        }
        command.add("-tex_type");
        command.add("2darray");
        command.add("-output_file");
        command.add(output.toString());
        for (Path layer : layers) {
            command.add(layer.toString());
        }
        return command;
    }

    private List<Path> classpathResourceRoots() {
        List<String> configured = getClasspathResourceDirectories()
                .getOrElse(getResourceDirectories().get());
        List<Path> roots = new ArrayList<>();
        for (String directory : configured) {
            Path root = getProject().file(directory).toPath().toAbsolutePath().normalize();
            if (Files.isDirectory(root)) {
                roots.add(root);
            }
        }
        roots.sort(Comparator.comparingInt(Path::getNameCount).reversed());
        return roots;
    }
}
