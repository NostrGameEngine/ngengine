package org.ngengine.gui.svg;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Logger;

import com.jme3.asset.AssetInfo;
import com.jme3.asset.AssetKey;
import com.jme3.asset.AssetLoader;
import com.jme3.texture.Image;
import com.jme3.texture.plugins.AWTLoader;
import org.apache.batik.transcoder.TranscoderInput;
import org.apache.batik.transcoder.TranscoderOutput;
import org.apache.batik.transcoder.image.ImageTranscoder;
import org.apache.batik.anim.dom.SAXSVGDocumentFactory;
import org.apache.batik.util.XMLResourceDescriptor;
import org.w3c.dom.Document;
import java.awt.Color;
import java.awt.image.BufferedImage;

public class SVGLoader implements AssetLoader {
    private static final Logger log = Logger.getLogger(SVGLoader.class.getName());
    static AtomicLong id = new AtomicLong(0);

    @Override
    public Object load(AssetInfo assetInfo) throws IOException {
        AssetKey key = assetInfo.getKey();
        int width = 256;
        int height = 256;
        boolean flipY = false;


        if (key instanceof SVGTextureKey) {
            SVGTextureKey svgKey = (SVGTextureKey) key;
            width = svgKey.getWidth();
            height = svgKey.getHeight();
            flipY = svgKey.isFlipY();
        }


        InputStream in = assetInfo.openStream();
        try {

            // Read the SVG file as a string
            String svg = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            // Replace currentColor with white
            svg = svg.replace("currentColor", "#ffffff");

            // Parse the SVG
            String parser = XMLResourceDescriptor.getXMLParserClassName();
            SAXSVGDocumentFactory factory = new SAXSVGDocumentFactory(parser);

            Document svgDocument = factory.createDocument(null,
                    new ByteArrayInputStream(svg.getBytes(StandardCharsets.UTF_8)));

             // Convert the SVG to a BufferedImage using Batik
             BufferedImageTranscoder transcoder = new BufferedImageTranscoder();
             transcoder.addTranscodingHint(ImageTranscoder.KEY_WIDTH, (float) width);
             transcoder.addTranscodingHint(ImageTranscoder.KEY_HEIGHT, (float) height);
             transcoder.addTranscodingHint(ImageTranscoder.KEY_BACKGROUND_COLOR, new Color(0, 0, 0, 0));
             transcoder.addTranscodingHint(ImageTranscoder.KEY_FORCE_TRANSPARENT_WHITE, true);

            TranscoderInput input = new TranscoderInput(svgDocument);
            transcoder.transcode(input, null);

            BufferedImage image = transcoder.getBufferedImage();
            // Convert to jME3 image
            AWTLoader awtLoader = new AWTLoader();
            Image img = awtLoader.load(image, flipY);
            return img;


        } catch (Exception e) {
            throw new IOException("Error loading SVG: " + e.getMessage(), e);
        } finally {
            if (in != null) {
                in.close();
            }
        }
    }

    /**
     * Custom transcoder that creates a BufferedImage from an SVG document
     */
    private static class BufferedImageTranscoder extends ImageTranscoder {
        private BufferedImage image = null;

        @Override
        public BufferedImage createImage(int width, int height) {
            return new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        }

        @Override
        public void writeImage(BufferedImage img, TranscoderOutput output) {
            this.image = img;
        }

        public BufferedImage getBufferedImage() {
            return image;
        }
    }
}