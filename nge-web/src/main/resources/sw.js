importScripts("./zip-core.js");
zip.configure({ useWebWorkers: false });

const SCOPE = self.registration.scope;
const HASH_HEADER = "X-NGE-Resource-Hash";
const SNAPSHOT_KEY = new URL("__preload_manifest__", SCOPE).href;
const DIRECTORY_PREFIX = new URL("__bundle_directory__/", SCOPE).href;
const MAX_JOBS = 4;
const BACKGROUND_JOBS = 3; // Leave capacity for an urgent game request.
let cachePromise;
let statePromise;
let preloadPromise;
let preloading = false;
let reportTimer;

async function getCache() {
    if (!cachePromise) cachePromise = (async () => {
        // Keep the existing namespace so completed resources survive this upgrade.
        const digest = await crypto.subtle.digest("SHA-256",
            new TextEncoder().encode(self.location.origin + SCOPE));
        const hash = Array.from(new Uint8Array(digest), b => b.toString(16).padStart(2, "0")).join("");
        return caches.open("ngeapp-" + hash.slice(0, 16) + "-v1");
    })();
    return cachePromise;
}

async function initialize(configUrl = "ngeapp.json", refresh = true) {
    if (statePromise) return statePromise;
    statePromise = (async () => {
        const cache = await getCache();
        const saved = await cache.match(SNAPSHOT_KEY);
        let snapshot = saved ? await saved.json() : null;
        if (refresh || !snapshot) {
            try {
                const responses = await Promise.all([configUrl, "resources.index.txt", "preload-ignore.txt"]
                    .map(path => fetch(new URL(path, SCOPE).href, { cache: "no-cache" })));
                for (const response of responses) {
                    if (!response.ok) throw new Error("Unable to load preload manifest: " + response.status);
                }
                snapshot = { config: await responses[0].json(), index: await responses[1].text(),
                    ignore: await responses[2].text() };
                await cache.put(SNAPSHOT_KEY, new Response(JSON.stringify(snapshot)));
            } catch (error) {
                if (!snapshot) throw error;
                console.warn("Using the saved preload manifest", error);
            }
        }
        const entries = new Map();
        const ignores = snapshot.ignore.split(/\r?\n/).map(line => line.trim()).filter(Boolean);
        for (const line of snapshot.index.split(/\r?\n/)) {
            const match = /^(\S+)\s+(\d+)\s+(.+)$/.exec(line);
            if (!match) continue;
            const path = match[3];
            const url = new URL(path, SCOPE);
            if (url.origin !== self.location.origin || !url.href.startsWith(SCOPE)) continue;
            entries.set(path, { path, url: url.href, hash: match[1], size: Number(match[2]),
                preload: !ignores.some(prefix => path.startsWith(prefix)) });
        }
        if (!entries.size) throw new Error("The resource index is empty");
        const oldHashes = await cache.match(new URL("__hashes__", SCOPE).href);
        const state = { cache, config: snapshot.config, entries,
            legacyHashes: oldHashes ? await oldHashes.json() : {},
            completed: new Set(), doneBytes: 0, totalBytes: 0, total: 0,
            jobs: new Map(), queue: [], active: 0, foreground: 0,
            bundlePromise: null, bundle: null, canSkip: !snapshot.config.bundle,
            last: "", status: "Checking saved resources...", error: null, scanned: false, disabled: false };
        for (const entry of entries.values()) {
            if (entry.preload) { state.total++; state.totalBytes += entry.size; }
        }
        const obsoleteChunks = new URL("__bundle_chunks__/", SCOPE).href;
        const directory = DIRECTORY_PREFIX + encodeURIComponent(snapshot.config.bundleHash
            || new URL(snapshot.config.bundle || "", SCOPE).href) + "/";
        for (const request of await cache.keys()) {
            if (request.url.startsWith(obsoleteChunks)
                    || (request.url.startsWith(DIRECTORY_PREFIX) && !request.url.startsWith(directory))) {
                await cache.delete(request);
            }
        }
        return state;
    })();
    try { return await statePromise; }
    catch (error) { statePromise = null; throw error; }
}

async function cachedResource(state, entry) {
    const response = await state.cache.match(entry.url);
    if (response?.ok && (response.headers.get(HASH_HEADER) || state.legacyHashes[entry.url]) === entry.hash) {
        return response;
    }
    return null;
}

function markCompleted(state, entry) {
    if (!entry.preload || state.completed.has(entry.path)) return;
    state.completed.add(entry.path);
    state.doneBytes += entry.size;
    state.last = entry.path;
}

async function report(state, override = {}) {
    const complete = state.scanned && (state.completed.size === state.total || state.disabled) && !state.error;
    const message = { type: "preload-progress", done: state.completed.size, total: state.total,
        doneBytes: state.doneBytes, totalBytes: state.totalBytes, last: state.last,
        status: complete ? "Ready" : state.status, complete,
        canSkip: state.canSkip, error: state.error, ...override };
    for (const client of await self.clients.matchAll({ includeUncontrolled: true, type: "window" })) {
        client.postMessage(message);
    }
}

async function readRange(bundle, offset, length) {
    if (!Number.isSafeInteger(offset) || !Number.isSafeInteger(length)
            || offset < 0 || length < 0 || offset + length > bundle.size) {
        throw new Error("Invalid ZIP resource range");
    }
    if (!length) return new Uint8Array();
    const end = offset + length - 1;
    const response = await fetch(bundle.url, {
        headers: { Range: "bytes=" + offset + "-" + end }, cache: "no-store"
    });
    const expected = "bytes " + offset + "-" + end + "/" + bundle.size;
    if (response.status !== 206 || response.headers.get("Content-Range") !== expected) {
        await response.body?.cancel();
        throw new Error("The server returned an invalid ZIP range");
    }
    const bytes = new Uint8Array(await response.arrayBuffer());
    if (bytes.length !== length) throw new Error("Truncated ZIP resource range");
    return bytes;
}

async function openBundle(state) {
    if (!state.config.bundle) return null;
    if (state.bundlePromise) return state.bundlePromise;
    state.bundlePromise = (async () => {
        const url = new URL(state.config.bundle, SCOPE).href;
        const hash = state.config.bundleHash || url;
        const metadataKey = DIRECTORY_PREFIX + encodeURIComponent(hash) + "/metadata";
        const storedMetadata = await state.cache.match(metadataKey);
        let metadata = storedMetadata ? await storedMetadata.json() : null;
        const fullKey = new URL("bundle.zip", SCOPE).href;
        const full = await state.cache.match(fullKey);
        let reader;
        const bundle = { url, hash, size: metadata?.size, segments: new Map(), directoryOffset: null };
        if (full && (full.headers.get(HASH_HEADER) || state.legacyHashes["bundle.zip"]) === hash) {
            reader = new zip.BlobReader(await full.blob());
        } else {
            if (!metadata) {
                state.status = "Reading ZIP directory...";
                await report(state, { complete: false });
                const probe = await fetch(url, { headers: { Range: "bytes=0-0" }, cache: "no-store" });
                if (probe.status === 200) {
                    // Keep this response: a server ignoring Range already sent the entire archive.
                    state.canSkip = false;
                    state.status = "Downloading bundled resources...";
                    const total = Number(probe.headers.get("Content-Length")) || null;
                    let received = 0;
                    let lastReport = 0;
                    const input = probe.body.getReader();
                    const stream = new ReadableStream({
                        async pull(controller) {
                            try {
                                const result = await input.read();
                                if (result.done) {
                                    if (total !== null && received !== total) throw new Error("Truncated ZIP download");
                                    controller.close();
                                    return;
                                }
                                received += result.value.length;
                                controller.enqueue(result.value);
                                if (Date.now() - lastReport > 150) {
                                    lastReport = Date.now();
                                    await report(state, { complete: false, doneBytes: received, totalBytes: total });
                                }
                            } catch (error) { controller.error(error); }
                        },
                        cancel(reason) { return input.cancel(reason); }
                    });
                    await state.cache.put(fullKey, new Response(stream, { headers: {
                        "Content-Type": "application/zip", [HASH_HEADER]: hash
                    } }));
                    reader = new zip.BlobReader(await (await state.cache.match(fullKey)).blob());
                } else {
                    const range = /^bytes 0-0\/(\d+)$/.exec(probe.headers.get("Content-Range") || "");
                    if (probe.status !== 206 || !range) throw new Error("Invalid ZIP range probe");
                    const bytes = await probe.arrayBuffer();
                    bundle.size = Number(range[1]);
                    if (bytes.byteLength !== 1 || !Number.isSafeInteger(bundle.size) || bundle.size < 22) {
                        throw new Error("Invalid ZIP size");
                    }
                    metadata = { size: bundle.size };
                    await state.cache.put(metadataKey, new Response(JSON.stringify(metadata)));
                }
            }
            if (!reader) {
                bundle.mode = "range";
                reader = new zip.Reader();
                reader.size = bundle.size;
                let readingDirectory = true;
                reader.readUint8Array = async (offset, length) => {
                    for (const [start, bytes] of bundle.segments) {
                        if (offset >= start && offset + length <= start + bytes.length) {
                            return bytes.subarray(offset - start, offset - start + length);
                        }
                    }
                    if (!readingDirectory) throw new Error("ZIP read outside the scheduled resource");
                    const key = DIRECTORY_PREFIX + encodeURIComponent(hash) + "/" + offset + "-" + length;
                    const saved = await state.cache.match(key);
                    const bytes = saved ? new Uint8Array(await saved.arrayBuffer())
                        : await readRange(bundle, offset, length);
                    if (bytes.length !== length) throw new Error("Truncated cached ZIP directory");
                    if (!saved) await state.cache.put(key, new Response(bytes));
                    if (length >= 4 && new DataView(bytes.buffer, bytes.byteOffset, bytes.byteLength)
                            .getUint32(0, true) === 0x02014b50) bundle.directoryOffset = offset;
                    return bytes;
                };
                bundle.finishDirectory = () => { readingDirectory = false; };
            }
        }
        bundle.reader = new zip.ZipReader(reader);
        try {
            const entries = await bundle.reader.getEntries();
            if (!entries.length) throw new Error("The ZIP archive is empty");
            bundle.entries = new Map();
            const sorted = entries.slice().sort((a, b) => a.offset - b.offset);
            for (let i = 0; i < sorted.length; i++) {
                const entry = sorted[i];
                if (bundle.mode === "range") {
                    entry.rangeEnd = sorted[i + 1]?.offset ?? bundle.directoryOffset;
                    if (!Number.isSafeInteger(entry.offset) || !Number.isSafeInteger(entry.rangeEnd)
                            || entry.rangeEnd <= entry.offset || entry.rangeEnd > bundle.size) {
                        throw new Error("Invalid ZIP entry boundaries");
                    }
                }
                if (!entry.directory) bundle.entries.set(entry.filename, entry);
            }
            bundle.finishDirectory?.();
        } catch (error) {
            if (bundle.mode !== "range") await state.cache.delete(fullKey);
            throw error;
        }
        state.bundle = bundle;
        state.canSkip = bundle.mode === "range";
        state.status = state.canSkip ? "Preparing resources..." : "Downloading launcher resources...";
        return bundle;
    })();
    try { return await state.bundlePromise; }
    catch (error) { state.bundlePromise = null; throw error; }
}

async function loadResource(state, entry) {
    const saved = await cachedResource(state, entry);
    if (saved) { markCompleted(state, entry); return saved; }
    const bundle = await openBundle(state);
    const zipEntry = bundle?.entries.get(entry.path);
    let response;
    if (zipEntry) {
        if (bundle.mode === "range") {
            const bytes = await readRange(bundle, zipEntry.offset, zipEntry.rangeEnd - zipEntry.offset);
            bundle.segments.set(zipEntry.offset, bytes);
        }
        try {
            const blob = await zipEntry.getData(new zip.BlobWriter(), {
                checkSignature: true, useWebWorkers: false
            });
            if (blob.size !== entry.size) throw new Error("Unexpected resource size: " + entry.path);
            response = new Response(blob, { headers: { "Content-Type": guessMime(entry.path) } });
        } finally {
            bundle.segments.delete(zipEntry.offset);
        }
    } else {
        response = await fetch(entry.url, { cache: "no-cache" });
    }
    if (!response.ok) throw new Error("Resource " + entry.path + ": HTTP " + response.status);
    const headers = new Headers(response.headers);
    headers.set(HASH_HEADER, entry.hash);
    // The body and its version are committed together, without rewriting a global hash map.
    const versioned = new Response(response.body, { status: response.status, headers });
    await state.cache.put(entry.url, versioned);
    const stored = await state.cache.match(entry.url);
    if (!stored) throw new Error("Resource cache write did not persist: " + entry.path);
    markCompleted(state, entry);
    return stored;
}

function drainJobs(state) {
    while (state.active < MAX_JOBS && state.queue.length) {
        const urgent = state.queue.findIndex(job => job.foreground);
        if (urgent < 0 && (state.foreground || state.active >= BACKGROUND_JOBS)) return;
        const job = state.queue.splice(urgent < 0 ? 0 : urgent, 1)[0];
        state.active++;
        loadResource(state, job.entry).then(job.resolve, job.reject).finally(() => {
            state.active--;
            state.jobs.delete(job.entry.path);
            drainJobs(state);
        });
    }
}

function requestResource(state, entry, foreground = false) {
    let job = state.jobs.get(entry.path);
    if (!job) {
        job = { entry, foreground };
        job.promise = new Promise((resolve, reject) => { job.resolve = resolve; job.reject = reject; });
        state.jobs.set(entry.path, job);
        state.queue.push(job);
    }
    if (foreground) {
        job.foreground = true;
        state.foreground++;
    }
    drainJobs(state);
    // Each consumer receives its own body; the download/extraction/cache job is shared.
    return job.promise.then(response => foreground ? response.clone() : undefined).finally(() => {
        if (foreground) { state.foreground--; drainJobs(state); }
    });
}

async function startPreload(configUrl) {
    if (preloadPromise) {
        if (statePromise) await report(await statePromise);
        return preloadPromise;
    }
    preloadPromise = (async () => {
        const state = await initialize(configUrl, true);
        state.error = null;
        preloading = true;
        reportTimer = setInterval(() => { report(state).catch(console.warn); }, 200);
        try {
            const entries = [...state.entries.values()].filter(entry => entry.preload);
            let cursor = 0;
            // Cache lookups are bounded too; progress resumes from committed resources.
            await Promise.all(Array.from({ length: 8 }, async () => {
                while (cursor < entries.length) {
                    const entry = entries[cursor++];
                    if (await cachedResource(state, entry)) markCompleted(state, entry);
                }
            }));
            state.scanned = true;
            if (state.completed.size === state.total) { state.canSkip = true; return; }
            const bundle = await openBundle(state);
            if (bundle && bundle.mode !== "range") {
                // Bootstrap files live outside the ZIP. Finish their downloads as well
                // before offering a local-only extraction phase on non-Range servers.
                const loose = entries.filter(entry => !bundle.entries.has(entry.path)
                    && !state.completed.has(entry.path));
                let nextLoose = 0;
                await Promise.all(Array.from({ length: BACKGROUND_JOBS }, async () => {
                    while (preloading && nextLoose < loose.length) {
                        await requestResource(state, loose[nextLoose++]);
                    }
                }));
            }
            state.canSkip = true;
            state.status = "Preparing resources...";
            if (state.config.preloadResources === false || state.config.preload_resources === false) {
                state.disabled = true;
                await report(state, { status: "Ready (preload disabled)", complete: true, canSkip: true });
                return;
            }
            cursor = 0;
            await Promise.all(Array.from({ length: BACKGROUND_JOBS }, async () => {
                while (preloading && cursor < entries.length) {
                    const entry = entries[cursor++];
                    if (state.completed.has(entry.path)) continue;
                    try { await requestResource(state, entry); }
                    catch (error) {
                        state.error = "Some resources could not be cached. Reload to retry.";
                        console.warn("Resource preload failed", entry.path, error);
                    }
                }
            }));
            if (state.error) state.status = state.error;
        } finally {
            preloading = false;
            clearInterval(reportTimer);
            reportTimer = null;
            await report(state);
        }
    })();
    try { return await preloadPromise; }
    catch (error) {
        console.warn("Preload failed", error);
        if (statePromise) {
            const state = await statePromise;
            state.error = String(error.message || error);
            state.status = "Preload failed. Reload to retry.";
            await report(state, { complete: false });
        }
    } finally { preloadPromise = null; }
}

self.addEventListener("fetch", event => {
    const request = event.request;
    const url = new URL(request.url);
    if (request.method !== "GET" || url.origin !== self.location.origin
            || !url.href.startsWith(SCOPE) || request.headers.has("Range")) return;
    // Bootstrap/manifest requests stay network-first, with a saved offline fallback.
    const path = decodeURIComponent(url.pathname.slice(new URL(SCOPE).pathname.length));
    if (path === "sw.js") return;
    event.respondWith((async () => {
        try {
            const manifestFile = ["ngeapp.json", "resources.index.txt", "preload-ignore.txt"].includes(path);
            if (!path || path === "index.html" || manifestFile) {
                try {
                    const response = await fetch(request, { cache: "no-cache" });
                    if (response.ok) return response;
                } catch (_) { /* Use the last complete manifest and cached launcher offline. */ }
                if (manifestFile) {
                    const cached = await (await getCache()).match(SNAPSHOT_KEY);
                    if (!cached) throw new Error("No saved preload manifest");
                    const snapshot = await cached.json();
                    if (path === "ngeapp.json") return new Response(JSON.stringify(snapshot.config), {
                        headers: { "Content-Type": "application/json" }
                    });
                    return new Response(path === "resources.index.txt" ? snapshot.index : snapshot.ignore);
                }
            }
            const state = await initialize();
            const entry = state.entries.get(path || "index.html");
            if (!entry || url.search) return fetch(request);
            const saved = await cachedResource(state, entry);
            return saved || await requestResource(state, entry, true);
        } catch (error) {
            console.warn("Resource request failed", path, error);
            return new Response("Resource unavailable", { status: 503 });
        }
    })());
});

self.addEventListener("message", event => {
    if (event.source?.type !== "window") return;
    if (event.data?.type === "start-preload") event.waitUntil(startPreload(event.data.config));
    else if (event.data?.type === "stop-preload") preloading = false;
});
self.addEventListener("install", event => event.waitUntil(self.skipWaiting()));
self.addEventListener("activate", event => event.waitUntil(self.clients.claim()));

function guessMime(path) {
    if (path.endsWith(".png")) return "image/png";
    if (path.endsWith(".jpg") || path.endsWith(".jpeg")) return "image/jpeg";
    if (path.endsWith(".webp")) return "image/webp";
    if (path.endsWith(".gif")) return "image/gif";
    if (path.endsWith(".svg")) return "image/svg+xml";
    if (path.endsWith(".js") || path.endsWith(".mjs")) return "application/javascript";
    if (path.endsWith(".css")) return "text/css";
    if (path.endsWith(".html") || path.endsWith(".htm")) return "text/html";
    if (path.endsWith(".json")) return "application/json";
    if (path.endsWith(".wasm")) return "application/wasm";
    if (path.endsWith(".mp3")) return "audio/mpeg";
    if (path.endsWith(".ogg")) return "audio/ogg";
    if (path.endsWith(".mp4")) return "video/mp4";
    if (path.endsWith(".ttf")) return "font/ttf";
    if (path.endsWith(".otf")) return "font/otf";
    if (path.endsWith(".woff")) return "font/woff";
    if (path.endsWith(".woff2")) return "font/woff2";
    return "application/octet-stream";
}
