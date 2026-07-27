/**
 * Copyright (c) 2025-2026, Nostr Game Engine
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the conditions in the project
 * license are met.
 */
package org.ngengine.world2d.tiled.components;

import org.ngengine.components.AbstractComponent;
import org.ngengine.components.ComponentManager;
import org.ngengine.world2d.box2d.TiledPhysicsComponent;
import org.ngengine.world2d.tiled.components.fragments.TiledEntityLogicFragment;
import org.ngengine.world2d.tiled.core.TiledBase;
import org.ngengine.world2d.tiled.core.entity.TiledObjectEntity;
import org.ngengine.world2d.tiled.util.CoordinateSystem;

import com.jme3.app.Application;
import com.jme3.audio.AudioListenerState;
import com.jme3.audio.Listener;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.jme3.renderer.Camera;
import org.jbox2d.common.Vec2;

/**
 * Anchors the application audio listener to a Tiled object in physics space.
 *
 * <p>This component is intended for the local player. While enabled it disables
 * jMonkeyEngine's camera-driven {@link AudioListenerState}, keeps the listener
 * location on the entity's 2D physics plane, and retains the scene camera's
 * orientation for stereo panning.</p>
 */
public class TiledAudioListenerComponent extends AbstractComponent
        implements TiledEntityLogicFragment {
    private final Vector2f gridPosition = new Vector2f();
    private final Vector2f physicsPosition = new Vector2f();
    private final Vector3f listenerPosition = new Vector3f();
    private final Vector3f previousPosition = new Vector3f();
    private final Vector3f listenerVelocity = new Vector3f();
    private AudioListenerState cameraListenerState;
    private boolean restoreCameraListenerState;
    private boolean positionInitialized;

    @Override
    protected void onEnable(ComponentManager mng, boolean firstTime) {
        Application app = getInstanceOf(Application.class);
        cameraListenerState = app != null
            ? app.getStateManager().getState(AudioListenerState.class)
            : null;
        restoreCameraListenerState = cameraListenerState != null
            && cameraListenerState.isEnabled();
        if (cameraListenerState != null) {
            cameraListenerState.setEnabled(false);
        }
        positionInitialized = false;
    }

    @Override
    protected void onDisable(ComponentManager mng) {
        if (cameraListenerState != null && restoreCameraListenerState) {
            cameraListenerState.setEnabled(true);
        }
        cameraListenerState = null;
        restoreCameraListenerState = false;
        positionInitialized = false;
    }

    @Override
    public void onTiledEntityLogicUpdate(
            ComponentManager mng,
            float tpf,
            TiledBase entry) {
        TiledObjectEntity entity = getInstanceOf(TiledObjectEntity.class);
        CoordinateSystem coordinates = getInstanceOf(CoordinateSystem.class);
        Application app = getInstanceOf(Application.class);
        Listener listener = app != null ? app.getListener() : null;
        if (entity == null || coordinates == null || listener == null) {
            return;
        }

        TiledPhysicsComponent physics = getInstanceOf(TiledPhysicsComponent.class);
        if (physics != null && physics.getBody() != null) {
            Vec2 center = physics.getBody().getWorldCenter();
            physicsPosition.set(center.x, center.y);
        } else {
            coordinates.getCenterInGridSpace(entity, gridPosition);
            coordinates.gridToWorldSpace(
                gridPosition.x,
                gridPosition.y,
                physicsPosition
            );
            coordinates.worldToPhysicsSpace(
                physicsPosition.x,
                physicsPosition.y,
                physicsPosition
            );
        }
        listenerPosition.set(physicsPosition.x, 0f, physicsPosition.y);

        if (positionInitialized && tpf > 0f) {
            listenerVelocity.set(listenerPosition)
                .subtractLocal(previousPosition)
                .divideLocal(tpf);
        } else {
            listenerVelocity.set(Vector3f.ZERO);
            positionInitialized = true;
        }
        listener.setLocation(listenerPosition);
        listener.setVelocity(listenerVelocity);
        previousPosition.set(listenerPosition);

        Camera camera = app.getCamera();
        if (camera != null) {
            listener.setRotation(camera.getRotation());
        }
    }
}
