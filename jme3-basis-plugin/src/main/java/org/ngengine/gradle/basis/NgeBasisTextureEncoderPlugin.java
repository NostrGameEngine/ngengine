package org.ngengine.gradle.basis;

import java.io.File;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import org.gradle.api.Project;
import org.gradle.api.plugins.JavaPlugin;
import org.gradle.api.tasks.Copy;
import org.gradle.api.tasks.SourceSetContainer;
import org.gradle.api.tasks.TaskProvider;
import org.ngengine.basis.gradle.BasisTextureEncoderExtension;
import org.ngengine.basis.gradle.BasisTextureEncoderPlugin;
import org.ngengine.basis.gradle.EncodeBasisTexturesTask;

/**
 * Extends the jBasis encoder with automatic TSX/TMX atlas texture arrays.
 */
public final class NgeBasisTextureEncoderPlugin extends BasisTextureEncoderPlugin {

    @Override
    protected Class<? extends BasisTextureEncoderExtension> getExtensionType() {
        return NgeBasisTextureEncoderExtension.class;
    }

    @Override
    protected void configureAdditionalEncoding(
            Project project,
            BasisTextureEncoderExtension baseExtension,
            TaskProvider<EncodeBasisTexturesTask> encodeTask) {
        NgeBasisTextureEncoderExtension extension = (NgeBasisTextureEncoderExtension) baseExtension;

        extension.getExcludedResourcePaths().addAll(project.provider(() -> {
            if (!extension.getHandleTiledTilesets().get()) {
                return Collections.emptyList();
            }
            return TiledAtlasScanner.scan(resourceRoots(project, extension)).stream()
                    .map(TiledAtlas::getResourcePath)
                    .collect(Collectors.toList());
        }));

        TaskProvider<EncodeTiledBasisTextureArraysTask> tiledTask = project.getTasks().register(
                "encodeTiledBasisTextureArrays",
                EncodeTiledBasisTextureArraysTask.class,
                task -> {
                    task.setGroup("build");
                    task.setDescription("Encodes Tiled atlas resources as mipmapped Basis texture arrays.");
                    task.getResourceDirectories().set(extension.getResourceDirectories());
                    task.getBasisuArguments().set(extension.getBasisuArguments());
                    task.getBasisuExecutable().set(extension.getBasisuExecutable());
                    task.getHandleTiledTilesets().set(extension.getHandleTiledTilesets());
                    task.getOutputDirectory().set(encodeTask.flatMap(EncodeBasisTexturesTask::getOutputDirectory));
                    task.setOrdinaryEncoder(encodeTask.get());
                    task.dependsOn(encodeTask);
                });

        project.getPlugins().withType(JavaPlugin.class, plugin -> {
            SourceSetContainer sourceSets = project.getExtensions().getByType(SourceSetContainer.class);
            sourceSets.named("main", sourceSet -> {
                List<String> resourceDirectories = sourceSet.getResources().getSrcDirs().stream()
                        .map(File::getPath)
                        .collect(Collectors.toList());
                tiledTask.configure(task -> {
                    task.getClasspathResourceDirectories().set(resourceDirectories);
                    task.getResourceFiles().from(extension.getResourceDirectories().map(directories ->
                            directories.stream()
                                    .map(project::file)
                                    .collect(Collectors.toList())));
                });
            });
            project.getTasks().named("processResources", Copy.class, task -> task.dependsOn(tiledTask));
        });
    }

    private static List<Path> resourceRoots(
            Project project,
            NgeBasisTextureEncoderExtension extension) {
        return extension.getResourceDirectories().get().stream()
                .map(project::file)
                .map(File::toPath)
                .collect(Collectors.toList());
    }
}
