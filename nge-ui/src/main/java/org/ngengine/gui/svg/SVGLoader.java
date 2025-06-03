package org.ngengine.gui.svg;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicLong;

import com.jme3.asset.AssetInfo;
import com.jme3.asset.AssetKey;
import com.jme3.asset.AssetLoader;
import com.jme3.texture.Image;
import com.jme3.texture.Texture2D;
import com.jme3.texture.plugins.AWTLoader;
import com.kitfox.svg.SVGDiagram;
import com.kitfox.svg.SVGUniverse;
import com.kitfox.svg.app.beans.SVGIcon;

import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Color;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;

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
            URI uri = universe.loadSVG(in, key.getName()+"svg"+id.incrementAndGet());

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