package io.github.jmecn.tiled.loader;

import com.jme3.asset.AssetKey;
import com.jme3.asset.AssetManager;
import io.github.jmecn.tiled.core.Layer;
import io.github.jmecn.tiled.util.ColorUtil;
import io.github.jmecn.tiled.xml.XmlNode;

import java.io.IOException;
import java.util.Properties;

import static io.github.jmecn.tiled.TiledConst.*;
import static io.github.jmecn.tiled.loader.Utils.*;
import static io.github.jmecn.tiled.loader.Utils.getDoubleAttribute;

/**
 * The base class for all layer loaders.
 *
 * @author yanmaoyuan
 */
public abstract class LayerLoader {

    protected AssetManager assetManager;
    protected AssetKey<?> assetKey;
    protected PropertyLoader propertiesLoader;
    protected ImageLoader imageLoader;

    protected LayerLoader(AssetManager assetManager, AssetKey<?> key) {
        this.assetManager = assetManager;
        this.assetKey = key;

        this.propertiesLoader = new PropertyLoader();
        this.imageLoader = new ImageLoader(assetManager, key);
    }

    /**
     * Loads a map layer from a layer node.
     * @param node the node representing the "layer" element
     * @return the loaded map layer
     * @throws IOException if an I/O error occurs
     */
    public abstract Layer load(XmlNode node) throws IOException;

    /**
     * read the common part of a Layer
     *
     * @param node the Layer node
     * @param layer the Layer
     */
    protected void readLayerBase(XmlNode node, Layer layer) {
        String id = getAttributeValue(node, ID);
        if (id != null) {
            layer.setId(Integer.parseInt(id));
        }

        final String name = getAttributeValue(node, NAME);
        String clazz = getAttribute(node, CLASS, EMPTY);
        double opacity = getDoubleAttribute(node, OPACITY, 1.0);
        boolean visible = getAttribute(node, VISIBLE, 1) == 1;
        boolean locked = getAttribute(node, LOCKED, 0) == 1;
        String tintColor = getAttributeValue(node, TINT_COLOR);
        int offsetX = getAttribute(node, OFFSET_X, 0);
        int offsetY = getAttribute(node, OFFSET_Y, 0);
        float parallaxX = (float) getDoubleAttribute(node, PARALLAX_X, 1.0);
        float parallaxY = (float) getDoubleAttribute(node, PARALLAX_Y, 1.0);

        layer.setName(name);
        layer.setClazz(clazz);
        layer.setOpacity(opacity);

        if (tintColor != null) {
            layer.setTintColor(ColorUtil.toColorRGBA(tintColor));
        }

        // This is done at the end, otherwise the offset is applied during
        // the loading of the tiles.
        layer.setOffset(offsetX, offsetY);

        // The parallax scrolling factor determines the amount by which the layer
        // moves in relation to the camera.
        layer.setParallaxFactor(parallaxX, parallaxY);

        // Invisible layers are automatically locked, so it is important to
        // set the layer to potentially invisible _after_ the layer data is
        // loaded.
        layer.setVisible(visible);

        layer.setLocked(locked);

        // read properties
        Properties props = propertiesLoader.readProperties(node);
        layer.setProperties(props);
    }

}
