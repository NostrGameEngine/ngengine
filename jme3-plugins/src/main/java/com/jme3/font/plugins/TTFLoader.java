package com.jme3.font.plugins;

import com.jme3.asset.AssetInfo;
import com.jme3.asset.AssetKey;
import com.jme3.asset.AssetLoader;
import com.jme3.asset.AssetManager;
import com.jme3.export.binary.ByteUtils;
import com.jme3.font.BitmapCharacter;
import com.jme3.font.BitmapCharacterSet;
import com.jme3.font.BitmapFont;
import com.jme3.material.Material;
import com.jme3.material.MaterialDef;
import com.jme3.material.RenderState.BlendMode;
import com.jme3.texture.Image;
import com.jme3.texture.Texture;
import com.jme3.texture.Texture2D;
import com.jme3.texture.image.ColorSpace;
import com.jme3.util.BufferUtils;

import org.ngengine.stbttf.FontMetrics;
import org.ngengine.stbttf.GlyphBitmap;
import org.ngengine.stbttf.GlyphBounds;
import org.ngengine.stbttf.StbFont;
import org.ngengine.stbttf.StbGlyph;
import org.ngengine.stbttf.StbTrueType;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.BitSet;
import java.util.Objects;

/** Rasterizes supported BMP characters from a TrueType font into a bitmap atlas. */
public class TTFLoader implements AssetLoader {
    public static final int DEFAULT_FONT_SIZE = 64;

    private static final String DEFAULT_SYMBOLS = "\u2013\u2014\u2018\u2019\u201c\u201d\u2022\u2026\u20ac\u20bf";
    private static final BitSet DEFAULT_CHARACTERS = defaultCharacters();
    private static final int MAX_ATLAS_SIZE = 8192;

    /** An inclusive interval of Unicode code points for a bitmap font. */
    public static final class CharacterRange {
        private final int first;
        private final int last;

        public CharacterRange(int first, int last) {
            if (first < Character.MIN_VALUE || last > Character.MAX_VALUE || first > last) {
                throw new IllegalArgumentException("Expected an inclusive BMP character range");
            }
            this.first = first;
            this.last = last;
        }
    }

    public static class BitmapFontKey extends AssetKey<BitmapFont> {
        private final int fontSize;
        private final BitSet characters;

        public BitmapFontKey(String name) {
            this(name, DEFAULT_FONT_SIZE);
        }

        public BitmapFontKey(String name, int fontSize) {
            this(name, fontSize, DEFAULT_CHARACTERS);
        }

        /**
         * Bakes only glyphs in the supplied inclusive ranges, rather than using
         * the default Latin repertoire. Ranges may overlap or leave gaps.
         * Characters missing from the font are skipped.
         * BitmapText currently addresses characters in the Basic Multilingual Plane only.
         *
         * @param name the TrueType asset path
         * @param fontSize the height in pixels
         * @param ranges one or more BMP intervals, in any order
         */
        public BitmapFontKey(String name, int fontSize, CharacterRange... ranges) {
            this(name, fontSize, requestedCharacters(ranges));
        }

        private BitmapFontKey(String name, int fontSize, BitSet characters) {
            super(name);
            if (fontSize < 1) throw new IllegalArgumentException("Font size must be positive");
            this.fontSize = fontSize;
            this.characters = characters;
        }

        public int getFontSize() {
            return fontSize;
        }

        @Override
        public String toString() {
            return name + " (" + fontSize + "px, " + characters.cardinality() + " requested characters)";
        }

        @Override
        public boolean equals(Object obj) {
            if (!(obj instanceof BitmapFontKey) || !super.equals(obj)) {
                return false;
            }
            BitmapFontKey other = (BitmapFontKey) obj;
            return fontSize == other.fontSize && characters.equals(other.characters);
        }

        @Override
        public int hashCode() {
            return Objects.hash(super.hashCode(), fontSize, characters);
        }
    }

    private static BitSet defaultCharacters() {
        BitSet characters = requestedCharacters(
            new CharacterRange(0x20, 0x7e),   // Printable ASCII.
            new CharacterRange(0xa0, 0x24f),  // Latin-1 and Latin Extended A/B, including Ș/Ț.
            new CharacterRange(0x1e00, 0x1eff) // Latin Extended Additional.
        );
        DEFAULT_SYMBOLS.chars().forEach(characters::set);
        return characters;
    }

    private static BitSet requestedCharacters(CharacterRange... ranges) {
        BitSet requested = new BitSet();
        for (CharacterRange range : ranges) {
            requested.set(range.first, range.last + 1);
        }
        requested.clear(0, 0x20);
        requested.clear(0x7f, 0xa0);
        requested.clear(Character.MIN_SURROGATE, Character.MAX_SURROGATE + 1);
        if (requested.isEmpty()) {
            throw new IllegalArgumentException("At least one printable character must be requested");
        }
        return requested;
    }

    @Override
    public Object load(AssetInfo assetInfo) throws IOException {
        AssetKey<?> key = assetInfo.getKey();
        int fontSize = key instanceof BitmapFontKey
            ? ((BitmapFontKey) key).getFontSize()
            : DEFAULT_FONT_SIZE;
        BitSet requested = key instanceof BitmapFontKey ? ((BitmapFontKey) key).characters : DEFAULT_CHARACTERS;

        try (InputStream input = assetInfo.openStream()) {
            ByteBuffer fontBytes = ByteBuffer.wrap(ByteUtils.getByteContent(input));
            StbFont stbFont = new StbTrueType(BufferUtils::createByteBuffer).load(fontBytes);

            int[] codepoints = requested.stream().filter(codepoint -> stbFont.findGlyphIndex(codepoint) != 0).toArray();
            int count = codepoints.length;
            if (count == 0) throw new IOException("TrueType font has no requested glyphs: " + key.getName());
            StbGlyph[] glyphs = new StbGlyph[count];
            int[] glyphWidths = new int[count];
            int[] glyphHeights = new int[count];
            int maxGlyphWidth = 0;

            for (int i = 0; i < count; i++) {
                StbGlyph glyph = stbFont.glyph(codepoints[i]);
                GlyphBounds bounds = glyph.getBitmapBounds(fontSize);
                int width = bounds.width();
                int height = bounds.height();
                if (width < 0 || height < 0
                        || width >= MAX_ATLAS_SIZE - 2
                        || height >= MAX_ATLAS_SIZE - 2) {
                    throw new IOException("Glyph does not fit in the maximum atlas size");
                }
                glyphs[i] = glyph;
                glyphWidths[i] = width;
                glyphHeights[i] = height;
                maxGlyphWidth = Math.max(maxGlyphWidth, width);
            }

            // Match stbtt_BakeFontBitmap's shelf layout and select the smallest
            // power-of-two atlas by area.
            int minAtlasWidth = 1;
            while (minAtlasWidth <= maxGlyphWidth + 2) {
                minAtlasWidth *= 2;
            }

            int atlasWidth = 0;
            int atlasHeight = 0;
            long atlasArea = Long.MAX_VALUE;
            for (int candidateWidth = minAtlasWidth;
                    candidateWidth <= MAX_ATLAS_SIZE;
                    candidateWidth *= 2) {
                int x = 1;
                int y = 1;
                int bottomY = 1;

                for (int i = 0; i < count; i++) {
                    int width = glyphWidths[i];
                    int height = glyphHeights[i];
                    if (x + width + 1 >= candidateWidth) {
                        y = bottomY;
                        x = 1;
                    }
                    x += width + 1;
                    bottomY = Math.max(bottomY, y + height + 1);
                }

                int candidateHeight = 1;
                while (candidateHeight <= bottomY
                        && candidateHeight < MAX_ATLAS_SIZE) {
                    candidateHeight *= 2;
                }
                if (candidateHeight <= bottomY) {
                    continue;
                }

                long candidateArea = (long) candidateWidth * candidateHeight;
                if (candidateArea < atlasArea
                        || (candidateArea == atlasArea
                            && Math.max(candidateWidth, candidateHeight)
                            < Math.max(atlasWidth, atlasHeight))) {
                    atlasWidth = candidateWidth;
                    atlasHeight = candidateHeight;
                    atlasArea = candidateArea;
                }
            }

            if (atlasWidth == 0) {
                throw new IOException("Font does not fit in the maximum atlas size");
            }

            ByteBuffer texturePixels = BufferUtils.createByteBuffer(
                atlasWidth * atlasHeight * 2
            );

            AssetManager manager = assetInfo.getManager();

            Image image = new Image(
                Image.Format.Luminance8Alpha8,
                atlasWidth,
                atlasHeight,
                texturePixels,
                ColorSpace.Linear
            );

            Texture2D texture = new Texture2D(image);
            texture.setMagFilter(Texture.MagFilter.Bilinear);
            texture.setMinFilter(Texture.MinFilter.Trilinear);

            MaterialDef materialDef = manager.loadAsset(
                new AssetKey<MaterialDef>(
                    "Common/MatDefs/Misc/Unshaded.j3md"
                )
            );

            Material material = new Material(materialDef);
            material.setTexture("ColorMap", texture);
            material.setBoolean("VertexColor", true);
            material.getAdditionalRenderState().setBlendMode(BlendMode.Alpha);

            FontMetrics metrics = stbFont.getMetrics();
            float scale = stbFont.scaleForPixelHeight(fontSize);

            int base = Math.round(metrics.ascent() * scale);
            int lineHeight = Math.max(
                1,
                Math.round(
                    (metrics.ascent()
                        - metrics.descent()
                        + metrics.lineGap()) * scale
                )
            );

            BitmapCharacterSet characterSet = new BitmapCharacterSet();
            characterSet.setRenderedSize(fontSize);
            characterSet.setLineHeight(lineHeight);
            characterSet.setBase(base);
            characterSet.setWidth(atlasWidth);
            characterSet.setHeight(atlasHeight);

            int x = 1;
            int y = 1;
            int bottomY = 1;
            for (int i = 0; i < count; i++) {
                GlyphBitmap bitmap = glyphs[i].render(fontSize);
                int width = bitmap.getWidth();
                int height = bitmap.getHeight();
                if (x + width + 1 >= atlasWidth) {
                    y = bottomY;
                    x = 1;
                }
                // BitmapText addresses top-down atlas coordinates; the texture
                // buffer stores the bottom row first.
                for (int row = 0; row < height; row++) {
                    int destination = ((atlasHeight - 1 - y - row) * atlasWidth + x) * 2;
                    for (int col = 0; col < width; col++) {
                        byte coverage = bitmap.getData().get(row * bitmap.getStride() + col);
                        texturePixels.put(destination++, coverage);
                        texturePixels.put(destination++, coverage);
                    }
                }
                int codepoint = codepoints[i];
                BitmapCharacter character = new BitmapCharacter((char) codepoint);
                character.setX(x);
                character.setY(y);
                character.setWidth(width);
                character.setHeight(height);
                character.setXOffset(bitmap.getXOffset());
                character.setYOffset(base + bitmap.getYOffset());
                character.setXAdvance(Math.round(glyphs[i].getMetrics().advanceWidth() * scale));
                character.setPage(0);

                characterSet.addCharacter(codepoint, character);
                x += width + 1;
                bottomY = Math.max(bottomY, y + height + 1);
            }

            for (int left = 0; left < count; left++) {
                BitmapCharacter leftCharacter =
                    characterSet.getCharacter(codepoints[left]);

                for (int right = 0; right < count; right++) {
                    int amount = Math.round(
                        stbFont.getKerning(
                            glyphs[left],
                            glyphs[right]
                        ) * scale
                    );

                    if (amount != 0) {
                        leftCharacter.addKerning(
                            codepoints[right],
                            amount
                        );
                    }
                }
            }

            BitmapFont bitmapFont = new BitmapFont();
            bitmapFont.setCharSet(characterSet);
            bitmapFont.setPages(new Material[] { material });

            return bitmapFont;            
        }

    }
}
