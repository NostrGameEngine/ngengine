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

package org.ngengine.world2d.tiled.core.entity;

import java.math.BigInteger;
import java.util.List;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import org.ngengine.components.Component;

import com.jme3.math.Vector2f;
import com.jme3.util.SafeArrayList;

import org.ngengine.world2d.tiled.core.TiledEntity;
import org.ngengine.world2d.tiled.core.TiledObjectLayer;
import org.ngengine.world2d.tiled.core.TiledObjectText;
import org.ngengine.world2d.tiled.core.tileset.Tile;
import org.ngengine.world2d.tiled.enums.ObjectShape;

/**
 * An object occupying an {@link TiledObjectLayer}.
 * <p>
 * While tile layers are very suitable for anything repetitive aligned to the
 * tile grid, sometimes you want to annotate your map with other information,
 * not necessarily aligned to the grid. Hence the objects have their coordinates
 * and size in pixels, but you can still easily align that to the grid when you
 * want to.
 * </p>
 * <p>
 * You generally use objects to add custom information to your tile map, such as
 * spawn points, warps, exits, etc.
 * </p>
 * <p>
 * When the object has a gid set, then it is represented by the image of the
 * tile with that global ID. The image alignment currently depends on the map
 * orientation. In orthogonal orientation it's aligned to the bottom-left while
 * in isometric it's aligned to the bottom-center.
 * </p>
 * <p>
 * Can contain: properties, ellipse (since 0.9), polygon, polyline, image
 * </p>
 * @author yanmaoyuan, Riccardo Balbo
 *  
 */
public class TiledObjectEntity extends TiledEntity {

    // private static final AtomicInteger generatedIDCounter = new AtomicInteger(Integer.MIN_VALUE);

    private TiledObjectLayer objectGroup;

    /**
     * Unique ID of the object. Each object that is placed on a map gets a
     * unique id. Even if an object was deleted, no object gets the same ID. Can
     * not be changed in Tiled Qt. (since Tiled 0.11)
     */
    private BigInteger id = null;


    /**
     * The class of the object. An arbitrary string.
     */
    private String clazz;
    private ObjectShape shape = ObjectShape.RECTANGLE;

    /**
     * The (x, y) coordinate of the object in pixels.
     */
    private double x;
    private double y;

    /**
     * The width and height of the object in pixels (defaults to 0).
     */
    private double width = 0;
    private double height = 0;

    /**
     * The rotation of the object in degrees clockwise (defaults to 0). (since 0.10)
     */
    private double rotation = 0f;

    private String template;

    /**
     * Whether the object is shown (1) or hidden (0). Defaults to 1. (since 0.9)
     */
    private boolean visible = true;

    /**
     * A reference to a tile (optional).
     * 
     * When the object has a gid set, then it is represented by the image of the
     * tile with that global ID. The image alignment currently depends on the
     * map orientation. In orthogonal orientation it's aligned to the
     * bottom-left while in isometric it's aligned to the bottom-center.
     */
    private int gid;// ObjectType.TILE
    private Tile tile;// ObjectType.TILE
    private List<Vector2f> points;// ObjectType.POLYGON, ObjectType.POLYLINE
    private TiledImageEntity image;// ObjectType.IMAGE
    private TiledObjectText textData;// ObjectType.TEXT

    public void removeFromLayer(){
        if(objectGroup!=null){
            objectGroup.remove(this);
        }
    }
 
    protected TiledObjectEntity() {
        // for serialization
    }

    public TiledObjectEntity(int id, double x, double y, double width, double height) {
        this(BigInteger.valueOf(id), x, y, width, height);
    }

    public TiledObjectEntity(BigInteger id, double x, double y, double width, double height) {
        this.id = id;
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    public TiledObjectEntity(int id, double x, double y, Tile tile) {
        this(BigInteger.valueOf(id), x, y, tile);
    }

    public TiledObjectEntity(BigInteger id, double x, double y, Tile tile) {
        this.id = id;
        this.x = x;
        this.y = y;
        this.tile = tile;
        if (tile != null) {
            this.gid = tile.getGid();
            this.width = tile.getWidth();
            this.height = tile.getHeight();
        }
        setShape(ObjectShape.TILE);
    }

    public TiledObjectLayer getObjectGroup() {
        return objectGroup;
    }

    public void setObjectGroup(TiledObjectLayer objectGroup) {
        if(Objects.equals(this.objectGroup, objectGroup)){
            return;
        }
        this.objectGroup = objectGroup;
        setUpdateNeeded();        
    }

    @Override
    public BigInteger getId() {
        return id;
    }

    public void setId(int id) {
        setId(BigInteger.valueOf(id));
    }

    public void setId(BigInteger id) {
        if(Objects.equals(this.id, id)){
            return;
        }
        this.id = id;
        setUpdateNeeded();
    }

  
 
    @Override
    public String getClazz() {
        return clazz;
    }

    public void setClazz(String clazz) {
        if(Objects.equals(this.clazz, clazz)){
            return;
        }
        this.clazz = clazz;
        setUpdateNeeded();        
    }

    public ObjectShape getShape() {
        return shape;
    }

    public void setShape(ObjectShape shape) {
        if(this.shape == shape){
            return;
        }
        this.shape = shape;
        setUpdateNeeded();        
    }
    
    @Override
    public double getX() {
        return x;
    }

    public void setX(double x) {
        if(this.x == x){
            return;
        }
        this.x = x;
        setUpdateNeeded();        
    }

    @Override
    public double getY() {
        return y;
    }

    public void setY(double y) {
        if(this.y == y){
            return;
        }
        this.y = y;
        setUpdateNeeded();        
    }

    @Override
    public double getWidth() {
        return width;
    }

    public void setWidth(double width) {
        if(this.width == width){
            return;
        }
        this.width = width;
        setUpdateNeeded();        

    }

    @Override
    public double getHeight() {
        return height;
    }

    public void setHeight(double height) {
        if(this.height == height){
            return;
        }
        this.height = height;
        setUpdateNeeded();        
    }

    public double getRotation() {
        return rotation;
    }

    public void setRotation(double rotation) {
        if(this.rotation == rotation){
            return;
        }
        this.rotation = rotation;
        setUpdateNeeded();        
    }

    public String getTemplate() {
        return template;
    }

    public void setTemplate(String template) {
        if(Objects.equals(this.template, template)){
            return;
        }
        this.template = template;
        setUpdateNeeded();        
    }

    public boolean isVisible() {
        return visible;
    }

    public void setVisible(boolean visible) {
        if(this.visible == visible){
            return;
        }
        this.visible = visible;
        setUpdateNeeded();        
    }

    public int getGid() {
        return gid;
    }

    public void setGid(int gid) {
        if(this.gid == gid){
            return;
        }
        this.gid = gid;
        setUpdateNeeded();        
    }

    public Tile getTile() {
        return tile;
    }

    public void setTile(Tile tile) {
        if(Objects.equals(this.tile, tile)){
            return;
        }
        this.tile = tile;
        this.gid = this.tile.getGid();
        setUpdateNeeded();      
        mergedProperties = null;  
    }

    public List<Vector2f> getPoints() {
        return points;
    }

    public void setPoints(List<Vector2f> points) {
        if(Objects.equals(this.points, points)){
            return;
        }
        this.points = points;
        setUpdateNeeded();        
    }

    public TiledImageEntity getImage() {
        return image;
    }

    public void setImage(TiledImageEntity image) {
        if(Objects.equals(this.image, image)){
            return;
        }
        this.image = image;
        setUpdateNeeded();        
    }

    public TiledObjectText getTextData() {
        return textData;
    }

    public void setTextData(TiledObjectText textData) {
        if(Objects.equals(this.textData, textData)){
            return;
        }
        this.textData = textData;
        setUpdateNeeded();        
    }

    @Override
    public void setUpdateNeeded() {
        super.setUpdateNeeded();
        // if (objectGroup != null) {
        //     objectGroup.setUpdateNeeded();
        // }
    }

    private Map<String, Object> mergedProperties = null;

    protected Map<String, Object> getMergedProperties(){
        if(shape==ObjectShape.TILE&&tile!=null){
            if(mergedProperties==null){
                mergedProperties = new HashMap<>();
                tile.copyPropertiesTo(mergedProperties); 
                for(String k:super.listPropertyKeys()){
                    mergedProperties.put(k, super.getProperty(k));
                }
            } 
            return mergedProperties;
        } else {
            mergedProperties = null;
            return properties;
        }
    }
 
    @Override
    public Object getProperty(String key) {
        Object v = getMergedProperties().get(key);
        return v;

    }
    

    @Override
    public Set<String> listPropertyKeys(){
        return getMergedProperties().keySet();
    }

 
    @Override
    public void setPropertiesUpdateNeeded(){
        mergedProperties = null;
        super.setPropertiesUpdateNeeded();
    }


 
    @Override
    public String toString() {
        return "MapObject [id=" + getId() + ", name=" + name + ", shape=" + shape
                + ", x=" + x + ", y=" + y + ", width=" + width + ", height=" + height + ", rotation=" + rotation
                + ", gid=" + gid + ", tile=" + tile + ", points=" + points + ", image=" + image + ", textData=" + textData + ", properties=" + listPropertyKeys() + "]";
    }




 
    
 
}
