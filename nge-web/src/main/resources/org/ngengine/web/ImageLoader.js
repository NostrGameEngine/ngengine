import Binds from "./WebBindsHub.js";
import {loadHDR} from "./Hdr.js";
function isWorker() {
    return (typeof WorkerGlobalScope !== 'undefined' && self instanceof WorkerGlobalScope);
}
function s() {
    return ((typeof window !== 'undefined' && window) ||
        (typeof globalThis !== 'undefined' && globalThis) ||
        (typeof global !== 'undefined' && global) ||
        (typeof self !== 'undefined' && self));
}

function mimeFromFilename(name) {
    if (!name || typeof name !== 'string') return 'image/*';
    const ext = name.split('.').pop().toLowerCase();
    switch (ext) {
        case 'png': return 'image/png';
        case 'jpg':
        case 'jpeg': return 'image/jpeg';
        case 'webp': return 'image/webp';
        case 'gif': return 'image/gif';
        case 'bmp': return 'image/bmp';
        case 'avif': return 'image/avif';
        case 'svg': return 'image/svg+xml';
        default: return 'image/*';
    }
}

async function decodeHdrImage (data, filename) { 
    return loadHDR(data);
}

async function decodeImage (data, filename, scaleW, scaleH) { 
   
    /*
        returns { data: Uint8Array, width: number, height: number }
    */
    const g = s();
    const URL_ = g.URL || g.webkitURL;

    // const doc = g.document;
    let u8in = new Uint8Array(data);
    let svgText;

    if (filename && filename.toLowerCase().endsWith('.svg')){
        svgText = new TextDecoder().decode(u8in);
        svgText = svgText.replace(/currentColor/g, "#ffffff");
        u8in = new TextEncoder().encode(svgText);
    }


    const loadImg = async (u8in, filename)=>{
        let free = null;
        let img = null;
        if(!isWorker()){
            const blob = new Blob([u8in], { type: mimeFromFilename(filename) });
            const url = URL_.createObjectURL(blob);
            img = new Image();
            const p = new Promise((res, rej) => { img.onload = res; img.onerror = () => rej(new Error('Image decode error')); });
            img.src = url;
            await p;
            free = () => { try { URL_.revokeObjectURL(url); } catch (e) { } };
        } else {
            const blob = new Blob([u8in], { type: mimeFromFilename(filename) });
            img = await createImageBitmap(blob, {
                imageOrientation: 'none',
                premultiplyAlpha: 'none',
                colorSpaceConversion: 'default'
            });
            free = () => { try { img.close(); } catch (e) { } };
        }
        return [img, free];
        
    }

    let canvas = null;
    let free = null;
    let img = null;
    try {
        [img, free] = await loadImg(u8in, filename);
        if (!img) {
            throw new Error("Failed to decode image");
        }

        // Intrinsic dimensions
        let w = (img.naturalWidth || img.width) | 0;
        let h = (img.naturalHeight || img.height) | 0;

        // Fallback for SVG without explicit size
        if ((w === 0 || h === 0) && svgText) {
            try {
                const m = svgText.match(/viewBox\s*=\s*"([\d.\s]+)"/i);
                if (m) {
                    const parts = m[1].trim().split(/\s+/).map(Number);
                    if (parts.length === 4) {
                        const vw = parts[2];
                        const vh = parts[3];
                        if (vw > 0 && vh > 0) {
                            const maxDim = 256;
                            const scale = vw > vh ? maxDim / vw : maxDim / vh;
                            w = Math.max(1, (vw * scale) | 0);
                            h = Math.max(1, (vh * scale) | 0);
                        }
                    }
                }
                if (w === 0 || h === 0) { w = 256; h = 256; }
            } catch (_) {
                w = 256; h = 256;
            }
        }

        // scale texture
        let dw = (typeof scaleW === 'number' && scaleW > 0) ? scaleW | 0 : 0;
        let dh = (typeof scaleH === 'number' && scaleH > 0) ? scaleH | 0 : 0;
        if (dw && !dh) {
            dh = Math.max(1, Math.round(h * (dw / w)));
        } else if (dh && !dw) {
            dw = Math.max(1, Math.round(w * (dh / h)));
        }
        if (!dw) dw = w;
        if (!dh) dh = h;

        // canvas = doc.createElement('canvas');
        canvas = new g.OffscreenCanvas(dw, dh);
        // canvas.style.cssText = 'position:absolute;top:-10000px;left:-10000px;visibility:hidden;';
        // doc.body.appendChild(canvas); // some browsers require canvas to be in DOM for certain operations
        canvas.width = dw;
        canvas.height = dh;

        const ctx = canvas.getContext('2d',{
                alpha: true,
                premultipliedAlpha: false   ,
                colorSpace: 'srgb', 
                willReadFrequently: true 
        });
        ctx.clearRect(0, 0, dw, dh);
        ctx.fillStyle = 'rgba(0,0,0,0)';
        ctx.fillRect(0, 0, dw, dh);
        // ctx.imageSmoothingEnabled = true;
        // ctx.imageSmoothingQuality = 'high';

        if (svgText) {
            img.width = dw;
            img.height = dh;
            ctx.drawImage(img, 0, 0, dw, dh);
            const tempData = ctx.getImageData(0, 0, dw, dh);
            ctx.putImageData(tempData, 0, 0);
        } else {
            ctx.drawImage(img, 0, 0, dw, dh);
        }
        ctx.fillRect(0, 0, dw, dh);

        const imageData = ctx.getImageData(0, 0, dw, dh, {
            colorSpace: 'srgb'
        });
        const pixels = imageData.data;  

        return {
            data: new Uint8Array(pixels.buffer, pixels.byteOffset, pixels.length),
            width: dw,
            height: dh
        };
    } finally {
        if (free) free();        
    }
}

function bind(){
    Binds.addEventListener("decodeImage", (data, filename, targetWidth, targetHeight) => {
        return decodeImage(data, filename, targetWidth, targetHeight);
    });
    Binds.addEventListener("decodeHdrImage", (data, filename) => {
        return decodeHdrImage(data, filename);
    });
}


export default {
    decodeHdrImage,
    decodeImage,
    bind
}
