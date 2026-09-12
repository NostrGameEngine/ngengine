import test from 'node:test';
import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import vm from 'node:vm';

const windowSource = readFileSync(new URL('../../main/resources/org/ngengine/web/Window.js', import.meta.url), 'utf8');
const bindsSource = readFileSync(new URL('../../main/resources/org/ngengine/web/WebBinds.js', import.meta.url), 'utf8');

test('window binding reloads the current page once for repeated restart requests', () => {
    const listeners = new Map();
    let reloads = 0;
    const context = vm.createContext({
        Binds: { addEventListener: (name, callback) => listeners.set(name, callback) },
        window: { location: { reload() { reloads++; } } },
        document: {},
        navigator: {},
        console,
        URL
    });
    vm.runInContext(windowSource.replace(/^import .*;\n/m, '')
        .replace('export default { bind, bindListeners };', 'globalThis.bind = bind;'), context);
    context.bind({ addEventListener() {} }, {});
    const reload = listeners.get('reloadPage');
    assert.equal(typeof reload, 'function');
    reload();
    reload();
    assert.equal(reloads, 1);
});

test('runtime requests page reload through the shared window bridge, including in a worker', () => {
    const events = [];
    const context = vm.createContext({ Binds: { fireEvent: name => events.push(name) } });
    const start = bindsSource.indexOf('export const reloadPage =');
    assert.notEqual(start, -1);
    const end = bindsSource.indexOf('\n}', start) + 2;
    vm.runInContext(bindsSource.slice(start, end)
        .replace('export const reloadPage =', 'globalThis.reloadPage ='), context);
    context.reloadPage();
    assert.deepEqual(events, ['reloadPage']);
});
