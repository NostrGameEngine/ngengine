const HEADER_BYTES = 64;
const STATE_BYTES = 128;
const DEFAULT_RING_BYTES = 256 * 1024;

const HEAD = 0;
const TAIL = 1;
const WAKE = 2;
const DROPPED = 3;
const RENDER_SEQUENCE = 4;
const RESIZE_SEQUENCE = 5;
const MOUSE_SEQUENCE = 6;
const WHEEL_SEQUENCE = 7;

const MOUSE_CLIENT_X = 0;
const MOUSE_CLIENT_Y = 1;
const MOUSE_SCREEN_X = 2;
const MOUSE_SCREEN_Y = 3;
const MOUSE_BUTTON = 4;
const MOUSE_BUTTONS = 5;
const MOUSE_MODIFIERS = 6;
const MOUSE_MOVEMENT_X = 7;
const MOUSE_MOVEMENT_Y = 8;
const WHEEL_DELTA_Y = 9;
const WHEEL_DELTA_MODE = 10;
const WHEEL_CLIENT_X = 11;
const WHEEL_CLIENT_Y = 12;
const RESIZE_WIDTH = 13;
const RESIZE_HEIGHT = 14;

const SHARED_EVENTS = new Set([
    "render",
    "resizeRenderTarget",
    "mousemove",
    "wheel",
    "mousedown",
    "mouseup",
    "touchstart",
    "touchmove",
    "touchcancel",
    "touchend",
    "keydown",
    "keyup",
    "keypress",
    "textinput",
    "inputreset",
    "gamepadconnected",
    "gamepaddisconnected",
    "gamepadstate",
    "pollGamepads",
    "setGamepadRumble",
    "pointerlockchange",
    "fullscreenchange",
    "ping",
    "setPageTitle",
    "toggleFullscreen",
    "togglePointerLock"
]);

// Keep Web Audio commands on postMessage. Context/buffer/source creation waits
// for a response, and mixing that ordered control stream with one-way shared
// events lets property updates overtake the object they address.

const encoder = new TextEncoder();
const decoder = new TextDecoder();

function notify(header) {
    Atomics.add(header, WAKE, 1);
    Atomics.notify(header, WAKE);
}

function number(value) {
    const result = Number(value);
    return Number.isFinite(result) ? result : 0;
}

function modifierMask(event) {
    return (event.ctrlKey ? 1 : 0)
        | (event.shiftKey ? 2 : 0)
        | (event.altKey ? 4 : 0)
        | (event.metaKey ? 8 : 0);
}

function writeUint32(ring, offset, value) {
    for (let i = 0; i < 4; i++) {
        ring[(offset + i) % ring.length] = (value >>> (i * 8)) & 0xff;
    }
}

function readUint32(ring, offset) {
    let value = 0;
    for (let i = 0; i < 4; i++) {
        value |= ring[(offset + i) % ring.length] << (i * 8);
    }
    return value >>> 0;
}

function copyIntoRing(ring, offset, bytes) {
    for (let i = 0; i < bytes.length; i++) {
        ring[(offset + i) % ring.length] = bytes[i];
    }
}

function copyFromRing(ring, offset, length) {
    const bytes = new Uint8Array(length);
    for (let i = 0; i < length; i++) {
        bytes[i] = ring[(offset + i) % ring.length];
    }
    return bytes;
}

function makeViews(buffer) {
    return {
        buffer,
        header: new Int32Array(buffer, 0, HEADER_BYTES / Int32Array.BYTES_PER_ELEMENT),
        state: new Float64Array(buffer, HEADER_BYTES, STATE_BYTES / Float64Array.BYTES_PER_ELEMENT),
        ring: new Uint8Array(buffer, HEADER_BYTES + STATE_BYTES)
    };
}

export function canUseSharedEvents() {
    return typeof SharedArrayBuffer !== "undefined"
        && typeof Atomics !== "undefined"
        && globalThis.crossOriginIsolated === true;
}

function writerForViews(views) {
    function writeEvent(event, args) {
        if (!SHARED_EVENTS.has(event)) {
            return false;
        }
        if (event === "render") {
            Atomics.add(views.header, RENDER_SEQUENCE, 1);
            notify(views.header);
            return true;
        }
        if (event === "resizeRenderTarget") {
            views.state[RESIZE_WIDTH] = number(args[0]);
            views.state[RESIZE_HEIGHT] = number(args[1]);
            Atomics.add(views.header, RESIZE_SEQUENCE, 1);
            notify(views.header);
            return true;
        }
        if (event === "mousemove") {
            const value = args && args[0] ? args[0] : {};
            views.state[MOUSE_CLIENT_X] = number(value.clientX);
            views.state[MOUSE_CLIENT_Y] = number(value.clientY);
            views.state[MOUSE_SCREEN_X] = number(value.screenX);
            views.state[MOUSE_SCREEN_Y] = number(value.screenY);
            views.state[MOUSE_BUTTON] = number(value.button);
            views.state[MOUSE_BUTTONS] = number(value.buttons);
            views.state[MOUSE_MODIFIERS] = modifierMask(value);
            views.state[MOUSE_MOVEMENT_X] += number(value.movementX);
            views.state[MOUSE_MOVEMENT_Y] += number(value.movementY);
            Atomics.add(views.header, MOUSE_SEQUENCE, 1);
            notify(views.header);
            return true;
        }
        if (event === "wheel") {
            const value = args && args[0] ? args[0] : {};
            views.state[WHEEL_DELTA_Y] += number(value.deltaY);
            views.state[WHEEL_DELTA_MODE] = number(value.deltaMode);
            views.state[WHEEL_CLIENT_X] = number(value.clientX);
            views.state[WHEEL_CLIENT_Y] = number(value.clientY);
            Atomics.add(views.header, WHEEL_SEQUENCE, 1);
            notify(views.header);
            return true;
        }

        let payload;
        try {
            payload = encoder.encode(JSON.stringify({ event, args: args || [] }));
        } catch (error) {
            console.warn(`Could not encode shared event ${event}`, error);
            return false;
        }

        const required = payload.length + 4;
        if (required >= views.ring.length) {
            Atomics.add(views.header, DROPPED, 1);
            return false;
        }
        const head = Atomics.load(views.header, HEAD);
        const tail = Atomics.load(views.header, TAIL);
        const free = (tail - head - 1 + views.ring.length) % views.ring.length;
        if (required > free) {
            Atomics.add(views.header, DROPPED, 1);
            return false;
        }

        writeUint32(views.ring, head, payload.length);
        copyIntoRing(views.ring, (head + 4) % views.ring.length, payload);
        Atomics.store(views.header, HEAD, (head + required) % views.ring.length);
        notify(views.header);
        return true;
    }

    return { buffer: views.buffer, writeEvent };
}

export function createSharedEventWriter(ringBytes = DEFAULT_RING_BYTES) {
    return writerForViews(makeViews(new SharedArrayBuffer(HEADER_BYTES + STATE_BYTES + ringBytes)));
}

export function createSharedEventWriterForBuffer(buffer) {
    if (!(buffer instanceof SharedArrayBuffer)) {
        throw new TypeError("Shared event transport requires a SharedArrayBuffer");
    }
    return writerForViews(makeViews(buffer));
}

export function startSharedEventReader(buffer, dispatch) {
    const views = makeViews(buffer);
    let active = true;
    let renderSequence = Atomics.load(views.header, RENDER_SEQUENCE);
    let resizeSequence = Atomics.load(views.header, RESIZE_SEQUENCE);
    let mouseSequence = Atomics.load(views.header, MOUSE_SEQUENCE);
    let wheelSequence = Atomics.load(views.header, WHEEL_SEQUENCE);
    let mouseMovementX = views.state[MOUSE_MOVEMENT_X];
    let mouseMovementY = views.state[MOUSE_MOVEMENT_Y];
    let wheelDeltaY = views.state[WHEEL_DELTA_Y];
    const mouseEvent = {
        type: "mousemove",
        touches: [],
        targetTouches: [],
        changedTouches: []
    };
    const wheelEvent = {
        type: "wheel",
        touches: [],
        targetTouches: [],
        changedTouches: []
    };

    function applyModifiers(event, mask) {
        event.ctrlKey = (mask & 1) !== 0;
        event.shiftKey = (mask & 2) !== 0;
        event.altKey = (mask & 4) !== 0;
        event.metaKey = (mask & 8) !== 0;
    }

    function drain() {
        const nextRenderSequence = Atomics.load(views.header, RENDER_SEQUENCE);
        if (nextRenderSequence !== renderSequence) {
            renderSequence = nextRenderSequence;
            dispatch("render", []);
        }

        const nextResizeSequence = Atomics.load(views.header, RESIZE_SEQUENCE);
        if (nextResizeSequence !== resizeSequence) {
            resizeSequence = nextResizeSequence;
            dispatch("resizeRenderTarget", [views.state[RESIZE_WIDTH], views.state[RESIZE_HEIGHT]]);
        }

        const nextMouseSequence = Atomics.load(views.header, MOUSE_SEQUENCE);
        if (nextMouseSequence !== mouseSequence) {
            mouseSequence = nextMouseSequence;
            mouseEvent.clientX = views.state[MOUSE_CLIENT_X];
            mouseEvent.clientY = views.state[MOUSE_CLIENT_Y];
            mouseEvent.screenX = views.state[MOUSE_SCREEN_X];
            mouseEvent.screenY = views.state[MOUSE_SCREEN_Y];
            mouseEvent.button = views.state[MOUSE_BUTTON];
            mouseEvent.buttons = views.state[MOUSE_BUTTONS];
            applyModifiers(mouseEvent, views.state[MOUSE_MODIFIERS]);
            const nextMovementX = views.state[MOUSE_MOVEMENT_X];
            const nextMovementY = views.state[MOUSE_MOVEMENT_Y];
            mouseEvent.movementX = nextMovementX - mouseMovementX;
            mouseEvent.movementY = nextMovementY - mouseMovementY;
            mouseMovementX = nextMovementX;
            mouseMovementY = nextMovementY;
            dispatch("mousemove", [mouseEvent]);
        }

        const nextWheelSequence = Atomics.load(views.header, WHEEL_SEQUENCE);
        if (nextWheelSequence !== wheelSequence) {
            wheelSequence = nextWheelSequence;
            const nextDeltaY = views.state[WHEEL_DELTA_Y];
            wheelEvent.deltaY = nextDeltaY - wheelDeltaY;
            wheelDeltaY = nextDeltaY;
            wheelEvent.deltaMode = views.state[WHEEL_DELTA_MODE];
            wheelEvent.clientX = views.state[WHEEL_CLIENT_X];
            wheelEvent.clientY = views.state[WHEEL_CLIENT_Y];
            dispatch("wheel", [wheelEvent]);
        }

        let tail = Atomics.load(views.header, TAIL);
        const head = Atomics.load(views.header, HEAD);
        while (tail !== head) {
            const length = readUint32(views.ring, tail);
            if (length > views.ring.length - 4) {
                console.error("Invalid shared event record length", length);
                Atomics.store(views.header, TAIL, head);
                return;
            }
            const bytes = copyFromRing(views.ring, (tail + 4) % views.ring.length, length);
            tail = (tail + length + 4) % views.ring.length;
            Atomics.store(views.header, TAIL, tail);
            try {
                const record = JSON.parse(decoder.decode(bytes));
                dispatch(record.event, record.args || []);
            } catch (error) {
                console.warn("Could not decode shared event", error);
            }
        }
    }

    async function pump() {
        while (active) {
            drain();
            const wake = Atomics.load(views.header, WAKE);
            if (typeof Atomics.waitAsync === "function") {
                const wait = Atomics.waitAsync(views.header, WAKE, wake, 16);
                if (wait.async) {
                    await wait.value;
                } else {
                    // A synchronous "not-equal" result does not yield to the
                    // worker task queue. Under continuous render traffic that
                    // can starve postMessage events, including application
                    // startup and non-shared bridge calls.
                    await new Promise(resolve => setTimeout(resolve, 0));
                }
            } else {
                await new Promise(resolve => setTimeout(resolve, 4));
            }
        }
    }

    pump().catch(error => console.error("Shared event reader stopped", error));
    return () => {
        active = false;
        notify(views.header);
    };
}
