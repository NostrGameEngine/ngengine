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

package org.ngengine.web.context;
import com.jme3.asset.AssetManager;
import com.jme3.input.JoyInput;
import com.jme3.input.KeyInput;
import com.jme3.input.MouseInput;
import com.jme3.input.TouchInput;
import com.jme3.material.Material;
import com.jme3.opencl.Context;
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

    protected AtomicBoolean created = new AtomicBoolean(false);
    protected AtomicBoolean needClose = new AtomicBoolean(false);
    protected final Object createdLock = new Object();

    protected AppSettings settings = new AppSettings(true);
    protected Timer timer;
    protected SystemListener listener;
    protected Renderer renderer;
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

    @JSFunctor
    public static interface CanvasResizeHandler extends JSObject {
        void onResize(int width, int height);
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
        if(!settings.isResizable()){
            width = settings.getWidth();
            height = settings.getHeight();
        }

        int samples = settings.getSamples() <= 0?1:settings.getSamples();
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


        WebBinds.addResizeRenderTargetListener((width,height)->{
            canvasWidth = width;
            canvasHeight = height;
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
        attrs.setAlpha(true);
        attrs.setDesynchronized(true);
        attrs.setPremultipliedAlpha(false);
        attrs.setPreserveDrawingBuffer(true);
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
        renderer.setMainFrameBufferSrgb(settings.isGammaCorrection()); 
        renderer.setLinearizeSrgbImages(settings.isGammaCorrection());
            
        logger.fine("WebGL renderer started!");

        listener.initialize();
        logger.fine("WebGL created!");

    }

    private void doDestroy() {
          listener.destroy();
        timer = null;
   
        logger.fine("WebGL destroyed.");
    }

    @Override
    public  void onRenderManagerReady(RenderManager rm, AssetManager assetManager) {
        this.assetManager = assetManager;
        this.renderManager = rm;
    }

    int internalTargetW = 0;
    int internalTargetH = 0;

    public void reshapeIfNeeded(){
        int w = settings.getWidth();
        int h = settings.getHeight();
        if(canvasWidth>0 && settings.isResizable()){
            w = canvasWidth;
        }
        if(canvasHeight>0 && settings.isResizable()){
            h = canvasHeight;
        }

        if(settings.getWidth()!=w || settings.getHeight()!=h){
            settings.setResolution(w, h);        
        }

        if(canvasTarget!=null&&(internalTargetW!=w||internalTargetH!=h)){
            canvasTarget.setWidth(w);
            canvasTarget.setHeight(h);
            listener.reshape(w, h);
            internalTargetW = w;
            internalTargetH = h;
        }
    }

    private boolean loop() {
        try{
            if(needClose.get()){
                doDestroy();
                return false;
            }

            if(canvasTarget==null) return true;
     
            
            reshapeIfNeeded();

            WebBinds.toggleFullscreen(settings.isFullscreen());
     
            Thread.yield();


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
                Thread.yield();
   

                gl.setFrameBuffer(null);
                blitGeom.updateGeometricState();
                renderManager.renderGeometry(blitGeom);
                
                Thread.yield();
            } else {
                listener.update();
            }
            Thread.yield();

         } catch(Throwable e){
            logger.log(Level.SEVERE, "Error in WebGL context: "+e.getMessage(), e);
            throw new RuntimeException(e);
        }       

       

      
        return true;
    }


    long pingDelta = 0;

    @Override
    public void run() {            
        doInit();  

        Object lock = new Object();
        AtomicBoolean slept = new AtomicBoolean(false);
        while(true){
            if(!loop())return;
            long timeNow = timer.getTime();

            pingDelta += (timeNow - timeThen);
            if(pingDelta>1000){
                WebBinds.pingFrontEnd();
                pingDelta = 0;
            }

            slept.set(false);
            if(settings.isVSync()){
                WebBinds.waitNextFrame(n->{
                    new Thread(()->{
                        synchronized(lock){
                            slept.set(true);
                            lock.notifyAll();
                        }
                    }).start();
                });
            } else {
                int fps = settings.getFrameRate();
                long gapTo = timer.getResolution() / fps + timeThen;
                long sleepTime = gapTo - timeNow - timeLate;
                if(sleepTime>0){
                    WebBinds.runWithDelay(m->{
                        new Thread(()->{
                            synchronized(lock){
                                slept.set(true);
                                lock.notifyAll();
                            }
                        }).start();
                    }, (int)(sleepTime/1_000_000l));
                } else {
                    slept.set(true);
                }
                if (gapTo < timeNow) {
                    timeLate = timeNow - gapTo;
                } else {
                    timeLate = 0;
                }
            }

            if(!slept.get()){
                synchronized(lock){   
                    try{
                        lock.wait(100);
                    }catch(InterruptedException ex){
                    }
                }   
            }    
            timeThen = timeNow;
        }             
    }

    // @Async
    // public static native void vsync();
    // private static void vsync( AsyncCallback<Void> callback) {
    //     vsyncAsync(result -> callback.complete(result));
    // }
 

    @Override
    public void destroy(boolean waitFor) {
        needClose.set(true);
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
    }

    @Override
    public void setAutoFlushFrames(boolean enabled) {
    }

    @Override
    public MouseInput getMouseInput() {
        return new WebMouseInput();
    }

    @Override
    public KeyInput getKeyInput() {
        return new WebKeyInput();
    }

    @Override
    public JoyInput getJoyInput() {
        return null;
    }

    @Override
    public TouchInput getTouchInput() {
        return new WebTouchInput(()->canvasTarget, settings);
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

    @Override
    public Context getOpenCLContext() {
        return null;
    }

    /**
     * Returns the height of the framebuffer.
     *
     * @throws UnsupportedOperationException
     */
    @Override
    public int getFramebufferHeight() {
        if(auxiliaryFrameBuffer !=null)return auxiliaryFrameBuffer.getHeight();
        if(canvasTarget!=null) return  canvasTarget.getClientHeight();
        return 768;
    }

    /**
     * Returns the width of the framebuffer.
     *
     * @throws UnsupportedOperationException
     */
    @Override
    public int getFramebufferWidth() {
        if(auxiliaryFrameBuffer !=null)return auxiliaryFrameBuffer.getWidth();
        if(canvasTarget!=null) return  canvasTarget.getClientWidth();
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
