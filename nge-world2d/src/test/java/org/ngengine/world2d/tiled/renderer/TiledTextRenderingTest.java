package org.ngengine.world2d.tiled.renderer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import java.math.BigInteger;

import org.junit.jupiter.api.Test;
import org.ngengine.world2d.tiled.core.TiledObjectText;
import org.ngengine.world2d.tiled.core.entity.TiledObjectEntity;
import org.ngengine.world2d.tiled.enums.ObjectShape;

import com.jme3.asset.AssetManager;
import com.jme3.font.BitmapFont;
import com.jme3.font.BitmapText;
import com.jme3.font.LineWrapMode;
import com.jme3.math.ColorRGBA;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.system.JmeSystem;

class TiledTextRenderingTest {
    @Test
    void tiledTextUsesAuthoredContentColorAndLayout() {
        AssetManager assets = JmeSystem.newAssetManager(
            TiledTextRenderingTest.class.getResource("/com/jme3/asset/Desktop.cfg")
        );
        TiledObjectEntity object = new TiledObjectEntity(BigInteger.ONE, 0, 0, 320, 96);
        object.setShape(ObjectShape.TEXT);
        TiledObjectText data = new TiledObjectText("SERVER ROOM");
        data.setPixelSize(42);
        data.setWrap(true);
        data.setHorizontalAlignment("center");
        data.setVerticalAlignment("bottom");
        data.setColor(new ColorRGBA(0.2f, 0.3f, 0.4f, 0.5f));
        object.setTextData(data);

        Spatial spatial = MapRenderer.newTextSpatial(assets, object);
        MapRenderer.configureTextSpatial(spatial, object, null);

        Node root = assertInstanceOf(Node.class, spatial);
        BitmapText text = assertInstanceOf(BitmapText.class, root.getChild(0));
        assertEquals("SERVER ROOM", text.getText());
        assertEquals(42f, text.getSize());
        assertEquals(LineWrapMode.Word, text.getLineWrapMode());
        assertEquals(BitmapFont.Align.Center, text.getAlignment());
        assertEquals(BitmapFont.VAlign.Bottom, text.getVerticalAlignment());
        assertEquals(data.getColor(), text.getColor());
    }
}
