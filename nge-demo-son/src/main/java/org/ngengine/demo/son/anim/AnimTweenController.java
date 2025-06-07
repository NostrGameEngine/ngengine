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
package org.ngengine.demo.son.anim;

import com.jme3.anim.tween.Tween;

public class AnimTweenController implements Tween {

    protected final Tween source;
    protected double currentTime = 0;
    protected Float step = null;
    protected boolean play = true;

    public AnimTweenController(Tween source) {
        this.source = source;
    }

    @Override
    public double getLength() {
        return source.getLength();
    }

    public Tween getSource() {
        return source;
    }

    public void unsetStep() {
        step = null;
    }

    public void setStep(Float i) {
        if (i == null) {
            unsetStep();
            return;
        }
        step = i;
    }

    public Float getStep() {
        return step;
    }

    public void pause() {
        play = false;
    }

    public void play() {
        play = true;
    }

    public boolean isPlaying() {
        return play;
    }

    @Override
    public boolean interpolate(double t) {
        if (!play) {
            return source.interpolate(currentTime);
        }
        if (step != null) {
            double length = getLength();
            double time = length * (double) step;
            currentTime = time;

            return source.interpolate(currentTime);
        } else {
            currentTime = t;
            return source.interpolate(currentTime);
        }
    }
}
