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

package io.github.jmecn.tiled.core;

/**
 * This element is used to describe which transformations can be applied to the tiles
 * (e.g. to extend a Wang set by transforming existing tiles). (since 1.5)
 *
 * @author yanmaoyuan
 */
public class TiledTransformations {
    /**
     * Whether the tiles in this set can be flipped horizontally (default 0)
     */
    private int verticallyFlip;
    /**
     * Whether the tiles in this set can be flipped vertically (default 0)
     */
    private int horizontallyFlip;
    /**
     * Whether the tiles in this set can be rotated in 90 degree increments (default 0)
     */
    private int rotate;
    /**
     * Whether untransformed tiles remain preferred, otherwise transformed tiles are
     * used to produce more variations (default 0)
     */
    private int preferUntransformed;

    public TiledTransformations() {
        this.horizontallyFlip = 0;
        this.verticallyFlip = 0;
        this.rotate = 0;
        this.preferUntransformed = 0;
    }

    public TiledTransformations(int verticallyFlip, int horizontallyFlip, int rotate, int preferUntransformed) {
        this.verticallyFlip = verticallyFlip;
        this.horizontallyFlip = horizontallyFlip;
        this.rotate = rotate;
        this.preferUntransformed = preferUntransformed;
    }

    public int getVerticallyFlip() {
        return verticallyFlip;
    }

    public void setVerticallyFlip(int verticallyFlip) {
        this.verticallyFlip = verticallyFlip;
    }

    public int getHorizontallyFlip() {
        return horizontallyFlip;
    }

    public void setHorizontallyFlip(int horizontallyFlip) {
        this.horizontallyFlip = horizontallyFlip;
    }

    public int getRotate() {
        return rotate;
    }

    public void setRotate(int rotate) {
        this.rotate = rotate;
    }

    public int getPreferUntransformed() {
        return preferUntransformed;
    }

    public void setPreferUntransformed(int preferUntransformed) {
        this.preferUntransformed = preferUntransformed;
    }
}