package org.ngengine.gradle.basis;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import javax.imageio.ImageIO;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.gradle.api.GradleException;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

final class TiledAtlasScanner {

    private TiledAtlasScanner() {
    }

    static List<TiledAtlas> scan(List<Path> roots) {
        List<Path> normalizedRoots = new ArrayList<>();
        for (Path root : roots) {
            Path normalized = root.toAbsolutePath().normalize();
            if (Files.isDirectory(normalized)) {
                normalizedRoots.add(normalized);
            }
        }
        normalizedRoots.sort(Comparator.comparingInt(Path::getNameCount).reversed());

        List<Path> descriptors = new ArrayList<>();
        for (Path root : normalizedRoots) {
            try (var files = Files.walk(root)) {
                files.filter(Files::isRegularFile)
                        .filter(TiledAtlasScanner::isTiledDescriptor)
                        .forEach(descriptors::add);
            } catch (IOException exception) {
                throw new GradleException("Unable to scan Tiled resources under " + root, exception);
            }
        }
        descriptors.sort(Comparator.comparing(Path::toString));

        Map<Path, TiledAtlas> atlases = new LinkedHashMap<>();
        for (Path descriptor : descriptors) {
            scanDescriptor(descriptor, normalizedRoots, atlases);
        }
        return new ArrayList<>(atlases.values());
    }

    private static void scanDescriptor(
            Path descriptor,
            List<Path> resourceRoots,
            Map<Path, TiledAtlas> atlases) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
            factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            factory.setXIncludeAware(false);
            factory.setExpandEntityReferences(false);
            Element root = factory.newDocumentBuilder().parse(descriptor.toFile()).getDocumentElement();
            if ("tileset".equals(root.getTagName())) {
                addTileset(root, descriptor.getParent(), resourceRoots, atlases);
                return;
            }
            if (!"map".equals(root.getTagName())) {
                return;
            }
            for (Element tileset : directChildren(root, "tileset")) {
                if (!tileset.hasAttribute("source")) {
                    addTileset(tileset, descriptor.getParent(), resourceRoots, atlases);
                }
            }
        } catch (ParserConfigurationException | SAXException | IOException exception) {
            throw new GradleException("Unable to parse Tiled descriptor " + descriptor, exception);
        }
    }

    private static void addTileset(
            Element tileset,
            Path descriptorDirectory,
            List<Path> resourceRoots,
            Map<Path, TiledAtlas> atlases) throws IOException {
        List<Element> images = directChildren(tileset, "image");
        if (images.size() != 1) {
            return;
        }
        Element imageElement = images.get(0);
        String source = imageElement.getAttribute("source");
        if (source.isBlank() || source.contains(":")) {
            return;
        }

        Path imagePath = descriptorDirectory.resolve(source).normalize().toAbsolutePath();
        if (!Files.isRegularFile(imagePath)) {
            throw new GradleException("Tiled atlas image does not exist: " + imagePath);
        }
        Path resourceRoot = containingRoot(imagePath, resourceRoots);
        if (resourceRoot == null) {
            throw new GradleException("Tiled atlas image is outside configured resource roots: " + imagePath);
        }

        BufferedImage image = ImageIO.read(imagePath.toFile());
        if (image == null) {
            return;
        }
        int tileWidth = positiveInt(tileset, "tilewidth", imagePath);
        int tileHeight = positiveInt(tileset, "tileheight", imagePath);
        int margin = nonNegativeInt(tileset, "margin", 0, imagePath);
        int spacing = nonNegativeInt(tileset, "spacing", 0, imagePath);
        int computedColumns = (image.getWidth() - margin * 2 + spacing) / (tileWidth + spacing);
        int computedRows = (image.getHeight() - margin * 2 + spacing) / (tileHeight + spacing);
        if (computedColumns <= 0 || computedRows <= 0) {
            throw new GradleException("Tiled atlas has no complete cells: " + imagePath);
        }
        int columns = nonNegativeInt(tileset, "columns", computedColumns, imagePath);
        if (columns <= 0 || columns > computedColumns) {
            throw new GradleException("Invalid Tiled atlas column count for " + imagePath + ": " + columns);
        }
        int capacity = Math.multiplyExact(columns, computedRows);
        int tileCount = nonNegativeInt(tileset, "tilecount", capacity, imagePath);
        if (tileCount <= 0 || tileCount > capacity) {
            throw new GradleException("Invalid Tiled atlas tile count for " + imagePath + ": " + tileCount);
        }

        String resourcePath = resourceRoot.relativize(imagePath).toString().replace('\\', '/');
        TiledAtlas atlas = new TiledAtlas(
                imagePath,
                resourcePath,
                tileWidth,
                tileHeight,
                margin,
                spacing,
                columns,
                tileCount,
                image);
        TiledAtlas previous = atlases.putIfAbsent(imagePath, atlas);
        if (previous != null && !previous.hasSameLayout(atlas)) {
            throw new GradleException("Tiled atlas is referenced with conflicting layouts: " + imagePath);
        }
    }

    private static Path containingRoot(Path path, List<Path> roots) {
        for (Path root : roots) {
            if (path.startsWith(root)) {
                return root;
            }
        }
        return null;
    }

    private static int positiveInt(Element element, String name, Path imagePath) {
        int value = nonNegativeInt(element, name, -1, imagePath);
        if (value <= 0) {
            throw new GradleException("Missing or invalid " + name + " for Tiled atlas " + imagePath);
        }
        return value;
    }

    private static int nonNegativeInt(Element element, String name, int fallback, Path imagePath) {
        if (!element.hasAttribute(name) || element.getAttribute(name).isBlank()) {
            return fallback;
        }
        try {
            int value = Integer.parseInt(element.getAttribute(name));
            if (value < 0) {
                throw new NumberFormatException("negative value");
            }
            return value;
        } catch (NumberFormatException exception) {
            throw new GradleException("Invalid " + name + " for Tiled atlas " + imagePath, exception);
        }
    }

    private static List<Element> directChildren(Element parent, String name) {
        List<Element> children = new ArrayList<>();
        NodeList nodes = parent.getChildNodes();
        for (int i = 0; i < nodes.getLength(); i++) {
            Node node = nodes.item(i);
            if (node instanceof Element && name.equals(((Element) node).getTagName())) {
                children.add((Element) node);
            }
        }
        return children;
    }

    private static boolean isTiledDescriptor(Path path) {
        String name = path.getFileName().toString().toLowerCase(Locale.ROOT);
        return name.endsWith(".tsx") || name.endsWith(".tmx");
    }
}
