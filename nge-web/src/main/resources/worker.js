import Binds from "./org/ngengine/web/WebBindsHub.js";
import loadRuntime from "./runtime.js";

function reportWorkerFailure(kind, error) {
    const value = error instanceof Error ? error : new Error(String(error));
    self.postMessage({
        type: "nge-worker-error",
        kind,
        message: value.message,
        stack: value.stack || ""
    });
}

self.addEventListener("error", event => {
    reportWorkerFailure("error", event.error || event.message);
});

self.addEventListener("unhandledrejection", event => {
    reportWorkerFailure("unhandled rejection", event.reason);
});

Binds.addEventListener("main", async (request)=>{
    const config = request && request.config ? request.config : {};
    const args = request && request.args ? request.args : [];
    const runtime = await loadRuntime(config);
    console.log(`Starting NGE worker with TeaVM ${runtime.backend}`);
    return runtime.main(args);
});

Binds.fireEvent("ready");
console.log("NGE Worker started");
