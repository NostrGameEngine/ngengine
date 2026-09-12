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

package org.ngengine.web.context;
import com.jme3.asset.AssetManager;
import com.jme3.input.JoyInput;
import com.jme3.input.KeyInput;
import com.jme3.input.MouseInput;
import com.jme3.input.TouchInput;
import com.jme3.material.Material;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.Renderer;
import com.jme3.renderer.opengl.GLRenderer;
import com.jme3.scene.Geometry;
import com.jme3.system.*;
import com.jme3.texture.FrameBuffer;
import com.jme3.texture.FrameBuffer.FrameBufferTarget;
import com.jme3.texture.Image;
import com.jme3.texture.Image.Format;
import com.jme3.texture.Texture2D;
import com.jme3.texture.image.ColorSpace;
import com.jme3.ui.Picture;

import org.ngengine.web.WebBinds;
import org.ngengine.web.WebBindsAsync;
import org.ngengine.web.input.WebKeyInput;
import org.ngengine.web.input.WebJoyInput;
import org.ngengine.web.input.WebMouseInput;
import org.ngengine.web.input.WebTouchInput;
import org.ngengine.web.rendering.WebGL;
import org.ngengine.web.rendering.WebGLOptions;
import org.ngengine.web.rendering.WebGLWrapper;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.teavm.jso.JSFunctor;
import org.teavm.jso.JSObject;
 

public class WebContext implements JmeContext, Runnable {
   

    protected static final Logger logger = Logger.getLogger(WebContext.class.getName());

    protected static final String THREAD_NAME = "jME3 Web Main";

    private static volatile WebContext activeContext;

    protected AtomicBoolean created = new AtomicBoolean(false);
    protected AtomicBoolean needClose = new AtomicBoolean(false);
    private final AtomicBoolean restartRequested = new AtomicBoolean(false);
    protected final Object createdLock = new Object();

    protected AppSettings settings = new AppSettings(true);
    protected Timer timer;
    protected SystemListener listener;
    protected Renderer renderer;
    protected WebJoyInput joyInput;
    protected WebCanvasElement canvasTarget;
    protected RenderManager renderManager;
    protected AssetManager assetManager;

    protected Material blitMat;
    protected Geometry blitGeom;
    
    protected FrameBuffer auxiliaryFrameBuffer;
    protected boolean auxiliaryFrameBufferRefreshed = false;
    protected boolean needAuxiliaryFrameBuffer = false;
    
    protected long timeThen;
    protected long timeLate;

    protected int canvasWidth;
    protected int canvasHeight;
    protected float canvasPixelRatio = 1f;

    @JSFunctor
    public static interface CanvasResizeHandler extends JSObject {
        void onResize(int width, int height, float pixelRatio);
    }

    @JSFunctor
    public static interface CanvasSwapHandler extends JSObject {
        void onSwap(WebCanvasElement canvas);
    }

    @Override
    public Type getType() {
        return Type.Display;
    }

    /**
     * Accesses the listener that receives events related to this context.
     *
     * @return the pre-existing instance
     */
    @Override
    public SystemListener getSystemListener() {
        return listener;
    }

    @Override
    public void setSystemListener(SystemListener listener) {
        this.listener = listener;
    }




    private void rebuildAuxiliaryFrameBufferIfNeeded(int width, int height) {
        // This attachment is sampled by the blit pass. Copied application
        // settings may request canvas antialiasing, but not multisample textures.
        int samples = 1;
        boolean srgb = settings.isGammaCorrection();
        boolean hasDepth = settings.getDepthBits() > 0;
        boolean hasStencil = settings.getStencilBits() > 0;

        // check if we already have a framebuffer with the desired properties
        if(auxiliaryFrameBuffer!=null
            &&auxiliaryFrameBuffer.getWidth()==width
            &&auxiliaryFrameBuffer.getHeight()==height
        ){
            return;
        }
        
        logger.info("Rebuild auxiliary framebuffer: "+width+"x"+height+" srgb="+srgb+" samples="+samples);
        FrameBuffer mainFrameBuffer = new FrameBuffer(width, height, samples);
        
        // color target
        // we use an f16 render target to avoid losing precision before the sRGB conversion
        Texture2D colorTex = new Texture2D(new Image(srgb?Format.RGBA16F:Format.RGBA8, width, height, null, ColorSpace.Linear));
        colorTex.setMagFilter(com.jme3.texture.Texture.MagFilter.Bilinear);
        colorTex.setMinFilter(com.jme3.texture.Texture.MinFilter.BilinearNoMipMaps);
        if(samples>1){
            colorTex.getImage().setMultiSamples(samples);
        }
        mainFrameBuffer.addColorTarget(FrameBufferTarget.newTarget(colorTex));


        // depth and stencil targets
        if (hasDepth || hasStencil) {
            if(hasStencil){
                mainFrameBuffer.setDepthTarget(FrameBufferTarget.newTarget(Format.Depth24Stencil8));
            }else{
                mainFrameBuffer.setDepthTarget(FrameBufferTarget.newTarget(Format.Depth));
            }
        }

        
        // cleanup the old framebuffer 
        if (this.auxiliaryFrameBuffer != null) 
            this.auxiliaryFrameBuffer.dispose();
        
        // set the new framebuffer
        this.auxiliaryFrameBuffer = mainFrameBuffer;
        auxiliaryFrameBufferRefreshed = true;
    }

    private void doInit() {
       
        
        timer = new NanoTimer();
        canvasTarget  = WebBindsAsync.getRenderTarget();


        WebBinds.addResizeRenderTargetListener((width,height,pixelRatio)->{
            canvasWidth = width;
            canvasHeight = height;
            canvasPixelRatio = DisplayScaleUtils.sanitizeScale(pixelRatio);
        });


        WebBinds.addSwapRenderTargetListener(c -> {
            if (canvasTarget != c) {
                canvasTarget = c;
            }
        
        });

        
        setTitle(settings.getTitle());

        boolean hasAntialias = settings.getSamples() > 1;
        settings.setSamples(1);  // WebGL doesn't support MSAA

        
        this.needAuxiliaryFrameBuffer = settings.isGammaCorrection();
        
        WebGLOptions attrs =  WebGLOptions.create();
        String colorSpace="srgb";
        attrs.setColorSpace(colorSpace);
        attrs.setDrawingColorSpace(colorSpace);
        attrs.setPowerPreference("high-performance");
        attrs.setDepth(settings.getDepthBits()>0);
        // The canvas is redrawn every frame. Keep it on the compositor's paced path
        // and avoid preserving or blending a buffer whose previous contents are unused.
        attrs.setAlpha(false);
        attrs.setDesynchronized(false);
        attrs.setPremultipliedAlpha(false);
        attrs.setPreserveDrawingBuffer(false);
        attrs.setFailIfMajorPerformanceCaveat(false);
        attrs.setStencil(settings.getStencilBits()>0);
        attrs.setAntialias(hasAntialias);

 
        WebGLWrapper ctx = (WebGLWrapper) canvasTarget.getContext("webgl2", attrs);
        if (ctx == null) {
            throw new RuntimeException("WebGL2 not supported");
        }
        ctx.pixelStorei(WebGLWrapper.UNPACK_COLORSPACE_CONVERSION_WEBGL, WebGLWrapper.NONE);

        logger.fine("Starting WebGL renderer...");


        WebGL gl = new WebGL(ctx);
        renderer = new GLRenderer(gl, gl, gl);

        renderer.initialize();

        gl.setCaps(renderer.getCaps());


        logger.fine("sRGB: "+settings.isGammaCorrection());
        // Gamma correction on WebGL is performed explicitly by WebGLBlit.frag
        // when copying the linear RGBA16F target to the browser-managed canvas.
        // WebGL has no GL_FRAMEBUFFER_SRGB write-control toggle, so requesting
        // an sRGB default framebuffer here is both redundant and unsupported.
        renderer.setMainFrameBufferSrgb(false);
        renderer.setLinearizeSrgbImages(settings.isGammaCorrection());
            
        logger.fine("WebGL renderer started!");

        listener.initialize();
        logger.fine("WebGL created!");

    }

    private void doDestroy() {
        listener.destroy();
        timer = null;
        if (activeContext == this) {
            activeContext = null;
        }
   
        logger.fine("WebGL destroyed.");
    }

    @Override
    public  void onRenderManagerReady(RenderManager rm, AssetManager assetManager) {
        this.assetManager = assetManager;
        this.renderManager = rm;
    }

    int logicalTargetW = 0;
    int logicalTargetH = 0;
    int framebufferTargetW = 0;
    int framebufferTargetH = 0;
    int internalTargetW = 0;
    int internalTargetH = 0;

    static int[] resolveCanvasSizes(float mode, int width, int height, float pixelRatio) {
        int windowWidth = Math.max(width, 1);
        int windowHeight = Math.max(height, 1);
        float density = DisplayScaleUtils.requestsHighDensityFramebuffer(mode)
                ? DisplayScaleUtils.sanitizeScale(pixelRatio)
                : 1f;
        int framebufferWidth = Math.max(Math.round(windowWidth * density), 1);
        int framebufferHeight = Math.max(Math.round(windowHeight * density), 1);
        int[] logicalSize = DisplayScaleUtils.resolveLogicalSize(mode, windowWidth, windowHeight,
                framebufferWidth, framebufferHeight, density, density);
        int renderWidth = DisplayScaleUtils.isEmulatedScaleMode(mode)
                ? Math.max(Math.round(framebufferWidth * mode), 1)
                : framebufferWidth;
        int renderHeight = DisplayScaleUtils.isEmulatedScaleMode(mode)
                ? Math.max(Math.round(framebufferHeight * mode), 1)
                : framebufferHeight;
        return new int[] {
            logicalSize[0], logicalSize[1], framebufferWidth, framebufferHeight, renderWidth, renderHeight
        };
    }

    public void reshapeIfNeeded(){
        int width = settings.getWidth();
        int height = settings.getHeight();
        if(canvasWidth>0 && settings.isResizable()){
            width = canvasWidth;
        }
        if(canvasHeight>0 && settings.isResizable()){
            height = canvasHeight;
        }

        int[] sizes = resolveCanvasSizes(settings.getDisplayScaleMode(), width, height, canvasPixelRatio);
        int logicalWidth = sizes[0];
        int logicalHeight = sizes[1];
        int framebufferWidth = sizes[2];
        int framebufferHeight = sizes[3];
        int renderWidth = sizes[4];
        int renderHeight = sizes[5];

        if(settings.getWidth()!=logicalWidth || settings.getHeight()!=logicalHeight){
            settings.setResolution(logicalWidth, logicalHeight);
        }

        if(canvasTarget!=null&&(logicalTargetW!=logicalWidth||logicalTargetH!=logicalHeight
                ||framebufferTargetW!=framebufferWidth||framebufferTargetH!=framebufferHeight
                ||internalTargetW!=renderWidth||internalTargetH!=renderHeight)){
            canvasTarget.setWidth(framebufferWidth);
            canvasTarget.setHeight(framebufferHeight);
            listener.reshape(logicalWidth, logicalHeight, renderWidth, renderHeight);
            logicalTargetW = logicalWidth;
            logicalTargetH = logicalHeight;
            framebufferTargetW = framebufferWidth;
            framebufferTargetH = framebufferHeight;
            internalTargetW = renderWidth;
            internalTargetH = renderHeight;
            needAuxiliaryFrameBuffer = settings.isGammaCorrection()
                    || renderWidth != framebufferWidth || renderHeight != framebufferHeight;
        }
    }

    private boolean loop() {
        try{
            if (restartRequested.get()) return false;
            if(needClose.get()){
                doDestroy();
                return false;
            }

            if(canvasTarget==null) return true;
     
            
            reshapeIfNeeded();

            WebBinds.toggleFullscreen(settings.isFullscreen());
     
            int w = internalTargetW;
            int h = internalTargetH;

            if(needAuxiliaryFrameBuffer && w>2 && h>2){
                rebuildAuxiliaryFrameBufferIfNeeded(w, h);
    
                // we render on a auxiliary framebuffer
                // and then we blit it to the main framebuffer applying gamma correction
                // manually

                GLRenderer gl = (GLRenderer) renderer;
                
                gl.setMainFrameBufferOverride(auxiliaryFrameBuffer);
                listener.update();
                gl.setMainFrameBufferOverride(null);

                if(blitMat==null){
                    blitMat = new Material(assetManager, "Common/MatDefs/Post/WebGLBlit.j3md");
                    blitMat.getAdditionalRenderState().setDepthTest(false);
                    blitMat.getAdditionalRenderState().setDepthWrite(false);
                    logger.info("Rebuild blit material");
                }
                blitMat.setBoolean("Srgb", settings.isGammaCorrection());

                if(blitGeom==null){
                    blitGeom = new Picture("blit surface");
                    blitGeom.setMaterial(blitMat);        
                    logger.info("Rebuild blit geometry");    
                }

                if(auxiliaryFrameBufferRefreshed){
                    blitMat.setTexture("Texture", (Texture2D) auxiliaryFrameBuffer.getColorTarget(0).getTexture());
                    if(auxiliaryFrameBuffer.getSamples()<=1){
                        blitMat.clearParam("NumSamples");    
                    } else {
                        blitMat.setInt("NumSamples", auxiliaryFrameBuffer.getSamples());
                    }
                    logger.info("Update blit material");
                    auxiliaryFrameBufferRefreshed = false;
                }
                gl.setFrameBuffer(null);
                // Binding the browser-managed default framebuffer does not
                // update GLRenderer's viewport because that framebuffer has
                // no jME FrameBuffer object carrying its dimensions. The
                // scene pass can therefore leave the viewport at a stale
                // logical size after the canvas has been resized, confining
                // the final image to the lower-left corner. WebGLBlit.vert
                // operates directly in clip space, so restoring the physical
                // drawing-buffer viewport is sufficient for this pass.
                gl.setViewPort(0, 0, framebufferTargetW, framebufferTargetH);
                blitGeom.updateGeometricState();
                renderManager.renderGeometry(blitGeom);
            } else {
                listener.update();
            }

         } catch(Throwable e){
            logger.log(Level.SEVERE, "Error in WebGL context: "+e.getMessage(), e);
            throw new RuntimeException(e);
        }       

       

      
        return true;
    }


    long pingDelta = 0;

    @Override
    public void run() {
        activeContext = this;
        doInit();  

        long lastReport = timer.getTime();
        long measuredWork = 0;
        long maximumWork = 0;
        int measuredFrames = 0;
        long previousFrameStart = -1;
        long measuredIntervals = 0;
        long maximumInterval = 0;
        int intervalCount = 0;
        boolean diagnostics = settings.getBoolean("WebFrameDiagnostics");
        timeThen = timer.getTime();
        while(true){
            long frameStart = timer.getTime();
            if(!loop())return;
            long timeNow = timer.getTime();
            if (diagnostics) {
                if (previousFrameStart >= 0) {
                    long interval = frameStart - previousFrameStart;
                    measuredIntervals += interval;
                    maximumInterval = Math.max(maximumInterval, interval);
                    intervalCount++;
                }
                previousFrameStart = frameStart;
                long work = timeNow - frameStart;
                measuredWork += work;
                maximumWork = Math.max(maximumWork, work);
                measuredFrames++;
                if (timeNow - lastReport >= timer.getResolution() * 5) {
                    logger.info("Web frame work: count=" + measuredFrames
                            + " averageMs=" + measuredWork * 1000.0 / timer.getResolution() / measuredFrames
                            + " maximumMs=" + maximumWork * 1000.0 / timer.getResolution()
                            + " intervalAverageMs=" + measuredIntervals * 1000.0 / timer.getResolution() / Math.max(1, intervalCount)
                            + " intervalMaximumMs=" + maximumInterval * 1000.0 / timer.getResolution());
                    measuredWork = maximumWork = 0;
                    measuredIntervals = maximumInterval = 0;
                    intervalCount = 0;
                    measuredFrames = 0;
                    lastReport = timeNow;
                }
            }

            pingDelta += (timeNow - timeThen);
            if(pingDelta >= timer.getResolution()){
                WebBinds.pingFrontEnd();
                pingDelta = 0;
            }

            if(settings.isVSync()){
                WebBindsAsync.waitNextFrame();
            } else {
                int delay = frameDelayMillis(frameStart, timer.getTime(), timer.getResolution(), settings.getFrameRate());
                if(delay > 0){
                    WebBindsAsync.delay(delay);
                } else {
                    Thread.yield();
                }
            }

            timeThen = timeNow;
        }             
    }

    static int frameDelayMillis(long frameStart, long now, long resolution, int fps) {
        if (fps <= 0) return 0;
        // Pace start-to-start. The previous pre-sleep timestamp produced alternating
        // short/long frames; missed deadlines must not create catch-up render bursts.
        long remaining = resolution / fps - Math.max(0, now - frameStart);
        if (remaining <= 0) return 0;
        return (int) Math.max(1, (remaining * 1000 + resolution - 1) / resolution);
    }

    // @Async
    // public static native void vsync();
    // private static void vsync( AsyncCallback<Void> callback) {
    //     vsyncAsync(result -> callback.complete(result));
    // }
 

    @Override
    public void destroy(boolean waitFor) {
        // Closing a browser application returns to its launcher instead of leaving a frozen canvas.
        restart();
    }

    /** Returns to the page launcher if a browser context is active. */
    public static void requestExit() {
        WebContext context = activeContext;
        if (context != null) {
            context.destroy(false);
        }
    }

    @Override
    public void create(boolean waitFor) {
        if (created.get()) {
            logger.warning("create() called when WebGL context is already created!");
            return;
        }
        new Thread(this, THREAD_NAME).start();

    }

    @Override
    public void restart() {
        if (restartRequested.compareAndSet(false, true)) {
            requestPageReload();
        }
    }

    /** Recreate the page, worker and GPU resources together after settings are saved. */
    protected void requestPageReload() {
        WebBinds.reloadPage();
    }

    @Override
    public void setAutoFlushFrames(boolean enabled) {
    }

    @Override
    public MouseInput getMouseInput() {
        return new WebMouseInput(getOrCreateJoyInput());
    }

    @Override
    public KeyInput getKeyInput() {
        return new WebKeyInput();
    }

    @Override
    public JoyInput getJoyInput() {
        return getOrCreateJoyInput();
    }

    @Override
    public TouchInput getTouchInput() {
        return new WebTouchInput(()->canvasTarget, settings, getOrCreateJoyInput());
    }

    private WebJoyInput getOrCreateJoyInput() {
        if (joyInput == null) {
            joyInput = new WebJoyInput(settings);
        }
        return joyInput;
    }

    @Override
    public void setTitle(String title) {
        WebBinds.setPageTitle(title);       
    }

    public void create() {
        create(false);
    }

    public void destroy() {
        destroy(false);
    }

    protected void waitFor(boolean createdVal) {
        // synchronized (createdLock) {
        //     while (created.get() != createdVal) {
        //         try {
        //             createdLock.wait();
        //         } catch (InterruptedException ex) {
        //         }
        //     }
        // }
    }

    @Override
    public boolean isCreated() {
        return created.get();
    }

    @Override
    public void setSettings(AppSettings settings) {
        this.settings.copyFrom(settings);
    }

    @Override
    public AppSettings getSettings() {
        return settings;
    }

    @Override
    public Renderer getRenderer() {
        return renderer;
    }

    @Override
    public Timer getTimer() {
        return timer;
    }

    @Override
    public boolean isRenderable() {
        return true;  
    }

    /**
     * Returns the height of the framebuffer.
     *
     * @throws UnsupportedOperationException
     */
    @Override
    public int getFramebufferHeight() {
        if(framebufferTargetH > 0) return framebufferTargetH;
        if(canvasTarget!=null) return canvasTarget.getHeight();
        return 768;
    }

    /**
     * Returns the width of the framebuffer.
     *
     * @throws UnsupportedOperationException
     */
    @Override
    public int getFramebufferWidth() {
        if(framebufferTargetW > 0) return framebufferTargetW;
        if(canvasTarget!=null) return canvasTarget.getWidth();
        return 1024;
    }

    /**
     * Returns the screen X coordinate of the left edge of the content area.
     *
     * @throws UnsupportedOperationException
     */
    @Override
    public int getWindowXPosition() {
        return 0;
    }

    /**
     * Returns the screen Y coordinate of the top edge of the content area.
     *
     * @throws UnsupportedOperationException
     */
    @Override
    public int getWindowYPosition() {
        return 0;
    }

    @Override
    public Displays getDisplays() {
     
        Displays displayList = new Displays();
        
        long monitorI = 0;
        int monPos = displayList.addNewMonitor(monitorI);
        displayList.setPrimaryDisplay(monPos);
        int width = canvasTarget ==null? 1024:canvasTarget.getWidth();
        int height = canvasTarget==null?768:canvasTarget.getHeight();
        int rate =  60; // TODO: set real rate
        displayList.setInfo(monPos, "Canvas", width, height, rate);

        return displayList;
    }

    @Override
    public int getPrimaryDisplay() {
        return 0;
    }
}
