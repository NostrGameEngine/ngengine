import Binds from "./org/ngengine/web/WebBindsHub.js";
import AudioRenderer from "./org/ngengine/web/AudioRenderer.js";
import ImageLoader from "./org/ngengine/web/ImageLoader.js";
import Nip07Proxy from "./org/ngengine/web/Nip07Proxy.js";
import WindowHooks from "./org/ngengine/web/Window.js";
import WebRTCProxy from "./org/ngengine/web/WebRTCProxy.js";
import ClipboardProxy from "./org/ngengine/web/ClipboardProxy.js";
import { findMissingWebWorkerRequirements } from "./org/ngengine/web/WebWorkerSupport.js";
import loadRuntime from "./runtime.js";




let loadingAnimationTimer = null;
let loadingAnimation = null;
let render = true;
let fullscreenRequested = false;
let fullscreenWarningIssued = false;

function animLoop(){
    window.requestAnimationFrame(()=>{
        if(render){
            Binds.fireEvent("render");
        }
        animLoop();
    });
}


function renderLoadingAnimation(){
    if(loadingAnimation) return;
    const el = document.createElement("div");
    el.setAttribute("id", "ngeLoading");
    el.innerHTML = '<span class="ngeLoader"></span>';  
    document.body.appendChild(el);
    loadingAnimation = el;
}


function bind(canvas, renderTarget){
    AudioRenderer.bind();
    ImageLoader.bind();
    Nip07Proxy.bind();
    WebRTCProxy.bind();
    WindowHooks.bind(canvas, renderTarget);
    WindowHooks.bindListeners(canvas, renderTarget);  
    ClipboardProxy.bind();  
    Binds.addEventListener("ping",()=>{
        if(loadingAnimation){
            loadingAnimation.remove();
            loadingAnimation = null;
        }

        if(loadingAnimationTimer) clearTimeout(loadingAnimationTimer);

        loadingAnimationTimer = setTimeout(()=>{
            renderLoadingAnimation();
        }, 1500);
    });
}


function requestAppFullscreen(canvas){
    const target = document.documentElement || canvas;
    if (!fullscreenRequested || document.fullscreenElement === target) return;
    if (!document.fullscreenEnabled || typeof target.requestFullscreen !== "function") {
        if (!fullscreenWarningIssued) {
            fullscreenWarningIssued = true;
            console.warn("NGE web: fullscreen was requested, but the Fullscreen API is unavailable.");
        }
        return;
    }
    const request = target.requestFullscreen();
    if (request && typeof request.catch === "function") {
        request.catch(error => {
            console.warn("NGE web: fullscreen request was rejected by the browser.", error);
        });
    }
}

function showFullscreenButton(config, canvas, show){
    const requested = Boolean(show);
    if (fullscreenRequested !== requested) {
        fullscreenRequested = requested;
        fullscreenWarningIssued = false;
        if (!requested && document.fullscreenElement && document.exitFullscreen) {
            const exit = document.exitFullscreen();
            if (exit && typeof exit.catch === "function") exit.catch(() => {});
        }
    }

    const button = document.querySelector("#ngeFullscreenButton");
    if(!config.showFullScreenButton){
        if(button) button.remove();
        return;
    }
    if(requested && !button){
        const el = document.createElement("div");
        el.setAttribute("id", "ngeFullscreenButton");
        el.innerHTML = '<span class="ngeFullscreenIcon"></span>';  
        document.body.appendChild(el);
        el.addEventListener("click", (e) => {
            e.stopPropagation();
            requestAppFullscreen(canvas);
        });
    }else if(!requested && button){
        button.remove();
    }   
}

function tweakConfig(config){
    if(!config) config = {};
    if(typeof config.is_capacitor === "undefined"){
        config.is_capacitor = typeof Capacitor !== "undefined" && Capacitor.getPlatform
    };
    if(typeof config.enableWebWorker === "undefined"){
        config.enableWebWorker = typeof config.enable_web_worker === "undefined"
            ? !config.is_capacitor
            : Boolean(config.enable_web_worker);
    }
    if(typeof config.webRuntime === "undefined"){
        config.webRuntime = config.web_runtime || "wasm-gc";
    }
    if(typeof config.webJavaScriptFallback === "undefined"){
        config.webJavaScriptFallback = typeof config.web_javascript_fallback === "undefined"
            ? true
            : Boolean(config.web_javascript_fallback);
    }
    if(typeof config.canvasSelector === "undefined"){
        config.canvasSelector = 'canvas#nge';
    }
    if(typeof config.showFullScreenButton === "undefined"){
        config.showFullScreenButton = !config.is_capacitor;
    }
    return config;
}

export default async function launch(config){
    config = tweakConfig(config);
    console.log("Launch with config",config)

    const canvas = document.querySelector(config.canvasSelector);
    Binds.addEventListener("toggleFullscreen", (v) => {
        showFullscreenButton(config, canvas,v);
    });
    // Browsers only allow requestFullscreen() while handling a user gesture.
    // Remember the application preference and honor it on the next canvas click.
    canvas.addEventListener("click", () => requestAppFullscreen(canvas), true);

    // make canvas always full screen
    let resizeTimeout = null;
    function resize() {
        if(resizeTimeout){
            clearTimeout(resizeTimeout);
        }
        resizeTimeout = setTimeout(()=>{
            let r = 1;
            canvas.style.width = window.innerWidth + 'px';
            canvas.style.height = window.innerHeight + 'px';
            const width = window.innerWidth * r;
            const height = window.innerHeight * r;
            Binds.fireEvent("resizeRenderTarget", width, height);
            resizeTimeout = null;
        },100);     
    }
    window.addEventListener('resize', resize);
    canvas.addEventListener('resize', resize);

    // listen to loss of webgl context
    canvas.addEventListener("webglcontextlost", (event) => {
        event.preventDefault();
        console.warn("WebGL context lost");
        render = false;
        Binds.fireEvent("webglcontextlost");
    });

    canvas.addEventListener("webglcontextrestored", (event) => {
        console.info("WebGL context restored");
        render = true;
        Binds.fireEvent("webglcontextrestored");
    });

    let worker = null;
    let runInWorker = false;
    let renderTarget = canvas;
    if (config.enableWebWorker) {
        const missing = findMissingWebWorkerRequirements(globalThis, canvas);
        if (missing.length > 0) {
            console.warn(
                `NGE web: Web Worker cannot be used because these requirements are unavailable: ${missing.join(", ")}. `
                + "Falling back to the main thread."
            );
        } else {
            try {
                worker = new Worker("./worker.js", { type: "module" });
                worker.addEventListener("error", event => {
                    console.error("NGE worker error", event.message, event.error);
                });
                worker.addEventListener("messageerror", event => {
                    console.error("NGE worker message error", event.data);
                });
                worker.addEventListener("message", event => {
                    if (event.data && event.data.type === "nge-worker-error") {
                        console.error(
                            `NGE worker ${event.data.kind}: ${event.data.message}`,
                            event.data.stack || ""
                        );
                    }
                });
                renderTarget = canvas.transferControlToOffscreen();
                runInWorker = true;
            } catch (error) {
                if (worker) {
                    worker.terminate();
                    worker = null;
                }
                console.warn(
                    "NGE web: Web Worker initialization failed. Falling back to the main thread.",
                    error
                );
            }
        }
    }

    // Bind client actions
    bind(canvas, renderTarget);


    // Start anim loop trigger
    animLoop();

    // resize canvas the first time the backend comes alive
    let firstPing = true;
    Binds.addEventListener("ping", () => {
        if (!firstPing) return;
        resize();
        firstPing = false;
    })


       
    canvas.style.visibility = 'visible';
    console.log("Starting nge...");
    renderLoadingAnimation();
    if (runInWorker) {
        console.info("NGE web: executing on a Web Worker with SharedArrayBuffer.");
        Binds.addEventListener("ready", () => {
            console.log("NGE worker is ready");
            Binds.fireEvent("main", { args: [], config }).then(() => {
                resize();
            })
        });
        Binds.registerWorker(worker);

    } else {
        console.info("NGE web: executing on the main thread.");
        const runtime = await loadRuntime(config);
        Binds.addEventListener("ready", () => {
            console.log(`Starting NGE with TeaVM ${runtime.backend}`);
            runtime.main([]);
            resize();
        });
        Binds.fireEvent("ready");
    }
    

}
