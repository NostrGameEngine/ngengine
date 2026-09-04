importScripts("./zip-core.js");

// Force in-thread codecs in Service Worker (no sub-workers allowed)
if (globalThis.zip && typeof globalThis.zip.configure === "function") {
  globalThis.zip.configure({ useWebWorkers: false });
}

const USE_CACHE = true;
const INDEX_URL = "./resources.index.txt";
const PRELOAD_IGNORE_URL = "./preload-ignore.txt";
const CACHE_BASENAME = "ngeapp";
const CACHE_VERSION = "v1";
const PREFETCH_WORKERS = 5;
const HASHES_STORE = "__hashes__";
const BUNDLE_KEY = "bundle.zip";
const BUNDLE_META_KEY = "__bundle_meta__";
const BUNDLE_CHUNK_PREFIX = "__bundle_chunks__/";
const BUNDLE_CHUNK_SIZE = 1024 * 1024;
const BUNDLE_INITIAL_READ_AHEAD_CHUNKS = 4;
const BUNDLE_MIN_READ_AHEAD_CHUNKS = 1;
const BUNDLE_MAX_READ_AHEAD_CHUNKS = 8;
const BUNDLE_PROGRESS_INTERVAL_MS = 100;

const NON_CACHEABLE = [
    INDEX_URL,
    PRELOAD_IGNORE_URL,
    BUNDLE_KEY
];

let CACHE = null;
let HASHES = null;
let INDEX = null;
let PREFETCH = true;
let NUM_PREFETCHED = 0;
let NUM_ENTRIES_TO_PRELOAD = null;
let LAST_FETCHED = "";
let BYTES_TO_PRELOAD = 0;
let BYTES_PREFETCHED = 0;
let PRELOAD_IGNORE_LIST = null;

let bundledLoaded = false;

let bundleEntries = undefined;
let bundleOpenPromise = null;
let bundleBlob = null;
let bundleBlobReader = null;
let bundleZipReader = null;
let bundleUpdatePromise = null;
let bundleEntryMap = null;
let bundleMode = null;
let bundleMetadata = null;
const bundleChunkRequests = new Map();
const bundleInflightBytes = new Map();
const bundleEntryRequests = new Map();
const bundleEntryQueue = [];
let bundleCachedChunkIndexes = new Set();
let bundleCachedBytes = 0;
let bundleProgressHash = null;
let bundleReadAheadChunks = BUNDLE_INITIAL_READ_AHEAD_CHUNKS;
let bundleProgressLastUpdate = 0;
let bundlePreloadActive = false;
let COUNT_PROMISE = null;
let bundleEntryQueueRunning = false;

function closeBundleReaders() {
  try { bundleZipReader && bundleZipReader.close && bundleZipReader.close(); } catch(_) {}
  bundleZipReader = null;
  bundleBlobReader = null;
  bundleBlob = null;
  bundleEntries = undefined;
  bundleEntryMap = null;
  bundleMode = null;
  bundleMetadata = null;
  bundledLoaded = false;
}

function queueBundleEntryRead(entry, path, background) {
    const existing = bundleEntryRequests.get(path);
    if (existing) return existing;

    let resolveJob;
    let rejectJob;
    const promise = new Promise((resolve, reject) => {
        resolveJob = resolve;
        rejectJob = reject;
    });
    const job = { entry, path, resolve: resolveJob, reject: rejectJob };
    bundleEntryRequests.set(path, promise);

    if (background) {
        bundleEntryQueue.push(job);
    } else {
        // A foreground asset miss always runs before queued background work. The
        // current ZIP read is allowed to finish, avoiding an aborted range request;
        // requesting the same entry simply shares the in-flight promise above.
        const firstBackground = bundleEntryQueue.findIndex(queued => queued.background);
        job.background = false;
        if (firstBackground < 0) bundleEntryQueue.push(job);
        else bundleEntryQueue.splice(firstBackground, 0, job);
    }
    if (background) job.background = true;
    drainBundleEntryQueue();
    return promise;
}

async function drainBundleEntryQueue() {
    if (bundleEntryQueueRunning) return;
    bundleEntryQueueRunning = true;
    const Zip = await getZip();
    try {
        while (bundleEntryQueue.length) {
            const job = bundleEntryQueue.shift();
            try {
                const blob = await job.entry.getData(new Zip.BlobWriter());
                job.resolve(blob);
            } catch (error) {
                job.reject(error);
            } finally {
                bundleEntryRequests.delete(job.path);
            }
        }
    } finally {
        bundleEntryQueueRunning = false;
        if (bundleEntryQueue.length) drainBundleEntryQueue();
    }
}

async function getZip(){
   return globalThis.zip;
}

function bundleChunkUrl(hash, index) {
    const safeHash = encodeURIComponent(hash || "unversioned");
    return new URL(`${BUNDLE_CHUNK_PREFIX}${safeHash}/${index}`, self.registration.scope).href;
}

function bundleChunkPrefixUrl(hash) {
    const safeHash = encodeURIComponent(hash || "unversioned");
    return new URL(`${BUNDLE_CHUNK_PREFIX}${safeHash}/`, self.registration.scope).href;
}

function bundleChunkLength(metadata, index) {
    const start = index * metadata.chunkSize;
    return Math.max(0, Math.min(metadata.chunkSize, metadata.size - start));
}

function chooseInitialReadAheadChunks() {
    const downlink = Number(self.navigator?.connection?.downlink);
    if (!Number.isFinite(downlink) || downlink <= 0) {
        return BUNDLE_INITIAL_READ_AHEAD_CHUNKS;
    }
    if (downlink < 2) return 1;
    if (downlink < 8) return 2;
    if (downlink >= 25) return 8;
    return BUNDLE_INITIAL_READ_AHEAD_CHUNKS;
}

function tuneBundleReadAhead(byteLength, elapsedMs) {
    if (byteLength < BUNDLE_CHUNK_SIZE || elapsedMs <= 0) return;
    const bytesPerSecond = byteLength * 1000 / elapsedMs;
    if (elapsedMs < 900 && bytesPerSecond >= 6 * 1024 * 1024) {
        bundleReadAheadChunks = Math.min(
            BUNDLE_MAX_READ_AHEAD_CHUNKS,
            bundleReadAheadChunks * 2
        );
    } else if (elapsedMs > 2500 || bytesPerSecond < 1.5 * 1024 * 1024) {
        bundleReadAheadChunks = Math.max(
            BUNDLE_MIN_READ_AHEAD_CHUNKS,
            Math.ceil(bundleReadAheadChunks / 2)
        );
    }
}

function currentBundleTransferBytes(metadata) {
    let inflightBytes = 0;
    for (const bytes of bundleInflightBytes.values()) inflightBytes += bytes;
    return Math.min(metadata.size, bundleCachedBytes + inflightBytes);
}

async function reportBundleTransferProgress(metadata, force) {
    const now = Date.now();
    if (!force && now - bundleProgressLastUpdate < BUNDLE_PROGRESS_INTERVAL_MS) return;
    bundleProgressLastUpdate = now;

    await updateProgress(
        bundlePreloadActive ? LAST_FETCHED : "",
        bundlePreloadActive ? NUM_PREFETCHED : 0,
        bundlePreloadActive ? (NUM_ENTRIES_TO_PRELOAD || 1) : 1,
        currentBundleTransferBytes(metadata),
        metadata.size,
        bundlePreloadActive ? "Loading..." : "Preparing bundled resources...",
        true
    );
}

async function initializeBundleTransferProgress(metadata) {
    if (bundleProgressHash === metadata.hash) return;
    const cache = await getCache();
    const prefix = bundleChunkPrefixUrl(metadata.hash);
    const chunkCount = Math.ceil(metadata.size / metadata.chunkSize);
    const cachedIndexes = new Set();
    let cachedBytes = 0;

    for (const request of await cache.keys()) {
        if (!request.url.startsWith(prefix)) continue;
        const index = Number(request.url.substring(prefix.length));
        if (!Number.isSafeInteger(index) || index < 0 || index >= chunkCount) continue;
        if (cachedIndexes.has(index)) continue;
        cachedIndexes.add(index);
        cachedBytes += bundleChunkLength(metadata, index);
    }

    bundleProgressHash = metadata.hash;
    bundleCachedChunkIndexes = cachedIndexes;
    bundleCachedBytes = Math.min(metadata.size, cachedBytes);
    bundleInflightBytes.clear();
    bundleReadAheadChunks = chooseInitialReadAheadChunks();
    bundleProgressLastUpdate = 0;
}

async function readBundleMetadata() {
    try {
        const cache = await getCache();
        const response = await cache.match(BUNDLE_META_KEY);
        return response ? await response.json() : null;
    } catch (error) {
        console.warn("Failed to read bundle metadata", error);
        return null;
    }
}

async function writeBundleMetadata(metadata) {
    const cache = await getCache();
    await cache.put(BUNDLE_META_KEY, new Response(JSON.stringify(metadata), {
        headers: { "Content-Type": "application/json" }
    }));
}

async function clearBundleChunks() {
    const cache = await getCache();
    const requests = await cache.keys();
    await Promise.all(requests.map(request => {
        const relative = toScopeRelative(new URL(request.url).pathname);
        return relative.startsWith(BUNDLE_CHUNK_PREFIX)
            ? cache.delete(request)
            : Promise.resolve(false);
    }));
    bundleChunkRequests.clear();
    bundleInflightBytes.clear();
    bundleCachedChunkIndexes = new Set();
    bundleCachedBytes = 0;
    bundleProgressHash = null;
    bundleReadAheadChunks = BUNDLE_INITIAL_READ_AHEAD_CHUNKS;
    bundleProgressLastUpdate = 0;
}

async function probeBundleRange(url) {
    const absoluteUrl = new URL(url, self.location).href;
    try {
        const response = await fetch(absoluteUrl, {
            headers: { Range: "bytes=0-0" },
            cache: "no-store"
        });
        const contentRange = response.headers.get("content-range") || "";
        const match = /^bytes\s+0-0\/(\d+)$/i.exec(contentRange.trim());
        if (response.status !== 206 || !match) {
            try { await response.body?.cancel(); } catch (_) {}
            return { supported: false, url: absoluteUrl, size: null };
        }
        const size = Number(match[1]);
        const probe = new Uint8Array(await response.arrayBuffer());
        if (!Number.isSafeInteger(size) || size <= 0 || probe.length !== 1) {
            return { supported: false, url: absoluteUrl, size: null };
        }
        return { supported: true, url: absoluteUrl, size };
    } catch (error) {
        console.info("Bundle range requests are unavailable; using the full-download fallback.", error);
        return { supported: false, url: absoluteUrl, size: null };
    }
}

async function readBundleChunk(metadata, index) {
    const chunkCount = Math.ceil(metadata.size / metadata.chunkSize);
    if (!Number.isSafeInteger(index) || index < 0 || index >= chunkCount) {
        throw new RangeError(`Invalid bundle chunk index: ${index}`);
    }
    await initializeBundleTransferProgress(metadata);
    const requestKey = `${metadata.hash}:${index}`;
    const pending = bundleChunkRequests.get(requestKey);
    if (pending) return pending;

    const cache = await getCache();
    const chunkUrl = bundleChunkUrl(metadata.hash, index);
    const cached = await cache.match(chunkUrl);
    if (cached) {
        if (!bundleCachedChunkIndexes.has(index)) {
            bundleCachedChunkIndexes.add(index);
            bundleCachedBytes += bundleChunkLength(metadata, index);
        }
        return new Uint8Array(await cached.arrayBuffer());
    }

    const pendingAfterCache = bundleChunkRequests.get(requestKey);
    if (pendingAfterCache) return pendingAfterCache;

    const lastPossibleChunk = Math.min(chunkCount - 1, index + bundleReadAheadChunks - 1);
    const candidateIndexes = [];
    for (let candidate = index; candidate <= lastPossibleChunk; candidate++) {
        candidateIndexes.push(candidate);
    }
    const candidateCached = await Promise.all(candidateIndexes.map(candidate =>
        cache.match(bundleChunkUrl(metadata.hash, candidate))
    ));

    const pendingAfterLookup = bundleChunkRequests.get(requestKey);
    if (pendingAfterLookup) return pendingAfterLookup;
    if (candidateCached[0]) {
        if (!bundleCachedChunkIndexes.has(index)) {
            bundleCachedChunkIndexes.add(index);
            bundleCachedBytes += bundleChunkLength(metadata, index);
        }
        return new Uint8Array(await candidateCached[0].arrayBuffer());
    }

    let lastChunk = index;
    for (let offset = 1; offset < candidateIndexes.length; offset++) {
        const candidate = candidateIndexes[offset];
        if (candidateCached[offset] || bundleChunkRequests.has(`${metadata.hash}:${candidate}`)) break;
        lastChunk = candidate;
    }

    const claimedKeys = [];
    const operationId = `${requestKey}:${Date.now()}:${Math.random()}`;
    const operation = (async () => {
        const start = index * metadata.chunkSize;
        const end = Math.min((lastChunk + 1) * metadata.chunkSize, metadata.size) - 1;
        const startedAt = performance.now();
        const response = await fetch(metadata.url, {
            headers: { Range: `bytes=${start}-${end}` },
            cache: "no-store"
        });
        const contentRange = response.headers.get("content-range") || "";
        const expectedRange = `bytes ${start}-${end}/${metadata.size}`;
        if (response.status !== 206 || contentRange.trim().toLowerCase() !== expectedRange.toLowerCase()) {
            try { await response.body?.cancel(); } catch (_) {}
            throw new Error(`Bundle range request failed: ${response.status} ${contentRange}`);
        }
        const expectedLength = end - start + 1;
        const pieces = [];
        let received = 0;
        if (response.body) {
            const reader = response.body.getReader();
            while (true) {
                const result = await reader.read();
                if (result.done) break;
                if (!result.value || result.value.length === 0) continue;
                pieces.push(result.value);
                received += result.value.length;
                bundleInflightBytes.set(operationId, received);
                await reportBundleTransferProgress(metadata, false);
            }
        } else {
            const value = new Uint8Array(await response.arrayBuffer());
            pieces.push(value);
            received = value.length;
            bundleInflightBytes.set(operationId, received);
        }

        const data = new Uint8Array(received);
        let dataOffset = 0;
        for (const piece of pieces) {
            data.set(piece, dataOffset);
            dataOffset += piece.length;
        }
        if (data.length !== expectedLength) {
            throw new Error(`Bundle range ${index}-${lastChunk} is truncated: ${data.length}/${expectedLength}`);
        }

        const chunks = [];
        for (let chunk = index; chunk <= lastChunk; chunk++) {
            const relativeOffset = chunk * metadata.chunkSize - start;
            const length = bundleChunkLength(metadata, chunk);
            const chunkData = data.slice(relativeOffset, relativeOffset + length);
            await cache.put(bundleChunkUrl(metadata.hash, chunk), new Response(chunkData, {
                headers: {
                    "Content-Type": "application/octet-stream",
                    "Content-Length": String(chunkData.length)
                }
            }));
            chunks.push(chunkData);
        }

        bundleInflightBytes.delete(operationId);
        for (let chunk = index; chunk <= lastChunk; chunk++) {
            if (!bundleCachedChunkIndexes.has(chunk)) {
                bundleCachedChunkIndexes.add(chunk);
                bundleCachedBytes += bundleChunkLength(metadata, chunk);
            }
        }
        tuneBundleReadAhead(data.length, performance.now() - startedAt);
        return chunks;
    })();

    for (let chunk = index; chunk <= lastChunk; chunk++) {
        const key = `${metadata.hash}:${chunk}`;
        const chunkPromise = operation.then(chunks => chunks[chunk - index]);
        chunkPromise.catch(() => {});
        bundleChunkRequests.set(key, chunkPromise);
        claimedKeys.push([key, chunkPromise]);
    }
    operation.then(
        () => reportBundleTransferProgress(metadata, true),
        () => {
            bundleInflightBytes.delete(operationId);
            return reportBundleTransferProgress(metadata, true);
        }
    ).catch(error => console.warn("Failed to report bundle transfer progress", error))
        .finally(() => {
            for (const [key, promise] of claimedKeys) {
                if (bundleChunkRequests.get(key) === promise) bundleChunkRequests.delete(key);
            }
        });
    return bundleChunkRequests.get(requestKey);
}

function createCachedBundleRangeReader(Zip, metadata) {
    return new class extends Zip.Reader {
        constructor() {
            super();
            this.size = metadata.size;
        }

        async init() {
            super.init();
        }

        async readUint8Array(index, length) {
            if (index < 0 || length < 0 || index + length > metadata.size) {
                throw new RangeError(`Invalid bundle byte range: ${index}+${length}`);
            }
            if (length === 0) return new Uint8Array();

            const firstChunk = Math.floor(index / metadata.chunkSize);
            const lastChunk = Math.floor((index + length - 1) / metadata.chunkSize);
            const chunkPromises = [];
            for (let chunk = firstChunk; chunk <= lastChunk; chunk++) {
                chunkPromises.push(readBundleChunk(metadata, chunk));
            }
            const chunks = await Promise.all(chunkPromises);

            const result = new Uint8Array(length);
            let outputOffset = 0;
            for (let chunk = firstChunk; chunk <= lastChunk; chunk++) {
                const data = chunks[chunk - firstChunk];
                const chunkStart = chunk * metadata.chunkSize;
                const copyStart = Math.max(index, chunkStart);
                const copyEnd = Math.min(index + length, chunkStart + data.length);
                const sourceStart = copyStart - chunkStart;
                const copyLength = copyEnd - copyStart;
                result.set(data.subarray(sourceStart, sourceStart + copyLength), outputOffset);
                outputOffset += copyLength;
            }
            return result;
        }
    }();
}

async function openRangeBundle(metadata) {
    closeBundleReaders();
    await initializeBundleTransferProgress(metadata);
    const Zip = await getZip();
    bundleMode = "range";
    bundleMetadata = metadata;
    bundleBlobReader = createCachedBundleRangeReader(Zip, metadata);
    bundleZipReader = new Zip.ZipReader(bundleBlobReader);
    bundleEntries = await bundleZipReader.getEntries();
    bundleEntryMap = Object.create(null);
    for (const entry of bundleEntries) {
        bundleEntryMap[entry.filename] = entry;
    }
    bundledLoaded = true;
    return bundleEntries;
}

// Normalize a URL pathname to scope-relative path used by resources.index.txt
function toScopeRelative(pathname) {
    const base = new URL(self.registration.scope).pathname; // exact scope path
    let p = pathname || "/";
    if (p.startsWith(base)) p = p.slice(base.length);
    if (p.startsWith("/")) p = p.slice(1);
    return decodeURIComponent(p);
}

// Build normalized non-cacheable list (scope-relative)
const NON_CACHEABLE_REL = (() => {
    const keys = NON_CACHEABLE.concat([HASHES_STORE]);
    const out = [];
    for (const k of keys) {
        try {
            const p = new URL(k, self.location).pathname;
            out.push(toScopeRelative(p));
        } catch {
            out.push(String(k).replace(/^\.\//, ""));
        }
    }
    return out;
})();

function isNonCacheablePathname(pathname) {
    const rel = toScopeRelative(pathname);
    return NON_CACHEABLE_REL.includes(rel)
        || rel === BUNDLE_META_KEY
        || rel.startsWith(BUNDLE_CHUNK_PREFIX);
}

async function updateBundle(url, hash) {
    const cache = await getCache();
    const hashes = await getStoredHashes();
    const storedHash = hashes[BUNDLE_KEY];
    const cachedBundle = await cache.match(BUNDLE_KEY);
    const storedMetadata = await readBundleMetadata();
    const probe = await probeBundleRange(url);

    if (cachedBundle && hash && storedHash === hash) {
        console.log("Complete bundle already up-to-date");
        await cache.delete(BUNDLE_META_KEY);
        await clearBundleChunks();
        closeBundleReaders();
        bundledLoaded = true;
        return { mode: "blob" };
    }

    if (probe.supported) {
        const metadata = {
            version: 1,
            url: probe.url,
            hash: hash || `${probe.size}:${probe.url}`,
            size: probe.size,
            chunkSize: BUNDLE_CHUNK_SIZE
        };
        const metadataMatches = storedMetadata
            && storedMetadata.version === metadata.version
            && storedMetadata.url === metadata.url
            && storedMetadata.hash === metadata.hash
            && storedMetadata.size === metadata.size
            && storedMetadata.chunkSize === metadata.chunkSize;

        if (!metadataMatches) {
            closeBundleReaders();
            await cache.delete(BUNDLE_KEY);
            await cache.delete(BUNDLE_META_KEY);
            await clearBundleChunks();
            await writeBundleMetadata(metadata);
        }

        await initializeBundleTransferProgress(metadata);
        await updateProgress(
            "",
            0,
            1,
            currentBundleTransferBytes(metadata),
            probe.size,
            "Preparing bundled resources...",
            true
        );
        try {
            const entries = await openRangeBundle(metadata);
            if (!Array.isArray(entries) || entries.length === 0) {
                throw new Error("Invalid/empty ranged ZIP");
            }
            if (hash) await storeHash(BUNDLE_KEY, hash);
            console.log("Bundle opened with on-demand HTTP range loading");
            return { mode: "range" };
        } catch (error) {
            console.warn("On-demand bundle loading failed; downloading the complete ZIP instead.", error);
            closeBundleReaders();
            await cache.delete(BUNDLE_META_KEY);
            await clearBundleChunks();
        }
    }

    await updateProgress("", 0, 1, 0, probe.size, "Downloading bundled resources...", false);
    const response = await fetch(probe.url, { cache: "no-store" });
    if (!response.ok || !response.body) {
        throw new Error(`Failed to fetch bundle: ${response.status}`);
    }

    const totalBytes = Number(response.headers.get("content-length")) || null;
    const reader = response.body.getReader();
    let received = 0;
    let lastProgressUpdate = 0;
    const stream = new ReadableStream({
        async pull(controller) {
            try {
                const { done, value } = await reader.read();
                if (value && value.length) {
                    controller.enqueue(value);
                    received += value.length;
                    const now = Date.now();
                    if (done || now - lastProgressUpdate >= 100) {
                        lastProgressUpdate = now;
                        await updateProgress(
                            "",
                            0,
                            1,
                            received,
                            totalBytes,
                            "Downloading bundled resources...",
                            false
                        );
                    }
                }
                if (done) {
                    controller.close();
                    await updateProgress(
                        "",
                        0,
                        1,
                        received,
                        totalBytes,
                        "Downloading bundled resources...",
                        false
                    );
                }
            } catch (error) {
                controller.error(error);
            }
        },
        cancel() {
            try { reader.cancel(); } catch (_) {}
        }
    });

    await cache.put(BUNDLE_KEY, new Response(stream, {
        headers: { "Content-Type": "application/zip" }
    }));
    if (totalBytes != null && received !== totalBytes) {
        await cache.delete(BUNDLE_KEY);
        throw new Error(`Bundle truncated: got ${received} of ${totalBytes} bytes`);
    }

    const stored = await cache.match(BUNDLE_KEY);
    if (!stored) throw new Error("Bundle missing from cache after download");
    const blob = await stored.blob();
    const Zip = await getZip();
    try {
        const zipReader = new Zip.ZipReader(new Zip.BlobReader(blob));
        const entries = await zipReader.getEntries();
        await zipReader.close();
        if (!Array.isArray(entries) || entries.length === 0) {
            throw new Error("Invalid/empty ZIP");
        }
    } catch (error) {
        console.error("Bundle validation failed:", error);
        await cache.delete(BUNDLE_KEY);
        throw error;
    }

    closeBundleReaders();
    await cache.delete(BUNDLE_META_KEY);
    await clearBundleChunks();
    if (hash) await storeHash(BUNDLE_KEY, hash);
    bundledLoaded = true;
    console.log("Complete bundle downloaded and validated");
    return { mode: "blob" };
}

async function loadBundledResources() {
    if (bundleEntries) return bundleEntries;
    if (bundleOpenPromise) return bundleOpenPromise;

    // If an update is in flight, await it to avoid opening a half-written ZIP
    if (bundleUpdatePromise) {
        try { await bundleUpdatePromise; } catch (_) { /* ignored here; will fall back */ }
    }

    bundleOpenPromise = (async () => {
        const metadata = await readBundleMetadata();
        if (metadata) {
            try {
                return await openRangeBundle(metadata);
            } catch (error) {
                console.warn("Failed to reopen the ranged bundle", error);
                closeBundleReaders();
            }
        }

        const Zip = await getZip();
        const cache = await getCache();
        const resp = await cache.match(BUNDLE_KEY);
        if (!resp) {
            bundleEntries = [];
            bundleEntryMap = Object.create(null);
            return bundleEntries;
        }
        bundledLoaded = true;

        bundleBlob = await resp.blob();
        try {
            bundleBlobReader = new Zip.BlobReader(bundleBlob);
            bundleZipReader = new Zip.ZipReader(bundleBlobReader);
            bundleEntries = await bundleZipReader.getEntries();
            bundleEntryMap = Object.create(null);
            for (const e of bundleEntries) {
                bundleEntryMap[e.filename] = e;
            }
        } catch (e) {
            console.error("Failed to open bundle:", e);
            closeBundleReaders();
            bundleEntries = [];
            bundleEntryMap = Object.create(null);
        }
        return bundleEntries;
    })();

    try {
        return await bundleOpenPromise;
    } finally {
        bundleOpenPromise = null;
    }
}

async function getContent(url, background = false) {
    const entries = await loadBundledResources();
    let path;
    if (entries && entries.length) {
        path = toScopeRelative(new URL(url, self.location).pathname);

        const entry = bundleEntryMap ? bundleEntryMap[path] : undefined;
        if (entry) {
            const blob = await queueBundleEntryRead(entry, path, background);
            return new Response(blob, { headers: { "Content-Type": guessMime(path) } });
        }
    }
    return fetch(url, { cache: "no-cache" });
}

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

async function loadConfig(url) {
    return fetch(url).then(r => r.json()).catch(e => {
        console.warn("Failed to load config", e);
        return {};
    });
}

async function getCache() {
    if (CACHE) return CACHE;
    const origin = self.location.origin;
    const scope = self.registration.scope;
    const input = origin + scope;

    const encoder = new TextEncoder();
    const data = encoder.encode(input);
    const digest = await crypto.subtle.digest("SHA-256", data);

    let hex = "";
    const bytes = new Uint8Array(digest);
    for (let i = 0; i < bytes.length; i++) {
        hex += bytes[i].toString(16).padStart(2, "0");
    }

    const shortHash = hex.substring(0, 16);
    const scopedName = `${CACHE_BASENAME}-${shortHash}-${CACHE_VERSION}`;

    CACHE = await caches.open(scopedName);
    if (!CACHE) throw new Error("Failed to open cache");
    return CACHE;
}

async function getStoredHashes() {
    if (HASHES) return HASHES;
    try {
        const cache = await getCache();
        const resp = await cache.match(HASHES_STORE);
        if (!resp) {
            HASHES = {};
        }
        else HASHES = await resp.json();
        if (!HASHES) throw new Error("No hashes");
    } catch (e) {
        console.warn(e);
        HASHES = {};
    }
    return HASHES;
}

async function storeHash(url, hash) {
    const hashes = await getStoredHashes();
    hashes[url] = hash;
    try {
        const cache = await getCache();
        await cache.put(
            HASHES_STORE,
            new Response(JSON.stringify(hashes), {
                headers: { "Content-Type": "application/json" }
            })
        );
    } catch (e) {
        console.warn("Failed to store hashes", e);
    }
}

async function canPreload(path) {
    if (!PRELOAD_IGNORE_LIST) {
        try {
            const resp = await getContent(PRELOAD_IGNORE_URL);
            if (!resp.ok) throw new Error("Failed to fetch preload ignore list: " + resp.status);
            const text = await resp.text();
            const lines = text.trim().split("\n");
            const entries = new Set();
            for (let i = 0; i < lines.length; i++) {
                const line = lines[i].trim();
                if (line) entries.add(line);
            }
            PRELOAD_IGNORE_LIST = entries;
        } catch (e) {
            console.warn("Failed to fetch preload ignore list", e);
            PRELOAD_IGNORE_LIST = new Set();
        }
    }
    if (path.startsWith("/")) path = path.substring(1);

    for (const ignore of PRELOAD_IGNORE_LIST) {
        if (path.startsWith(ignore)) return false;
    }
    return true;
}

async function getIndex() {
    if (INDEX) return INDEX;
    try {
        const resp = await getContent(INDEX_URL);
        if (!resp.ok) throw new Error("Failed to fetch index: " + resp.status);
        const text = await resp.text();
        const lines = text.trim().split("\n");
        const entries = {};
        for (let i = 0; i < lines.length; i++) {
            const parts = lines[i].split(/\s+/);
            if (parts.length < 3) continue;
            const hash = parts[0];
            const size = parseInt(parts[1]);
            const path = parts.slice(2).join(" ");
            entries[path] = { hash, size, path };
        }
        INDEX = entries;
    } catch (e) {
        console.warn("Failed to fetch index", e);
        INDEX = {};
    }
    return INDEX;
}

async function getIndexEntry(urlOrPath) {
    let pathname;
    try {
        pathname = new URL(urlOrPath, self.location.origin).pathname;
    } catch (_) {
        pathname = String(urlOrPath || "/");
    }
    const rel = toScopeRelative(pathname);
    const index = await getIndex();
    return index[rel];
}

async function countPreloadEntries() {
    if (NUM_ENTRIES_TO_PRELOAD != null && BYTES_TO_PRELOAD != null && NUM_ENTRIES_TO_PRELOAD !== 0) {
        return [NUM_ENTRIES_TO_PRELOAD, BYTES_TO_PRELOAD];
    }
    if (COUNT_PROMISE) return COUNT_PROMISE;

    COUNT_PROMISE = (async () => {
        const index = await getIndex();
        let count = 0;
        let bytes = 0;
        for (const path in index) {
            if (await canPreload(path)) {
                count++;
                bytes += index[path].size;
            }
        }
        NUM_ENTRIES_TO_PRELOAD = count;
        BYTES_TO_PRELOAD = bytes;
        return [count, bytes];
    })();

    try {
        return await COUNT_PROMISE;
    } finally {
        COUNT_PROMISE = null;
    }
}

async function getFromCache(url) {
    if (!USE_CACHE) return null;

    // skip non-cacheable by pathname (normalized)
    const pathname = new URL(url, self.location.origin).pathname;
    if (isNonCacheablePathname(pathname)) return null;

    // check if cached
    const hashes = await getStoredHashes();
    const cachedHash = hashes[url];
    if (!cachedHash) {
        return null;
    }

    // check if still valid (compare with index hash)
    const entry = await getIndexEntry(url);
    const hash = entry?.hash;
    if (hash !== cachedHash) {
        return null;
    }

    // return from cache
    try {
        const cache = await getCache();
        const resp = await cache.match(url);
        if (!resp || !resp.ok) return null;
        return resp;
    } catch (e) {
        console.warn("Failed to get from cache", e);
        return null;
    }
}

async function storeInCache(url, response, hash) {
    try {
        const cache = await getCache();
        await cache.put(url, response);
        await storeHash(url, hash);
    } catch (e) {
        console.warn("Failed to store in cache", e);
    }
}

async function fetchAndCache(request, awaitCaching = false, background = false) {
    const pathname = new URL(request.url, self.location.origin).pathname;
    if (isNonCacheablePathname(pathname)) {
        return await fetch(request, { cache: "no-cache" });
    }

    // Serve from bundle if present; else network
    const resp = await getContent(request.url, background);
    if (resp.ok) {
        const cachable = resp.clone();
        LAST_FETCHED = new URL(request.url).pathname;

        // Use hash from index (no SHA-256 generation)
        const entry = await getIndexEntry(request.url);
        let c = Promise.resolve();
        if (entry && entry.hash) {
            c = storeInCache(request.url, cachable, entry.hash)
                .catch(e => console.warn("Failed to store in cache", e));
        }
        if (awaitCaching) await c;
    }
    return resp;
}

// intercept fetch requests
// return cached value if available and up to date
// otherwise fetch and cache
self.addEventListener("fetch", (event) => {
    event.respondWith((async () => {
        const req = event.request;
        const url = new URL(req.url);
        if (url.origin !== self.location.origin || (req.method !== "GET" && req.method !== "HEAD")) {
            return fetch(req);
        }

        const cached = await getFromCache(req.url);
        if (cached) return cached;

        try {
            return await fetchAndCache(req);
        } catch (err) {
            return new Response("Offline", { status: 503 });
        }
    })());
});

async function prefetchResources() {
    if (!PREFETCH) return;
    console.log("Starting prefetch...");
    const index = await getIndex();

    const entries = Object.values(index);
    if (entries.length === 0) return;

    let i = 0;

    async function worker() {
        while (i < entries.length) {
            const entry = entries[i++];
            if (!await canPreload(entry.path)) continue;
            try {
                if (!PREFETCH) break;
                const url = new URL(entry.path, self.location).href;
                const cached = await getFromCache(url);
                if (!cached) {
                    const req = new Request(url);
                    const resp = await fetchAndCache(req, true, true);
                    if (!resp.ok) throw new Error("Failed to fetch: " + resp.status);
                } else {
                }

            } catch (e) {
                console.warn(e);
            } finally {
                BYTES_PREFETCHED += entry.size;
                NUM_PREFETCHED++;
                try {
                    const [maxEntries, maxBytes] = await countPreloadEntries();
                    if (NUM_PREFETCHED >= maxEntries) NUM_PREFETCHED = maxEntries;
                    if (BYTES_PREFETCHED >= maxBytes) BYTES_PREFETCHED = maxBytes;
                } catch (e) {
                    console.warn(e);
                }

            }
        }
    }

    const workers = [];
    // A ranged ZIP uses one sequential background extractor. This prevents
    // decompression/range bursts from competing with the running game; cache
    // misses from fetch events are inserted ahead of this queue.
    const workerCount = bundleMode === "range" ? 1 : PREFETCH_WORKERS;
    for (let w = 0; w < workerCount; w++) {
        workers.push(worker());
    }
    await Promise.all(workers);
}

async function stopPreload() {
    PREFETCH = false;
    console.log("Stopping preload as requested by client");
}

async function updateProgress(
    lastFile,
    done,
    total,
    doneBytes,
    totalBytes,
    status,
    canSkip
) {
    const clients = await self.clients.matchAll({ includeUncontrolled: true, type: 'window' });
    for (const client of clients) {
        client.postMessage({
            type: "preload-progress",
            total: total,
            done: done,
            last: lastFile,
            totalBytes: totalBytes,
            doneBytes: doneBytes,
            status: status,
            canSkip: canSkip
        });
    }
}

let updating = false;
let updateTask = null;
async function updateClient(start) {
    const update = async (force) =>{
        try {
            if(!force&&!updating) return;
            const [maxEntries, maxBytes] = await countPreloadEntries();
            if(!force&&!updating) return;
            const reportBundleBytes = bundleMode === "range" && bundleMetadata;
            await updateProgress(
                LAST_FETCHED,
                NUM_PREFETCHED,
                maxEntries,
                reportBundleBytes ? currentBundleTransferBytes(bundleMetadata) : BYTES_PREFETCHED,
                reportBundleBytes ? bundleMetadata.size : maxBytes,
                "Loading...",
                true
            );
        } catch (e) {
            console.warn(e);
        }
    };
    
    if (start) {
        if(updating) return;
        updating = true;
        const updateLoop = async () => {
            updateTask = new Promise((res,rej)=>{
                update().then(res).catch(rej);
            });
            await updateTask;
            if(!updating) return;
            setTimeout(()=>{
                if (updating) {
                    updateLoop().catch((e)=>{
                        console.warn("Update loop failed", e);
                    });
                }
            },200);
        };
        updateLoop();              
    } else {
        if (!updating) return;
        updating = false;
        if (updateTask) await updateTask;
        await update(true);

    }
}

async function startPreload(configUrl) {
    PREFETCH = true;
    bundlePreloadActive = false;
    INDEX = null;
    NUM_PREFETCHED = 0;
    BYTES_PREFETCHED = 0;
    LAST_FETCHED = "";
    NUM_ENTRIES_TO_PRELOAD = null;
    BYTES_TO_PRELOAD = 0;
    PRELOAD_IGNORE_LIST = null;
    COUNT_PROMISE = null;

    const config = await loadConfig(configUrl);
    if (config.bundle) {
        console.log("Download app bundle");
        bundleUpdatePromise = updateBundle(config.bundle, config.bundleHash)
            .catch(e => { throw e; })
            .finally(() => { bundleUpdatePromise = null; });
        await bundleUpdatePromise;
    }

    const shouldPrefetch = config.preloadResources !== false && config.preload_resources !== false;

    const cache = await getCache();
    const index = await getIndex();
    const requests = await cache.keys();
    for (const request of requests) {
        const url = request.url;
        const pathname = new URL(url).pathname;

        // Skip system entries (normalized)
        if (isNonCacheablePathname(pathname)) continue;

        // If not in index, delete from cache
        const relPath = toScopeRelative(pathname);
        if (!index[relPath]) {
            await cache.delete(request);
            console.log("Clearing stale cache for:", relPath);
        }
    }

    if (!shouldPrefetch) {
        PREFETCH = false;
        await updateProgress("", 0, 0, 0, 0, "Ready", true);
        return;
    }

    bundlePreloadActive = true;
    await updateClient(true);
    try {
        await prefetchResources();
    } finally {
        await updateClient(false); // final update
        bundlePreloadActive = false;
    }
}

self.addEventListener("message", (event) => {
    event.waitUntil((async () => {
        try {
            const data = event.data || {};
            const src = event.source;
            const isWindow = src && (src.type === "window");
            if (isWindow && data.type === "stop-preload") {
                await stopPreload();
            } else if (isWindow && data.type === "start-preload") {
                await startPreload(data.config);
            } else {
                console.warn("Unknown message to service worker:", data);
            }
        } catch (e) {
            console.warn("Failed to process message", e);
        }
    })());
});

// starts prefetching
self.addEventListener("install", (event) => {
    event.waitUntil(self.skipWaiting());
});

self.addEventListener("activate", (event) => {
    event.waitUntil((async () => {
        await self.clients.claim();

        try {
            const cache = await getCache();
            const resp = await cache.match(BUNDLE_KEY);
            const metadata = await readBundleMetadata();
            if (resp || metadata) {
                bundledLoaded = true;
                // Warm up entries (do not block activation)
                loadBundledResources().catch(() => {});
            }
        } catch (e) {
            // ignore
        }
    })());
});
