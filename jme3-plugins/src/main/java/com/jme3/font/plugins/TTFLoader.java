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
import org.ngengine.stbttf.GlyphBounds;
import org.ngengine.stbttf.StbFont;
import org.ngengine.stbttf.StbGlyph;
import org.ngengine.stbttf.StbTrueType;
import org.ngengine.stbttf.StbttBakedChar;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.Objects;

public class TTFLoader implements AssetLoader {
    public static final int DEFAULT_FONT_SIZE = 64;

    private static final int FIRST_CHAR = 32;
    private static final int CHAR_COUNT = 95;
    private static final int MAX_ATLAS_SIZE = 8192;
 
    public static class BitmapFontKey extends AssetKey<BitmapFont> {
        private final int fontSize;

        public BitmapFontKey(String name) {
            this(name, DEFAULT_FONT_SIZE);
        }

        public BitmapFontKey(String name, int fontSize) {
            super(name);
            if (fontSize < 1) throw new IllegalArgumentException("Font size must be positive");
            this.fontSize = fontSize;
        }

        public int getFontSize() {
            return fontSize;
        }

        @Override
        public String toString() {
            return name + " (" + fontSize + "px)";
        }

        @Override
        public boolean equals(Object obj) {
            if (!super.equals(obj)) {
                return false;
            }
            BitmapFontKey other = (BitmapFontKey) obj;
            return fontSize == other.fontSize;
        }

        @Override
        public int hashCode() {
            return Objects.hash(super.hashCode(), fontSize);
        }
    }

    @Override
    public Object load(AssetInfo assetInfo) throws IOException {
        AssetKey<?> key = assetInfo.getKey();
        int fontSize = key instanceof BitmapFontKey
            ? ((BitmapFontKey) key).getFontSize()
            : DEFAULT_FONT_SIZE;


        try (InputStream input = assetInfo.openStream()) {
            ByteBuffer fontBytes = ByteBuffer.wrap(ByteUtils.getByteContent(input));
            StbFont stbFont = new StbTrueType(BufferUtils::createByteBuffer).load(fontBytes);

            StbGlyph[] glyphs = new StbGlyph[CHAR_COUNT];
            int[] glyphWidths = new int[CHAR_COUNT];
            int[] glyphHeights = new int[CHAR_COUNT];
            int maxGlyphWidth = 0;

            for (int i = 0; i < CHAR_COUNT; i++) {
                StbGlyph glyph = stbFont.glyph(FIRST_CHAR + i);
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

                for (int i = 0; i < CHAR_COUNT; i++) {
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

            ByteBuffer atlasPixels = BufferUtils.createByteBuffer(atlasWidth * atlasHeight);

            StbttBakedChar[] bakedChars = new StbttBakedChar[CHAR_COUNT];

            for (int i = 0; i < CHAR_COUNT; i++) {
                bakedChars[i] = new StbttBakedChar();
            }

            int bakeResult = stbFont.bakeFontBitmap(
                fontSize,
                atlasPixels,
                atlasWidth,
                atlasHeight,
                FIRST_CHAR,
                CHAR_COUNT,
                bakedChars
            );

            if (bakeResult <= 0) {
                throw new IOException(
                    "Font does not fit in the " +
                    atlasWidth + "x" + atlasHeight + " atlas"
                );
            }

            // BitmapText uses top-down BMFont coordinates, while OpenGL uploads
            // the first row at the bottom. Duplicate coverage into luminance and
            // alpha so glyphs are white and their background stays transparent.
            ByteBuffer texturePixels = BufferUtils.createByteBuffer(
                atlasWidth * atlasHeight * 2
            );
            for (int y = atlasHeight - 1; y >= 0; y--) {
                int row = y * atlasWidth;
                for (int x = 0; x < atlasWidth; x++) {
                    byte coverage = atlasPixels.get(row + x);
                    texturePixels.put(coverage).put(coverage);
                }
            }
            texturePixels.flip();

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

            for (int i = 0; i < CHAR_COUNT; i++) {
                int codepoint = FIRST_CHAR + i;

                StbttBakedChar baked = bakedChars[i];

                BitmapCharacter character = new BitmapCharacter(
                    (char) codepoint
                );

                character.setX(baked.x0u());
                character.setY(baked.y0u());
                character.setWidth(baked.x1u() - baked.x0u());
                character.setHeight(baked.y1u() - baked.y0u());

                character.setXOffset(Math.round(baked.xoff));

                character.setYOffset(
                    base + Math.round(baked.yoff)
                );

                character.setXAdvance(Math.round(baked.xadvance));
                character.setPage(0);

                characterSet.addCharacter(codepoint, character);
            }

            for (int left = 0; left < CHAR_COUNT; left++) {
                BitmapCharacter leftCharacter =
                    characterSet.getCharacter(FIRST_CHAR + left);

                for (int right = 0; right < CHAR_COUNT; right++) {
                    int amount = Math.round(
                        stbFont.getKerning(
                            glyphs[left],
                            glyphs[right]
                        ) * scale
                    );

                    if (amount != 0) {
                        leftCharacter.addKerning(
                            FIRST_CHAR + right,
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
