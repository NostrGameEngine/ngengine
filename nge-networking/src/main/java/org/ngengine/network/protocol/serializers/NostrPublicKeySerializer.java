package org.ngengine.network.protocol.serializers;

import java.nio.ByteBuffer;

import org.ngengine.nostr4j.keypair.NostrPublicKey;

public class NostrPublicKeySerializer extends DynamicSerializer {

    @Override
    public NostrPublicKey readObject(java.nio.ByteBuffer data, Class c) throws java.io.IOException {
        byte[] array = new byte[32];
        data.get(array);
        NostrPublicKey key = NostrPublicKey.fromBytes(array);
        return key;        
    }

    @Override
    public void writeObject(org.ngengine.network.protocol.GrowableByteBuffer buffer, Object object) throws java.io.IOException {
        NostrPublicKey key = (NostrPublicKey) object;
        byte array[] = key._array();
        if (array.length != 32) {
            throw new IllegalArgumentException("Invalid NostrPublicKey length: " + array.length);
        }
        buffer.put(array);
    }
    
}
