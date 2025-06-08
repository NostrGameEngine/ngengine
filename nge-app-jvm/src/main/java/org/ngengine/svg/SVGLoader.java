/**
 * Copyright (c) 2025, Nostr Game Engine
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
 * the BSD 3-Clause License. The original jMonkeyEngine license is as follows:
 */
package org.ngengine.svg;

import com.jme3.asset.AssetInfo;
import com.jme3.asset.AssetKey;
import com.jme3.asset.AssetLoader;
import com.jme3.texture.Image;
import com.jme3.texture.plugins.AWTLoader;
import com.kitfox.svg.SVGDiagram;
import com.kitfox.svg.SVGUniverse;
import com.kitfox.svg.app.beans.SVGIcon;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicLong;
import org.ngengine.gui.svg.SVGTextureKey;

public class SVGLoader implements AssetLoader {

    static AtomicLong id = new AtomicLong(0);

    @Override
    public Object load(AssetInfo assetInfo) throws IOException {
        AssetKey key = assetInfo.getKey();
        int width = 256;
        int height = 256;
        boolean flipY = false;
        Graphics2D g2d = null;

        if (key instanceof SVGTextureKey) {
            SVGTextureKey svgKey = (SVGTextureKey) key;
            width = svgKey.getWidth();
            height = svgKey.getHeight();
            flipY = svgKey.isFlipY();
        }

        InputStream in = assetInfo.openStream();
        try {
            String svg = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            in.close();

            svg = svg.replace("currentColor", "#ffffff");
            in = new ByteArrayInputStream(svg.getBytes(StandardCharsets.UTF_8));

            SVGUniverse universe = new SVGUniverse();
            URI uri = universe.loadSVG(in, key.getName() + "svg" + id.incrementAndGet());

            // Get the diagram directly for more control
            SVGDiagram diagram = universe.getDiagram(uri);
            if (diagram == null) {
                throw new IOException("Failed to load SVG diagram from: " + key.getName());
            }

            // Set diagram properties for better rendering
            diagram.setIgnoringClipHeuristic(true);

            // Create a properly configured SVG icon
            SVGIcon icon = new SVGIcon();
            icon.setSvgUniverse(universe);
            icon.setSvgURI(uri);
            icon.setAntiAlias(true);
            icon.setAutosize(SVGIcon.AUTOSIZE_STRETCH);
            icon.setPreferredSize(new Dimension(width, height));

            // Create the image with proper transparency
            BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
            g2d = image.createGraphics();

            // Clear the background to transparent
            g2d.setBackground(new Color(0, 0, 0, 0));
            g2d.clearRect(0, 0, width, height);

            // Set rendering hints
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            g2d.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);

            // Method 1: Use SVGIcon (the current way)
            icon.paintIcon(null, g2d, 0, 0);

            // Convert to jME3 image
            AWTLoader awtLoader = new AWTLoader();
            Image img = awtLoader.load(image, flipY);
            return img;
        } catch (Exception e) {
            throw new IOException("Error loading SVG: " + e.getMessage(), e);
        } finally {
            in.close();
            if (g2d != null) {
                g2d.dispose();
            }
        }
    }
}
