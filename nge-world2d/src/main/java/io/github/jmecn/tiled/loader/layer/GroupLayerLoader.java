package io.github.jmecn.tiled.loader.layer;

import com.jme3.asset.AssetKey;
import com.jme3.asset.AssetManager;
import io.github.jmecn.tiled.core.*;
import io.github.jmecn.tiled.loader.LayerLoader;
import io.github.jmecn.tiled.xml.XmlNode;

import java.io.IOException;

import static io.github.jmecn.tiled.TiledConst.*;
import static io.github.jmecn.tiled.TiledConst.TEXT_EMPTY;

/**
 * desc:
 *
 * @author yanmaoyuan
 */
public class GroupLayerLoader extends LayerLoader {

    private final TiledMap map;

    public GroupLayerLoader(AssetManager assetManager, AssetKey<?> key, TiledMap map) {
        super(assetManager, key);
        this.map = map;
    }

    @Override
    public GroupLayer load(XmlNode node) throws IOException {
        GroupLayer groupLayer = new GroupLayer();
        readLayerBase(node, groupLayer);
        groupLayer.setMap(map);

        LayerLoaders layerLoaders = new LayerLoaders(assetManager, assetKey, map);

        XmlNode child = node.getFirstChild();
        while (child != null) {
            // ignore properties
            if (!PROPERTIES.equals(child.getNodeName()) && !TEXT_EMPTY.equals(child.getNodeName())) {
                LayerLoader layerLoader = layerLoaders.create(child.getNodeName());
                if (layerLoader != null) {
                    Layer layer = layerLoader.load(child);
                    groupLayer.addLayer(layer);
                }
            }
            child = child.getNextSibling();
        }
        return groupLayer;
    }
}
