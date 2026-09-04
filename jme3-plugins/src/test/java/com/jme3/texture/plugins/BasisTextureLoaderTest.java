/*
 * Copyright (c) 2009-2026 jMonkeyEngine
 * All rights reserved.
 */
package com.jme3.texture.plugins;

import com.jme3.asset.AssetInfo;
import com.jme3.asset.BasisTextureKey;
import com.jme3.asset.DesktopAssetManager;
import com.jme3.asset.TextureKey;
import com.jme3.renderer.Caps;
import com.jme3.texture.Image;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.ngengine.basis.BasisColorSpace;
import org.ngengine.basis.BasisDecodeRequest;
import org.ngengine.basis.BasisDecodeResult;
import org.ngengine.basis.BasisDecoder;
import org.ngengine.basis.BasisTranscodeTarget;
import org.ngengine.basis.Ktx2BasisTextureFormat;

public class BasisTextureLoaderTest {

    @Test
    public void choosesBestRendererSupportedTargetForRegularTexture() throws Exception {
        CapturingDecoder decoder = new CapturingDecoder(result(
                ImageData.singlePixel(), org.ngengine.basis.BasisImageFormat.BC7, 1, 1));
        DesktopAssetManager assetManager = new DesktopAssetManager(false);
        assetManager.setRendererCaps(EnumSet.of(
                Caps.TextureCompressionS3TC, Caps.TextureCompressionBPTC));

        Image image = load(decoder, assetManager, new TextureKey("Textures/console.png.basis", false));

        Assertions.assertEquals(Image.Format.BC7_UNORM, image.getFormat());
        Assertions.assertEquals(BasisTranscodeTarget.BC7, decoder.request.getTarget());
    }

    @Test
    public void forcesTilesetAtlasToRgba8ForIndependentMipGeneration() throws Exception {
        CapturingDecoder decoder = new CapturingDecoder(result(
                ImageData.singlePixel(), org.ngengine.basis.BasisImageFormat.RGBA8, 1, 1));
        DesktopAssetManager assetManager = new DesktopAssetManager(false);
        assetManager.setRendererCaps(EnumSet.of(Caps.TextureCompressionBPTC));

        Image image = load(decoder, assetManager,
                new TextureKey("Maps/tilesetSERVER.png.basis", false));

        Assertions.assertEquals(Image.Format.RGBA8, image.getFormat());
        Assertions.assertEquals(BasisTranscodeTarget.RGBA8, decoder.request.getTarget());
    }

    @Test
    public void fallsBackToRgba8ForUnalignedWebGlCompressedTexture() throws Exception {
        RetryDecoder decoder = new RetryDecoder();
        DesktopAssetManager assetManager = new DesktopAssetManager(false);
        assetManager.setRendererCaps(EnumSet.of(Caps.WebGL, Caps.TextureCompressionBPTC));

        Image image = load(decoder, assetManager,
                new TextureKey("Textures/unaligned.png.basis", false));

        Assertions.assertEquals(Image.Format.RGBA8, image.getFormat());
        Assertions.assertEquals(2, decoder.requests.size());
        Assertions.assertEquals(BasisTranscodeTarget.BC7,
                decoder.requests.get(0).getTarget());
        Assertions.assertEquals(BasisTranscodeTarget.RGBA8,
                decoder.requests.get(1).getTarget());
    }

    @Test
    public void selectsAlphaPreservingS3tcTargetInEnginePolicy() {
        Assertions.assertEquals(
                Arrays.asList(BasisTranscodeTarget.BC3, BasisTranscodeTarget.RGBA8),
                BasisTextureFormatSelector.candidates(
                        EnumSet.of(Caps.TextureCompressionS3TC),
                        true,
                        Ktx2BasisTextureFormat.cETC1S));
    }

    @Test
    public void opaqueKeyPrefersOpaqueThenAlphaCompressedThenRasterFallbacks()
            throws Exception {
        CapturingDecoder decoder = new CapturingDecoder(result(
                ImageData.singlePixel(), org.ngengine.basis.BasisImageFormat.BC1, 1, 1));
        DesktopAssetManager assetManager = new DesktopAssetManager(false);
        assetManager.setRendererCaps(EnumSet.of(
                Caps.TextureCompressionS3TC,
                Caps.TextureCompressionBPTC,
                Caps.TextureCompressionETC2));
        BasisTextureKey key = new BasisTextureKey("Textures/opaque.ktx2", false);
        key.setWithAlpha(false);

        Image image = load(decoder, assetManager, key);

        Assertions.assertEquals(Image.Format.DXT1, image.getFormat());
        Assertions.assertEquals(BasisTranscodeTarget.BC1, decoder.request.getTarget());
        Assertions.assertEquals(
                Arrays.asList(
                        BasisTranscodeTarget.BC1,
                        BasisTranscodeTarget.ETC2_NO_ALPHA,
                        BasisTranscodeTarget.BC7,
                        BasisTranscodeTarget.BC3,
                        BasisTranscodeTarget.ETC2,
                        BasisTranscodeTarget.RGB565,
                        BasisTranscodeTarget.RGBA8),
                BasisTextureFormatSelector.candidates(
                        EnumSet.of(
                                Caps.TextureCompressionS3TC,
                                Caps.TextureCompressionBPTC,
                                Caps.TextureCompressionETC2),
                        false,
                        Ktx2BasisTextureFormat.cETC1S));
    }

    @Test
    public void selectsAstcBeforeEtcForMobileUastc() {
        Assertions.assertEquals(
                Arrays.asList(
                        BasisTranscodeTarget.ASTC_LDR_4X4,
                        BasisTranscodeTarget.ETC2,
                        BasisTranscodeTarget.RGBA8),
                BasisTextureFormatSelector.candidates(
                        EnumSet.of(
                                Caps.TextureCompressionASTC,
                                Caps.TextureCompressionETC2),
                        true,
                        Ktx2BasisTextureFormat.cUASTC_LDR_4x4));
    }

    @Test
    public void webGlPrefersFastAlphaTargetsBeforeBc7() {
        Assertions.assertEquals(
                Arrays.asList(
                        BasisTranscodeTarget.ASTC_LDR_4X4,
                        BasisTranscodeTarget.BC3,
                        BasisTranscodeTarget.ETC2,
                        BasisTranscodeTarget.BC7,
                        BasisTranscodeTarget.RGBA8),
                BasisTextureFormatSelector.candidates(
                        EnumSet.of(
                                Caps.WebGL,
                                Caps.TextureCompressionASTC,
                                Caps.TextureCompressionS3TC,
                                Caps.TextureCompressionETC2,
                                Caps.TextureCompressionBPTC),
                        true,
                        Ktx2BasisTextureFormat.cUASTC_LDR_4x4));
    }

    @Test
    public void prefersFastCompressedTargetsForLargeTextureArrays() {
        Assertions.assertEquals(
                Arrays.asList(
                        BasisTranscodeTarget.ASTC_LDR_4X4,
                        BasisTranscodeTarget.BC3,
                        BasisTranscodeTarget.ETC2,
                        BasisTranscodeTarget.BC7,
                        BasisTranscodeTarget.RGBA8),
                BasisTextureFormatSelector.prioritizeLargeTextureArray(
                        Arrays.asList(
                                BasisTranscodeTarget.BC7,
                                BasisTranscodeTarget.BC3,
                                BasisTranscodeTarget.ASTC_LDR_4X4,
                                BasisTranscodeTarget.ETC2,
                                BasisTranscodeTarget.RGBA8),
                        true));
    }

    @Test
    public void selectsCompressedHdrTargetWhenBptcIsAvailable() {
        Assertions.assertEquals(
                Arrays.asList(
                        BasisTranscodeTarget.BC6H,
                        BasisTranscodeTarget.RGBA8),
                BasisTextureFormatSelector.candidates(
                        EnumSet.of(Caps.TextureCompressionBPTC),
                        false,
                        Ktx2BasisTextureFormat.cUASTC_HDR_4x4));
    }

    @Test
    public void flipsEveryRgba8MipLevelWhenRequested() throws Exception {
        byte[] rgba = new byte[] {
                1, 2, 3, 4, 5, 6, 7, 8,
                9, 10, 11, 12, 13, 14, 15, 16,
                17, 18, 19, 20
        };
        CapturingDecoder decoder = new CapturingDecoder(new BasisDecodeResult(
                2, 2, ByteBuffer.wrap(rgba), org.ngengine.basis.BasisImageFormat.RGBA8,
                new int[] {16, 4}, BasisColorSpace.sRGB));
        DesktopAssetManager assetManager = new DesktopAssetManager(false);

        Image image = load(decoder, assetManager, new TextureKey("Textures/test.ktx2", true));

        byte[] actual = new byte[rgba.length];
        image.getData(0).duplicate().get(actual);
        Assertions.assertArrayEquals(new byte[] {
                9, 10, 11, 12, 13, 14, 15, 16,
                1, 2, 3, 4, 5, 6, 7, 8,
                17, 18, 19, 20
        }, actual);
        Assertions.assertEquals(BasisTranscodeTarget.RGBA8, decoder.request.getTarget());
    }

    private static Image load(
            BasisDecoder decoder, DesktopAssetManager assetManager, TextureKey key)
            throws Exception {
        BasisTextureLoader loader = new BasisTextureLoader(decoder);
        return (Image) loader.load(new AssetInfo(assetManager, key) {
            @Override
            public InputStream openStream() {
                return new ByteArrayInputStream(new byte[] {0, 1, 2, 3});
            }
        });
    }

    private static BasisDecodeResult result(
            byte[] data, org.ngengine.basis.BasisImageFormat format, int width, int height) {
        return new BasisDecodeResult(width, height, ByteBuffer.wrap(data), format,
                null, BasisColorSpace.sRGB);
    }

    private static final class CapturingDecoder implements BasisDecoder {
        private final BasisDecodeResult result;
        private BasisDecodeRequest request;

        private CapturingDecoder(BasisDecodeResult result) {
            this.result = result;
        }

        @Override
        public boolean isAvailable() {
            return true;
        }

        @Override
        public String getBackendName() {
            return "test";
        }

        @Override
        public BasisDecodeResult decode(BasisDecodeRequest request) {
            this.request = request;
            return result;
        }
    }

    private static final class RetryDecoder implements BasisDecoder {
        private final ArrayList<BasisDecodeRequest> requests = new ArrayList<>();

        @Override
        public boolean isAvailable() {
            return true;
        }

        @Override
        public String getBackendName() {
            return "test";
        }

        @Override
        public BasisDecodeResult decode(BasisDecodeRequest request) {
            requests.add(request);
            if (request.getTarget() == BasisTranscodeTarget.RGBA8) {
                return result(new byte[6 * 4 * 4],
                        org.ngengine.basis.BasisImageFormat.RGBA8, 6, 4);
            }
            return result(new byte[32], org.ngengine.basis.BasisImageFormat.BC7, 6, 4);
        }
    }

    private static final class ImageData {
        private ImageData() {
        }

        private static byte[] singlePixel() {
            return new byte[] {0, 0, 0, 0};
        }
    }
}
