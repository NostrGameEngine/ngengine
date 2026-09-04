package org.ngengine.gradle.basis;

import javax.inject.Inject;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.provider.Property;
import org.ngengine.basis.gradle.BasisTextureEncoderExtension;

/**
 * Basis encoder settings with automatic Tiled atlas preprocessing.
 *
 * <p>All ordinary encoder properties are inherited from the jBasis Universal
 * plugin. Tiled handling is enabled by default.</p>
 */
public abstract class NgeBasisTextureEncoderExtension extends BasisTextureEncoderExtension {

    /**
     * Creates the extension and establishes its defaults.
     *
     * @param objects Gradle object factory
     */
    @Inject
    public NgeBasisTextureEncoderExtension(ObjectFactory objects) {
        super(objects);
        getHandleTiledTilesets().convention(true);
        getDimensionAlignment().convention(4);
    }

    /**
     * Controls whether atlas images referenced by TSX or inline TMX tilesets
     * are split into one Basis texture-array layer per tile.
     *
     * @return handling toggle, {@code true} by default
     */
    public abstract Property<Boolean> getHandleTiledTilesets();
}
