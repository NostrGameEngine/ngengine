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

import com.jme3.math.Vector2f;
import com.simsilica.lemur.GuiGlobals;
import com.simsilica.lemur.component.IconComponent;
import org.ngengine.gui.svg.SVGTextureKey;

public class NSVGIcon extends IconComponent {

    public NSVGIcon(String imagePath, int width, int height) {
        this(imagePath, width, height, new Vector2f(1, 1), 0, 0, 0, false);
        if (width < 2) width = 2;
        if (height < 2) height = 2;
        setIconSize(new Vector2f(width, height));
    }

    public NSVGIcon(
        String imagePath,
        int width,
        int height,
        Vector2f iconScale,
        float xMargin,
        float yMargin,
        float zOffset,
        boolean lit
    ) {
        super(
            GuiGlobals
                .getInstance()
                .loadTexture(new SVGTextureKey(imagePath, width < 2 ? 2 : width, height < 2 ? 2 : height), false),
            iconScale,
            xMargin,
            yMargin,
            zOffset,
            lit
        );
        if (width < 2) width = 2;
        if (height < 2) height = 2;
        setIconSize(new Vector2f(width, height));
    }
}
