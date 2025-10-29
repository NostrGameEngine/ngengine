package io.github.jmecn.tiled.core;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * GroupLayer
 *
 * @author yanmaoyuan
 */
public class GroupLayer extends Layer {

    private final List<Layer> layers;

    private final Map<String, Layer> layerMap;

    public GroupLayer() {
        layers = new ArrayList<>();
        layerMap = new HashMap<>();
    }

    public void addLayer(Layer layer) {
        layer.setMap(getMap());
        layer.setParent(this);
        layers.add(layer);
        layerMap.put(layer.getName(), layer);
        layer.setUpdateNeeded();
    }

    @Override
    public void setUpdateNeeded() {
        for (Layer layer : layers) {
            layer.setUpdateNeeded();
        }
    }

    /**
     * @return The number of layers in this group.
     */
    public int getLayerCount() {
        return layers.size();
    }

    /**
     * @return The list of layers in this group.
     */
    public List<Layer> getLayers() {
        return layers;
    }

    /**
     * @param index The index of the layer.
     * @return The layer at the given index.
     */
    public Layer getLayer(int index) {
        return layers.get(index);
    }

    /**
     * @param name The name of the layer.
     * @return The layer with the given name, or null if no layer with that name
     */
    public Layer getLayer(String name) {
        return layerMap.get(name);
    }

    /**
     * Invalid the render offset of all layers.
     */
    @Override
    public void invalidRenderOffset() {
        super.invalidRenderOffset();
        for (Layer layer : layers) {
            layer.invalidRenderOffset();
        }
    }

    /**
     * Invalid the render parallax of all layers.
     */
    @Override
    public void invalidRenderParallax() {
        super.invalidRenderParallax();
        for (Layer layer : layers) {
            layer.invalidRenderParallax();
        }
    }

}