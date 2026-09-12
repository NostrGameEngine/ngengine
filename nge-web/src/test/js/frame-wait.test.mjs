import test from 'node:test';
import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import vm from 'node:vm';

const source = readFileSync(new URL('../../main/resources/org/ngengine/web/WebBinds.js', import.meta.url), 'utf8');
const start = source.includes('const pendingFrameWaits') ? source.indexOf('const pendingFrameWaits')
    : source.indexOf('export const waitNextFrame');
const waitSource = source.slice(start, source.indexOf('// export const fireEventAsync', start))
    .replace('export const waitNextFrame =', 'globalThis.waitNextFrame =');

function fixture(nativeFrames = false) {
    const listeners = new Set();
    const otherListeners = new Map();
    const timers = new Map();
    let nextId = 0;
    let now = 0;
    const frames = new Map();
    let frameId = 0;
    const context = vm.createContext({
        console: { error() {}, warn() {} },
        performance: { now: () => now },
        Binds: {
            setNativeFrameWait() {},
            addEventListener: (name, callback) => name === 'render'
                ? listeners.add(callback) : otherListeners.set(name, callback),
            removeEventListener: (name, callback) => name === 'render'
                ? listeners.delete(callback) : otherListeners.delete(name)
        },
        setTimeout: (callback, delay) => { timers.set(++nextId, { callback, at: now + delay }); return nextId; },
        clearTimeout: id => timers.delete(id)
    });
    if (nativeFrames) {
        context.requestAnimationFrame = callback => {
            if (nativeFrames === 'unsupported') throw new Error('NotSupportedError');
            frames.set(++frameId, callback);
            return frameId;
        };
        context.cancelAnimationFrame = id => frames.delete(id);
    }
    vm.runInContext(waitSource, context);
    return { listeners, timers, frames, wait: context.waitNextFrame,
        event(name) { otherListeners.get(name)?.(); },
        nativeFrame() {
            const batch = [...frames.values()];
            frames.clear();
            for (const callback of batch) callback(now);
        },
        get installedTimers() { return nextId; },
        render() { for (const callback of [...listeners]) callback(); },
        advance(ms) {
            now += ms;
            for (const [id, timer] of [...timers]) {
                if (timer.at <= now) { timers.delete(id); timer.callback(); }
            }
        }
    };
}

test('render and fallback complete each wait exactly once and idle state is released', () => {
    const f = fixture();
    let wakes = 0;
    f.wait(() => wakes++);
    f.render();
    f.advance(100);
    assert.equal(wakes, 1);
    assert.equal(f.listeners.size, 0);
    assert.equal(f.timers.size, 0);
});

test('native animation frames wake directly without a front-end event listener', () => {
    const f = fixture(true);
    let wakes = 0;
    f.wait(() => wakes++);
    assert.equal(f.listeners.size, 0);
    assert.equal(f.frames.size, 1);
    f.render();
    assert.equal(wakes, 0);
    f.nativeFrame();
    assert.equal(wakes, 1);
    f.advance(100);
    assert.equal(wakes, 1);
});

test('native frame callbacks share one request and reentrant waits use the next frame', () => {
    const f = fixture(true);
    const wakes = [];
    f.wait(() => { wakes.push(1); f.wait(() => wakes.push(3)); });
    f.wait(() => wakes.push(2));
    assert.equal(f.frames.size, 1);
    f.nativeFrame();
    assert.deepEqual(wakes, [1, 2]);
    assert.equal(f.frames.size, 1);
    f.nativeFrame();
    assert.deepEqual(wakes, [1, 2, 3]);
});

test('native-frame watchdog cancels the obsolete request without a duplicate wake', () => {
    const f = fixture(true);
    let wakes = 0;
    f.wait(() => wakes++);
    f.advance(100);
    assert.equal(wakes, 1);
    assert.equal(f.frames.size, 0);
    f.wait(() => wakes++);
    f.nativeFrame();
    assert.equal(wakes, 2);
});

test('workers rejecting animation frames retain the front-end render fallback', () => {
    const f = fixture('unsupported');
    let wakes = 0;
    f.wait(() => wakes++);
    assert.equal(f.listeners.size, 1);
    f.render();
    assert.equal(wakes, 1);
});

test('context loss retains the watchdog and restoration resumes native frames', () => {
    const f = fixture(true);
    let wakes = 0;
    f.wait(() => { wakes++; f.wait(() => wakes++); });
    f.event('webglcontextlost');
    assert.equal(f.frames.size, 0);
    f.advance(100);
    assert.equal(wakes, 1);
    assert.equal(f.frames.size, 0);
    f.event('webglcontextrestored');
    assert.equal(f.frames.size, 1);
    f.nativeFrame();
    assert.equal(wakes, 2);
});

test('native watchdog expires only overdue waiters and rearms the remaining frame', () => {
    const f = fixture(true);
    const wakes = [];
    f.wait(() => wakes.push(1));
    f.advance(50);
    f.wait(() => wakes.push(2));
    f.advance(50);
    assert.deepEqual(wakes, [1]);
    assert.equal(f.frames.size, 1);
    f.nativeFrame();
    assert.deepEqual(wakes, [1, 2]);
    f.advance(100);
    assert.equal(f.frames.size, 0);
    assert.equal(f.timers.size, 0);
});

test('successive native frames reuse the watchdog and release idle state', () => {
    const f = fixture(true);
    for (let i = 0; i < 120; i++) {
        f.wait(() => {});
        f.nativeFrame();
        f.advance(7);
    }
    assert.ok(f.installedTimers < 12);
    f.advance(100);
    assert.equal(f.frames.size, 0);
    assert.equal(f.timers.size, 0);
});

test('fallback wakes after 100 ms without render events', () => {
    const f = fixture();
    let wakes = 0;
    f.wait(() => wakes++);
    f.advance(99);
    assert.equal(wakes, 0);
    f.advance(1);
    f.render();
    assert.equal(wakes, 1);
    assert.equal(f.listeners.size, 0);
    assert.equal(f.timers.size, 0);
});

test('successive frames reuse the watchdog instead of installing a timer per frame', () => {
    const f = fixture();
    for (let i = 0; i < 120; i++) {
        f.wait(() => {});
        f.render();
        f.advance(7);
    }
    assert.ok(f.installedTimers < 12, `Installed ${f.installedTimers} timers for 120 frames`);
    f.advance(100);
    assert.equal(f.listeners.size, 0);
    assert.equal(f.timers.size, 0);
});

test('a wait registered by a callback belongs to the next frame', () => {
    const f = fixture();
    let wakes = 0;
    f.wait(() => { wakes++; f.wait(() => wakes++); });
    f.render();
    assert.equal(wakes, 1);
    f.render();
    assert.equal(wakes, 2);
    f.advance(100);
    assert.equal(wakes, 2);
});

test('concurrent waiters keep their own timeout deadlines', () => {
    const f = fixture();
    const wakes = [];
    f.wait(() => wakes.push(1));
    f.advance(50);
    f.wait(() => wakes.push(2));
    f.advance(50);
    assert.deepEqual(wakes, [1]);
    f.advance(49);
    assert.deepEqual(wakes, [1]);
    f.advance(1);
    assert.deepEqual(wakes, [1, 2]);
    assert.equal(f.timers.size, 0);
    assert.equal(f.listeners.size, 0);
});

test('one callback throwing does not strand the other waiters', () => {
    const f = fixture();
    let wakes = 0;
    f.wait(() => { throw new Error('test'); });
    f.wait(() => wakes++);
    f.render();
    f.advance(100);
    assert.equal(wakes, 1);
    assert.equal(f.listeners.size, 0);
});

test('a timeout callback registering another wait does not delay older waiters', () => {
    const f = fixture();
    const wakes = [];
    f.wait(() => { wakes.push(1); f.wait(() => wakes.push(3)); });
    f.advance(50);
    f.wait(() => wakes.push(2));
    f.advance(50);
    assert.deepEqual(wakes, [1]);
    f.advance(50);
    assert.deepEqual(wakes, [1, 2]);
    f.advance(50);
    assert.deepEqual(wakes, [1, 2, 3]);
    assert.equal(f.timers.size, 0);
});
