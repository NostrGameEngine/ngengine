/*
 * Copyright (c) 2009-2026 jMonkeyEngine
 * All rights reserved.
 */
package com.jme3.texture.plugins;

import com.jme3.renderer.Caps;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import org.ngengine.basis.BasisContainer;
import org.ngengine.basis.BasisDecodeException;
import org.ngengine.basis.BasisTranscodeTarget;
import org.ngengine.basis.Ktx2BasisTextureFormat;
import org.ngengine.basis.Ktx2Container;

/**
 * jMonkeyEngine policy for choosing Basis Universal transcode targets.
 *
 * <p>The decoder remains platform agnostic: this class intersects the active
 * renderer capabilities with targets implemented by the decoder, orders the
 * candidates by GPU suitability, and passes each candidate explicitly to the
 * decoder.</p>
 */
final class BasisTextureFormatSelector {

    private BasisTextureFormatSelector() {
    }

    static List<BasisTranscodeTarget> candidates(
            byte[] encodedData,
            Set<Caps> caps,
            boolean forceRgba8,
            boolean withAlpha) {
        if (forceRgba8) {
            return Collections.singletonList(BasisTranscodeTarget.RGBA8);
        }
        SourceInfo source = inspect(encodedData);
        return candidates(caps, withAlpha, source.format);
    }

    static List<BasisTranscodeTarget> candidates(
            Set<Caps> caps,
            boolean withAlpha,
            Ktx2BasisTextureFormat sourceFormat) {
        ArrayList<BasisTranscodeTarget> targets = new ArrayList<>(10);
        if (sourceFormat != null && sourceFormat.isHdr()) {
            if (!withAlpha && caps.contains(Caps.TextureCompressionBPTC)) {
                targets.add(BasisTranscodeTarget.BC6H);
            }
            if (caps.contains(Caps.TextureCompressionASTC)
                    && sourceFormat == Ktx2BasisTextureFormat.cUASTC_HDR_4x4) {
                targets.add(BasisTranscodeTarget.ASTC_HDR_4X4);
            }
            if (!withAlpha && caps.contains(Caps.SharedExponentTexture)) {
                targets.add(BasisTranscodeTarget.RGB_9E5);
            }
            if (caps.contains(Caps.HalfFloatTexture)) {
                if (!withAlpha) {
                    targets.add(BasisTranscodeTarget.RGB_HALF);
                }
                targets.add(BasisTranscodeTarget.RGBA_HALF);
            }
            targets.add(BasisTranscodeTarget.RGBA8);
            return targets;
        }

        if (!withAlpha) {
            addOpaqueCompressedTargets(targets, caps);
        }
        addAlphaCompressedTargets(targets, caps, sourceFormat);
        if (!withAlpha) {
            targets.add(BasisTranscodeTarget.RGB565);
        }
        targets.add(BasisTranscodeTarget.RGBA8);
        return targets;
    }

    private static void addOpaqueCompressedTargets(
            List<BasisTranscodeTarget> targets,
            Set<Caps> caps) {
        if (caps.contains(Caps.TextureCompressionS3TC)) {
            targets.add(BasisTranscodeTarget.BC1);
        }
        if (caps.contains(Caps.TextureCompressionETC2)) {
            targets.add(BasisTranscodeTarget.ETC2_NO_ALPHA);
        }
        if (caps.contains(Caps.TextureCompressionETC1)) {
            targets.add(BasisTranscodeTarget.ETC1);
        }
    }

    private static void addAlphaCompressedTargets(
            List<BasisTranscodeTarget> targets,
            Set<Caps> caps,
            Ktx2BasisTextureFormat sourceFormat) {
        if (caps.contains(Caps.WebGL)) {
            addWebAlphaCompressedTargets(targets, caps, sourceFormat);
            return;
        }
        if (caps.contains(Caps.TextureCompressionBPTC)) {
            targets.add(BasisTranscodeTarget.BC7);
        }
        if (caps.contains(Caps.TextureCompressionS3TC)) {
            targets.add(BasisTranscodeTarget.BC3);
        }
        if (caps.contains(Caps.TextureCompressionASTC)
                && supportsAstc4x4(sourceFormat)) {
            targets.add(BasisTranscodeTarget.ASTC_LDR_4X4);
        }
        if (caps.contains(Caps.TextureCompressionETC2)) {
            targets.add(BasisTranscodeTarget.ETC2);
        }
    }

    /**
     * Browser startup is especially sensitive to CPU transcoding cost. Prefer
     * formats with direct or inexpensive transcode paths there, while retaining
     * BC7 as a quality fallback when it is the only compressed alpha format.
     */
    private static void addWebAlphaCompressedTargets(
            List<BasisTranscodeTarget> targets,
            Set<Caps> caps,
            Ktx2BasisTextureFormat sourceFormat) {
        if (caps.contains(Caps.TextureCompressionASTC)
                && supportsAstc4x4(sourceFormat)) {
            targets.add(BasisTranscodeTarget.ASTC_LDR_4X4);
        }
        if (caps.contains(Caps.TextureCompressionS3TC)) {
            targets.add(BasisTranscodeTarget.BC3);
        }
        if (caps.contains(Caps.TextureCompressionETC2)) {
            targets.add(BasisTranscodeTarget.ETC2);
        }
        if (caps.contains(Caps.TextureCompressionBPTC)) {
            targets.add(BasisTranscodeTarget.BC7);
        }
    }

    /**
     * Reorders otherwise compatible targets for large texture arrays.
     *
     * <p>Pure-Java BC7 endpoint optimization is deliberately kept behind the
     * direct UASTC-to-ASTC path and the cheaper desktop/web block formats. A
     * regular texture still prefers BC7, while an atlas with hundreds of
     * layers avoids spending seconds optimizing every block before the first
     * frame can be shown.</p>
     */
    static List<BasisTranscodeTarget> prioritizeLargeTextureArray(
            List<BasisTranscodeTarget> candidates,
            boolean withAlpha) {
        if (candidates.size() < 2) {
            return candidates;
        }
        ArrayList<BasisTranscodeTarget> ordered = new ArrayList<>(candidates);
        ordered.sort(Comparator.comparingInt(
                target -> arrayTargetGroup(target, withAlpha) * 10
                        + arrayTargetPriority(target)));
        return ordered;
    }

    private static int arrayTargetGroup(
            BasisTranscodeTarget target,
            boolean withAlpha) {
        if (withAlpha) {
            return isUncompressed(target) ? 1 : 0;
        }
        if (!target.supportsAlpha()) {
            return isUncompressed(target) ? 2 : 0;
        }
        return isUncompressed(target) ? 3 : 1;
    }

    private static boolean isUncompressed(BasisTranscodeTarget target) {
        switch (target) {
            case RGB565:
            case BGR565:
            case RGBA4444:
            case RGB_HALF:
            case RGBA_HALF:
            case RGB_9E5:
            case RGBA8:
                return true;
            default:
                return false;
        }
    }

    private static int arrayTargetPriority(BasisTranscodeTarget target) {
        switch (target) {
            case ASTC_LDR_4X4:
                return 0;
            case BC1:
            case BC3:
                return 1;
            case ETC1:
            case ETC2:
            case ETC2_NO_ALPHA:
                return 2;
            case BC7:
                return 3;
            case RGBA8:
                return 4;
            default:
                return 5;
        }
    }

    private static SourceInfo inspect(byte[] encodedData) {
        try {
            if (isKtx2(encodedData)) {
                Ktx2Container container = Ktx2Container.parse(encodedData);
                return new SourceInfo(container.getBasisTextureFormat());
            }
            BasisContainer container = BasisContainer.parse(encodedData);
            return new SourceInfo(container.getTextureFormat());
        } catch (BasisDecodeException | IllegalArgumentException invalidContainer) {
            // The decoder reports the authoritative container error. Until
            // then, preserve alpha and use only broadly supported targets.
            return SourceInfo.UNKNOWN;
        }
    }

    private static boolean supportsAstc4x4(Ktx2BasisTextureFormat sourceFormat) {
        return sourceFormat == Ktx2BasisTextureFormat.cUASTC_LDR_4x4
                || (sourceFormat != null
                && sourceFormat.isXUastcLdr()
                && sourceFormat.getBlockWidth() == 4
                && sourceFormat.getBlockHeight() == 4);
    }

    private static boolean isKtx2(byte[] data) {
        return data != null
                && data.length >= 12
                && data[0] == (byte) 0xAB
                && data[1] == 0x4B
                && data[2] == 0x54
                && data[3] == 0x58
                && data[4] == 0x20
                && data[5] == 0x32
                && data[6] == 0x30
                && data[7] == (byte) 0xBB
                && data[8] == 0x0D
                && data[9] == 0x0A
                && data[10] == 0x1A
                && data[11] == 0x0A;
    }

    private static final class SourceInfo {
        private static final SourceInfo UNKNOWN = new SourceInfo(null);

        private final Ktx2BasisTextureFormat format;

        private SourceInfo(Ktx2BasisTextureFormat format) {
            this.format = format;
        }
    }
}
