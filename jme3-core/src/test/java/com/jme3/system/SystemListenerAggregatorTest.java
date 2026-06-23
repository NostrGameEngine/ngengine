/*
 * Copyright (c) 2009-2026 jMonkeyEngine
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
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
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
 * TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; OR BUSINESS INTERRUPTION)
 * HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 * LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY
 * OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH
 * DAMAGE.
 */
package com.jme3.system;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SystemListenerAggregatorTest {

    @Test
    void forwardsModernReshape() {
        SystemListenerAggregator aggregator = new SystemListenerAggregator();
        ReshapeListener listener = new ReshapeListener();
        aggregator.addListener(listener);

        aggregator.reshape(320, 240, 640, 480);

        assertEquals(1, listener.modernReshapeCalls);
        assertEquals(320, listener.logicalWidth);
        assertEquals(240, listener.logicalHeight);
        assertEquals(640, listener.framebufferWidth);
        assertEquals(480, listener.framebufferHeight);
        assertEquals(0, listener.legacyReshapeCalls);
    }

    private static final class ReshapeListener implements SystemListener {

        private int legacyReshapeCalls;
        private int modernReshapeCalls;
        private int logicalWidth;
        private int logicalHeight;
        private int framebufferWidth;
        private int framebufferHeight;

        @Override
        public void initialize() {
        }

        @Override
        public void reshape(int width, int height) {
            legacyReshapeCalls++;
        }

        @Override
        public void reshape(int logicalWidth, int logicalHeight, int framebufferWidth, int framebufferHeight) {
            modernReshapeCalls++;
            this.logicalWidth = logicalWidth;
            this.logicalHeight = logicalHeight;
            this.framebufferWidth = framebufferWidth;
            this.framebufferHeight = framebufferHeight;
        }

        @Override
        public void update() {
        }

        @Override
        public void requestClose(boolean esc) {
        }

        @Override
        public void gainFocus() {
        }

        @Override
        public void loseFocus() {
        }

        @Override
        public void handleError(String errorMsg, Throwable t) {
        }

        @Override
        public void destroy() {
        }
    }
}
