package org.ngengine.web;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.teavm.classlib.ReflectionContext;
import org.teavm.model.AccessLevel;
import org.teavm.model.ClassHolder;
import org.teavm.model.ClassReaderSource;
import org.teavm.model.MethodHolder;
import org.teavm.model.ValueType;

class NgeReflectionSupplierTest {

    @TempDir
    Path temporaryDirectory;

    @Test
    void readsApplicationMetadataFromAnAdditionalClassLoader() throws Exception {
        Path metadata = temporaryDirectory.resolve("META-INF/ngengine/reflection-classes.txt");
        Files.createDirectories(metadata.getParent());
        Files.writeString(
                metadata,
                "example.GameComponent\njava.lang.String\n",
                StandardCharsets.UTF_8);

        try (URLClassLoader applicationLoader = new URLClassLoader(
                new java.net.URL[] {temporaryDirectory.toUri().toURL()},
                null)) {
            NgeReflectionSupplier supplier = new NgeReflectionSupplier(applicationLoader);

            assertTrue(supplier.isClassFoundByName(null, "example.GameComponent"));
            assertFalse(supplier.isClassFoundByName(null, "java.lang.String"));
        }
    }

    @Test
    void readsApplicationMetadataFromTheTeaVmReflectionContext() throws Exception {
        Path metadata = temporaryDirectory.resolve("META-INF/ngengine/reflection-classes.txt");
        Files.createDirectories(metadata.getParent());
        Files.writeString(metadata, "example.MapOnlyComponent\n", StandardCharsets.UTF_8);

        try (URLClassLoader applicationLoader = new URLClassLoader(
                new java.net.URL[] {temporaryDirectory.toUri().toURL()}, null)) {
            NgeReflectionSupplier supplier = new NgeReflectionSupplier(new ClassLoader[0]);
            ReflectionContext context = new ReflectionContext() {
                @Override
                public ClassLoader getClassLoader() {
                    return applicationLoader;
                }

                @Override
                public ClassReaderSource getClassSource() {
                    return className -> null;
                }
            };

            assertTrue(supplier.isClassFoundByName(context, "example.MapOnlyComponent"));
        }
    }

    @Test
    void exposesOnlyMembersDeclaredByTheSharedDiscovery() throws Exception {
        String className = "example.CloneTarget";
        Path metadata = temporaryDirectory.resolve("META-INF/ngengine/reflection-classes.txt");
        Files.createDirectories(metadata.getParent());
        Files.writeString(metadata, className + "\n", StandardCharsets.UTF_8);
        Files.writeString(
                temporaryDirectory.resolve("META-INF/ngengine/reflection-metadata.txt"),
                className + "\tC\tclone()\n",
                StandardCharsets.UTF_8);

        ClassHolder target = new ClassHolder(className);
        MethodHolder constructor = new MethodHolder("<init>", ValueType.VOID);
        constructor.setLevel(AccessLevel.PUBLIC);
        target.addMethod(constructor);
        MethodHolder clone = new MethodHolder("clone", ValueType.object(className));
        clone.setLevel(AccessLevel.PUBLIC);
        target.addMethod(clone);
        MethodHolder unrelated = new MethodHolder("update", ValueType.VOID);
        unrelated.setLevel(AccessLevel.PUBLIC);
        target.addMethod(unrelated);

        try (URLClassLoader applicationLoader = new URLClassLoader(
                new java.net.URL[] {temporaryDirectory.toUri().toURL()}, null)) {
            NgeReflectionSupplier supplier = new NgeReflectionSupplier(applicationLoader);
            ReflectionContext context = new ReflectionContext() {
                @Override
                public ClassLoader getClassLoader() {
                    return applicationLoader;
                }

                @Override
                public ClassReaderSource getClassSource() {
                    return name -> className.equals(name) ? target : null;
                }
            };

            java.util.Collection<org.teavm.model.MethodDescriptor> methods =
                    supplier.getAccessibleMethods(context, className);
            assertTrue(methods.contains(constructor.getDescriptor()));
            assertTrue(methods.contains(clone.getDescriptor()));
            assertFalse(methods.contains(unrelated.getDescriptor()));
        }
    }
}
