/**
 * Copyright (c) 2025, Nostr Game Engine
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
 * the BSD 3-Clause License. The original jMonkeyEngine license is as follows:
 */
package org.ngengine.gui.components;

import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.simsilica.lemur.component.QuadBackgroundComponent;

public class NAspectPreservingQuadBackground extends QuadBackgroundComponent {

    public NAspectPreservingQuadBackground() {
        super();
    }

    public NAspectPreservingQuadBackground(ColorRGBA color) {
        super(color);
    }

    public NAspectPreservingQuadBackground(ColorRGBA color, float xMargin, float yMargin) {
        super(color, xMargin, yMargin);
    }

    public NAspectPreservingQuadBackground(ColorRGBA color, float xMargin, float yMargin, float zOffset, boolean lit) {
        super(color, xMargin, yMargin, zOffset, lit);
    }

    @Override
    public void reshape(Vector3f pos, Vector3f size) {
        float smallestDimension = Math.min(size.x, size.y);
        float xMargin = size.x - smallestDimension;
        float yMargin = size.y - smallestDimension;
        size.x = smallestDimension;
        size.y = smallestDimension;
        pos.x += xMargin / 2;
        pos.y += yMargin / 2;

        super.reshape(pos, size);
    }
}
