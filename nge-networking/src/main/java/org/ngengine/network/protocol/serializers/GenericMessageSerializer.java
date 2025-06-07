/**
 * Copyright (c) 2025, Nostr Game Engine
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 * 
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 
 * 3. Neither the name of the copyright holder nor the names of its
 *    contributors may be used to endorse or promote products derived from
 *    this software without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 * 
 * Nostr Game Engine is a fork of the jMonkeyEngine, which is licensed under
 * the BSD 3-Clause License. The original jMonkeyEngine license is as follows:
 */
package org.ngengine.network.protocol.serializers;

import com.jme3.network.serializing.SerializerException;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.BufferOverflowException;
import java.nio.ByteBuffer;
import java.util.*;
import java.util.function.BiFunction;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.ngengine.network.protocol.GrowableByteBuffer;

/**
 * The field serializer is the default serializer used for custom class.
 *
 * @author Lars Wesselius, Nathan Sweet
 */
public class GenericMessageSerializer extends DynamicSerializer {

    private static final Logger log = Logger.getLogger(GenericMessageSerializer.class.getName());
    protected final BiFunction<Object, GrowableByteBuffer, Void> serialize;
    protected final BiFunction<ByteBuffer, Class<?>, Object> deserialize;

    public GenericMessageSerializer(
        BiFunction<Object, GrowableByteBuffer, Void> serialize,
        BiFunction<ByteBuffer, Class<?>, Object> deserialize
    ) {
        this.serialize = serialize;
        this.deserialize = deserialize;
    }

    protected void serialize(Object object, GrowableByteBuffer buffer) {
        this.serialize.apply(object, buffer);
    }

    protected Object deserialize(ByteBuffer data, Class<?> c) {
        return this.deserialize.apply(data, c);
    }

    @SuppressWarnings("unchecked")
    protected <T> Constructor<T> getConstructor(Class<?> clazz) {
        // See if the class has a public no-arg constructor
        try {
            return (Constructor<T>) clazz.getConstructor();
        } catch (NoSuchMethodException e) {
            //throw new RuntimeException( "Registration error: no-argument constructor not found on:" + clazz );
        }

        // See if it has a non-public no-arg constructor
        try {
            Constructor<?> ctor = clazz.getDeclaredConstructor();

            // Make sure we can call it later.
            ctor.setAccessible(true);
            return (Constructor<T>) ctor;
        } catch (NoSuchMethodException e) {}

        throw new RuntimeException("no-argument constructor not found on:" + clazz);
    }

    protected Collection<Field> getFields(Class<?> clazz) {
        List<Field> fields = new ArrayList<>();

        Class<?> processingClass = clazz;
        while (processingClass != Object.class) {
            Collections.addAll(fields, processingClass.getDeclaredFields());
            processingClass = processingClass.getSuperclass();
        }

        List<Field> cachedFields = new ArrayList<>(fields.size());
        for (Field field : fields) {
            int modifiers = field.getModifiers();
            if (Modifier.isTransient(modifiers)) continue;
            if (Modifier.isStatic(modifiers)) continue;
            if (field.isSynthetic()) continue;
            field.setAccessible(true);

            cachedFields.add(field);
        }

        Collections.sort(
            cachedFields,
            new Comparator<Field>() {
                @Override
                public int compare(Field o1, Field o2) {
                    return o1.getName().compareTo(o2.getName());
                }
            }
        );
        return cachedFields;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> T readObject(ByteBuffer data, Class<T> c) throws IOException {
        Collection<Field> fields = getFields(c);

        T object;
        try {
            Constructor<T> ctor = getConstructor(c);
            object = ctor.newInstance();
        } catch (Exception e) {
            throw new SerializerException("Error creating object of type:" + c, e);
        }

        for (Field savedField : fields) {
            try {
                Field field = savedField;
                if (log.isLoggable(Level.FINER)) {
                    log.log(Level.FINER, "Reading field:{0}", new Object[] { field });
                }
                Object value = deserialize(data, savedField.getType());
                field.set(object, value);
            } catch (Exception e) {
                throw new SerializerException("Error reading object", e);
            }
        }
        return object;
    }

    @Override
    public void writeObject(GrowableByteBuffer buffer, Object object) throws IOException {
        Collection<Field> fields = getFields(object.getClass());
        if (fields == null) throw new IOException("The " + object.getClass() + " is not registered" + " in the serializer!");

        for (Field field : fields) {
            Object val = null;
            try {
                val = field.get(object);
            } catch (IllegalAccessException e) {
                throw new SerializerException("Unable to access field:" + field + " on:" + object, e);
            }

            try {
                if (log.isLoggable(Level.FINER)) {
                    log.log(Level.FINER, "Writing field:{0}", new Object[] { field });
                }

                this.serialize(val, buffer);
            } catch (BufferOverflowException boe) {
                throw boe;
            } catch (Exception e) {
                throw new SerializerException("Error writing object for field:" + field, e);
            }
        }
    }
}
