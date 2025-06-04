package org.ngengine.store;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.ngengine.platform.NGEPlatform;
import org.ngengine.platform.VStore;

import com.jme3.app.Application;
import com.jme3.asset.AssetInfo;
import com.jme3.asset.AssetKey;
import com.jme3.asset.AssetManager;

import com.jme3.export.Savable;
import com.jme3.export.SavableWrapSerializable;
import com.jme3.export.binary.BinaryExporter;
import com.jme3.export.binary.BinaryImporter;

public class DataStore {
    private final Logger log = Logger.getLogger(DataStore.class.getName());
    static class SerializableEntry implements Serializable{
        public Object value;
    }

    private final VStore store;
    // private final Application app;
    private final boolean isCache;
    private final String name;
    private final AssetManager assetManager;

    public DataStore(String appName, AssetManager assetManager, String name, boolean isCache) {
        this.assetManager = assetManager;

        this.isCache = isCache;
        this.name = name;
        NGEPlatform platform = NGEPlatform.get();
        if(isCache) {
            store = platform.getCacheStore(appName, name);
        } else {
            store = platform.getDataStore(appName, name);
        }     
    }

    public void write(String key, Object value) throws IOException {
        if(!(value instanceof Savable)){
            SerializableEntry entry = new SerializableEntry();
            entry.value = value;
            value = new SavableWrapSerializable(entry);
        }
        try(OutputStream os = store.write(key+".j3o").await()) {
            BinaryExporter exporter = BinaryExporter.getInstance();
            exporter.save((Savable)value, os);
        } catch (Throwable e) {
            throw new IOException("Failed to write to store: " + key, e);
        }
    }

    private <T> T readFromStream(InputStream is) throws IOException{
        BinaryImporter importer = BinaryImporter.getInstance();
        Object out = importer.load(is);
        if (out instanceof SerializableEntry) {
            SerializableEntry entry = (SerializableEntry) out;
            out = entry.value;
        }
        return (T) out;
    }
    
    public  <T> T read(String key) throws IOException {
        try{

            String prefix = (isCache ? "cache/" : "data/") + name + "/";
            AssetKey<Object> assetKey = new AssetKey<>(prefix + key + ".j3o");
            AssetInfo assetInfo = assetManager.locateAsset(assetKey);
            if(assetInfo!=null){
                try(InputStream is = assetInfo.openStream()) {
                    return readFromStream(is);
                } catch (Throwable e) {
                    log.log(Level.WARNING, "Failed to read bundled store data: " + key, e);
                }
            }
        } catch (Exception e) {
            log.log(Level.WARNING, "Failed to locate bundled store data: " + key, e);
        }
        try(InputStream is = store.read(key+".j3o").await()) {
            return readFromStream(is);           
        }catch (Throwable e) {
            throw new IOException("Failed to read from store: " + key, e);
        }
    }

    public boolean exists(String key) {
        try{
            return store.exists(key + ".j3o").await();
        } catch (Exception e) {
            throw new RuntimeException("Failed to check existence of key: " + key, e);
        }
    }

    public void delete(String key) {
        try {
            store.delete(key + ".j3o").await();
        } catch (Exception e) {
            throw new RuntimeException("Failed to delete key: " + key, e);
        }
    }

    public List<String> list() {
        try {
            return store.listAll().await();
        } catch (Exception e) {
            throw new RuntimeException("Failed to clear the store", e);
        }
    }


    



    
}
