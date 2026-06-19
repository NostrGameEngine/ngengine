# jMonkeyEngine Native Image Gradle Plugin

Generates GraalVM Native Image reachability metadata and native runtime library layouts for jMonkeyEngine and NGEngine projects.

Apply the plugin to a Java source set:

```gradle
plugins {
    id 'org.jmonkeyengine.nativeimage'
}
```

The plugin adds generated metadata resources under:

```text
build/generated/native-image-metadata/resources/META-INF/native-image/<group>/<project>/
```

and wires them into `processResources`.

## Metadata Tasks

- `generateNativeImageMetadata`: scans the main source set and runtime classpath, then writes `reachability-metadata.json` and `reflect-config.json`.
- `validateNativeImageMetadata`: validates the generated metadata shape and duplicate entries.
- `prepareNativeRuntimeLibraries`: extracts platform native libraries needed by native-image executables.

## Reflection Targets

By default, the plugin registers common jME runtime extension points for reflection, such as `Savable`, `AssetLoader`, `Control`, `Filter`, `SceneProcessor`, renderer interfaces, serializers, and related engine types.

Additional target types can be configured with:

```gradle
ext.jmeNativeImageAdditionalTargetTypes = [
    'org.example.MyRuntimeInterface'
]
```

Target annotations can be configured with:

```gradle
ext.jmeNativeImageAdditionalTargetAnnotations = [
    'org.example.NativeImageReflective'
]
```

## Unsafe Allocation

The plugin automatically detects direct `SafeArrayList(SomeType.class)` constructor usage in compiled bytecode and emits an `unsafeAllocated` entry for the backing array type:

```json
{
    "name": "[Lorg.example.SomeType;",
    "unsafeAllocated": true
}
```

This covers `SafeArrayList` array creation through `Array.newInstance(...)` in GraalVM Native Image.

Manual unsafe allocation entries can be added with:

```gradle
ext.jmeNativeImageAdditionalUnsafeAllocatedTypes = [
    'org.example.Foo[]',
    '[Lorg.example.Bar;'
]
```

Accepted values are binary array descriptors, Java-style array names, or class names.

## Resources

Additional resource globs can be included with:

```gradle
ext.jmeNativeImageAdditionalResourceGlobs = [
    'assets/**',
    'config/*.json'
]
```

Native Image resource patterns can be adjusted with:

```gradle
ext.jmeNativeImageIncludeResourcesPattern = '.*'
ext.jmeNativeImageExcludeResourcesPattern = '(?i).*\\.(class|jar|dylib|so|dll|jnilib)$'
```
