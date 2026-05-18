/**
 * Copyright (c) 2025-2026, Nostr Game Engine
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 * 
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 
 * 3. Neither the name of the copyright holder nor the names of its
 *    contributors may be used to endorse or promote products derived from
 *    this software without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 * 
 * Nostr Game Engine is a fork of the jMonkeyEngine, which is licensed under
 * the BSD 3-Clause License. 
 */

package io.github.jmecn.tiled.loader;

import io.github.jmecn.tiled.util.ColorUtil;
import io.github.jmecn.tiled.xml.XmlNode;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import org.ngengine.platform.NGEUtils;

import static io.github.jmecn.tiled.TiledConst.*;
import static io.github.jmecn.tiled.loader.Utils.*;

/**
 * The property loader.
 *
 * @author yanmaoyuan
 */
public class PropertyLoader {

    private static final Logger logger = Logger.getLogger(PropertyLoader.class.getName());

    /**
     * Reads properties from amongst the given children. When a "properties"
     * element is encountered, it recursively calls itself with the children of
     * this node. This function ensures backward compatibility with tmx version
     * 0.99a.
     * <p>
     * Support for reading property values stored as character data was added in
     * Tiled 0.7.0 (tmx version 0.99c).
     *
     * @param node the node which contains properties
     * @return the properties
     */
    public Map<String, Object> readProperties(XmlNode node) {
        Map<String, Object> props = new HashMap<>();

        if (node == null) {
            return props;
        }

        XmlNode propertiesNode = getChildByTag(node, PROPERTIES);
        if (propertiesNode == null) {
            return props;
        }

        for (XmlNode child : propertiesNode.getChildNodes()) {
            if (PROPERTY.equals(child.getNodeName())) {
                readProperty(child, props);
            }
        }
        return props;
    }

    /**
     * read every property in a properties
     *
     * @param node the node which contains property
     * @param props the properties to store the property
     */
    private void readProperty(XmlNode node, Map<String, Object> props) {
        String keyName = getAttributeValue(node, NAME);
        String type = getAttribute(node, TYPE, "string");
    // TODO support custom property type
    // @see https://doc.mapeditor.org/en/stable/manual/custom-properties/#custom-property-types
        String value = getAttributeValue(node, VALUE);
        if (value == null) {
            value = node.getTextContent();
            if (value != null) value = value.trim();
        }

        if (value != null) {
            Object val = convertPropertyValue(type, value);
            if (val != null) props.put(keyName, val);
        }
    }

    /**
     * <p>type can be as follows:</p>
     * <ul>
     * <li>
     * file: stored as paths relative from the location of the map file. (since 0.17)
     * </li>
     * <li>
     * object: can reference any object on the same map and are stored as an integer (the ID of
     * the referenced object, or 0 when no object is referenced). When used on objects in the
     * Tile Collision Editor, they can only refer to other objects on the same tile. (since 1.4)
     * </li>
     * <li>
     * class: will have their member values stored in a nested &lt;properties&gt; element. Only the
     * actually set members are saved. When no members have been set the properties element is
     * left out entirely.(since 1.8)
     * </li>
     * </ul>
     *
     * @param type the type of the property
     * @param value the value of the property
     */
    private Object convertPropertyValue(String type, String value) {
        switch (type) {
            // string (default) (since 0.16)
            case "string":
                return NGEUtils.safeString(value);
            // a int value (since 0.16)
            case "int":
                return NGEUtils.safeInt(value);
            // a float value (since 0.16)
            case "float":
                return (float)NGEUtils.safeDouble(value);
            // has a value of either "true" or "false". (since 0.16)
            case "bool":
                return NGEUtils.safeBool(value);
            // stored in the format #AARRGGBB. (since 0.17)
            case COLOR:
                return ColorUtil.toColorRGBA(NGEUtils.safeString(value));                
            // stored as paths relative from the location of the map file. (since 0.17)
            case "file":
                return NGEUtils.safeString(value);
            // can reference any object on the same map and are stored as an integer
            // (the ID of the referenced object, or 0 when no object is referenced).
            // When used on objects in the Tile Collision Editor, they can only refer
            // to other objects on the same tile. (since 1.4)
            case OBJECT:
                return NGEUtils.safeInt(value);
            // will have their member values stored in a nested <properties> element.
            // Only the actually set members are saved. When no members have been set
            // the properties element is left out entirely. (since 1.8)
            case CLASS:
                return NGEUtils.safeString(value);
            default:
                logger.warning("unknown type: "+ type);
                return null;
        }
    }

}
