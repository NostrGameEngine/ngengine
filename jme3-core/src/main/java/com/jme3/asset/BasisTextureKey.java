/*
 * Copyright (c) 2009-2026 jMonkeyEngine
 * All rights reserved.
 */
package com.jme3.asset;

import com.jme3.export.InputCapsule;
import com.jme3.export.JmeExporter;
import com.jme3.export.JmeImporter;
import com.jme3.export.OutputCapsule;
import java.io.IOException;

/**
 * Texture key for Basis Universal and KTX2 assets.
 *
 * <p>Alpha-preserving output is required by default. Applications may opt out
 * for known-opaque textures so the loader can prefer smaller opaque GPU
 * formats while retaining alpha-capable formats as fallbacks.</p>
 */
public class BasisTextureKey extends TextureKey {
    private boolean withAlpha = true;

    public BasisTextureKey() {
    }

    public BasisTextureKey(String name) {
        super(name);
    }

    public BasisTextureKey(String name, boolean flipY) {
        super(name, flipY);
    }

    public BasisTextureKey(String name, boolean flipY, boolean withAlpha) {
        super(name, flipY);
        this.withAlpha = withAlpha;
    }

    /**
     * Returns whether the selected transcode target must preserve alpha.
     *
     * @return {@code true} by default
     */
    public boolean isWithAlpha() {
        return withAlpha;
    }

    /**
     * Selects whether alpha-preserving transcode targets are required.
     *
     * @param withAlpha {@code true} to exclude alpha-less targets;
     *                  {@code false} to prefer opaque targets first
     */
    public void setWithAlpha(boolean withAlpha) {
        this.withAlpha = withAlpha;
    }

    @Override
    public boolean equals(Object object) {
        return super.equals(object)
                && withAlpha == ((BasisTextureKey) object).withAlpha;
    }

    @Override
    public int hashCode() {
        return 31 * super.hashCode() + (withAlpha ? 1 : 0);
    }

    @Override
    public void write(JmeExporter exporter) throws IOException {
        super.write(exporter);
        OutputCapsule capsule = exporter.getCapsule(this);
        capsule.write(withAlpha, "with_alpha", true);
    }

    @Override
    public void read(JmeImporter importer) throws IOException {
        super.read(importer);
        InputCapsule capsule = importer.getCapsule(this);
        withAlpha = capsule.readBoolean("with_alpha", true);
    }
}
