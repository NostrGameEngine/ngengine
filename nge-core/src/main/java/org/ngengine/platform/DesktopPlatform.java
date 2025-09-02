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

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Desktop platform implementation that uses LWJGL3/GLFW for clipboard operations.
 * This avoids AWT dependencies that can cause issues on macOS with GraalVM.
 */
public class DesktopPlatform extends NGEPlatform {
    
    private static final Logger logger = Logger.getLogger(DesktopPlatform.class.getName());
    
    // Simple in-memory fallback for environments where GLFW is not available
    private String fallbackClipboard = "";
    
    @Override
    public void setClipboardContent(String text) {
        if (text == null) {
            text = "";
        }
        
        try {
            if (isGlfwAvailable()) {
                // Use reflection to access GLFW clipboard functions to avoid hard dependency
                setGlfwClipboard(text);
            } else {
                // Fallback: store in memory for testing/development environments
                fallbackClipboard = text;
                logger.log(Level.FINE, "Clipboard content stored in memory fallback");
            }
        } catch (Exception e) {
            // If GLFW fails, fall back to in-memory storage
            fallbackClipboard = text;
            logger.log(Level.WARNING, "Failed to set clipboard content via GLFW, using fallback", e);
        }
    }
    
    @Override
    public String getClipboardContent() {
        try {
            if (isGlfwAvailable()) {
                // Use reflection to access GLFW clipboard functions to avoid hard dependency
                return getGlfwClipboard();
            } else {
                // Fallback: return from memory storage
                logger.log(Level.FINE, "Clipboard content retrieved from memory fallback");
                return fallbackClipboard;
            }
        } catch (Exception e) {
            // If GLFW fails, fall back to in-memory storage
            logger.log(Level.WARNING, "Failed to get clipboard content via GLFW, using fallback", e);
            return fallbackClipboard;
        }
    }
    
    /**
     * Check if GLFW is available in the classpath.
     * @return true if GLFW classes are available
     */
    private boolean isGlfwAvailable() {
        try {
            Class.forName("org.lwjgl.glfw.GLFW");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }
    
    /**
     * Set clipboard content using GLFW.
     * @param text the text to set
     */
    private void setGlfwClipboard(String text) {
        try {
            // Use reflection to avoid hard dependency on LWJGL
            Class<?> glfwClass = Class.forName("org.lwjgl.glfw.GLFW");
            java.lang.reflect.Method glfwSetClipboardString = glfwClass.getMethod("glfwSetClipboardString", long.class, CharSequence.class);
            java.lang.reflect.Method glfwGetCurrentContext = glfwClass.getMethod("glfwGetCurrentContext");
            
            long window = (Long) glfwGetCurrentContext.invoke(null);
            if (window != 0L) {
                glfwSetClipboardString.invoke(null, window, text);
                logger.log(Level.FINE, "Set clipboard content via GLFW");
            } else {
                logger.log(Level.WARNING, "No GLFW window context available for clipboard operation");
            }
        } catch (Exception e) {
            logger.log(Level.WARNING, "Failed to set GLFW clipboard content", e);
        }
    }
    
    /**
     * Get clipboard content using GLFW.
     * @return the clipboard content or empty string if unavailable
     */
    private String getGlfwClipboard() {
        try {
            // Use reflection to avoid hard dependency on LWJGL
            Class<?> glfwClass = Class.forName("org.lwjgl.glfw.GLFW");
            java.lang.reflect.Method glfwGetClipboardString = glfwClass.getMethod("glfwGetClipboardString", long.class);
            java.lang.reflect.Method glfwGetCurrentContext = glfwClass.getMethod("glfwGetCurrentContext");
            
            long window = (Long) glfwGetCurrentContext.invoke(null);
            if (window != 0L) {
                String result = (String) glfwGetClipboardString.invoke(null, window);
                logger.log(Level.FINE, "Retrieved clipboard content via GLFW");
                return result != null ? result : "";
            } else {
                logger.log(Level.WARNING, "No GLFW window context available for clipboard operation");
                return "";
            }
        } catch (Exception e) {
            logger.log(Level.WARNING, "Failed to get GLFW clipboard content", e);
            return "";
        }
    }
}