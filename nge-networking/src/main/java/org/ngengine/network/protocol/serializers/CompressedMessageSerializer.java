package org.ngengine.network.protocol.serializers;

 
import com.jme3.network.Message;
import com.jme3.network.serializing.Serializer;
import com.jme3.network.serializing.SerializerException;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.BufferOverflowException;
import java.nio.ByteBuffer;
import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.Deflater;
import java.util.zip.Inflater;

import org.ngengine.network.protocol.GrowableByteBuffer;
import org.ngengine.network.protocol.messages.CompressedMessage;

/**
 * The field serializer is the default serializer used for custom class.
 *
 * @author Lars Wesselius, Nathan Sweet
 */
public class CompressedMessageSerializer extends DynamicSerializer {
    
    private static final Logger log = Logger.getLogger(CompressedMessageSerializer.class.getName());
    protected final BiFunction<Object, GrowableByteBuffer, Void> serialize;
    protected final BiFunction<ByteBuffer, Class<?>, Object> deserialize;

 
    public CompressedMessageSerializer(
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
        return  this.deserialize.apply(data, c);
        
    }

    
    @Override    
    public <T> T readObject(ByteBuffer data, Class<T> c) throws IOException {    
        try{

            if (data.remaining() < 2) {
                throw new IOException("Not enough data to read compressed message");
            }
            int length = data.getInt();

            if (length < 0 || length > data.remaining()) {
                throw new IOException("Invalid compressed message length: " + length);
            }

            byte[] compressedData = new byte[length];
            data.get(compressedData);

            // print compressed data
            System.out.println("Compressed data:");
            for (int i = 0; i < compressedData.length; i++) {
                System.out.print(compressedData[i] + " ");
            }
            System.out.println();

            Inflater inflater = new Inflater();
            inflater.setInput(compressedData);

            ByteArrayOutputStream bos = new ByteArrayOutputStream(compressedData.length);
            int decompressedSize = 0;
            byte[] chunk = new byte[1024];
            while (!inflater.finished()) {
                int s = inflater.inflate(chunk);
                if (s == 0) {
                    break;
                }
                bos.write(chunk, 0, s);
                decompressedSize += s;
            }
            inflater.end();
            byte[] decompressedData = bos.toByteArray();
            ByteBuffer buffer = ByteBuffer.wrap(decompressedData,0, decompressedSize);

            System.out.println("Decompressed data:");
            for (int i = 0; i < decompressedSize; i++) {
                System.out.print(buffer.get(i) + " ");
            }
            System.out.println();

            Message object = (Message) this.deserialize(buffer,Object.class); 
            CompressedMessage compressedMessage = new CompressedMessage(object);         
            return (T) compressedMessage;
        } catch (Exception e) {
            throw new IOException(e);
        }
    }


    @Override
    public void writeObject(GrowableByteBuffer buffer, Object cm) throws IOException {
        CompressedMessage compressedMessage = (CompressedMessage) cm;
        Message object = compressedMessage.getMessage();

        GrowableByteBuffer tmp = new GrowableByteBuffer(ByteBuffer.allocate(1024), 1024);
        this.serialize(object, tmp);
        tmp.flip();

    
        System.out.println("Uncompressed data:");   
        for (int i = 0; i < tmp.limit(); i++) {
            System.out.print(tmp.get(i) + " ");
        }
        System.out.println();

        Deflater deflater = new Deflater();
        byte[] inputBytes = new byte[tmp.limit()];
        tmp.get(inputBytes);
        deflater.setInput(inputBytes);
        deflater.finish();

        
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        byte[] chunk = new byte[1024];
        while (!deflater.finished()) {
            int count = deflater.deflate(chunk);
            if (count > 0) {
                outputStream.write(chunk, 0, count);
            } else {
                break;
            }
        }
        deflater.end();

        byte[] compressedData = outputStream.toByteArray();
        buffer.putInt(compressedData.length);
        buffer.put(compressedData);

        // print compressed data
        System.out.println("Compressed data:");
        for (int i = 0; i < compressedData.length; i++) {
            System.out.print(compressedData[i] + " ");
        }
        System.out.println();

     

    }

 

}
