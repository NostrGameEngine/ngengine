package io.github.jmecn.tiled.loader;

import com.jme3.asset.AssetInfo;
import com.jme3.asset.AssetKey;
import com.jme3.asset.AssetManager;
import com.jme3.asset.TextureKey;
import com.jme3.texture.Image;
import com.jme3.texture.Texture;
import com.jme3.texture.Texture2D;
import io.github.jmecn.tiled.core.TiledImage;
import io.github.jmecn.tiled.xml.XmlNode;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.util.Base64;
import java.util.logging.Level;
import java.util.logging.Logger;

import static io.github.jmecn.tiled.TiledConst.*;
import static io.github.jmecn.tiled.loader.Utils.*;

/**
 * Tiled Image Loader.
 *
 * @author yanmaoyuan
 */
public final class ImageLoader {

    private static final Logger logger = Logger.getLogger(ImageLoader.class.getName());

    private final AssetManager assetManager;
    private final AssetKey<?> assetKey;

    public ImageLoader(AssetManager assetManager, AssetKey<?> assetKey) {
        this.assetManager = assetManager;
        this.assetKey = assetKey;
    }

    /**
     * <p>Load an image from file or decode from the data elements.</p>
     * <p>
     * Note that it is not currently possible to use Tiled to create maps with
     * embedded image data, even though the TMX format supports this. It is
     * possible to create such maps using libtiled (Qt/C++) or tmxlib (Python).
     * </p>
     *
     * @param node the node representing the "image" element
     * @return the loaded image
     */
    public TiledImage load(XmlNode node) {
        String source = getAttributeValue(node, SOURCE);
        String trans = getAttributeValue(node, TRANS);
        String format = getAttributeValue(node, FORMAT);
        int width = getAttribute(node, WIDTH, 0);
        int height = getAttribute(node, HEIGHT, 0);

        Texture2D texture = null;
        // load an image from file or decode from the CDATA.
        if (source != null) {
            String assetPath = toJmeAssetPath(assetManager, assetKey, assetKey.getFolder() + source);
            source = assetPath;
            texture = loadTexture2D(assetPath);
        } else {
            // embedded image data, decode from the <data> node text
            XmlNode item = getChildByTag(node, DATA);
            if (item != null) {
                String encodedData = item.getTextContent();
                if ((encodedData == null || encodedData.trim().isEmpty()) && item.getFirstChild() != null) {
                    encodedData = item.getFirstChild().getTextContent();
                }
                if (encodedData != null && !encodedData.trim().isEmpty()) {
                    byte[] imageData = Base64.getDecoder().decode(encodedData.trim());
                    texture = loadTexture2D(imageData);
                }
            }
        }

        if (texture == null) {
            logger.log(Level.SEVERE, "Image source not found: " + source);
            throw new IllegalArgumentException("Image source not found: " + source);
        }

        if (width == 0 || height == 0) {
            logger.info("Image size is not specified, using the texture size.");
            width = texture.getImage().getWidth();
            height = texture.getImage().getHeight();
        }

        TiledImage image = new TiledImage(source, trans, format, width, height);
        image.setTexture(texture);

        return image;
    }


    /**
     * Load a Texture from source
     *
     * @param source the source path
     * @return the loaded texture
     */
    private Texture2D loadTexture2D(final String source) {
        Texture2D tex = null;
        try {
            TextureKey texKey = new TextureKey(source, true);
            texKey.setGenerateMips(false);
            tex = (Texture2D) assetManager.loadTexture(texKey);
            tex.setWrap(Texture.WrapMode.EdgeClamp);
            tex.setMagFilter(Texture.MagFilter.Nearest);
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Can't load texture " + source, e);
        }

        return tex;
    }

    private Texture2D loadTexture2D(final byte[] data) {
        Class<?> loaderClass = null;
        Object loaderInstance;
        Method loadMethod;

        try {
            // try Desktop first
            loaderClass = Class.forName("com.jme3.texture.plugins.AWTLoader");
        } catch (ClassNotFoundException e) {
            logger.info("Can't find AWTLoader.");
            try {
                // then try Android Native Image Loader
                loaderClass = Class.forName("com.jme3.texture.plugins.AndroidNativeImageLoader");
            } catch (ClassNotFoundException e1) {
                logger.info("Can't find AndroidNativeImageLoader.");
                try {
                    // then try Android BufferImage Loader
                    loaderClass = Class.forName("com.jme3.texture.plugins.AndroidBufferImageLoader");
                } catch (ClassNotFoundException e2) {
                    logger.info("Can't find AndroidNativeImageLoader.");
                }
            }
        }

        if (loaderClass == null) {
            return null;
        } else {
            // try Desktop first
            try {
                loaderInstance = loaderClass.getConstructor().newInstance();
                loadMethod = loaderClass.getMethod("load", AssetInfo.class);
            } catch (ReflectiveOperationException e) {
                logger.log(Level.SEVERE, "Can't find loader class.", e);
                throw new IllegalArgumentException("Can't find AWTLoader.");
            }
        }

        TextureKey texKey = new TextureKey();
        AssetInfo info = new AssetInfo(assetManager, texKey) {
            public InputStream openStream() {
                return new ByteArrayInputStream(data);
            }
        };

        Texture2D tex = null;
        try {
            Image img = (Image) loadMethod.invoke(loaderInstance, info);

            tex = new Texture2D();
            tex.setWrap(Texture.WrapMode.EdgeClamp);
            tex.setMagFilter(Texture.MagFilter.Nearest);
            tex.setAnisotropicFilter(texKey.getAnisotropy());
            tex.setName(texKey.getName());
            tex.setImage(img);
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Can't load texture from byte array", e);
        }
        return tex;
    }
}
