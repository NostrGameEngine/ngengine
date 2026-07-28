/*
 * Copyright (c) 2026 jMonkeyEngine
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * * Redistributions of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 *
 * * Redistributions in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer in the
 *   documentation and/or other materials provided with the distribution.
 *
 * * Neither the name of 'jMonkeyEngine' nor the names of its contributors
 *   may be used to endorse or promote products derived from this software
 *   without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */
package com.jme3.app.state;

import com.jme3.math.FastMath;
import com.jme3.renderer.Camera;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.ViewPort;
import com.jme3.system.NullRenderer;
import com.jme3.texture.FrameBuffer;
import com.jme3.texture.FrameBuffer.FrameBufferTarget;
import com.jme3.texture.Image;
import com.jme3.util.BufferUtils;
import java.io.File;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

class ScreenshotAppStateTest {

    @Test
    void usesPhysicalRenderTargetWhenLogicalCameraSizeDiffers() {
        Camera camera = new Camera(1778, 1000);
        camera.setViewPort(0.25f, 0.75f, 0.1f, 0.9f);
        ViewPort viewPort = new ViewPort("relative GUI", camera);
        viewPort.setRenderTargetSize(1280, 720);
        RecordingRenderer renderer = new RecordingRenderer();
        RenderManager renderManager = new RenderManager(renderer);
        ScreenshotWithoutFile state = new ScreenshotWithoutFile();

        state.initialize(renderManager, viewPort);
        renderManager.setCamera(camera, false, 1280, 720);
        renderer.viewPorts.clear();
        state.takeScreenshot();
        state.postFrame(null);

        assertEquals(1280 * 720 * 4, renderer.readBufferSize);
        assertEquals(List.of(
                List.of(0, 0, 1280, 720),
                List.of(320, 72, 640, 576)
        ), renderer.viewPorts);
    }

    @Test
    void readsRgba16fTargetInItsNativeFormat() {
        Camera camera = new Camera(1, 1);
        ViewPort viewPort = new ViewPort("main", camera);
        viewPort.setRenderTargetSize(1, 1);
        FrameBuffer target = new FrameBuffer(1, 1, 1);
        target.addColorTarget(FrameBufferTarget.newTarget(Image.Format.RGBA16F));
        RecordingRenderer renderer = new RecordingRenderer();
        renderer.currentFrameBuffer = target;
        RenderManager renderManager = new RenderManager(renderer);
        ScreenshotWithoutFile state = new ScreenshotWithoutFile();

        state.initialize(renderManager, viewPort);
        renderManager.setCamera(camera, false, 1, 1);
        state.takeScreenshot();
        state.postFrame(null);

        assertEquals(Image.Format.RGBA16F, renderer.readFormat);
        assertEquals(8, renderer.readBufferSize);
        assertEquals(0, renderer.rgba8Reads);
    }

    @Test
    void convertsLinearHalfFloatPixelsToSrgbRgba8() {
        ByteBuffer source = BufferUtils.createByteBuffer(8);
        source.putShort(FastMath.convertFloatToHalf(0f));
        source.putShort(FastMath.convertFloatToHalf(0.25f));
        source.putShort(FastMath.convertFloatToHalf(1f));
        source.putShort(FastMath.convertFloatToHalf(0.5f));
        ByteBuffer target = BufferUtils.createByteBuffer(4);

        ScreenshotAppState.convertRgba16fToRgba8(source, target, true);

        byte[] actual = new byte[4];
        target.get(actual);
        assertArrayEquals(new byte[] {0, (byte) 137, (byte) 255, (byte) 128}, actual);
    }

    private static class RecordingRenderer extends NullRenderer {
        private final List<List<Integer>> viewPorts = new ArrayList<>();
        private int readBufferSize;
        private int rgba8Reads;
        private Image.Format readFormat;
        private FrameBuffer currentFrameBuffer;

        @Override
        public void setViewPort(int x, int y, int width, int height) {
            viewPorts.add(List.of(x, y, width, height));
        }

        @Override
        public void readFrameBuffer(FrameBuffer fb, ByteBuffer byteBuf) {
            readBufferSize = byteBuf.remaining();
            rgba8Reads++;
        }

        @Override
        public void readFrameBufferWithFormat(FrameBuffer fb, ByteBuffer byteBuf, Image.Format format) {
            readBufferSize = byteBuf.remaining();
            readFormat = format;
        }

        @Override
        public FrameBuffer getCurrentFrameBuffer() {
            return currentFrameBuffer;
        }
    }

    private static class ScreenshotWithoutFile extends ScreenshotAppState {
        ScreenshotWithoutFile() {
            super("", "screenshot-test");
        }

        @Override
        protected void writeImageFile(File file) {
        }
    }
}
