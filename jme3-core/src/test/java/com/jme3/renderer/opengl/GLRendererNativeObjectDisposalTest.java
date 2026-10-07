/*
 * Copyright (c) 2026 jMonkeyEngine
 * All rights reserved.
 */
package com.jme3.renderer.opengl;

import com.jme3.texture.FrameBuffer;
import com.jme3.texture.Image;
import com.jme3.texture.Texture2D;
import com.jme3.scene.VertexBuffer;
import com.jme3.shader.Shader;
import com.jme3.shader.bufferobject.BufferObject;
import com.jme3.util.NativeObject;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

class GLRendererNativeObjectDisposalTest {
    @Test
    void deletedImagesInvalidateEveryTextureUnitBeforeReupload() throws Exception {
        for (String methodName : new String[]{"bindTextureAndUnit", "bindTextureOnly"}) {
            for (boolean destructibleClone : new boolean[]{false, true}) {
                GL gl = mock(GL.class);
                GLRenderer renderer = new GLRenderer(gl, mock(GLExt.class), mock(GLFbo.class));
                java.lang.reflect.Method bind = GLRenderer.class.getDeclaredMethod(methodName, int.class, Image.class, int.class);
                bind.setAccessible(true);
                Image image = new Texture2D(4, 4, Image.Format.RGBA8).getImage();
                Image unrelated = new Texture2D(4, 4, Image.Format.RGBA8).getImage();
                image.setId(17);
                unrelated.setId(99);
                bind.invoke(renderer, GL.GL_TEXTURE_2D, image, 0);
                bind.invoke(renderer, GL.GL_TEXTURE_2D, image, 1);
                bind.invoke(renderer, GL.GL_TEXTURE_2D, unrelated, 2);
                renderer.deleteImage(destructibleClone ? (Image) image.createDestructableClone() : image);
                if (destructibleClone) image.resetObject();
                image.setId(23);
                bind.invoke(renderer, GL.GL_TEXTURE_2D, image, 0);
                bind.invoke(renderer, GL.GL_TEXTURE_2D, image, 1);
                bind.invoke(renderer, GL.GL_TEXTURE_2D, unrelated, 2);
                verify(gl, times(2)).glBindTexture(GL.GL_TEXTURE_2D, 23);
                verify(gl, times(1)).glBindTexture(GL.GL_TEXTURE_2D, 99);
            }
        }
    }

    @Test
    void explicitlyDeletedImagesAreNotDeletedAgainAtShutdown() {
        GL gl = mock(GL.class);
        ArrayList<Integer> deleted = new ArrayList<>();
        doAnswer(call -> {
            deleted.add(((IntBuffer) call.getArgument(0)).get(0));
            return null;
        }).when(gl).glDeleteTextures(any());
        GLRenderer renderer = new GLRenderer(gl, mock(GLExt.class), mock(GLFbo.class));
        Image image = new Texture2D(4, 4, Image.Format.RGBA16F).getImage();
        image.setId(17);
        renderer.registerNativeObject(image);
        image.dispose();
        renderer.deleteImage(image);
        image.setId(23);
        renderer.registerNativeObject(image);
        assertDoesNotThrow(renderer::cleanup);
        assertEquals(Arrays.asList(17, 23), deleted);
    }

    @Test
    void explicitlyDeletedFramebuffersAreNotDeletedAgainAtShutdown() {
        GLFbo glfbo = mock(GLFbo.class);
        ArrayList<Integer> deleted = new ArrayList<>();
        doAnswer(call -> {
            deleted.add(((IntBuffer) call.getArgument(0)).get(0));
            return null;
        }).when(glfbo).glDeleteFramebuffersEXT(any());
        GLRenderer renderer = new GLRenderer(mock(GL.class), mock(GLExt.class), glfbo);
        FrameBuffer frameBuffer = new FrameBuffer(4, 4, 1);
        frameBuffer.setId(17);
        renderer.registerNativeObject(frameBuffer);
        renderer.deleteFrameBuffer(frameBuffer);
        assertDoesNotThrow(renderer::cleanup);
        assertEquals(Collections.singletonList(17), deleted);
    }

    @Test
    void explicitlyDeletedBuffersAndShadersAreNotDeletedAgainAtShutdown() {
        GLRenderer renderer = new GLRenderer(mock(GL.class), mock(GLExt.class), mock(GLFbo.class));
        NativeObject[] objects = {
                new VertexBuffer(VertexBuffer.Type.Position), new BufferObject(), new Shader()
        };
        for (NativeObject object : objects) {
            object.setId(17);
            renderer.registerNativeObject(object);
            object.deleteObject(renderer);
        }
        assertDoesNotThrow(renderer::cleanup);
    }
}
