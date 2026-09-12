import { readFileSync } from 'node:fs';
import vm from 'node:vm';
import assert from 'node:assert/strict';
import { test } from 'node:test';

// Keep ended events queued after stop(), as in Web Audio across a worker bridge.
function renderer() {
    const listeners = new Map();
    const events = [];
    const nodes = [];
    class Node {
        playbackRate = { value: 1 };
        gain = { value: 1 };
        connect() {}
        disconnect() {}
        setOrientation() {}
        setPosition() {}
        start() { this.started = true; }
        stop() { assert.ok(this.started, 'Cannot stop an unstarted source'); }
        addEventListener(name, callback) { this.ended = callback; }
    }
    class Context {
        currentTime = 0;
        destination = {};
        createConvolver() { return new Node(); }
        createPanner() { return new Node(); }
        createGain() { return new Node(); }
        createBufferSource() { const node = new Node(); nodes.push(node); return node; }
        close() {}
    }
    const source = readFileSync(new URL('../../main/resources/org/ngengine/web/AudioRenderer.js', import.meta.url), 'utf8')
        .replace('import Binds from "./WebBindsHub.js";', '')
        .replace('export default {', 'globalThis.renderer = {');
    const context = vm.createContext({
        window: { AudioContext: Context },
        Binds: {
            addEventListener: (name, callback) => listeners.set(name, callback),
            fireEvent: (...args) => { events.push(args); return Promise.resolve(); }
        }
    });
    vm.runInContext(source, context);
    context.renderer.bind();
    const call = (name, ...args) => listeners.get(name)(...args);
    call('createAudioContext', 44100, 1);
    call('createAudioSource', 1, 2);
    return { call, events, nodes };
}

test('natural completion emits one ended notification', () => {
    const { call, events, nodes } = renderer();
    call('playAudioSource', 1, 2);
    nodes[0].ended();
    assert.deepEqual(events, [['audioSourceEnded', 1, 2]]);
});

for (const action of ['stopAudioSource', 'pauseAudioSource', 'freeAudioSource', 'freeAudioContext']) {
    test(`queued ended event after ${action} is ignored`, () => {
        const { call, events, nodes } = renderer();
        call('playAudioSource', 1, 2);
        call(action, 1, 2);
        nodes[0].ended();
        assert.deepEqual(events, []);
    });
}

test('context cleanup accepts sources that never started', () => {
    const { call } = renderer();
    call('freeAudioContext', 1);
});
