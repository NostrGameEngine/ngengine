# Copilot Instructions for Nostr Game Engine

## About This Project

Nostr Game Engine (NGE) is a fork of [jMonkeyEngine](https://jmonkeyengine.org/) extended with Nostr ecosystem integration and P2P networking capabilities. The codebase consists of two main parts:

- **jme3-\*** modules: Core engine from upstream jMonkeyEngine (BSD 3-Clause License)
- **nge-\*** modules: Nostr Game Engine extensions with Nostr/P2P features

## Build, Test, and Development Commands

### Building

```bash
# Full build from scratch
./gradlew clean build

# Build without cleaning
./gradlew build

# Build a specific module
./gradlew :jme3-core:build
./gradlew :nge-core:build

# Install to local Maven repository
./gradlew install
```

### Testing

```bash
# Run all tests
./gradlew test

# Run tests for a specific module
./gradlew :jme3-core:test
./gradlew :nge-core:test

# Run a single test class (use --tests flag)
./gradlew :jme3-core:test --tests "com.jme3.anim.AnimComposerTest"
```

### Code Quality

```bash
# Run SpotBugs (static analysis)
./gradlew spotbugsMain

# Format NGE modules with Spotless (only applies to nge-* modules)
./gradlew spotlessApply

# Check formatting
./gradlew spotlessCheck

# Add/update license headers (for modules with .LICENSE files)
./gradlew addLicenseHeaders
```

### Documentation

```bash
# Generate merged Javadoc for all modules
./gradlew mergedJavadoc
# Output: dist/javadoc/

# Generate Dokka markdown docs (for NGE modules)
./gradlew dokkaGfmMultiModule
# Output: build/dokka/
```

### Running Examples

```bash
# Run jME3 examples
./gradlew run
# Or specifically:
./gradlew :jme3-examples:run
```

## Architecture Overview

### Module Organization

The project uses a multi-module Gradle structure:

**Core Engine Modules (jme3-\*):**
- `jme3-core` - Core rendering, scene graph, animation, input handling
- `jme3-effects` - Post-processing effects
- `jme3-networking` - Basic networking (SpiderMonkey)
- `jme3-plugins` - Asset loaders (OBJ, glTF, etc.)
- `jme3-terrain` - Terrain rendering
- `jme3-lwjgl3` - LWJGL3 desktop backend
- `jme3-android` - Android platform support
- `jme3-jbullet` - JBullet physics integration
- `jme3-desktop` - Desktop utilities

**NGE Extensions (nge-\*):**
- `nge-core` - Component system, async asset loading, dev mode utilities
- `nge-networking` - P2P networking and lobby system
- `nge-auth` - Nostr authentication (NIP-46 bunker support)
- `nge-gui` - GUI components
- `nge-app` - Application framework
- `nge-ads` - Advertising integration
- `nge-web` - Web/browser support (TeaVM-based)
- `nge-world2d` - 2D world/tile support
- `nge-android` - Android-specific NGE features

### Key Architectural Patterns

#### Component System (NGE)

NGE introduces a component-based architecture on top of jME3:

- **Component**: Interface for attachable behaviors (see `org.ngengine.components.Component`)
- **AbstractComponent**: Base class for components with lifecycle hooks
- **ComponentManager**: Manages components attached to entities
- **Lifecycle methods**: `onAttached()`, `onDetached()`, `onEnable()`, `onDisable()`

Components are mounted to entities and managed through `ComponentManager` with support for enable/disable states.

#### Asset Management

- **AssetManager**: Standard jME3 asset loading (synchronous)
- **AsyncAssetManager** (NGE): Asynchronous asset loading with callbacks and thread pools
  - Location: `org.ngengine.AsyncAssetManager`
  - Pattern: Submit asset keys with callbacks, results delivered on main thread

#### Nostr Integration

- **Auth module**: NIP-46 remote signer support for Nostr authentication
- **Networking module**: P2P channels and lobby management for multiplayer
- **Lobby system**: `LobbyManager`, `Lobby`, `LobbyCursor` for managing game sessions

#### Platform Backends

- Desktop: LWJGL3 (`jme3-lwjgl3`)
- Android: Custom Android backend (`jme3-android`, `nge-android`)
- Web: TeaVM-based transpilation (`nge-web`, `nge-web-bullet`)
- iOS: iOS backend (`jme3-ios`)

### Scene Graph and Rendering

jME3 uses a scene graph architecture:
- **Spatial**: Base node (Node for containers, Geometry for renderable meshes)
- **Material**: Shader-based materials with techniques
- **RenderManager**: Handles rendering pipeline
- **ViewPort**: Render targets with cameras and scene processors
- **AppState**: Modular game states attached to Application

## Key Conventions

### License Headers

- **jme3-\* modules**: Use jMonkeyEngine BSD 3-Clause header (see `source-file-header-template.txt`)
- **nge-\* modules**: Use module-specific license files
  - Per-module: `<module-name>.LICENSE` (e.g., `nge-gui.LICENSE`)
  - Prefix fallback: `<prefix>.LICENSE` (e.g., `nge.LICENSE` for all nge-\* modules)
  - NGE modules should include attribution to jMonkeyEngine fork origin
  - Use `./gradlew addLicenseHeaders` to apply headers automatically

### Code Style

Follow Google Java Style with these modifications (see CONTRIBUTING.md):
1. No blank line before `package` statement
2. Block indentation: **+4 spaces** (not +2)
3. Column limit: **110** (not 100)
4. Continuation indentation: **+8 spaces** (not +4)
5. No trailing whitespace

NGE modules (`nge-*`) use Spotless for automated formatting with Prettier:
```bash
./gradlew spotlessApply
```

### Logging Conventions

**IMPORTANT**: Use appropriate log levels to avoid performance issues and log spam:

- **`logger.severe()`** / **`Level.SEVERE`**: Critical errors that prevent functionality
- **`logger.warning()`** / **`Level.WARNING`**: Recoverable errors, deprecated API usage, configuration issues
- **`logger.info()`** / **`Level.INFO`**: Important production events (startup, shutdown, major state changes)
  - **NEVER use INFO or higher for per-frame or frequent updates** (causes log spam)
- **`logger.fine()`** / **`Level.FINE`**: Detailed tracing for debugging (disabled in production)
  - Use for per-frame updates, frequent operations, detailed state tracking
- **`logger.finer()`** / **`Level.FINER`**: Very detailed debugging
- **`logger.finest()`** / **`Level.FINEST`**: Extremely verbose debugging

**Examples**:
```java
// ✓ GOOD - Important one-time event
logger.info("Initializing TiledGuiFragment: " + fragment.getClass().getName());

// ✗ BAD - Called every frame, use FINE instead
logger.info("Updated GUI fragment position to: " + pos.x + ", " + pos.y);

// ✓ GOOD - Per-frame update with FINE level
logger.fine("Updated GUI fragment position to: " + pos.x + ", " + pos.y);

// ✓ GOOD - Startup/initialization
logger.info("Fragment initialization complete. FB size: " + width + "x" + height);

// ✓ GOOD - Error condition
logger.warning("No FragmentData found for component: " + component);
```

### Module Dependencies

- jme3-\* modules should **not** depend on nge-\* modules
- nge-\* modules can depend on jme3-\* modules
- Use `api` dependency for transitive API exposure
- Use `implementation` for internal dependencies

Example from `nge-auth/build.gradle`:
```groovy
dependencies {
    api project(':nge-core')
    api project(':nge-gui')
    implementation project(':nge-networking')
}
```

### Java Version Requirements

- **Compilation target**: Java 11 (for library artifacts)
- **Test execution**: Java 21 (configured in common.gradle)
- **Toolchain**: Gradle auto-provisions JDK via Foojay resolver

### Version Management

Version controlled by `version.gradle` and `gradle.properties`:
- Base version: `gradle.properties` (`jmeVersion`)
- Automatic versioning from Git tags and branches
- SNAPSHOT suffix for development builds
- Version info embedded in jars via manifest

### Native Libraries

The project can use pre-built natives or build from source:
- **Pre-built**: Downloaded from `natives-snapshot.properties` URL (default)
- **Build from source**: Set `buildNativeProjects=true` in gradle.properties
- **Skip pre-built**: Set `skipPrebuildLibraries=true`
- Native modules: `jme3-android-native` (C++)

### Application Entry Points

- **Desktop/jME3**: Extend `SimpleApplication` or `Application`
- **Android**: Use Android-specific application classes
- **Web**: TeaVM-compiled entry points

### Asset Paths

Assets are loaded via `AssetManager` with registered locators:
- Classpath assets typically in `src/main/resources/`
- Common asset folders: `Textures/`, `Models/`, `Materials/`, `Sounds/`
- Test data: `jme3-testdata` module

## Testing

- Test framework: JUnit 4 (configured in common.gradle)
- Mocking: Mockito
- Tests run headless: `java.awt.headless=true` is set
- Test classes don't need to end in "Test" (style exception)

## Project-Specific Notes

### Nostr Integration

When working with Nostr features:
- Authentication uses NIP-46 bunker protocol for remote signing
- Use `nge-auth` module for identity management
- P2P networking in `nge-networking` for multiplayer/social features

### Platform-Specific Code

- Android native code: `jme3-android-native` (requires Android NDK)
- Web builds: TeaVM-based, special considerations for threading and file I/O
- Desktop: LWJGL3 is the primary backend (LWJGL 2 is deprecated)

### Build Properties

Configure builds via `gradle.properties`:
- `buildNativeProjects` - Build C++ natives from source
- `buildAndroidExamples` - Include Android example app
- `enableSpotBugs` - Run SpotBugs static analysis
- `buildJavaDoc` - Generate JavaDoc

### Development Workflow

1. Make changes to source code
2. Apply formatting (if NGE module): `./gradlew spotlessApply`
3. Add license headers (if new files): `./gradlew addLicenseHeaders`
4. Build and test: `./gradlew build test`
5. Check static analysis (optional): `./gradlew spotbugsMain`

## Contributing Guidelines

See CONTRIBUTING.md for detailed contribution guidelines including:
- Git workflow and pull request process
- Communication via forum (hub.jmonkeyengine.org)
- Code quality expectations
- Style enforcement for PRs

## Additional Resources

- Documentation: https://ngengine.org/docs
- Roadmap: https://ngengine.org/
- Maven artifacts: https://central.sonatype.com/search?namespace=org.ngengine
- jMonkeyEngine wiki: https://github.com/jMonkeyEngine/wiki
