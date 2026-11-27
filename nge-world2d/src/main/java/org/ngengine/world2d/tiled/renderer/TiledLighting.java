package org.ngengine.world2d.tiled.renderer;

import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector4f;
import com.jme3.texture.Image;
import com.jme3.texture.Texture;
import com.jme3.texture.Texture2D;
import com.jme3.texture.image.ColorSpace;
import com.jme3.util.BufferUtils;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import org.ngengine.world2d.tiled.core.TiledBase;
import org.ngengine.world2d.tiled.core.TiledLayer;
import org.ngengine.world2d.tiled.core.TiledMap;
import org.ngengine.world2d.tiled.core.TiledObjectLayer;
import org.ngengine.world2d.tiled.core.TiledTileLayer;
import org.ngengine.world2d.tiled.core.tileset.Tile;
import org.ngengine.world2d.tiled.util.CoordinateSystem;

/** A shared, low-resolution RGB multiplier in the map's X/Z plane, independent of tile albedo. */
final class TiledLighting {
    static final int INHERIT = 0;
    static final int DYNAMIC = 1;
    static final int UNSHADED = 2;
    static final int INSTANCE_MODE_SHIFT = 3;
    private static final int MASK_LIMIT = 160;
    private static final int POINT = 0, SPOT = 1, DIRECTIONAL = 2, SHADOW = 3;

    private final int columns;
    private final int rows;
    private final Vector4f rect;
    private final ByteBuffer texels;
    private final Texture2D texture;
    private final float[] light;
    private final float[] shade;
    private List<Emitter> previous = List.of();
    private final List<Emitter> current = new ArrayList<>();
    private final Map<TiledTileLayer, TileEmitters> tileEmitters = new IdentityHashMap<>();
    private final Vector2f position = new Vector2f();
    private float previousAmbient = -1f;

    TiledLighting(float minX, float minY, float width, float height) {
        rect = new Vector4f(minX, minY, 1f / width, 1f / height);
        float scale = MASK_LIMIT / Math.max(width, height);
        columns = Math.max(1, Math.round(width * scale));
        rows = Math.max(1, Math.round(height * scale));
        texels = BufferUtils.createByteBuffer(columns * rows * 4);
        light = new float[columns * rows * 3];
        shade = new float[columns * rows];
        texture = new Texture2D(new Image(Image.Format.RGBA8, columns, rows, texels, ColorSpace.Linear));
        texture.setMinFilter(Texture.MinFilter.BilinearNoMipMaps);
        texture.setMagFilter(Texture.MagFilter.Bilinear);
        texture.setWrap(Texture.WrapMode.EdgeClamp);
    }

    Texture2D texture() {
        return texture;
    }

    Vector4f rect() {
        return rect;
    }

    static int mode(TiledBase entry) {
        Object value = entry == null ? null : entry.getProperty("lit");
        if (value == null) return INHERIT;
        switch (value.toString().trim().toLowerCase(Locale.ROOT)) {
            case "true":
            case "dynamic":
            case "shaded":
                return DYNAMIC;
            case "false":
            case "unshaded":
                return UNSHADED;
            default:
                return INHERIT;
        }
    }

    static int tileMode(TiledBase entry, Tile tile) {
        int mode = mode(entry);
        if (mode != INHERIT) return mode;
        mode = mode(tile);
        return mode == INHERIT && tile != null ? mode(tile.getTileset()) : mode;
    }

    static int layerMode(TiledLayer layer, TiledMap map) {
        for (TiledLayer current = layer; current != null; current = current.getParent()) {
            int mode = mode(current);
            if (mode != INHERIT) return mode;
        }
        return mode(map) == DYNAMIC ? DYNAMIC : UNSHADED;
    }

    void update(TiledMap map, CoordinateSystem coords, Function<TiledLayer, Vector2f> offset) {
        current.clear();
        for (TiledLayer layer : map.getLayersFlat()) {
            if (!visible(layer)) continue;
            if (layer instanceof TiledObjectLayer) {
                Vector2f shift = offset.apply(layer);
                for (var object : ((TiledObjectLayer) layer).getObjects()) {
                    if (!object.isVisible()) continue;
                    Tile tile = object.getTile();
                    if (!emits(object) && !emits(tile)) continue;
                    coords.gridToWorldSpace((float) object.getX(), (float) object.getY(), position);
                    collect(current, object, tile, position.x + shift.x, position.y + shift.y,
                        (float) object.getRotation(), map.getTileWidth());
                }
            } else if (layer instanceof TiledTileLayer) {
                TiledTileLayer tiles = (TiledTileLayer) layer;
                Vector2f shift = offset.apply(layer);
                TileEmitters cached = tileEmitters.computeIfAbsent(tiles, ignored -> new TileEmitters());
                if (cached.version != tiles.getUpdateId() || !cached.offset.equals(shift)) {
                    cached.version = tiles.getUpdateId();
                    cached.offset.set(shift);
                    cached.values.clear();
                    for (int y = tiles.getY(); y < tiles.getY() + tiles.getHeight(); y++) {
                        for (int x = tiles.getX(); x < tiles.getX() + tiles.getWidth(); x++) {
                            var entry = tiles.getTileAt(x, y);
                            Tile tile = entry != null ? entry.getTile() : null;
                            if (!emits(entry) && !emits(tile)) continue;
                            coords.tileToWorldSpace(x, y, position);
                            collect(cached.values, entry, tile, position.x + shift.x, position.y + shift.y,
                                0f, map.getTileWidth());
                        }
                    }
                }
                current.addAll(cached.values);
            }
        }
        float ambient = clamp(number(map.getProperty("light.ambient"), .72f));
        if (ambient == previousAmbient && current.equals(previous)) return;
        previousAmbient = ambient;
        previous = new ArrayList<>(current);
        Arrays.fill(light, ambient);
        Arrays.fill(shade, 1f);
        for (Emitter emitter : current) {
            stamp(emitter);
        }
        for (int i = 0; i < shade.length; i++) {
            int pixel = i * 4;
            int rgb = i * 3;
            texels.put(pixel, encode(light[rgb] * shade[i]));
            texels.put(pixel + 1, encode(light[rgb + 1] * shade[i]));
            texels.put(pixel + 2, encode(light[rgb + 2] * shade[i]));
            texels.put(pixel + 3, (byte) 255);
        }
        texture.getImage().setUpdateNeeded();
    }

    private static boolean visible(TiledLayer layer) {
        for (TiledLayer current = layer; current != null; current = current.getParent()) {
            if (!current.isVisible()) return false;
        }
        return true;
    }

    private static boolean emits(TiledBase entry) {
        return entry != null && (entry.getProperty("light.type") != null || entry.getProperty("shadow.radius") != null);
    }

    private static Object property(TiledBase entry, Tile tile, String key) {
        Object value = entry == null ? null : entry.getProperty(key);
        return value == null && tile != null ? tile.getProperty(key) : value;
    }

    private static void collect(List<Emitter> output, TiledBase entry, Tile tile,
                                float x, float y, float rotation, float tileWidth) {
        Object type = property(entry, tile, "light.type");
        if (type != null) {
            int kind;
            switch (type.toString().toLowerCase(Locale.ROOT)) {
                case "point": kind = POINT; break;
                case "spot": kind = SPOT; break;
                case "directional": kind = DIRECTIONAL; break;
                default: kind = -1;
            }
            if (kind >= 0) {
                Object color = property(entry, tile, "light.color");
                ColorRGBA tint = color instanceof ColorRGBA ? (ColorRGBA) color : ColorRGBA.White;
                output.add(new Emitter(kind, x, y, rotation, "light.", entry, tile,
                    setting(entry, tile, "light.range", tileWidth * 2f), tint));
            }
        }
        float radius = setting(entry, tile, "shadow.radius", 0f);
        if (radius > 0f) {
            output.add(new Emitter(SHADOW, x, y, rotation, "shadow.", entry, tile, radius, ColorRGBA.White));
        }
    }

    private void stamp(Emitter emitter) {
        float xStep = 1f / (rect.z * columns);
        float yStep = 1f / (rect.w * rows);
        float radius = emitter.values[2];
        float yRadius = radius * emitter.values[3];
        if (radius <= 0f || yRadius <= 0f) return;
        boolean global = emitter.kind == DIRECTIONAL;
        float originX = emitter.values[0];
        float originY = emitter.values[1];
        int startX = global ? 0 : Math.max(0, (int) ((originX - radius - rect.x) / xStep));
        int startY = global ? 0 : Math.max(0, (int) ((originY - yRadius - rect.y) / yStep));
        int endX = global ? columns : Math.min(columns, 1 + (int) ((originX + radius - rect.x) / xStep));
        int endY = global ? rows : Math.min(rows, 1 + (int) ((originY + yRadius - rect.y) / yStep));
        for (int y = startY; y < endY; y++) {
            float dy = (rect.y + (y + .5f) * yStep - originY) / yRadius;
            for (int x = startX; x < endX; x++) {
                float dx = (rect.x + (x + .5f) * xStep - originX) / radius;
                float amount = global ? 1f : emitter.attenuation(dx, dy);
                int index = y * columns + x;
                if (emitter.kind == SHADOW) {
                    shade[index] *= 1f - clamp(amount * emitter.values[4]);
                } else {
                    int rgb = index * 3;
                    amount *= emitter.values[4];
                    light[rgb] += amount * emitter.values[5];
                    light[rgb + 1] += amount * emitter.values[6];
                    light[rgb + 2] += amount * emitter.values[7];
                }
            }
        }
    }

    private static byte encode(float value) {
        return (byte) Math.round(clamp(value) * 255f);
    }

    private static float clamp(float value) {
        return Math.max(0f, Math.min(1f, value));
    }

    private static float setting(TiledBase entry, Tile tile, String key, float fallback) {
        return number(property(entry, tile, key), fallback);
    }

    private static float number(Object value, float fallback) {
        if (value == null) return fallback;
        try {
            float number = value instanceof Number ? ((Number) value).floatValue() : Float.parseFloat(value.toString());
            return Float.isFinite(number) ? number : fallback;
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    private static final class TileEmitters {
        int version = Integer.MIN_VALUE;
        final Vector2f offset = new Vector2f();
        final List<Emitter> values = new ArrayList<>();
    }

    private static final class Emitter {
        final int kind;
        final float[] values;

        Emitter(int kind, float x, float y, float rotation, String prefix, TiledBase entry, Tile tile,
                float radius, ColorRGBA color) {
            this.kind = kind;
            double radians = Math.toRadians(rotation);
            float offsetX = setting(entry, tile, prefix + "x", 0f);
            float offsetY = setting(entry, tile, prefix + "y", 0f);
            x += offsetX * Math.cos(radians) - offsetY * Math.sin(radians);
            y += offsetX * Math.sin(radians) + offsetY * Math.cos(radians);
            double direction = radians + Math.toRadians(setting(entry, tile, "light.direction", 90f));
            double halfCone = Math.toRadians(Math.max(1f, Math.min(179f, setting(entry, tile, "light.cone", 70f))) * .5f);
            float aspect = Math.max(.05f, setting(entry, tile, prefix + "aspect", kind == SHADOW ? .45f : 1f));
            values = new float[] { x, y, radius, aspect,
                Math.max(0f, setting(entry, tile, prefix + "strength", kind == SHADOW ? .28f : .65f)),
                color.r, color.g, color.b, (float) Math.cos(direction), (float) Math.sin(direction),
                (float) Math.cos(halfCone), (float) Math.cos(halfCone * .65f) };
        }

        float attenuation(float dx, float dy) {
            float distance = (float) Math.sqrt(dx * dx + dy * dy);
            if (distance >= 1f) return 0f;
            float amount = 1f - distance;
            amount *= amount * (3f - 2f * amount);
            if (kind == SPOT && distance > .0001f) {
                float actualY = dy * values[3];
                float actualDistance = (float) Math.sqrt(dx * dx + actualY * actualY);
                float dot = (values[8] * dx + values[9] * actualY) / actualDistance;
                amount *= clamp((dot - values[10]) / (values[11] - values[10]));
            }
            return amount;
        }

        @Override
        public boolean equals(Object other) {
            if (!(other instanceof Emitter)) return false;
            Emitter emitter = (Emitter) other;
            return kind == emitter.kind && Arrays.equals(values, emitter.values);
        }

        @Override
        public int hashCode() {
            return 31 * kind + Arrays.hashCode(values);
        }
    }
}
