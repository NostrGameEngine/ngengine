# Application localization

An NGE app can opt in by packaging UTF-8 properties catalogs as assets.
The defaults are `i18n/en.properties`, with both the base and selected language
set to `en`. An app can override the settings in `ngeapp.json`:

```json
{
    "BaseLanguage": "en",
    "Language": "it",
    "LocalizationPath": "i18n"
}
```

The selected catalog overlays the base by key. If both language codes match,
the asset is read just once. A missing catalog is optional; a missing key
returns its own name, or a supplied fallback. Language filenames use
lowercase, hyphen-separated codes.

```java
String title = app.getLocalization().get("menu.title");
String delivery = app.getLocalization().format("delivery.ready", itemName);
```

Templates accept positional tokens such as `{0}`. Plain text and inserted
values are literal, including apostrophes. To reload on the application thread
after changing the settings, call `app.reloadLocalization()`, then refresh
any displayed text. The API does not change the JVM locale or discover
classpath resource bundles.
