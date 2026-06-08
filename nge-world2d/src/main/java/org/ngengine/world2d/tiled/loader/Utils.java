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

package org.ngengine.world2d.tiled.loader;

import com.jme3.asset.AssetKey;
import com.jme3.asset.AssetManager;
import org.ngengine.world2d.tiled.xml.XmlNode;

import java.util.ArrayList;
import java.util.List;

/**
 * desc:
 *
 * @author yanmaoyuan
 */
public final class Utils {
    private Utils() {
    }

    /**
     * Utilities method to correct the asset path.
     *
     * @param assetManager the asset manager
     * @param key          the asset key
     * @param src          the source path
     * @return the corrected asset path
     */
    public static String toJmeAssetPath(AssetManager assetManager, AssetKey<?> key, String src) {
        /*
         * 1st: try to locate it with assetManager. No need to handle the src
         * path unless assetManager can't locate it.
         */
        if (assetManager.locateAsset(new AssetKey<>(src)) != null) {
            return src;
        }

        /*
         * 2nd: In JME I suppose that all the files needed are in the same
         * folder, that's why I cut the filename and contact it to
         * key.getFolder().
         */
        String dest = src.replace("\\\\", "/");
        int idx = dest.lastIndexOf("/");
        if (idx >= 0) {
            dest = key.getFolder() + src.substring(idx + 1);
        } else {
            dest = key.getFolder() + dest;
        }

        /*
         * 3rd: try to locate it again.
         */
        if (assetManager.locateAsset(new AssetKey<>(dest)) != null) {
            return dest;
        } else {
            throw new IllegalArgumentException("Can't locate asset: " + src);
        }
    }

    public static XmlNode getChildByTag(XmlNode node, String tag) {
        for (XmlNode child : node.getChildNodes()) {
            if (tag.equals(child.getNodeName())) {
                return child;
            }
        }
        return null;
    }

    public static List<XmlNode> getChildrenByTag(XmlNode node, String tag) {
        List<XmlNode> children = new ArrayList<>();
        for (XmlNode child : node.getChildNodes()) {
            if (tag.equals(child.getNodeName())) {
                children.add(child);
            }
        }
        return children;
    }

    public static String getAttributeValue(XmlNode node, String attributeName) {
        return node.getAttribute(attributeName);
    }

    public static String getAttribute(XmlNode node, String attributeName, String def) {
        final String attr = getAttributeValue(node, attributeName);
        if (attr != null) {
            return attr;
        } else {
            return def;
        }
    }

    public static int getAttribute(XmlNode node, String attributeName, int def) {
        final String attr = getAttributeValue(node, attributeName);
        if (attr != null) {
            return Integer.parseInt(attr);
        } else {
            return def;
        }
    }

    public static double getDoubleAttribute(XmlNode node, String attributeName, double def) {
        final String attr = getAttributeValue(node, attributeName);
        if (attr != null) {
            return Double.parseDouble(attr);
        } else {
            return def;
        }
    }
}