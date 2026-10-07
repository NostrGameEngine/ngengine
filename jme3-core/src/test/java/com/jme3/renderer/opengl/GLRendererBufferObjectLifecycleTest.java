package com.jme3.renderer.opengl;

import com.jme3.renderer.Caps;
import com.jme3.renderer.RenderContext;
import com.jme3.shader.bufferobject.BufferObject;
import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class GLRendererBufferObjectLifecycleTest {
    @Test
    void deletingBuffersRebindsEveryIndexedUseAndPreservesUnrelatedBindings() throws Exception {
        verifyDeletion(false);
    }

    @Test
    void deletingDestructibleClonesInvalidatesTheLiveObjectBindings() throws Exception {
        verifyDeletion(true);
    }

    private static void verifyDeletion(boolean clone) throws Exception {
        Calls calls = new Calls();
        GLRenderer renderer = calls.renderer();
        BufferObject shared = new BufferObject(); shared.initializeEmpty(16);
        BufferObject unrelated = new BufferObject(); unrelated.initializeEmpty(16);
        for (int point = 0; point < 2; point++) {
            renderer.setUniformBufferObject(point, shared);
            renderer.setShaderStorageBufferObject(point, shared);
        }
        renderer.setUniformBufferObject(2, unrelated);
        renderer.setShaderStorageBufferObject(2, unrelated);
        int oldId = shared.getId(), unrelatedId = unrelated.getId();
        renderer.deleteBuffer(clone ? (BufferObject) shared.createDestructableClone() : shared);
        if (clone) shared.resetObject();
        for (int point = 0; point < 2; point++) {
            renderer.setUniformBufferObject(point, shared);
            renderer.setShaderStorageBufferObject(point, shared);
        }
        renderer.setUniformBufferObject(2, unrelated);
        renderer.setShaderStorageBufferObject(2, unrelated);
        assertNotEquals(oldId, shared.getId());
        assertEquals(List.of(oldId), calls.deleted);
        for (int target : new int[]{GL4.GL_UNIFORM_BUFFER, GL4.GL_SHADER_STORAGE_BUFFER}) {
            for (int point = 0; point < 2; point++) {
                assertEquals(1, calls.count(target, point, oldId));
                assertEquals(1, calls.count(target, point, shared.getId()));
            }
            assertEquals(1, calls.count(target, 2, unrelatedId));
        }
    }

    @Test
    void invalidatingStateForcesBothIndexedBufferNamespacesToBindAgain() throws Exception {
        Calls calls = new Calls();
        GLRenderer renderer = calls.renderer();
        BufferObject buffer = new BufferObject(); buffer.initializeEmpty(16);
        renderer.setUniformBufferObject(0, buffer);
        renderer.setShaderStorageBufferObject(1, buffer);
        renderer.invalidateState();
        renderer.setUniformBufferObject(0, buffer);
        renderer.setShaderStorageBufferObject(1, buffer);
        assertEquals(2, calls.count(GL4.GL_UNIFORM_BUFFER, 0, buffer.getId()));
        assertEquals(2, calls.count(GL4.GL_SHADER_STORAGE_BUFFER, 1, buffer.getId()));
        assertEquals(4, calls.context.boundUniformBuffers.length);
        assertEquals(4, calls.context.boundShaderStorageBuffers.length);
    }

    @Test
    void resettingNativeObjectsUploadsAndRebindsTheSameCpuBufferAfterContextLoss() throws Exception {
        Calls calls = new Calls();
        GLRenderer renderer = calls.renderer();
        BufferObject buffer = new BufferObject(); buffer.initializeEmpty(16);
        buffer.getByteData().putInt(0, 12345);
        renderer.setUniformBufferObject(0, buffer);
        renderer.setShaderStorageBufferObject(1, buffer);
        int previous = buffer.getId();
        renderer.resetGLObjects();
        renderer.setUniformBufferObject(0, buffer);
        renderer.setShaderStorageBufferObject(1, buffer);
        assertNotEquals(previous, buffer.getId());
        assertEquals(1, calls.count(GL4.GL_UNIFORM_BUFFER, 0, buffer.getId()));
        assertEquals(1, calls.count(GL4.GL_SHADER_STORAGE_BUFFER, 1, buffer.getId()));
        assertEquals(List.of(12345, 12345), calls.uploadedValues);
    }

    private static final class Calls {
        final List<int[]> bindings = new ArrayList<>();
        final List<Integer> deleted = new ArrayList<>();
        final List<Integer> uploadedValues = new ArrayList<>();
        RenderContext context;
        int nextBuffer = 17;

        GLRenderer renderer() throws Exception {
            GL4 gl = (GL4) Proxy.newProxyInstance(GL.class.getClassLoader(), new Class<?>[]{GL4.class},
                    (proxy, method, arguments) -> {
                        switch (method.getName()) {
                            case "glGenBuffers": ((IntBuffer) arguments[0]).put(0, nextBuffer++); break;
                            case "glDeleteBuffers": deleted.add(((IntBuffer) arguments[0]).get(0)); break;
                            case "glBindBufferBase": bindings.add(new int[]{(Integer) arguments[0], (Integer) arguments[1], (Integer) arguments[2]}); break;
                            // GL consumes raw native-order bytes, not the duplicate's Java byte-order metadata.
                            case "glBufferData": uploadedValues.add(((ByteBuffer) arguments[1]).duplicate().order(ByteOrder.nativeOrder()).getInt(0)); break;
                            case "glGetInteger": ((IntBuffer) arguments[1]).put(0, 0); break;
                            default:
                                assertTrue(List.of("glBindBuffer", "glDepthMask", "glColorMask", "glDisable", "glDepthFunc", "glDepthRange", "glBlendFunc", "glBlendEquationSeparate", "glActiveTexture", "glClearColor", "glLineWidth").contains(method.getName()), "Unexpected GL call: " + method.getName());
                        }
                        return null;
                    });
            GLRenderer renderer = new GLRenderer(gl, null, null);
            renderer.getCaps().add(Caps.UniformBufferObject);
            renderer.getCaps().add(Caps.ShaderStorageBufferObject);
            Field field = GLRenderer.class.getDeclaredField("context"); field.setAccessible(true);
            context = (RenderContext) field.get(renderer); context.resetBufferObjectBindings(4, 4);
            return renderer;
        }

        long count(int target, int point, int buffer) {
            return bindings.stream().filter(value -> value[0] == target && value[1] == point && value[2] == buffer).count();
        }
    }
}
