import { createReadStream, statSync } from "node:fs";
import { createServer } from "node:http";
import { extname, resolve, sep } from "node:path";

const MIME_TYPES = {
    ".css": "text/css; charset=utf-8",
    ".gif": "image/gif",
    ".html": "text/html; charset=utf-8",
    ".ico": "image/x-icon",
    ".jpeg": "image/jpeg",
    ".jpg": "image/jpeg",
    ".js": "text/javascript; charset=utf-8",
    ".json": "application/json; charset=utf-8",
    ".ktx": "image/ktx",
    ".ktx2": "image/ktx2",
    ".mjs": "text/javascript; charset=utf-8",
    ".mp3": "audio/mpeg",
    ".ogg": "audio/ogg",
    ".png": "image/png",
    ".svg": "image/svg+xml",
    ".ttf": "font/ttf",
    ".wasm": "application/wasm",
    ".webp": "image/webp",
    ".woff": "font/woff",
    ".woff2": "font/woff2",
    ".zip": "application/zip"
};

function option(name, fallback) {
    const index = process.argv.indexOf(name);
    return index >= 0 && process.argv[index + 1] ? process.argv[index + 1] : fallback;
}

const host = option("--host", "0.0.0.0");
const port = Number(option("--port", "8000"));
const root = resolve(option("--root", process.cwd()));
const rootPrefix = root.endsWith(sep) ? root : root + sep;

function applyIsolationHeaders(response) {
    response.setHeader("Cross-Origin-Opener-Policy", "same-origin");
    response.setHeader("Cross-Origin-Embedder-Policy", "require-corp");
    response.setHeader("Cross-Origin-Resource-Policy", "same-origin");
    response.setHeader("Cache-Control", "no-cache");
}

function parseRange(rangeHeader, size) {
    if (!rangeHeader) return null;
    if (rangeHeader.includes(",")) return false;

    const match = /^bytes=(\d*)-(\d*)$/.exec(rangeHeader.trim());
    if (!match || (!match[1] && !match[2])) return false;

    let start;
    let end;
    if (!match[1]) {
        const suffixLength = Number(match[2]);
        if (!Number.isSafeInteger(suffixLength) || suffixLength <= 0) return false;
        start = Math.max(size - suffixLength, 0);
        end = size - 1;
    } else {
        start = Number(match[1]);
        end = match[2] ? Math.min(Number(match[2]), size - 1) : size - 1;
    }

    if (
        !Number.isSafeInteger(start)
        || !Number.isSafeInteger(end)
        || start < 0
        || start > end
        || start >= size
    ) {
        return false;
    }
    return { start, end };
}

const server = createServer((request, response) => {
    try {
        if (request.method !== "GET" && request.method !== "HEAD") {
            response.setHeader("Allow", "GET, HEAD");
            response.writeHead(405).end("Method not allowed");
            return;
        }
        const requestUrl = new URL(request.url || "/", `http://${request.headers.host || "localhost"}`);
        const decodedPath = decodeURIComponent(requestUrl.pathname);
        let file = resolve(root, "." + decodedPath);
        if (file !== root && !file.startsWith(rootPrefix)) {
            response.writeHead(403).end("Forbidden");
            return;
        }

        let info;
        try {
            info = statSync(file);
        } catch (_error) {
            response.writeHead(404).end("Not found");
            return;
        }
        if (info.isDirectory()) {
            file = resolve(file, "index.html");
            try {
                info = statSync(file);
            } catch (_error) {
                response.writeHead(404).end("Not found");
                return;
            }
        }
        if (!info.isFile()) {
            response.writeHead(404).end("Not found");
            return;
        }

        applyIsolationHeaders(response);
        response.setHeader("Content-Type", MIME_TYPES[extname(file).toLowerCase()] || "application/octet-stream");
        response.setHeader("Accept-Ranges", "bytes");
        const range = parseRange(request.headers.range, info.size);
        let start = 0;
        let end = info.size - 1;
        let status = 200;
        if (range === false) {
            response.setHeader("Content-Range", `bytes */${info.size}`);
            response.writeHead(416).end();
            return;
        }
        if (range) {
            start = range.start;
            end = range.end;
            status = 206;
            response.setHeader("Content-Range", `bytes ${start}-${end}/${info.size}`);
        }
        response.setHeader("Content-Length", end - start + 1);
        if (request.method === "HEAD") {
            response.writeHead(status).end();
            return;
        }
        response.writeHead(status);
        createReadStream(file, { start, end })
            .on("error", error => {
                console.error(error);
                if (!response.headersSent) response.writeHead(500);
                response.end();
            })
            .pipe(response);
    } catch (error) {
        console.error(error);
        response.writeHead(400).end("Bad request");
    }
});

server.listen(port, host, () => {
    console.log(`NGE web app listening on http://${host}:${port}`);
});
