package com.jme3.effect;

import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.math.Matrix3f;
import com.jme3.math.Vector3f;
import com.jme3.renderer.Camera;
import com.jme3.scene.VertexBuffer;
import com.jme3.util.TempVars;
import java.nio.FloatBuffer;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ParticleTriMeshReuseTest {
    @Test
    void repeatedUpdatesPreserveCameraAxesBuffersAndTempVars() {
        ParticleEmitter emitter = new ParticleEmitter("test", ParticleMesh.Type.Triangle, 1);
        ParticleTriMesh mesh = (ParticleTriMesh) emitter.getMesh();
        Camera camera = new Camera(64, 64);
        Vector3f cameraUp = camera.getUp();
        Vector3f cameraLeft = camera.getLeft();
        Vector3f cameraDirection = camera.getDirection();
        Particle particle = new Particle();
        particle.life = 1;
        particle.size = 1;
        particle.position.set(2, 3, 4);
        particle.velocity.set(Vector3f.UNIT_X);
        particle.color.set(ColorRGBA.White);
        FloatBuffer positions = (FloatBuffer) mesh.getBuffer(VertexBuffer.Type.Position).getData();
        for (int mode = 0; mode < 3; mode++) {
            emitter.setFacingVelocity(mode == 1);
            emitter.setFaceNormal(mode == 2 ? Vector3f.UNIT_Z : null);
            particle.angle = mode == 2 ? FastMath.HALF_PI : 0;
            for (int frame = 0; frame < 20; frame++) {
                TempVars caller = TempVars.get();
                caller.vect1.set(7, 8, 9);
                mesh.updateParticleData(new Particle[]{particle}, camera, Matrix3f.IDENTITY);
                assertEquals(new Vector3f(7, 8, 9), caller.vect1);
                caller.release();
                assertSame(positions, mesh.getBuffer(VertexBuffer.Type.Position).getData());
                assertEquals(cameraUp, camera.getUp());
                assertEquals(cameraLeft, camera.getLeft());
                assertEquals(cameraDirection, camera.getDirection());
                for (int i = 0; i < positions.capacity(); i++) assertTrue(Float.isFinite(positions.get(i)));
                assertEquals(2f, (positions.get(0) + positions.get(3) + positions.get(6) + positions.get(9)) / 4, 1e-6f);
                assertEquals(3f, (positions.get(1) + positions.get(4) + positions.get(7) + positions.get(10)) / 4, 1e-6f);
                assertEquals(4f, (positions.get(2) + positions.get(5) + positions.get(8) + positions.get(11)) / 4, 1e-6f);
            }
        }
    }
}
