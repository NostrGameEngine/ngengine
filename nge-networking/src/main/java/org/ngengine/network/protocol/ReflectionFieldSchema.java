package org.ngengine.network.protocol;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Shared reflection schema for field-based serializers/diff logic.
 * The field order and filtering rules must stay stable across writers/readers.
 */
public final class ReflectionFieldSchema {

    public static final class Schema {
        private final Constructor<?> constructor;
        private final List<Field> fields;

        private Schema(Constructor<?> constructor, List<Field> fields) {
            this.constructor = constructor;
            this.fields = fields;
        }

        public Constructor<?> constructor() {
            return constructor;
        }

        public List<Field> fields() {
            return fields;
        }
    }

    private static final Map<Class<?>, Schema> CACHE = new ConcurrentHashMap<>();

    private ReflectionFieldSchema() {
    }

    public static Schema getSchema(Class<?> clazz) {
        return CACHE.computeIfAbsent(clazz, ReflectionFieldSchema::buildSchema);
    }

    public static <T> T newInstance(Class<T> clazz) {
        try {
            @SuppressWarnings("unchecked")
            Constructor<T> ctor = (Constructor<T>) getSchema(clazz).constructor();
            return ctor.newInstance();
        } catch (Exception e) {
            throw new RuntimeException("Error creating object of type:" + clazz, e);
        }
    }

    private static Schema buildSchema(Class<?> clazz) {
        Constructor<?> ctor = findConstructor(clazz);
        List<Field> fields = findSerializableFields(clazz);
        return new Schema(ctor, fields);
    }

    private static Constructor<?> findConstructor(Class<?> clazz) {
        try {
            return clazz.getConstructor();
        } catch (NoSuchMethodException e) {
            // try non-public no-arg constructor
        }

        try {
            Constructor<?> ctor = clazz.getDeclaredConstructor();
            ctor.setAccessible(true);
            return ctor;
        } catch (NoSuchMethodException e) {
            throw new RuntimeException("no-argument constructor not found on:" + clazz);
        }
    }

    private static List<Field> findSerializableFields(Class<?> clazz) {
        List<Field> all = new ArrayList<>();

        Class<?> processingClass = clazz;
        while (processingClass != Object.class) {
            Collections.addAll(all, processingClass.getDeclaredFields());
            processingClass = processingClass.getSuperclass();
        }

        List<Field> filtered = new ArrayList<>(all.size());
        for (Field field : all) {
            int modifiers = field.getModifiers();
            if (Modifier.isTransient(modifiers)) continue;
            if (Modifier.isStatic(modifiers)) continue;
            if (field.isSynthetic()) continue;
            field.setAccessible(true);
            filtered.add(field);
        }

        filtered.sort(Comparator.comparing(Field::getName));
        return Collections.unmodifiableList(filtered);
    }
}

