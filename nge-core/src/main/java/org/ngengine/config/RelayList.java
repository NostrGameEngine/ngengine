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

package org.ngengine.config;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.ngengine.platform.NGEUtils;

public class RelayList {
    private static final Logger log = Logger.getLogger(RelayList.class.getName());
    private final List<String> defaultFallback;
    private final String unit;
    private final NGEAppSettings settings;

    public RelayList(NGEAppSettings settings, String unit,  List<String> defaultFallback) {
        this.settings = settings;
        this.unit = unit;
        this.defaultFallback = defaultFallback;
    }

 
    public void set(String group, List<String> relays){
        Map<Object,Object> raw = (Map<Object,Object>) settings.get("relays");
        if(raw==null){
            raw = new HashMap<>();
            settings.put("relays", raw);
        }
        Map<Object,Object> unitMap = (Map<Object,Object>) raw.get(this.unit);
        if(unitMap==null){
            unitMap = new HashMap<>();
            raw.put(this.unit, unitMap);
        }
        unitMap.put(group, relays);
        settings.checkRestartRequired("relatys");
    }


    private Map<String,List<String>> get(){        
        try{
            Map<Object,Object> raw = settings.get("relays");
            if(raw==null) return Map.of();
            raw = (Map<Object,Object>) raw.get(this.unit);               
            if(raw==null) return Map.of();
            Map<String,List<String>> parsed = new HashMap<>();
            for(Object e : raw.entrySet()){
                String key = NGEUtils.safeString(((Entry)e).getKey());
                List<String> vals = NGEUtils.safeStringList(((Entry)e).getValue());
                parsed.put(key, vals); 
            }
            return Collections.unmodifiableMap(parsed);        
        }catch(Exception ex){
            log.log(Level.WARNING, "Failed to parse "+this.unit+" relay config", ex);
        }
        return Map.of();
    }


    public List<String> get(String group){
        Map<String,List<String>> map = get();
        if(map != null){
            List<String> relays = map.get(group);
            if(relays==null)  relays = map.get("default");
            if(relays != null && !relays.isEmpty()){
                return relays;
            }
        }
        return defaultFallback;
    }
    
   
 
}
