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
package org.ngengine.components.fragments;

/**
 * Fragments are interfaces that define specialized capabilities for components in the engine.
 * <p>
 * The fragment system provides a composition-based approach to building app components, allowing developers
 * to opt-in to specific engine functionalities without complex inheritance hierarchies or linking code. Each
 * fragment represents a specific capability or integration point with the engine.
 * <p>
 * Common fragment types include:
 * <ul>
 * <li>{@link MainViewPortFragment} - For scene rendering and camera manipulation</li>
 * <li>{@link LogicFragment} - For game logic</li>
 * <li>{@link InputHandlerFragment} - For receiving and processing input events</li>
 * <li>{@link AsyncAssetLoadingFragment} - For loading assets in background threads</li>
 * <li>{@link RenderFragment} - For low-level render operations</li>
 * </ul>
 * <p>
 * When a component implements a fragment interface, the ComponentManager automatically detects this
 * capability and connects the component to the appropriate subsystem, eliminating the need for manual
 * registration or boilerplate code.
 * </p>
 * Multiple fragments can be implemented by a single component, enabling a flexible mix of capabilities
 * through interface composition rather than inheritance.
 *
 *
 * <h3>Method Execution Order</h3>
 *
 * Fragments follow a specific execution sequence:
 * <ol>
 * <li><strong>Initialization Phase:</strong> All {@code receiveXXX} methods (like {@code receiveViewPort},
 * {@code receiveInputManager}) are called when the component is first initialized.</li>
 * <li><strong>Loading Phase:</strong> All {@code load} methods (like {@code loadAssetsAsync}) are called
 * right before the component would be enabled. The component remains in pending state until all load methods
 * complete.</li>
 * <li><strong>Enable Phase:</strong> The
 * {@link org.ngengine.components.Component#onEnable(ComponentManager, Runner, DataStoreProvider, boolean, Object)}
 * method is called when the component is enabled, after the initialization and loading phases are
 * complete.</li>
 * <li><strong>Update Phase:</strong> All update methods (like {@code updateViewPort}, {@code update}) are
 * called every frame for each enabled component.</li>
 * <li><strong>Disable Phase:</strong> The
 * {@link org.ngengine.components.Component#onDisable(ComponentManager, Runner, DataStoreProvider)} method is
 * called when the component is disabled, either explicitly, due to a dependency change or due it being
 * detached.</li>
 * </ol>
 *
 * @see org.ngengine.components.Component
 * @see org.ngengine.components.ComponentManager
 */
public interface Fragment {}
