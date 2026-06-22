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

package org.ngengine.world2d.tiled.components;

import java.util.logging.Level;
import java.util.logging.Logger;

import com.jme3.asset.AssetManager;
import com.jme3.math.FastMath;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;

import org.ngengine.components.AbstractComponent;
import org.ngengine.components.ComponentManager;
import org.ngengine.world2d.tiled.components.fragments.TiledEntityLifecycleFragment;
import org.ngengine.world2d.tiled.components.fragments.TiledEntityRenderFragment;
import org.ngengine.world2d.tiled.core.TiledBase;
import org.ngengine.world2d.tiled.core.TiledMap;
import org.ngengine.world2d.tiled.core.entity.TiledObjectEntity;
import org.ngengine.world2d.tiled.core.entity.TiledTileEntity;
import org.ngengine.world2d.tiled.core.tileset.Tile;
import org.ngengine.world2d.tiled.enums.Orientation;
import org.ngengine.world2d.tiled.renderer.queue.YAxisComparator;
import org.ngengine.world2d.tiled.util.CoordinateSystem;

/**
 * Renders a JME {@link Spatial} for a Tiled entity whose object or tile defines
 * a model property.
 *
 * <p>The component creates a positioned root node and an orientation parent node.
 * Game code can attach extra children or mutate the loaded model through
 * {@link #getRootNode()}, {@link #getModelParent()}, and
 * {@link #getModelSpatial()}.</p>
 */
public class TiledModelComponent extends AbstractComponent
        implements TiledEntityRenderFragment, TiledEntityLifecycleFragment {
    private static final Logger log = Logger.getLogger(TiledModelComponent.class.getName());

    public static final String MODEL_PROPERTY = "model";
    public static final String MODEL_PATH_PROPERTY = "model.path";
    public static final String SPATIAL_PROPERTY = "spatial";
    public static final String SPATIAL_PATH_PROPERTY = "spatial.path";
    public static final String JME_MODEL_PROPERTY = "jme.model";
    public static final String JME_SPATIAL_PROPERTY = "jme.spatial";

    private final Node rootNode = new Node("TiledModelRoot");
    private final Node modelParent = new Node("TiledModelParent");
    private final Quaternion orientationRotation = new Quaternion();
    private final Quaternion authoredRotation = new Quaternion();
    private final Quaternion finalRotation = new Quaternion();
    private final Vector2f tmpTranslation2d = new Vector2f();
    private final Vector3f tmpTranslation = new Vector3f();
    private final Vector3f tmpScale = new Vector3f(1f, 1f, 1f);

    private Spatial modelSpatial;
    private String loadedPath;

    public TiledModelComponent() {
        rootNode.attachChild(modelParent);
    }

    public static boolean hasModel(TiledBase entry) {
        return modelPath(entry) != null;
    }

    public static String modelPath(TiledBase entry) {
        String path = stringProperty(entry, MODEL_PATH_PROPERTY);
        if (path == null) path = stringProperty(entry, MODEL_PROPERTY);
        if (path == null) path = stringProperty(entry, SPATIAL_PATH_PROPERTY);
        if (path == null) path = stringProperty(entry, SPATIAL_PROPERTY);
        if (path == null) path = stringProperty(entry, JME_MODEL_PROPERTY);
        if (path == null) path = stringProperty(entry, JME_SPATIAL_PROPERTY);
        if (path == null) {
            Tile tile = tileOf(entry);
            if (tile != null && tile != entry) {
                path = modelPath(tile);
            }
        }
        return path;
    }

    public Node getRootNode() {
        return rootNode;
    }

    public Node getModelParent() {
        return modelParent;
    }

    public Spatial getModelSpatial() {
        return modelSpatial;
    }

    @Override
    protected void onEnable(ComponentManager mng, boolean firstTime) {
    }

    @Override
    protected void onDisable(ComponentManager mng) {
        detachModel();
    }

    @Override
    protected void onDetached() {
        detachModel();
    }

    @Override
    public void onTiledEntityInitialize(ComponentManager mng, TiledBase entry) {
    }

    @Override
    public void onTiledEntityCleanup(ComponentManager mng, TiledBase entry) {
        detachModel();
    }

    @Override
    public void onTiledEntryRender(ComponentManager mng, TiledBase entry, Spatial renderedSpatial) {
        String path = modelPath(entry);
        if (path == null || !isVisible(entry)) {
            detachModel();
            return;
        }
        AssetManager assets = mng.getInstanceOf(AssetManager.class);
        if (assets == null) {
            return;
        }
        Spatial loaded = ensureLoaded(assets, path);
        if (loaded == null) {
            detachModel();
            return;
        }

        Node attachParent = attachParent(renderedSpatial);
        if (attachParent == null) {
            detachModel();
            return;
        }
        if (rootNode.getParent() != attachParent) {
            rootNode.removeFromParent();
            attachParent.attachChild(rootNode);
        }
        updateTransform(mng, entry, renderedSpatial);
    }

    private Spatial ensureLoaded(AssetManager assets, String path) {
        if (path.equals(loadedPath) && modelSpatial != null) {
            return modelSpatial;
        }
        modelParent.detachAllChildren();
        modelSpatial = null;
        loadedPath = null;
        try {
            modelSpatial = assets.loadModel(path);
        } catch (RuntimeException ex) {
            log.log(Level.WARNING, "Failed to load tiled model: " + path, ex);
            return null;
        }
        if (modelSpatial == null) {
            return null;
        }
        modelSpatial.setName("TiledModel:" + path);
        modelParent.attachChild(modelSpatial);
        loadedPath = path;
        return modelSpatial;
    }

    private void updateTransform(ComponentManager mng, TiledBase entry, Spatial renderedSpatial) {
        TiledMap map = mng.getInstanceOf(TiledMap.class);
        CoordinateSystem coords = mng.getInstanceOf(CoordinateSystem.class);
        Vector3f localTranslation = renderedSpatial != null && !(renderedSpatial instanceof Node)
                ? renderedSpatial.getLocalTranslation()
                : null;
        if (localTranslation != null) {
            rootNode.setLocalTranslation(localTranslation);
        } else if (coords != null) {
            coords.getPositionInGridSpace(entry, tmpTranslation2d);
            coords.gridToWorldSpace(tmpTranslation2d.x, tmpTranslation2d.y, tmpTranslation2d);
            rootNode.setLocalTranslation(tmpTranslation2d.x, 0f, tmpTranslation2d.y);
        }

        float sortY = sortNumber(renderedSpatial, YAxisComparator.SORT_Y_USER_DATA, rootNode.getLocalTranslation().y);
        float sortOrder = sortNumber(renderedSpatial, YAxisComparator.SORT_ORDER_USER_DATA, 0f);
        applySortUserData(rootNode, sortY, sortOrder);
        rootNode.setQueueBucket(RenderQueue.Bucket.Opaque);

        tmpTranslation.set(
                floatProperty(entry, "model.offsetX", 0f),
                floatProperty(entry, "model.offsetY", 0f),
                floatProperty(entry, "model.offsetZ", 0f)
        );
        modelParent.setLocalTranslation(tmpTranslation);

        orientationRotation.loadIdentity();
        if (!"none".equalsIgnoreCase(stringProperty(entry, "model.orientation", "map")) && map != null) {
            applyMapOrientation(map.getOrientation(), orientationRotation);
        }
        authoredRotation.fromAngles(
                FastMath.DEG_TO_RAD * floatProperty(entry, "model.rotationX", 0f),
                FastMath.DEG_TO_RAD * floatProperty(entry, "model.rotationY", 0f),
                FastMath.DEG_TO_RAD * floatProperty(entry, "model.rotationZ", 0f)
        );
        finalRotation.set(orientationRotation).multLocal(authoredRotation);
        modelParent.setLocalRotation(finalRotation);

        float scale = floatProperty(entry, "model.scale", 1f);
        tmpScale.set(
                scale * floatProperty(entry, "model.scaleX", 1f),
                scale * floatProperty(entry, "model.scaleY", 1f),
                scale * floatProperty(entry, "model.scaleZ", 1f)
        );
        modelParent.setLocalScale(tmpScale);
    }

    private static void applyMapOrientation(Orientation orientation, Quaternion out) {
        if (orientation == Orientation.ISOMETRIC) {
            out.fromAngles(0f, -FastMath.QUARTER_PI, 0f);
        }
    }

    private static Node attachParent(Spatial renderedSpatial) {
        if (renderedSpatial == null) {
            return null;
        }
        if (renderedSpatial instanceof Node) {
            return (Node) renderedSpatial;
        }
        return renderedSpatial.getParent();
    }

    private void detachModel() {
        rootNode.removeFromParent();
    }

    private static boolean isVisible(TiledBase entry) {
        if (entry instanceof TiledObjectEntity) {
            return ((TiledObjectEntity) entry).isVisible();
        }
        return true;
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

    private static float sortNumber(Spatial spatial, String key, float defaultValue) {
        Number value = spatial != null ? spatial.getUserData(key) : null;
        return value != null ? value.floatValue() : defaultValue;
    }

    private static void applySortUserData(Spatial spatial, float sortY, float sortOrder) {
        spatial.setUserData(YAxisComparator.SORT_Y_USER_DATA, sortY);
        spatial.setUserData(YAxisComparator.SORT_ORDER_USER_DATA, sortOrder);
        if (spatial instanceof Geometry) {
            return;
        }
        if (spatial instanceof Node) {
            for (Spatial child : ((Node) spatial).getChildren()) {
                applySortUserData(child, sortY, sortOrder);
            }
        }
    }
}
