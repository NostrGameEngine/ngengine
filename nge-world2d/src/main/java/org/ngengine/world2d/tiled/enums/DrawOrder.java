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

package org.ngengine.world2d.tiled.enums;

import java.util.Comparator;

import org.ngengine.world2d.tiled.core.entity.TiledObjectEntity;

/**
 * Whether the objects are drawn according to the order of appearance
 * ("index") or sorted by their y-coordinate ("topdown"). Defaults to
 * "topdown".
 */
public enum DrawOrder implements Comparator<TiledObjectEntity> {
    INDEX("index") {
        @Override
        public int compare(TiledObjectEntity o1, TiledObjectEntity o2) {
            if (o1.getId() == null && o2.getId() == null) {
                return 0;
            }
            if (o1.getId() == null) {
                return -1;
            }
            if (o2.getId() == null) {
                return 1;
            }
            return o1.getId().compareTo(o2.getId());
        }
    },
    TOPDOWN("topdown") {
        @Override
        public int compare(TiledObjectEntity o1, TiledObjectEntity o2) {
            return Double.compare(o1.getY(), o2.getY());
        }
    };

    private final String value;
    DrawOrder(String value) {
        this.value = value;
    }

    public static DrawOrder fromValue(String value) {
        for (DrawOrder c : DrawOrder.values()) {
            if (c.value.equals(value)) {
                return c;
            }
        }
        return TOPDOWN;
    }
}
