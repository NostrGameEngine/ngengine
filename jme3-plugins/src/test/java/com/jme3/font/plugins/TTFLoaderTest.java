package com.jme3.font.plugins;

import com.jme3.asset.AssetKey;
import com.jme3.asset.DesktopAssetManager;
import com.jme3.asset.plugins.ClasspathLocator;
import com.jme3.font.BitmapCharacter;
import com.jme3.font.BitmapFont;
import com.jme3.texture.Texture;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TTFLoaderTest {
    private static final String FONT = "Interface/Fonts/NotJamMonoClean13/Not Jam Mono Clean 16.ttf";

    private DesktopAssetManager assets() {
        DesktopAssetManager assets = new DesktopAssetManager(false);
        assets.registerLocator("/", ClasspathLocator.class);
        assets.registerLoader(TTFLoader.class, "ttf");
        assets.registerLoader(com.jme3.material.plugins.J3MLoader.class, "j3md");
        return assets;
    }

    @Test
    void defaultFontBakesLatinGlyphsAndCoverage() {
        BitmapFont font = assets().loadAsset(new AssetKey<BitmapFont>(FONT));
        for (char character : "AàèéìòùÀÈÉÌÒÙł".toCharArray()) {
            assertInk(font, character);
        }
    }

    @Test
    void customRangesPreserveHolesAndKeepIndependentCacheKeys() {
        var ascii = new TTFLoader.CharacterRange(' ', '~');
        var cyrillic = new TTFLoader.CharacterRange('А', 'я');
        var greek = new TTFLoader.CharacterRange('β', 'β');
        var first = new TTFLoader.BitmapFontKey(FONT, 32, ascii, cyrillic, greek);
        var reordered = new TTFLoader.BitmapFontKey(FONT, 32, greek, cyrillic, ascii, ascii);
        var second = new TTFLoader.BitmapFontKey(FONT, 32, ascii);
        assertEquals(first, reordered);
        assertEquals(first.hashCode(), reordered.hashCode());
        assertNotEquals(first, second);
        var assets = assets();
        BitmapFont custom = assets.loadAsset(first);
        assertInk(custom, 'A');
        assertInk(custom, 'Б');
        assertInk(custom, 'β');
        assertNull(custom.getCharSet().getCharacter('è'));
        assertNull(custom.getCharSet().getCharacter('γ'));
        assertNull(assets.loadAsset(second).getCharSet().getCharacter('Б'));
        assertThrows(IllegalArgumentException.class, () -> new TTFLoader.CharacterRange(0x10000, 0x10001));
        assertThrows(IllegalArgumentException.class, () -> new TTFLoader.CharacterRange('z', 'a'));
    }

    private static void assertInk(BitmapFont font, char character) {
        BitmapCharacter glyph = font.getCharSet().getCharacter(character);
        assertNotNull(glyph, () -> "Missing glyph: " + character);
        assertTrue(glyph.getXAdvance() > 0);
        var image = ((Texture) font.getPage(0).getTextureParam("ColorMap").getTextureValue()).getImage();
        var pixels = image.getData(0);
        for (int y = glyph.getY(); y < glyph.getY() + glyph.getHeight(); y++) {
            for (int x = glyph.getX(); x < glyph.getX() + glyph.getWidth(); x++) {
                if (pixels.get(((image.getHeight() - 1 - y) * image.getWidth() + x) * 2 + 1) != 0) return;
            }
        }
        fail("Empty glyph coverage: " + character);
    }
}
