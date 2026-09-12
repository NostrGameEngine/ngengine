import Binds from "./WebBindsHub.js";

const instances = {};
 
function createAudioContext(sampleRate, id){
    const options = {
        sampleRate,
        latencyHint: "interactive"
    };
    const Context = window.AudioContext || window.webkitAudioContext; 
    const ctx = new Context(options);
    ctx.nge = {};
    ctx.nge.buffers = {};
    ctx.nge.sources = {};    
    ctx.nge.convolver = ctx.createConvolver();
    ctx.nge.convolver.connect(ctx.destination);
    ctx.nge.env = false;

    instances[id] = ctx;
}


function freeAudioContext(id){
    const ctx = instances[id];
    if(ctx){
        ctx.nge.convolver.disconnect();
        for(const src of Object.values(ctx.nge.sources)){
            if(src.node){
                if (src.started) src.node.stop();
                src.node.disconnect();
            }
            if(src.panner) src.panner.disconnect();
            if(src.gain) src.gain.disconnect();
        }
        ctx.nge.sources = {};
        ctx.nge.buffers = {};
        ctx.close();
        delete instances[id];
    }
}

function createAudioBuffer(ctxId, id,  f32channelData, lengthInSamples, sampleRate){
    const numChannels = f32channelData.length;
    const ctx = instances[ctxId];
    if(!ctx) throw new Error("Invalid context id");
    const audioBuffer = ctx.createBuffer(numChannels, lengthInSamples, sampleRate);
    for(let c=0;c<numChannels;c++){
        audioBuffer.copyToChannel(f32channelData[c], c, 0);
    }
    ctx.nge.buffers[id] = audioBuffer;
}
    
function freeAudioBuffer(ctxId, bufId){
    const ctx = instances[ctxId];
    if(!ctx) throw new Error("Invalid context id");
    if(ctx.nge.buffers[bufId]){
        delete ctx.nge.buffers[bufId];
    }
}

function createAudioSource(ctxId, id){
    const ctx = instances[ctxId];
    if(!ctx) throw new Error("Invalid context id");
    ctx.nge.sources[id] ={
        node: ctx.createBufferSource(),
        panner: ctx.createPanner(),
        gain: ctx.createGain(),

        loop: false,
        pausedAt: 0,
        startTime: 0,
        maxDistance: 10000,
        refDistance: 1,
        direction: [1,0,0],
        velocity: [0,0,0],
        position: [0,0,0],
        coneInnerAngle: 360,
        coneOuterAngle: 0,
        coneOuterGain: 0,
        pitch: 1,
        volume: 1,
        duration: 0,
        positional: false,
        started: false,
        reconnectionNeeded: true
        
    };
}

function freeAudioSource(ctxId, srcId){
    const ctx = instances[ctxId];
    if(!ctx) throw new Error("Invalid context id");
    const src = ctx.nge.sources[srcId];
    if(src){
        if(src.node && src.started){
            src.node.stop();
        }
        if(src.node) src.node.disconnect();
        if(src.panner) src.panner.disconnect();
        if(src.gain) src.gain.disconnect();
        delete ctx.nge.sources[srcId];
    }
}



function applyProperties(ctxId, srcId){
    const ctx = instances[ctxId];
    if(!ctx) throw new Error("Invalid context id");
    const src = ctx.nge.sources[srcId];
    if(!src) throw new Error("Invalid source id");
    
    if(src.reconnectionNeeded){
        if(src.positional){
            src.node.disconnect();
            src.panner.disconnect();
            src.gain.disconnect();
            src.node.connect(src.panner);
            src.panner.connect(src.gain);
            if(!ctx.nge.env){
                src.gain.connect(ctx.destination);
            } else {
                src.gain.connect(ctx.nge.convolver);
            }
        } else {
            src.node.disconnect();
            src.panner.disconnect();
            src.gain.disconnect();
            src.node.connect(src.gain);
            if(!ctx.nge.env){
                src.gain.connect(ctx.destination);
            } else {
                src.gain.connect(ctx.nge.convolver);
            }
        }
        src.reconnectionNeeded = false;
    }

    src.panner.maxDistance = src.maxDistance;
    src.panner.refDistance = src.refDistance;
    src.panner.setOrientation(...src.direction);
    src.panner.setPosition(...src.position);
    // TODO velocity
    src.panner.coneInnerAngle = src.coneInnerAngle;
    src.panner.coneOuterAngle = src.coneOuterAngle;
    src.panner.coneOuterGain = src.coneOuterGain;

    if(src.duration){
        src.node.loop = src.loop;
        src.node.loopEnd = src.duration;
        src.node.loopStart = 0;
    } else {
        src.node.loop = false;
    }
    src.node.playbackRate.value = src.pitch;
    src.gain.gain.value = src.volume;   

    if(src.node.buffer !== src.buffer){
        src.node.buffer = src.buffer;
    }
}



function playAudioSource(ctxId, srcId){
    const ctx = instances[ctxId];
    if(!ctx) throw new Error("Invalid context id");
    const src = ctx.nge.sources[srcId];
    if(!src) throw new Error("Invalid source id");   
    applyProperties(ctxId, srcId);
    src.startTime = ctx.currentTime;
    src.node.start(0, src.pausedAt);
    src.started = true;
    const playingNode = src.node;
    src.node.addEventListener('ended', ()=>{
        // stop/pause replace the node; freeing a source/context retires it.
        if (instances[ctxId] !== ctx || ctx.nge.sources[srcId] !== src
                || src.node !== playingNode) return;
        src.started = false;
        Binds.fireEvent("audioSourceEnded", ctxId, srcId);
    }, { once: true });
}

function stopAudioSource(ctxId, srcId){ 
    const ctx = instances[ctxId];
    if(!ctx) throw new Error("Invalid context id");
    const src = ctx.nge.sources[srcId];
    if(!src) throw new Error("Invalid source id");
    src.pausedAt = 0;
    if(src.started) src.node.stop(0);
    src.node.disconnect();
    src.node = ctx.createBufferSource(); 
    src.started = false;
    src.reconnectionNeeded = true;
}

function pauseAudioSource(ctxId, srcId){
    const ctx = instances[ctxId];
    if(!ctx) throw new Error("Invalid context id");
    const src = ctx.nge.sources[srcId];
    if(!src) throw new Error("Invalid source id");
    src.pausedAt = ctx.currentTime - src.startTime;
    if(src.started) src.node.stop(0);
    src.node.disconnect();
    src.node = ctx.createBufferSource(); 
    src.started = false;
    src.reconnectionNeeded = true;
}

function setAudioBuffer(ctxId, srcId, bufId){
    const ctx = instances[ctxId];
    if(!ctx) throw new Error("Invalid context id");
    const src = ctx.nge.sources[srcId];
    if(!src) throw new Error("Invalid source id");
    const buf = ctx.nge.buffers[bufId];
    if(!buf) throw new Error("Invalid buffer id");
    src.buffer = buf;
    src.duration = buf.duration;
    applyProperties(ctxId, srcId);
}


function setContextAudioEnv(ctxId, i8data){
    const ctx = instances[ctxId];
    if(!ctx) throw new Error("Invalid context id");
    if(i8data){
        ctx.decodeAudioData(i8data.buffer).then((audioBuffer)=>{
            ctx.nge.convolver.buffer = audioBuffer;
        });
        ctx.nge.env = true;
    } else {
        ctx.nge.env = false;
    }
    for(const [srcId, src] of Object.entries(ctx.nge.sources)){
        src.reconnectionNeeded = true;    
        applyProperties(ctxId, srcId);
    }
}


function setAudioLoop(ctxId, srcId, v){
    const ctx = instances[ctxId];
    if(!ctx) throw new Error("Invalid context id");
    const src = ctx.nge.sources[srcId];
    if(!src) throw new Error("Invalid source id");
    src.loop = v;
    applyProperties(ctxId, srcId);
}


function setAudioPositional(ctxId, srcId, v){
    const ctx = instances[ctxId];
    if(!ctx) throw new Error("Invalid context id");
    const src = ctx.nge.sources[srcId];
    if(!src) throw new Error("Invalid source id");
    src.positional = v;
    src.reconnectionNeeded = true;
    applyProperties(ctxId, srcId);
}

function setAudioPosition(ctxId, srcId, x, y, z){
    const ctx = instances[ctxId];
    if(!ctx) throw new Error("Invalid context id");
    const src = ctx.nge.sources[srcId];
    if(!src) throw new Error("Invalid source id");
    src.position = [x,y,z];
    applyProperties(ctxId, srcId);
}

function setAudioVelocity(ctxId, srcId, x, y, z){
    const ctx = instances[ctxId];
    if(!ctx) throw new Error("Invalid context id");
    const src = ctx.nge.sources[srcId];
    if(!src) throw new Error("Invalid source id");
    src.velocity = [x,y,z];
    applyProperties(ctxId, srcId);
}

function setAudioMaxDistance(ctxId, srcId, v){
    const ctx = instances[ctxId];
    if(!ctx) throw new Error("Invalid context id");
    const src = ctx.nge.sources[srcId];
    if(!src) throw new Error("Invalid source id");
    src.maxDistance = v;
    applyProperties(ctxId, srcId);
}

function setAudioRefDistance(ctxId, srcId, v){
    const ctx = instances[ctxId];
    if(!ctx) throw new Error("Invalid context id");
    const src = ctx.nge.sources[srcId];
    if(!src) throw new Error("Invalid source id");
    src.refDistance = v;
    applyProperties(ctxId, srcId);
}

function setAudioDirection(ctxId, srcId, x, y, z){
    const ctx = instances[ctxId];
    if(!ctx) throw new Error("Invalid context id");
    const src = ctx.nge.sources[srcId];
    if(!src) throw new Error("Invalid source id");
    src.direction = [x,y,z];
    applyProperties(ctxId, srcId);
}

function setAudioConeInnerAngle(ctxId, srcId, v){
    const ctx = instances[ctxId];
    if(!ctx) throw new Error("Invalid context id");
    const src = ctx.nge.sources[srcId];
    if(!src) throw new Error("Invalid source id");
    src.coneInnerAngle = v;
    applyProperties(ctxId, srcId);
}


function setAudioConeOuterAngle(ctxId, srcId, v){
    const ctx = instances[ctxId];
    if(!ctx) throw new Error("Invalid context id");
    const src = ctx.nge.sources[srcId];
    if(!src) throw new Error("Invalid source id");
    src.coneOuterAngle = v;
    applyProperties(ctxId, srcId);
}

function setAudioConeOuterGain(ctxId, srcId, v){
    const ctx = instances[ctxId];
    if(!ctx) throw new Error("Invalid context id");
    const src = ctx.nge.sources[srcId];
    if(!src) throw new Error("Invalid source id");
    src.coneOuterGain = v;
    applyProperties(ctxId, srcId);
}

function setAudioPitch(ctxId, srcId, v){
    const ctx = instances[ctxId];
    if(!ctx) throw new Error("Invalid context id");
    const src = ctx.nge.sources[srcId];
    if(!src) throw new Error("Invalid source id");
    src.pitch = v;
    applyProperties(ctxId, srcId);
}

function setAudioVolume(ctxId, srcId, v){
    const ctx = instances[ctxId];
    if(!ctx) throw new Error("Invalid context id");
    const src = ctx.nge.sources[srcId];
    if(!src) throw new Error("Invalid source id");
    src.volume = v;
    applyProperties(ctxId, srcId);
}

function getAudioPlaybackRate(ctxId, srcId){
    const ctx = instances[ctxId];
    if(!ctx) throw new Error("Invalid context id");
    const src = ctx.nge.sources[srcId];
    if(!src) throw new Error("Invalid source id");
    return src.node.playbackRate.value;   
}

function setAudioContextListener(
    ctxId,
    posX, posY, posZ,
    velX, velY, velZ,
    dirX, dirY, dirZ,
    upX, upY, upZ
){
    const ctx = instances[ctxId];
    if(!ctx) throw new Error("Invalid context id");
    const listener = ctx.listener;
    if (listener.positionX) {
        listener.positionX.value = posX;
        listener.positionY.value = posY;
        listener.positionZ.value = posZ;
    } else if (listener.setPosition) {
        listener.setPosition(posX, posY, posZ);
    }


    if (listener.forwardX && listener.upX) {
        listener.forwardX.value = dirX;
        listener.forwardY.value = dirY;
        listener.forwardZ.value = dirZ;
        listener.upX.value = upX;
        listener.upY.value = upY;
        listener.upZ.value = upZ;
    } else if (listener.setOrientation) {
        listener.setOrientation(dirX, dirY, dirZ, upX, upY, upZ);
    }
}

function bind(){
    Binds.addEventListener("createAudioContext", (sampleRate, id) => {
        createAudioContext(sampleRate, id);
         
    });

    Binds.addEventListener("freeAudioContext", (id) => {
        freeAudioContext(id);
    });

    Binds.addEventListener("createAudioBuffer", (ctxId, id, f32channelData, lengthInSamples, sampleRate) => {
        createAudioBuffer(ctxId, id, f32channelData, lengthInSamples, sampleRate);
          
    });

    Binds.addEventListener("freeAudioBuffer", (ctxId, bufId) => {
        freeAudioBuffer(ctxId, bufId);
    });

    Binds.addEventListener("createAudioSource", (ctxId, id) => {
        createAudioSource(ctxId, id);          
    });

    Binds.addEventListener("freeAudioSource", (ctxId, srcId) => {
        freeAudioSource(ctxId, srcId);
    });

    Binds.addEventListener("setAudioBuffer", (ctxId, srcId, bufId) => {
        setAudioBuffer(ctxId, srcId, bufId);
    });

    Binds.addEventListener("setAudioPositional", (ctxId, srcId, v) => {
        setAudioPositional(ctxId, srcId, v);
    });

    Binds.addEventListener("setContextAudioEnv", (ctxId, i8data) => {
        setContextAudioEnv(ctxId, i8data);
    });

    Binds.addEventListener("setAudioPosition", (ctxId, srcId, x, y, z) => {
        setAudioPosition(ctxId, srcId, x, y, z);
    });

    Binds.addEventListener("setAudioVelocity", (ctxId, srcId, x, y, z) => {
        setAudioVelocity(ctxId, srcId, x, y, z);
    });

    Binds.addEventListener("setAudioMaxDistance", (ctxId, srcId, v) => {
        setAudioMaxDistance(ctxId, srcId, v);
    });

    Binds.addEventListener("setAudioRefDistance", (ctxId, srcId, v) => {
        setAudioRefDistance(ctxId, srcId, v);
    });

    Binds.addEventListener("setAudioDirection", (ctxId, srcId, x, y, z) => {
        setAudioDirection(ctxId, srcId, x, y, z);
    });

    Binds.addEventListener("setAudioConeInnerAngle", (ctxId, srcId, v) => {
        setAudioConeInnerAngle(ctxId, srcId, v);
    });

    Binds.addEventListener("setAudioConeOuterAngle", (ctxId, srcId, v) => {
        setAudioConeOuterAngle(ctxId, srcId, v);
    });

    Binds.addEventListener("setAudioConeOuterGain", (ctxId, srcId, v) => {
        setAudioConeOuterGain(ctxId, srcId, v);
    });

    Binds.addEventListener("setAudioLoop", (ctxId, srcId, v) => {
        setAudioLoop(ctxId, srcId, v);
    });

    Binds.addEventListener("setAudioPitch", (ctxId, srcId, v) => {
        setAudioPitch(ctxId, srcId, v);
    });

    Binds.addEventListener("setAudioVolume", (ctxId, srcId, v) => {
        setAudioVolume(ctxId, srcId, v);
    });

    Binds.addEventListener("getAudioPlaybackRate", (ctxId, srcId) => {
        return getAudioPlaybackRate(ctxId, srcId);
    });

    Binds.addEventListener("playAudioSource", (ctxId, srcId) => {
        playAudioSource(ctxId, srcId);
    });

    Binds.addEventListener("pauseAudioSource", (ctxId, srcId) => {
        pauseAudioSource(ctxId, srcId);
    });

    Binds.addEventListener("stopAudioSource", (ctxId, srcId) => {
        stopAudioSource(ctxId, srcId);
    });

    Binds.addEventListener("setAudioContextListener", (
        ctxId,
        posX, posY, posZ,
        velX, velY, velZ,
        dirX, dirY, dirZ,
        upX, upY, upZ
    ) => {
        setAudioContextListener(
            ctxId,
            posX, posY, posZ,
            velX, velY, velZ,
            dirX, dirY, dirZ,
            upX, upY, upZ
        );
    });
}

export default {
    bind: bind
}
