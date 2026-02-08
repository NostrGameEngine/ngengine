/**
 * Copyright (c) 2025-2026, Nostr Game Engine
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

package org.ngengine.export;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

import com.jme3.export.InputCapsule;
import com.jme3.export.OutputCapsule;
import com.jme3.scene.UserData;

@SuppressWarnings("unchecked")
public class ExportUtils {

 

    public static void writeMap(Map<String,?> map, OutputCapsule out) throws IOException {
        UserData data = new UserData(UserData.getObjectType(map), map);
        out.write(data, "data", null);        
    }

    public static <T> Map<String,T> readMap(InputCapsule in) throws IOException {
        UserData data = (UserData) in.readSavable("data", null);
        if(data == null){
            return null;
        }
        return (Map<String,T>) data.getValue();
    }

    public static void writeCollection(Collection<Object> col, OutputCapsule out) throws IOException {
        ArrayList<Object> s = new ArrayList<>(col);
        UserData data = new UserData(UserData.getObjectType(s), s);
        out.write(data, "data", null);        
    }

    public static Collection<Object> readCollection(InputCapsule in) throws IOException {
        UserData data = (UserData) in.readSavable("data", null);
        if(data == null){
            return null;
        }
        return (Collection<Object>) data.getValue();
    }

    public static void writeArray(Object[] arr, OutputCapsule out) throws IOException {
        ArrayList<Object> s = new ArrayList<>();
        for(Object o : arr){
            s.add(o);
        }
        UserData data = new UserData(UserData.getObjectType(s), s);
        out.write(data, "data", null);        
    }

    public static Object[] readArray(InputCapsule in) throws IOException {
        UserData data = (UserData) in.readSavable("data", null);
        if(data == null){
            return null;
        }
        Collection<Object> col = (Collection<Object>) data.getValue();
        return col.toArray(new Object[0]);
    }
    
}
