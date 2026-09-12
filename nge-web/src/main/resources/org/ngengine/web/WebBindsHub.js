import {
    canUseSharedEvents,
    createSharedEventWriter,
    createSharedEventWriterForBuffer,
    startSharedEventReader
} from "./SharedEvents.js";

const LISTENERS = {};
const WORKERS = [];
const NATIVE_FRAME_WORKERS = new Set();
let nativeFrameWait = false;
const SHARED_WRITERS = new Map();
const PENDING_RESPONSES = {};
let SHARED_PARENT_WRITER = null;
let RESPONSE_COUNTER = 0;

function isWorker() {
    return (typeof WorkerGlobalScope !== 'undefined' && self instanceof WorkerGlobalScope);
}

function checkEvent(e) {
    if (!e.data || typeof e.data !== 'object') {
        throw new Error("Invalid data");
    }

    // if (typeof location !== "undefined" && e.origin !== location.origin) {
    //     throw new Error("Invalid origin: " + e.origin);
    // }
}

function makeTransferableList(args) {
    const transferables = [];
    if (args) {
        for (const a of args) {
            if (a instanceof ArrayBuffer) {
                transferables.push(a);
            } else if (ArrayBuffer.isView(a) && a.buffer) { // Float32Array, Uint8Array, DataView, etc.
                const buf = a.buffer;
                if (buf instanceof ArrayBuffer) {
                    transferables.push(buf);
                }
            } else if (a instanceof MessagePort) {
                transferables.push(a);
            } else if (typeof OffscreenCanvas !== 'undefined' && a instanceof OffscreenCanvas) {
                transferables.push(a);
            } else if (typeof a === 'object' && a !== null && !Array.isArray(a)) {
                try {
                    const values = Object.values(a);
                    transferables.push(...makeTransferableList(values));
                } catch (e) {
                }
            } else if (typeof a === 'object' && a !== null && Array.isArray(a)) {
                try {
                    transferables.push(...makeTransferableList(a));
                } catch (e) {
                }
            }
        }
    }
    return transferables;
}

async function onEvent(e, source){
    checkEvent(e);
    if(!e.data) return;
    if(e.data.type === "frame-wait-mode" && WORKERS.includes(source)) {
        if (e.data.native === true) NATIVE_FRAME_WORKERS.add(source);
        else NATIVE_FRAME_WORKERS.delete(source);
    } else if(e.data.type === "shared-events" && isWorker() && (e.data.toWorker || e.data.buffer)) {
        startSharedEventReader(e.data.toWorker || e.data.buffer, (event, args) => {
            notifyListeners(event, args || []);
        });
        if (e.data.toMain) {
            SHARED_PARENT_WRITER = createSharedEventWriterForBuffer(e.data.toMain);
        }
    } else if(e.data.type === "event" && e.data.event) {
        const id = e.data.id;
        const results = id ? callListeners(e.data.event, ...(e.data.args || []))
            : notifyListeners(e.data.event, e.data.args || []);
        if(id) {
            if(results.length === 0) {
                source.postMessage({ type: "response", id: id, ignored: true, error: "No listeners for "+e.data.event , event: e.data.event });
            } else {
                try{
                    const res = await Promise.race(results);
                    source.postMessage({ type: "response", id: id, result: res , event: e.data.event }, makeTransferableList([res]) );
                } catch(err){
                    source.postMessage({ type: "response", id: id, error: err.message  , event: e.data.event });
                }
            }
        }
    } else if(e.data.type === "response" && e.data.id) {
        const pr = PENDING_RESPONSES[e.data.id];
        if(pr) {
            delete PENDING_RESPONSES[e.data.id];
            if(!e.data.error){
                pr.res(e.data.result);
            } else {
                if(e.data.ignored){
                    pr.res(null);
                }else{
                    console.log("Error response from worker for event "+e.data);
                    pr.rej(new Error(e.data.error));
                }
            }
        }   
    }
}
    
if(isWorker()){
    self.addEventListener("message", (e) => {
        onEvent(e,self).catch((err) => {
            console.warn("Error handling event", err);
        });       
    });
}
    

function registerWorker(worker, options = {}){
    if(!canUseSharedEvents()) {
        throw new Error("A Web Worker requires SharedArrayBuffer and cross-origin isolation");
    }
    WORKERS.push(worker);
    worker.addEventListener("message", (e) => {
        onEvent(e,worker).catch((err) => {
            console.warn("Error handling event", err);
        });
    });
    const toWorker = createSharedEventWriter(options.sharedEventBufferBytes);
    const toMain = createSharedEventWriter(options.sharedEventBufferBytes);
    SHARED_WRITERS.set(worker, toWorker);
    startSharedEventReader(toMain.buffer, (event, args) => {
        notifyListeners(event, args || []);
    });
    worker.postMessage({
        type: "shared-events",
        toWorker: toWorker.buffer,
        toMain: toMain.buffer
    });
}

function addEventListener(name, f){
    if(!LISTENERS[name]){
        LISTENERS[name] = [];
    }
    LISTENERS[name].push(f);
}

function callListeners(event, ...args){
    const f = LISTENERS[event];
    const results = [];
    if(f) {
        for(const fx of f){
            try {
                let r = fx(...(args||[]));
                if(!r || typeof r.then === 'function'){
                    r = Promise.resolve(r);
                }
                results.push(r); 
            } catch(e){
                console.log(fx);
                console.error("Error in event listener for event "+event, e);
            }
        }
    }
    return results;
}

function fireEvent(event, ...args){
    const results = [];
    results.push(...callListeners(event, ...args));

    const waitResponse = (id) => {
        return new Promise((res, rej) => {
            const timeout = setTimeout(() => {
                if (PENDING_RESPONSES[id]) {
                    delete PENDING_RESPONSES[id];
                    console.log("Timeout waiting for response from worker for event ",event,args);
                    rej(new Error("Timeout waiting for response from worker for event "+event) );
                }
            }, 60000);

            PENDING_RESPONSES[id] = {
                res: (r) => {
                    clearTimeout(timeout);
                    res(r);
                }, rej: (e) => {
                    clearTimeout(timeout);
                    rej(e);
                }
            };
        });
    };

    // send to each worker
    for(const w of WORKERS){
         if (event === 'render' && NATIVE_FRAME_WORKERS.has(w)) continue;
         try {
            const sharedWriter = SHARED_WRITERS.get(w);
            if(sharedWriter && sharedWriter.writeEvent(event, args || [])) {
                continue;
            }
            const id = String(RESPONSE_COUNTER++);
            results.push(waitResponse(id));
            w.postMessage({ type: "event", event: event, args: args||[], id }, makeTransferableList(args) ); 
        } catch(e){
            console.error("Error sending event "+event+" to worker", e);
        }
    }

    if(isWorker()){
        if (SHARED_PARENT_WRITER && SHARED_PARENT_WRITER.writeEvent(event, args || [])) {
            return results.length === 0 ? Promise.resolve(null) : Promise.race(results);
        }
        const id = String(RESPONSE_COUNTER++);
        results.push(waitResponse(id));
        self.postMessage({ type: "event", event: event, args: args||[] , id}, makeTransferableList(args) );
    }

    return results.length === 0 ? Promise.resolve(null) : Promise.race(results);
}



function notifyListeners(event, args) {
    for (const listener of LISTENERS[event] || []) {
        try {
            const result = listener(...args);
            if (result && typeof result.then === 'function') {
                Promise.resolve(result).catch(error => console.error('Error in event listener', event, error));
            }
        } catch (error) {
            console.error('Error in event listener', event, error);
        }
    }
}

function notify(event, ...args) {
    notifyListeners(event, args);
    for (const worker of WORKERS) {
        if (event === 'render' && NATIVE_FRAME_WORKERS.has(worker)) continue;
        if (!SHARED_WRITERS.get(worker)?.writeEvent(event, args)) {
            worker.postMessage({ type: 'event', event, args }, makeTransferableList(args));
        }
    }
    if (isWorker() && !SHARED_PARENT_WRITER?.writeEvent(event, args)) {
        self.postMessage({ type: 'event', event, args }, makeTransferableList(args));
    }
}

function setNativeFrameWait(enabled) {
    if (!isWorker() || nativeFrameWait === enabled) return;
    nativeFrameWait = enabled;
    self.postMessage({ type: 'frame-wait-mode', native: enabled });
}

function removeEventListener(event, listener){
    const f = LISTENERS[event];
    if(f) {
        const idx = f.indexOf(listener);
        if(idx >= 0){
            f.splice(idx, 1);
        }
    }
}


export default { addEventListener, fireEvent, notify, setNativeFrameWait, registerWorker, removeEventListener };
