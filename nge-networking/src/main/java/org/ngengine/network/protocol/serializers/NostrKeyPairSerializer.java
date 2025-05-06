package org.ngengine.network.protocol.serializers;

import org.ngengine.nostr4j.keypair.NostrKeyPair;
import org.ngengine.nostr4j.keypair.NostrPrivateKey;
import org.ngengine.nostr4j.keypair.NostrPublicKey;

public class NostrKeyPairSerializer extends DynamicSerializer {

    @Override
    public Object readObject(java.nio.ByteBuffer data, Class c) throws java.io.IOException {
        byte[] array = new byte[32];
        data.get(array);
        NostrPrivateKey key = NostrPrivateKey.fromBytes(array);
        
        byte []array2 = new byte[32];
        data.get(array2);
        NostrPublicKey key2 = NostrPublicKey.fromBytes(array2);

        return new NostrKeyPair(key, key2);


    }

    @Override
    public void writeObject(org.ngengine.network.protocol.GrowableByteBuffer buffer, Object object) throws java.io.IOException {
        byte array[] = ((NostrKeyPair) object).getPrivateKey()._array();
        if (array.length != 32) {
            throw new IllegalArgumentException("Invalid NostrPublicKey length: " + array.length);
        }
        buffer.put(array);
        byte array2[] = ((NostrKeyPair) object).getPublicKey()._array();
        if (array2.length != 32) {
            throw new IllegalArgumentException("Invalid NostrPublicKey length: " + array2.length);
        }
        buffer.put(array2);
        
        
    }
    
}
