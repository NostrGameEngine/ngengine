import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import { runInNewContext } from 'node:vm';
import test from 'node:test';

const source = readFileSync(new URL('../../main/java/org/ngengine/web/WebPlatformInfo.java', import.meta.url), 'utf8');
const annotation = source.slice(source.indexOf('@JSBody'), source.indexOf('public static native'));
const script = [...annotation.matchAll(/"(?:[^"\\]|\\.)*"/g)].map(match => JSON.parse(match[0])).join('');
const mobile = globals => runInNewContext(`(function () { ${script} })()`, globals);

test('browser classification wins, including false on an Android user agent', () => {
    assert.equal(mobile({ navigator: { userAgentData: { mobile: false }, userAgent: 'Android Mobile' } }), false);
    assert.equal(mobile({ navigator: { userAgentData: { mobile: true }, userAgent: 'Windows' } }), true);
});

test('missing or non-boolean client hint falls back to the existing broad regex', () => {
    for (const ua of ['Android TV', 'iPhone', 'iPad', 'iPod', 'IEMobile', 'Opera Mini', 'Mobile']) {
        assert.equal(mobile({ navigator: { userAgent: ua } }), true, ua);
        assert.equal(mobile({ navigator: { userAgentData: { mobile: 'false' }, userAgent: ua } }), true, ua);
    }
});

test('small touch windows do not override a desktop user agent', () => {
    assert.equal(mobile({ navigator: { userAgent: 'Windows', maxTouchPoints: 10 },
        window: { innerWidth: 400, innerHeight: 300 }, matchMedia: () => ({ matches: true }) }), false);
});

test('works in workers and treats missing navigator as unknown', () => {
    assert.equal(mobile({ navigator: { userAgent: 'Android' } }), true);
    assert.equal(mobile({}), false);
    assert.equal(mobile({ navigator: {} }), false);
});
