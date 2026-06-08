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

package org.ngengine.world2d.tiled.app;

import com.jme3.app.Application;
import com.jme3.app.FlyCamAppState;
import com.jme3.app.SimpleApplication;
import com.jme3.app.state.BaseAppState;
import com.jme3.asset.AssetManager;
import com.jme3.input.InputManager;
import com.jme3.input.Joystick;
import com.jme3.input.KeyInput;
import com.jme3.input.MouseInput;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.AnalogListener;
import com.jme3.input.controls.KeyTrigger;
import com.jme3.input.controls.MouseAxisTrigger;
import com.jme3.input.controls.MouseButtonTrigger;
import com.jme3.material.Material;
import com.jme3.math.*;
import com.jme3.renderer.Camera;
import com.jme3.renderer.ViewPort;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.util.TempVars;

import org.ngengine.world2d.tiled.core.TiledLayer;
import org.ngengine.world2d.tiled.core.TiledLayerGroup;
import org.ngengine.world2d.tiled.core.TiledBase;
import org.ngengine.world2d.tiled.core.TiledEntity;
import org.ngengine.world2d.tiled.math2d.Point;
import org.ngengine.world2d.tiled.core.TiledMap;
import org.ngengine.world2d.tiled.enums.ZoomMode;
import org.ngengine.world2d.tiled.renderer.*;
import org.ngengine.world2d.tiled.renderer.MapRenderer.Listener;
import org.ngengine.world2d.tiled.renderer.factory.DefaultMaterialFactory;
import org.ngengine.world2d.tiled.renderer.factory.DefaultMeshFactory;
import org.ngengine.world2d.tiled.renderer.factory.DefaultSpriteFactory;
import org.ngengine.world2d.tiled.renderer.factory.MaterialFactory;
import org.ngengine.world2d.tiled.renderer.queue.YAxisComparator;
import java.util.logging.Logger;

/**
 * TiledMapAppState manage a {@link MapRenderer} for {@link TiledMap} and render it.
 * 
 * @author yanmaoyuan
 */
public class TiledMapAppState extends BaseAppState implements AnalogListener, ActionListener, MapRenderer.Listener {

    private final static Logger logger = Logger.getLogger(TiledMapAppState.class.getName());
    public static final String LEFT = "left";
    public static final String RIGHT = "right";
    public static final String UP = "up";
    public static final String DOWN = "down";
    public static final String DRAG = "dragAndDrop";
    public static final String ZOOM_IN = "zoom_in";
    public static final String ZOOM_OUT = "zoom_out";
    public static final String GRID = "grid";
    public static final String PARALLAX = "parallax";

    private static final String[] MAPPINGS = new String[] { LEFT, RIGHT, UP, DOWN, DRAG, ZOOM_IN, ZOOM_OUT, GRID, PARALLAX };

    // Tiled Map
    private TiledMap map;
    private MapRenderer mapRenderer;

    // The rootNode
    private final Node rootNode;

    // The grid
    private boolean isGridVisible = false;
    private final Node gridVisual;// for render grid
    private Material gridMaterial;// for render grid
    private boolean isGridUpdated = true;

    private boolean isCursorVisible = true;
    private Point currentTile;
    private Spatial gridCursor;
    private Material cursorMaterial;
    private boolean isCursorUpdated = true;

    // The parallax
    private boolean isParallaxEnabled = true;

    // The map scale, user can zoom in/out the map from 10% to 400%
    private float mapScale;
    private static final float MAP_SCALE_MIN = 0.1f;
    private static final float MAP_SCALE_MAX = 4f;

    // The camera
    private Camera cam;
    private ViewPort viewPort;
    private InputManager inputManager;
    private MaterialFactory materialFactory;

    // The
    protected float moveSpeed = 10f;// in tiles
    protected float zoomSpeed = 1f;// in tiles

    private ZoomMode zoomMode = ZoomMode.CAMERA;

    // variables used to drag map
    private final Vector3f startLoc = new Vector3f();
    private final Vector2f startPos = new Vector2f();

    // variables used to move camera
    private final Vector3f vel = new Vector3f();
    private final Vector3f pos = new Vector3f();

    /**
     * Constructor
     */
    public TiledMapAppState() {
        mapScale = 1f;

        rootNode = new Node("Tiled Map Root");

        gridVisual = new Node("Tiled Map Grid");
        gridVisual.setLocalTranslation(0f, 999f, 0f);
    }

    @Override
    protected void initialize(Application app) {

        inputManager = app.getInputManager();
        AssetManager assetManager = app.getAssetManager();
        materialFactory = new DefaultMaterialFactory(assetManager);

        viewPort = app.getViewPort();
        cam = app.getCamera();

        // sort by y-axis
        viewPort.getQueue().setGeometryComparator(RenderQueue.Bucket.Opaque, new YAxisComparator());

        float near = -1000f;
        float far = 1000f;
        float halfWidth = cam.getWidth() * 0.5f;
        float halfHeight = cam.getHeight() * 0.5f;
        cam.setFrustum(near, far, -halfWidth, halfWidth, halfHeight, -halfHeight);
        cam.setParallelProjection(true);
        cam.lookAtDirection(new Vector3f(0f, -1f, 0f), new Vector3f(0f, 0f, -1f));
        logger.info("cam: " + cam);

        gridMaterial = materialFactory.newMaterial();
        materialFactory.setColor(gridMaterial, ColorRGBA.DarkGray);
        cursorMaterial = materialFactory.newMaterial();
        cursorMaterial.setColor(MaterialConst.COLOR, MaterialConst.CURSOR_AVAILABLE_COLOR);
    }

    @Override
    protected void cleanup(Application app) {
        rootNode.detachAllChildren();
    }

    @Override
    protected void onEnable() {
        ((SimpleApplication) getApplication()).getRootNode().attachChild(rootNode);

        // disable flyCamAppState
        FlyCamAppState flyCamAppState = getStateManager().getState(
                FlyCamAppState.class);
        if (flyCamAppState != null) {
            flyCamAppState.setEnabled(false);
        }

        registerWithInput();
    }

    @Override
    protected void onDisable() {
        rootNode.removeFromParent();

        // enable flyCamAppState
        FlyCamAppState flyCamAppState = getStateManager().getState(
                FlyCamAppState.class);
        if (flyCamAppState != null) {
            flyCamAppState.setEnabled(true);
        }

        unregisterInput();
    }

    @Override
    public void update(float tpf) {
        if (mapRenderer != null) {
            mapRenderer.render(this,tpf);

            if (isGridUpdated) {
                createGird();
            }

            if (isCursorUpdated) {
                createCursor();
            }

            moveCursor();
        }
    }

    private void createGird() {
        gridVisual.getChildren().clear();
        mapRenderer.renderGrid(gridVisual, gridMaterial);
        if (gridVisual.getParent() != null) {
            gridVisual.removeFromParent();
            mapRenderer.getRootNode().attachChild(gridVisual);
        }
        isGridUpdated = false;
    }

    private void createCursor() {
        // remove old cursor
        if (gridCursor != null) {
            gridCursor.removeFromParent();
        }

        currentTile = null;
        cursorMaterial.setColor(MaterialConst.COLOR, MaterialConst.CURSOR_AVAILABLE_COLOR);
        gridCursor = mapRenderer.createTileGrid(cursorMaterial);
        mapRenderer.getRootNode().attachChild(gridCursor);
        isCursorUpdated = false;
    }
    /**
     * Set map. It will instance a new MapRenderer and create visual parts for this map.
     * 
     * @param map the tiled map
     */
    public void setMap(TiledMap map) {
        if (!isInitialized()) {
            throw new IllegalStateException("TiledMapAppState is not initialized.");
        }
        if (map == null) {
            return;
        }

        rootNode.detachAllChildren();
        
        viewPort.setBackgroundColor(map.getBackgroundColor());

        if (this.map != map) {
            this.map = map;
        }

        mapRenderer = MapRenderer.create(map, 32, new Node());

        // new sprite factory. the materialFactory is set in initialize()
        DefaultSpriteFactory spriteFactory = new DefaultSpriteFactory();
        spriteFactory.setMeshFactory(new DefaultMeshFactory(map));
        spriteFactory.setMaterialFactory(materialFactory);

        mapRenderer.setSpriteFactory(spriteFactory);

        // create the visual part for the map
        rootNode.attachChild(mapRenderer.getRootNode());

        Vector2f loc = mapRenderer.gridToWorldSpace(map.getParallaxOriginX(), map.getParallaxOriginY());
        cam.setLocation(new Vector3f(loc.x, 0, loc.y));
        calculateMapParallax();

        if (gridMaterial != null) {
            createGird();
        } else {
            isGridUpdated = true;
        }

        if (cursorMaterial != null) {
            createCursor();
        } else {
            isCursorUpdated = true;
        }
    }

    public TiledMap getMap() {
        return map;
    }

    public MapRenderer getMapRenderer() {
        return mapRenderer;
    }

    public void moveToTile(float x, float y) {
        Vector2f tilePos = mapRenderer.tileToWorldSpace(x, y).multLocal(mapScale);
        cam.setLocation(new Vector3f(tilePos.x, cam.getLocation().y, tilePos.y));
        calculateMapParallax();
    }

    public void moveToPixel(float x, float y) {
        Vector2f pixelPos = mapRenderer.gridToWorldSpace(x, y).multLocal(mapScale);
        cam.setLocation(new Vector3f(pixelPos.x, cam.getLocation().y, pixelPos.y));
        calculateMapParallax();
    }

    private void calculateMapParallax() {
        if (mapRenderer == null) {
            return;
        }

        TempVars vars = TempVars.get();
        Vector2f distance = vars.vect2d;

        if (isParallaxEnabled) {
            // current position of the camera in the map
            distance.set(cam.getLocation().x, cam.getLocation().z).divideLocal(mapScale);
            // calculate the distance between the camera and the parallax origin
            distance.subtractLocal(map.getParallaxOriginX(), map.getParallaxOriginY());
        } else {
            distance.set(0, 0);
        }

        applyParallax(distance);

        vars.release();
    }

    private void applyParallax(Vector2f distance) {
        for (TiledLayer layer : map.getLayersFlat()) {
            if (!layer.isVisible()) {
                continue;
            }
            applyParallax(layer, distance);
        }
    }

    private void applyParallax(TiledLayer layer, Vector2f distance) {
        if (!layer.isVisible()) {
            return;
        }

        if (layer instanceof TiledLayerGroup) {
            for (TiledLayer child : ((TiledLayerGroup) layer).getLayers()) {
                applyParallax(child, distance);
            }
        } else {
            // When the camera moves, the layer moves in relation to the camera by a factor of the parallax scrolling factor.
            // As move mapVisual means move the map, so we need to move the layer in the opposite direction, 1.0-parallaxFactor
            float x = (float)((1.0 - layer.getRenderParallaxX()) * distance.x);
            float y = (float)((1.0 - layer.getRenderParallaxY()) * distance.y);

            Node layerNode = mapRenderer.getLayerNode(layer);
            float z = layerNode.getLocalTranslation().y;
            layerNode.setLocalTranslation(x, z, y);
        }
    }

    public void setMapScale(float scale) {
        mapScale = scale;
        rootNode.setLocalScale(mapScale, 1, mapScale);
    }

    public float getMapScale() {
        return mapScale;
    }

    /**
     * Sets the move speed. The speed is given in world units per second.
     * 
     * @param moveSpeed move speed
     */
    public void setMoveSpeed(float moveSpeed) {
        this.moveSpeed = moveSpeed;
    }

    /**
     * Gets the move speed. The speed is given in world units per second.
     * 
     * @return moveSpeed
     */
    public float getMoveSpeed() {
        return moveSpeed;
    }

    public void registerWithInput() {
        if (inputManager == null) {
            return;
        }

        // keyboard only WASD for movement
        inputManager.addMapping(LEFT, new KeyTrigger(KeyInput.KEY_A), new KeyTrigger(KeyInput.KEY_LEFT));
        inputManager.addMapping(RIGHT, new KeyTrigger(KeyInput.KEY_D), new KeyTrigger(KeyInput.KEY_RIGHT));
        inputManager.addMapping(UP, new KeyTrigger(KeyInput.KEY_W), new KeyTrigger(KeyInput.KEY_UP));
        inputManager.addMapping(DOWN, new KeyTrigger(KeyInput.KEY_S), new KeyTrigger(KeyInput.KEY_DOWN));

        inputManager.addMapping(ZOOM_IN, new MouseAxisTrigger(MouseInput.AXIS_WHEEL, false));
        inputManager.addMapping(ZOOM_OUT, new MouseAxisTrigger(MouseInput.AXIS_WHEEL, true));
        inputManager.addMapping(DRAG, new MouseButtonTrigger(MouseInput.BUTTON_LEFT));

        inputManager.addMapping(GRID, new KeyTrigger(KeyInput.KEY_G));// add key mapping to show/hide grid
        inputManager.addMapping(PARALLAX, new KeyTrigger(KeyInput.KEY_P));// add key mapping to enable/disable parallax

        inputManager.addListener(this, MAPPINGS);

        Joystick[] joysticks = inputManager.getJoysticks();
        if (joysticks != null) {
            for (Joystick joystick : joysticks) {
                // Make the left stick move
                joystick.getXAxis().assignAxis(RIGHT, LEFT);
                joystick.getYAxis().assignAxis(DOWN, UP);
            }
        }
    }

    public void unregisterInput() {

        if (inputManager == null) {
            return;
        }

        for (String s : MAPPINGS) {
            if (inputManager.hasMapping(s)) {
                inputManager.deleteMapping(s);
            }
        }

        inputManager.removeListener(this);
    }

    public Point getCameraTileCoordinate() {
        Vector2f center = getCameraPixelCoordinate();
        return getMapRenderer().gridToTile(center.x, center.y);
    }

    public Vector2f getCameraPixelCoordinate() {
        return getCameraScreenCoordinate().divideLocal(mapScale);
    }

    public Vector2f getCameraScreenCoordinate() {
        return new Vector2f(cam.getLocation().x, cam.getLocation().z);
    }

    /**
     * Get the cursor tile coordinate in the map
     *
     * @return The tile coordinate of the cursor
     */
    public Point getCursorTileCoordinate(Vector2f cursor) {
        Vector2f pixel = getCursorPixelCoordinate(cursor);
        return getMapRenderer().getCoordinateSystem().worldToTile(pixel.x, pixel.y);
    }

    /**
     * Get the cursor pixel coordinate in the map
     *
     * @return The pixel coordinate of the cursor
     */
    public Vector2f getCursorPixelCoordinate(Vector2f cursor) {
        Vector3f worldPos = cam.getWorldCoordinates(cursor, 0);
        return new Vector2f(worldPos.x, worldPos.z).divideLocal(mapScale);
    }

    /**
     * move camera
     * 
     * @param value move value
     * @param sideways move up-down or right-left
     */
    public void move(float value, boolean sideways) {
        pos.set(cam.getLocation());

        if (sideways) {
            vel.set(1f, 0f, 0f);
        } else {
            vel.set(0f, 0f, 1f);
        }
        vel.multLocal(value * moveSpeed * map.getTileWidth() * mapScale);

        pos.addLocal(vel);

        cam.setLocation(pos);
        calculateMapParallax();
    }

    /**
     * drag camera
     * 
     */
    private void drag() {
        TempVars vars = TempVars.get();
        // record the mouse position
        Vector2f stopPos = vars.vect2d.set(inputManager.getCursorPosition());
        Vector3f from = cam.getWorldCoordinates(startPos, 0, vars.vect1);
        Vector3f to = cam.getWorldCoordinates(stopPos, 0, vars.vect2);
        Vector3f dir = from.subtractLocal(to);

        // move camera
        cam.setLocation(new Vector3f(startLoc.x + dir.x, cam.getLocation().y, startLoc.z + dir.z));
        vars.release();

        calculateMapParallax();
    }

    private void moveCursor() {
        if (gridCursor != null) {
            Vector2f input = inputManager.getCursorPosition();
            Point cursor = getCursorTileCoordinate(input);
            if (currentTile == null) {
                currentTile = cursor;
            } else if (!currentTile.equals(cursor)) {
                currentTile.set(cursor.getX(), cursor.getY());
            } else {
                return;
            }
            Vector2f loc = mapRenderer.tileToWorldSpace(cursor.getX(), cursor.getY());
            gridCursor.setLocalTranslation(loc.x, 1000f, loc.y);
            if (map.contains(cursor.getX(), cursor.getY())) {
                cursorMaterial.setColor(MaterialConst.COLOR, MaterialConst.CURSOR_AVAILABLE_COLOR);
            } else {
                cursorMaterial.setColor(MaterialConst.COLOR, MaterialConst.CURSOR_UNAVAILABLE_COLOR);
            }
        }
    }

    public void setZoomMode(ZoomMode zoomMode) {
        this.zoomMode = zoomMode;
    }

    public ZoomMode getZoomMode() {
        return zoomMode;
    }

    public void zoom(float value) {
        float zoomValue = zoomSpeed * value;
        switch (zoomMode) {
            case MAP:
                zoomMap(zoomValue);
                break;
            case CAMERA:
                zoomCamera(value);
                break;
            default:
                break;
        }
    }

    public void zoomMap(float value) {
        value /= 100;// zoom value is too large
        value *= mapScale;// scale the zoom value, make it more smooth
        value = FastMath.clamp(value, MAP_SCALE_MIN - mapScale, MAP_SCALE_MAX - mapScale);// keep the map scale in a reasonable range
        setMapScale(mapScale + value);
    }

    /**
     * zoom camera
     * 
     * @param value zoom value
     */
    public void zoomCamera(float value) {
        // calculate camera frustum
        if (cam.isParallelProjection()) {
            float top = cam.getFrustumTop();
            float bottom = cam.getFrustumBottom();
            float left = cam.getFrustumLeft();
            float right = cam.getFrustumRight();
            float near = cam.getFrustumNear();
            float far = cam.getFrustumFar();
            // keep the near and far plane

            float height = top - bottom;
            float width = right - left;
            float aspect = width / height;

            if (height + value < 1f) {
                // too small
                return;
            }

            float newHeight = height + value;
            float newWidth = newHeight * aspect;
            float newTop = top + (newHeight - height) * 0.5f;
            float newBottom = bottom - (newHeight - height) * 0.5f;
            float newLeft = left - (newWidth - width) * 0.5f;
            float newRight = right + (newWidth - width) * 0.5f;
            cam.setFrustum(near, far, newLeft, newRight, newTop, newBottom);
        } else {
            float fov = cam.getFov();

            fov = FastMath.clamp(fov + value, 15f, 179f);
            cam.setFov(fov);
        }
    }

    public void onAnalog(String name, float value, float tpf) {
        if (mapRenderer == null) {
            return;
        }
        switch (name) {
            case UP:
                move(-tpf, false);
                break;
            case DOWN:
                move(tpf, false);
                break;
            case LEFT:
                move(-tpf, true);
                break;
            case RIGHT:
                move(tpf, true);
                break;
            case DRAG:
                drag();
                break;
            case ZOOM_IN:
                zoom(value);
                break;
            case ZOOM_OUT:
                zoom(-value);
                break;
            default:
                break;
        }
    }

    @Override
    public void onAction(String name, boolean isPressed, float tpf) {
        if (DRAG.equals(name)) {
            if (isPressed) {
                // record the mouse position
                startPos.set(inputManager.getCursorPosition());
                startLoc.set(cam.getLocation());
            } else {
                drag();
            }
        } else if (GRID.equals(name) && isPressed) {
            isGridVisible = !isGridVisible;
            toggleGrid();
        } else if (PARALLAX.equals(name) && isPressed) {
            isParallaxEnabled = !isParallaxEnabled;
            calculateMapParallax();
        }
    }

    public boolean isGridVisible() {
        return isGridVisible;
    }

    public void setGridVisible(boolean visible) {
        isGridVisible = visible;

        if (gridVisual != null) {
            if (isGridVisible) {
                if (map != null && mapRenderer.getRootNode() != null) {
                    mapRenderer.getRootNode().attachChild(gridVisual);
                }
            } else {
                gridVisual.removeFromParent();
            }
        }
    }

    public boolean isCursorVisible() {
        return isCursorVisible;
    }

    public void setCursorVisible(boolean visible) {
        isCursorVisible = visible;
        if (gridCursor != null) {
            if (isCursorVisible && map != null && mapRenderer.getRootNode() != null) {
                mapRenderer.getRootNode().attachChild(gridCursor);
            } else {
                gridCursor.removeFromParent();
            }
        }
    }

    public boolean isParallaxEnabled() {
        return isParallaxEnabled;
    }

    public void setParallaxEnabled(boolean enabled) {
        isParallaxEnabled = enabled;
        calculateMapParallax();
    }

    /**
     * show/hide the grid
     */
    private void toggleGrid() {
        if (isGridVisible) {
            if (map != null && mapRenderer.getRootNode() != null) {
                mapRenderer.getRootNode().attachChild(gridVisual);
            }
        } else {
            gridVisual.removeFromParent();
        }
    }

    @Override
    public void beforeMapRender(float tpf, TiledMap map) {
       
    }

    @Override
    public void afterMapRender(float tpf, TiledMap map, Spatial visual) {
 
    }

    @Override
    public void beforeEntityRender(float tpf, TiledMap map, TiledLayer layer, TiledEntity tile) {
         
    }

 
    @Override
    public void beforeLayerRender(float tpf, TiledMap map, TiledLayer layer) {
        
    }

    @Override
    public void afterLayerRender(float tpf, TiledMap map, TiledLayer layer, Spatial visual) {
    
    }

    @Override
    public void afterEntityRender(float tpf, TiledMap map, TiledLayer layer, TiledEntity entry, Spatial visual) {
   
    }

   
  
 
}
