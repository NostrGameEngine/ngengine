/**
 * Copyright (c) 2025-2026, Nostr Game Engine
 * All rights reserved.
 */
package org.ngengine.web;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import org.teavm.classlib.ReflectionContext;
import org.teavm.classlib.ReflectionSupplier;
import org.teavm.model.ClassReader;
import org.teavm.model.FieldReader;
import org.teavm.model.MethodDescriptor;
import org.teavm.model.MethodReader;
import org.teavm.model.ValueType;

/**
 * Reuses the reflection class lists produced by the NGE Native Image Gradle
 * plugin when compiling the same application with TeaVM.
 */
public final class NgeReflectionSupplier implements ReflectionSupplier {
    private final Set<String> reflectedClasses = new HashSet<>();
    private final Map<String, NgeReflectionMetadata.ReflectionProfile> reflectionProfiles =
            new java.util.HashMap<>();
    private final Set<ClassLoader> scannedClassLoaders =
            Collections.newSetFromMap(new IdentityHashMap<>());

    public NgeReflectionSupplier() {
        this(candidateClassLoaders());
    }

    NgeReflectionSupplier(ClassLoader... classLoaders) {
        for (ClassLoader classLoader : classLoaders) {
            loadReflectionClasses(classLoader);
        }
    }

    private synchronized void loadReflectionClasses(ClassLoader classLoader) {
        if (classLoader == null || !scannedClassLoaders.add(classLoader)) {
            return;
        }
        reflectedClasses.addAll(NgeReflectionMetadata.loadClasses(classLoader));
        NgeReflectionMetadata.loadProfiles(classLoader).forEach((className, profile) ->
                reflectionProfiles.merge(className, profile,
                        NgeReflectionSupplier::mergeProfiles));
    }

    private static ClassLoader[] candidateClassLoaders() {
        LinkedHashSet<ClassLoader> loaders = new LinkedHashSet<>();
        loaders.add(Thread.currentThread().getContextClassLoader());
        loaders.add(NgeReflectionSupplier.class.getClassLoader());
        loaders.add(ClassLoader.getSystemClassLoader());
        return loaders.toArray(new ClassLoader[0]);
    }

    @Override
    public Collection<String> getAccessibleFields(ReflectionContext context, String className) {
        loadContextMetadata(context);
        if (!reflectedClasses.contains(className)) {
            return Collections.emptyList();
        }
        ClassReader type = context.getClassSource().get(className);
        if (type == null) {
            return Collections.emptyList();
        }

        NgeReflectionMetadata.ReflectionProfile profile = reflectionProfiles.get(className);
        if (profile != null && !profile.hasDeclaredFields()) {
            return Collections.emptyList();
        }
        Set<String> fields = new HashSet<>();
        for (FieldReader field : type.getFields()) {
            fields.add(field.getName());
        }
        return fields;
    }

    @Override
    public Collection<MethodDescriptor> getAccessibleMethods(
            ReflectionContext context, String className) {
        loadContextMetadata(context);
        if (!reflectedClasses.contains(className)) {
            return Collections.emptyList();
        }
        ClassReader type = context.getClassSource().get(className);
        if (type == null) {
            return Collections.emptyList();
        }

        NgeReflectionMetadata.ReflectionProfile profile = reflectionProfiles.get(className);
        Set<MethodDescriptor> methods = new HashSet<>();
        for (MethodReader method : type.getMethods()) {
            if (profile == null || isAccessible(profile, method)) {
                methods.add(method.getDescriptor());
            }
        }
        return methods;
    }

    @Override
    public boolean isClassFoundByName(ReflectionContext context, String className) {
        loadContextMetadata(context);
        return reflectedClasses.contains(className);
    }

    private void loadContextMetadata(ReflectionContext context) {
        if (context != null) {
            loadReflectionClasses(context.getClassLoader());
        }
    }

    private static boolean isAccessible(
            NgeReflectionMetadata.ReflectionProfile profile,
            MethodReader method) {
        if ("<init>".equals(method.getName())) {
            return profile.hasDeclaredConstructors();
        }
        if (profile.hasDeclaredMethods()
                || (profile.hasPublicMethods()
                        && method.getLevel() == org.teavm.model.AccessLevel.PUBLIC)) {
            return true;
        }
        return profile.hasExplicitMethod(methodSignature(method.getDescriptor()));
    }

    private static String methodSignature(MethodDescriptor descriptor) {
        StringBuilder result = new StringBuilder(descriptor.getName()).append('(');
        for (int index = 0; index < descriptor.parameterCount(); index++) {
            if (index > 0) {
                result.append(',');
            }
            result.append(javaTypeName(descriptor.parameterType(index)));
        }
        return result.append(')').toString();
    }

    private static String javaTypeName(ValueType type) {
        if (type instanceof ValueType.Object) {
            return ((ValueType.Object) type).getClassName();
        }
        if (type instanceof ValueType.Array) {
            return javaTypeName(((ValueType.Array) type).getItemType()) + "[]";
        }
        if (type instanceof ValueType.Primitive) {
            switch (((ValueType.Primitive) type).getKind()) {
                case BOOLEAN:
                    return "boolean";
                case BYTE:
                    return "byte";
                case SHORT:
                    return "short";
                case INTEGER:
                    return "int";
                case LONG:
                    return "long";
                case FLOAT:
                    return "float";
                case CHARACTER:
                    return "char";
                case DOUBLE:
                    return "double";
                default:
                    throw new IllegalArgumentException("Unsupported primitive type " + type);
            }
        }
        return "void";
    }

    private static NgeReflectionMetadata.ReflectionProfile mergeProfiles(
            NgeReflectionMetadata.ReflectionProfile first,
            NgeReflectionMetadata.ReflectionProfile second) {
        first.merge(second);
        return first;
    }

}
