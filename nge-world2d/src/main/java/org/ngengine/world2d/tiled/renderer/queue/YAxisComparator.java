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

package org.ngengine.world2d.tiled.renderer.queue;

import com.jme3.bounding.BoundingBox;
import com.jme3.bounding.BoundingVolume;
import com.jme3.renderer.Camera;
import com.jme3.renderer.queue.GeometryComparator;
import com.jme3.scene.Geometry;

/**
 * Sorts tiled world geometries by their highest world-space Y coordinate.
 * <p>
 * Instanced batches keep their instance data in world space and set bounds over
 * all contained instances, so using the bound's top edge gives the render queue
 * a stable back-to-front key for both regular geometries and batch geometries.
 * </p>
 */
public class YAxisComparator implements GeometryComparator {
    public static final String SORT_Y_USER_DATA = "ngengine.world2d.sortY";
    public static final String SORT_ORDER_USER_DATA = "ngengine.world2d.sortOrder";
    private final boolean useBoundingBox = true;
    @Override
    public int compare(Geometry o1, Geometry o2) {
        if(!useBoundingBox){
            float y1 = o1.getWorldTranslation().getY();
            float y2 = o2.getWorldTranslation().getY();
            int result = Float.compare(y1, y2);
            return result != 0 ? result : Float.compare(sortOrder(o1), sortOrder(o2));
        } else {
            float y1 = sortY(o1);
            float y2 = sortY(o2);
            int result = Float.compare(y1, y2);
            return result != 0 ? result : Float.compare(sortOrder(o1), sortOrder(o2));
        }
    }

    /**
     * Returns the Y value used as the render-queue sort key for a geometry.
     *
     * @param geometry the geometry to sort
     * @return the top Y edge of the world bound, or the world translation Y when
     *         no bound is available
     */
    public static float sortY(Geometry geometry) {
        Number explicitSortY = geometry.getUserData(SORT_Y_USER_DATA);
        if (explicitSortY != null) {
            return explicitSortY.floatValue();
        }
        BoundingVolume bound = geometry.getWorldBound();
        if (bound instanceof BoundingBox) {
            BoundingBox box = (BoundingBox) bound;
            return box.getCenter().y + box.getYExtent();
        }
        return bound != null ? bound.getCenter().y : geometry.getWorldTranslation().getY();
    }

    /**
     * Returns a deterministic tie-break key for geometries with the same sort Y.
     *
     * @param geometry the geometry to sort
     * @return an explicit authoring-order key, or {@code 0} when absent
     */
    public static float sortOrder(Geometry geometry) {
        Number explicitSortOrder = geometry.getUserData(SORT_ORDER_USER_DATA);
        return explicitSortOrder != null ? explicitSortOrder.floatValue() : 0f;
    }

    @Override
    public void setCamera(Camera cam) {
        // nothing
    }
}
