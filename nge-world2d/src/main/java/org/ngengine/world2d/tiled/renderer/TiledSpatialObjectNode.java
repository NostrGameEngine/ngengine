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

package org.ngengine.world2d.tiled.renderer;

import com.jme3.math.FastMath;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;

import org.ngengine.world2d.tiled.core.TiledBase;
import org.ngengine.world2d.tiled.core.TiledMap;
import org.ngengine.world2d.tiled.core.entity.TiledObjectEntity;
import org.ngengine.world2d.tiled.core.entity.TiledTileEntity;
import org.ngengine.world2d.tiled.core.tileset.Tile;
import org.ngengine.world2d.tiled.enums.Orientation;
import org.ngengine.world2d.tiled.renderer.queue.YAxisComparator;

/**
 * Map-object visual that hosts a regular jME {@link Spatial}.
 *
 * <p>This is intentionally not a component. Tiled object properties select a
 * spatial asset, the renderer creates this node as the visual for that object,
 * and game code can retrieve it from the renderer like any other map object
 * spatial.</p>
 */
public class TiledSpatialObjectNode extends Node {
    public static final String MODEL_PROPERTY = "model";
    public static final String MODEL_PATH_PROPERTY = "model.path";
    public static final String SPATIAL_PROPERTY = "spatial";
    public static final String SPATIAL_PATH_PROPERTY = "spatial.path";
    public static final String JME_MODEL_PROPERTY = "jme.model";
    public static final String JME_SPATIAL_PROPERTY = "jme.spatial";

    private final TiledObjectEntity object;
    private final String spatialPath;
    private final Node spatialParent = new Node("TiledSpatialObjectParent");
    private final Quaternion orientationRotation = new Quaternion();
    private final Quaternion authoredRotation = new Quaternion();
    private final Quaternion finalRotation = new Quaternion();
    private final Vector3f tmpTranslation = new Vector3f();
    private final Vector3f tmpScale = new Vector3f(1f, 1f, 1f);
    private Spatial contentSpatial;

    public TiledSpatialObjectNode(TiledObjectEntity object, String spatialPath, Spatial contentSpatial) {
        super(object != null ? object.getName() : "TiledSpatialObject");
        this.object = object;
        this.spatialPath = spatialPath;
        attachChild(spatialParent);
        setContentSpatial(contentSpatial);
    }

    public TiledObjectEntity getObject() {
        return object;
    }

    public String getSpatialPath() {
        return spatialPath;
    }

    /**
     * Parent node used for map-orientation, authored rotation, offset, and scale.
     */
    public Node getSpatialParent() {
        return spatialParent;
    }

    public Spatial getContentSpatial() {
        return contentSpatial;
    }

    public void setContentSpatial(Spatial contentSpatial) {
        spatialParent.detachAllChildren();
        this.contentSpatial = contentSpatial;
        if (contentSpatial != null) {
            contentSpatial.setName("TiledSpatial:" + spatialPath);
            spatialParent.attachChild(contentSpatial);
        }
    }

    public boolean matches(TiledObjectEntity object) {
        return this.object == object && samePath(spatialPath, spatialPath(object));
    }

    public void configure(TiledMap map, float sortY, float sortOrder) {
        tmpTranslation.set(
                floatProperty(object, "model.offsetX", 0f),
                floatProperty(object, "model.offsetY", 0f),
                floatProperty(object, "model.offsetZ", 0f)
        );
        spatialParent.setLocalTranslation(tmpTranslation);

        orientationRotation.loadIdentity();
        if (!"none".equalsIgnoreCase(stringProperty(object, "model.orientation", "map")) && map != null) {
            applyMapOrientation(map.getOrientation(), orientationRotation);
        }
        authoredRotation.fromAngles(
                FastMath.DEG_TO_RAD * floatProperty(object, "model.rotationX", 0f),
                FastMath.DEG_TO_RAD * floatProperty(object, "model.rotationY", 0f),
                FastMath.DEG_TO_RAD * floatProperty(object, "model.rotationZ", 0f)
        );
        finalRotation.set(orientationRotation).multLocal(authoredRotation);
        spatialParent.setLocalRotation(finalRotation);

        float scale = floatProperty(object, "model.scale", 1f);
        tmpScale.set(
                scale * floatProperty(object, "model.scaleX", 1f),
                scale * floatProperty(object, "model.scaleY", 1f),
                scale * floatProperty(object, "model.scaleZ", 1f)
        );
        spatialParent.setLocalScale(tmpScale);

        applySortUserData(this, sortY, sortOrder);
    }

    public static boolean hasSpatial(TiledBase entry) {
        return spatialPath(entry) != null;
    }

    public static String spatialPath(TiledBase entry) {
        String path = stringProperty(entry, MODEL_PATH_PROPERTY);
        if (path == null) path = stringProperty(entry, MODEL_PROPERTY);
        if (path == null) path = stringProperty(entry, SPATIAL_PATH_PROPERTY);
        if (path == null) path = stringProperty(entry, SPATIAL_PROPERTY);
        if (path == null) path = stringProperty(entry, JME_MODEL_PROPERTY);
        if (path == null) path = stringProperty(entry, JME_SPATIAL_PROPERTY);
        if (path == null) {
            Tile tile = tileOf(entry);
            if (tile != null && tile != entry) {
                path = spatialPath(tile);
            }
        }
        return path;
    }

    public static void applySortUserData(Spatial spatial, float sortY, float sortOrder) {
        if (spatial == null) {
            return;
        }
        spatial.setUserData(YAxisComparator.SORT_Y_USER_DATA, sortY);
        spatial.setUserData(YAxisComparator.SORT_ORDER_USER_DATA, sortOrder);
        if (spatial instanceof Node) {
            for (Spatial child : ((Node) spatial).getChildren()) {
                applySortUserData(child, sortY, sortOrder);
            }
        }
    }

    private static void applyMapOrientation(Orientation orientation, Quaternion out) {
        if (orientation == Orientation.ISOMETRIC) {
            out.fromAngles(0f, -FastMath.QUARTER_PI, 0f);
        }
    }

    private static Tile tileOf(TiledBase entry) {
        if (entry instanceof TiledObjectEntity) {
            return ((TiledObjectEntity) entry).getTile();
        }
        if (entry instanceof TiledTileEntity) {
            return ((TiledTileEntity) entry).getTile();
        }
        return entry instanceof Tile ? (Tile) entry : null;
    }

    private static String stringProperty(TiledBase entry, String key) {
        if (entry == null) {
            return null;
        }
        Object value = propertyValue(entry, key);
        if (value == null) {
            return null;
        }
        String string = String.valueOf(value).trim();
        return string.isEmpty() ? null : string;
    }

    private static String stringProperty(TiledBase entry, String key, String defaultValue) {
        String value = stringProperty(entry, key);
        return value != null ? value : defaultValue;
    }

    private static float floatProperty(TiledBase entry, String key, float defaultValue) {
        Object value = propertyValue(entry, key);
        if (value instanceof Number) {
            return ((Number) value).floatValue();
        }
        if (value == null) {
            return defaultValue;
        }
        try {
            return Float.parseFloat(String.valueOf(value).trim());
        } catch (NumberFormatException ex) {
            return defaultValue;
        }
    }

    private static Object propertyValue(TiledBase entry, String key) {
        if (entry == null) {
            return null;
        }
        Object value = entry.getProperty(key);
        if (value != null) {
            return value;
        }
        Tile tile = tileOf(entry);
        return tile != null && tile != entry ? tile.getProperty(key) : null;
    }

    private static boolean samePath(String a, String b) {
        return a == null ? b == null : a.equals(b);
    }
}
