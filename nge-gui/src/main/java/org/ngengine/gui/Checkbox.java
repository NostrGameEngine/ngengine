/*
 * $Id$
 *
 * Copyright (c) 2012-2012 jMonkeyEngine
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 * * Redistributions of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 *
 * * Redistributions in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer in the
 *   documentation and/or other materials provided with the distribution.
 *
 * * Neither the name of 'jMonkeyEngine' nor the names of its contributors
 *   may be used to endorse or promote products derived from this software
 *   without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
 * TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package org.ngengine.gui;

import org.ngengine.gui.style.ElementId;
import org.ngengine.gui.style.StyleAttribute;
import org.ngengine.gui.style.Styles;
import org.ngengine.gui.core.GuiComponent;
import org.ngengine.gui.core.VersionedReference;



/**
 *  A standard Checkbox GUI element that can be used to toggle
 *  a boolean state represented by a CheckboxModel.
 *
 *  @author    Paul Speed
 */
public class Checkbox extends Button {

    public static final String ELEMENT_ID = "checkbox";

    public static final Command<Button> TOGGLE_COMMAND = new ToggleCommand();

    private CheckboxModel model;
    private VersionedReference<Boolean> state;
    private GuiComponent onView;
    private GuiComponent offView;

    public Checkbox() {
        this("");
    }

    public Checkbox(String s){
        this(s, (CheckboxModel)null);
    }

    public Checkbox(String s, CheckboxModel model ) {
        this(s, model, new ElementId(ELEMENT_ID));
    }

    public Checkbox(String s, ElementId elementId ) {
        this(s, null, elementId);
    }

    public Checkbox(String s, CheckboxModel model, ElementId elementId ) {
        super(s,elementId);
        setModel(model == null ? new DefaultCheckboxModel() : model);
        setupCommands();
        applyStyles(Checkbox.class);
    }

    protected final void setupCommands() {
        addCommands(ButtonAction.Click, Checkbox.TOGGLE_COMMAND);
    }

    public void setModel( CheckboxModel model ) {
        if( this.model == model )
            return;
        this.model = model;
        this.state = model.createReference();
        resetStateView();
    }

    public CheckboxModel getModel() {
        return model;
    }

    protected void setStateView( GuiComponent c ) { 
        setIcon(c);
    }

    protected void resetStateView() {
        setStateView(isChecked() ? onView : offView);
    }

    @Override
    public void updateLogicalState(float tpf) {
        super.updateLogicalState(tpf);
        if( state.update() ) {
            resetStateView();
        }
    }

    public void setChecked( boolean b ) {
        getModel().setChecked(b);
    }

    public boolean isChecked() {
        if( getModel() == null )
            return false;
        return getModel().isChecked();
    }

    @StyleAttribute(value="onView", lookupDefault=false)
    public void setOnView( GuiComponent c ) {
        if( this.onView == c )
            return;

        this.onView = c.clone();
        resetStateView();
    }

    public GuiComponent getOnView() {
        return onView;
    }

    @StyleAttribute(value="offView", lookupDefault=false)
    public void setOffView( GuiComponent c ) {
        if( this.onView == c )
            return;

        this.offView = c.clone();
        resetStateView();
    }

    public GuiComponent getOffView() {
        return offView;
    }

    @Override
    public String toString() {
        return getClass().getName() + "[text=" + getText() + ", state=" + isChecked() + ", color=" + getColor() + ", elementId=" + getElementId() + "]";
    }

    protected static class ToggleCommand implements Command<Button> {

        public void execute( Button source ) {
            if( source instanceof Checkbox ) {
                Checkbox cb = (Checkbox)source;
                cb.setChecked(!cb.isChecked());
            }
        }
    }
}



