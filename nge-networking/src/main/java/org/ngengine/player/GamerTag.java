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
package org.ngengine.player;

import java.util.regex.Pattern;

public class GamerTag {

    private static final Pattern GAMERTAG_PATTERN = Pattern.compile("^[a-zA-Z0-9_]+$");
    private final String tag;
    private final String code;

    protected GamerTag(String tag, String code) {
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
        if (!uid.startsWith("npub1")) throw new IllegalArgumentException("Invalid UID. Should be a bech32 Nostr public key");
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
        if (gamerTagParts[0].length() < 3) {
            throw new IllegalArgumentException("GamerTag is too short. Minimum length is 3 characters");
        }
        if (gamerTagParts[0].length() > 21) {
            throw new IllegalArgumentException("GamerTag is too long. Maximum length is 21 characters");
        }
        if (!GAMERTAG_PATTERN.matcher(gamerTagParts[0]).matches()) {
            throw new IllegalArgumentException(
                "GamerTag contains invalid characters. Only alphanumeric and underscore are allowed"
            );
        }
        return new GamerTag(gamerTagParts[0], gamerTagParts[1]);
    }

    public static GamerTag generate(String uid, String desiredName) {
        if (!uid.startsWith("npub1")) throw new IllegalArgumentException("Invalid UID. Should be a bech32 Nostr public key");
        if (desiredName == null || desiredName.isEmpty()) {
            throw new IllegalArgumentException("GamerTag cannot be null or empty");
        }
        String gamerCode = uid.substring(5, 9);
        String sanitizedTag = desiredName.replaceAll("[^a-zA-Z0-9_]", "_");
        if (!GAMERTAG_PATTERN.matcher(sanitizedTag).matches()) {
            throw new IllegalArgumentException(
                "GamerTag contains invalid characters. Only alphanumeric and underscore are allowed"
            );
        }
        sanitizedTag = sanitizedTag.substring(0, Math.min(sanitizedTag.length(), 16));
        return new GamerTag(sanitizedTag, gamerCode);
    }
}
