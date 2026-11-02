# NGE test data

This module contains the Tiled maps and other example assets maintained by
NostrGameEngine. Asset paths, including `tmx/`, remain unchanged on the classpath.
Keep the licenses and attribution files alongside their assets.

Official jMonkeyEngine assets come from `org.jmonkeyengine:jme3-testdata`, whose
version is declared in `gradle/libs.versions.toml`. They are consumed directly;
this module does not copy or republish that artifact.

`nge-examples` uses both asset sets. `jme3-examples` uses only the official
artifact. World2D tests depend on this module for their Tiled fixtures.
