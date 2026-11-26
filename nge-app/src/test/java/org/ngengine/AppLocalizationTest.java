package org.ngengine;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class AppLocalizationTest {
    @Test
    void baseIsLoadedFirstAndSelectedOnlyOverwritesPresentKeys() {
        var visited = new ArrayList<String>();
        var sources = Map.of(
            "messages/en.properties", "name=Job\norder=Order {0}\nonlyBase=Only in English\n",
            "messages/it.properties", "order=Ordina {0}\naccent=È qui\n"
        );
        AppLocalization bundle = AppLocalization.load("messages/", "en", "IT", name -> {
            visited.add(name);
            String text = sources.get(name);
            return text == null ? null : bytes(text);
        });
        assertEquals(List.of("messages/en.properties", "messages/it.properties"), visited);
        assertEquals("it", bundle.getLanguage());
        assertEquals("È qui", bundle.get("accent"));
        assertEquals("Ordina l'adattatore", bundle.format("order", "l'adattatore"));
        assertEquals("Only in English", bundle.get("onlyBase"));
        assertEquals("missing", bundle.get("missing"));
    }

    @Test
    void defaultLanguageIsReadOnceAndTemplatingDoesNotInterpretInsertedText() {
        var visited = new ArrayList<String>();
        AppLocalization bundle = AppLocalization.load("", "en", "en", name -> {
            visited.add(name);
            return bytes("job=We've got {1} for {0}.\n");
        });
        assertEquals(List.of("i18n/en.properties"), visited);
        assertEquals("We've got {0} for Arthur.", bundle.format("job", "Arthur", "{0}"));
        assertEquals("test {0} {99}", bundle.formatOrDefault("unknown", "test {0} {99}", "{0}"));
    }

    @Test
    void missingOptionalCatalogKeepsTheBase() {
        AppLocalization bundle = AppLocalization.load("i18n", "en", "it", name ->
            name.equals("i18n/en.properties") ? bytes("ready=Ready") : null);
        assertEquals("Ready", bundle.get("ready"));
    }

    private static ByteArrayInputStream bytes(String text) {
        return new ByteArrayInputStream(text.getBytes(StandardCharsets.UTF_8));
    }
}
