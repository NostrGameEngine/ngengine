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
 * the BSD 3-Clause License. 
 */
package org.ngengine.platform;

/**
 * Platform abstraction layer for NGE applications.
 * Provides access to platform-specific functionality like clipboard operations.
 */
public abstract class NGEPlatform {
    
    private static NGEPlatform instance;
    
    /**
     * Get the current platform instance.
     * @return the platform instance
     */
    public static NGEPlatform get() {
        if (instance == null) {
            instance = createDefaultPlatform();
        }
        return instance;
    }
    
    /**
     * Set the platform instance (mainly for testing).
     * @param platform the platform instance to use
     */
    public static void set(NGEPlatform platform) {
        instance = platform;
    }
    
    /**
     * Create the default platform implementation based on the current environment.
     * @return the default platform implementation
     */
    private static NGEPlatform createDefaultPlatform() {
        return new DesktopPlatform();
    }
    
    /**
     * Set text content to the system clipboard.
     * @param text the text to copy to clipboard
     */
    public abstract void setClipboardContent(String text);
    
    /**
     * Get text content from the system clipboard.
     * @return the clipboard text content, or empty string if unavailable
     */
    public abstract String getClipboardContent();
}