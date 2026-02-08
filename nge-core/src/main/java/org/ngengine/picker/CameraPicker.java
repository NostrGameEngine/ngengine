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

package org.ngengine.picker;

import com.jme3.collision.Collidable;
import com.jme3.collision.CollisionResult;
import com.jme3.collision.CollisionResults;
import com.jme3.math.Ray;
import com.jme3.renderer.Camera;
import jakarta.annotation.Nullable;

public class CameraPicker {

    private static final ThreadLocal<CollisionResults> resultsTl = ThreadLocal.withInitial(CollisionResults::new);
    private static final ThreadLocal<Ray> rayTl = ThreadLocal.withInitial(Ray::new);
    public static final CameraPickerBehavior CLOSER_VISIBLE_PICKER = new CloserVisibleCameraPickerBehavior();

    /**
     * Picks the closest visible object in the scene using the {@link #CLOSER_VISIBLE_PICKER} behavior.
     * @param rootNode the root node to pick against
     * @param cam the camera to pick from
     * @return the collision result, or null if nothing was picked
     */
    @Nullable public static CollisionResult pick(Collidable rootNode, Camera cam) {
        return pick(rootNode, cam, CLOSER_VISIBLE_PICKER);
    }

    /**
     * Picks an object in the scene using the specified picker behavior.
     * @param rootNode the root node to pick against
     * @param cam the camera to pick from
     * @param picker the picker behavior to use
     * @return the collision result, or null if nothing was picked
     */
    @Nullable public static CollisionResult pick(Collidable rootNode, Camera cam, CameraPickerBehavior picker) {
        CollisionResults results = resultsTl.get();
        results.clear();
        Ray ray = rayTl.get();
        ray.setOrigin(cam.getLocation());
        ray.setDirection(cam.getDirection());
        ray = picker.tweakRay(rootNode, cam, ray);
        rootNode.collideWith(ray, results);
        CollisionResult result = picker.select(rootNode, cam, results);
        return result;
    }
}
