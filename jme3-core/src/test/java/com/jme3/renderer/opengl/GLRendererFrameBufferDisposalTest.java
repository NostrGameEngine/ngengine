/*
 * Copyright (c) 2026 jMonkeyEngine
 * All rights reserved.
 */
package com.jme3.renderer.opengl;

import com.jme3.texture.FrameBuffer;
import java.util.Set;
import java.util.HashSet;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class GLRendererFrameBufferDisposalTest extends FrameBufferTestSupport {
    @Test
    void deletesEveryRenderbufferByItsOwnIdWithoutDeletingTextureAttachments() {
        FrameBuffer framebuffer = bufferedFramebuffer(2);
        var texture = texture();
        framebuffer.addColorTarget(FrameBuffer.FrameBufferTarget.newTarget(texture));
        renderer.setFrameBuffer(framebuffer);
        Set<Integer> ids = Set.of(framebuffer.getDepthTarget().getId(),
                framebuffer.getColorTarget(0).getId(), framebuffer.getColorTarget(1).getId());
        int textureId = texture.getImage().getId();

        renderer.deleteFrameBuffer(framebuffer);

        assertEquals(ids, new HashSet<>(driver.deletedRenderbuffers));
        assertEquals(3, driver.deletedRenderbuffers.size());
        assertTrue(driver.liveTextures.contains(textureId));
        assertEquals(-1, framebuffer.getId());
    }
}
