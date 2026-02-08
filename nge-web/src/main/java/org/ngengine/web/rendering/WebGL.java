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

package org.ngengine.web.rendering;

import com.jme3.renderer.Caps;
import com.jme3.renderer.RendererException;
import com.jme3.renderer.opengl.*;
import com.jme3.texture.Image.Format;
import com.jme3.util.BufferUtils;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

import org.teavm.jso.JSObject;
import org.teavm.jso.core.JSArrayReader;
import org.teavm.jso.core.JSString;
import org.teavm.jso.typedarrays.ArrayBufferView;
import org.teavm.jso.typedarrays.Float32Array;
import org.teavm.jso.typedarrays.Int16Array;
import org.teavm.jso.typedarrays.Int32Array;
import org.teavm.jso.typedarrays.Int8Array;
import org.teavm.jso.typedarrays.Uint16Array;
import org.teavm.jso.typedarrays.Uint8Array;
import org.teavm.jso.webgl.WebGLBuffer;
import org.teavm.jso.webgl.WebGLFramebuffer;
import org.teavm.jso.webgl.WebGLProgram;
import org.teavm.jso.webgl.WebGLRenderbuffer;
import org.teavm.jso.webgl.WebGLRenderingContext;
import org.teavm.jso.webgl.WebGLShader;
import org.teavm.jso.webgl.WebGLTexture;
import org.teavm.jso.webgl.WebGLUniformLocation;

public class WebGL implements GL, GL2, GLES_30, GLExt, GLFbo {
    WebGLWrapper gl;
    IntBuffer tmpBuff = BufferUtils.createIntBuffer(1);
    IntBuffer tmpBuff16 = BufferUtils.createIntBuffer(16);

    AtomicInteger pCount = new AtomicInteger(0);
    Map<Integer,JSObject> pMap = new HashMap<>();
    WebGLLintExt debugExt;
    WebGLLintConfig debugCfg;
    Map<String,String> webgl2glextMap= new HashMap<>();
    private final Logger logger = Logger.getLogger(WebGL.class.getName());

    private void enableExtension(String webGlName, String glName) {
        if (webGlName == null) {
            webgl2glextMap.put(webGlName, glName);
        } else {
            JSObject ext = this.gl.getExtension(webGlName);
            if (ext == null) {
                System.out.println("Warn: " + webGlName + " extension is not supported in this context.");
            } else if (glName != null) {
                webgl2glextMap.put(webGlName, glName);
            }
        }
    }

    public WebGL(WebGLWrapper ctx) {
        this.gl = ctx;
        
        enableExtension("WEBGL_compressed_texture_s3tc","GL_EXT_texture_compression_s3tc");
        enableExtension("WEBGL_compressed_texture_s3tc_srgb",null);
        enableExtension("WEBGL_compressed_texture_etc","GL_ARB_ES3_compatibility");
        enableExtension("WEBGL_compressed_texture_rgtc","GL_EXT_texture_compression_rgtc");
        enableExtension("EXT_color_buffer_half_float",null);
        enableExtension("EXT_color_buffer_float",null);
        enableExtension("WEBGL_depth_texture",null);
        enableExtension("WEBGL_texture_non_power_of_two",null);

        JSObject debugExt = gl.getExtension("GMAN_debug_helper");
        if (debugExt != null) {
            this.debugExt = debugExt.cast();
            this.debugCfg = WebGLLintConfig.create();
            this.debugCfg.setFailUnsetUniforms(false);
            this.debugCfg.setMaxDrawCalls(0);
            this.debugCfg.setWarnUndefinedUniforms(false);
            this.debugCfg.setThrowOnError(false);
            this.debugExt.setConfiguration(debugCfg);
            //   this.debugExt.disable();
        }

        gl.enable(WebGLRenderingContext.DITHER);


        // enableExtension("WEBGL_compressed_texture_pvrtc");
        // enableExtension("WEBGL_compressed_texture_astc");

    }

 
    private void _tr(Float32Array in, ByteBuffer out) {
        int p = out.position();
    
    
        for (int i = 0; i < in.getLength(); i++) {
            int v = Float.floatToIntBits(in.get(i));

            byte b1 = (byte) (v & 0xff);
            byte b2 = (byte) ((v >> 8) & 0xff);
            byte b3 = (byte) ((v >> 16) & 0xff);
            byte b4 = (byte) ((v >> 24) & 0xff);
            
            out.put(p++, b1);
            out.put(p++, b2);
            out.put(p++, b3);
            out.put(p++, b4);
                    

        }
    }

    private void _tr(Uint16Array in, ByteBuffer out) {
        int p = out.position();
        
        for (int i = 0; i < in.getLength(); i++) {
            short v = (short) in.get(i);
            byte b1 = (byte) (v & 0xff);
            byte b2 = (byte) ((v >> 8) & 0xff);
            out.put(p++, b1);
            out.put(p++, b2);
            

        }
    }

    private void _tr(Uint8Array a, ByteBuffer data) {
        int p = data.position();
        for (int i = 0; i < a.getLength(); ++i) {
            data.put(p++, (byte) a.get(i));
        }
    }


    private Float32Array _a(FloatBuffer buffer) {
        if (buffer == null) return null;
        Float32Array result = Float32Array.create(buffer.remaining());
        for (int i = buffer.position(); i < buffer.limit(); ++i) {
            result.set(i, buffer.get(i));
        }

        return result;
    }

    private Int16Array _a(ShortBuffer buffer) {
        if (buffer == null) return null;
        Int16Array result = Int16Array.create(buffer.remaining());
        for (int i = buffer.position(); i < buffer.limit(); ++i) {
            result.set(i, buffer.get(i));
        }

        return result;
    }

    private Int32Array _a(IntBuffer buffer) {
        if (buffer == null) return null;
        Int32Array result = Int32Array.create(buffer.remaining());
        for (int i = buffer.position(); i < buffer.limit(); ++i) {
            result.set(i, buffer.get(i));
        }

        return result;
    }

    

    private Int8Array _a(ByteBuffer buffer) {
        if (buffer == null) return null;
        Int8Array result = Int8Array.create(buffer.remaining());
        for (int i = buffer.position(); i < buffer.limit(); ++i) {
            result.set(i, buffer.get(i));
        }

        return result;
    }

    private int[] tmpOut= new int[2];

    private int[] bppChan(int glFormat, int glType) {
        if(glFormat==-1&&glType==-1){
            tmpOut[0] = 8;
            tmpOut[1] = 1;
            return tmpOut;
        }
        int bpp = 8;
        int channels = 1;
        Format format=findClosestFormat( glFormat, glType);

        bpp = format.getBitsPerPixel();
        channels = getNumChannels(format);
    
        tmpOut[0] = bpp;
        tmpOut[1] = channels;

       

        return tmpOut;
    }
    

 

    private ArrayBufferView _aX(ByteBuffer buffer, int glFormat, int glType) {
        if (buffer == null) return null;
        ArrayBufferView out;
    
               
        int start = buffer.position();
        int end = buffer.limit();
        int length = buffer.remaining();
        
        int bppChan[]=bppChan(glFormat, glType);
        int bpp=bppChan[0];
        int channels = bppChan[1];
        int bpc=bpp/channels;
        ByteOrder bufferEndianess = buffer.order();

        if (bpc == 8) {
            Uint8Array bbf = Uint8Array.create(length);
            int j=0;
            for (int i = start; i < end; i++) {
                byte b = buffer.get(i);
                short unsigned = (short) (b & 0xff);
                bbf.set(j++, unsigned);
            }
            out = bbf;
        } else if (bpc==32){
            
            float[] b = new float[length / 4];
            int j=0;
            for (int i = start; i < end; i += 4) {

                byte b0 = buffer.get(i);
                byte b1 = buffer.get(i + 1);
                byte b2 = buffer.get(i + 2);
                byte b3 = buffer.get(i + 3);
                int i0 = (b0 & 0xff) | ((b1 & 0xff) << 8) | ((b2 & 0xff) << 16) | ((b3 & 0xff) << 24);
                float f = Float.intBitsToFloat(i0);
                b[j++] = f;

            }
            Float32Array bbf = Float32Array.create(length / 4);
            bbf.set(b);
            out = bbf;
         } else  {
            short[] b = new short[length / 2];
             int j=0;
            for (int i = start; i < end; i += 2) {
                // buffer.get(c);
                // c= correctEndianess(bufferEndianess,ByteOrder.LITTLE_ENDIAN, c);
                byte b0 = buffer.get(i);
                byte b1 = buffer.get(i + 1);
                b[j++]  = (short) ((b0 & 0xff) | ((b1 & 0xff) << 8));
            }
            Uint16Array bbf = Uint16Array.create(length / 2);
            bbf.set(b);
            out = bbf;            
        }

        

        return out;
    }

    private void _pD(JSObject obj) {
        pMap.entrySet().removeIf(entry -> entry.getValue() == obj);

    }
    
    private <T extends JSObject> T _pD(int i) {
        return (T) pMap.remove(i);
        
    }

    private <T extends JSObject> T _pG(int i) {
        return (T) pMap.get(i);
        
    }

    private int _pS(JSObject o) {
        int index = 0;
        while (true) {
            index = pCount.incrementAndGet();
            if (index!=0&&index!=-1&&!pMap.containsKey(index)) {
                break;
            }
        }
        pMap.put(index, o);
        return index;
    }

    @Override
    public void resetStats() {
    }

    private static int getLimitBytes(ByteBuffer buffer) {
        checkLimit(buffer);
        return buffer.limit();
    }

    private static int getLimitBytes(ShortBuffer buffer) {
        checkLimit(buffer);
        return buffer.limit() * 2;
    }

    private static int getLimitBytes(IntBuffer buffer) {
        checkLimit(buffer);
        return buffer.limit() * 4;
    }

    private static int getLimitBytes(FloatBuffer buffer) {
        checkLimit(buffer);
        return buffer.limit() * 4;
    }

    private static int getLimitCount(Buffer buffer, int elementSize) {
        checkLimit(buffer);
        return buffer.limit() / elementSize;
    }

    private static void checkLimit(Buffer buffer) {
        if (buffer == null) {
            return;
        }
        if (buffer.limit() == 0) {
            throw new RendererException("Attempting to upload empty buffer (limit = 0), that's an error");
        }
        if (buffer.remaining() == 0) {
            throw new RendererException("Attempting to upload empty buffer (remaining = 0), that's an error");
        }
    }

    @Override
    public void glActiveTexture(int texture) {
        gl.activeTexture(texture);

    }

    @Override
    public void glAttachShader(int program, int shader) {
        WebGLProgram p = _pG(program);
        WebGLShader s = _pG(shader);
        gl.attachShader(p, s);
    }

    @Override
    public void glBeginQuery(int target, int query) {
        WebGLQuery q = _pG(query);
        gl.beginQuery(target, q);
    }

    @Override
    public void glBindBuffer(int target, int buffer) {
        WebGLBuffer b = _pG(buffer);
        gl.bindBuffer(target, b);
    }

    @Override
    public void glBindTexture(int target, int texture) {
        WebGLTexture t = _pG(texture);
        gl.bindTexture(target, t);
    }

    @Override
    public void glBlendFunc(int sfactor, int dfactor) {
        gl.blendFunc(sfactor, dfactor);
    }

    @Override
    public void glBlendFuncSeparate(int sfactorRGB, int dfactorRGB, int sfactorAlpha, int dfactorAlpha) {
        gl.blendFuncSeparate(sfactorRGB, dfactorRGB, sfactorAlpha, dfactorAlpha);
    }

    @Override
    public void glBufferData(int target, FloatBuffer data, int usage) {
        gl.bufferData(target, _a(data), usage);
    }

    @Override
    public void glBufferData(int target, ShortBuffer data, int usage) {
        gl.bufferData(target, _a(data), usage);
    }

    @Override
    public void glBufferData(int target, ByteBuffer data, int usage) {
        gl.bufferData(target, _a(data), usage);
    }

    @Override
    public void glBufferData(int target, long dataSize, int usage) {
        gl.bufferData(target, (int) dataSize, usage);
    }

    @Override
    public void glBufferSubData(int target, long offset, FloatBuffer data) {
        gl.bufferSubData(target, (int) offset, _a(data));
    }

    @Override
    public void glBufferSubData(int target, long offset, ShortBuffer data) {
        gl.bufferSubData(target, (int) offset, _a(data));
    }

    @Override
    public void glBufferSubData(int target, long offset, ByteBuffer data) {
        gl.bufferSubData(target, (int) offset, _a(data));
    }

    @Override
    public void glGetBufferSubData(int target, long offset, ByteBuffer data) {
        Uint8Array a = Uint8Array.create(data.remaining());
        gl.getBufferSubData(target,(int) offset, a);
        _tr(a, data);
    }

    @Override
    public void glClear(int mask) {
        gl.clear(mask);
    }

    @Override
    public void glClearColor(float red, float green, float blue, float alpha) {
        gl.clearColor(red, green, blue, alpha);
    }

    @Override
    public void glColorMask(boolean red, boolean green, boolean blue, boolean alpha) {
        gl.colorMask(red, green, blue, alpha);
    }

    @Override
    public void glCompileShader(int shader) {
        WebGLShader s = _pG(shader);
        gl.compileShader(s);
    }

    @Override
    public void glCompressedTexImage2D(int target, int level, int internalformat, int width, int height, int border, ByteBuffer data) {
        gl.compressedTexImage2D(target, level, internalformat, width, height, 0, _aX(data,-1,-1));
    }

    @Override
    public void glCompressedTexSubImage2D(int target, int level, int xoffset, int yoffset, int width, int height, int format, ByteBuffer data) {
        gl.compressedTexSubImage2D(target, level, xoffset, yoffset, width, height, format, _aX(data,-1,-1));
    }

    @Override
    public int glCreateProgram() {
        WebGLProgram p = gl.createProgram();
        return _pS(p);
    }

    @Override
    public int glCreateShader(int shaderType) {
        WebGLShader s = gl.createShader(shaderType);
        return _pS(s);
    }

    @Override
    public void glCullFace(int mode) {
        gl.cullFace(mode);
    }

    @Override
    public void glDeleteBuffers(IntBuffer buffers) {
        checkLimit(buffers);
        for (int i = 0; i < buffers.limit(); i++) {
            WebGLBuffer b = _pG(buffers.get(i));
            gl.deleteBuffer(b);
            _pD(b);
        }
    }

    @Override
    public void glDeleteProgram(int program) {
        WebGLProgram p = _pG(program);
        gl.deleteProgram(p);
        _pD(p);
    }

    @Override
    public void glDeleteShader(int shader) {
        WebGLShader s = _pG(shader);
        gl.deleteShader(s);
        _pD(s);

    }

    @Override
    public void glDeleteTextures(IntBuffer textures) {
        checkLimit(textures);
        for (int i = 0; i < textures.limit(); i++) {
            WebGLTexture t = _pG(textures.get(i));
            gl.deleteTexture(t);
            _pD(t);
        }
    }

    @Override
    public void glDepthFunc(int func) {
        gl.depthFunc(func);
    }

    @Override
    public void glDepthMask(boolean flag) {
        gl.depthMask(flag);
    }

    @Override
    public void glDepthRange(double nearVal, double farVal) {
        gl.depthRange((float) nearVal, (float) farVal);
    }

    @Override
    public void glDetachShader(int program, int shader) {
        WebGLProgram p = _pG(program);
        WebGLShader s = _pG(shader);
        gl.detachShader(p, s);
    }

    @Override
    public void glDisable(int cap) {
        gl.disable(cap);
    }

    @Override
    public void glDisableVertexAttribArray(int index) {
        gl.disableVertexAttribArray(index);
    }

    @Override
    public void glDrawArrays(int mode, int first, int count) {
        gl.drawArrays(mode, first, count);
    }

    @Override
    public void glDrawRangeElements(int mode, int start, int end, int count, int type, long indices) {
        gl.drawElements(mode, count, type, (int) indices);
    }

    @Override
    public void glEnable(int cap) {
        gl.enable(cap);
    }

    @Override
    public void glEnableVertexAttribArray(int index) {
        gl.enableVertexAttribArray(index);
    }

    @Override
    public void glEndQuery(int target) {
        gl.endQuery(target);
    }

    @Override
    public void glGenBuffers(IntBuffer buffers) {
        checkLimit(buffers);
        for (int i = 0; i < buffers.limit(); i++) {
            WebGLBuffer b = gl.createBuffer();
            buffers.put(i, _pS(b));
        }
    }

    @Override
    public void glGenTextures(IntBuffer textures) {
        checkLimit(textures);
        for (int i = 0; i < textures.limit(); i++) {
            WebGLTexture t = gl.createTexture();
            textures.put(i, _pS(t));
        }

    }

    @Override
    public void glGenQueries(int num, IntBuffer buff) {
        checkLimit(buff);
        for (int i = 0; i < buff.limit(); i++) {
            WebGLQuery q = gl.createQuery();
            buff.put(i, _pS(q));
        }
    }

    @Override
    public int glGetAttribLocation(int program, String name) {
        return gl.getAttribLocation(_pG(program), name);
    }

    @Override
    public void glGetBoolean(int pname, ByteBuffer params) {
        boolean v = gl.getParameterb(pname);
        params.put(0, v ? (byte) 1 : (byte) 0);
    }

    @Override
    public int glGetError() {
        return gl.getError();
    }

    @Override
    public void glGetFloat(int parameterId, FloatBuffer storeValues) {
        checkLimit(storeValues);
        float v = gl.getParameterf(parameterId);
        storeValues.put(0, v);
    }

    @Override
    public void glGetInteger(int pname, IntBuffer params) {
        checkLimit(params);
        int v = gl.getParameteri(pname);
        params.put(0, v);
    }

    @Override
    public void glGetProgram(int program, int pname, IntBuffer params) {
        checkLimit(params);
        WebGLProgram p = _pG(program);
        int v = gl.getProgramParameteri(p, pname);
        params.put(0, v);
    }

    @Override
    public String glGetProgramInfoLog(int program, int maxLength) {
        WebGLProgram p = _pG(program);
        return gl.getProgramInfoLog(p);
    }

    @Override
    public long glGetQueryObjectui64(int query, int pname) {

        WebGLQuery q = _pG(query);
        long v = gl.getQueryParameteri64(q, pname);
        return v;

    }

    @Override
    public int glGetQueryObjectiv(int query, int pname) {
        WebGLQuery q = _pG(query);

        int v = gl.getQueryParameteri(q, pname);
        return v;
    }

    @Override
    public void glGetShader(int shader, int pname, IntBuffer params) {
        checkLimit(params);
        WebGLShader s = _pG(shader);
        if (pname == GL_INFO_LOG_LENGTH) {
            String infoLog = gl.getShaderInfoLog(s);
            params.put(0, infoLog.length());
            return;
        }
        int v = gl.getShaderParameteri(s, pname);
        params.put(0, v);
    }

    @Override
    public String glGetShaderInfoLog(int shader, int maxLength) {
        WebGLShader s = _pG(shader);
        return gl.getShaderInfoLog(s);
    }

 

    @Override
    public String glGetString(int name) {
        if (name == GL_EXTENSIONS) {
            StringBuilder sb = new StringBuilder();
            JSArrayReader<JSString> exts = gl.getSupportedExtensions();
            for (int i = 0; i < exts.getLength(); i++) {
                String ext = exts.get(i).stringValue();
                sb.append(ext);
                sb.append(" ");
                String ext2 = webgl2glextMap.get(ext);
                if (ext2 != null) {
                    sb.append(ext2);
                    sb.append(" ");
                }
            }
            return sb.toString().trim();

        }
        return gl.getParameterString(name);
    }

    @Override
    public int glGetUniformLocation(int program, String name) {
        WebGLProgram p = _pG(program);
        WebGLUniformLocation loc = gl.getUniformLocation(p, name);
        return _pS(loc);
    }

    @Override
    public boolean glIsEnabled(int cap) {
        return gl.isEnabled(cap);
    }

    @Override
    public void glLineWidth(float width) {
        gl.lineWidth(width);
    }

    @Override
    public void glLinkProgram(int program) {
        WebGLProgram p = _pG(program);
        gl.linkProgram(p);
    }

    @Override
    public void glPixelStorei(int pname, int param) {
        gl.pixelStorei(pname, param);
    }

    @Override
    public void glPolygonOffset(float factor, float units) {
        gl.polygonOffset(factor, units);
    }

    @Override
    public void glReadPixels(int x, int y, int width, int height, int format, int type, ByteBuffer data) {
        
        int bppChan[] = bppChan( format, type);
        int bpp=bppChan[0];
        int channels = bppChan[1];
        int bpc = bpp / channels;
        ArrayBufferView a;
        if (bpc == 8) a = Uint8Array.create(data.remaining());
        else if (bpc == 16) a = Uint16Array.create(data.remaining() / 2);
        else if (bpc == 32) a = Float32Array.create(data.remaining() / 4);
        else throw new UnsupportedOperationException("Unsupported format "+bpc);
        gl.readPixels(x, y, width, height, format, type, a);
        if (bpc == 8) _tr((Uint8Array)a, data);
        else if (bpc == 16) _tr((Uint16Array) a, data);
        else if (bpc == 32) _tr((Float32Array) a, data);
        

    }

  
    @Override
    public void glScissor(int x, int y, int width, int height) {
        gl.scissor(x, y, width, height);
    }

    @Override
    public void glShaderSource(int shader, String[] string, IntBuffer length) {
        if (string.length != 1) {
            throw new UnsupportedOperationException("Today is not a good day");
        }
        WebGLShader s = _pG(shader);
        gl.shaderSource(s, string[0]);
    }

    @Override
    public void glStencilFuncSeparate(int face, int func, int ref, int mask) {
        gl.stencilFuncSeparate(face, func, ref, mask);
    }

    @Override
    public void glStencilOpSeparate(int face, int sfail, int dpfail, int dppass) {
        gl.stencilOpSeparate(face, sfail, dpfail, dppass);
    }

    @Override
    public void glTexImage2D(int target, int level, int internalFormat, int width, int height, int border, int format, int type, ByteBuffer data) {
        gl.texImage2D(target, level, internalFormat, width, height, 0, format, type, _aX(data,format,type));
    }

    @Override
    public void glTexParameterf(int target, int pname, float param) {
        gl.texParameterf(target, pname, param);
    }

    @Override
    public void glTexParameteri(int target, int pname, int param) {
        gl.texParameteri(target, pname, param);
    }

    @Override
    public void glTexSubImage2D(int target, int level, int xoffset, int yoffset, int width, int height, int format, int type, ByteBuffer data) {
        gl.texSubImage2D(target, level, xoffset, yoffset, width, height, format, type, _aX(data,format,type));
    }

    @Override
    public void glUniform1(int location, FloatBuffer value) {
        WebGLUniformLocation loc = _pG(location);
        gl.uniform1fv(loc, _a(value));

    }

    @Override
    public void glUniform1(int location, IntBuffer value) {
        WebGLUniformLocation loc = _pG(location);
        gl.uniform1iv(loc, _a(value));

    }

    @Override
    public void glUniform1f(int location, float v0) {
        WebGLUniformLocation loc = _pG(location);
        gl.uniform1f(loc, v0);
    }

    @Override
    public void glUniform1i(int location, int v0) {
        WebGLUniformLocation loc = _pG(location);
        gl.uniform1i(loc, v0);
    }

    @Override
    public void glUniform2(int location, IntBuffer value) {
        gl.uniform2iv(_pG(location), _a(value));
    }

    @Override
    public void glUniform2(int location, FloatBuffer value) {
        gl.uniform2fv(_pG(location), _a(value));
    }

    @Override
    public void glUniform2f(int location, float v0, float v1) {
        gl.uniform2f(_pG(location), v0, v1);
    }

    @Override
    public void glUniform3(int location, IntBuffer value) {
        gl.uniform3iv(_pG(location), _a(value));
    }

    @Override
    public void glUniform3(int location, FloatBuffer value) {
        gl.uniform3fv(_pG(location), _a(value));
    }

    @Override
    public void glUniform3f(int location, float v0, float v1, float v2) {
        gl.uniform3f(_pG(location), v0, v1, v2);
    }

    @Override
    public void glUniform4(int location, FloatBuffer value) {
        gl.uniform4fv(_pG(location), _a(value));
    }

    @Override
    public void glUniform4(int location, IntBuffer value) {
        gl.uniform4iv(_pG(location), _a(value));
    }

    @Override
    public void glUniform4f(int location, float v0, float v1, float v2, float v3) {
        gl.uniform4f(_pG(location), v0, v1, v2, v3);
    }

    @Override
    public void glUniformMatrix3(int location, boolean transpose, FloatBuffer value) {
        gl.uniformMatrix3fv(_pG(location), transpose, _a(value));
    }

    @Override
    public void glUniformMatrix4(int location, boolean transpose, FloatBuffer value) {
        gl.uniformMatrix4fv(_pG(location), transpose, _a(value));
    }

    @Override
    public void glUseProgram(int program) {
        WebGLProgram p = _pG(program);
        gl.useProgram(p);
    }

    @Override
    public void glVertexAttribPointer(int index, int size, int type, boolean normalized, int stride, long pointer) {
        gl.vertexAttribPointer(index, size, type, normalized, stride, (int) pointer);
    }

    @Override
    public void glViewport(int x, int y, int width, int height) {
        gl.viewport(x, y, width, height);
    }

    @Override
    public void glBlitFramebufferEXT(int srcX0, int srcY0, int srcX1, int srcY1, int dstX0, int dstY0, int dstX1, int dstY1, int mask, int filter) {
        gl.blitFramebuffer(srcX0, srcY0, srcX1, srcY1, dstX0, dstY0, dstX1, dstY1, mask, filter);

    }

    @Override
    public void glBufferData(int target, IntBuffer data, int usage) {
        gl.bufferData(target, _a(data), usage);
    }

    @Override
    public void glBufferSubData(int target, long offset, IntBuffer data) {
        gl.bufferSubData(target, (int) offset, _a(data));
    }

    @Override
    public void glDrawArraysInstancedARB(int mode, int first, int count, int primcount) {
        gl.drawArraysInstanced(mode, first, count, primcount);
    }

    @Override
    public void glDrawBuffers(IntBuffer bufs) {
        int bf[] = new int[bufs.remaining()];
        bufs.get(bf);
        gl.drawBuffers(bf);
    }

    @Override
    public void glDrawElementsInstancedARB(int mode, int indicesCount, int type, long indicesBufferOffset, int primcount) {
        gl.drawElementsInstanced(mode, indicesCount, type, (int) indicesBufferOffset, primcount);
    }

    @Override
    public void glGetMultisample(int pname, int index, FloatBuffer val) {
        throw new UnsupportedOperationException("glGetMultisample not available on WebGL");

    }

    @Override
    public void glRenderbufferStorageMultisampleEXT(int target, int samples, int internalformat, int width, int height) {
        gl.renderbufferStorageMultisample(target, samples, internalformat, width, height);

    }

    @Override
    public void glTexImage2DMultisample(int target, int samples, int internalformat, int width, int height, boolean fixedSampleLocations) {
        throw new UnsupportedOperationException("Multisample textures not available on WebGL");

    }

    @Override
    public void glVertexAttribDivisorARB(int index, int divisor) {
        gl.vertexAttribDivisor(index, divisor);
    }

    @Override
    public void glBindFramebufferEXT(int param1, int param2) {
        WebGLFramebuffer fb = _pG(param2);
        gl.bindFramebuffer(param1, fb);
    }

    @Override
    public void glBindRenderbufferEXT(int param1, int param2) {
        WebGLRenderbuffer rb = _pG(param2);
        gl.bindRenderbuffer(param1, rb);
    }

    @Override
    public int glCheckFramebufferStatusEXT(int param1) {
        return gl.checkFramebufferStatus(param1);

    }

    @Override
    public void glDeleteFramebuffersEXT(IntBuffer param1) {
        checkLimit(param1);
        for (int i = param1.position(); i < param1.limit(); i++) {
            WebGLFramebuffer fb = _pG(param1.get(i));
            gl.deleteFramebuffer(fb);
            _pD(fb);

        }
    }

    @Override
    public void glDeleteRenderbuffersEXT(IntBuffer param1) {
        checkLimit(param1);
        for (int i = param1.position(); i < param1.limit(); i++) {
            WebGLRenderbuffer rb = _pG(param1.get(i));
            gl.deleteRenderbuffer(rb);
            _pD(rb);
        }
    }

    @Override
    public void glFramebufferRenderbufferEXT(int param1, int param2, int param3, int param4) {
        WebGLRenderbuffer rb = _pG(param4);
        gl.framebufferRenderbuffer(param1, param2, param3, rb);
    }

    @Override
    public void glFramebufferTexture2DEXT(int target, int attachment, int type, int textureId, int level) {
        WebGLTexture tx = _pG(textureId);
        gl.framebufferTexture2D(target, attachment, type, tx, level);
    }

    @Override
    public void glGenFramebuffersEXT(IntBuffer param1) {
        checkLimit(param1);
        for (int i = 0; i < param1.limit(); i++) {
            param1.put(i, _pS(gl.createFramebuffer()));
        }

    }

    @Override
    public void glGenRenderbuffersEXT(IntBuffer param1) {
        checkLimit(param1);
        for (int i = 0; i < param1.limit(); i++) {
            param1.put(i, _pS(gl.createRenderbuffer()));
        }
    }

    @Override
    public void glGenerateMipmapEXT(int param1) {       
        gl.generateMipmap(param1);

    }

    @Override
    public void glRenderbufferStorageEXT(int param1, int param2, int param3, int param4) {
        gl.renderbufferStorage(param1, param2, param3, param4);
    }

    @Override
    public void glReadPixels(int x, int y, int width, int height, int format, int type, long offset) {
        // TODO: no offset???
        gl.readPixels(x, y, width, height, format, type, null);
    }

    @Override
    public int glClientWaitSync(Object sync, int flags, long timeout) {
        return gl.waitSync(sync, flags, (int)timeout);
    }

    @Override
    public void glDeleteSync(Object sync) {
        gl.deleteSync(sync);
    }

    @Override
    public Object glFenceSync(int condition, int flags) {
        return gl.fenceSync(condition, flags);
    }

    @Override
    public void glBlendEquationSeparate(int colorMode, int alphaMode) {
        gl.blendEquationSeparate(colorMode, alphaMode);
    }

    @Override
    public void glFramebufferTextureLayerEXT(int target, int attachment, int texture, int level, int layer) {
        WebGLTexture tx = _pG(texture);
        gl.framebufferTextureLayer(target, attachment, tx, level, layer);

    }

    @Override
    public void glAlphaFunc(int func, float ref) {

    }

    @Override
    public void glPointSize(float size) {
    }

    @Override
    public void glPolygonMode(int face, int mode) {

    }

    // Wrapper to DrawBuffers as there's no DrawBuffer method in GLES
    @Override
    public void glDrawBuffer(int mode) {
        int nBuffers = (mode - GLFbo.GL_COLOR_ATTACHMENT0_EXT) + 1;
        if (nBuffers <= 0 || nBuffers > 16) {
            throw new IllegalArgumentException("Draw buffer outside range: " + Integer.toHexString(mode));
        }
        tmpBuff16.clear();
        for (int i = 0; i < nBuffers - 1; i++) {
            tmpBuff16.put(GL.GL_NONE);
        }
        tmpBuff16.put(mode);
        tmpBuff16.flip();
        glDrawBuffers(tmpBuff16);
    }

    @Override
    public void glReadBuffer(int mode) {
        gl.readBuffer(mode);
    }

    @Override
    public void glCompressedTexImage3D(int target, int level, int internalFormat, int width, int height, int depth, int border, ByteBuffer data) {

        gl.compressedTexImage3D(target, level, internalFormat, width, height, depth, border, _aX(data,-1,-1));

    }

    @Override
    public void glCompressedTexSubImage3D(int target, int level, int xoffset, int yoffset, int zoffset, int width, int height, int depth, int format, ByteBuffer data) {
        gl.compressedTexSubImage3D(target, level, xoffset, yoffset, zoffset, width, height, depth, format, _aX(data,-1,-1));
    }

    @Override
    public void glTexImage3D(int target, int level, int internalFormat, int width, int height, int depth, int border, int format, int type, ByteBuffer data) {

        gl.texImage3D(target, level, internalFormat, width, height, depth, border, format, type, _aX(data,format,type));
    }

    @Override
    public void glTexSubImage3D(int target, int level, int xoffset, int yoffset, int zoffset, int width, int height, int depth, int format, int type, ByteBuffer data) {
        gl.texSubImage3D(target, level, xoffset, yoffset, zoffset, width, height, depth, format, type, _aX(data,format,type));
    }


    EnumSet<Caps> caps;
    GLImageFormat formats[][]=null;

    public void setCaps(EnumSet<Caps> caps) {
        this.caps = caps;
        loadTxFormats();
    }
    
    private void loadTxFormats() {
        this.formats = GLImageFormats.getFormatsForCaps(caps);

    }

    private Format findClosestFormat(int format, int type) {
        if (this.formats == null) throw new IllegalStateException("Caps or Formats not set");

        // if (format != -1) {
        //     for (int i = 0; i < formats.length; i++) {
        //         for (int j = 0; j < formats[i].length; j++) {
        //             if (formats[i][j] == null) continue;
        //             Format cf = Format.values()[j];
        //             if (

        //             ) {
        //                 return cf;
        //             }
        //         }
        //     }
        // }

        // if (type != -1) {
        for (int i = 0; i < formats.length; i++) {
            for (int j = 0; j < formats[i].length; j++) {
                if (formats[i][j] == null) continue;
                Format cf = Format.values()[j];
                if (formats[i][j].dataType == type && formats[i][j].format == format

                ) {
                    return cf;
                }
            }
        }
        // }
        logger.warning("No format found for " + Integer.toHexString(format) + " " + Integer.toHexString(type));
        return null;
    }


    private int getNumChannels(Format format) {
        switch (format) {
            case Alpha8:
            case Luminance8:
            case Luminance16F:
            case Luminance32F:
            case Luminance8Alpha8:
            case Luminance16FAlpha16F:
            case R8I:
            case R8UI:
            case R16I:
            case R16UI:
            case R32I:
            case R32UI:
            case Depth:
            case Depth16:
            case Depth24:
            case Depth32:
            case Depth32F:
            case Depth24Stencil8:
            case R16F:
            case R32F:
                // pretend compressed are 1 channel / avoid messing with
                // endianess
            case SIGNED_RGTC1:
            case ETC1:
            case DXT1:
            case DXT1A:
            case DXT3:
            case DXT5:
            case RGTC2:
            case SIGNED_RGTC2:
            case RGTC1:
            case BC7_UNORM:
            case BC7_UNORM_SRGB:
            case BC6H_SF16:
            case BC6H_UF16:
                return 1;
            case BGR8:
            case RGB8:
            case RGB565:
            case RGB16F_to_RGB111110F:
            case RGB9E5:
            case RGB16F:
            case RGB32F:
            case RGB8I:
            case RGB8UI:
            case RGB16I:
            case RGB16UI:
            case RGB32I:
            case RGB32UI:           
                return 3;
            case RGB5A1:

            case RG16F:
            case RG32F:
                return 2;
            case RGBA8:
            case ABGR8:
            case ARGB8:
            case BGRA8:
            case RGBA16F:
            case RGBA32F:
            case RGB10A2:
            case RGBA8I:
            case RGBA8UI:
            case RGBA16I:
            case RGBA16UI:
            case RGBA32I:
            case RGBA32UI:
            
                return 4;
            default:
                return 1; // Unknown format
        }

    }
    
    public void glBindVertexArray(int array) {
        WebGLVertexArrayObject vao = _pG(array);
        gl.bindVertexArray(vao);
    }

    public void glDeleteVertexArrays(IntBuffer arrays) {
        for (int i = 0; i < arrays.limit(); i++) {
            int j=arrays.get(i);
            WebGLVertexArrayObject vao = _pG(j);
            gl.deleteVertexArray(vao);
            _pD(j);
        }
    }

   
    public void glGenVertexArrays(IntBuffer arrays) {
        for (int i = 0; i < arrays.limit(); i++) {
            WebGLVertexArrayObject vao = gl.createVertexArray();
            arrays.put(i, _pS(vao));
        }
    }
}
