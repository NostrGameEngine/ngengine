import test from 'node:test';
import assert from 'node:assert/strict';

import {
    createSharedEventWriter,
    startSharedEventReader
} from '../../main/resources/org/ngengine/web/SharedEvents.js';

test('shared resize events preserve the browser pixel ratio', async () => {
    const writer = createSharedEventWriter(1024);
    const received = [];
    const stop = startSharedEventReader(writer.buffer, (event, args) => {
        received.push({ event, args });
    });

    writer.writeEvent('resizeRenderTarget', [1280, 720, 1.5]);
    for (let attempt = 0; attempt < 20 && received.length === 0; attempt++) {
        await new Promise(resolve => setTimeout(resolve, 5));
    }
    stop();

    assert.deepEqual(received, [{
        event: 'resizeRenderTarget',
        args: [1280, 720, 1.5]
    }]);
});
