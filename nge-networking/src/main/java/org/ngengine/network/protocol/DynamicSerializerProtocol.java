/**
 * Copyright (c) 2025-2026, Nostr Game Engine
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
 * the BSD 3-Clause License. 
 */

package org.ngengine.network.protocol;

import com.jme3.math.ColorRGBA;
import com.jme3.math.Matrix3f;
import com.jme3.math.Matrix4f;
import com.jme3.math.Quaternion;
import com.jme3.math.Transform;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.jme3.math.Vector4f;
import com.jme3.network.Message;
import com.jme3.network.base.MessageBuffer;
import com.jme3.network.base.MessageProtocol;
import com.jme3.network.base.protocol.LazyMessageBuffer;
import com.jme3.network.serializing.Serializable;
import com.jme3.network.serializing.Serializer;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Array;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.IdentityHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.Vector;
import java.util.WeakHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.jar.Attributes;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.ngengine.network.protocol.messages.ByteDataMessage;
import org.ngengine.network.protocol.messages.ClassRegistrationAckMessage;
import org.ngengine.network.protocol.messages.CompressedMessage;
import org.ngengine.network.protocol.messages.TextDataMessage;
import org.ngengine.network.protocol.serializers.BooleanSerializer;
import org.ngengine.network.protocol.serializers.BigIntegerSerializer;
import org.ngengine.network.protocol.serializers.ByteBufferSerializer;
import org.ngengine.network.protocol.serializers.ByteMessageSerializer;
import org.ngengine.network.protocol.serializers.CharSerializer;
import org.ngengine.network.protocol.serializers.CollectionSerializer;
import org.ngengine.network.protocol.serializers.ColorRGBASerializer;
import org.ngengine.network.protocol.serializers.CompressedMessageSerializer;
import org.ngengine.network.protocol.serializers.DateSerializer;
import org.ngengine.network.protocol.serializers.DurationSerializer;
import org.ngengine.network.protocol.serializers.DynamicSerializer;
import org.ngengine.network.protocol.serializers.EnumSerializer;
import org.ngengine.network.protocol.serializers.GenericMessageSerializer;
import org.ngengine.network.protocol.serializers.InstantSerializer;
import org.ngengine.network.protocol.serializers.MapSerializer;
import org.ngengine.network.protocol.serializers.Matrix3fSerializer;
import org.ngengine.network.protocol.serializers.Matrix4fSerializer;
import org.ngengine.network.protocol.serializers.NostrKeyPairSerializer;
import org.ngengine.network.protocol.serializers.NostrPrivateKeySerializer;
import org.ngengine.network.protocol.serializers.NostrPublicKeySerializer;
import org.ngengine.network.protocol.serializers.NumberSerializer;
import org.ngengine.network.protocol.serializers.QuaternionSerializer;
import org.ngengine.network.protocol.serializers.StringSerializer;
import org.ngengine.network.protocol.serializers.TextMessageSerializer;
import org.ngengine.network.protocol.serializers.TransformSerializer;
import org.ngengine.network.protocol.serializers.Vector2fSerializer;
import org.ngengine.network.protocol.serializers.Vector3fSerializer;
import org.ngengine.network.protocol.serializers.Vector4fSerializer;
import org.ngengine.nostr4j.event.SignedNostrEvent;
import org.ngengine.nostr4j.event.UnsignedNostrEvent;
import org.ngengine.nostr4j.keypair.NostrKeyPair;
import org.ngengine.nostr4j.keypair.NostrPrivateKey;
import org.ngengine.nostr4j.keypair.NostrPublicKey;

/**
 * Message protocol used by NGE P2P networking.
 *
 * <p>Envelope format is:
 * <pre>
 * [classPathLength varint signed] [-1 means null]
 * [optional class path bytes]
 * [classId varint signed]
 * [bodyLength uint32]
 * [body bytes]
 * </pre>
 *
 * <p>Body handling is split in two paths:
 * <ul>
 * <li>Regular path: body starts with {@code DIFF_BODY_MODE_FULL} and then serializer payload.</li>
 * <li>Diff runtime path (for {@link DiffableMessage}): body starts with a runtime marker and a varint
 * header containing mode/lane/group/packet references, then full or diff payload.</li>
 * </ul>
 *
 * <p>Diff reconstruction model:
 * <ul>
 * <li>Reliable FULL initializes or refreshes the base snapshot for a group.</li>
 * <li>Reliable DIFF applies on a reliable base and promotes the reconstructed state as new base.</li>
 * <li>Unreliable DIFF applies on the current reliable base and is never promoted.</li>
 * <li>Unreliable base miss is silently dropped, reliable base miss is a strict protocol failure.</li>
 * </ul>
 *
 * <p>Diff retention is asymmetric by design:
 * sender keeps shorter TTLs, receiver keeps longer TTLs, reducing base-miss probability while still
 * evicting stale groups/snapshots.
 */
public class DynamicSerializerProtocol implements MessageProtocol {
    private static final Logger logger = Logger.getLogger(DynamicSerializerProtocol.class.getName());
    private static final ByteBuffer EMPTY_MESSAGE_BUFFER = ByteBuffer.allocate(0).asReadOnlyBuffer();
    private static final long DIFF_BODY_MODE_FULL = 0L;

    protected static class RegisteredSerializer {

        private final Class<?> cls;
        private final Serializer serializer;

        protected RegisteredSerializer(Class<?> cls, Serializer serializer) {
            this.cls = cls;
            this.serializer = serializer;
        }

        public Class<?> getType() {
            return cls;
        }

        public boolean isSerializerFor(Class<?> cls) {
            return this.cls.isAssignableFrom(cls);
        }

        public Serializer get() {
            return serializer;
        }
    }

    private final Map<Class<?>, Long> classXid = new HashMap<>();
    private final Map<Long, Class<?>> idXClass = new HashMap<>();
    private final Set<Long> pendingAcks = new HashSet<>();
    private final AtomicLong classIdCounter = new AtomicLong(0);
    private final long classIdStep;
    private final Map<Class<?>, Serializer> serializerCache = new HashMap<>();
    private final ThreadLocal<ByteBuffer> tmpBuffer = ThreadLocal.withInitial(() -> ByteBuffer.allocate(32767));
    private final DiffRuntime diffRuntime = new DiffRuntime(this);

    private final BiFunction<Object, GrowableByteBuffer, Void> serializeFun = (obj, bbf) -> {
        try {
            this.serialize(obj, bbf, false);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return null;
    };
    private final BiFunction<ByteBuffer, Class<?>, Object> deserializeFun = (bbf, cls) -> {
        try {
            return this.deserializeInternal(bbf, cls, false);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    };

    private final List<RegisteredSerializer> serializers = new ArrayList<>();
    private final Collection<Class<?>> serializables = new ArrayList<>();
    private final Collection<Class> serializablesAnnotation = new ArrayList<>();
    private final boolean spidermonkeyCompatible;
    private final boolean reliableFullCheckpointEnabled;
    private boolean forceSpidermonkeyStaticBuffer = false;
    private final Consumer<Long> onClassRegistered;
    /**
     * Creates a new DynamicSerializerProtocol that automatically handles class registration and
     * serialization.
     *
     *
     * @param strict
     *            set if the serializer should be strict (safer) or unstrict (FAFO)
     */
    public DynamicSerializerProtocol(boolean spidermonkeyCompatible, Consumer<Long> onClassRegistered, long initialLastId) {
        this(spidermonkeyCompatible, onClassRegistered, initialLastId, true);
    }

    public DynamicSerializerProtocol(
        boolean spidermonkeyCompatible,
        Consumer<Long> onClassRegistered,
        long initialLastId,
        boolean reliableFullCheckpointEnabled
    ) {
        this.spidermonkeyCompatible = spidermonkeyCompatible;
        this.reliableFullCheckpointEnabled = reliableFullCheckpointEnabled;
        this.onClassRegistered = onClassRegistered;
        this.classIdStep = initialLastId < 0 ? -1L : 1L;
        this.classIdCounter.set(initialLastId - this.classIdStep);
        registerDefaultSerializers();
        registerDefaultSerializables(spidermonkeyCompatible);
    }

    protected void registerDefaultSerializables(boolean spidermonkeyCompatible) {
        registerSerializable(
            Vector.class,
            Vector2f.class,
            Vector3f.class,
            Vector4f.class,
            Transform.class,
            ColorRGBA.class,
            Matrix3f.class,
            Matrix4f.class,
            Date.class,
            Instant.class,
            Duration.class,
            NostrPublicKey.class,
            NostrPrivateKey.class,
            NostrKeyPair.class,
            UnsignedNostrEvent.class,
            SignedNostrEvent.class,
            HashMap.class,
            WeakHashMap.class,
            IdentityHashMap.class,
            Hashtable.class,
            TreeMap.class,
            HashSet.class,
            ArrayList.class,
            LinkedList.class,
            LinkedHashSet.class,
            TreeSet.class,
            Attributes.class,
            Integer.class,
            Long.class,
            Float.class,
            Double.class,
            String.class,
            Short.class,
            Boolean.class,
            Byte.class,
            Character.class,
            int.class,
            long.class,
            float.class,
            double.class,
            boolean.class,
            byte.class,
            char.class,
            short.class,
            BigInteger.class
        );
        registerSerializableAnnotation(NetworkSafe.class);

        if (spidermonkeyCompatible) registerSerializableAnnotation(Serializable.class);
    }

    protected void registerDefaultSerializers() {
        // bottom => highest priority

        // lists
        registerSerializer(Collection.class, new CollectionSerializer(serializeFun, deserializeFun));

        // maps
        registerSerializer(Map.class, new MapSerializer(serializeFun, deserializeFun));

        // primitive wrappers
        registerSerializer(Boolean.class, new BooleanSerializer());
        registerSerializer(Byte.class, new NumberSerializer());
        registerSerializer(Character.class, new CharSerializer());
        registerSerializer(Short.class, new NumberSerializer());
        registerSerializer(Integer.class, new NumberSerializer());
        registerSerializer(Long.class, new NumberSerializer());
        registerSerializer(Float.class, new NumberSerializer());
        registerSerializer(Double.class, new NumberSerializer());
        registerSerializer(BigInteger.class, new BigIntegerSerializer());
        registerSerializer(String.class, new StringSerializer());

        // primitives
        registerSerializer(boolean.class, new BooleanSerializer());
        registerSerializer(byte.class, new NumberSerializer());
        registerSerializer(char.class, new CharSerializer());
        registerSerializer(short.class, new NumberSerializer());
        registerSerializer(int.class, new NumberSerializer());
        registerSerializer(long.class, new NumberSerializer());
        registerSerializer(float.class, new NumberSerializer());
        registerSerializer(double.class, new NumberSerializer());

        // other java stuff enum
        registerSerializer(ByteBuffer.class, new ByteBufferSerializer());
        registerSerializer(Enum.class, new EnumSerializer());
        registerSerializer(Date.class, new DateSerializer());
        registerSerializer(Instant.class, new InstantSerializer());
        registerSerializer(Duration.class, new DurationSerializer());

        // nge stuff
        registerSerializer(NostrPublicKey.class, new NostrPublicKeySerializer());
        registerSerializer(NostrPrivateKey.class, new NostrPrivateKeySerializer());
        registerSerializer(NostrKeyPair.class, new NostrKeyPairSerializer());

        // jme3 stuff
        registerSerializer(Vector4f.class, new Vector4fSerializer());
        registerSerializer(Vector3f.class, new Vector3fSerializer());
        registerSerializer(Vector2f.class, new Vector2fSerializer());
        registerSerializer(ColorRGBA.class, new ColorRGBASerializer());
        registerSerializer(Matrix3f.class, new Matrix3fSerializer());
        registerSerializer(Matrix4f.class, new Matrix4fSerializer());
        registerSerializer(Quaternion.class, new QuaternionSerializer());
        registerSerializer(Transform.class, new TransformSerializer());

        // messages
        registerSerializer(Message.class, new GenericMessageSerializer(serializeFun, deserializeFun));
        registerSerializer(TextDataMessage.class, new TextMessageSerializer());
        registerSerializer(ByteDataMessage.class, new ByteMessageSerializer());
        registerSerializer(CompressedMessage.class, new CompressedMessageSerializer(serializeFun, deserializeFun));

        classXid.put(ClassRegistrationAckMessage.class, 0L);
        idXClass.put(0L, ClassRegistrationAckMessage.class);
    }


    public void setLastId(long lastId) {
        this.classIdCounter.set(lastId);
    }
    

    /**
     * Force use of static buffer (old behavior) even if the serialize supports dynamic buffers. Used mostly
     * for debugging.
     *
     * @param forceStatic
     */
    public void setForceStaticBuffer(boolean forceStatic) {
        this.forceSpidermonkeyStaticBuffer = forceStatic;
    }

    public void registerSerializer(Class<?> cls, Serializer serializer) {
        Objects.requireNonNull(cls, "Class cannot be null");
        Objects.requireNonNull(serializer, "Serializer cannot be null");
        serializers.add(new RegisteredSerializer(cls, serializer));
    }

    public void registerSerializable(Class<?>... clss) {
        for (Class<?> cls : clss) {
            Objects.requireNonNull(cls, "Class cannot be null");
            serializables.add(cls);
        }
    }

    public void registerSerializableAnnotation(Class<?> cls) {
        Objects.requireNonNull(cls, "Class cannot be null");
        if (!cls.isAnnotation()) {
            throw new IllegalArgumentException("Class " + cls.getName() + " is not an annotation");
        }
        serializablesAnnotation.add(cls);
    }

    protected void checkIsSerializable(Class<?> messageClass, boolean messageOnly) {
        for (Class<? extends Annotation> serializableAnnotation : this.serializablesAnnotation) {
            if (messageClass.isAnnotationPresent(serializableAnnotation)) {
                return;
            }
        }

        if (Message.class.isAssignableFrom(messageClass)) {
            throw new RuntimeException(
                "Message " +
                messageClass.getName() +
                " is not whitelisted. Please mark it with the  org.ngengine.network.protocol.NetworkSafe annotation."
            );
        }

        if (!messageOnly) {
            for (Class<?> cls : this.serializables) {
                if (cls == messageClass || cls.isAssignableFrom(messageClass)) {
                    return;
                }
            }
        }

        for (RegisteredSerializer reg : serializers) {
            if (reg.isSerializerFor(messageClass)) {
                return;
            }
        }

        throw new RuntimeException(
            "Class " + messageClass.getName() + " is not serializable. Please register a serializer for this class."
        );
    }

    protected Serializer getBestSerializerFor(Class<?> cls) {
        Serializer cached = serializerCache.get(cls);
        if (cached != null) return cached;
        for (int i = serializers.size() - 1; i >= 0; i--) {
            RegisteredSerializer reg = serializers.get(i);
            if (reg.isSerializerFor(cls)) {
                serializerCache.put(cls, reg.get());
                return reg.get();
            }
        }
        throw new RuntimeException("No serializer found for class: " + cls.getName());
    }

    protected Object swapInternals(Object obj) {
        if (obj.getClass().getName().equals("java.util.Arrays$ArrayList")) {
            obj = new ArrayList((Collection) obj);
        }

        if (obj == Collections.EMPTY_LIST) {
            obj = new ArrayList<>();
        }

        if (obj == Collections.EMPTY_MAP) {
            obj = new HashMap<>();
        }

        if (obj == Collections.EMPTY_SET) {
            obj = new HashSet<>();
        }

        if (obj instanceof Map
            && !(obj instanceof HashMap)
            && !(obj instanceof WeakHashMap)
            && !(obj instanceof IdentityHashMap)
            && !(obj instanceof Hashtable)
            && !(obj instanceof TreeMap)) {
            obj = new HashMap<>((Map<?, ?>) obj);
        } else if (obj instanceof List
            && !(obj instanceof ArrayList)
            && !(obj instanceof LinkedList)
            && !(obj instanceof Vector)) {
            obj = new ArrayList<>((Collection<?>) obj);
        } else if (obj instanceof Set
            && !(obj instanceof HashSet)
            && !(obj instanceof LinkedHashSet)
            && !(obj instanceof TreeSet)) {
            obj = new LinkedHashSet<>((Collection<?>) obj);
        } else if (obj instanceof Collection
            && !(obj instanceof ArrayList)
            && !(obj instanceof LinkedList)
            && !(obj instanceof Vector)
            && !(obj instanceof HashSet)
            && !(obj instanceof LinkedHashSet)
            && !(obj instanceof TreeSet)) {
            obj = new ArrayList<>((Collection<?>) obj);
        }

        return obj;
    }

    private Object normalizeForSerialization(Object obj) {
        if (obj != null && obj.getClass().isArray()) {
            ArrayList<Object> list = new ArrayList<>();
            for (int i = 0; i < Array.getLength(obj); i++) {
                list.add(Array.get(obj, i));
            }
            obj = list;
        }
        return swapInternals(obj);
    }

    private static final class WriteEnvelopeResult {
        private final int bodyLengthPos;
        private final int beforeBodyPos;

        private WriteEnvelopeResult(int bodyLengthPos, int beforeBodyPos) {
            this.bodyLengthPos = bodyLengthPos;
            this.beforeBodyPos = beforeBodyPos;
        }
    }

    private WriteEnvelopeResult writeEnvelopeHeader(Object normalizedObj, GrowableByteBuffer buffer, boolean messageOnly)
        throws IOException {
        Class<?> messageClass = normalizedObj.getClass();
        checkIsSerializable(messageClass, messageOnly);

        Long id = classXid.get(messageClass);
        if (id == null) {
            id = allocateNextClassId();
            classXid.put(messageClass, id);
            idXClass.put(id, messageClass);
            pendingAcks.add(id);
        }

        boolean registerClass = pendingAcks.contains(id);
        if (registerClass) {
            logger.finer("Request registration for class " + messageClass.getName() + " with id " + id);
            byte classPath[] = messageClass.getName().getBytes(StandardCharsets.UTF_8);
            VarInt.encodeSigned(classPath.length, buffer);
            buffer.put(classPath);
        } else {
            VarInt.encodeSigned(0, buffer);
        }

        VarInt.encodeSigned(id, buffer);

        int bodyLengthPos = buffer.position();
        buffer.putInt(0);
        int beforeBodyPos = buffer.position();
        return new WriteEnvelopeResult(bodyLengthPos, beforeBodyPos);
    }

    private void finalizeBodyLength(GrowableByteBuffer buffer, WriteEnvelopeResult header) throws IOException {
        int lastPos = buffer.position();
        long bodyLength = lastPos - header.beforeBodyPos;
        if (bodyLength > 0xFFFFFFFFL) {
            throw new IOException("Serialized body too large: " + bodyLength + " bytes (max 4294967295)");
        }

        buffer.position(header.bodyLengthPos);
        buffer.putInt((int) bodyLength);
        buffer.position(lastPos);
    }

    private void writeBodyWithSerializer(Object obj, Serializer serializer, GrowableByteBuffer buffer) throws IOException {
        if (serializer instanceof DynamicSerializer && !this.forceSpidermonkeyStaticBuffer) {
            ((DynamicSerializer) serializer).writeObject(buffer, obj);
        } else if (spidermonkeyCompatible) {
            ByteBuffer bbf = tmpBuffer.get();
            synchronized (bbf) {
                if (bbf != buffer.getBuffer()) {
                    bbf.clear();
                    serializer.writeObject(bbf, obj);
                    bbf.flip();
                    buffer.put(bbf);
                } else {
                    serializer.writeObject(bbf, obj);
                }
            }
        } else {
            throw new IOException(
                "Serializer " +
                serializer.getClass().getName() +
                " does not support dynamic buffers. Please register a serializer for this class."
            );
        }
    }

    protected synchronized void serialize(Object obj, GrowableByteBuffer buffer, boolean messageOnly) throws IOException {
        if (obj == null) { // -1 = null
            VarInt.encodeSigned(-1, buffer);
            return;
        }
        Object normalized = normalizeForSerialization(obj);
        WriteEnvelopeResult header = writeEnvelopeHeader(normalized, buffer, messageOnly);
        Serializer serializer = getBestSerializerFor(normalized.getClass());
        VarInt.encodeUnsigned(DIFF_BODY_MODE_FULL, buffer);
        writeBodyWithSerializer(normalized, serializer, buffer);
        finalizeBodyLength(buffer, header);
    }

    public synchronized void markClassRegistered(long id){
        pendingAcks.remove(id);
    }

    private long allocateNextClassId() {
        long id;
        do {
            id = classIdCounter.addAndGet(classIdStep);
        } while (idXClass.containsKey(id));
        return id;
    }

    Serializer bestSerializer(Class<?> cls) {
        return getBestSerializerFor(cls);
    }

    Object normalizeForDiff(Object obj) {
        return normalizeForSerialization(obj);
    }

    long nowMillis() {
        return System.currentTimeMillis();
    }

    boolean logEnabled(Level level) {
        return logger.isLoggable(level);
    }

    boolean isReliableFullCheckpointEnabled() {
        return reliableFullCheckpointEnabled;
    }

    void logFinest(String msg) {
        logger.finest(msg);
    }

    void writeBodyWithSerializerBridge(Object obj, Serializer serializer, GrowableByteBuffer output) throws IOException {
        writeBodyWithSerializer(obj, serializer, output);
    }

    void writeEnvelopedBody(Object obj, GrowableByteBuffer output, boolean messageOnly, ByteBuffer body) throws IOException {
        Object normalized = normalizeForSerialization(obj);
        WriteEnvelopeResult header = writeEnvelopeHeader(normalized, output, messageOnly);
        ByteBuffer bodyCopy = body.duplicate();
        bodyCopy.flip();
        output.put(bodyCopy);
        finalizeBodyLength(output, header);
    }

    void serializeNestedValue(Object value, GrowableByteBuffer output) throws IOException {
        serialize(value, output, false);
    }

    Object deserializeNestedValue(ByteBuffer input, Class<?> expectedClass) throws IOException {
        return deserializeInternal(input, expectedClass, false);
    }

    Object cloneWithSerializer(Object source, Serializer serializer, Class<?> expectedClass) throws IOException {
        if (source == null) {
            return null;
        }
        GrowableByteBuffer tmp = new GrowableByteBuffer(ByteBuffer.allocate(512), 512);
        writeBodyWithSerializer(source, serializer, tmp);
        ByteBuffer serialized = tmp.getBuffer();
        serialized.flip();
        return serializer.readObject(serialized, expectedClass);
    }

    private static final class ReadEnvelopeResult {
        private final long id;
        private final Class<?> messageClass;
        private final long bodyLength;

        private ReadEnvelopeResult(long id, Class<?> messageClass, long bodyLength) {
            this.id = id;
            this.messageClass = messageClass;
            this.bodyLength = bodyLength;
        }
    }

    private ReadEnvelopeResult readEnvelopeHeader(ByteBuffer bytes, boolean messageOnly) throws IOException {
        long id = -1;
        try {
            long classPathLength = VarInt.decodeSigned(bytes);
            if (classPathLength == -1) { // is null
                return null;
            }
            if (classPathLength < -1) {
                throw new IOException("Invalid class path length: " + classPathLength);
            }

            // read class path for registration (if any)
            byte classPath[] = null;
            if (classPathLength > 0) {
                if(classPathLength>1024){
                    throw new IOException("Class path length too long: " + classPathLength);
                }
                // read class path
                classPath = new byte[(int) classPathLength];
                bytes.get(classPath);
            }

            // read class id
            id = VarInt.decodeSigned(bytes);

            // register if registration data was submitted
            if (classPath != null) {
                logger.finer("Register class " + new String(classPath, StandardCharsets.UTF_8) + " with id " + id + " due to remote request");
                String className = new String(classPath, StandardCharsets.UTF_8);
                // check if id is already in use
                Class<?> messageClass = idXClass.get(id);

                if (messageClass != null && !messageClass.getName().equals(className)) {
                    // already used by another class
                    throw new RuntimeException(
                        "Class ID collision: " + id + " for class: " + className + " and " + messageClass.getName()
                    );
                }

                if (messageClass == null) {
                    if (messageOnly && !className.endsWith("Message")) {
                        throw new RuntimeException("Message class name must end with 'Message': " + className);
                    }

                    // load the class
                    messageClass = (Class<?>) Class.forName(className);

                    // check if sendable
                    checkIsSerializable(messageClass, messageOnly);

             
                    classXid.put(messageClass, id);
                    idXClass.put(id, messageClass);
                    onClassRegistered.accept(id);
                    logger.fine("Registered class " + className + " with id " + id+" due to remote request");
                }
            }

            Class<?> messageClass = idXClass.get(id);
            if (messageClass == null) {
                // class not registered
                throw new RuntimeException("Class ID not registered: " + id);
            }

            // paranoia check
            checkIsSerializable(messageClass, messageOnly);

            // read body length (unsigned int)
            long dataLength = (long) bytes.getInt() & 0xFFFFFFFFL;
            return new ReadEnvelopeResult(id, messageClass, dataLength);
        } catch (Exception e) {
            throw new IOException("Error deserializing object, class ID:" + id, e);
        }
    }

    private synchronized <T> T deserializeInternal(
        ByteBuffer bytes,
        Class<?> expectedClass,
        boolean messageOnly
    ) throws IOException   {
        ReadEnvelopeResult header = readEnvelopeHeader(bytes, messageOnly);
        if (header == null) return null;
        if (header.bodyLength > bytes.remaining()) {
            throw new RuntimeException("Data length mismatch: " + header.bodyLength + " != " + bytes.remaining());
        }

        try {
            if (header.bodyLength > Integer.MAX_VALUE) {
                throw new IOException("Body too large: " + header.bodyLength);
            }
            ByteBuffer body = bytes.slice();
            body.limit((int) header.bodyLength);
            bytes.position(bytes.position() + (int) header.bodyLength);

            Serializer serializer = getBestSerializerFor(header.messageClass);
            DiffRuntime.DecodeResult runtime = diffRuntime.decodeIfRuntime(body, header.messageClass, serializer);
            if (runtime.matched()) {
                if (runtime.isDropped()) {
                    return null;
                }
                Object obj = runtime.message();
                if (obj instanceof Collection && expectedClass.isArray()) {
                    Collection<?> collection = (Collection<?>) obj;
                    T array = (T) Array.newInstance(expectedClass.getComponentType(), collection.size());
                    int i = 0;
                    for (Object element : collection) {
                        Array.set(array, i++, element);
                    }
                    return array;
                }
                return (T) obj;
            }

            Object obj = decodeRegularBody(body, header.messageClass, serializer);
            if (obj instanceof Collection && expectedClass.isArray()) {
                Collection<?> collection = (Collection<?>) obj;
                T array = (T) Array.newInstance(expectedClass.getComponentType(), collection.size());
                int i = 0;
                for (Object element : collection) {
                    Array.set(array, i++, element);
                }
                return array;
            } else {
                return (T) obj;
            }
        } catch (Exception e) {
            // logger.log(Level.FINER, "Error deserializing object, class ID:" + header.id, e);
            throw new IOException("Error deserializing object, class ID:" + header.id, e);
        }
    }

    private Object decodeRegularBody(ByteBuffer body, Class<?> messageClass, Serializer serializer) throws IOException {
        long mode = VarInt.decodeUnsigned(body);
        if (mode != DIFF_BODY_MODE_FULL) {
            throw new IOException("Unsupported regular body mode: " + mode);
        }
        return serializer.readObject(body, messageClass);
    }

    /**
     * Converts a message to a ByteBuffer using the com.jme3.network.serializing.Serializer and the (short
     * length) + data protocol. If target is null then a 32k byte buffer will be created and filled.
     */
    @Override
    public ByteBuffer toByteBuffer(Message message, ByteBuffer target) {
        GrowableByteBuffer buffer = (target == null)
            ? new GrowableByteBuffer(ByteBuffer.allocate(1024), 1024)
            : new GrowableByteBuffer(target, 0);
        try {
            buffer.position(0);
            if (message instanceof DiffableMessage) {
                DiffRuntime.EncodeOutcome outcome = diffRuntime.encode(message, buffer);
                if (outcome == DiffRuntime.EncodeOutcome.SKIP) {
                    return EMPTY_MESSAGE_BUFFER.duplicate();
                }
                if (outcome == DiffRuntime.EncodeOutcome.BYPASS) {
                    serialize(message, buffer, true);
                }
            } else {
                serialize(message, buffer, true);
            }
            ByteBuffer out = buffer.getBuffer();
            out.flip();
            return out;
        } catch (IOException e) {
            throw new RuntimeException("Error serializing message", e);
        }
    }

    /**
     * Creates and returns a message from the properly sized byte buffer using
     * com.jme3.network.serializing.Serializer.
     */
    @Override
    public Message toMessage(ByteBuffer bytes) {
        try {
            bytes.position(0);
            ReadEnvelopeResult header = readEnvelopeHeader(bytes, true);
            if (header == null) {
                return null;
            }
            if (header.bodyLength > bytes.remaining()) {
                throw new IOException("Data length mismatch: " + header.bodyLength + " != " + bytes.remaining());
            }
            if (header.bodyLength > Integer.MAX_VALUE) {
                throw new IOException("Body too large: " + header.bodyLength);
            }
            ByteBuffer body = bytes.slice();
            body.limit((int) header.bodyLength);
            bytes.position(bytes.position() + (int) header.bodyLength);
            Serializer serializer = getBestSerializerFor(header.messageClass);
            DiffRuntime.DecodeResult runtime = diffRuntime.decodeIfRuntime(body, header.messageClass, serializer);
            if (runtime.matched()) {
                return runtime.isDropped() ? null : (Message) runtime.message();
            }
            return (Message) decodeRegularBody(body, header.messageClass, serializer);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public MessageBuffer createBuffer() {
        // Defaulting to LazyMessageBuffer
        return new LazyMessageBuffer(this);
    }
}
