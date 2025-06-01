package org.ngengine.network;

import java.io.Serializable;
import java.time.Instant;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import org.ngengine.nostr4j.keypair.NostrPrivateKey;
import org.ngengine.nostr4j.nip49.Nip49;
import org.ngengine.nostr4j.nip49.Nip49FailedException;


public class Lobby implements Cloneable, Serializable{
   
    protected final String id;
    protected final String key;
    protected String roomRawData;// used for filtering
    protected final Map<String, String> data = new HashMap<>();
    protected final Instant expiration;

    Lobby(String roomId, String roomKey, String roomRawData, Instant expiration) {  
        this.key = Objects.requireNonNull(roomKey);
        this.roomRawData = Objects.requireNonNull(roomRawData);
        this.id = roomId;
        this.expiration = expiration;
    }

    public Instant getExpiration() {
        return expiration;
    }

    public String getId(){
        return id;
    }

    public boolean isOwnedByLocalPeer() {
        return false;
    }

    public NostrPrivateKey getKey(String passphrase) throws Nip49FailedException {
        if (Nip49.isEncrypted(key)) {
            return Nip49.decryptSync(key, passphrase);
        }
        return NostrPrivateKey.fromBech32(key);
    }

    public NostrPrivateKey getKey() {
        if (Nip49.isEncrypted(key)) {
            throw new IllegalArgumentException("Key is encrypted, please provide a passphrase");
        }
        return NostrPrivateKey.fromBech32(key);
    }

    public boolean matches(String words[]){
        if(words==null) return true;
        for(String word : words){
            if(roomRawData.contains(word)) return true;
        }
        return false;
    }

    @Override
    public Lobby clone() {
        try {
            return (Lobby) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new RuntimeException(e);
        }
    }

    protected void setData(String key, String value) {
        if (value == null) {
            data.remove(key);
        } else {
            data.put(key, value);
        }
    }

    public String getData(String key) {
        return data.get(key);
    }

    public String getDataOrDefault(String key, String defaultValue) {
        return data.getOrDefault(key, defaultValue);
    }

    public Collection<String> getDataKeys() {
        return data.keySet();
    }
    
    protected Map<String, String> getData() {
        return data;
    }

    protected String getRawData() {
        return roomRawData;
    }

    protected void setRawData(String rawData) {
        this.roomRawData = rawData;
    }
    // }

    @Override
    public String toString() {
        return "Lobby [roomId=" + id + ", roomKey=" + key + ", roomRawData=" + roomRawData + ", data=" + data
                +", expiration=" + expiration.getEpochSecond() + "]";
    }


    public boolean isLocked(){
        return Nip49.isEncrypted(key);
    }


  
    
}
