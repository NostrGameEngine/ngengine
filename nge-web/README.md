# NGE Web runtime

The web launcher prefers TeaVM Wasm GC and can fall back to the JavaScript
backend. Configure it in `ngeapp.json`:

```json
{
  "web_runtime": "wasm-gc",
  "web_javascript_fallback": true,
  "enable_web_worker": true
}
```

`enable_web_worker` defaults to `true` outside Capacitor. When enabled, the
launcher runs the game on a module Web Worker only if the browser supports the
Worker API, `OffscreenCanvas`, canvas transfer, `SharedArrayBuffer`, `Atomics`,
and cross-origin isolation. High-frequency render, resize, input, and one-way
audio state events then use a bidirectional shared-memory transport. Calls that
require a result still use worker messages.

If any required capability is unavailable, the entire game runs on the main
thread instead. The launcher logs the selected execution mode and warns with
the missing capability when an enabled worker has to fall back.

Shared memory requires a cross-origin isolated document. Production servers
must return these headers for the page and its same-origin resources:

```text
Cross-Origin-Opener-Policy: same-origin
Cross-Origin-Embedder-Policy: require-corp
Cross-Origin-Resource-Policy: same-origin
```

The bundled `dev-server.mjs` supplies these headers and the correct
`application/wasm` content type for local testing.
