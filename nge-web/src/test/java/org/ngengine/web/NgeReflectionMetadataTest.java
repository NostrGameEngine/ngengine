package org.ngengine.web;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.LinkedHashSet;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class NgeReflectionMetadataTest {

    @TempDir
    Path temporaryDirectory;

    @Test
    void mergesApplicationAndDependencyMetadataWithoutJdkTypes() throws Exception {
        Path firstRoot = temporaryDirectory.resolve("first");
        Path secondRoot = temporaryDirectory.resolve("second");
        writeMetadata(firstRoot, NgeReflectionMetadata.CLASSES_RESOURCE,
                "example.Component\njava.lang.String\n");
        writeMetadata(secondRoot, NgeReflectionMetadata.CLASSES_RESOURCE,
                "example.Message\nexample.Component\n");

        try (URLClassLoader firstLoader = new URLClassLoader(
                        new java.net.URL[] {firstRoot.toUri().toURL()}, null);
                URLClassLoader secondLoader = new URLClassLoader(
                        new java.net.URL[] {secondRoot.toUri().toURL()}, null)) {
            assertEquals(
                    new LinkedHashSet<>(Arrays.asList("example.Component", "example.Message")),
                    NgeReflectionMetadata.loadClasses(firstLoader, secondLoader));
        }
    }

    @Test
    void keepsReachabilityRootsSeparateFromTheCompleteReflectionList() throws Exception {
        writeMetadata(temporaryDirectory, NgeReflectionMetadata.CLASSES_RESOURCE,
                "example.Component\nexample.Message\n");
        writeMetadata(temporaryDirectory, NgeReflectionMetadata.ROOTS_RESOURCE,
                "example.Component\n");

        try (URLClassLoader loader = new URLClassLoader(
                new java.net.URL[] {temporaryDirectory.toUri().toURL()}, null)) {
            assertEquals(
                    new LinkedHashSet<>(Arrays.asList("example.Component")),
                    NgeReflectionMetadata.loadRoots(loader));
        }
    }

    private static void writeMetadata(Path root, String resource, String contents) throws Exception {
        Path metadata = root.resolve(resource);
        Files.createDirectories(metadata.getParent());
        Files.writeString(metadata, contents, StandardCharsets.UTF_8);
    }
}
