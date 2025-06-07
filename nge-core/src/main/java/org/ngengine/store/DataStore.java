/**
 * Copyright (c) 2025, Nostr Game Engine
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
 * the BSD 3-Clause License. The original jMonkeyEngine license is as follows:
 */
package org.ngengine.store;

import com.jme3.asset.AssetInfo;
import com.jme3.asset.AssetKey;
import com.jme3.asset.AssetManager;
import com.jme3.export.Savable;
import com.jme3.export.SavableWrapSerializable;
import com.jme3.export.binary.BinaryExporter;
import com.jme3.export.binary.BinaryImporter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.ngengine.platform.NGEPlatform;
import org.ngengine.platform.VStore;

/**
 * A data store is key-value storage that can be used to store ${@link Savable} and serializable objects and
 * primitives to a persistent storage.
 *
 * <p>
 * It uses {@link VStore} internally and differentiates between cache and data stores, however the actual
 * implementation is platform dependent and will use whatever form of storage is the most appropriate and
 * efficient for the platform and the intended use case.
 *
 */
public class DataStore {

    private final Logger log = Logger.getLogger(DataStore.class.getName());

    static class SerializableEntry implements Serializable {

        public Object value;
    }

    private final VStore store;
    private final boolean isCache;
    private final String name;
    private final AssetManager assetManager;

    public DataStore(String appName, AssetManager assetManager, String name, boolean isCache) {
        this.assetManager = assetManager;

        this.isCache = isCache;
        this.name = name;
        NGEPlatform platform = NGEPlatform.get();
        if (isCache) {
            store = platform.getCacheStore(appName, name);
        } else {
            store = platform.getDataStore(appName, name);
        }
    }

    public VStore getVStore() {
        return store;
    }

    /**
     * Fully writes and commits a savable or serializable object or a primitive.
     *
     * @param key
     *            the key to store the object under
     * @param value
     *            the object to store, must be a {@link Savable} or serializable object
     * @throws IOException
     */
    public void write(String key, Object value) throws IOException {
        if (!(value instanceof Savable)) {
            SerializableEntry entry = new SerializableEntry();
            entry.value = value;
            value = new SavableWrapSerializable(entry);
        }
        try (OutputStream os = store.write(key + ".j3o").await()) {
            BinaryExporter exporter = BinaryExporter.getInstance();
            exporter.save((Savable) value, os);
        } catch (Throwable e) {
            throw new IOException("Failed to write to store: " + key, e);
        }
    }

    private <T> T readFromStream(InputStream is) throws IOException {
        BinaryImporter importer = BinaryImporter.getInstance();
        Object out = importer.load(is);
        if (out instanceof SerializableEntry) {
            SerializableEntry entry = (SerializableEntry) out;
            out = entry.value;
        }
        return (T) out;
    }

    /**
     * Reads a savable or serializable object or a primitive from the store.
     *
     * @param key
     *            the key to read the object from
     * @return the object read from the store, can be a {@link Savable} or a serializable object
     * @throws IOException
     *             if reading fails
     */
    public <T> T read(String key) throws IOException {
        try {
            String prefix = (isCache ? "cache/" : "data/") + name + "/";
            AssetKey<Object> assetKey = new AssetKey<>(prefix + key + ".j3o");
            AssetInfo assetInfo = assetManager.locateAsset(assetKey);
            if (assetInfo != null) {
                try (InputStream is = assetInfo.openStream()) {
                    return readFromStream(is);
                } catch (Throwable e) {
                    log.log(Level.WARNING, "Failed to read bundled store data: " + key, e);
                }
            }
        } catch (Exception e) {
            log.log(Level.WARNING, "Failed to locate bundled store data: " + key, e);
        }
        try (InputStream is = store.read(key + ".j3o").await()) {
            return readFromStream(is);
        } catch (Throwable e) {
            throw new IOException("Failed to read from store: " + key, e);
        }
    }

    /**
     * Checks if a key exists in the store.
     *
     * @param key
     *            the key to check
     * @return true if the key exists, false otherwise
     */
    public boolean exists(String key) {
        try {
            return store.exists(key + ".j3o").await();
        } catch (Exception e) {
            throw new RuntimeException("Failed to check existence of key: " + key, e);
        }
    }

    /**
     * Deletes a key from the store.
     *
     * @param key
     *            the key to delete
     */
    public void delete(String key) {
        try {
            store.delete(key + ".j3o").await();
        } catch (Exception e) {
            throw new RuntimeException("Failed to delete key: " + key, e);
        }
    }

    /**
     * Clears the store, deleting all keys.
     */
    public List<String> list() {
        try {
            return store.listAll().await();
        } catch (Exception e) {
            throw new RuntimeException("Failed to clear the store", e);
        }
    }
}
