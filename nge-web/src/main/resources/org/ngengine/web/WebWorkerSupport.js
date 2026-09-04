function addMissingRequirement(missing, condition, description) {
    if (!condition) {
        missing.push(description);
    }
}

export function findMissingWebWorkerRequirements(scope, canvas) {
    const missing = [];
    addMissingRequirement(missing, typeof scope.Worker === "function", "the Worker API");
    addMissingRequirement(missing, typeof scope.OffscreenCanvas === "function", "OffscreenCanvas");
    addMissingRequirement(
        missing,
        canvas && typeof canvas.transferControlToOffscreen === "function",
        "canvas transferControlToOffscreen support"
    );
    addMissingRequirement(missing, typeof scope.SharedArrayBuffer === "function", "SharedArrayBuffer");
    addMissingRequirement(missing, typeof scope.Atomics === "object" && scope.Atomics !== null, "Atomics");
    addMissingRequirement(
        missing,
        scope.crossOriginIsolated === true,
        "cross-origin isolation (COOP and COEP headers)"
    );

    if (missing.length === 0) {
        try {
            new scope.SharedArrayBuffer(8);
        } catch (error) {
            missing.push("permission to allocate shared memory");
        }
    }
    return missing;
}
