package io.github.jmecn.tiled.core;

import java.util.Properties;

import com.jme3.util.SafeArrayList;

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
 * @author yanmaoyuan
 * 
 */
public class Base {
    protected String name;
    protected Properties properties = new Properties();
    protected boolean updateNeeded = true;
    protected boolean propertiesUpdateNeeded = true;

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
        return name;
    }   
    
    public boolean hasProperties() {
        return properties != null && !properties.isEmpty();
    }

    public void setUpdateNeeded(){
        this.updateNeeded = true;   
    }

    public boolean isUpdateNeeded() {
        return updateNeeded;
    }

    public void clearUpdateNeeded(){
        this.updateNeeded = false;
    }

    public void setPropertiesUpdateNeeded(){
        this.propertiesUpdateNeeded = true;   
    }

    public boolean isPropertiesUpdateNeeded() {
        return propertiesUpdateNeeded;
    }

    public void clearPropertiesUpdateNeeded(){
        this.propertiesUpdateNeeded = false;
    }

    /**
     * <p>
     * Getter for the field <code>properties</code>.
     * </p>
     * 
     * @return the map properties
     */
    public Properties getProperties() {
        return properties;
    }

    /**
     * <p>
     * Setter for the field <code>properties</code>.
     * </p>
     * 
     * @param properties
     *            a {@link java.util.Properties} object.
     */
    public void setProperties(Properties properties) {
        this.properties.clear();
        this.properties.putAll(properties);
        setPropertiesUpdateNeeded();
    }

    /**
     * @param key property name
     * @return true if and only if the property exists
     */
    public boolean containsKey(String key) {
        return properties.containsKey(key);
    }


    /** @param key property name
     * @return the value for that property if it exists, otherwise, null */
    public Object getProperty(String key) {
        return properties.get(key);
    }

    /** Returns the object for the given key, casting it to clazz.
     * @param key the key of the object
     * @param clazz the class of the object
     * @return the object or null if the object is not in the map
     * @throws ClassCastException if the object with the given key is not of type clazz */
    public <T> T getProperty(String key, Class<T> clazz) {
        return (T)getProperty(key);
    }

    /**
     * Returns the object for the given key, casting it to clazz.
     * @param key the key of the object
     * @param defaultValue the default value
     * @param clazz the class of the object
     * @return the object or the defaultValue if the object is not in the map
     * @throws ClassCastException if the object with the given key is not of type clazz
     */
    public <T> T getProperty(String key, T defaultValue, Class<T> clazz) {
        Object object = getProperty(key);
        return object == null ? defaultValue : (T)object;
    }

    /**
     * @param key property name
     * @param value value to be inserted or modified (if it already existed)
     */
    public void putProperty(String key, Object value) {
        properties.put(key, value);
        setPropertiesUpdateNeeded();
    }

    /**
     * @param properties set of properties to be added
     */
    public void putAll(Properties properties) {
        this.properties.putAll(properties);
        setPropertiesUpdateNeeded();
    }

    /**
     * @param key property name to be removed
     */
    public void removeProperty(String key) {
        properties.remove(key);
        setPropertiesUpdateNeeded();
    }

   
}
