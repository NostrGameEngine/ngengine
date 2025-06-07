package org.ngengine.network.protocol;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Array;
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
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.Vector;
import java.util.WeakHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.BiFunction;
import java.util.jar.Attributes;

import org.ngengine.network.protocol.messages.ByteDataMessage;
import org.ngengine.network.protocol.messages.CompressedMessage;
import org.ngengine.network.protocol.messages.TextDataMessage;
import org.ngengine.network.protocol.serializers.BooleanSerializer;
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


/**
 *
 * @author Riccardo Balbo
 */
public class DynamicSerializerProtocol implements MessageProtocol {

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
    private final AtomicLong lastId = new AtomicLong(0);
    private final ThreadLocal<ByteBuffer> tmpBuffer = ThreadLocal
            .withInitial(() -> ByteBuffer.allocate(32767));


    private final BiFunction<Object, GrowableByteBuffer, Void> serializeFun = (obj, bbf) -> {
        try {
            this.serialize(obj, bbf, false);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return null;
    };
    private final BiFunction<ByteBuffer,Class<?> , Object> deserializeFun = (bbf,cls) -> {
        try {
            return this.deserialize(bbf, cls, false);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    };

    private final Collection<RegisteredSerializer> serializers = new ArrayList<>();
    private final Collection<Class<?>> serializables = new ArrayList<>();
    private final Collection<Class> serializablesAnnotation = new ArrayList<>();
    private final boolean spidermonkeyCompatible;
    private boolean forceSpidermonkeyStaticBuffer = false;

    /**
     * Creates a new DynamicSerializerProtocol that automatically handles class registration and
     * serialization.
     * 
     * 
     * @param strict
     *            set if the serializer should be strict (safer) or unstrict (FAFO)
     */
    public DynamicSerializerProtocol(boolean spidermonkeyCompatible) {
        this.spidermonkeyCompatible=spidermonkeyCompatible;
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
            short.class
        );  
        registerSerializableAnnotation(NetworkSafe.class);
        
        if(spidermonkeyCompatible) registerSerializableAnnotation(Serializable.class);
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
        registerSerializer(CompressedMessage.class,
                new CompressedMessageSerializer(serializeFun, deserializeFun));

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

    public void registerSerializable(Class<?> ...clss) {
        for(Class<?> cls: clss){

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
        for(Class<? extends Annotation> serializableAnnotation: this.serializablesAnnotation){
            if (messageClass.isAnnotationPresent(serializableAnnotation)) {
                return;
            }
        }

        if (!messageOnly) {
            for(Class<?> cls: this.serializables){
                if (cls == messageClass) {
                    return;
                }
            }
        }

        if(Message.class.isAssignableFrom(messageClass)){
            throw new RuntimeException("Message " + messageClass.getName()
                    + " is not whitelisted. Please mark it with the  org.ngengine.network.protocol.NetworkSafe annotation.");
        } else {
            throw new RuntimeException("Class " + messageClass.getName()
                + " is not serializable. Please register a serializer for this class.");
        }

    }

    protected Serializer getBestSerializerFor(Class<?> cls) {
        for(int i = serializers.size()-1; i>=0; i--){
            RegisteredSerializer reg = (RegisteredSerializer) serializers.toArray()[i];
            if (reg.isSerializerFor(cls)) {
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

        return obj;
    }

    protected void serialize(Object obj, GrowableByteBuffer buffer, boolean messageOnly) throws IOException {
        if (obj == null) { // -1 = null
            buffer.putShort((short) -1);
            return;
        }

        if(obj.getClass().isArray()){
            ArrayList<Object> list = new ArrayList<>();
            for(int i=0; i<Array.getLength(obj); i++){
                list.add(Array.get(obj, i));
            }
            obj= list;
        }


        obj = swapInternals(obj);
        
        Class<?> messageClass = obj.getClass();

        

        // check if message is sendable to the network
        checkIsSerializable(messageClass, messageOnly);

        Long id = classXid.get(messageClass);

        boolean registerClass = false;
        if (id == null) {
            // This is a new class... assign it an ID
            id = lastId.incrementAndGet();
            classXid.put(messageClass, id);
            idXClass.put(id, messageClass);
            registerClass = true;
        }

        // set header
        if (registerClass) {
            // send registration data the first time we see the class for this connection
            byte classPath[] = messageClass.getName().getBytes(StandardCharsets.UTF_8);
            buffer.putShort((short) classPath.length);
            buffer.put(classPath);
        } else {
            buffer.putShort((short) 0);            
        }

        buffer.putLong(id);

        // skip body length
        int bodyLengthPos = buffer.position();
        buffer.putShort((short) 0); // placeholder for body length

        int beforeBodyPos = buffer.position();

        // write body
        Serializer serializer = getBestSerializerFor(obj.getClass());
        if (serializer instanceof DynamicSerializer && !this.forceSpidermonkeyStaticBuffer) {
            ((DynamicSerializer) serializer).writeObject(buffer, obj);
        } else  if(spidermonkeyCompatible){
            ByteBuffer bbf = tmpBuffer.get();
            synchronized (bbf) {
                // bbf.clear();
                if(bbf!=buffer.getBuffer()){
                    bbf.clear();
                    serializer.writeObject(bbf, obj);
                    bbf.flip();
                    buffer.put(bbf);
                } else {
                    serializer.writeObject(bbf, obj);
                }
                
            }
        } else{
            throw new IOException("Serializer " + serializer.getClass().getName()
                    + " does not support dynamic buffers. Please register a serializer for this class.");
        }
        int lastPos = buffer.position();

        // write body length
        buffer.position(bodyLengthPos);
        short dataLength = (short) (lastPos - beforeBodyPos);
        buffer.putShort(dataLength);

        // return to the end of the body
        buffer.position(lastPos);

        

    }

    protected <T> T deserialize(ByteBuffer bytes, Class<?> expectedClass,  boolean messageOnly) throws IOException {
        
   
        long id = -1;
        try {
            short classPathLength = bytes.getShort();
            if (classPathLength == -1) { // is null
                return null;
            }

            // read class path for registration (if any)
            byte classPath[] = null;
            if (classPathLength > 0) {
                // read class path
                classPath = new byte[classPathLength];
                bytes.get(classPath);
            }
 
            // read class id
            id = bytes.getLong();


            // register if registration data was submitted
            if (classPath != null) {
                String className = new String(classPath, StandardCharsets.UTF_8);
                // check if id is already in use
                Class<?> messageClass = idXClass.get(id);

                if (messageClass != null && !messageClass.getName().equals(className)) {
                    // already used by another class
                    throw new RuntimeException("Class ID collision: " + id + " for class: " + className
                            + " and " + messageClass.getName());
                }

                if (messageClass == null) {
                    if (!className.endsWith("Message")) {
                        throw new RuntimeException(
                                "Message class name must end with 'Message': " + className);
                    }

                    // load the class
                    messageClass = (Class<?>) Class.forName(className);

                    // check if sendable
                    checkIsSerializable(messageClass, messageOnly);

                    // check class is registered with another id
                    Long currentId = classXid.get(messageClass);
                    if (currentId != null) {
                        throw new RuntimeException("Class " + messageClass.getName()
                                + " is already registered with id: " + currentId);
                    }

                    // register
                    classXid.put(messageClass, id);
                    idXClass.put(id, messageClass);
                }
            }

            Class<?> messageClass = idXClass.get(id);
            if (messageClass == null) {
                // class not registered
                throw new RuntimeException("Class ID not registered: " + id);
            }

            // paranoia check
            checkIsSerializable(messageClass, messageOnly);


            // read body length
            short dataLength = bytes.getShort();

            if (dataLength > bytes.remaining()) {
                throw new RuntimeException(
                        "Data length mismatch: " + dataLength + " != " + bytes.remaining());
            }

            // read body
            Serializer serializer = getBestSerializerFor(messageClass);
            Object obj = serializer.readObject(bytes, messageClass);
            if(obj instanceof Collection && expectedClass.isArray()){
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
            throw new IOException("Error deserializing object, class ID:" + id, e);
        }

    }

    /**
     * Converts a message to a ByteBuffer using the com.jme3.network.serializing.Serializer and the (short
     * length) + data protocol. If target is null then a 32k byte buffer will be created and filled.
     */
    @Override
    public ByteBuffer toByteBuffer(Message message, ByteBuffer target) {

        // Could let the caller pass their own in
        // ByteBuffer buffer = target == null ? ByteBuffer.allocate(32767 + 2 + 8 + 2) : target;

        GrowableByteBuffer buffer = (target == null) ? new GrowableByteBuffer(ByteBuffer.allocate(1024), 1024)
                                                     : new GrowableByteBuffer(target, 0);
        try {
            // start from the beginning of the buffer
            buffer.position(0);

            // skip body length
            // int bodyLengthPos = buffer.position();
            // buffer.position(buffer.position() + 2);

            // // write body
            // Serializer serializer = getBestSerializerFor(messageClass);
            // serializer.writeObject(buffer, message);
            // buffer.flip();

            // // write body length
            // buffer.position(bodyLengthPos);
            // short dataLength = (short)(buffer.remaining() - 2 - 8 - 2);
            // buffer.putShort(dataLength);

            serialize(message, buffer, true);
            ByteBuffer out  = buffer.getBuffer();
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
            // read header
            bytes.position(0);

            return deserialize(bytes, Message.class, true);

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
