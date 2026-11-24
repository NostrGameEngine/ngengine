# nge-nav

Shared navigation for NGE: walkable, climbable and inverted surfaces, free-space
flight, and mixed routes. The same voxel boundary pipeline handles all surface
orientations. A plane constraint selects 2D operation.

The module depends on `jme3-core`, uses engine math and `BufferUtils`, and keeps
the engine's Java baseline (Java 21 on pipenext2). Native bindings and the Vector API are not required.

```groovy
implementation project(':nge-nav')
```

## Register geometry and an actor

```java
NavWorld world = new NavWorld(NavSettings.builder().cellSize(0.5f).build());
stateManager.attach(new NavAppState(world));
NavLayer terrain = world.layer("terrain");
NavSource source = terrain.add(geometry, true, 0, NavGeometry.DEFAULT_FLAGS);

NavView view = world.view()
    .radius(0.3f).height(1.8f)
    .allow(NavMode.WALK, NavMode.CLIMB, NavMode.FLY, NavMode.JUMP)
    .cost(NavMode.WALK, 1).cost(NavMode.CLIMB, 4).cost(NavMode.FLY, 2)
    .maxSlope(50).maxBend(100).stepHeight(0.4f)
    .costCutoff(5)
    .build();
NavNavigator navigator = view.navigator();
navigator.target(destination);
```

Positions and paths use **body centers in world coordinates**. The body is an
upright capsule for walking, a capsule aligned with the support normal for
climbing, and a capsule aligned with the view's up vector in flight. Top-down 2D
uses a circle; platformer 2D retains capsule height and real surface support.
Radius/height affect admission and collision queries without rebuilding shared
geometry. All actors share the same pages.

`true` declares a watertight, consistently oriented, non-self-intersecting mesh
as a filled solid. Interior navigation is removed using solid classification;
no sky-visibility test removes roofs or caves. Capture separate overlapping
solids separately. Use `false` for intentional open shells, terrain sheets and
room boundaries. Primitive `box` and `sphere` sources are filled solids.

Capture and source updates belong to the scene/physics owner thread, after world
transforms are current. Workers only read captured immutable geometry. Mesh snapshots build a packed triangle
BVH so tile rasterization and body queries can skip distant triangles. To update
a deformed mesh or moved object:

```java
source.update(geometry, true, 0, NavGeometry.DEFAULT_FLAGS);
// Or source.replace(newImmutableSnapshot).
```

Affected pages become unavailable immediately, and are rebuilt asynchronously.
Obsolete jobs cannot republish old geometry. Unaffected page references stay valid.
`source.close()` removes geometry; `terrain.close()` removes a layer's sources and
imported pages. Close `world` when finished, or let `NavAppState` own its lifetime.

### Movable collision objects

An interactable collider can remain an obstacle without contributing walkable or
climbable surfaces. Publish it with a positive identity owned by your world:

```java
NavSource boulder = terrain.add(NavGeometry.sphere(center, radius).asMovableObstacle(bodyIdentity));
// Moving it changes published clearance geometry without rebuilding the floor.
boulder.replace(NavGeometry.sphere(newCenter, radius).asMovableObstacle(bodyIdentity));
// A transport query excludes its own load, while retaining all other obstacles.
NavView transport = view.copy().ignoreMovableObstacle(bodyIdentity).build();
```

Ordinary views still collide with the object, including the floor below it.
`getSurfaceRevision()` tracks page/link changes; `getRevision()` also changes for
movable clearance updates. Reusing a surface route still requires current obstacle
clearance checks. Runtime obstacle identities are not serialized in static page
files; re-register interactable sources from the physics world after loading.

## Request movement without waiting

Allocate input, output and movement stores once per actor:

```java
NavMotion motion = new NavMotion();
NavStep step = new NavStep();
Vector3f velocity = new Vector3f();
Vector3f lastTravelVelocity = new Vector3f();
Vector3f nextPosition = new Vector3f();
```

For each simulation update, fill `position`, `forward`, measured `velocity`,
`mode` and `time`. Set normalized `up` to the actor's current body axis when
climbing. Then:

```java
navigator.update(motion, deltaTime, step);
if (step.getStatus() == NavStatus.MOVE) {
    step.getDesiredVelocity(velocity);
    if (!step.isPositionCorrection()) lastTravelVelocity.set(velocity);
    // Pass velocity to the game's Box2D/Box3D/controller implementation.
} else if (step.getStatus() == NavStatus.YIELD) {
    // The game may keep applying its previous velocity while guidance is pending.
    velocity.set(lastTravelVelocity);
    // step.getReason() explains why no fresh command is available.
} else if (step.getStatus() == NavStatus.TRANSITION) {
    long token = step.getActionId();
    step.getNextPosition(nextPosition);
    // Execute step.getAction(), using its target position and support normal.
    // navigator.acknowledge(token, succeeded) after the controller finishes.
}
```

`MOVE` supplies a desired velocity and a validated horizon, capped at 0.25 seconds.
`isPositionCorrection()` identifies alignment with the route's initial sample.
Apply that body correction while preserving facing and the previous travel vector;
otherwise a short backward alignment can turn the actor around and become its
next `YIELD` velocity. During traversal actions, update the retained travel vector
from the controller's trajectory. Stop/arrival handling remains the game's policy.
Other statuses provide no fresh velocity command. `arrivalDistance` relaxes the
final endpoint; intermediate samples remain precise to avoid cutting corners.

Rigid-body controllers can set a small `contactSkin` on the view, in world units,
to match their solver's permitted resting contact penetration. For example,
`.contactSkin(0.004f)` admits four millimetres rather than artificially raising
the measured body position. It defaults to zero, is bounded by the body radius,
and does not admit deeper overlaps. It is not extra step height or a bridge over
missing support. Keep the real controller position in `NavMotion`.

`ARRIVED` means arrival at the
admitted graph endpoint, projected within `nearestDistance` of the requested target.
`NO_PATH` means a completed unsuccessful search; missing data, search budgets,
queue pressure and unsupported positions produce explicit `YIELD` reasons.
When a completed plan arrives after the actor has moved, guidance reconnects to
an unpassed segment of its initial surface corridor after checking admission,
support and collision clearance. It preserves authored actions, real steps and climbing changes
of surface orientation rather than skipping them to reach a later waypoint.
Generation and path search never run inside `update`, including with an inline
platform executor. Guidance performs bounded local validation without acquiring
the world's mutation lock. A changed revision can require another update before
physics consumes an intent.

For continuous movement, give the navigator a reusable path follower:

```java
NavNavigator navigator = view.navigator(new NavPathFollower());
// Reuse motion and step. Supply measured motion, apply the intent in your controller.
navigator.target(destination);
navigator.update(motion, deltaTime, step);
```

`NavPathFollower` looks ahead up to 8 world units by default; pass a different
distance to its constructor. It removes visible grid zigzags, maintains cruise
speed across intermediate samples, anticipates corner braking and limits velocity
changes. With no view kinematics, steering defaults to acceleration 8 units/s²,
braking 12 units/s², turn rate 6 radians/s and zero arrival speed. A configured
`NavKinematics` replaces these defaults and also participates in route planning.
The no-argument `view.navigator()` retains precise sample following.

Shortcuts check capsule clearance, actor admission, area costs and continuous
support coverage. Walking follows the actual triangles across changing slopes;
a normal change on continuous terrain does not create a STEP action. Coverage
uses exact clipped face intervals and checks continuity in support height, so
narrow gaps and real ledges cannot become smoothing shortcuts. Capsule contact
accounts for finite triangle edges at ramp/landing seams. Walking shortcuts compare
integrated surface cost with the planned route, so admitted changes in slope do
not appear as holes in support coverage. Area costs and actor cutoffs still apply.
Missing pages and exhausted local budgets retain conservative guidance. Precise
legs use the same steering limits as visible shortcuts. Stopping distances account
for velocity-times-delta integration. Mandatory action points remain strict;
arrival can use the destination's configured tolerance to finish braking smoothly.
Climbing orientation changes, real steps, mode switches and authored links remain
explicit actions. Following works in world or moving-frame coordinates, and
allocates no objects during steady updates. Each follower has one navigator owner.

An explicit `replan()` keeps validating and following the current route while a
replacement is pending. Changed targets, invalidated geometry and failed actions
still cancel obsolete guidance. A replacement uses measured velocity to reconnect;
the controller continues to own its `YIELD` policy.
Blocked guidance waits for changed geometry or measured motion rather than
requesting the same route every frame. A changed measured velocity can resume
following even if collision handling kept the body at the same position.

Controllers without physics can share `NavStepTrajectory` for STEP actions. Call
`begin(view, motion.position, step)`, then `sample(travelledDistance,
motion.position, positionStore)` and apply the returned position. At
`getLength()`, acknowledge the token after updating the body and support axis.
It tries a direct crossing and a raised arc at uniform arc-length speed, retaining
the validated raised crossing in tight geometry. Sampling checks the move from
the actual body against the latest snapshot; fail the action if sampling fails.
Flight, animation, jump execution and character physics remain controller policy.

Transitions include takeoff, landing, attachment, climb reorientation, steps,
jumps, drops, teleportation and custom actions. Repeated polls return the same
pending token. One acknowledgement is accepted; success also requires the actual
body position, mode and climbing orientation to match the destination. The game
owns animation, physics, cancellation and failure recovery.

A navigator and its motion/step objects have one owner thread. Different navigators
can run concurrently. `NavView` is immutable; replace it with
`navigator.setView(view.copy().costCutoff(2).build())` to change an actor's policy.

## Costs and optional dynamics

`costCutoff` is a **local admission limit**: a difficulty greater than the cutoff
is forbidden; equality is admitted. It is independent of accumulated path cost.
Admission also applies to nearest/random points, traversal endpoints, support
samples and guidance. Flags and capabilities still apply when costs are zero.

Surface inclination is relative to `up`. `maxSlope` constrains walking;
`slopeCost(mode, costPerRadian)`, `areaCost(id, cost)` and `NavCostPolicy` provide
actor-relative difficulty. A policy receives mode, area, inclination and signed
ascent. It must be thread safe, allocation free and fast. Negative, non-finite
or over-cutoff results are forbidden. Register `NavGeometry.areaBox(...)` to
change area metadata without adding a physical obstacle.

Zero costs are valid, including zero-cost flight and teleportation. A signed
`preference(mode, bias)` changes a base cost, clamped at zero. Actual search costs
are nonnegative. Search uses admissible cost bounds from distance and the final
entry edge, with distance as a tie-breaker for equal-cost routes. Authored links
conservatively disable geometric estimates. Zero-cost routes do not acquire an
artificial minimum cost.

Measured velocity has no inertial effect in the default arcade configuration.
Enable only the dynamics that the actor needs:

```java
NavKinematics limits = NavKinematics.builder()
    .acceleration(6).braking(10).turnRate(2)
    .timeWeight(0.5f).terminalSpeed(0)
    .build();
navigator.setView(view.copy().kinematics(limits).build());
```

Finite limits constrain guidance. Turn rate and time weighting also influence
route selection. The route model retains one arrival state per spatial/mode
node; its velocity-aware result is an approximation, not a globally optimal
kinodynamic trajectory. Physics remains authoritative.

## Moving navigation frames

`NavFrame` gives a local navigation world a rigid pose in the scene. Register deck
geometry and issue actor queries in local coordinates, then translate and rotate
the frame without rebuilding its pages or invalidating local routes:

```java
NavWorld deckWorld = new NavWorld(settings);
NavFrame deckFrame = new NavFrame(deckWorld);
deckWorld.layer("deck").add(NavGeometry.box(deckMin, deckMax));
NavNavigator passenger = deckWorld.view().radius(0.16f).height(1).build().navigator();
deckFrame.setPose(initialPosition, initialRotation);

// Each owner-thread update; localMotion and worldMotion are reused.
deckFrame.updatePose(platformPosition, platformRotation, deltaTime);
passenger.update(localMotion, deltaTime, step);
// The game consumes the local guidance and updates its local body pose.
deckFrame.toWorld(localMotion, worldMotion);
```

`toWorld` rotates the body axes and relative velocity, adding platform translation
and angular point velocity. `toLocal` removes that motion from measured world
poses before querying locally. Conversions accept an existing output, including
the input itself. `toWorldPoint` and `toLocalPoint` map destinations and markers;
`getTransform(store)` supplies a reusable scene transform. Pose changes support
translation and rotation, with unit scale. `setPose` resets measured velocity for
placement or teleportation; consecutive `updatePose` rotations must differ by less
than half a turn.

Views, up axes and targets use the frame's local coordinates. Geometry edits still
rebuild affected local pages. The caller owns the `NavWorld` lifecycle, collisions
with other frames and transfers between domains; automatic cross-frame path search
and boarding links are not provided by `NavFrame`. `NavView.isSegmentClear(from, to)`
provides a bounded capsule sweep against the published geometry for external
controllers such as the example's jump. It uses the view's up axis and returns
false for missing pages or exceeded budgets. It checks clearance, so landing
support and actor permissions still need separate validation.

## Paging, links and 2D

Adding a source requests intersecting pages and a raster border automatically.
For flight beyond source bounds, request additional space explicitly with
`world.load(new NavChunkKey(x, y, z))`. Unknown or unloaded pages are unavailable,
including when the body's bounds cross a page boundary. `world.unload(key)`
invalidates its old references. Neighboring compatible pages join automatically.
A path whose requested start or target page is missing yields `WAITING_FOR_DATA`;
it does not report arrival at a nearby loaded page.

For game-controlled streaming, configure `NavSettings.builder().autoLoadPages(false)`.
Register geometry snapshots, then explicitly `load` the pages around active actors
and `unload` pages leaving that domain. Geometry edits rebuild requested pages;
they do not reload evicted pages. Keep enough neighboring pages for body clearance
and the intended route, rather than only the currently rendered terrain.

`world.getChunk(key)` obtains an immutable page. Store it with
`NavChunkIO.write(page, dataOutput)`; read on a loading worker with
`NavChunkIO.read(dataInput, settings)`, then `layer.attach(page)`.
The format is versioned, checked and independent of byte order. Loading does not
rebuild serialized nodes. Reattachment assigns a fresh world identity. Use the
read overload to bound total triangle allocation. Persist authored links separately.

```java
world.addLink(NavLink.jump(launchCenter, landingCenter, 0.3f, 1.2f, 0.8f));
world.addLink(NavLink.teleport(entranceCenter, exitCenter, 0.3f, 0, true));
```

Jump height and duration define a parabolic traversal. Its capsule clearance is
checked against current captured geometry. `NavLink` also supports drop and custom
traversals. Its authored difficulty adds to the actor's mode difficulty before the
cutoff; its nonnegative cost multiplier changes route utility. Enable the corresponding modes in each actor view before expecting those links to
be selected. The controller must execute the authored trajectory. Use
`world.getLink(step.getTraversalId())` for its duration and arc parameters.

For 2D, configure `NavSettings.builder().plane(origin, normal)`. The default view
uses the plane normal as `up`, giving top-down navigation with virtual support.
For a platformer in the XY plane, use `.plane(Vector3f.ZERO, Vector3f.UNIT_Z)`
and a view with `.up(Vector3f.UNIT_Y)`: ground must provide actual support,
height controls capsule clearance, and jump arcs remain in the motion plane.
Coplanar render triangles and solids only touching the plane do not block the
2D domain. Use extruded collider geometry or `NavGeometry.planarPolygon`, which
captures an ordered convex physics polygon in that plane's local x/y coordinates.
Box2D adapters should capture world-transformed polygon coordinates on the physics
thread and replace the source after shape changes. The 3D API accepts captured
triangle meshes, boxes and spheres from Box3D in the same way; the module does not
call either physics world from worker threads.

## Scheduling, memory and resolution

`new NavWorld(settings)` owns separate bounded generation and planning work queues
and Java worker pools. Pool width is capped at four workers per pool. For a supplied
platform executor, construct `new NavWorld(settings, generationExecutor, planningExecutor)`;
call `dispatch(maxJobs)` from the task scheduler to submit queued planning work.
Both executors remain caller owned.

For a cooperative platform use `NavWorld.manual(settings)` and call
`pump(maxJobs)` from a loading/task loop. One job is one page build or one bounded
search; this is a job-count budget, **not** a hard elapsed-time budget. These jobs
should run in a worker when single-frame build latency matters. Generation does
not require rendering or graphics. A failed/rejected build is observable through
`getFailure()` and can be retried with `retryBuilds()`.

Page and path payloads use engine-allocated direct buffers. Search uses reusable
primitive arrays and raster scratch per worker; navigators retain a small local workspace.
Local validation reads nearby pages and their sources rather than scanning all loaded geometry. Stable
guidance and repeated pending/full-queue polls avoid per-frame object allocation.
Changes, captures, queue submissions, immutable publications and new paths allocate.
Buffers remain managed by the configured engine allocator and are reclaimed after
all snapshots/readers release them. No eager free invalidates another reader.

The mesh consists of sampled oriented surface cells and free volume cells.
`cellSize` controls quantization and passage resolution; very thin passages can
be conservatively lost. Surface contacts project onto captured geometry. This
implementation does not reproduce Recast's 2.5D contour/detail mesh or its binary
format. Capsule sweeps and arcs use bounded subdivision, so they supplement the
physics controller rather than replace continuous collision detection. `searchBudget`,
`localBudget`, `chunkSize` and `jobCapacity` bound work and storage; exceeding a
budget produces an unavailable result instead of silently accepting an unsafe edge.

## Interactive 3D sandbox

```shell
./gradlew :nge-examples:runNavigationDemo
```

`ngetests.tests.navigation.TestNavigation` runs a rendered scene with two banks,
a permanent detour, an editable bridge and a ramp onto a raised terrace. The
character patrols between the banks using actual asynchronous navigation results.
Every eight simulation seconds, terrain events collapse the bridge, change its mesh into a raised crossing,
restore it, and raise a barrier. The demo controller consumes movement intents,
executes raised step/crest crossings and applies simple gravity/reset behavior;
no physics engine is loaded by this task. Each completed rebuild requests a new
route so a newly opened shortcut can replace a still-valid detour.

The scene uses flat-color `Unshaded` materials with directional shadows. The walker and spider
retain their last movement vector during `YIELD`, then follow fresh guidance as
soon as it becomes available. This is a controller policy: `YIELD` itself still
contains no new movement command. The walker's ground clearance follows the
visible terrain while navigation rebuilds. The HUD's `Coast / resume` counters
show continued movement and the return to current navigation guidance.

A floating platform translates, rotates and changes altitude in front of the
terrain. Its pink passenger navigates around an onboard crate on a separate local
world. Cyan samples and the passenger's route move with the deck. The HUD shows
local/world speeds, completed goals, jumps, landings, pose updates and a stable
local mesh revision. Movement never recaptures the deck geometry.

The camera stays fixed and navigation wireframes are always visible. Left-click
an actor or its label to select it; the yellow ring marks the selection.
Right-click a destination for that actor. The spider accepts walls and ceilings
as well as terrain. The pink passenger starts aboard. A terrain destination requests
disembarkation; a deck destination from the ground requests boarding. The actor
walks to a suitable approach, waits until the jump is possible, and continues to
the selected destination after landing. Boarding predicts the deck's translation
and rotation at touchdown; afterward the actor is carried in its local frame.

Jump points do not need to be authored. The example searches all four deck edges
at the navmesh resolution (0.5 m), chooses supported terrain nearby, and checks
body clearance against terrain, the moving deck and its crate. It allows at most
3.25 m of horizontal travel and 3.5 m of height difference. A valid free edge and
navigable landing are required. This is a bounded controller search, not a global
shortest-path search across navigation frames. Commands issued during a jump
change the subsequent destination; the current transfer finishes first.

While waiting for ground guidance, the passenger coasts only across supported,
clear terrain. Rising terrain moves an intersecting waiting body onto admitted
support. Boarding keeps a shore approach stable until the deck is reachable.

The controller regression runs without graphics or a physics engine:

```shell
./gradlew :nge-examples:navigationTransferSmoke
./gradlew :nge-examples:navigationMovementSmoke
```

It checks three round trips, including boarding a frozen deck, stopping the deck
during ascent, continued local navigation, and carriage after reboarding.
The movement smoke also exercises repeated wall, roof-edge and ceiling bends
with the real spider controller, including arrival from a transverse velocity.

Step crossings and the spider's surface bends travel at a uniform speed along
their validated trajectories. Character models interpolate
their displayed poses independently of the navigation body, so waypoint and
surface changes do not instantly rotate the rendered actor. This interpolation
does not change the body's movement or the collision checks used by navigation.

| Control | Action |
| --- | --- |
| Left mouse | Select the cyan walker, violet spider or pink passenger (actor or label) |
| Right mouse | Set the selected actor's destination; the pink passenger jumps ashore or boards the deck as needed |
| D / K | Freeze moving objects, hills and platform / switch walker terrain cost |
| E / T | Trigger the next terrain event / toggle automatic events |
| R | Reset the character and resume patrol |
| Space | Pause character motion and the automatic event clock |
| P | Toggle planned routes; navmesh wireframes remain visible |

Green lines show shared surface samples, orange lines show recently published
pages, and gold shows the current actor route. The shared overlay includes
orientations the walking actor cannot traverse. `NavChunk.createDebugMesh()`
provides surface geometry; `navigator.getPath()` exposes the immutable current
route, or null while no route is available. Cache debug geometry by page identity
and path identity rather than regenerating it every frame.

The HUD displays FPS, movement/status/reason, guidance time, routes/arrivals,
resident/pending pages, node count, world revision, publication count, edit latency,
heap usage and JVM collection counters. Rebuild latency includes capture,
scheduling, generation, publication and the render loop's observation delay.
These are live diagnostics, not isolated algorithm benchmarks. HUD text refreshes
four times per second; debug buffers change only when pages/routes change.

For a finite rendered smoke run (at least 32 seconds), use
`-Dnav.demo.seconds=42`. It exits unsuccessfully unless terrain events, new routes,
actual movement and an arrival occurred. To capture each settled event stage,
create an output directory and pass `-Dnav.demo.captureDirectory=/absolute/path/`.
This mode still requires a graphics display; it is not the headless engine smoke.
The rendered check also requires platform arrivals and local travel, verifies the
passenger's world transform and deck bounds, and fails if frame motion changes the
settled local mesh revision.

Run module verification with `:nge-nav:test`; `:nge-nav:allocationTest` selects
allocation/latency checks after warmup. `:nge-nav:engineSmoke` runs the real engine
headless loop. `:nge-nav:teavmSmoke` compiles and executes JavaScript with the engine
web backend; its generated `build/teavm-smoke/browser.html` also runs in a browser.

Capture large terrain in pieces. Page files retain relevant collision source
snapshots; a source spanning many pages is repeated in each serialized page.
