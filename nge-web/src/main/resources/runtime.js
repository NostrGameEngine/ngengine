function configuredRuntime(config) {
    const value = config.webRuntime ?? config.web_runtime ?? "wasm-gc";
    return String(value).trim().toLowerCase();
}

function allowJavaScriptFallback(config) {
    const value = config.webJavaScriptFallback ?? config.web_javascript_fallback;
    return typeof value === "undefined" ? true : Boolean(value);
}

async function loadJavaScriptRuntime() {
    const module = await import("./webapp.js");
    if (typeof module.main !== "function") {
        throw new Error("TeaVM JavaScript module does not export main()");
    }
    return { backend: "javascript", main: module.main };
}

async function loadWasmGCRuntime(config) {
    if (typeof WebAssembly === "undefined") {
        throw new Error("WebAssembly is not supported by this browser");
    }

    await import("./webapp.wasm-runtime.js");
    const teaVM = globalThis.TeaVM;
    if (!teaVM || !teaVM.wasmGC || typeof teaVM.wasmGC.load !== "function") {
        throw new Error("TeaVM Wasm GC loader is unavailable");
    }

    const stackDeobfuscator = config.webStackDeobfuscator
        ?? config.web_stack_deobfuscator
        ?? false;
    const instance = await teaVM.wasmGC.load("./webapp.wasm", {
        scheduler: { timeSliceMillis: config.web_scheduler_budget_ms ?? 4 },
        stackDeobfuscator: { enabled: Boolean(stackDeobfuscator) }
    });
    if (!instance.exports || typeof instance.exports.main !== "function") {
        throw new Error("TeaVM Wasm GC module does not export main()");
    }
    return {
        backend: "wasm-gc",
        main: args => instance.exports.main(args || [])
    };
}

export default async function loadRuntime(config = {}) {
    const runtime = configuredRuntime(config);
    if (runtime === "javascript" || runtime === "js") {
        return loadJavaScriptRuntime();
    }
    if (runtime !== "wasm-gc" && runtime !== "wasmgc" && runtime !== "wasm") {
        throw new Error(`Unsupported web runtime: ${runtime}`);
    }

    try {
        return await loadWasmGCRuntime(config);
    } catch (error) {
        if (!allowJavaScriptFallback(config)) {
            throw error;
        }
        console.warn("TeaVM Wasm GC startup failed; using JavaScript fallback", error);
        return loadJavaScriptRuntime();
    }
}
