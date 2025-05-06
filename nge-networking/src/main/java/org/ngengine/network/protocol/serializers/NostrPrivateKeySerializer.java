package org.ngengine.network.protocol.serializers;

import java.nio.ByteBuffer;

import org.ngengine.nostr4j.keypair.NostrPrivateKey;
import org.ngengine.nostr4j.keypair.NostrPublicKey;

public class NostrPrivateKeySerializer extends DynamicSerializer {

    @Override
    public NostrPrivateKey readObject(java.nio.ByteBuffer data, Class c) throws java.io.IOException {
        byte[] array = new byte[32];
        data.get(array);
        NostrPrivateKey key = NostrPrivateKey.fromBytes(array);
        return key;        
    }

    @Override
    public void writeObject(org.ngengine.network.protocol.GrowableByteBuffer buffer, Object object) throws java.io.IOException {
        NostrPrivateKey key = (NostrPrivateKey) object;
        byte array[] = key._array();
        if (array.length != 32) {
            throw new IllegalArgumentException("Invalid NostrPublicKey length: " + array.length);
        }
        buffer.put(array);
    }
    
}
