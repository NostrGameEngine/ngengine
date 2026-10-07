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
import java.util.ArrayDeque;
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
    public static final int MAX_FRAME_BYTES = 128 * 1024;
    public static final int MAX_COLLECTION_ITEMS = 4096;
    public static final int MAX_STRING_BYTES = 64 * 1024;
    public static final int MAX_CLASSES = 512;
    public static final int MAX_DEPTH = 32;
    public static final int MAX_DECODE_NODES = 8192;
    public static final int MAX_EXPANDED_BYTES = 256 * 1024;
    public static final int MAX_DECODE_RESOURCE_BYTES = 3 * 1024 * 1024;
    private int decodeDepth;
    private int decodeNodes;
    private int expandedBytes;
    private int decodedValueBytes;
    private static final ThreadLocal<DynamicSerializerProtocol> ACTIVE_DECODE = new ThreadLocal<>();
    private int writeDepth;
    private volatile boolean retired;
    private Boolean receiveReliable;
    private int lastDecodeResourceBytes;
    private int decodeResourceLimit = MAX_DECODE_RESOURCE_BYTES;
    private int decodeFrameBytes;
    private Consumer<String> resourceFailureHandler;
    public enum OutboundFailure { CAPACITY, SERIALIZATION }
    private java.util.function.BiConsumer<OutboundFailure, String> detailedResourceFailureHandler;

    public synchronized void setResourceFailureHandler(Consumer<String> handler) {
        resourceFailureHandler = handler;
        detailedResourceFailureHandler = null;
    }

    /** Reports a fixed category and exception type, never an exception message or serialized value. */
    public synchronized void setDetailedResourceFailureHandler(java.util.function.BiConsumer<OutboundFailure, String> handler) {
        detailedResourceFailureHandler = handler;
        resourceFailureHandler = null;
    }

    private static boolean isCapacityFailure(Throwable error) {
        // These are fixed failures from this protocol and its bounded buffer/history; unknown failures stay serialization.
        try {
            for (int depth = 0; error != null && depth < 8; depth++, error = error.getCause()) {
                if (error instanceof java.nio.BufferOverflowException) return true;
                String message = error.getMessage();
                if (message == null) continue;
                if (message.equals("Initial buffer exceeds capacity bound") || message.equals("Buffer exceeds capacity bound")
                        || message.equals("Diff group retention bound exceeded") || message.equals("Diff base retention bound exceeded")
                        || message.equals("Array exceeds item bound") || message.equals("Class registration bound exceeded")
                        || message.equals("Serialization depth bound exceeded") || message.equals("Frame exceeds byte bound")
                        || message.equals("Expanded message budget exceeded") || message.equals("Decoded value budget exceeded")
                        || message.startsWith("Invalid bounded length: ") || message.startsWith("Serialized body too large: ")) return true;
            }
        } catch (Throwable ignored) { /* Failure classification cannot prevent fail-closed retirement. */ }
        return false;
    }

    public static int checkedLength(long length, int maximum, int remaining) throws IOException {
        if (length < 0 || length > maximum || length > remaining) {
            throw new IOException("Invalid bounded length: " + length);
        }
        return (int) length;
    }

    public static void chargeDecodedBytes(int length) throws IOException {
        DynamicSerializerProtocol protocol = ACTIVE_DECODE.get();
        if (protocol != null) {
            if (length < 0 || length > 512 * 1024 - protocol.decodedValueBytes) throw new IOException("Decoded value budget exceeded");
            protocol.checkDecodeResourceBudget(length, 0, 0);
            protocol.decodedValueBytes += length;
        }
    }

    /** Containers reserve their inevitable child nodes before allocating backing storage. */
    public static void preflightDecodeNodes(int nodes) throws IOException {
        DynamicSerializerProtocol protocol = ACTIVE_DECODE.get();
        if (protocol != null) {
            if (nodes < 0 || nodes > MAX_DECODE_NODES - protocol.decodeNodes)
                throw new IOException("Decode structure bound exceeded");
            protocol.checkDecodeResourceBudget(0, 0, nodes);
        }
    }

    private void chargeExpandedBytes(int length) {
        if (length < 0 || length > MAX_EXPANDED_BYTES - expandedBytes) {
            throw new IllegalArgumentException("Expanded message budget exceeded");
        }
        checkDecodeResourceBudget(0, length, 0);
        expandedBytes += length;
    }

    private void checkDecodeResourceBudget(int values, int expanded, int nodes) {
        long required = (long) decodeFrameBytes * 2 + (long) (decodedValueBytes + values) * 2
                + (long) (expandedBytes + expanded) * 2 + (long) (decodeNodes + nodes) * 128;
        if (required > decodeResourceLimit) throw new IllegalArgumentException("Decode exceeds frame resource allowance");
    }

    public synchronized int getLastDecodeResourceBytes() {
        return lastDecodeResourceBytes;
    }

    public synchronized void retire() {
        retired = true;
        diffRuntime.clear();
        classXid.clear();
        idXClass.clear();
        pendingAcks.clear();
        serializerCache.clear();
        tmpBuffer.remove();
        resourceFailureHandler = null;
        detailedResourceFailureHandler = null;
    }

    boolean acceptsRuntimeLane(long lane) {
        return receiveReliable == null || receiveReliable.booleanValue() == (lane == DiffRuntime.LANE_RELIABLE);
    }

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
        registerSerializer(CompressedMessage.class, new CompressedMessageSerializer(serializeFun, deserializeFun, this::chargeExpandedBytes));

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
            if (Array.getLength(obj) > MAX_COLLECTION_ITEMS) throw new IllegalArgumentException("Array exceeds item bound");
            ArrayList<Object> list = new ArrayList<>(Array.getLength(obj));
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
            if (idXClass.size() >= MAX_CLASSES) throw new IOException("Class registration bound exceeded");
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
        if (bodyLength > MAX_FRAME_BYTES) {
            throw new IOException("Serialized body too large: " + bodyLength + " bytes (max 131072)");
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
        if (retired) throw new IOException("Retired protocol");
        if (writeDepth >= MAX_DEPTH) throw new IOException("Serialization depth bound exceeded");
        writeDepth++;
        try {
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
        } finally {
            writeDepth--;
        }
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
        GrowableByteBuffer tmp = new GrowableByteBuffer(ByteBuffer.allocate(512), 512, MAX_FRAME_BYTES);
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
                if(classPathLength>1024 || classPathLength > bytes.remaining()){
                    throw new IOException("Class path length too long: " + classPathLength);
                }
                // read class path
                classPath = new byte[(int) classPathLength];
                bytes.get(classPath);
            }

            // read class id
            id = VarInt.decodeSigned(bytes);

            // Validate the body before loading or retaining a remote class.
            long dataLength = (long) bytes.getInt() & 0xFFFFFFFFL;
            checkedLength(dataLength, MAX_FRAME_BYTES, bytes.remaining());

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
                    if (idXClass.size() >= MAX_CLASSES) throw new IOException("Class registration bound exceeded");
                    messageClass = Class.forName(className, false, getClass().getClassLoader());

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

            if (messageOnly && !Message.class.isAssignableFrom(messageClass)) {
                throw new IOException("Envelope does not contain a Message");
            }
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
        if (decodeDepth >= MAX_DEPTH || decodeNodes >= MAX_DECODE_NODES) throw new IOException("Decode structure bound exceeded");
        checkDecodeResourceBudget(0, 0, 1);
        decodeDepth++;
        decodeNodes++;
        try {
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
        } finally {
            decodeDepth--;
        }
    }

    private Object decodeRegularBody(ByteBuffer body, Class<?> messageClass, Serializer serializer) throws IOException {
        long mode = VarInt.decodeUnsigned(body);
        if (mode != DIFF_BODY_MODE_FULL) {
            throw new IOException("Unsupported regular body mode: " + mode);
        }
        Object result = serializer.readObject(body, messageClass);
        if (body.hasRemaining()) throw new IOException("Trailing serializer body data");
        return result;
    }

    /**
     * Converts a message to a ByteBuffer using the com.jme3.network.serializing.Serializer and the (short
     * length) + data protocol. If target is null then a 32k byte buffer will be created and filled.
     */
    @Override
    public synchronized ByteBuffer toByteBuffer(Message message, ByteBuffer target) {
        ByteBuffer targetView = target == null ? null : target.duplicate();
        if (targetView != null) { targetView.clear(); targetView.limit(Math.min(targetView.capacity(), MAX_FRAME_BYTES)); }
        GrowableByteBuffer buffer = (targetView == null)
            ? new GrowableByteBuffer(ByteBuffer.allocate(1024), 1024, MAX_FRAME_BYTES)
            : new GrowableByteBuffer(targetView.slice(), 0, MAX_FRAME_BYTES);
        try {
            if (retired) throw new IOException("Retired protocol");
            if (decodeDepth == 0) { decodeNodes = 0; expandedBytes = 0; }
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
            if (buffer.position() > MAX_FRAME_BYTES) throw new IOException("Frame exceeds byte bound");
            ByteBuffer out = buffer.getBuffer();
            out.flip();
            return out;
        } catch (IOException | RuntimeException e) {
            Consumer<String> handler = resourceFailureHandler;
            java.util.function.BiConsumer<OutboundFailure, String> detailedHandler = detailedResourceFailureHandler;
            if (detailedHandler != null) {
                OutboundFailure reason = isCapacityFailure(e) ? OutboundFailure.CAPACITY : OutboundFailure.SERIALIZATION;
                String type = e.getClass().getSimpleName();
                type = type.substring(0, Math.min(80, type.length()));
                // Fail closed before reporting; publish the first cause before clearing protocol state or native teardown.
                retired = true;
                detailedResourceFailureHandler = null;
                try { detailedHandler.accept(reason, type); } finally { retire(); }
            } else if (handler != null) {
                retire();
                handler.accept("Outbound protocol capacity or serialization failure");
            }
            throw new RuntimeException("Error serializing message", e);
        }
    }

    /**
     * Creates and returns a message from the properly sized byte buffer using
     * com.jme3.network.serializing.Serializer.
     */
    @Override
    public synchronized Message toMessage(ByteBuffer bytes) {
        return toMessage(bytes, null);
    }

    public synchronized Message toMessage(ByteBuffer bytes, Boolean reliable) {
        return toMessage(bytes, reliable, MAX_DECODE_RESOURCE_BYTES);
    }

    /** Caller reserves this allowance before decode; tracked growth is checked before serializer allocation. */
    public synchronized Message toMessage(ByteBuffer bytes, Boolean reliable, int resourceAllowance) {
        if (resourceAllowance < 1 || resourceAllowance > MAX_DECODE_RESOURCE_BYTES)
            throw new IllegalArgumentException("Invalid decode resource allowance");
        int frameBytes = bytes.limit();
        DynamicSerializerProtocol previousDecode = ACTIVE_DECODE.get();
        ACTIVE_DECODE.set(this);
        decodedValueBytes = 0;
        decodeDepth = 1;
        decodeNodes = 1;
        expandedBytes = 0;
        receiveReliable = reliable;
        decodeResourceLimit = resourceAllowance;
        decodeFrameBytes = Math.min(frameBytes, MAX_FRAME_BYTES);
        try {
            checkDecodeResourceBudget(0, 0, 0);
            if (retired || frameBytes > MAX_FRAME_BYTES) throw new IOException("Retired or oversized frame");
            bytes = bytes.duplicate();
            bytes.position(0);
            ReadEnvelopeResult header = readEnvelopeHeader(bytes, true);
            if (header == null) {
                return null;
            }
            if (header.bodyLength != bytes.remaining()) {
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
                if (!runtime.isDropped() && body.hasRemaining()) throw new IOException("Trailing runtime body data");
                return runtime.isDropped() ? null : (Message) runtime.message();
            }
            return (Message) decodeRegularBody(body, header.messageClass, serializer);
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            lastDecodeResourceBytes = Math.min(frameBytes, MAX_FRAME_BYTES) * 2
                + decodedValueBytes * 2 + expandedBytes * 2 + decodeNodes * 128;
            if (previousDecode == null) ACTIVE_DECODE.remove(); else ACTIVE_DECODE.set(previousDecode);
            decodeDepth = 0;
            receiveReliable = null;
            decodeResourceLimit = MAX_DECODE_RESOURCE_BYTES;
            decodeFrameBytes = 0;
        }
    }

    @Override
    public MessageBuffer createBuffer() {
        // Preserve the provider's unsigned two-byte stream prefix, including fragmented headers.
        return new MessageBuffer() {
            private final ArrayDeque<ByteBuffer> frames = new ArrayDeque<>();
            private ByteBuffer current;
            private int highByte = -1;
            private int queuedBytes;
            private boolean failed;

            public synchronized boolean hasMessages() { return !frames.isEmpty(); }

            public synchronized Message pollMessage() {
                if (failed) throw new IllegalStateException("Failed stream buffer");
                ByteBuffer frame = frames.poll();
                if (frame == null) return null;
                queuedBytes -= frame.remaining();
                return toMessage(frame);
            }

            public synchronized boolean addBytes(ByteBuffer input) {
                if (failed) throw new IllegalStateException("Failed stream buffer");
                try {
                    while (input.hasRemaining()) {
                        if (current == null) {
                            if (highByte < 0) highByte = input.get() & 255;
                            if (!input.hasRemaining()) break;
                            int size = (highByte << 8) | (input.get() & 255);
                            highByte = -1;
                            if (size == 0 || frames.size() >= 128 || size > 1024 * 1024 - queuedBytes) {
                                throw new IllegalArgumentException("Stream frame/queue bound exceeded");
                            }
                            current = ByteBuffer.allocate(size);
                            queuedBytes += size;
                        }
                        int count = Math.min(input.remaining(), current.remaining());
                        ByteBuffer part = input.slice();
                        part.limit(count);
                        current.put(part);
                        input.position(input.position() + count);
                        if (!current.hasRemaining()) {
                            current.flip();
                            frames.add(current);
                            current = null;
                        }
                    }
                    return hasMessages();
                } catch (RuntimeException error) {
                    failed = true;
                    frames.clear();
                    current = null;
                    queuedBytes = 0;
                    throw error;
                }
            }
        };
    }
}
