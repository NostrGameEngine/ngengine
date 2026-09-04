import Binds from "./WebBindsHub.js";
let pointerLock = false;
const SOFT_KEYBOARD_ID = "ngeSoftKeyboardInput";

function supportsNativeSoftKeyboard(){
    const nav = typeof navigator !== "undefined" ? navigator : {};
    const touchCapable = Number(nav.maxTouchPoints || 0) > 0
        || (typeof matchMedia === "function" && matchMedia("(pointer: coarse)").matches);
    return touchCapable && typeof document !== "undefined";
}

function bindSoftKeyboard(canvas){
    let requested = false;
    let input = null;
    const sentinel = "\u200b";

    const ensureInput = () => {
        if (input) return input;
        input = document.createElement("textarea");
        input.id = SOFT_KEYBOARD_ID;
        input.setAttribute("aria-hidden", "true");
        input.setAttribute("autocomplete", "off");
        input.setAttribute("autocapitalize", "off");
        input.setAttribute("autocorrect", "off");
        input.setAttribute("spellcheck", "false");
        input.inputMode = "text";
        input.value = sentinel;
        input.style.cssText = [
            "position:fixed",
            "left:0",
            "bottom:0",
            "width:1px",
            "height:1px",
            "padding:0",
            "border:0",
            "opacity:0.01",
            "pointer-events:none",
            "z-index:-1"
        ].join(";");

        const resetValue = () => {
            input.value = sentinel;
            input.setSelectionRange(input.value.length, input.value.length);
        };
        input.addEventListener("focus", resetValue);
        const forwardedCodes = new Set();
        const emitKey = event => Binds.fireEvent(event.type, {
            type: event.type,
            key: event.key,
            code: event.code,
            repeat: Boolean(event.repeat),
            ctrlKey: Boolean(event.ctrlKey),
            shiftKey: Boolean(event.shiftKey),
            altKey: Boolean(event.altKey),
            metaKey: Boolean(event.metaKey)
        });
        input.addEventListener("keydown", event => {
            const modified = event.ctrlKey || event.metaKey || event.altKey;
            const textKey = !modified && (String(event.key || "").length === 1
                || event.code === "Backspace" || event.code === "Delete" || event.code === "Enter");
            if (!textKey) {
                event.preventDefault();
                forwardedCodes.add(event.code);
                emitKey(event);
            }
        });
        input.addEventListener("keyup", event => {
            if (forwardedCodes.delete(event.code)) emitKey(event);
        });
        input.addEventListener("input", event => {
            if (!requested) {
                resetValue();
                return;
            }
            const inputType = String(event.inputType || "");
            let data = event.data == null ? "" : String(event.data);
            if (inputType === "insertLineBreak" || inputType === "insertParagraph") {
                data = "\n";
            }
            Binds.fireEvent("textinput", {
                type: "textinput",
                inputType,
                data
            });
            resetValue();
        });
        document.body.appendChild(input);
        return input;
    };

    const focusInput = () => {
        if (!requested || !supportsNativeSoftKeyboard()) return false;
        const field = ensureInput();
        try {
            field.focus({ preventScroll: true });
            field.setSelectionRange(field.value.length, field.value.length);
            if (navigator.virtualKeyboard && typeof navigator.virtualKeyboard.show === "function") {
                const result = navigator.virtualKeyboard.show();
                if (result && typeof result.catch === "function") result.catch(() => {});
            }
            return document.activeElement === field;
        } catch (error) {
            console.warn("NGE web: native virtual keyboard could not be opened.", error);
            return false;
        }
    };

    Binds.addEventListener("showSoftKeyboard", show => {
        requested = Boolean(show);
        if (!requested) {
            if (input && document.activeElement === input) input.blur();
            if (navigator.virtualKeyboard && typeof navigator.virtualKeyboard.hide === "function") {
                const result = navigator.virtualKeyboard.hide();
                if (result && typeof result.catch === "function") result.catch(() => {});
            }
            return true;
        }
        const focused = focusInput();
        if (!focused) requested = false;
        return focused;
    });
}

function bindGamepads(){
    const previous = new Map();
    let active = false;
    let pollSequence = 0;

    const snapshot = (gamepad, seenSequence) => ({
        id: String(gamepad.id || `Gamepad ${gamepad.index}`),
        mapping: String(gamepad.mapping || ""),
        axes: Array.from(gamepad.axes || [], value => Number(value) || 0),
        buttons: Array.from(gamepad.buttons || [], button => Number(button && button.value) || 0),
        pressed: Array.from(gamepad.buttons || [], button => Boolean(button && button.pressed)),
        seenSequence
    });

    const changed = (before, gamepad) => {
        const axes = gamepad.axes || [];
        const buttons = gamepad.buttons || [];
        if (!before || before.id !== String(gamepad.id || `Gamepad ${gamepad.index}`)
                || before.mapping !== String(gamepad.mapping || "")
                || before.axes.length !== axes.length
                || before.buttons.length !== buttons.length) return true;
        for (let i = 0; i < axes.length; i++) {
            if (Math.abs((Number(axes[i]) || 0) - before.axes[i]) > 0.0001) return true;
        }
        for (let i = 0; i < buttons.length; i++) {
            const button = buttons[i];
            if (Math.abs((Number(button && button.value) || 0) - before.buttons[i]) > 0.0001
                    || Boolean(button && button.pressed) !== before.pressed[i]) return true;
        }
        return false;
    };

    const poll = force => {
        if (!active || typeof navigator.getGamepads !== "function") return;
        const gamepads = navigator.getGamepads() || [];
        const sequence = ++pollSequence;
        for (const gamepad of gamepads) {
            if (!gamepad || !gamepad.connected) continue;
            const index = Number(gamepad.index);
            const prior = previous.get(index);
            if (!prior) {
                Binds.fireEvent("gamepadconnected", {
                    type: "gamepadconnected",
                    index,
                    id: String(gamepad.id || `Gamepad ${index}`),
                    mapping: String(gamepad.mapping || ""),
                    axisCount: (gamepad.axes || []).length,
                    buttonCount: (gamepad.buttons || []).length
                });
            }
            if (force || changed(prior, gamepad)) {
                const next = snapshot(gamepad, sequence);
                Binds.fireEvent("gamepadstate", {
                    type: "gamepadstate",
                    index,
                    id: next.id,
                    mapping: next.mapping,
                    axes: next.axes,
                    buttons: next.buttons,
                    pressed: next.pressed,
                    timestamp: Number(gamepad.timestamp) || 0
                });
                previous.set(index, next);
            } else {
                prior.seenSequence = sequence;
            }
        }
        for (const [index, state] of previous) {
            if (state.seenSequence !== sequence) {
                previous.delete(index);
                Binds.fireEvent("gamepaddisconnected", {
                    type: "gamepaddisconnected",
                    index
                });
            }
        }
    };

    Binds.addEventListener("pollGamepads", () => {
        active = true;
        poll(true);
        return typeof navigator.getGamepads === "function";
    });
    Binds.addEventListener("render", () => poll(false));
    Binds.addEventListener("setGamepadRumble", (index, amountHigh, amountLow, durationMillis) => {
        if (typeof navigator.getGamepads !== "function") return false;
        const gamepad = (navigator.getGamepads() || [])[Number(index)];
        if (!gamepad) return false;
        const actuator = gamepad.vibrationActuator
            || (gamepad.hapticActuators && gamepad.hapticActuators[0]);
        if (!actuator) return false;
        const duration = Math.max(0, Math.min(Number(durationMillis) || 0, 60_000));
        if (duration === 0 && typeof actuator.reset === "function") return actuator.reset();
        if (typeof actuator.playEffect !== "function") return false;
        return actuator.playEffect("dual-rumble", {
            duration,
            startDelay: 0,
            strongMagnitude: Math.max(0, Math.min(1, Number(amountLow) || 0)),
            weakMagnitude: Math.max(0, Math.min(1, Number(amountHigh) || 0))
        });
    });
}

function bind(canvas, renderTarget){

    Binds.addEventListener("getRenderTarget", ()=>{
        return renderTarget;
    });

 

 
    Binds.addEventListener("setPageTitle", (title)=>{
        if (typeof title !== 'string') return;
        if (window.document) {
            window.document.title = title;
        }
        window.name = title;        
    });

    Binds.addEventListener("togglePointerLock", (v)=>{
        pointerLock = Boolean(v);
        if (!pointerLock && document.pointerLockElement === canvas) {
            document.exitPointerLock();
        }
    });

    Binds.addEventListener("getBaseURL", ()=>{
        return new URL(".", window.location.href).href;
    });

    canvas.addEventListener("click", (e) => {
        if (pointerLock && document.pointerLockElement !== canvas) {
            const request = canvas.requestPointerLock();
            if (request && typeof request.catch === "function") {
                request.catch(() => {});
            }
        }
    });

    bindSoftKeyboard(canvas);
    bindGamepads();
}

function bindListeners(canvas,renderTarget){
    canvas.style.touchAction = "none";
    document.documentElement.style.overscrollBehavior = "none";
    let PIXELS_PER_LINE = null;
    const prepareEvent = (inputEvent) => {
        const event = {};
        const keys = [
            "type", "clientX", "clientY", "screenX", "screenY", "button", "buttons",
            "ctrlKey", "shiftKey", "altKey", "metaKey", "deltaY", "deltaMode",
            "key", "code", "repeat", "pointerId", "pointerType", "movementX", "movementY"
        ];
        for(const key of keys) {
            if (inputEvent[key] !== undefined) event[key] = inputEvent[key];
        }

        const makeTouchesClonable = (tx)=>{
            const touches = [];
            for (let i = 0; i < tx.length; i++) {
                const t = tx[i];
                const rect = canvas.getBoundingClientRect();
                const scaleX = canvas.width / rect.width;
                const scaleY = canvas.height / rect.height;
                touches.push({
                    identifier: t.identifier,
                    clientX: Math.round((t.clientX - rect.left) * scaleX),
                    clientY: Math.round(canvas.height - ((t.clientY - rect.top) * scaleY)),
                    screenX: t.screenX,
                    screenY: t.screenY,
                    force: t.force,
                });
            }
            return touches;
        }
        event.touches = inputEvent.touches ? makeTouchesClonable(inputEvent.touches) : [];
        event.targetTouches = inputEvent.targetTouches ? makeTouchesClonable(inputEvent.targetTouches) : [];
        event.changedTouches = inputEvent.changedTouches ? makeTouchesClonable(inputEvent.changedTouches) : [];

        {
            const rect = canvas.getBoundingClientRect();
            const scaleX = canvas.width / rect.width;
            const scaleY = canvas.height / rect.height;

            if(event.movementY) {
                event.movementY = -event.movementY * scaleY; 
            } 

            if(event.movementX){
                event.movementX = event.movementX * scaleX;
            }
        }

        if (event.clientX !== undefined) {
            const rect = canvas.getBoundingClientRect();    
            const scaleX = canvas.width / rect.width;    
            event.clientX = Math.round((event.clientX - rect.left) * scaleX);
        }
        if (event.clientY !== undefined) {
            const rect = canvas.getBoundingClientRect();
            const scaleY = canvas.height / rect.height;
            event.clientY = Math.round(canvas.height - ((event.clientY - rect.top) * scaleY));
        }
        if(event.deltaY !== undefined && event.deltaMode !== undefined) {
            const doc = window.document;
            let deltaValue = event.deltaY;
            let deltaMode = event.deltaMode; // 0=pixel, 1=line, 2=page
        
            let pixelsPerLine = PIXELS_PER_LINE;
            if (!pixelsPerLine) {
                if (!doc || !doc.body) {
                    pixelsPerLine = 16; // fallback when no DOM
                } else {
                    const el = doc.createElement("span");
                    el.style.cssText = "position:absolute;visibility:hidden;font-size:16px;line-height:1.2;margin:0;padding:0;border:0;";
                    el.textContent = "X";
                    doc.body.appendChild(el);
                    const cs = window.getComputedStyle ? window.getComputedStyle(el) : null;
                    const lh = cs && cs.lineHeight && cs.lineHeight.endsWith("px")
                        ? parseFloat(cs.lineHeight)
                        : (el.offsetHeight || 16);
                    doc.body.removeChild(el);
                    pixelsPerLine = lh || 16;
                }
                PIXELS_PER_LINE = pixelsPerLine;
            }
        
            if (deltaMode === 0) {
                event.deltaY = deltaValue; // pixels
            } else if (deltaMode === 1) {
                event.deltaY = deltaValue * pixelsPerLine; // lines -> pixels
            } else {
                // pages -> pixels
                const viewportHeight = doc && doc.documentElement
                    ? Math.max(doc.documentElement.clientHeight, window.innerHeight || 0)
                    : 800;
                const estimatedLinesPerPage = Math.max(1, Math.floor(viewportHeight / pixelsPerLine));
                event.deltaY = deltaValue * pixelsPerLine * estimatedLinesPerPage;
            }        
        }
        return event;
    }



    window.document.addEventListener('mousemove', (event) => {
        // event.preventDefault();
        event = prepareEvent(event);
        Binds.fireEvent("mousemove", event);
    }, false);

    window.document.addEventListener('wheel', (event) => {
        // event.preventDefault();
        event = prepareEvent(event);
        Binds.fireEvent("wheel", event);
    }, false);

    window.document.addEventListener('mousedown', (event) => {
        // event.preventDefault();
        event = prepareEvent(event);
        Binds.fireEvent("mousedown", event);
    }, false);

    window.document.addEventListener('mouseup', (event) => {
        // event.preventDefault();
        event = prepareEvent(event);
        Binds.fireEvent("mouseup", event);
    }, false);

    canvas.addEventListener('touchstart', (event) => {
        if (event.cancelable) event.preventDefault();
        event = prepareEvent(event);
        Binds.fireEvent("touchstart", event);
    }, { capture: true, passive: false });

    canvas.addEventListener('touchmove', (event) => {
        if (event.cancelable) event.preventDefault();
        event = prepareEvent(event);
        Binds.fireEvent("touchmove", event);
    }, { capture: true, passive: false });

    canvas.addEventListener('touchcancel', (event) => {
        if (event.cancelable) event.preventDefault();
        event = prepareEvent(event);
        Binds.fireEvent("touchcancel", event);
    }, { capture: true, passive: false });

    canvas.addEventListener('touchend', (event) => {
        if (event.cancelable) event.preventDefault();
        event = prepareEvent(event);
        Binds.fireEvent("touchend", event);
    }, { capture: true, passive: false });

    window.document.addEventListener('keydown', (event) => {
        if (event.target && event.target.id === SOFT_KEYBOARD_ID) return;
        // Tab is a regular game key.  Without cancelling the browser default,
        // Chrome and Firefox move DOM focus in addition to dispatching KEY_TAB
        // to the engine, which makes the in-game action unreliable.
        if (event.key === "Tab" && event.cancelable) event.preventDefault();
        event = prepareEvent(event);
        Binds.fireEvent("keydown", event);
    }, false);

    window.document.addEventListener('keyup', (event) => {
        if (event.target && event.target.id === SOFT_KEYBOARD_ID) return;
        // event.preventDefault();
        event = prepareEvent(event);
        Binds.fireEvent("keyup", event);
    }, false);

    window.document.addEventListener('keypress', (event) => {
        if (event.target && event.target.id === SOFT_KEYBOARD_ID) return;
        // event.preventDefault();
        event = prepareEvent(event);
        Binds.fireEvent("keypress", event);
    }, false);

    window.document.addEventListener('pointerlockchange', (event) => {
        event = prepareEvent(event);
        Binds.fireEvent("pointerlockchange", event);
    }, false);

    window.document.addEventListener('fullscreenchange', (event) => {
        event = prepareEvent(event);
        Binds.fireEvent("fullscreenchange", event);
    }, false);

    const resetInput = () => Binds.fireEvent("inputreset", { type: "inputreset" });
    window.addEventListener("blur", resetInput, false);
    window.document.addEventListener("visibilitychange", () => {
        if (window.document.hidden) resetInput();
    }, false);
}

export default { bind, bindListeners };
