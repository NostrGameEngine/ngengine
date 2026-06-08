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

package org.ngengine.world2d.tiled.components.fragments;

import org.jbox2d.callbacks.ContactImpulse;
import org.jbox2d.collision.Manifold;
import org.jbox2d.dynamics.World;
import org.jbox2d.dynamics.contacts.Contact;
import org.ngengine.components.ComponentManager;
import org.ngengine.components.fragments.Fragment;

import org.ngengine.world2d.tiled.core.TiledBase;
import org.ngengine.world2d.tiled.core.entity.TiledObjectEntity;
import jakarta.annotation.Nullable;

public interface TiledPhysicsLogicFragment extends Fragment{
    	/**
	 * Called when two fixtures begin to touch.
	 * @param contact
	 */
	public void beforeTiledPhysicsContact(
        ComponentManager mng,
        TiledBase entity,
        TiledBase collider,
        @Nullable TiledObjectEntity entityCollisionObject,
        @Nullable TiledObjectEntity colliderCollisionObject,
        Contact contact
    );
	
	/**
	 * Called when two fixtures cease to touch.
	 * @param contact
	 */
	public void afterTiledPhysicsContact(
        ComponentManager mng,
        TiledBase entity,
        TiledBase collider,
        @Nullable TiledObjectEntity entityCollisionObject,
        @Nullable TiledObjectEntity colliderCollisionObject,
        Contact contact
    );
	
	/**
	 * This is called after a contact is updated. This allows you to inspect a
	 * contact before it goes to the solver. If you are careful, you can modify the
	 * contact manifold (e.g. disable contact).
	 * A copy of the old manifold is provided so that you can detect changes.
	 * Note: this is called only for awake bodies.
	 * Note: this is called even when the number of contact points is zero.
	 * Note: this is not called for sensors.
	 * Note: if you set the number of contact points to zero, you will not
	 * get an EndContact callback. However, you may get a BeginContact callback
	 * the next step.
	 * Note: the oldManifold parameter is pooled, so it will be the same object for every callback
	 * for each thread.
	 * @param contact
	 * @param oldManifold
	 */
	public void beforeTiledPhysicsSolve(
        ComponentManager mng,
        TiledBase entity,
        TiledBase collider,
        @Nullable TiledObjectEntity entityCollisionObject,
        @Nullable TiledObjectEntity colliderCollisionObject,    
        Contact contact, Manifold oldManifold
    );
	
	/**
	 * This lets you inspect a contact after the solver is finished. This is useful
	 * for inspecting impulses.
	 * Note: the contact manifold does not include time of impact impulses, which can be
	 * arbitrarily large if the sub-step is small. Hence the impulse is provided explicitly
	 * in a separate data structure.
	 * Note: this is only called for contacts that are touching, solid, and awake.
	 * @param contact
	 * @param impulse this is usually a pooled variable, so it will be modified after
	 * this call
	 */
	public void afterTiledPhysicsSolve(
        ComponentManager mng,
        TiledBase entity,
        TiledBase collider,
        @Nullable TiledObjectEntity entityCollisionObject,
        @Nullable TiledObjectEntity colliderCollisionObject,    
        Contact contact, ContactImpulse impulse
    );


    // public void updateTiledPhysicsLogicAsync(ComponentManager mng, float tpf, World physics);

}
