package org.ngengine.player;

import java.util.regex.Pattern;

public class GamerTag {
    private static final Pattern GAMERTAG_PATTERN = Pattern.compile("^[a-zA-Z0-9_]+$");
    private final String tag;
    private final String code;

    protected GamerTag(String tag, String code){
        this.tag = tag;
        this.code = code;
    }

    @Override
    public String toString() {
        return tag + "#" + code;
    }

    public String getTag() {
        return tag;
    }

    public String getCode() {
        return code;
    }


    public static GamerTag parse(String uid, String gamerTag) {
        if (!uid.startsWith("npub1"))
            throw new IllegalArgumentException("Invalid UID. Should be a bech32 Nostr public key");
        if (gamerTag == null || gamerTag.isEmpty()) {
            throw new IllegalArgumentException("GamerTag cannot be null or empty");
        }
        String expectedGamerCode = uid.substring(5, 9);
        String gamerTagParts[] = gamerTag.split("#");
        if (gamerTagParts.length != 2) {
            throw new IllegalArgumentException("GamerTag should be in the format <tag>#<code>");
        }
        if (!gamerTagParts[1].equals(expectedGamerCode)) {
            throw new IllegalArgumentException("GamerTag code does not match the UID");
        }
        if (!GAMERTAG_PATTERN.matcher(gamerTagParts[0]).matches()) {
            throw new IllegalArgumentException(
                    "GamerTag contains invalid characters. Only alphanumeric and underscore are allowed");
        }
        return new GamerTag(gamerTagParts[0], gamerTagParts[1]);
    }

    public static GamerTag generate(String uid, String desiredName) {
        if (!uid.startsWith("npub1"))
            throw new IllegalArgumentException("Invalid UID. Should be a bech32 Nostr public key");
        if (desiredName == null || desiredName.isEmpty()) {
            throw new IllegalArgumentException("GamerTag cannot be null or empty");
        }
        String gamerCode = uid.substring(5, 9);
        String sanitizedTag = desiredName.replaceAll("[^a-zA-Z0-9_]", "_");
        if (!GAMERTAG_PATTERN.matcher(sanitizedTag).matches()) {
            throw new IllegalArgumentException(
                    "GamerTag contains invalid characters. Only alphanumeric and underscore are allowed");
        }
        sanitizedTag = sanitizedTag.substring(0, Math.min(sanitizedTag.length(), 16));
        return new GamerTag(sanitizedTag, gamerCode);
    }
}