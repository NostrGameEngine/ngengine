# NGEngine (Nostr Game Engine)
NGEngine is a Java-based game engine built on top of jMonkeyEngine 3, integrated with the Nostr ecosystem for p2p networking and decentralized gaming applications.

Always reference these instructions first and fallback to search or bash commands only when you encounter unexpected information that does not match the info here.

## Working Effectively

### Prerequisites and Setup
- Install Java 17+ (Java 21 recommended based on CI configuration)
- No manual Gradle installation needed - use the Gradle wrapper (`./gradlew`)
- Set `JAVA_HOME` environment variable if needed:
  ```bash
  export JAVA_HOME="/usr/lib/jvm/java-17-openjdk-amd64"  # or your Java path
  ```

### Build Commands (CRITICAL TIMING INFORMATION)
**NEVER CANCEL BUILD COMMANDS** - Builds can take significant time. Always use appropriate timeouts.

#### Core Build (Network Access Available)
- **Full build**: `./gradlew build` -- **takes 2-5 minutes. NEVER CANCEL. Set timeout to 10+ minutes.**
- **Clean build**: `./gradlew clean build` -- **takes 2-5 minutes. NEVER CANCEL. Set timeout to 10+ minutes.**
- **Build without tests**: `./gradlew build -x test` -- **takes 1-3 minutes. NEVER CANCEL. Set timeout to 5+ minutes.**

#### Network-Restricted Environment Build
If you encounter network access issues (dl.google.com, central.sonatype.com unreachable):
- **Core modules only**: `./gradlew :jme3-core:build :jme3-desktop:build :jme3-effects:build :jme3-jbullet:build :jme3-jogg:build :jme3-lwjgl3:build :jme3-networking:build :jme3-plugins:build :jme3-plugins-json:build :jme3-plugins-json-gson:build :jme3-terrain:build :jme3-testdata:build :jme3-examples:build :jme3-awt-dialogs:build :jme3-screenshot-tests:build :jme3-ios:build -PskipPrebuildLibraries=true -x test` -- **takes 1-2 minutes. NEVER CANCEL. Set timeout to 5+ minutes.**
- **Note**: Android modules and NGE modules require network access and may fail in restricted environments

### Testing
- **All core tests**: `./gradlew :jme3-core:test :jme3-desktop:test :jme3-effects:test :jme3-jbullet:test :jme3-jogg:test :jme3-lwjgl3:test :jme3-networking:test :jme3-plugins:test :jme3-plugins-json:test :jme3-plugins-json-gson:test :jme3-terrain:test -PskipPrebuildLibraries=true --continue` -- **takes 10-30 seconds. NEVER CANCEL. Set timeout to 60+ seconds.**
- **Single module tests**: `./gradlew :jme3-core:test -PskipPrebuildLibraries=true` -- **takes 5-15 seconds. Set timeout to 30+ seconds.**
- **Specific test patterns**: `./gradlew :jme3-core:test --tests "com.jme3.math.*" --continue` -- **takes 2-5 seconds. Set timeout to 30+ seconds.**

### Code Quality and Formatting
- **Format check**: `./gradlew :nge-core:spotlessCheck` -- **takes 15-30 seconds. Set timeout to 60+ seconds.**
- **Apply formatting**: `./gradlew :nge-core:spotlessApply` -- **takes 15-30 seconds. Set timeout to 60+ seconds.**
- **SpotBugs analysis**: `./gradlew spotbugsMain` -- **takes 30-60 seconds. Set timeout to 2+ minutes.**

### Running Examples and Applications
- **Examples launcher** (requires GUI): `./gradlew :jme3-examples:run` -- fails in headless environments with "No X11 DISPLAY" error, which is expected
- **Note**: Examples are GUI-based and cannot be visually tested in headless environments

## Validation
- **ALWAYS run a build after making changes**: Use the appropriate build command above based on your network environment
- **Test core functionality**: Run at least `./gradlew :jme3-core:test` to verify core engine functionality
- **Check formatting**: Run `./gradlew spotlessCheck` on NGE modules (nge-*) before committing changes
- **Known test failures**: One test in TestLocators may fail due to network dependencies - this is expected in restricted environments

## Common Development Tasks

### Project Structure
The repository contains:
- **jme3-\*** modules: Core jMonkeyEngine components (graphics, physics, networking, effects, etc.)
- **nge-\*** modules: Nostr Game Engine extensions (authentication, GUI, networking, etc.)
- **Gradle wrapper**: `./gradlew` and `./gradlew.bat` for cross-platform builds
- **Examples**: Located in `jme3-examples/` with TestChooser as main entry point

### Module Dependencies
- NGE modules depend on jMonkeyEngine core modules
- Some NGE modules require external snapshot dependencies that may not be available in restricted networks
- Android modules require Google's build tools and may fail without network access

### Network Dependencies
The build system requires access to:
- `dl.google.com` - Android build tools
- `central.sonatype.com` - NGE snapshot dependencies  
- `objects.jmonkeyengine.org` - Prebuilt native libraries

**In restricted environments**: Use `-PskipPrebuildLibraries=true` and build only core jMonkeyEngine modules.

### Important Notes
- **Gradle version**: Project uses Gradle 8.13 (automatically downloaded by wrapper)
- **Java compatibility**: Builds with Java 17+, CI uses Java 21
- **Deprecation warnings**: Dokka plugin shows deprecation warnings - these are expected and non-blocking
- **Compilation warnings**: Java unchecked cast warnings are present but non-blocking
- **Headless limitations**: GUI applications and examples cannot be visually tested in headless environments

### Troubleshooting Common Issues
- **Network timeouts**: Use `-PskipPrebuildLibraries=true` flag and build only jme3-* modules
- **Android build failures**: Comment out Android dependencies in build.gradle temporarily
- **Missing dependencies**: Ensure you're using supported Java version (17+) and have internet access for initial setup
- **Long build times**: This is normal - builds can take several minutes, especially clean builds

## Example Command Sequences

### First-time setup and validation:
```bash
# Initial build (with network access)
./gradlew clean build -x test  # 2-5 minutes, NEVER CANCEL

# Run core tests
./gradlew :jme3-core:test  # 5-15 seconds

# Check formatting
./gradlew :nge-core:spotlessCheck  # 15-30 seconds
```

### Restricted network environment:
```bash
# Build core modules only
./gradlew :jme3-core:build :jme3-desktop:build :jme3-lwjgl3:build -PskipPrebuildLibraries=true -x test  # 1-2 minutes, NEVER CANCEL

# Test what can be tested
./gradlew :jme3-core:test --continue  # 5-15 seconds
```

### After making changes:
```bash
# Build affected modules
./gradlew build -x test  # 1-3 minutes, NEVER CANCEL

# Run relevant tests
./gradlew :jme3-core:test --continue  # 5-15 seconds

# Format code if working on NGE modules
./gradlew :nge-core:spotlessApply  # 15-30 seconds
```