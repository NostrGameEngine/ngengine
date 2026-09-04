package org.ngengine.gradle.basis;

import java.awt.image.BufferedImage;
import java.nio.file.Path;

final class TiledAtlas {
    private final Path imagePath;
    private final String resourcePath;
    private final int tileWidth;
    private final int tileHeight;
    private final int margin;
    private final int spacing;
    private final int columns;
    private final int tileCount;
    private final BufferedImage image;

    TiledAtlas(
            Path imagePath,
            String resourcePath,
            int tileWidth,
            int tileHeight,
            int margin,
            int spacing,
            int columns,
            int tileCount,
            BufferedImage image) {
        this.imagePath = imagePath;
        this.resourcePath = resourcePath;
        this.tileWidth = tileWidth;
        this.tileHeight = tileHeight;
        this.margin = margin;
        this.spacing = spacing;
        this.columns = columns;
        this.tileCount = tileCount;
        this.image = image;
    }

    Path getImagePath() {
        return imagePath;
    }

    String getResourcePath() {
        return resourcePath;
    }

    int getTileWidth() {
        return tileWidth;
    }

    int getTileHeight() {
        return tileHeight;
    }

    int getMargin() {
        return margin;
    }

    int getSpacing() {
        return spacing;
    }

    int getColumns() {
        return columns;
    }

    int getTileCount() {
        return tileCount;
    }

    BufferedImage getImage() {
        return image;
    }

    boolean hasSameLayout(TiledAtlas other) {
        return tileWidth == other.tileWidth
                && tileHeight == other.tileHeight
                && margin == other.margin
                && spacing == other.spacing
                && columns == other.columns
                && tileCount == other.tileCount;
    }
}
