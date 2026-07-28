/*
 * Copyright (c) 2009-2026 jMonkeyEngine
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 * * Redistributions of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 *
 * * Redistributions in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer in the
 *   documentation and/or other materials provided with the distribution.
 *
 * * Neither the name of 'jMonkeyEngine' nor the names of its contributors
 *   may be used to endorse or promote products derived from this software
 *   without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
 * TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.jme3.system.lwjgl;

import com.jme3.asset.AssetManager;
import com.jme3.asset.TextureKey;
import com.jme3.math.ColorRGBA;
import com.jme3.texture.Image;
import com.jme3.texture.Texture;
import com.jme3.texture.image.ColorSpace;
import com.jme3.texture.image.ImageRaster;
import com.jme3.util.BufferUtils;
import com.jme3.util.MipMapGenerator;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.lwjgl.sdl.SDL_Surface;

import static org.lwjgl.sdl.SDLError.SDL_GetError;
import static org.lwjgl.sdl.SDLPixels.SDL_PIXELFORMAT_RGBA32;
import static org.lwjgl.sdl.SDLSurface.SDL_AddSurfaceAlternateImage;
import static org.lwjgl.sdl.SDLSurface.SDL_CreateSurface;
import static org.lwjgl.sdl.SDLSurface.SDL_DestroySurface;
import static org.lwjgl.sdl.SDLVideo.SDL_SetWindowIcon;

/**
 * Converts application icon settings to SDL surfaces without linking AWT.
 */
final class SdlWindowIcon {

    private static final Logger LOGGER = Logger.getLogger(SdlWindowIcon.class.getName());
    private static final int[] STANDARD_SIZES = {256, 128, 64, 32, 16};
    private static final int BASE_SIZE = 32;
    private static final String BUFFERED_IMAGE_CLASS = "java.awt.image.BufferedImage";

    private SdlWindowIcon() {
    }

    static boolean hasAssetPaths(Object[] icons) {
        if (icons != null) {
            for (Object icon : icons) {
                if (icon instanceof String) {
                    return true;
                }
            }
        }
        return false;
    }

    static void set(long window, Object[] icons, AssetManager assetManager) {
        List<IconImage> images = prepareImages(icons, assetManager);
        if (images.isEmpty()) {
            return;
        }

        IconImage baseImage = closestImage(images, BASE_SIZE, BASE_SIZE);
        SDL_Surface baseSurface = createSurface(baseImage);
        if (baseSurface == null) {
            return;
        }

        try {
            for (IconImage image : images) {
                if (image == baseImage) {
                    continue;
                }

                SDL_Surface alternate = createSurface(image);
                if (alternate == null) {
                    continue;
                }
                try {
                    if (!SDL_AddSurfaceAlternateImage(baseSurface, alternate)) {
                        LOGGER.log(Level.WARNING, "Unable to add SDL window icon representation: {0}",
                                SDL_GetError());
                    }
                } finally {
                    SDL_DestroySurface(alternate);
                }
            }

            if (!SDL_SetWindowIcon(window, baseSurface)) {
                LOGGER.log(Level.WARNING, "Unable to set SDL window icon: {0}", SDL_GetError());
            }
        } finally {
            SDL_DestroySurface(baseSurface);
        }
    }

    static List<IconImage> prepareImages(Object[] icons, AssetManager assetManager) {
        Map<String, IconImage> imagesBySize = new LinkedHashMap<>();
        if (icons == null) {
            return new ArrayList<>();
        }

        for (Object icon : icons) {
            IconImage image = resolveImage(icon, assetManager);
            if (image != null) {
                imagesBySize.putIfAbsent(sizeKey(image.image), image);
            }
        }

        List<IconImage> suppliedImages = new ArrayList<>(imagesBySize.values());
        for (int size : STANDARD_SIZES) {
            String key = sizeKey(size, size);
            if (imagesBySize.containsKey(key)) {
                continue;
            }

            IconImage source = closestScalableImage(suppliedImages, size, size);
            if (source == null) {
                continue;
            }

            try {
                Image scaled = MipMapGenerator.scaleImage(source.image, size, size);
                imagesBySize.put(key, new IconImage(scaled, source.flipY));
            } catch (RuntimeException exception) {
                LOGGER.log(Level.WARNING, "Unable to scale SDL window icon to " + size + "x" + size, exception);
            }
        }

        return new ArrayList<>(imagesBySize.values());
    }

    private static IconImage resolveImage(Object icon, AssetManager assetManager) {
        if (icon instanceof String) {
            if (assetManager == null) {
                return null;
            }

            String path = (String) icon;
            if (path.isEmpty()) {
                return null;
            }

            try {
                TextureKey key = new TextureKey(path, false);
                Texture texture = assetManager.loadTexture(key);
                return texture == null ? null : textureImage(texture, false);
            } catch (RuntimeException exception) {
                LOGGER.log(Level.WARNING, "Unable to load SDL window icon asset " + path, exception);
                return null;
            }
        }

        if (icon instanceof Texture) {
            Texture texture = (Texture) icon;
            boolean flipY = texture.getKey() instanceof TextureKey
                    && ((TextureKey) texture.getKey()).isFlipY();
            return textureImage(texture, flipY);
        }

        if (icon instanceof Image) {
            return checkedImage((Image) icon, false);
        }

        return bufferedImage(icon);
    }

    private static IconImage textureImage(Texture texture, boolean flipY) {
        return checkedImage(texture.getImage(), flipY);
    }

    private static IconImage checkedImage(Image image, boolean flipY) {
        if (image == null
                || image.getWidth() < 1
                || image.getHeight() < 1
                || image.getDepth() > 1
                || image.getData() == null
                || image.getData().isEmpty()
                || image.getData(0) == null
                || !ImageRaster.isSupported(image.getFormat())) {
            if (image != null) {
                LOGGER.log(Level.WARNING, "Unsupported SDL window icon image: {0}", image);
            }
            return null;
        }
        return new IconImage(image, flipY);
    }

    private static IconImage bufferedImage(Object icon) {
        Class<?> type = icon == null ? null : icon.getClass();
        while (type != null && !BUFFERED_IMAGE_CLASS.equals(type.getName())) {
            type = type.getSuperclass();
        }
        if (type == null) {
            if (icon != null) {
                LOGGER.log(Level.WARNING, "Unsupported SDL window icon type: {0}", icon.getClass().getName());
            }
            return null;
        }

        try {
            Method getWidth = type.getMethod("getWidth");
            Method getHeight = type.getMethod("getHeight");
            Method getRgb = type.getMethod("getRGB", int.class, int.class, int.class, int.class,
                    int[].class, int.class, int.class);
            int width = (Integer) getWidth.invoke(icon);
            int height = (Integer) getHeight.invoke(icon);
            if (width < 1 || height < 1) {
                return null;
            }

            int pixelCount = Math.multiplyExact(width, height);
            int[] colors = (int[]) getRgb.invoke(icon, 0, 0, width, height, null, 0, width);
            ByteBuffer data = BufferUtils.createByteBuffer(Math.multiplyExact(pixelCount, 4));
            for (int color : colors) {
                data.put((byte) ((color >> 16) & 0xFF));
                data.put((byte) ((color >> 8) & 0xFF));
                data.put((byte) (color & 0xFF));
                data.put((byte) ((color >> 24) & 0xFF));
            }
            data.flip();
            return new IconImage(new Image(Image.Format.RGBA8, width, height, data, ColorSpace.sRGB), false);
        } catch (ReflectiveOperationException | RuntimeException exception) {
            LOGGER.log(Level.WARNING, "Unable to read BufferedImage SDL window icon", exception);
            return null;
        }
    }

    private static IconImage closestScalableImage(List<IconImage> images, int width, int height) {
        IconImage closest = null;
        double closestDistance = Double.POSITIVE_INFINITY;

        for (IconImage image : images) {
            if (!MipMapGenerator.canGenerateMipmaps(image.image)) {
                continue;
            }

            double distance = sizeDistance(image.image, width, height);
            if (distance < closestDistance
                    || (distance == closestDistance && isLarger(image.image, closest == null ? null : closest.image))) {
                closest = image;
                closestDistance = distance;
            }
        }
        return closest;
    }

    private static IconImage closestImage(List<IconImage> images, int width, int height) {
        IconImage closest = null;
        double closestDistance = Double.POSITIVE_INFINITY;

        for (IconImage image : images) {
            double distance = sizeDistance(image.image, width, height);
            if (distance < closestDistance
                    || (distance == closestDistance && isLarger(image.image, closest == null ? null : closest.image))) {
                closest = image;
                closestDistance = distance;
            }
        }
        return closest;
    }

    private static double sizeDistance(Image image, int width, int height) {
        return Math.abs(Math.log((double) image.getWidth() / width))
                + Math.abs(Math.log((double) image.getHeight() / height));
    }

    private static boolean isLarger(Image candidate, Image current) {
        return current == null
                || (long) candidate.getWidth() * candidate.getHeight()
                > (long) current.getWidth() * current.getHeight();
    }

    private static SDL_Surface createSurface(IconImage icon) {
        Image image = icon.image;
        SDL_Surface surface = SDL_CreateSurface(image.getWidth(), image.getHeight(), SDL_PIXELFORMAT_RGBA32);
        if (surface == null) {
            LOGGER.log(Level.WARNING, "Unable to create SDL window icon surface: {0}", SDL_GetError());
            return null;
        }

        ByteBuffer pixels = surface.pixels();
        if (pixels == null) {
            SDL_DestroySurface(surface);
            return null;
        }

        try {
            ImageRaster raster = ImageRaster.create(image, 0, 0, false);
            ColorRGBA color = new ColorRGBA();
            int pitch = surface.pitch();

            for (int y = 0; y < image.getHeight(); y++) {
                int sourceY = icon.flipY ? image.getHeight() - 1 - y : y;
                pixels.position(y * pitch);
                for (int x = 0; x < image.getWidth(); x++) {
                    raster.getPixel(x, sourceY, color);
                    pixels.put(toByte(color.r));
                    pixels.put(toByte(color.g));
                    pixels.put(toByte(color.b));
                    pixels.put(toByte(color.a));
                }
            }
            return surface;
        } catch (RuntimeException exception) {
            SDL_DestroySurface(surface);
            LOGGER.log(Level.WARNING, "Unable to convert SDL window icon image", exception);
            return null;
        }
    }

    private static byte toByte(float value) {
        return (byte) Math.round(Math.max(0f, Math.min(1f, value)) * 255f);
    }

    private static String sizeKey(Image image) {
        return sizeKey(image.getWidth(), image.getHeight());
    }

    private static String sizeKey(int width, int height) {
        return width + "x" + height;
    }

    static final class IconImage {
        final Image image;
        final boolean flipY;

        IconImage(Image image, boolean flipY) {
            this.image = image;
            this.flipY = flipY;
        }
    }
}
