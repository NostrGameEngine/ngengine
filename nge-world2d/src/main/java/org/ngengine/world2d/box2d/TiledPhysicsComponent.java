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

package org.ngengine.world2d.box2d;

import java.util.logging.Logger;

import org.box2d4j.b2BodyId;
import org.box2d4j.b2Filter;
import org.box2d4j.b2ShapeId;
import org.box2d4j.b2Vec2;
import org.box2d4j.b2WorldId;
import org.ngengine.Components;
import org.ngengine.components.AbstractComponent;
import org.ngengine.components.Component;
import org.ngengine.components.ComponentManager;
import org.ngengine.components.fragments.LogicFragment;
import org.ngengine.platform.NGEUtils;
import org.ngengine.world2d.PropertiesKeys;
import org.ngengine.world2d.TiledWorld2d;
import org.ngengine.world2d.TiledWorld2dManagerComponent;
import org.ngengine.world2d.box2d.Box2dPhysicsFactory.PhysicsDef;
import org.ngengine.world2d.box2d.Box2dPhysicsFactory.PhysicsShapeDef;
import org.ngengine.world2d.debug.Box2dFixtureDebugDumper;

import com.jme3.math.Vector2f;
import com.jme3.util.TempVars;

import org.ngengine.world2d.tiled.components.TiledComponentManager;
import org.ngengine.world2d.tiled.components.TiledObjectSyncComponent;
import org.ngengine.world2d.tiled.components.fragments.TiledEntityLifecycleFragment;
import org.ngengine.world2d.tiled.components.fragments.TiledEntityLogicFragment;
import org.ngengine.world2d.tiled.core.TiledBase;
import org.ngengine.world2d.tiled.core.TiledEntity;
import org.ngengine.world2d.tiled.core.TiledMap;
import org.ngengine.world2d.tiled.core.entity.TiledObjectEntity;
import org.ngengine.world2d.tiled.core.entity.TiledTileEntity;
import org.ngengine.world2d.tiled.core.tileset.Tile;
import org.ngengine.world2d.tiled.enums.ObjectShape;
import org.ngengine.world2d.tiled.enums.Orientation;
import org.ngengine.world2d.tiled.util.CoordinateSystem;

import static org.box2d4j.B2.*;

public class TiledPhysicsComponent extends AbstractComponent
        implements  LogicFragment, TiledEntityLogicFragment, TiledEntityLifecycleFragment {

    private final Logger logger = Logger.getLogger(TiledPhysicsComponent.class.getName());
    private static final double WARP_EPSILON = 0.0001d;
     
    private b2BodyId body;
    private b2WorldId bodyWorld;

    private Tile lastTile;
    private double entityX = Double.MIN_VALUE, entityY = Double.MIN_VALUE;
    private double entityAngle = Double.MIN_VALUE;

  
    private Boolean needsUpdate = true;
    private float baseAngle = 0;

    private PhysicsDef def;
    private final Vector2f worldPos = new Vector2f();
    private final b2Vec2 tmpVec2 = new b2Vec2();
    private float worldRot;
    private boolean debugFixturesDumped;
    private boolean collisionEnabled = true;

    @Override
    public Component newInstance() {
        TiledPhysicsComponent c = new TiledPhysicsComponent();
        c.setUpdateNeeded();
        return c;
    }

    public boolean isUpdateNeeded() {
        return this.needsUpdate;
    }

    public Vector2f getPhysicsWorldPosition(){
        if (hasBody()) {
            b2Vec2 v2 = b2Body_GetPosition(body);
            worldPos.set(v2.x, v2.y);
        }
        return worldPos;
    }

    public void setPhysicsWorldPosition(Vector2f pos){

        this.worldPos.set(pos);
        if (hasBody()) {
            b2Body_SetTransform(body, tmpVec2.set(pos.x, pos.y), b2Body_GetRotation(body));
        }

        // TiledMap tiledMap = getInstanceOf(TiledMap.class);
        TiledObjectEntity entity = getInstanceOf(TiledObjectEntity.class);
        if(entity!=null){
            CoordinateSystem coords = getInstanceOf(CoordinateSystem.class);
            tmpVec2.x=pos.x;
            tmpVec2.y=pos.y;
            try(TempVars vars = TempVars.get()){
                Vector2f v = vars.vect2d;
                coords.physicsToWorldSpace(tmpVec2, v);    
                fixCoords(v);
                coords.worldToGridSpace(v.x, v.y, v);
                entity.setX(v.x);
                entity.setY(v.y);
            }
        }
    }

    public float getPhysicsWorldRotation(){
        if (hasBody()) {
            worldRot = b2Rot_GetAngle(b2Body_GetRotation(body));
        }
        return worldRot;
    }

    public void setPhysicsWorldRotation(float rot){
        this.worldRot = rot;
        if (hasBody()) {
            b2Body_SetTransform(body, b2Body_GetPosition(body), b2MakeRot(rot));
        }
        TiledObjectEntity entity = getInstanceOf(TiledObjectEntity.class);
        if(entity!=null){
            entity.setRotation(Math.toDegrees(rot));
        }
    }

    public void clearUpdateNeeded() {
        this.needsUpdate = false;
    }

    public void setUpdateNeeded() {
        this.needsUpdate = true;
    }

    /**
     * Rebuilds this entity's fixtures as soon as the Box2D world is safe to
     * mutate. Use this after changing collision-bearing Tiled state at runtime,
     * such as swapping an open door tile for its closed variant.
     */
    public void refreshPhysics() {
        setUpdateNeeded();
        ComponentManager manager = getComponentManager();
        if (manager == null) {
            return;
        }
        TiledWorld2d world = getInstanceOf(TiledWorld2d.class);
        TiledEntity entity = getInstanceOf(TiledEntity.class);
        if (world == null || entity == null || world.getPhysics() == null
                || !b2World_IsValid(world.getPhysics())) {
            return;
        }
        world.runAfterPhysicsStep(() -> updatePhysics(manager, world.getPhysics(), entity));
    }

    /**
     * Enables or disables collision without destroying the body's fixtures.
     * This is intended for stateful map objects such as doors whose logical
     * tile remains stable while their rendered state changes.
     *
     * @param enabled {@code true} when the entity should participate in physics
     */
    public void setCollisionEnabled(boolean enabled) {
        collisionEnabled = enabled;
        if (hasBody()) {
            if (b2Body_IsEnabled(body) != enabled) {
                if (enabled) {
                    b2Body_Enable(body);
                } else {
                    b2Body_Disable(body);
                }
            }
        } else if (enabled) {
            refreshPhysics();
        }
    }

    /** @return whether this component allows its body to participate in physics */
    public boolean isCollisionEnabled() {
        return collisionEnabled;
    }

    private final Vector2f linearVelocity = new Vector2f();

    public Vector2f getLinearVelocity() {
        if (hasBody()) {
            b2Vec2 v2 = b2Body_GetLinearVelocity(body);
            linearVelocity.set(v2.x, v2.y);
        }
        return linearVelocity;
    }

    public float getAngularVelocity() {
        if (hasBody()) {
            return b2Body_GetAngularVelocity(body);
        }
        return 0f;
    }

    public void setLinearVelocity(Vector2f linearVelocity) {
        if (hasBody()) {
            b2Body_SetLinearVelocity(body, tmpVec2.set(linearVelocity.x, linearVelocity.y));
        }
    }

    public void setAngularVelocity(float angularVelocity) {
        if (hasBody()) {
            b2Body_SetAngularVelocity(body, angularVelocity);
        }
    }

    public b2WorldId getBodyWorld() {
        return bodyWorld;
    }
   
    public b2BodyId getBody() {
        return body;
    }

    protected void updatePhysics(ComponentManager mng, b2WorldId phy, TiledEntity entity ) {
        // World phy = world.getPhysics();
        if (phy == null || !b2World_IsValid(phy)) {
            body = null;
            bodyWorld = null;
            def = null;
            setUpdateNeeded();
            return;
        }
        if (body != null && !hasBody()) {
            // A world/body teardown outside this component can invalidate the
            // opaque Box2D handle while leaving the Java reference non-null.
            // Drop every dependent handle before considering a rebuild.
            body = null;
            bodyWorld = null;
            def = null;
            setUpdateNeeded();
        }
        
        CoordinateSystem coords = getInstanceOf(CoordinateSystem.class);
        TiledMap tiledMap = getInstanceOf(TiledMap.class);

        boolean canCollide = false;
        if (entity instanceof TiledObjectEntity) {
            TiledObjectEntity object = (TiledObjectEntity) entity;
            canCollide = object.getShape() != ObjectShape.POINT && Box2dHelper.isPhysicsEnabled(object);
            if (canCollide && object.getShape() == ObjectShape.TILE) {
                canCollide = Box2dHelper.hasPhysicalCollisions(object.getTile());
            }
        } else if (entity instanceof TiledTileEntity) {
            TiledTileEntity tile = (TiledTileEntity) entity;
            canCollide = tile != null && Box2dHelper.hasPhysicalCollisions(tile.getTile());
        }

        if (!canCollide && body != null) {
            setUpdateNeeded();
        }

        if (entity instanceof TiledObjectEntity) {
            TiledObjectEntity obj = (TiledObjectEntity) entity;
            if (obj.getShape() == ObjectShape.TILE) {
                Tile tile = obj.getTile();
                if (tile != lastTile) {
                    lastTile = tile;
                    setUpdateNeeded(); // invalidate
                }
            }
        } else if (entity instanceof TiledTileEntity) {
            TiledTileEntity tile = (TiledTileEntity) entity;
            if (tile.getTile() != lastTile) {
                lastTile = tile.getTile();
                setUpdateNeeded(); // invalidate
            }
        }

        if (!canCollide) {
            if (isUpdateNeeded() && body != null && bodyWorld != null) {
               clean();
               clearUpdateNeeded();
            }
            return;
        }

        if (isUpdateNeeded()) {

            if (hasBody()) {
                int shapeCount = b2Body_GetShapeCount(body);
                b2ShapeId[] shapes = new b2ShapeId[shapeCount];
                b2Body_GetShapes(body, shapes, shapes.length);
                for (b2ShapeId shape : shapes) {
                    b2DestroyShape(shape, false);
                }
                debugFixturesDumped = false;
            }

            if(def==null) {
                def = Box2dPhysicsFactory.createBody(coords, tiledMap, entity);
                this.body = b2CreateBody(phy, def.getBodyDef());
            }

           
            def = Box2dPhysicsFactory.createFixtures(def, coords, tiledMap, entity);
            // baseAngle = def.getBodyDef().angle;
            this.bodyWorld = phy;

            

            // // copy over everything from old body to new body
            // if (this.body != null) {
            //     body.setType(this.body.getType());
            //     body.setActive(this.body.isActive());
            //     body.setAngularDamping(this.body.getAngularDamping());
            //     body.setAngularVelocity(this.body.getAngularVelocity());
            //     body.setAwake(this.body.isAwake());
            //     body.setBullet(this.body.isBullet());
            //     body.setFixedRotation(this.body.isFixedRotation());
            //     body.setGravityScale(this.body.getGravityScale());
            //     body.setLinearDamping(this.body.getLinearDamping());
            //     Vec2 v2 = body.getLinearVelocity();
            //     Vec2 v1 = this.body.getLinearVelocity();
            //     v2.x = v1.x;
            //     v2.y = v1.y;
            //     body.setLinearVelocity(v2);
            //     body.setSleepingAllowed(this.body.isSleepingAllowed());
            //     body.setTransform(this.body.getPosition(), baseAngle);

            //     Fixture fx = body.getFixtureList();
            //     while (fx != null) {
            //         Fixture oldFx = this.body.getFixtureList();
            //         while (oldFx != null) {
            //             if (Objects.equals(fx.getUserData(), oldFx.getUserData())) {
            //                 fx.setSensor(oldFx.isSensor());
            //                 break;
            //             }
            //             oldFx = oldFx.getNext();
            //         }
            //         fx = fx.getNext();
            //     }
            // }

            for (PhysicsShapeDef fd : def.getFixtureDefs()) {
                b2ShapeId fx = fd.createShape(body);

                Box2dUserData userData = (Box2dUserData) b2Shape_GetUserData(fx);
                TiledObjectEntity fxEntry = userData.getCollision();
                TiledBase bodyEntity = userData.getEntity();

                Object categoryBits = fxEntry.getProperty(PropertiesKeys.phy.categoryBits);
                if (categoryBits == null) {
                    categoryBits = bodyEntity.getProperty(PropertiesKeys.phy.categoryBits);
                }
                if (categoryBits == null) categoryBits = "0x0001";
                Object maskBits = fxEntry.getProperty( PropertiesKeys.phy.maskBits);
                if (maskBits == null) {
                    maskBits = bodyEntity.getProperty(PropertiesKeys.phy.maskBits);
                }
                if (maskBits == null) maskBits = "0xFFFF";
                Object groupIndex = fxEntry.getProperty(   PropertiesKeys.phy.groupIndex);
                if (groupIndex == null) {
                    groupIndex = bodyEntity.getProperty(PropertiesKeys.phy.groupIndex);
                }
                if (groupIndex == null) groupIndex = "0";

                b2Filter filterfx = b2Shape_GetFilter(fx);
                
                
                filterfx.categoryBits = Long.parseUnsignedLong(stripHexPrefix(categoryBits), 16);
                filterfx.maskBits = Long.parseUnsignedLong(stripHexPrefix(maskBits), 16);
                filterfx.groupIndex = Integer.parseInt(NGEUtils.safeString(groupIndex));
                b2Shape_SetFilter(fx, filterfx);
            }

            // if (this.body != null && this.bodyWorld != null) {
            //     this.bodyWorld.destroyBody(this.body);
            
                
            // }
            // this.body = body;
            clearUpdateNeeded();
        }

        if (hasBody()) {
            if (b2Body_IsEnabled(body) != collisionEnabled) {
                if (collisionEnabled) {
                    b2Body_Enable(body);
                } else {
                    b2Body_Disable(body);
                }
            }
            applyNetworkAuthorityBodyMode(mng, entity);

            double newX = 0, newY = 0, newAngle = 0;
            boolean isDynamic = b2Body_GetType(body) == b2_dynamicBody;

            if (entity instanceof TiledObjectEntity) {
                TiledObjectEntity obj = (TiledObjectEntity) entity;
                newX = obj.getX();
                newY = obj.getY();
                newAngle = obj.getRotation();

            } else if (entity instanceof TiledTileEntity) {
                TiledTileEntity tile = (TiledTileEntity) entity;
                newX = tile.getX();
                newY = tile.getY();
                newAngle = 0;
            }

            try (TempVars vars = TempVars.get()) {
                Vector2f pos = vars.vect2d;
                if (Math.abs(newX - entityX) > WARP_EPSILON
                    || Math.abs(newY - entityY) > WARP_EPSILON
                    || Math.abs(newAngle - entityAngle) > WARP_EPSILON) {

                    // Warp the physics body to entity
                    if (entity instanceof TiledObjectEntity) {
                        TiledObjectEntity obj = (TiledObjectEntity) entity;
                        if (obj.getShape() == ObjectShape.TILE) {
                            pos.set((float) obj.getX(), (float) obj.getY());
                            if (tiledMap.getOrientation() == Orientation.ORTHOGONAL) {
                                pos.x += (float) obj.getWidth() * 0.5f;
                            }
                        } else {
                            pos.set((float) obj.getX() + (float) obj.getWidth() * 0.5f,
                                    (float) obj.getY() + (float) obj.getHeight() * 0.5f);
                        }

                    } else if (entity instanceof TiledTileEntity) {
                        TiledTileEntity tile = (TiledTileEntity) entity;
                        coords.tileToGridSpace((float) tile.getX(),
                                (float) tile.getY(), pos);
                    }

                   coords.gridToWorldSpace(pos.x, pos.y, pos);

                    logger.finest("Warping physics body for entity: " + entity);
                    coords.worldToPhysicsSpace(pos, tmpVec2);
                    b2Body_SetTransform(body, tmpVec2, b2MakeRot((float) Math.toRadians(newAngle) + baseAngle));

                    entityX = newX;
                    entityY = newY;
                    entityAngle = newAngle;
                } else if (isDynamic) {
                    // sync entity to physics body
                    b2Vec2 vb = b2Body_GetPosition(body);
                    float angle = b2Rot_GetAngle(b2Body_GetRotation(body));
                    if (entity instanceof TiledObjectEntity) {
                        TiledObjectEntity obj = (TiledObjectEntity) entity;
                        coords.physicsToWorldSpace(vb, pos);
                                        fixCoords(pos);

                        //   if(
                        // obj.getShape() == ObjectShape.TILE&&    
                        // tiledMap.getOrientation() == Orientation.ORTHOGONAL){
                        //     pos.x-=((float) obj.getTile().getWidth() )* 0.5f;
                        // }
                        coords.worldToGridSpace(pos.x, pos.y, pos);

                     
                        obj.setX(pos.x);
                        obj.setY(pos.y);
                        obj.setRotation(Math.toDegrees(angle - baseAngle));

                        entityX = obj.getX();
                        entityY = obj.getY();
                        entityAngle = obj.getRotation();
                    }
                }
            }
            dumpFixturesIfRequested(coords);
        }
    }

    private void dumpFixturesIfRequested(CoordinateSystem coords) {
        if (debugFixturesDumped || body == null) {
            return;
        }
        if (!Box2dFixtureDebugDumper.isEnabled(getSettings())) {
            return;
        }

        int shapeCount = b2Body_GetShapeCount(body);
        b2ShapeId[] shapes = new b2ShapeId[shapeCount];
        b2Body_GetShapes(body, shapes, shapeCount);
        for (b2ShapeId shape : shapes) {
            Box2dFixtureDebugDumper.dumpFixture(coords, body, shape);
        }
        debugFixturesDumped = true;
    }

    private void fixCoords(Vector2f pos) {
        TiledMap tiledMap = getInstanceOf(TiledMap.class);
        TiledObjectEntity obj = getInstanceOf(TiledObjectEntity.class);
        if (obj.getShape() == ObjectShape.TILE && tiledMap.getOrientation() == Orientation.ORTHOGONAL) {
            pos.x -= ((float) obj.getTile().getWidth()) * 0.5f;
        }

        
    }

    private void applyNetworkAuthorityBodyMode(ComponentManager mng, TiledBase entry) {
        if (!hasBody()) {
            return;
        }
        int configuredType = def != null && def.getBodyDef() != null
            ? def.getBodyDef().type
            : b2Body_GetType(body);


        TiledObjectSyncComponent syncC = getInstanceOf(TiledObjectSyncComponent.class);
            
        boolean hasAuthority = syncC==null||syncC.checkAuthority();        

        int current = b2Body_GetType(body);
        if (!hasAuthority) {
            if (current != b2_staticBody && current != b2_kinematicBody) {
                b2Body_SetType(body, b2_kinematicBody);
                b2Body_SetLinearVelocity(body, tmpVec2.set(0f, 0f));
                b2Body_SetAngularVelocity(body, 0f);
            }
            return;
        }

        // Local authority (or no network session): leave STATIC as-is,
        // but recover DYNAMIC from temporary KINEMATIC proxy mode.
        if (current == b2_kinematicBody && configuredType != b2_kinematicBody) {
            b2Body_SetType(body, configuredType);
            b2Body_SetAwake(body, true);
        }
    }

    private void clean() {
         if (this.bodyWorld != null&&this.body != null) {
            TiledWorld2d world = getInstanceOf(TiledWorld2d.class);
            if (world != null) {
                world.destroyPhysics(this.body);
            }
            this.body = null;
            this.bodyWorld=null;
            this.def=null;
        }
        setUpdateNeeded();
    }

    @Override
    public void onTiledEntityLogicUpdate(ComponentManager mng, float tpf, TiledBase entity) {
        if (entity instanceof TiledEntity) {
            TiledWorld2d world = mng.getInstanceOf(TiledWorld2d.class);
            // if( this.bodyWorld !=null && this.body!=null&&this.bodyWorld!=world.getPhysics()){
            //     // physics world changed, need to recreate body
            //     setUpdateNeeded();
            // }
            updatePhysics(mng, world.getPhysics(), (TiledEntity)entity);
            
        }
    }

 

    @Override
    public void onEnable(ComponentManager mng,
            boolean firstTime) {
        TiledBase entity = getInstanceOf(TiledBase.class);
        TiledWorld2d world = getInstanceOf(TiledWorld2d.class);
        if(world!=null){
            Box2dHelper.apply(world.getPhysics(), entity);
            world.runAfterPhysicsStep(() -> updatePhysics(mng, world.getPhysics(), (TiledEntity) entity));
        }
    }

    public boolean hasBody() {
        return body != null && !B2_IS_NULL(body) && b2Body_IsValid(body);
    }

    private static String stripHexPrefix(Object value) {
        String text = NGEUtils.safeString(value);
        return text.startsWith("0x") || text.startsWith("0X") ? text.substring(2) : text;
    }

    @Override
    public void onDisable(ComponentManager mng) {
        clean();
 
    }

    @Override
    public void onTiledEntityInitialize(ComponentManager mng, TiledBase entity) {

    
    }

    @Override
    public void onTiledEntityCleanup(ComponentManager mng, TiledBase entity) {
        clean();

    }

    @Override
    public void updateAppLogic(ComponentManager mng, float tpf) {
        if(mng instanceof TiledComponentManager) return;
        TiledWorld2dManagerComponent world = Components.get(mng, TiledWorld2dManagerComponent.class).get();
        if(world ==null)return;
        for(TiledWorld2d map : world.getLoadedMaps().values()){
            Box2dHelper.apply(map.getPhysics(), map.getMap());
        }        
    }

}
