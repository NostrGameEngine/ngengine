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

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class AnimGroupController {

    protected List<AnimTweenController> controllers = new ArrayList<>();
    protected Float step = null;
    protected boolean play = true;

    public void forEach(Consumer<AnimTweenController> consumer) {
        for (AnimTweenController controller : controllers) {
            consumer.accept(controller);
        }
    }

    public void addIfAbsent(AnimTweenController controller) {
        if (controllers.contains(controller)) {
            return; // Already exists, do not add again
        }
        controllers.add(controller);
        // Iterator<AnimTweenController> iterator = controllers.iterator();

        // while (iterator.hasNext()) {
        //     AnimTweenController c = iterator.next();
        //     // AnimTweenController c = ref.get();
        //     if (c == null) {
        //         iterator.remove(); // Clean up null references
        //     } else  if(c== controller) {
        //         return; // Already exists, do not add again
        //     }
        // }

        // controllers.add(new WeakReference<>(controller));
    }

    public void add(AnimTweenController controller) {
        controllers.add(controller);
    }

    public void remove(AnimTweenController controller) {
        // Iterator<WeakReference<AnimTweenController>> iterator = controllers.iterator();
        // while (iterator.hasNext()) {
        //     WeakReference<AnimTweenController> ref = iterator.next();
        //     AnimTweenController c = ref.get();
        //     if (c == null || c == controller) {
        //         iterator.remove(); // Remove null references or the specified controller
        //     }
        // }
        controllers.remove(controller); // Remove the specified controller
    }

    public void clear() {
        controllers.clear(); // Clear all controllers
    }

    public void unsetStep() {
        step = null;
        forEach(controller -> controller.unsetStep());
    }

    public void setStep(Float i) {
        step = i;
        forEach(controller -> controller.setStep(i));
    }

    public Float getStep() {
        return step;
    }

    public void play() {
        play = true;
        forEach(AnimTweenController::play);
    }

    public void pause() {
        play = false;
        forEach(AnimTweenController::pause);
    }

    public boolean isPlaying() {
        return play;
    }
}
