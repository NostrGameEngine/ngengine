import test from 'node:test';
import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import vm from 'node:vm';

const source = readFileSync(new URL('../../main/resources/splash.js', import.meta.url), 'utf8');

async function launcher(config, capacitor, hasToggle = true) {
    const toggle = { checked: false, disabled: true };
    let play;
    let launched;
    const button = { addEventListener: (event, callback) => { play = callback; } };
    const splash = { querySelector: selector => selector === '#useWebWorker'
        ? (hasToggle ? toggle : null) : button };
    const context = vm.createContext({
        console,
        window: { addEventListener() {} },
        document: { querySelector: () => splash },
        ...(capacitor ? { Capacitor: capacitor } : {})
    });
    vm.runInContext(source, context);
    context.loadConfig = async () => config;
    context.startPreloader = async () => {};
    context.updateProgress = () => {};
    context.ready = () => {};
    context.launchWebApp = (_splash, effective) => { launched = effective; };
    await context.main();
    return { toggle, play: () => { play(); return launched; } };
}

test('worker toggle follows app settings and launcher defaults', async () => {
    for (const [config, expected] of [
        [{}, true],
        [{ enable_web_worker: false }, false],
        [{ enable_web_worker: true }, true],
        [{ enableWebWorker: false, enable_web_worker: true }, false],
        [{ enableWebWorker: true, enable_web_worker: false }, true],
        [{ is_capacitor: true }, false],
        [{ is_capacitor: true, enable_web_worker: true }, true]
    ]) {
        const { toggle } = await launcher(config);
        assert.equal(toggle.checked, expected);
        assert.equal(toggle.disabled, false);
    }
    assert.equal((await launcher({}, { getPlatform: () => 'ios' })).toggle.checked, false);
    assert.equal((await launcher({}, { getPlatform: () => 'web' })).toggle.checked, true);
});

test('Play applies both override aliases without changing the original configuration', async () => {
    for (const original of [true, false]) {
        const config = Object.freeze({ enableWebWorker: original, enable_web_worker: original, FrameRate: 180 });
        const { toggle, play } = await launcher(config);
        toggle.checked = !original;
        const effective = play();
        assert.equal(effective.enableWebWorker, !original);
        assert.equal(effective.enable_web_worker, !original);
        assert.equal(effective.FrameRate, 180);
        assert.equal(config.enableWebWorker, original);
        assert.equal((await launcher(config)).toggle.checked, original);
    }
});

test('custom launch pages without a toggle retain their existing configuration', async () => {
    const config = { enable_web_worker: false };
    assert.equal((await launcher(config, undefined, false)).play(), config);
});
