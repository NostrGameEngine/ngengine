/*
 * $Id$
 * 
 * Copyright (c) 2020, Simsilica, LLC
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions 
 * are met:
 * 
 * 1. Redistributions of source code must retain the above copyright 
 *    notice, this list of conditions and the following disclaimer.
 * 
 * 2. Redistributions in binary form must reproduce the above copyright 
 *    notice, this list of conditions and the following disclaimer in 
 *    the documentation and/or other materials provided with the 
 *    distribution.
 * 
 * 3. Neither the name of the copyright holder nor the names of its 
 *    contributors may be used to endorse or promote products derived 
 *    from this software without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS 
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT 
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS 
 * FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE 
 * COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, 
 * INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES 
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR 
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) 
 * HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, 
 * STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) 
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED 
 * OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package com.simsilica.lemur;


import com.simsilica.lemur.value.TextFieldValueEditor;

import java.util.function.Function;

import com.simsilica.lemur.text.TextFilters;

/**
 *  Factory methods for creating standard/common ValueEditors. 
 *
 *  @author    Paul Speed
 */
public class ValueEditors {

    public static TextFieldValueEditor<Double> doubleEditor( String format ) {
        TextFieldValueEditor<Double> result = new TextFieldValueEditor<>(ValueRenderers.formatString(format),
                                                                         toDouble());                                                                         
        result.getDocumentModelFilter().setInputTransform( 
                    TextFilters.charFilter(
                        TextFilters.isInChars('-', '.', '0', '1', '2', '3', '4', '5', '6', '7', '8', '9')));
        return result;
    }

    public static Function<String, Double> toDouble() {
        return new StringToDouble();
    }
    
    public static class StringToDouble implements Function<String, Double> {
        public Double apply( String s ) {
            if( s == null ) {
                return null;
            }
            return Double.parseDouble(s);
        }
    }
}
