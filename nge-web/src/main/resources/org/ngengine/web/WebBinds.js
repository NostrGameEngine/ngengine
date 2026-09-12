 
import Binds from "./WebBindsHub.js";
import ImageLoader from "./ImageLoader.js";
import Nip07Proxy from "./Nip07Proxy.js";
import WebRTCProxy from "./WebRTCProxy.js";
import ClipboardProxy from "./ClipboardProxy.js";
// convert various buffer types to Uint8Array
const _u = (data) => {
    if (data instanceof Uint8Array) {
        return data;
    } else if (data instanceof Int8Array) {
        return new Uint8Array(data.buffer, data.byteOffset, data.byteLength);
    } else if (Array.isArray(data)) {
        return new Uint8Array(data);
    } else if (data instanceof ArrayBuffer) {
        return new Uint8Array(data);
    } else if (data instanceof Uint8ClampedArray) {
        return new Uint8Array(data.buffer, data.byteOffset, data.byteLength);
    } else if (data instanceof DataView) {
        return new Uint8Array(data.buffer, data.byteOffset, data.byteLength);
    } else if (typeof Buffer !== 'undefined' && data instanceof Buffer) {
        return new Uint8Array(data.buffer, data.byteOffset, data.byteLength);
    } else {
        throw new TypeError('Unsupported data type for conversion to Uint8Array');
    }
};

function isWorker() {
    return (typeof WorkerGlobalScope !== 'undefined' && self instanceof WorkerGlobalScope);
}

function s() {
    return ((typeof window !== 'undefined' && window) ||
        (typeof globalThis !== 'undefined' && globalThis) ||
        (typeof global !== 'undefined' && global) ||
        (typeof self !== 'undefined' && self));
}

export const decodeImageAsync = (data /*byte[]*/, filename /*str*/ , targetWidth /*int*/, targetHeight /*int*/, res, rej) => { /* {
        data: Uint8Array,
        width: number,
        height: number
    
    }*/
   if(!isWorker()||!filename.toLowerCase().endsWith('.svg')) {
        ImageLoader.decodeImage(_u(data), filename, targetWidth, targetHeight ).then(res, e=>rej(String(e)));
   } else{
        Binds.fireEvent("decodeImage", _u(data), filename, targetWidth, targetHeight).then(res, e=>rej(String(e)));
   }
}

export const decodeImagePromise = (data, filename, targetWidth, targetHeight) => {
   if (!isWorker() || !filename.toLowerCase().endsWith('.svg')) {
        return ImageLoader.decodeImage(_u(data), filename, targetWidth, targetHeight);
   }
   return Binds.fireEvent("decodeImage", _u(data), filename, targetWidth, targetHeight);
}

export const decodeHdrImageAsync = (data /*byte[]*/, filename /*str*/ , res, rej) => { /* {
        data: Float32Array,
        width: number,
        height: number
    
    }*/
   ImageLoader.decodeHdrImage(_u(data), filename ).then(res, e=>rej(String(e)));
 
}

export const decodeHdrImagePromise = (data, filename) => {
   return ImageLoader.decodeHdrImage(_u(data), filename);
}


export const helloBinds = () => {
    console.log("nge.js is loaded!");
    const g = s();
    if (g) {
        console.log("User Agent: " + (g.navigator ? g.navigator.userAgent : "unknown"));
        console.log("Platform: " + (g.navigator ? g.navigator.platform : "unknown"));
    }
}
 


 





export const loadScriptAsync = async (script, res, rej) => {
    const isWorker = (typeof WorkerGlobalScope !== 'undefined' && self instanceof WorkerGlobalScope);
    if(isWorker) {
        const code = await fetch(script).then(r => r.text());
        self.eval(code);
        console.log("Script loaded via fetch+eval: " + script);
        res();        
    } else {
        console.log("Loading script: " + script);
        const g = s();
        const scriptElement = g.document.createElement("script");
        scriptElement.src = script;
        g.document.head.appendChild(scriptElement);
    
        scriptElement.onload = () => {
            console.log("Script loaded: " + script);
            res();
        }
        scriptElement.onerror = (e) => {
            console.error("Failed to load script: " + script, e);
            rej(String(e));
        }
    }
};




export const setPageTitle = (title) => {
    Binds.fireEvent("setPageTitle", title);
}

export const reloadPage = () => {
    Binds.fireEvent("reloadPage");
}

let lastFullscreenRequest;
export const toggleFullscreen = (v) =>{
    if (v === lastFullscreenRequest) return;
    Binds.fireEvent("toggleFullscreen", v);
    lastFullscreenRequest = v;
}

export const togglePointerLock = (v) =>{
    Binds.fireEvent("togglePointerLock", v);
}

 

const pendingFrameWaits = []; // Alternating callback and monotonic deadline.
const readyFrameWaits = [];
let frameWaitTimer = null;
let frameWaitListening = false;
let frameWaitFlushing = false;
let nativeFrameRequest = null;
let nativeFramesAvailable = typeof globalThis.requestAnimationFrame === 'function'
    && typeof globalThis.cancelAnimationFrame === 'function';
let frameContextLost = false;

Binds.addEventListener('webglcontextlost', () => {
    frameContextLost = true;
    if (nativeFrameRequest !== null) {
        globalThis.cancelAnimationFrame(nativeFrameRequest);
        nativeFrameRequest = null;
    }
});
Binds.addEventListener('webglcontextrestored', () => {
    frameContextLost = false;
    if (pendingFrameWaits.length > 0) requestFrameWake();
});

function completeFrameWaits(deadline) {
    if (frameWaitFlushing) return;
    frameWaitFlushing = true;
    let read = 0;
    while (read < pendingFrameWaits.length && pendingFrameWaits[read + 1] <= deadline) {
        readyFrameWaits.push(pendingFrameWaits[read]);
        read += 2;
    }
    pendingFrameWaits.copyWithin(0, read);
    pendingFrameWaits.length -= read;
    // Detach the batch before invoking callbacks: a resumed game loop can
    // immediately register another wait, which belongs to the next frame.
    for (let i = 0; i < readyFrameWaits.length; i++) {
        const callback = readyFrameWaits[i];
        readyFrameWaits[i] = null;
        try {
            callback();
        } catch (error) {
            console.error("Error in waitNextFrame callback", error);
        }
    }
    readyFrameWaits.length = 0;
    frameWaitFlushing = false;
}

function onFrameWaitRender() {
    completeFrameWaits(Infinity);
}

function onNativeFrame() {
    nativeFrameRequest = null;
    completeFrameWaits(Infinity);
}

function frameWaitWatchdog() {
    frameWaitTimer = null;
    // A suspended native frame must not complete a later wait after this fallback.
    if (pendingFrameWaits.length > 0 && pendingFrameWaits[1] <= performance.now()
            && nativeFrameRequest !== null) {
        globalThis.cancelAnimationFrame(nativeFrameRequest);
        nativeFrameRequest = null;
    }
    completeFrameWaits(performance.now());
    if (pendingFrameWaits.length > 0) {
        requestFrameWake();
        if (frameWaitTimer === null) {
            frameWaitTimer = setTimeout(frameWaitWatchdog,
                Math.max(0, pendingFrameWaits[1] - performance.now()));
        }
    } else if (frameWaitListening) {
        Binds.removeEventListener("render", onFrameWaitRender);
        frameWaitListening = false;
    }
}

function requestFrameWake() {
    if (frameContextLost) return;
    if (nativeFramesAvailable) {
        if (nativeFrameRequest !== null) return;
        try {
            nativeFrameRequest = globalThis.requestAnimationFrame(onNativeFrame);
            Binds.setNativeFrameWait(true);
            return;
        } catch (error) {
            // Some worker environments expose the API without an owner Window.
            nativeFramesAvailable = false;
            Binds.setNativeFrameWait(false);
        }
    }
    if (!frameWaitListening) {
        Binds.addEventListener("render", onFrameWaitRender);
        frameWaitListening = true;
    }
}

export const waitNextFrame = (callback) => {
    pendingFrameWaits.push(callback, performance.now() + 100);
    requestFrameWake();
    // One shared watchdog keeps hidden-tab shutdown/input progressing. Normal
    // frames leave it armed instead of allocating and cancelling a timer each time.
    if (frameWaitTimer === null) {
        frameWaitTimer = setTimeout(frameWaitWatchdog,
            Math.max(0, pendingFrameWaits[1] - performance.now()));
    }
}

// export const fireEventAsync = (event, args, res,rej) => {
//     Binds.fireEvent(event, ...args).then(res).catch(e=>rej(String(e)));
// }

export const getRenderTargetAsync = (res, rej) => {
    Binds.fireEvent("getRenderTarget").then(res, e=>rej(String(e)));
}

export const addResizeRenderTargetListener = (fun)=>{
    Binds.addEventListener("resizeRenderTarget", fun);
}

export const addSwapRenderTargetListener = (fun)=>{
    Binds.addEventListener("swapRenderTarget", fun);
}

export const addInputEventListener = (event, fun) => {
    Binds.addEventListener(event, fun);
}

export const removeInputEventListener = (event, fun) => {
    Binds.removeEventListener(event, fun);
}

export const showSoftKeyboardAsync = (show, res, rej) => {
    Binds.fireEvent("showSoftKeyboard", Boolean(show))
        .then(value => res(Boolean(value)), error => rej(String(error)));
}

export const refreshGamepads = () => {
    Binds.fireEvent("pollGamepads");
}

export const setGamepadRumble = (gamepadIndex, amountHigh, amountLow, durationMillis) => {
    Binds.fireEvent(
        "setGamepadRumble",
        Number(gamepadIndex),
        Number(amountHigh),
        Number(amountLow),
        Number(durationMillis)
    );
}




// audio
export const addAudioEndListener = (fun) => {
    Binds.addEventListener("audioSourceEnded", fun);
    return fun;
};

export const removeAudioEndListener = (fun) => {
    Binds.removeEventListener("audioSourceEnded", fun);
};

export const createAudioContextAsync = (sampleRate, id, res, rej) => {
    Binds.fireEvent("createAudioContext", sampleRate, id).then(res, e=>rej(String(e)));
};

export const freeAudioContext = (id) => {
    Binds.notify("freeAudioContext", id);
};

export const createAudioBufferAsync = (ctxId, id, f32channelData, lengthInSamples, sampleRate, res, rej) => {
    Binds.fireEvent("createAudioBuffer", ctxId, id, f32channelData, lengthInSamples, sampleRate).then(res, e=>rej(String(e)));
};

export const freeAudioBuffer = (ctxId, bufId) => {
    Binds.notify("freeAudioBuffer", ctxId, bufId);
};

export const createAudioSourceAsync = (ctxId, id,  res, rej) => {
    Binds.fireEvent("createAudioSource", ctxId, id).then(res, e=>rej(String(e)));
};

export const freeAudioSource = (ctxId, srcId) => {
    Binds.notify("freeAudioSource", ctxId, srcId);
};

export const setAudioBufferAsync = (ctxId, srcId, bufId, res, rej) => {
    Binds.fireEvent("setAudioBuffer", ctxId, srcId, bufId).then(res, e=>rej(String(e)));
};

export const setAudioPositional = (ctxId, srcId, v) => {
    Binds.notify("setAudioPositional", ctxId, srcId, v);
};

export const setContextAudioEnv = (ctxId, i8data) => {
    Binds.notify("setContextAudioEnv", ctxId, i8data);
};

export const setAudioPosition = (ctxId, srcId, x, y, z) => {
    Binds.notify("setAudioPosition", ctxId, srcId, x, y, z);
};

export const setAudioVelocity = (ctxId, srcId, x, y, z) => {
    Binds.notify("setAudioVelocity", ctxId, srcId, x, y, z);
};

export const setAudioMaxDistance = (ctxId, srcId, v) => {
    Binds.notify("setAudioMaxDistance", ctxId, srcId, v);
};

export const setAudioRefDistance = (ctxId, srcId, v) => {
    Binds.notify("setAudioRefDistance", ctxId, srcId, v);
};

export const setAudioDirection = (ctxId, srcId, x, y, z) => {
    Binds.notify("setAudioDirection", ctxId, srcId, x, y, z);
};

export const setAudioConeInnerAngle = (ctxId, srcId, v) => {
    Binds.notify("setAudioConeInnerAngle", ctxId, srcId, v);
};

export const setAudioConeOuterAngle = (ctxId, srcId, v) => {
    Binds.notify("setAudioConeOuterAngle", ctxId, srcId, v);
};

export const setAudioConeOuterGain = (ctxId, srcId, v) => {
    Binds.notify("setAudioConeOuterGain", ctxId, srcId, v);
};

export const setAudioLoop = (ctxId, srcId, v) => {
    Binds.notify("setAudioLoop", ctxId, srcId, v);
};

export const setAudioPitch = (ctxId, srcId, v) => {
    Binds.notify("setAudioPitch", ctxId, srcId, v);
};

export const setAudioVolume = (ctxId, srcId, v) => {
    Binds.notify("setAudioVolume", ctxId, srcId, v);
};

export const getAudioPlaybackRateAsync = (ctxId, srcId, res, rej) => {
    Binds.fireEvent("getAudioPlaybackRate", ctxId, srcId).then(res, e=>rej(String(e)));
};

export const playAudioSourceAsync = (ctxId, srcId, res, rej) => {
    Binds.fireEvent("playAudioSource", ctxId, srcId).then(res, e=>rej(String(e)));
};

export const pauseAudioSourceAsync = (ctxId, srcId, res, rej) => {
    Binds.fireEvent("pauseAudioSource", ctxId, srcId).then(res, e=>rej(String(e)));
};

export const stopAudioSourceAsync = (ctxId, srcId, res, rej) => {
    Binds.fireEvent("stopAudioSource", ctxId, srcId).then(res, e=>rej(String(e)));
};

export const setAudioContextListener = (
    ctxId, 
    px, py, pz, 
    dx, dy, dz, 
    ux, uy, uz
) => {
    Binds.notify("setAudioContextListener",
        ctxId, 
        px, py, pz, 
        0,0,0,
        dx, dy, dz, 
        ux, uy, uz
    );
}

let baseUrl = null;
export const getBaseURLAsync = (res, rej) => {
    if(baseUrl){
        res(baseUrl);
        return;
    }
    Binds.fireEvent("getBaseURL").then((url) => {
        baseUrl = ""+url;
        res(baseUrl);
    }, e=>rej(String(e)));
}

export const connectNip07BackendAsync = (res, rej) => {
    Nip07Proxy.inject().then(res, e=>rej(String(e)));
}

export const connectWebRTCBackendAsync = (res, rej) => {
    WebRTCProxy.inject().then(res, e=>rej(String(e)));
}

export const connectClipboardBackendAsync = (res, rej) => {
    ClipboardProxy.inject().then(res, e=>rej(String(e)));
}


export const pingFrontEnd = () => {
    Binds.fireEvent("ping");
}


export const runWithDelay=(f, t) => {
    setTimeout(f, t);
}
