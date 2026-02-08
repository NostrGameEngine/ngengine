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

import org.teavm.jso.core.JSArray;
import org.teavm.jso.core.JSBoolean;
import org.teavm.jso.core.JSNumber;
import org.teavm.jso.core.JSString;

import com.jme3.plugins.json.*;



public class TeaJSONElement implements JsonElement {
    protected org.teavm.jso.JSObject element;

    public TeaJSONElement( org.teavm.jso.JSObject  element) {
        this.element = element;
    }

    @Override
    public String getAsString() {
        JSString str=element.cast();
        return str.stringValue();
    }

    @Override
    public JsonObject getAsJsonObject() {
         
        return new TeaJSONObject(element.cast());
    }

    @Override
    public float getAsFloat() {
        JSNumber num=element.cast();
        return num.floatValue();
    }

    @Override
    public int getAsInt() {
        JSNumber num=element.cast();
        return num.intValue();
    }

    @Override
    public boolean getAsBoolean() {
        JSBoolean bool=element.cast();
        return bool.booleanValue();
    }

    @Override
    public JsonArray getAsJsonArray() {
        JSArray<?> arr=element.cast();
        return new TeaJSONArray(arr);
    }

    @Override
    public Number getAsNumber() {
        JSNumber num=element.cast();
        return num.doubleValue();
    }

    @Override
    public JsonPrimitive getAsJsonPrimitive() {
        return new TeaJSONPrimitive(element);
    }

    protected boolean isNull( org.teavm.jso.JSObject element) {
        if (element == null) return true;
        return false;
    }

    @Override
    public <T extends JsonElement> T autoCast() {
        if(isNull(element)) return null;

        if (element instanceof JSString || element instanceof JSNumber || element instanceof JSBoolean) {
            return (T) getAsJsonPrimitive();
        } else if (element instanceof JSArray) {
            return (T) getAsJsonArray();
        } else {
            return (T) getAsJsonObject();
        }
    }
    
}
