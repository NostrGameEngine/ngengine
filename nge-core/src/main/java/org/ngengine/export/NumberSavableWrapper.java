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

package org.ngengine.export;

import java.io.IOException;

import com.jme3.export.InputCapsule;
import com.jme3.export.JmeExporter;
import com.jme3.export.JmeImporter;
import com.jme3.export.OutputCapsule;

public class NumberSavableWrapper implements SavableWrapper<Number>{
    private Number number;
    
    protected NumberSavableWrapper(){

    }

    public NumberSavableWrapper(Number number){
        this.number = number;
    }

    @Override
    public void write(JmeExporter ex) throws IOException {
        OutputCapsule out = ex.getCapsule(this);
        if(number instanceof Integer){
            out.write(0,"t", 0);
            out.write(number.intValue(), "v", 0);
        } else if (number instanceof Long){
            out.write(1,"t", 0);
            out.write(number.longValue(), "v", 0L);
        } else if (number instanceof Float){
            out.write(2,"t", 0);
            out.write(number.floatValue(), "v", 0f);
        } else if (number instanceof Short){
            out.write(3,"t", 0);
            out.write(number.shortValue(), "v", (short)0);
        } else if (number instanceof Byte){
            out.write(4,"t", 0);
            out.write(number.byteValue(), "v", (byte)0);
        } else {
            out.write(4,"t", 0);
            out.write(number.doubleValue(), "v", 0d);
        }
    }

    @Override
    public void read(JmeImporter im) throws IOException {
        InputCapsule in = im.getCapsule(this);
        int t = in.readInt("t", 0);
        switch(t){
            case 0:
                number = Integer.valueOf(in.readInt("v", 0));
                break;
            case 1:
                number = Long.valueOf(in.readLong("v", 0L));
                break;
            case 2:
                number = Float.valueOf(in.readFloat("v", 0f));
                break;
            case 3:
                number = Short.valueOf(in.readShort("v", (short)0));
                break;
            case 4:
                number = Byte.valueOf(in.readByte("v", (byte)0));
                break;
            case 5:
            default:
                number = Double.valueOf(in.readDouble("v", 0d));
                break;  
        }
    }

    @Override
    public Number get() {
        return number;
    }
    
}
