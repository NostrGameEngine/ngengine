import test from 'node:test';
import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import vm from 'node:vm';
import { webcrypto } from 'node:crypto';

const sw = readFileSync(new URL('../../main/resources/sw.js', import.meta.url), 'utf8');
const library = readFileSync(new URL('../../main/resources/zip-core.js', import.meta.url), 'utf8');
const scope = 'https://game.test/play/';
const cachedBodies = new WeakMap();

async function fixture({ range = true, cache = new Map(), archive, files, corrupt = false, zip64 = false, level = 0,
        failCacheFor } = {}) {
    const messages = [], requests = [], handlers = {}, intervals = new Set();
    const key = input => new URL(typeof input === 'string' ? input : input.url, scope).href;
    const storage = {
        async keys() { return [...cache.keys()].map(url => new Request(url)); },
        async match(input) {
            const saved = cache.get(key(input));
            if (!saved) return undefined;
            const bytes = cachedBodies.get(saved);
            return bytes ? new Response(bytes, { headers: saved.headers, status: saved.status }) : saved.clone();
        },
        async put(input, response) {
            if (key(input).endsWith(failCacheFor || '\0')) throw new Error('Simulated quota failure');
            const bytes = await response.arrayBuffer();
            const saved = new Response(bytes, { headers: response.headers, status: response.status });
            cachedBodies.set(saved, bytes);
            cache.set(key(input), saved);
        },
        async delete(input) { return cache.delete(key(input)); }
    };
    const context = vm.createContext({ console, URL, Response, Request, Headers, Blob, TextEncoder, TextDecoder,
        Uint8Array, Uint16Array, Uint32Array, DataView, ArrayBuffer, ReadableStream, WritableStream,
        TransformStream, CompressionStream, DecompressionStream, crypto: webcrypto, btoa,
        setTimeout, clearTimeout,
        setInterval(fn) { intervals.add(fn); return fn; }, clearInterval(fn) { intervals.delete(fn); },
        caches: { open: async () => storage },
        self: { location: new URL(scope + 'sw.js'), registration: { scope },
            clients: { matchAll: async () => [{ postMessage: message => messages.push(message) }], claim() {} },
            addEventListener: (name, fn) => { handlers[name] = fn; }, skipWaiting() {} },
        importScripts() {}
    });
    vm.runInContext(library, context);
    const zip = context.zip;
    zip.configure({ useWebWorkers: false });
    files ||= new Map([['large.bin', new Uint8Array(1200000).fill(17)],
        ['second.txt', new TextEncoder().encode('second resource')],
        ['third.txt', new TextEncoder().encode('third resource')]]);
    if (!archive) {
        const writer = new zip.ZipWriter(new zip.Uint8ArrayWriter(), { useWebWorkers: false, zip64 });
        for (const [name, bytes] of files) await writer.add(name, new zip.Uint8ArrayReader(bytes), { level });
        archive = await writer.close(new TextEncoder().encode('A ZIP with a comment'));
    }
    const parsed = await new zip.ZipReader(new zip.Uint8ArrayReader(archive)).getEntries();
    const index = [...files].map(([name, bytes]) => `${name}-hash ${bytes.length} ${name}`).join('\n');
    context.fetch = async (input, options = {}) => {
        const url = key(input), requestedRange = options.headers?.Range;
        requests.push({ url, range: requestedRange });
        if (url.endsWith('ngeapp.json')) return Response.json({ bundle: 'app.zip', bundleHash: 'bundle-v1' });
        if (url.endsWith('resources.index.txt')) return new Response(index);
        if (url.endsWith('preload-ignore.txt')) return new Response('');
        if (url.endsWith('app.zip')) {
            if (!range) return new Response(archive, { headers: { 'Content-Length': archive.length } });
            const [, first, last] = /^bytes=(\d+)-(\d+)$/.exec(requestedRange);
            const start = Number(first), end = Number(last);
            const data = archive.slice(start, end + 1);
            return new Response(corrupt && start === parsed[0].offset && end > 0 ? data.slice(1) : data,
                { status: 206, headers: { 'Content-Range': `bytes ${start}-${end}/${archive.length}` } });
        }
        return new Response('missing', { status: 404 });
    };
    vm.runInContext(sw, context);
    return { context, cache, archive, files, parsed, messages, requests, intervals,
        start: () => context.startPreload('ngeapp.json'),
        state: () => context.initialize(),
        resourceRequests: () => requests.filter(r => r.range && parsed.some(e =>
            r.range.startsWith(`bytes=${e.offset}-`) && r.range !== 'bytes=0-0'
                && r.range !== `bytes=0-${archive.length - 1}`)) };
}

for (const zip64 of [false, true]) test(`one complete Range per resource, ZIP64=${zip64}`, async () => {
    const f = await fixture({ zip64 });
    await f.start();
    assert.equal(f.messages.at(-1).complete, true);
    assert.equal(f.resourceRequests().length, f.files.size);
    assert.equal(f.intervals.size, 0);
    for (const [name, bytes] of f.files) {
        assert.deepEqual(new Uint8Array(await f.cache.get(scope + name).clone().arrayBuffer()), bytes);
    }
    assert.ok(f.resourceRequests().some(r => Number(r.range.split('-')[1]) > 1048576));
});

test('complete reload reuses extracted cache without reading the ZIP', async () => {
    const first = await fixture();
    await first.start();
    const next = await fixture(first);
    await next.start();
    assert.equal(next.requests.filter(r => r.url.endsWith('app.zip')).length, 0);
    assert.equal(next.messages.at(-1).complete, true);
});

test('deflated resources use the library codec and CRC validation', async () => {
    const f = await fixture({ level: 6 });
    await f.start();
    assert.equal(f.messages.at(-1).complete, true);
    assert.equal(f.resourceRequests().length, f.files.size);
    assert.deepEqual(new Uint8Array(await f.cache.get(scope + 'large.bin').clone().arrayBuffer()),
        f.files.get('large.bin'));
});

test('partial reload fetches only the missing resource and reuses the ZIP directory', async () => {
    const first = await fixture();
    await first.start();
    first.cache.delete(scope + 'second.txt');
    const next = await fixture(first);
    await next.start();
    assert.equal(next.resourceRequests().length, 1);
    assert.equal(next.requests.filter(r => r.url.endsWith('app.zip')).length, 1);
    assert.equal(next.messages.at(-1).complete, true);
});

test('server ignoring Range uses the probe body once, then extracts locally', async () => {
    const f = await fixture({ range: false });
    await f.start();
    assert.equal(f.requests.filter(r => r.url.endsWith('app.zip')).length, 1);
    assert.equal(f.messages.at(-1).complete, true);
    const download = f.messages.filter(m => m.status === 'Downloading bundled resources...');
    assert.ok(download.length);
    assert.ok(download.every(m => !m.complete && !m.canSkip));
});

test('a partial full-download cache resumes extraction without fetching the ZIP again', async () => {
    const first = await fixture({ range: false });
    await first.start();
    assert.equal(first.cache.get(scope + 'bundle.zip').bodyUsed, false, 'stored ZIP remains reusable');
    first.cache.delete(scope + 'second.txt');
    const next = await fixture(first);
    await next.start();
    assert.equal(next.requests.filter(r => r.url.endsWith('app.zip')).length, 0);
    assert.equal(next.messages.at(-1).complete, true);
});

test('legacy extracted resources survive the upgrade while obsolete megabyte chunks are removed', async () => {
    const first = await fixture();
    await first.start();
    const hashes = {};
    for (const [name, bytes] of first.files) {
        first.cache.set(scope + name, new Response(bytes));
        hashes[scope + name] = name + '-hash';
    }
    first.cache.set(scope + '__hashes__', Response.json(hashes));
    first.cache.set(scope + '__bundle_chunks__/old/0', new Response('obsolete chunk'));
    const next = await fixture(first);
    await next.start();
    assert.equal(next.requests.filter(r => r.url.endsWith('app.zip')).length, 0);
    assert.equal(next.cache.has(scope + '__bundle_chunks__/old/0'), false);
    assert.equal(next.messages.at(-1).complete, true);
});

test('cache write failure does not count as a prepared resource', async () => {
    const f = await fixture({ failCacheFor: 'second.txt' });
    await f.start();
    assert.equal(f.messages.at(-1).complete, false);
    assert.equal(f.messages.at(-1).done, 2);
    assert.equal(f.cache.has(scope + 'second.txt'), false);
});

test('a truncated resource is never cached or reported Ready', async () => {
    const f = await fixture({ corrupt: true });
    await f.start();
    assert.equal(f.cache.has(scope + 'large.bin'), false);
    assert.equal(f.messages.at(-1).complete, false);
    assert.equal(f.messages.at(-1).done, 2);
    assert.ok(f.messages.at(-1).error);
    assert.equal(f.intervals.size, 0);
});

test('foreground request has reserved capacity, pauses new background work and deduplicates', async () => {
    const f = await fixture();
    const state = await f.state();
    const started = [], finish = new Map();
    f.context.loadResource = (s, entry) => {
        started.push(entry.path);
        return new Promise(resolve => finish.set(entry.path, () => resolve(new Response(entry.path))));
    };
    const request = (name, urgent = false) => f.context.requestResource(state, { path: name }, urgent);
    const a = request('a'), b = request('b'), c = request('c'), d = request('d');
    const urgent = request('urgent', true), same = request('urgent', true);
    assert.deepEqual(started, ['a', 'b', 'c', 'urgent']);
    finish.get('a')(); await a;
    assert.equal(started.includes('d'), false);
    finish.get('urgent')();
    assert.equal(await (await urgent).text(), 'urgent');
    assert.equal(await (await same).text(), 'urgent');
    assert.equal(started.filter(name => name === 'urgent').length, 1);
    assert.ok(started.includes('d'));
    for (const name of ['b', 'c', 'd']) finish.get(name)();
    await Promise.all([b, c, d]);
});

test('an urgent request promotes an already queued background job', async () => {
    const f = await fixture();
    const state = await f.state();
    const started = [], finish = new Map();
    f.context.loadResource = (s, entry) => {
        started.push(entry.path);
        return new Promise(resolve => finish.set(entry.path, () => resolve(new Response(entry.path))));
    };
    const request = (name, urgent = false) => f.context.requestResource(state, { path: name }, urgent);
    const jobs = ['a', 'b', 'c', 'd', 'e'].map(name => request(name));
    const urgent = request('e', true);
    assert.deepEqual(started, ['a', 'b', 'c', 'e']);
    finish.get('e')(); await urgent;
    finish.get('a')(); await jobs[0];
    assert.ok(started.includes('d'));
    for (const name of ['b', 'c', 'd']) finish.get(name)();
    await Promise.all(jobs);
    assert.equal(started.filter(name => name === 'e').length, 1);
});
