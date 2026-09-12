# Mobile-device classification for automatic controls

`JmeSystem.isMobileDevice()` reports a handheld phone/tablet form factor suitable
for on-screen game controls. An operating system or a touchscreen alone does not
establish that form factor. Connected physical gamepads are checked separately by
`InputHandlerFragment.showOnScreenJoystick()`; explicit fragment overrides remain
available.

## Backend policy

- Web: a boolean `navigator.userAgentData.mobile` is authoritative, including
  `false`. When unavailable, use the existing broad mobile user-agent regex.
  The fallback intentionally also matches Android TV user agents. Window size,
  touch-point count, and pointer media queries are not additional fallbacks.
- Android: require a real touchscreen and exclude PC, TV/leanback-only,
  automotive, watch, and Chromebook hardware features. Require normal UI mode,
  reject the optional Samsung DeX mode when exposed, and on API 17+ require the
  view to be attached to the default display. A missing view is unknown/false.
  Cache hardware features per context and DeX field lookup per configuration
  class; continue reading current UI mode, DeX value, and display identity.
- iOS: query UIKit through `LibJGLIOSDeviceBridge.isMobileDevice()`. Phone and pad
  idioms qualify; TV, Mac, other idioms, and iOS apps running on Mac do not. Cache
  the immutable answer in the engine delegate. A missing Java/native bridge
  returns false without repeated linkage failures.
- Desktop and unknown backends: false by default. The SDL version used by the
  current desktop bindings does not identify desktop tablets with `SDL_IsTablet`.
  A small window or touch-capable monitor is not assumed to be handheld.

The GUI uses the same backend classification for its default cursor policy;
actual touch events remain a separate way to interact directly with windows.
The legacy `isMobilePlatform()` helper still describes the OS, not this policy.

## Local iOS development

The new native query requires the updated libJGLIOS core artifact. `jme3-ios`
currently consumes `org.ngengine:libjglios-core-ios:0.0-SNAPSHOT` for that bridge.
Before building it against an unpublished local libJGLIOS change, run in the
libJGLIOS checkout on macOS with Xcode installed:

```sh
./gradlew :core-ios:check :core-ios:publishToMavenLocal
```

Then run `./gradlew :jme3-ios:check` in the engine checkout. Publishing to Maven
local is not a remote release: CI/other machines need the matching updated
snapshot or a future released version containing both Java and native changes.

Android vendor features remain best-effort, not a physical-size measurement.
An unrecognized kiosk or vendor desktop mode may still look like a normal touch
tablet. Browser mobile classification is likewise a browser policy, not a
guarantee about physical dimensions.
