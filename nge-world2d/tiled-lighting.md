# Tiled map lighting

Set the custom Tiled property `lit` to `inherit`, `unshaded`, or `dynamic`.
Resolution is: placed object/tile → tile definition → tileset → layer (including
parent groups) → map. With no override the map is unshaded. An object can
emit light even if its own sprite is unshaded.

The map's `light.ambient` (0–1, default 0.72) controls the baseline. Add these
custom properties to a placed object or its tile definition:

- `light.type`: `point`, `spot`, or `directional` (a uniform color wash in 2D).
- `light.color` (Tiled color), `light.strength`, and `light.range` (render-space pixels).
- `light.x` and `light.y`: offset from the tile's placement origin, right/down.
- `light.direction`: clockwise degrees from right; `light.cone`: full cone angle.
  `light.aspect` multiplies the vertical radius.
- `shadow.radius` (pixels), `shadow.x`, `shadow.y`, `shadow.aspect`, and
  `shadow.strength` create a soft elliptical contact shadow in the mask.

This is a screen-plane light field, shared across the map and sampled by the
ordinary and instanced Tiled materials. The renderer multiplies the separate
mask over the shaded pixels without changing tile alpha or source assets.
Shadows are soft contact blobs, not wall occlusion. Embedded 3D models keep
their own materials and lighting; their tile object can still cast a blob on
surrounding shaded tiles.
