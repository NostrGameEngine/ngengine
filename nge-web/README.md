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
and cross-origin isolation. High-frequency render, resize and input events use
a bidirectional shared-memory transport. Audio notifications remain ordered on
the message channel alongside creation and playback commands, but do not allocate
response promises or timeouts. Calls requiring a result still use request/reply.

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

## Restarting

`WebContext.restart()` reloads the page, recreating the worker and GPU resources
together. Save persistent settings before requesting a restart; in-memory game
state is not retained. The request is forwarded to the main window in worker
mode, and repeated requests are ignored while the page is reloading.
Quitting the application (including `System.exit`) uses the same reload path to
return to the launcher instead of leaving the last rendered frame on screen.

## Class-path enumeration

The web class-library patch makes `ClassLoader.getResources(String)` return an
empty enumeration: JVM class-path/package scanning is unavailable in the browser.
This compatibility shim allows optional reflected scanners to compile; it is not
an asset lookup implementation. `WebResourceLoader` continues to resolve and
enumerate indexed game assets from the web bundle. The patch is applied only by
the NGE TeaVM plugin, without changing TeaVM or desktop class loaders.

## Cooperative scheduling and frame diagnostics

With the NGE TeaVM runtime, `web_scheduler_budget_ms` sets the Wasm GC
cooperative scheduling budget (integer milliseconds, 1–100, default 4).
The launcher passes it as `scheduler.timeSliceMillis` to the TeaVM loader.
Each runtime instance has its own queue. Ready Java thread starts, yields and
sleep resumptions use the thread priority; delayed tasks cannot run before their
deadline. Waiting tasks gradually gain priority to prevent starvation. This does
not change the JavaScript fallback scheduler or create parallel Java threads.

The budget is checked between callbacks and at explicit `Thread.yield()` calls.
It cannot interrupt synchronous decoding, a physics step, GPU work or arbitrary
JavaScript/Promise callbacks. Existing priority changes take effect on subsequent
enqueue operations; they do not reorder an already queued continuation.

Set `WebFrameDiagnostics` to `true` in `ngeapp.json` to log frame-work count,
average and maximum duration every five seconds. These are wall-clock durations
inside the update/render path (including any asynchronous asset waits), not CPU
times or GPU timings. The measurement excludes the normal end-of-frame wait.
It is disabled by default and does not allocate per-frame diagnostic records.
`intervalAverageMs` and `intervalMaximumMs` measure start-to-start wall-clock
spacing, including frame waits and scheduling delays, separately from frame work.

Frame waiting suspends the existing fiber directly instead of allocating a Java
thread and monitor notification per frame. It uses `requestAnimationFrame` in
the rendering worker when available, avoiding the page-to-worker render event
hop. Environments without a usable native callback retain the bridged render
event. A shared 100 ms watchdog keeps shutdown progressing when animation frames
are paused, without waking the same wait twice.
After a worker confirms native frame support, the page stops forwarding render
events to that worker while retaining its local gamepad polling. A native-frame
failure restores forwarding for the fallback.

## Resource preload and persistent cache

The service worker reads the ZIP central directory at runtime. It downloads one
complete local ZIP record per missing resource, then extracts and caches it.
There are up to three background jobs and four jobs total, reserving capacity
for game requests. A foreground miss pauses new background jobs; already running
jobs finish, and concurrent requests for the same resource share one job.
Background work resumes when foreground requests finish, including after Skip.

Completed resources store their index hash with the response in Cache Storage.
Reloads reuse matching resources and start progress from the completed set; an
interrupted resource restarts individually. Existing extracted caches from the
previous loader are reused. ZIP directory reads are cached per bundle version.
Cache storage is scoped to the site origin and can be evicted by the browser.

Play indicates that every non-ignored resource was successfully cached, unless
preload was explicitly disabled. Failed downloads or cache writes never count as
completed. Once preload finishes, its jobs and progress timer stop. If the server
ignores Range, Skip stays disabled during the full ZIP and unbundled launcher
resource downloads; extraction and cache preparation can then proceed locally,
with the same foreground priority.

Run the browser-runtime regression tests from the engine root with
`node --test nge-web/src/test/js/*.test.mjs`. The preload fixtures exercise real
ZIP32/ZIP64 archives, compression, cache migration/resume, no-Range fallback,
corrupt downloads, cache write failures, deduplication and foreground priority.

With VSync disabled, the FPS cap measures each interval from the current frame's
start, rounds timer delays up to milliseconds, and does not accumulate catch-up
debt after a slow frame. Browser timer throttling can still delay a frame.
Unchanged fullscreen preferences are sent to the page only once, rather than
generating a redundant control event on every frame.

Neither path raises the priority inherited by gameplay-created Java threads.
Animation-frame completion resumes the waiting fiber directly; raising Java
thread priority does not preempt an update already running. Physics and rendering
remain in the same application update, so prioritizing one independently would
require changes outside this backend. The backend does not skip physics steps.
