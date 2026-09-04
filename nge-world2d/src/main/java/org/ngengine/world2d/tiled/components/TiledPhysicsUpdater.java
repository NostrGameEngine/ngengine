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

package org.ngengine.world2d.tiled.components;

import org.box2d4j.b2ContactBeginTouchEvent;
import org.box2d4j.b2ContactEndTouchEvent;
import org.ngengine.components.Component;
import org.ngengine.components.ComponentManager;
import org.ngengine.runner.MainThreadRunner;
import org.ngengine.world2d.tiled.components.fragments.TiledPhysicsLogicFragment;
import org.ngengine.world2d.tiled.core.TiledBase;
import org.ngengine.world2d.tiled.core.entity.TiledObjectEntity;
import jakarta.annotation.Nullable;

public class TiledPhysicsUpdater  {
  
    public TiledPhysicsUpdater(){}

    public boolean canManage(ComponentManager componentManager, Component fragment){
     
        return fragment instanceof TiledPhysicsLogicFragment;
    }

 
  
    public void beforeTiledPhysicsContact(
        ComponentManager mng,
        Component component,
        TiledBase entityA,
        TiledBase entityB,
        @Nullable TiledObjectEntity colliderA,
        @Nullable TiledObjectEntity colliderB,
        b2ContactBeginTouchEvent contact
    ){
  
        if (component instanceof TiledPhysicsLogicFragment){
            MainThreadRunner runner = mng.getInstanceOf(MainThreadRunner.class);
            runner.enqueue(()->{;
                TiledPhysicsLogicFragment physicsFragment = (TiledPhysicsLogicFragment) component;
                physicsFragment.beforeTiledPhysicsContact(
                    mng,
                    entityA,
                    entityB,
                    colliderA,
                    colliderB,
                    contact
                );
            });
        }
    }


   	public void afterTiledPhysicsContact(
        ComponentManager mng,
        Component component,
        TiledBase entityA,
        TiledBase entityB,
        @Nullable TiledObjectEntity colliderA,
        @Nullable TiledObjectEntity colliderB,
        b2ContactEndTouchEvent contact
    ){
  
        if (component instanceof TiledPhysicsLogicFragment){
            MainThreadRunner runner = mng.getInstanceOf(MainThreadRunner.class);
            runner.enqueue(()->{;
                TiledPhysicsLogicFragment physicsFragment = (TiledPhysicsLogicFragment) component;
                physicsFragment.afterTiledPhysicsContact(
                    mng,
                    entityA,
                    entityB,
                    colliderA,
                    colliderB,
                    contact
                );
            });
        }
    }

}
