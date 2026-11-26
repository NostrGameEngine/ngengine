package org.ngengine;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.jme3.asset.AssetKey;
import com.jme3.asset.AssetManager;
import org.ngengine.config.NGEAppSettings;

/**
 * An app's UTF-8 message catalog. The selected catalog overlays the base
 * catalog by key. No global locale, bundle discovery, or platform-specific lookup.
 */
public final class AppLocalization {
    private static final Logger LOG = Logger.getLogger(AppLocalization.class.getName());
    static final AppLocalization EMPTY = new AppLocalization("en", Map.of());
    private final String language;
    private final Map<String, String> messages;

    private AppLocalization(String language, Map<String, String> messages) {
        this.language = language;
        this.messages = Map.copyOf(messages);
    }

    public String getLanguage() {
        return language;
    }

    /** Returns the key if neither catalog defines it. */
    public String get(String key) {
        return getOrDefault(key, key);
    }

    public String getOrDefault(String key, String fallback) {
        return messages.getOrDefault(Objects.requireNonNull(key, "key"), fallback);
    }

    /** Replaces positional tokens such as {0}. Apostrophes and replacements are literal. */
    public String format(String key, Object... arguments) {
        return interpolate(get(key), arguments);
    }

    public String formatOrDefault(String key, String fallback, Object... arguments) {
        return interpolate(getOrDefault(key, fallback), arguments);
    }

    static AppLocalization load(AssetManager assets, NGEAppSettings settings) {
        return load(settings.getLocalizationPath(), settings.getBaseLanguage(), settings.getLanguage(), name -> {
            var asset = assets.locateAsset(new AssetKey<>(name));
            return asset != null ? asset.openStream() : null;
        });
    }

    @FunctionalInterface
    interface CatalogSource {
        InputStream open(String name) throws IOException;
    }

    static AppLocalization load(String directory, String base, String selected, CatalogSource source) {
        String baseLanguage = languageCode(base, "en");
        String language = languageCode(selected, baseLanguage);
        String prefix = directory == null || directory.isBlank() ? "i18n" : directory.trim();
        if (!prefix.endsWith("/")) prefix += "/";
        Map<String, String> merged = new HashMap<>();
        readCatalog(source, prefix + baseLanguage + ".properties", merged);
        if (!baseLanguage.equals(language)) {
            readCatalog(source, prefix + language + ".properties", merged);
        }
        return new AppLocalization(language, merged);
    }

    private static void readCatalog(CatalogSource source, String path, Map<String, String> target) {
        try (InputStream stream = source.open(path)) {
            if (stream == null) return; // Catalogs are optional for applications.
            Properties properties = new Properties();
            properties.load(new InputStreamReader(stream, StandardCharsets.UTF_8));
            for (String key : properties.stringPropertyNames()) {
                target.put(key, properties.getProperty(key));
            }
        } catch (IOException | IllegalArgumentException e) {
            LOG.log(Level.WARNING, "Could not read language catalog " + path, e);
        }
    }

    private static String languageCode(String value, String fallback) {
        if (value == null || value.isBlank()) return fallback;
        String code = value.trim().replace('_', '-');
        StringBuilder normalized = new StringBuilder(code.length());
        for (int i = 0; i < code.length(); i++) {
            char c = code.charAt(i);
            if (c >= 'A' && c <= 'Z') c += 'a' - 'A';
            if (!(c >= 'a' && c <= 'z' || c >= '0' && c <= '9' || c == '-')) return fallback;
            normalized.append(c);
        }
        return normalized.toString();
    }

    private static String interpolate(String pattern, Object[] args) {
        if (args.length == 0 || pattern.indexOf('{') < 0) return pattern;
        StringBuilder result = new StringBuilder(pattern.length());
        for (int i = 0; i < pattern.length(); i++) {
            char c = pattern.charAt(i);
            if (c == '{') {
                int next = i + 1;
                int index = 0;
                while (next < pattern.length() && index < args.length) {
                    char digit = pattern.charAt(next);
                    if (digit < '0' || digit > '9') break;
                    index = index * 10 + digit - '0';
                    next++;
                }
                if (next > i + 1 && next < pattern.length() && pattern.charAt(next) == '}'
                        && index >= 0 && index < args.length) {
                    result.append(args[index]);
                    i = next;
                    continue;
                }
            }
            result.append(c);
        }
        return result.toString();
    }
}
