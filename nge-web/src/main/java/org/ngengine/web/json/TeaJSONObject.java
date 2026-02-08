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
 * the BSD 3-Clause License. 
 */

package org.ngengine.web.json;

import java.util.Set;
import java.util.Map.Entry;

import org.teavm.jso.core.JSMapLike;
import org.teavm.jso.core.JSObjects;

import com.jme3.plugins.json.*;



public class TeaJSONObject extends TeaJSONElement implements JsonObject {
 
    public TeaJSONObject(JSMapLike gsonObject) {
        super(gsonObject);
    }

    private JSMapLike obj() {
        return (JSMapLike) element;
    }

    @Override
    public JsonArray getAsJsonArray(String string) {
        if (!JSObjects.hasProperty(element, string)) return null;
        return new TeaJSONArray(((org.teavm.jso.JSObject)obj().get(string)).cast());
    }

    @Override
    public JsonObject getAsJsonObject(String string) {
        if (!JSObjects.hasProperty(element, string)) return null;
        return new TeaJSONObject(((org.teavm.jso.JSObject)obj().get(string)).cast());
    }

    @Override
    public boolean has(String string) {        
        return JSObjects.hasProperty(element, string);
    }

    @Override
    public JsonElement get(String string) {
        if (!JSObjects.hasProperty(element, string)) return null;
        return new TeaJSONElement(((org.teavm.jso.JSObject)obj().get(string)));
        
    }

    @Override
    public Entry<String, JsonElement>[] entrySet() {
        String keys[]=JSObjects.getOwnPropertyNames(obj());
        Entry<String, JsonElement>[] entries = new Entry[keys.length];
        int i = 0;
        for (String key: keys) {

            Entry<String, JsonElement> e = new Entry<String, JsonElement>() {
                @Override
                public String getKey() {
                    return key;
                }

                @Override
                public TeaJSONElement getValue() {
                    return new TeaJSONElement((org.teavm.jso.JSObject)obj().get(key));
                }

                @Override
                public TeaJSONElement setValue(JsonElement value) {
                    throw new UnsupportedOperationException("Unimplemented method 'setValue'");
                }
            };

            entries[i++] = e;
        }
        return entries;
        
    }

    @Override
    public JsonPrimitive getAsJsonPrimitive(String string) {
        if (!JSObjects.hasProperty(element, string)) return null;
        return new TeaJSONPrimitive(((org.teavm.jso.JSObject)obj().get(string)).cast());
    }
}