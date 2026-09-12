import test from 'node:test';
import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import vm from 'node:vm';

const source = readFileSync(new URL('../../main/resources/org/ngengine/web/WebBinds.js', import.meta.url), 'utf8');
const start = source.includes('let lastFullscreenRequest') ? source.indexOf('let lastFullscreenRequest')
    : source.indexOf('export const toggleFullscreen');
const toggleSource = source.slice(start, source.indexOf('export const togglePointerLock', start))
    .replace('export const toggleFullscreen =', 'globalThis.toggleFullscreen =');

test('unchanged fullscreen settings do not dispatch a control message every frame', () => {
    const events = [];
    const context = vm.createContext({ Binds: { fireEvent: (...args) => events.push(args) } });
    vm.runInContext(toggleSource, context);
    for (let frame = 0; frame < 144; frame++) context.toggleFullscreen(false);
    assert.deepEqual(events, [['toggleFullscreen', false]]);
    context.toggleFullscreen(true);
    context.toggleFullscreen(true);
    context.toggleFullscreen(false);
    assert.deepEqual(events, [
        ['toggleFullscreen', false], ['toggleFullscreen', true], ['toggleFullscreen', false]
    ]);
});
