import test from 'node:test';
import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import vm from 'node:vm';

const source = readFileSync(new URL('../../main/resources/org/ngengine/web/WebBindsHub.js', import.meta.url), 'utf8')
    .replace(/import \{[\s\S]*?from "\.\/SharedEvents.js";/, '')
    .replace('export default', 'globalThis.hub =');

test('native frame mode suppresses only that worker and notifications allocate no response timers', async () => {
    let timers = 0;
    const context = vm.createContext({ console, ArrayBuffer, MessagePort,
        setTimeout() { timers++; }, clearTimeout() {},
        canUseSharedEvents: () => true,
        createSharedEventWriter: () => ({ buffer: {}, writeEvent: () => false }),
        startSharedEventReader() {}
    });
    vm.runInContext(source, context);
    const worker = () => ({ messages: [], addEventListener(name, fn) { this.receive = fn; },
        postMessage(message) { this.messages.push(message); } });
    const a = worker(), b = worker();
    context.hub.registerWorker(a);
    context.hub.registerWorker(b);
    let localFrames = 0;
    context.hub.addEventListener('render', () => localFrames++);
    a.receive({ data: { type: 'frame-wait-mode', native: true } });
    context.hub.notify('render');
    assert.equal(localFrames, 1);
    assert.equal(a.messages.length, 1);
    assert.equal(b.messages.length, 2);
    assert.equal(b.messages[1].id, undefined);
    assert.equal(timers, 0);
    a.receive({ data: { type: 'frame-wait-mode', native: false } });
    context.hub.notify('render');
    assert.equal(a.messages.length, 2);
    assert.equal(b.messages.length, 3);
});

test('worker audio notifications keep message order and native mode is announced only on change', async () => {
    class WorkerGlobalScope {}
    const self = new WorkerGlobalScope();
    const sent = [];
    let receive, timers = 0;
    self.postMessage = message => sent.push(message);
    self.addEventListener = (name, listener) => { receive = listener; };
    const context = vm.createContext({ console, ArrayBuffer, MessagePort, WorkerGlobalScope, self,
        setTimeout() { return ++timers; }, clearTimeout() {} });
    vm.runInContext(source, context);
    const created = context.hub.fireEvent('createAudioSource', 1, 2);
    for (let i = 0; i < 1000; i++) context.hub.notify('setAudioVolume', 1, 2, i / 1000);
    assert.equal(sent[0].event, 'createAudioSource');
    assert.ok(sent.slice(1).every(message => message.id === undefined));
    assert.equal(timers, 1);
    receive({ data: { type: 'response', id: sent[0].id, result: true } });
    assert.equal(await created, true);
    context.hub.setNativeFrameWait(true);
    context.hub.setNativeFrameWait(true);
    context.hub.setNativeFrameWait(false);
    assert.deepEqual(sent.slice(-2).map(message => message.native), [true, false]);
});
