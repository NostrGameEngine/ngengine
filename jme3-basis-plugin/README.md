# NGE Tiled Basis Texture Encoder Plugin

`org.ngengine.tiled-basis-texture-encoder` extends the jBasis Universal Gradle
encoder. It preserves the `basisTextures` DSL and automatically converts atlas
images referenced by TSX or inline TMX tilesets into mipmapped Basis/KTX2
2D-array textures. Each Tiled cell becomes one independent array layer, so its
mipmap chain cannot bleed into an adjacent tile.

```groovy
plugins {
    id 'org.ngengine.tiled-basis-texture-encoder' version '0.3.0-SNAPSHOT'
}

basisTextures {
    basisuArguments = ['-ktx2', '-uastc', '-mipmap', '-y_flip', '-quiet']
    handleTiledTilesets = true // default
}
```

The plugin publishes both its implementation artifact and Gradle marker to the
same Maven Central release and snapshot repositories used by the NGE Native
Image plugin.
