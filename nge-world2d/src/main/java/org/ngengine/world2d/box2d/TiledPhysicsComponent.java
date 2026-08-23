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

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import org.jbox2d.common.Vec2;
// import org.example.Box2DPhysicsFactory;
import org.jbox2d.dynamics.Body;
import org.jbox2d.dynamics.BodyType;
import org.jbox2d.dynamics.Filter;
import org.jbox2d.dynamics.Fixture;
import org.jbox2d.dynamics.FixtureDef;
import org.jbox2d.dynamics.World;
import org.ngengine.Components;
import org.ngengine.components.AbstractComponent;
import org.ngengine.components.Component;
import org.ngengine.components.ComponentManager;
import org.ngengine.components.fragments.LogicFragment;
import org.ngengine.network.components.NetcodeManagerComponent;
import org.ngengine.platform.NGEUtils;
import org.ngengine.world2d.PropertiesKeys;
import org.ngengine.world2d.TiledWorld2d;
import org.ngengine.world2d.TiledWorld2dManagerComponent;
import org.ngengine.world2d.box2d.Box2dPhysicsFactory.PhysicsDef;
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

public class TiledPhysicsComponent extends AbstractComponent
        implements  LogicFragment, TiledEntityLogicFragment, TiledEntityLifecycleFragment {

    private final Logger logger = Logger.getLogger(TiledPhysicsComponent.class.getName());
    private static final double WARP_EPSILON = 0.0001d;
     
    private Body body;
    private World bodyWorld;

    private Tile lastTile;
    private double entityX = Double.MIN_VALUE, entityY = Double.MIN_VALUE;
    private double entityAngle = Double.MIN_VALUE;

  
    private Boolean needsUpdate = true;
    private float baseAngle = 0;

    private PhysicsDef def;
    private final Vector2f worldPos = new Vector2f();
    private final Vec2 tmpVec2 = new Vec2();
    private float worldRot;
    private int collisionGroup;
    private int collisionMask;
    private boolean debugFixturesDumped;

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
        if (body != null) {
            Vec2 v2 = body.getPosition();
            worldPos.set(v2.x, v2.y);
        }
        return worldPos;
    }

    public void setPhysicsWorldPosition(Vector2f pos){

        this.worldPos.set(pos);
        if (body != null) {
            Vec2 v2 = body.getPosition();
            v2.x = pos.x;
            v2.y = pos.y;
            body.setTransform(v2, body.getAngle());
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
        if (body != null) {
            worldRot = body.getAngle();
        }
        return worldRot;
    }

    public void setPhysicsWorldRotation(float rot){
        this.worldRot = rot;
        if (body != null) {
            body.setTransform( body.getPosition(), rot);
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
        if (world == null || entity == null || world.getPhysics() == null) {
            return;
        }
        world.runAfterPhysicsStep(() -> updatePhysics(manager, world.getPhysics(), entity));
    }

    private final Vector2f linearVelocity = new Vector2f();

    public Vector2f getLinearVelocity() {
        if (body != null) {
            Vec2 v2 = body.getLinearVelocity();
            linearVelocity.set(v2.x, v2.y);
        }
        return linearVelocity;
    }

    public float getAngularVelocity() {
        if (body != null) {
            return body.getAngularVelocity();
        }
        return 0f;
    }

    public void setLinearVelocity(Vector2f linearVelocity) {
        if (body != null) {
            Vec2 v2 = body.getLinearVelocity();
            v2.x = linearVelocity.x;
            v2.y = linearVelocity.y;
            body.setLinearVelocity(v2);
        }
    }

    public void setAngularVelocity(float angularVelocity) {
        if (body != null) {
            body.setAngularVelocity(angularVelocity);
        }
    }

    public World getBodyWorld() {
        return bodyWorld;
    }
   
    public Body getBody() {
        return body;
    }

    protected void updatePhysics(ComponentManager mng, World phy, TiledEntity entity ) {
        // World phy = world.getPhysics();
        
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

            if(this.body!=null){
                Fixture oldFx = this.body.getFixtureList();
                List<Fixture> toRemove = new ArrayList<>();
                while(oldFx!=null){
                    toRemove.add(oldFx);
                    oldFx = oldFx.getNext();
                }
                for(Fixture fx : toRemove){
                    this.body.destroyFixture(fx);
                }
                debugFixturesDumped = false;
            }

            if(def==null) {
                def = Box2dPhysicsFactory.createBody(coords, tiledMap, entity);
                this.body = phy.createBody(def.getBodyDef());
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

            for (FixtureDef fd : def.getFixtureDefs()) {
                Fixture fx = this.body.createFixture(fd);

                TiledObjectEntity fxEntry = ((Box2dUserData) fx.getUserData()).getCollision();
                TiledBase bodyEntity = ((Box2dUserData) fx.getUserData()).getEntity();

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

                Filter filterfx = new Filter();
                
                
                filterfx.categoryBits = Integer.parseInt(NGEUtils.safeString(categoryBits).replace("0x", ""),
                        16);
                filterfx.maskBits = Integer.parseInt(NGEUtils.safeString(maskBits).replace("0x", ""), 16);
                filterfx.groupIndex = Short.parseShort(NGEUtils.safeString(groupIndex));
                fx.setFilterData(filterfx);                
            }

            // if (this.body != null && this.bodyWorld != null) {
            //     this.bodyWorld.destroyBody(this.body);
            
                
            // }
            // this.body = body;
            clearUpdateNeeded();
        }

        if (body != null) {
            applyNetworkAuthorityBodyMode(mng, entity);

            double newX = 0, newY = 0, newAngle = 0;
            boolean isDynamic = body.getType() == BodyType.DYNAMIC;

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
                    Vec2 vb = body.getPosition();
                    coords.worldToPhysicsSpace(pos, vb);
                    body.setTransform(vb, (float) Math.toRadians(newAngle) + baseAngle);

                    entityX = newX;
                    entityY = newY;
                    entityAngle = newAngle;
                } else if (isDynamic) {
                    // sync entity to physics body
                    Vec2 vb = body.getPosition();
                    float angle = body.getAngle();
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

        Fixture fixture = body.getFixtureList();
        while (fixture != null) {
            Box2dFixtureDebugDumper.dumpFixture(coords, body, fixture);
            fixture = fixture.getNext();
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
        if (body == null) {
            return;
        }
        BodyType configuredType = def != null && def.getBodyDef() != null && def.getBodyDef().type != null
            ? def.getBodyDef().type
            : body.getType();


        TiledObjectSyncComponent syncC = getInstanceOf(TiledObjectSyncComponent.class);
            
        boolean hasAuthority = syncC==null||syncC.checkAuthority();        

        BodyType current = body.getType();
        if (!hasAuthority) {
            if (current != BodyType.STATIC && current != BodyType.KINEMATIC) {
                body.setType(BodyType.KINEMATIC);
                body.setLinearVelocity(new Vec2(0f, 0f));
                body.setAngularVelocity(0f);
            }
            return;
        }

        // Local authority (or no network session): leave STATIC as-is,
        // but recover DYNAMIC from temporary KINEMATIC proxy mode.
        if (current == BodyType.KINEMATIC && configuredType != BodyType.KINEMATIC) {
            body.setType(configuredType);
            body.setAwake(true);
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
            updatePhysics(mng, world.getPhysics(), (TiledEntity)entity);
        }
    }

    public boolean hasBody() {
        return this.body != null;
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
