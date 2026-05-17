/*
 * $Id$
 * 
 * Copyright (c) 2016, Simsilica, LLC
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

package org.ngengine.gui;

import java.util.function.Predicate;

import org.ngengine.gui.style.ElementId;
import org.ngengine.gui.style.StyleAttribute;
import org.ngengine.gui.text.*;

/**
 * A special TextField implementation that displays an obscured version of the password the user has entered.
 * In all other ways, it acts exactyl like a TextField.
 *
 * @author Paul Speed, Riccardo Balbo
 */
public class PasswordField extends TextField {

    public static final String ELEMENT_ID = "password.textField";

    private Predicate<Character> allowed;
    private char outputChar;

    public PasswordField(String text) {
        this(text, new ElementId(ELEMENT_ID));
    }

    public PasswordField(ElementId elementId) {
        this("", elementId);
    }

    public PasswordField(String text, ElementId elementId) {
        super(text, elementId);
        setOutputCharacter('*');
        applyStyles(PasswordField.class);
    }

    /**
     * Sets the character used to obscure output. If set to null then the default '*' will be used.
     */
    @StyleAttribute(value = "outputCharacter", lookupDefault = false)
    public void setOutputCharacter(Character c) {
        this.outputChar = c == null ? '*' : c;
        setOutputTransform(TextFilters.constantTransform(this.outputChar));
    }

    public char getOutputCharacter() {
        return outputChar;
    }

    /**
     * Sets a predicate that returns true for characters that are allowed in the password field. All other
     * input will be skipped.
     */
    @StyleAttribute(value = "allowedCharacters", lookupDefault = false)
    public void setAllowedCharacters(Predicate<Character> allowed) {
        this.allowed = allowed;
        setInputTransform(TextFilters.charFilter(allowed));
    }

    public Predicate<Character> getAllowedCharacters() {
        return allowed;
    }

    @Override
    public String toString() {
        return getClass().getName() + "[text=" + getDisplayText() + ", color=" + getColor() + ", elementId="
                + getElementId() + "]";
    }

}
