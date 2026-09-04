/*
 * Copyright (c) 2009-2026 jMonkeyEngine
 * All rights reserved.
 */
package com.jme3.texture.plugins;

import com.jme3.asset.AssetInfo;
import com.jme3.asset.AssetKey;
import com.jme3.asset.AssetLoader;
import com.jme3.asset.BasisTextureKey;
import com.jme3.asset.TextureKey;
import com.jme3.export.binary.ByteUtils;
import com.jme3.renderer.Caps;
import com.jme3.texture.Image;
import com.jme3.texture.Texture;
import com.jme3.texture.image.ColorSpace;
import com.jme3.util.BufferUtils;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.ngengine.basis.BasisContainer;
import org.ngengine.basis.BasisColorSpace;
import org.ngengine.basis.BasisDecodeException;
import org.ngengine.basis.BasisDecodeRequest;
import org.ngengine.basis.BasisDecodeResult;
import org.ngengine.basis.BasisDecoder;
import org.ngengine.basis.BasisDecoderFactory;
import org.ngengine.basis.BasisImageFormat;
import org.ngengine.basis.BasisTranscodeTarget;
import org.ngengine.basis.Ktx2BasisTextureType;
import org.ngengine.basis.Ktx2Container;

/**
 * Loads Basis Universal and KTX2 images and transcodes them directly to the
 * best texture format supported by the active renderer.
 * <p>
 * Prebuilt arrays retain GPU compression and their per-layer mip chains.
 * Legacy atlas images whose base name starts with {@code tileset} deliberately
 * decode to RGBA8 so the world-2D renderer can still split their cells safely
 * at runtime without neighbouring-tile bleeding.
 */
public final class BasisTextureLoader implements AssetLoader {

    private final BasisDecoder decoder;

    /**
     * Creates a loader backed by the default pure-Java decoder.
     */
    public BasisTextureLoader() {
        this(BasisDecoderFactory.createDefault());
    }

    BasisTextureLoader(BasisDecoder decoder) {
        this.decoder = decoder;
    }

    @Override
    public Object load(AssetInfo assetInfo) throws IOException {
        AssetKey<?> key = assetInfo.getKey();
        TextureKey textureKey = key instanceof TextureKey ? (TextureKey) key : null;
        boolean withAlpha = !(key instanceof BasisTextureKey)
                || ((BasisTextureKey) key).isWithAlpha();
        boolean flipY = textureKey != null && textureKey.isFlipY();
        boolean arrayRequested = textureKey != null
                && textureKey.getTextureTypeHint() == Texture.Type.TwoDimensionalArray;
        boolean forceRgba8 = flipY || (isTileset(key.getName()) && !arrayRequested);

        try (InputStream input = assetInfo.openStream()) {
            byte[] encodedData = ByteUtils.getByteContent(input);
            if (arrayRequested && !isDeclaredTextureArray(encodedData)) {
                throw new IOException("Basis/KTX2 image is not a declared texture array: "
                        + key.getName());
            }

            Set<Caps> rendererCaps = assetInfo.getManager().getRendererCaps();
            List<BasisTranscodeTarget> targetCandidates =
                    BasisTextureFormatSelector.candidates(
                            encodedData, rendererCaps, forceRgba8, withAlpha);
            if (arrayRequested) {
                targetCandidates = BasisTextureFormatSelector
                        .prioritizeLargeTextureArray(targetCandidates, withAlpha);
            }
            if (targetCandidates.isEmpty()) {
                throw new IOException("No Basis transcode target is supported by the active renderer: "
                        + key.getName());
            }
            List<BasisDecodeResult> decodedImages = null;
            BasisDecodeResult result = null;
            BasisDecodeException lastUnsupportedTarget = null;
            for (BasisTranscodeTarget target : targetCandidates) {
                BasisDecodeRequest decodeRequest = createDecodeRequest(encodedData, target);
                try {
                    decodedImages = arrayRequested
                            ? decoder.decodeAllImages(decodeRequest)
                            : null;
                    result = arrayRequested
                            ? decodedImages.get(0)
                            : decoder.decode(decodeRequest);
                    break;
                } catch (BasisDecodeException unsupportedTarget) {
                    lastUnsupportedTarget = unsupportedTarget;
                }
            }
            if (result == null) {
                throw new IOException("Basis decoder supports none of the renderer-compatible "
                        + "transcode targets for " + key.getName(), lastUnsupportedTarget);
            }
            if (!forceRgba8
                    && rendererCaps.contains(Caps.WebGL)
                    && isBlockCompressed(result.getImageFormat())
                    && !isBlockAligned(result.getWidth(), result.getHeight())) {
                BasisDecodeRequest decodeRequest = createDecodeRequest(
                        encodedData, BasisTranscodeTarget.RGBA8);
                decodedImages = arrayRequested ? decoder.decodeAllImages(decodeRequest) : null;
                result = arrayRequested ? decodedImages.get(0) : decoder.decode(decodeRequest);
            }
            if (!arrayRequested && result.getImageCount() != 1) {
                throw new IOException("Basis/KTX2 texture arrays are not supported by Texture2D: "
                        + key.getName() + " contains " + result.getImageCount() + " images");
            }
            if (arrayRequested && decodedImages.size() != result.getImageCount()) {
                throw new IOException("Basis decoder returned " + decodedImages.size()
                        + " layers for " + key.getName() + " but declared "
                        + result.getImageCount());
            }

            Image.Format imageFormat = toJmeFormat(result.getImageFormat());
            ByteBuffer pixelData = result.getPixelData();
            int[] mipMapSizes = result.getMipMapSizes();
            if (pixelData == null) {
                throw new IOException("Basis decoder returned no pixel data for " + key.getName());
            }
            if (flipY) {
                if (imageFormat != Image.Format.RGBA8) {
                    throw new IOException("Flipping a compressed Basis texture is unsupported: "
                            + key.getName());
                }
                pixelData = flipRgba8MipChain(
                        pixelData, result.getWidth(), result.getHeight(), mipMapSizes);
            }

            ColorSpace colorSpace = result.getColorSpace() == BasisColorSpace.sRGB
                    ? ColorSpace.sRGB
                    : ColorSpace.Linear;
            if (!arrayRequested) {
                return new Image(imageFormat, result.getWidth(), result.getHeight(),
                        pixelData, mipMapSizes, colorSpace);
            }

            ArrayList<ByteBuffer> layers = new ArrayList<>(result.getImageCount());
            layers.add(pixelData);
            for (int imageIndex = 1; imageIndex < result.getImageCount(); imageIndex++) {
                BasisDecodeResult layer = decodedImages.get(imageIndex);
                validateArrayLayer(result, layer, imageIndex, key.getName());
                layers.add(layer.getPixelData());
            }
            return new Image(
                    imageFormat,
                    result.getWidth(),
                    result.getHeight(),
                    result.getImageCount(),
                    layers,
                    mipMapSizes,
                    colorSpace);
        } catch (IOException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            throw new IOException("Failed to decode Basis/KTX2 image " + key.getName(), exception);
        }
    }

    private static BasisDecodeRequest createDecodeRequest(
            byte[] encodedData,
            BasisTranscodeTarget target) {
        return BasisDecodeRequest.builder(encodedData)
                .target(target)
                .allocator(BufferUtils::createByteBuffer)
                .linearColorSpace(true)
                .build();
    }

    private static void validateArrayLayer(
            BasisDecodeResult first,
            BasisDecodeResult layer,
            int imageIndex,
            String assetName) throws IOException {
        if (layer.getWidth() != first.getWidth()
                || layer.getHeight() != first.getHeight()
                || layer.getImageFormat() != first.getImageFormat()
                || layer.getColorSpace() != first.getColorSpace()
                || !java.util.Arrays.equals(layer.getMipMapSizes(), first.getMipMapSizes())
                || layer.getPixelData() == null) {
            throw new IOException("Incompatible Basis texture-array layer " + imageIndex
                    + " in " + assetName);
        }
    }

    private static boolean isDeclaredTextureArray(byte[] encodedData) {
        try {
            return Ktx2Container.parse(encodedData).getHeader().getRawLayerCount() > 0;
        } catch (RuntimeException notKtx2) {
            try {
                return BasisContainer.parse(encodedData).getTextureType()
                        == Ktx2BasisTextureType.cBASISTexType2DArray;
            } catch (RuntimeException notBasis) {
                return false;
            }
        }
    }

    private static boolean isBlockCompressed(BasisImageFormat format) {
        switch (format) {
            case BC1:
            case BC3:
            case BC4:
            case BC5:
            case BC6H:
            case BC7:
            case ETC1:
            case ETC2:
            case ETC2_NO_ALPHA:
            case ETC2_EAC_R11:
            case ETC2_EAC_RG11:
            case ASTC_HDR_4X4:
            case ASTC_HDR_6X6:
            case ASTC_LDR_4X4:
            case ASTC_LDR_5X4:
            case ASTC_LDR_5X5:
            case ASTC_LDR_6X5:
            case ASTC_LDR_6X6:
            case ASTC_LDR_8X5:
            case ASTC_LDR_8X6:
            case ASTC_LDR_8X8:
            case ASTC_LDR_10X5:
            case ASTC_LDR_10X6:
            case ASTC_LDR_10X8:
            case ASTC_LDR_10X10:
            case ASTC_LDR_12X10:
            case ASTC_LDR_12X12:
                return true;
            default:
                return false;
        }
    }

    private static boolean isBlockAligned(int width, int height) {
        return (width & 3) == 0 && (height & 3) == 0;
    }

    private static Image.Format toJmeFormat(BasisImageFormat format) throws IOException {
        switch (format) {
            case BC1:
                return Image.Format.DXT1;
            case BC3:
                return Image.Format.DXT5;
            case BC4:
                return Image.Format.RGTC1;
            case BC5:
                return Image.Format.RGTC2;
            case BC6H:
                return Image.Format.BC6H_UF16;
            case BC7:
                return Image.Format.BC7_UNORM;
            case ETC1:
            case ETC2_NO_ALPHA:
                return Image.Format.ETC1;
            case ETC2:
                return Image.Format.ETC2;
            case ASTC_LDR_4X4:
                return Image.Format.ASTC_4x4;
            case RGB_HALF:
                return Image.Format.RGB16F;
            case RGBA_HALF:
                return Image.Format.RGBA16F;
            case RGB_9E5:
                return Image.Format.RGB9E5;
            case RGB565:
                return Image.Format.RGB565;
            case RGBA8:
                return Image.Format.RGBA8;
            default:
                throw new IOException("Unsupported Basis output format: " + format);
        }
    }

    private static boolean isTileset(String name) {
        if (name == null) {
            return false;
        }
        int slash = Math.max(name.lastIndexOf('/'), name.lastIndexOf('\\'));
        return name.substring(slash + 1).toLowerCase(Locale.ROOT).startsWith("tileset");
    }

    private static ByteBuffer flipRgba8MipChain(
            ByteBuffer source, int width, int height, int[] mipMapSizes) throws IOException {
        ByteBuffer flipped = BufferUtils.createByteBuffer(source.remaining());
        ByteBuffer input = source.duplicate();
        int levels = mipMapSizes == null ? 1 : mipMapSizes.length;
        int offset = input.position();
        for (int level = 0; level < levels; level++) {
            int levelWidth = Math.max(1, width >> level);
            int levelHeight = Math.max(1, height >> level);
            int rowSize = levelWidth * 4;
            int expectedSize = rowSize * levelHeight;
            int levelSize = mipMapSizes == null ? expectedSize : mipMapSizes[level];
            if (levelSize != expectedSize || offset + levelSize > input.limit()) {
                throw new IOException("Invalid RGBA8 mip level " + level + ": expected "
                        + expectedSize + " bytes, got " + levelSize);
            }
            for (int row = levelHeight - 1; row >= 0; row--) {
                int rowOffset = offset + row * rowSize;
                for (int column = 0; column < rowSize; column++) {
                    flipped.put(input.get(rowOffset + column));
                }
            }
            offset += levelSize;
        }
        if (offset != input.limit()) {
            throw new IOException("Unexpected trailing Basis RGBA8 data: "
                    + (input.limit() - offset) + " bytes");
        }
        flipped.flip();
        return flipped;
    }
}
