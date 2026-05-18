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

package io.github.jmecn.tiled.animation;

import java.util.ArrayList;
import java.util.List;

/**
 * Contains a list of animation frames.
 * 
 * As of Tiled 0.10, each tile can have exactly one animation associated with
 * it. In the future, there could be support for multiple named animations on a
 * tile.
 * 
 * Can contain: frame
 * 
 * @author yanmaoyuan
 * 
 */
public class Animation {

    public static final int MASK_ANIMATION = 0x0000000F;

    public static final int KEY_LOOP = 0x01;
    public static final int KEY_STOP = 0x02;
    public static final int KEY_AUTO = 0x04;
    public static final int KEY_REVERSE = 0x08;

    public static final int KEY_NAME_LENGTH_MAX = 32;

    private String name = null;
    private int id = -1;
    private int flags;
    private float frameRate = 1.0f; // one fps
    
    // animation
    private final List<Frame> frames;

    public Animation() {
        flags = KEY_LOOP;
        frames = new ArrayList<>();
    }

    public Animation(String name) {
        this();
        this.name = name;
    }

    public Animation(String name, List<Frame> frames) {
        this(name);
        this.frames.addAll(frames);
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setFrameRate(float r) {
        frameRate = r;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getId() {
        return id;
    }

    public int getLastFrame() {
        return frames.size() - 1;
    }

    public boolean isFrameLast(int frame) {
        return frames.size() - 1 == frame;
    }

    public void setFlags(int f) {
        flags = f;
    }

    public int getFlags() {
        return flags;
    }

    public String getName() {
        return name;
    }

    public void addFrame(Frame frame) {
        frames.add(frame);
    }
    
    public Frame getFrame(int f) {
        if (f >= 0 && f < frames.size()) {
            return frames.get(f);
        }
        return null;
    }

    public float getFrameRate() {
        return frameRate;
    }

    public int getTotalFrames() {
        return frames.size();
    }

    public boolean equalsIgnoreCase(String n) {
        return name != null && name.equalsIgnoreCase(n);
    }

    @Override
    public String toString() {
        return "(" + name + ")" + id + ": @ " + frameRate;
    }
}