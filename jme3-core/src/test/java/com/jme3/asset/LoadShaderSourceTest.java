/*
 * Copyright (c) 2009-2015 jMonkeyEngine
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
package com.jme3.asset;

import com.jme3.asset.plugins.ClasspathLocator;
import com.jme3.shader.plugins.GLSLLoader;
import com.jme3.system.JmeSystem;
import com.jme3.system.MockJmeSystemDelegate;
import org.junit.jupiter.api.Test;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import static org.junit.jupiter.api.Assertions.*;

public class LoadShaderSourceTest {

    @Test
    public void conditionalRootExtensionsKeepNestedGuardsAndBranches() throws Exception {
        String input = "#define DEPTH 1\n#if defined(GL_ES)\n#ifdef DEPTH\n#extension GL_EXT_frag_depth : require\n"
                + "#elif defined(COLOR)\n#extension GL_EXT_color_buffer_float : enable\n"
                + "#else\n#extension GL_EXT_other : enable\n#endif\n#endif\n"
                + "void main() {}\n#extension GL_ARB_texture_multisample : enable\n";
        AssetInfo info = new AssetInfo(new DesktopAssetManager(), new AssetKey<String>("guarded.frag")) {
            @Override public InputStream openStream() {
                return new ByteArrayInputStream(input.getBytes(StandardCharsets.UTF_8));
            }
        };
        String source = (String) new GLSLLoader().load(info);
        assertTrue(source.startsWith("#extension GL_ARB_texture_multisample : enable\n"));
        assertTrue(source.contains(input.substring(0, input.indexOf("void main()"))));
        assertTrue(source.indexOf("#extension GL_ARB_texture_multisample") < source.indexOf("void main()"));
    }

    @Test
    public void disabledRootExtensionsStayDisabled() throws Exception {
        AssetInfo info = new AssetInfo(null, new AssetKey<String>("disabled.frag")) {
            @Override public InputStream openStream() {
                return new ByteArrayInputStream(("#if 0\n#extension GL_TEST_unavailable : require\n"
                        + "#endif\nvoid main() {}\n").getBytes(StandardCharsets.UTF_8));
            }
        };
        String source = (String) new GLSLLoader().load(info);
        assertTrue(source.startsWith("#if 0\n#extension GL_TEST_unavailable : require\n#endif\n"));
    }

    @Test
    public void importedExtensionsKeepAvailabilityGuardsBeforeShaderCode() throws Exception {
        JmeSystem.setSystemDelegate(new MockJmeSystemDelegate());
        AssetManager manager = new DesktopAssetManager();
        manager.registerLocator(null, ClasspathLocator.class);
        manager.registerLoader(GLSLLoader.class, "glsllib");
        AssetInfo info = new AssetInfo(manager, new AssetKey<String>("imports.frag")) {
            @Override public InputStream openStream() {
                return new ByteArrayInputStream(("#import \"Common/ShaderLib/Ubo.glsllib\"\n"
                        + "#import \"Common/ShaderLib/Shadows.glsllib\"\nvoid main() {}\n").getBytes(StandardCharsets.UTF_8));
            }
        };
        String source = (String) new GLSLLoader().load(info);
        assertTrue(source.contains("#ifdef ENABLE_UBO\n    #extension GL_ARB_uniform_buffer_object : enable\n#endif\n"));
        assertTrue(source.contains("#if __VERSION__ >= 130\n    #ifdef GL_ES\n        #extension GL_OES_gpu_shader5 : enable\n#endif\n#endif\n"));
        assertTrue(source.contains("#if __VERSION__ >= 130\n    #ifdef GL_ES\n    #else\n        #extension GL_ARB_gpu_shader5 : enable\n#endif\n#endif\n"));
        assertTrue(source.indexOf("#extension GL_ARB_gpu_shader5") < source.indexOf("#define IVEC2"));
    }

    @Test
    public void conditionalRootExtensionsAfterAnImportPrecedeImportedDeclarations() throws Exception {
        JmeSystem.setSystemDelegate(new MockJmeSystemDelegate());
        AssetManager manager = new DesktopAssetManager();
        manager.registerLocator(null, ClasspathLocator.class);
        manager.registerLoader(GLSLLoader.class, "glsllib");
        AssetInfo info = new AssetInfo(manager, new AssetKey<String>("tiled.frag")) {
            @Override public InputStream openStream() {
                return new ByteArrayInputStream(("#import \"Common/ShaderLib/GLSLCompat.glsllib\"\n"
                        + "#if defined(HAS_COLOR_ARRAY)\n#extension GL_EXT_texture_array : enable\n"
                        + "#endif\nvoid main() {}\n").getBytes(StandardCharsets.UTF_8));
            }
        };
        String source = (String) new GLSLLoader().load(info);
        assertTrue(source.startsWith("#if defined(HAS_COLOR_ARRAY)\n"
                + "#extension GL_EXT_texture_array : enable\n#endif\n"));
        assertTrue(source.indexOf("#extension GL_EXT_texture_array") < source.indexOf("// -- begin import"));
    }

    @Test
    public void testLoadShaderSource() {
        JmeSystem.setSystemDelegate(new MockJmeSystemDelegate());
        AssetManager assetManager = new DesktopAssetManager();
        assetManager.registerLocator(null, ClasspathLocator.class);
        assetManager.registerLoader(GLSLLoader.class, "frag");
        assetManager.registerLoader(GLSLLoader.class, "glsllib");
        assetManager.registerLoader(GLSLLoader.class, "glsl");
        String showNormals = (String) assetManager.loadAsset("Common/MatDefs/Misc/ShowNormals.frag");
        System.out.println(showNormals);
    }
    
}
