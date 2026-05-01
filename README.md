# Nostr Game Engine
### Based on [jMonkeyEngine](https://jmonkeyengine.org/) under BSD 3-Clause License

A game engine and framework for building games and applications integrated with the Nostr ecosystem and p2p networking.

![img](https://ngengine.org/imgs/nge-v0.gif)

- Roadmap: https://ngengine.org/
- Release: [mavenCentral](https://central.sonatype.com/search?namespace=org.ngengine)
- Documentation: https://ngengine.org/docs 


## Build-time requirements

- JDK 21+
- Android SDK 33+
- Android NDK 29+
- Gradle 8.13+
- cmake 3.16+

## Runtime Requirements

### Linux, Windows, macOS (portable jar)
- Java 21 or higher
- OpenGL 3.2+ 

### Linux, Windows, macOS (GraalVM native build)
- OpenGL 3.2+

### Android
- Android API level 33 or higher
- OpenGL ES 3.0

## Buildtime Requirements

### Linux, Windows, macOS (portable jar)
- Java 21 or higher
- Gradle 8.13+ (or included gradlew wrapper)

### Linux, Windows, macOS (GraalVM native build)
- GraalVM 24+
- Gradle 8.13+ (or included gradlew wrapper)

### Android
- Android Sdk 33 or higher
- Android NDK
- Gradle 8.13+ (or included gradlew wrapper)

## Running examples

The engine comes with examples that you can run with:

```bash
./gradlew runExamples
```

You can use the `-Pexample` property to start an example directly:

```bash
./gradlew runExamples -Pexample=jme3test.light.pbr.TestPBRSimple
```
