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

package org.ngengine.world2d.tiled.core;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;

import org.ngengine.components.ComponentManagerProvider;
import org.ngengine.components.Component;

import org.ngengine.world2d.tiled.components.TiledComponentManager;

/**
 * Wraps any number of custom properties. Can be used as a child of the map,
 * tile (when part of a tileset), layer, objectgroup and object elements.
 * 
 * The type of the property. Can be string (default), int, float, bool, color or
 * file (since 0.16, with color and file added in 0.17).
 * 
 * Boolean properties have a value of either "true" or "false".
 * 
 * Color properties are stored in the format #AARRGGBB.
 * 
 * File properties are stored as paths relative from the location of the map
 * file.
 * 
 * @author yanmaoyuan, Riccardo Balbo
 * 
 */
public abstract class TiledBase  {
    private final static java.util.logging.Logger logger = java.util.logging.Logger.getLogger(TiledBase.class.getName());
    private static final AtomicInteger updateCounter = new AtomicInteger(1);

    protected Map<String, Object> properties = new HashMap<>();
    protected String name;
    protected int updateNeeded = 0;
    protected int propertiesUpdateNeeded = 0;
    
    public static final AtomicInteger nextId = new AtomicInteger(Integer.MIN_VALUE + 1);


    
    {
        setUpdateNeeded();
        setPropertiesUpdateNeeded();
    }

    public boolean hasProperties() {
        return properties != null && !properties.isEmpty();
    }

    public void setUpdateNeeded(){        
        int id = -1;
        while(id == -1||id==0){
            id = updateCounter.getAndIncrement();
        }
        this.updateNeeded = id;
    }

    public void setPropertiesUpdateNeeded(){
        int id = -1;
        while(id == -1||id==0){
            id = updateCounter.getAndIncrement();
        }
        this.propertiesUpdateNeeded = id;
    }

    public int getPropertiesUpdateId() {
        return propertiesUpdateNeeded;
    }

    public int getUpdateId() {
        return updateNeeded;
    }


    /**
     * <p>
     * Getter for the field <code>properties</code>.
     * </p>
     * 
     * @return the map properties
     */
    // Properties getProperties() {
    //     return properties;
    // }

    /**
     * <p>
     * Setter for the field <code>properties</code>.
     * </p>
     * 
     * @param properties
     *            a map object.
     */
    public void setProperties(Map<?, ?> properties) {
        this.properties.clear();
        for (Map.Entry<?, ?> entry : properties.entrySet()) {
            String key = String.valueOf(entry.getKey());
            Object v = entry.getValue();
            if (v == null) {
                this.properties.remove(key);
            } else {
                this.properties.put(key, v);
            }
        }
        setPropertiesUpdateNeeded();
    }

    /** @param key property name
     * @return the value for that property if it exists, otherwise, null */
    public Object getProperty(String key) {
        return properties.get(key);
    }

    public Object getPropertyOrDefault(String key, Object defaultValue) {
        Object value = getProperty(key);
        return value != null ? value : defaultValue;
    }

    public void putProperties(Map<?,?> props) {
        for(Map.Entry<?,?> entry : props.entrySet()){
            String key = String.valueOf(entry.getKey());
            Object v = entry.getValue();
            if(v==null){
                properties.remove(key);                 
            } else{
                properties.put(key, v);
            }             
        }
        setPropertiesUpdateNeeded();       
    }

  

    /**
     * @param key property name
     * @param value value to be inserted or modified (if it already existed)
     */
    public void putProperty(String key, Object value) {
        if(value==null){
            properties.remove(key);
            setPropertiesUpdateNeeded();
            return;
        }
        properties.put(key, value);
        setPropertiesUpdateNeeded();
    }

 
    public void copyPropertiesTo(TiledBase target){
        for(String k: listPropertyKeys()){
            Object v = getProperty(k);
            target.putProperty(k, v);
        }
        target.setPropertiesUpdateNeeded();
    }

    public void copyPropertiesTo(Map<String, Object> target){
        // target.putAll(this.properties);
        for(String k: listPropertyKeys()){
            Object v = getProperty(k);
            target.put(k, v);           
        }
    }

    public void clearProperties(){
        properties.clear();
        setPropertiesUpdateNeeded();
    }

    public Set<String> listPropertyKeys(){
        return properties.keySet();

    }
    

    public String getName() {
        return name != null ? name : getClass().getSimpleName();
    }
 

    /**
     * @param name The name of the layer.
     */
    public void setName(String name) {
        this.name = name;
        setUpdateNeeded();
    }

    @Override
    public String toString() {
        return getName()+ "{" +
                "properties=" + properties +
                ", name='" + name + '\'' +
                '}';
    }   

    public Map<String, Object> getAllProperties(){
        return properties;
    }
    
   
}
