/*
 * Copyright (c) 2009-2024 jMonkeyEngine
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 * * Redistributions of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 *
 * * Redistributions in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer in the
 *   documentation and/or other materials provided with the distribution.
 *
 * * Neither the name of 'jMonkeyEngine' nor the names of its contributors
 *   may be used to endorse or promote products derived from this software
 *   without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
 * TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.jme3.renderer;

/**
 * Holds information about the GPU hardware and drivers.
 * This information can be used to detect device capabilities
 * and adjust performance settings accordingly.
 *
 * @author jMonkeyEngine
 */
public class GpuInfo {

    private final String vendor;
    private final String renderer;
    private final String version;
    private final String glslVersion;
    private final String profile;

    /**
     * Creates a new GpuInfo instance.
     *
     * @param vendor The GPU vendor (e.g., "NVIDIA Corporation", "AMD", "Intel")
     * @param renderer The GPU renderer string (e.g., "GeForce GTX 1080")
     * @param version The OpenGL version string
     * @param glslVersion The GLSL version string
     * @param profile The OpenGL profile ("Core" or "Compatibility")
     */
    public GpuInfo(String vendor, String renderer, String version, String glslVersion, String profile) {
        this.vendor = vendor != null ? vendor : "Unknown";
        this.renderer = renderer != null ? renderer : "Unknown";
        this.version = version != null ? version : "Unknown";
        this.glslVersion = glslVersion != null ? glslVersion : "Unknown";
        this.profile = profile != null ? profile : "Unknown";
    }

    /**
     * Gets the GPU vendor name.
     *
     * @return The vendor name (e.g., "NVIDIA Corporation", "AMD", "Intel")
     */
    public String getVendor() {
        return vendor;
    }

    /**
     * Gets the GPU renderer name.
     *
     * @return The renderer name (e.g., "GeForce GTX 1080", "Radeon RX 580")
     */
    public String getRenderer() {
        return renderer;
    }

    /**
     * Gets the OpenGL version string.
     *
     * @return The OpenGL version
     */
    public String getVersion() {
        return version;
    }

    /**
     * Gets the GLSL (OpenGL Shading Language) version string.
     *
     * @return The GLSL version
     */
    public String getGlslVersion() {
        return glslVersion;
    }

    /**
     * Gets the OpenGL profile being used.
     *
     * @return "Core", "Compatibility", or "Unknown"
     */
    public String getProfile() {
        return profile;
    }

    /**
     * Checks if this appears to be a low-end GPU based on vendor and renderer strings.
     * This is a heuristic method that may not be 100% accurate.
     *
     * @return true if this appears to be a low-end GPU
     */
    public boolean isLowEndGpu() {
        String lowerVendor = vendor.toLowerCase();
        String lowerRenderer = renderer.toLowerCase();

        // Intel integrated graphics (most are considered low-end for gaming)
        if (lowerVendor.contains("intel") && 
            (lowerRenderer.contains("hd graphics") || 
             lowerRenderer.contains("uhd graphics") ||
             lowerRenderer.contains("iris") ||
             lowerRenderer.contains("integrated"))) {
            return true;
        }

        // Software rendering
        if (lowerRenderer.contains("software") || 
            lowerRenderer.contains("llvmpipe") ||
            lowerRenderer.contains("mesa")) {
            return true;
        }

        // Very old or low-end discrete GPUs
        if (lowerRenderer.contains("geforce") && 
            (lowerRenderer.contains("gt ") || lowerRenderer.contains("mx "))) {
            return true;
        }

        return false;
    }

    @Override
    public String toString() {
        return "GpuInfo{" +
                "vendor='" + vendor + '\'' +
                ", renderer='" + renderer + '\'' +
                ", version='" + version + '\'' +
                ", glslVersion='" + glslVersion + '\'' +
                ", profile='" + profile + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        GpuInfo gpuInfo = (GpuInfo) o;

        if (!vendor.equals(gpuInfo.vendor)) return false;
        if (!renderer.equals(gpuInfo.renderer)) return false;
        if (!version.equals(gpuInfo.version)) return false;
        if (!glslVersion.equals(gpuInfo.glslVersion)) return false;
        return profile.equals(gpuInfo.profile);
    }

    @Override
    public int hashCode() {
        int result = vendor.hashCode();
        result = 31 * result + renderer.hashCode();
        result = 31 * result + version.hashCode();
        result = 31 * result + glslVersion.hashCode();
        result = 31 * result + profile.hashCode();
        return result;
    }
}