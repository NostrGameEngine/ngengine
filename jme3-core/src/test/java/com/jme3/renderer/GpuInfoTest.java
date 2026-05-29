/*
 * Copyright (c) 2025 jMonkeyEngine
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

import com.jme3.system.NullRenderer;
import org.junit.Assert;
import org.junit.Test;

/**
 * Tests for GPU information functionality.
 *
 * @author jMonkeyEngine
 */
public class GpuInfoTest {

    /**
     * Test basic GpuInfo construction and getter methods.
     */
    @Test
    public void testGpuInfoConstruction() {
        GpuInfo gpuInfo = new GpuInfo("NVIDIA Corporation", "GeForce GTX 1080", 
                                     "4.6.0 NVIDIA 456.71", "4.60 NVIDIA", "Core");

        Assert.assertEquals("NVIDIA Corporation", gpuInfo.getVendor());
        Assert.assertEquals("GeForce GTX 1080", gpuInfo.getRenderer());
        Assert.assertEquals("4.6.0 NVIDIA 456.71", gpuInfo.getVersion());
        Assert.assertEquals("4.60 NVIDIA", gpuInfo.getGlslVersion());
        Assert.assertEquals("Core", gpuInfo.getProfile());
    }

    /**
     * Test GpuInfo construction with null values.
     */
    @Test
    public void testGpuInfoWithNulls() {
        GpuInfo gpuInfo = new GpuInfo(null, null, null, null, null);

        Assert.assertEquals("Unknown", gpuInfo.getVendor());
        Assert.assertEquals("Unknown", gpuInfo.getRenderer());
        Assert.assertEquals("Unknown", gpuInfo.getVersion());
        Assert.assertEquals("Unknown", gpuInfo.getGlslVersion());
        Assert.assertEquals("Unknown", gpuInfo.getProfile());
    }

    /**
     * Test low-end GPU detection for Intel integrated graphics.
     */
    @Test
    public void testLowEndDetectionIntel() {
        GpuInfo intel1 = new GpuInfo("Intel", "Intel(R) HD Graphics 4000", 
                                    "4.0.0", "4.00", "Compatibility");
        Assert.assertTrue("Intel HD Graphics should be detected as low-end", intel1.isLowEndGpu());

        GpuInfo intel2 = new GpuInfo("Intel Corporation", "Intel(R) UHD Graphics 620", 
                                    "4.5.0", "4.50", "Core");
        Assert.assertTrue("Intel UHD Graphics should be detected as low-end", intel2.isLowEndGpu());

        GpuInfo intel3 = new GpuInfo("Intel", "Intel(R) Iris(TM) Graphics 6100", 
                                    "4.3.0", "4.30", "Core");
        Assert.assertTrue("Intel Iris Graphics should be detected as low-end", intel3.isLowEndGpu());
    }

    /**
     * Test low-end GPU detection for software rendering.
     */
    @Test
    public void testLowEndDetectionSoftware() {
        GpuInfo software1 = new GpuInfo("VMware, Inc.", "llvmpipe (LLVM 10.0.0, 256 bits)", 
                                       "3.1 Mesa 20.0.0", "3.10", "Core");
        Assert.assertTrue("Software rendering should be detected as low-end", software1.isLowEndGpu());

        GpuInfo software2 = new GpuInfo("Microsoft Corporation", "GDI Generic", 
                                       "1.1.0", "1.10", "Compatibility");
        Assert.assertTrue("Generic software renderer should be detected as low-end", software2.isLowEndGpu());
    }

    /**
     * Test low-end GPU detection for NVIDIA GT series.
     */
    @Test
    public void testLowEndDetectionNvidiaGT() {
        GpuInfo nvidiaGT = new GpuInfo("NVIDIA Corporation", "GeForce GT 730", 
                                      "4.6.0 NVIDIA 456.71", "4.60 NVIDIA", "Core");
        Assert.assertTrue("NVIDIA GT series should be detected as low-end", nvidiaGT.isLowEndGpu());

        GpuInfo nvidiaMX = new GpuInfo("NVIDIA Corporation", "GeForce MX150", 
                                      "4.6.0 NVIDIA 456.71", "4.60 NVIDIA", "Core");
        Assert.assertTrue("NVIDIA MX series should be detected as low-end", nvidiaMX.isLowEndGpu());
    }

    /**
     * Test that high-end GPUs are not detected as low-end.
     */
    @Test
    public void testHighEndDetection() {
        GpuInfo nvidiaRTX = new GpuInfo("NVIDIA Corporation", "GeForce RTX 3080", 
                                       "4.6.0 NVIDIA 456.71", "4.60 NVIDIA", "Core");
        Assert.assertFalse("NVIDIA RTX should not be detected as low-end", nvidiaRTX.isLowEndGpu());

        GpuInfo amdRX = new GpuInfo("Advanced Micro Devices, Inc.", "AMD Radeon RX 6800 XT", 
                                   "4.6.0", "4.60", "Core");
        Assert.assertFalse("AMD RX should not be detected as low-end", amdRX.isLowEndGpu());

        GpuInfo nvidiaGTX = new GpuInfo("NVIDIA Corporation", "GeForce GTX 1080", 
                                       "4.6.0 NVIDIA 456.71", "4.60 NVIDIA", "Core");
        Assert.assertFalse("NVIDIA GTX (non-GT) should not be detected as low-end", nvidiaGTX.isLowEndGpu());
    }

    /**
     * Test equals and hashCode methods.
     */
    @Test
    public void testEqualsAndHashCode() {
        GpuInfo gpu1 = new GpuInfo("NVIDIA Corporation", "GeForce GTX 1080", 
                                  "4.6.0 NVIDIA 456.71", "4.60 NVIDIA", "Core");
        GpuInfo gpu2 = new GpuInfo("NVIDIA Corporation", "GeForce GTX 1080", 
                                  "4.6.0 NVIDIA 456.71", "4.60 NVIDIA", "Core");
        GpuInfo gpu3 = new GpuInfo("AMD", "Radeon RX 580", 
                                  "4.6.0", "4.60", "Core");

        Assert.assertEquals("Same GPU info should be equal", gpu1, gpu2);
        Assert.assertEquals("Same GPU info should have same hash code", gpu1.hashCode(), gpu2.hashCode());
        Assert.assertNotEquals("Different GPU info should not be equal", gpu1, gpu3);
    }

    /**
     * Test toString method.
     */
    @Test
    public void testToString() {
        GpuInfo gpuInfo = new GpuInfo("NVIDIA Corporation", "GeForce GTX 1080", 
                                     "4.6.0 NVIDIA 456.71", "4.60 NVIDIA", "Core");
        String str = gpuInfo.toString();
        
        Assert.assertTrue("toString should contain vendor", str.contains("NVIDIA Corporation"));
        Assert.assertTrue("toString should contain renderer", str.contains("GeForce GTX 1080"));
        Assert.assertTrue("toString should contain version", str.contains("4.6.0 NVIDIA 456.71"));
        Assert.assertTrue("toString should contain GLSL version", str.contains("4.60 NVIDIA"));
        Assert.assertTrue("toString should contain profile", str.contains("Core"));
    }

    /**
     * Test NullRenderer returns null for GPU info.
     */
    @Test
    public void testNullRendererGpuInfo() {
        NullRenderer nullRenderer = new NullRenderer();
        Assert.assertNull("NullRenderer should return null GPU info", nullRenderer.getGpuInfo());
    }
}