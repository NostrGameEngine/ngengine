package org.ngengine.world2d;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import org.jbox2d.collision.shapes.CircleShape;
import org.jbox2d.collision.shapes.PolygonShape;
import org.jbox2d.common.Vec2;
import org.jbox2d.dynamics.BodyDef;
import org.jbox2d.dynamics.BodyType;
import org.jbox2d.dynamics.FixtureDef;
import com.jme3.math.Vector2f;
import com.jme3.util.TempVars;

import io.github.jmecn.tiled.core.MapObject;
import io.github.jmecn.tiled.core.ObjectGroup;
import io.github.jmecn.tiled.core.Tile;
import io.github.jmecn.tiled.enums.ObjectType;
import io.github.jmecn.tiled.renderer.MapRenderer;
import ngetest.tests.world2d.TiledWorld2DComponent;

public class Box2DPhysicsFactory   {
    private static Logger logger = Logger.getLogger(Box2DPhysicsFactory.class.getName());

 

    public List<PhysicsDef> createBody(TiledWorld2DComponent world, Tile tile){
        int x = tile.getX();
        int y = tile.getY();
        ObjectGroup collisions = tile.getCollisions();
        if(collisions==null) return List.of();
        try(TempVars tmp = TempVars.get()){
            Vector2f pos = tmp.vect2d;
            Vector2f size = tmp.vect2d2;
            size.set(tile.getWidth(), tile.getHeight());
            world.tileToPixelCoords(x, y, pos);
            ArrayList<PhysicsDef> defs = new ArrayList<>();
            for (MapObject obj : tile.getCollisions().getObjects()) {
                defs.add(createTileBody(pos, size, obj));
            }
            return defs;
        }
    }
 
    public List<PhysicsDef> createBody( MapObject obj){
        if(obj.getShape() == ObjectType.TILE){
            Tile tile = obj.getTile();
            if(tile!=null && tile.getCollisions()!=null){
                Vector2f size = new Vector2f(tile.getWidth(), tile.getHeight());
                Vector2f pos = new Vector2f((float)obj.getX(), (float)obj.getY());
                ArrayList<PhysicsDef> defs = new ArrayList<>();
                for (MapObject collision : tile.getCollisions().getObjects()) {
                    defs.add(createObjectBody(pos, size, collision));
                }
                return defs;
            } else {
                return List.of();
            }
        } else{
            ArrayList<PhysicsDef> defs = new ArrayList<>();
            defs.add(createObjectBody(obj));
            return defs;    
        }
    }
 
    public static class PhysicsDef {
        private FixtureDef fixtureDef;
        private BodyDef bodyDef;
        PhysicsDef(
            FixtureDef fixtureDef,
            BodyDef bodyDef
        ){
            this.fixtureDef = fixtureDef;
            this.bodyDef = bodyDef;
        }

        public FixtureDef getFixtureDef() {
            return fixtureDef;
        }

        public BodyDef getBodyDef() {
            return bodyDef;
        }

    }
  
    /**
     * Tile coordinate origin is on left-top corner.
     * 
     * @param physicsState
     * @param pos
     * @param size
     * @param obj
     */
     public static PhysicsDef createTileBody(Vector2f pos, Vector2f size, MapObject obj) {
        FixtureDef fixtureDef = new FixtureDef();
        fixtureDef.density = 1.0f;
        fixtureDef.friction = 0.0f;
        fixtureDef.restitution = 0.0f;

        BodyDef bodyDef = new BodyDef();

        switch (obj.getShape()) {
            case RECTANGLE: {
                float hx = (float) (obj.getWidth() * 0.5);
                float hy = (float) (obj.getHeight() * 0.5);
                PolygonShape shape = new PolygonShape();
                shape.setAsBox(hx, hy, new Vec2(hx, hy), 0);
                fixtureDef.shape = shape;
                break;
            }
            case POLYGON: {
                List<Vec2> vertices = new ArrayList<>();
                for (Vector2f v : obj.getPoints()) {
                    vertices.add(new Vec2(v.x, v.y));
                }
                PolygonShape shape = new PolygonShape();
                shape.set(vertices.toArray(new Vec2[0]), vertices.size());
                fixtureDef.shape = shape;
                break;
            }
            case ELLIPSE: {// box2d dose not support ellipse, use circle instead
                float hx = (float) (obj.getWidth() * 0.5);
                float hy = (float) (obj.getHeight() * 0.5);
                CircleShape shape = new CircleShape();
                shape.m_radius = Math.min(hx, hy);
                shape.m_p.set(hx, -hy);
                fixtureDef.shape = shape;
                break;
            }
            default: {
                logger.warning("Unsupported tile collision shape: " + obj.getShape());
                return null;
            }
        }

        bodyDef.position.set((float) (obj.getX() + pos.x), (float) (obj.getY() + pos.y));
        bodyDef.type = BodyType.STATIC;

        logger.info("Create body at: " + bodyDef.position.x + " " + bodyDef.position.y);

        return new PhysicsDef(fixtureDef, bodyDef);
    }

    /**
     * MapObject coordinate origin is on left-bottom corner.
     * 
     * @param physicsState
     * @param pos
     * @param size
     * @param obj
     */
    public static PhysicsDef createObjectBody(Vector2f pos, Vector2f size, MapObject obj) {
        FixtureDef fixtureDef = new FixtureDef();
        fixtureDef.density = 1.0f;
        fixtureDef.friction = 0.0f;
        fixtureDef.restitution = 0.0f;

        BodyDef bodyDef = new BodyDef();

        float delta = (float) (size.y - obj.getHeight());
        switch (obj.getShape()) {
            case RECTANGLE: {
                float hx = (float) (obj.getWidth() * 0.5);
                float hy = (float) (obj.getHeight() * 0.5);
                PolygonShape shape = new PolygonShape();
                shape.setAsBox(hx, hy, new Vec2(hx, -hy), 0);
                fixtureDef.shape = shape;
                break;
            }
            case POLYGON: {
                List<Vec2> vertices = new ArrayList<>();
                for (Vector2f v : obj.getPoints()) {
                    vertices.add(new Vec2(v.x, v.y));
                }
                PolygonShape shape = new PolygonShape();
                shape.set(vertices.toArray(new Vec2[0]), vertices.size());
                fixtureDef.shape = shape;
                break;
            }
            case ELLIPSE: {// box2d dose not support ellipse, use circle instead
                float hx = (float) (obj.getWidth() * 0.5);
                float hy = (float) (obj.getHeight() * 0.5);
                CircleShape shape = new CircleShape();
                shape.m_radius = Math.min(hx, hy);
                shape.m_p.set(hx, -hy);
                fixtureDef.shape = shape;
                break;
            }
            default: {
                logger.warning("Unsupported shape: " + obj.getShape());
                return null;
            }
        }

        bodyDef.position.set((float) (pos.x + obj.getX()), (float) (pos.y + obj.getY() - delta));
        bodyDef.type = BodyType.STATIC;

        return new PhysicsDef(fixtureDef, bodyDef);
    }

    /**
     * MapObject coordinate origin is on left-bottom corner.
     * 
     * @param physicsState
     * @param obj
     */
    public PhysicsDef createObjectBody(  MapObject obj) {
        FixtureDef fixtureDef = new FixtureDef();
        fixtureDef.density = 1.0f;
        fixtureDef.friction = 0.0f;
        fixtureDef.restitution = 0.0f;

        BodyDef bodyDef = new BodyDef();

        switch (obj.getShape()) {
            case RECTANGLE: {
                float hx = (float) (obj.getWidth() * 0.5);
                float hy = (float) (obj.getHeight() * 0.5);
                PolygonShape shape = new PolygonShape();
                shape.setAsBox(hx, hy, new Vec2(hx, hy), 0);
                fixtureDef.shape = shape;
                break;
            }
            case POLYGON: {
                List<Vec2> vertices = new ArrayList<>();
                for (Vector2f v : obj.getPoints()) {
                    vertices.add(new Vec2(v.x, v.y));
                }
                PolygonShape shape = new PolygonShape();
                shape.set(vertices.toArray(new Vec2[0]), vertices.size());
                fixtureDef.shape = shape;
                break;
            }
            case ELLIPSE: {// box2d dose not support ellipse, use circle instead
                float hx = (float) (obj.getWidth() * 0.5);
                float hy = (float) (obj.getHeight() * 0.5);
                CircleShape shape = new CircleShape();
                shape.m_radius = Math.min(hx, hy);
                shape.m_p.set(hx, -hy);
                fixtureDef.shape = shape;
                break;
            }
            default: {
                logger.warning("Unsupported shape: " + obj.getShape());
                return null;
            }
        }

        bodyDef.position.set((float) (obj.getX()), (float) (obj.getY()));
        bodyDef.type = BodyType.STATIC;

        return new PhysicsDef(fixtureDef, bodyDef);
    }

  

}
