package com.jme3.shader;

import com.jme3.math.FastMath;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class PBRShaderRegressionTest {
    @Test
    void boxBlendingTransformsTheOffsetRatherThanTheWorldPosition() throws IOException {
        String source = resource("/Common/ShaderLib/PBR.glsllib");
        assertTrue(source.contains("vec3 direction = worldPos - probePos.xyz;"));
        assertTrue(source.contains("vec3 localPos = wToLocalRot * direction;"));
        assertFalse(source.contains("localPos -= probePos.xyz;"));
        Vector3f center = new Vector3f(10, 0, 0);
        Quaternion inverse = new Quaternion().fromAngleAxis(FastMath.HALF_PI, Vector3f.UNIT_Z).inverse();
        assertEquals(Vector3f.ZERO, inverse.mult(center.subtract(center)));
        Vector3f offset = inverse.mult(center.add(0, .5f, 0).subtract(center));
        assertEquals(.5f, offset.x, 1e-6f);
        assertEquals(0f, offset.y, 1e-6f);
    }

    @Test
    void probeShaderAcceptsExplicitAndLegacyRadiusMetadata() throws IOException {
        String source = resource("/Common/ShaderLib/PBR.glsllib");
        assertTrue(source.contains("lightProbeData[0][3] == 0.0 && lightProbeData[2][3] < 0.0"));
        assertTrue(source.contains("? lightProbeData[1][3] : fract(probePos.w)"));
        assertTrue(source.contains("float nbMipMaps = floor(probePos.w);"));
    }

    @Test
    void debugOpacityIsAppliedToTheReturnedColor() throws IOException {
        String source = resource("/Common/ShaderLib/module/pbrlighting/PBRLightingUtils.glsllib");
        int start = source.indexOf("vec4 PBRLightingUtils_getColorOutputForDebugMode");
        int end = source.indexOf("return outputColorForLayer;", start);
        assertTrue(start >= 0 && end > start);
        String function = source.substring(start, end);
        assertTrue(function.contains("outputColorForLayer.a = 1.0;"));
        assertFalse(function.contains("gl_FragColor.a = 1.0;"));
    }

    @Test
    void multipleProbeBlendingUsesFloatDivisorsOnGles() throws IOException {
        String source = resource("/Common/ShaderLib/module/pbrlighting/PBRLightingUtils.glsllib");
        assertEquals(3, source.split("/ float\\(NB_PROBES - 1\\)", -1).length - 1);
        assertFalse(source.contains("/ (NB_PROBES - 1)"));
    }

    @Test
    void neutralToneMappingAppliesGammaBelowTheCompressionShoulder() throws IOException {
        String source = resource("/Common/ShaderLib/Hdr.glsllib");
        String function = source.substring(source.indexOf("vec3 HDR_KHRToneMap"));
        assertTrue(function.contains("if (peak >= startCompression)"));
        assertFalse(function.contains("if (peak < startCompression) return color"));
        assertTrue(function.contains("return pow(max(color, vec3(0.0)), gamma);"));
        assertEquals(.0256, Math.pow(.2 - .04, 2), 1e-8);
    }

    @Test
    void implicitMultisampleResolveDoesNotEnableSampler2DMSOverloads() throws IOException {
        String source = resource("/Common/ShaderLib/MultiSample.glsllib");
        int end = source.indexOf("vec4 textureFetch(in sampler2DMS");
        String guard = source.substring(source.lastIndexOf("#if ", end), end);
        assertTrue(guard.contains("defined(RESOLVE_MS)"));
        assertTrue(guard.contains("__VERSION__ >= 310"));
        assertFalse(guard.contains("GL_EXT_multisampled_render_to_texture"));
    }

    private static String resource(String name) throws IOException {
        try (InputStream input = PBRShaderRegressionTest.class.getResourceAsStream(name)) {
            assertNotNull(input, name);
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
