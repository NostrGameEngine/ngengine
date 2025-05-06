package org.ngengine.network;

import java.io.Serializable;
import java.time.Instant;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;



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

  

    public String getKey() {
        return key;
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

    protected void setData(String key, String value) throws Exception {
        if (value == null) {
            data.remove(key);
        } else {
            data.put(key, value);
        }
    }

    public String getData(String key) {
        return data.get(key);
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
        return !key.startsWith("nsec");
    }


  
    
}
