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

package com.simsilica.lemur;

import java.util.Objects;
import java.util.logging.Logger;

import com.jme3.input.event.MouseButtonEvent;
import com.jme3.input.event.MouseMotionEvent;
import com.jme3.scene.Spatial;

import com.simsilica.lemur.core.*;
import com.simsilica.lemur.component.SpringGridLayout;
 
import com.simsilica.lemur.focus.FocusListener;
import com.simsilica.lemur.focus.NavigatorListener;
import com.simsilica.lemur.focus.ScrollDirection;
import com.simsilica.lemur.focus.TraversalDirection;
import com.simsilica.lemur.style.*;
import com.simsilica.lemur.value.DefaultValueRenderer;
import com.simsilica.lemur.input.InputMapper.ViewState;

/**
 * A GUI element that allows 'spinning' through a potentially unbound sequence of values.
 *
 * Adapted for Navigator-based focus + scroll input: - Up/Down navigation spins values (consumes navigation) -
 * Scroll spins values - Action (release) starts editing (instead of auto-edit on focus)
 */
public class Spinner<T> extends Panel {

    static Logger log = Logger.getLogger(Spinner.class.getName());

    public static final String ELEMENT_ID = "spinner";
    public static final String VALUE_ID = "value";
    public static final String EDITOR_ID = "editor";
    public static final String BUTTON_PANEL_ID = "buttons.container";
    public static final String UP_ID = "up.button";
    public static final String DOWN_ID = "down.button";

    public static final String EFFECT_NEXT = "next";
    public static final String EFFECT_PREVIOUS = "previous";
    public static final String EFFECT_ACTIVATE = "activate";
    public static final String EFFECT_DEACTIVATE = "deactivate";
    public static final String EFFECT_START_EDIT = "startEdit";
    public static final String EFFECT_STOP_EDIT = "stopEdit";
    public static final String EFFECT_FOCUS = "focus";
    public static final String EFFECT_UNFOCUS = "unfocus";
    public static final String EFFECT_ENABLE = "enable";
    public static final String EFFECT_DISABLE = "disable";

    public enum SpinnerAction {
        PreviousValue, NextValue, StartEdit, StopEdit, HighlightOn, HighlightOff, FocusGained, FocusLost, Hover, Enabled, Disabled
    };

    private SequenceModel<T> model;
    private VersionedReference<T> modelRef;

    private ValueRenderer<T> valueRenderer;
    private ValueEditor<T> valueEditor;

    private SpringGridLayout layout;
    private Panel view;
    private Panel edit;
    private Button previous;
    private Button next;

    private final SpinnerFocusHandler focusHandler = new SpinnerFocusHandler();

    private boolean enabled = true;
    private final CommandMap<Spinner, SpinnerAction> commandMap = new CommandMap<>(this);

    public Spinner(SequenceModel<T> model) {
        this(true, model, null, new ElementId(ELEMENT_ID), null);
    }

    public Spinner(SequenceModel<T> model, ValueRenderer<T> valueRenderer) {
        this(true, model, valueRenderer, new ElementId(ELEMENT_ID), null);
    }

    public Spinner(SequenceModel<T> model, String style) {
        this(true, model, null, new ElementId(ELEMENT_ID), style);
    }

    public Spinner(SequenceModel<T> model, ValueRenderer<T> valueRenderer, String style) {
        this(true, model, valueRenderer, new ElementId(ELEMENT_ID), style);
    }

    public Spinner(SequenceModel<T> model, ValueRenderer<T> valueRenderer, ElementId elementId,
            String style) {
        this(true, model, valueRenderer, elementId, style);
    }

    protected Spinner(boolean applyStyles, SequenceModel<T> model, ValueRenderer<T> valueRenderer,
            ElementId elementId, String style) {
        super(false, elementId, style);

        this.layout = new SpringGridLayout(Axis.Y, Axis.X, FillMode.ForcedEven, FillMode.First);
        getControl(GuiControl.class).setLayout(layout);

        if (valueRenderer == null) {
            valueRenderer = new DefaultValueRenderer<>(elementId.child(VALUE_ID), style);
        } else {
            valueRenderer.configureStyle(elementId.child(VALUE_ID), style);
        }
        this.valueRenderer = valueRenderer;

        if (applyStyles) {
            Styles styles = GuiGlobals.getInstance().getStyles();
            styles.applyStyles(this, elementId, style);
        }

        // Build children after styles
        Container buttons = layout.addChild(new Container(elementId.child(BUTTON_PANEL_ID), style), 0, 1);
        this.next = buttons.addChild(new Button(null, elementId.child(UP_ID), style));
        this.previous = buttons.addChild(new Button(null, elementId.child(DOWN_ID), style));

        FocusBlocker focusBlocker = new FocusBlocker();
        next.getControl(GuiControl.class).addNavigatorListener(focusBlocker);
        next.addClickCommands(new Command<Button>() {
            public void execute(Button source) {
                nextValue();
            }
        });

        previous.getControl(GuiControl.class).addNavigatorListener(focusBlocker);
        previous.addClickCommands(new Command<Button>() {
            public void execute(Button source) {
                previousValue();
            }
        });

        setModel(model);
    }

    private class FocusBlocker implements NavigatorListener{
        public boolean beforeNavigatorNavigateTo(TraversalDirection dir, Spatial from, Spatial to) {
            if(to == next || to == previous) return false;
            return true;
        }
    }

    @Override
    public void updateLogicalState(float tpf) {
        super.updateLogicalState(tpf);

        if (modelRef == null || modelRef.update()) {
            resetValue();
        }
        if (valueEditor != null) {
            if (edit != null && !valueEditor.updateState(tpf)) {
                stopEditing();
            }
        }
    }

    public void setModel(SequenceModel<T> model) {
        if (this.model == model) {
            return;
        }
        this.model = model;
        this.modelRef = null;
    }

    public SequenceModel<T> getModel() {
        return model;
    }

    public void setValueEditor(ValueEditor<T> valueEditor) {
        if (Objects.equals(this.valueEditor, valueEditor)) {
            return;
        }
        stopEditing();
        this.valueEditor = valueEditor;
    }

    public ValueEditor<T> getValueEditor() {
        return valueEditor;
    }

    public void setValue(T value) {
        getModel().setObject(value);
    }

    public T getValue() {
        if (model == null) {
            return null;
        }
        return getModel().getObject();
    }

    public void nextValue() {
        if (model == null) {
            return;
        }
        model.setObject(model.getNextObject());
        commandMap.runCommands(SpinnerAction.NextValue);
        runEffect(EFFECT_NEXT);
    }

    public void previousValue() {
        if (model == null) {
            return;
        }
        model.setObject(model.getPreviousObject());
        commandMap.runCommands(SpinnerAction.PreviousValue);
        runEffect(EFFECT_PREVIOUS);
    }

    public void setEnabled(boolean b) {
        if (this.enabled == b) {
            return;
        }
        this.enabled = b;

        previous.setEnabled(b);
        next.setEnabled(b);

        // If disabled, also make our view not focusable
        if (view != null) {
            view.getControl(GuiControl.class).setFocusable(b);
        }

        if (isEnabled()) {
            commandMap.runCommands(SpinnerAction.Enabled);
            runEffect(EFFECT_ENABLE);
        } else {
            commandMap.runCommands(SpinnerAction.Disabled);
            runEffect(EFFECT_DISABLE);
        }
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void startEditing() {
        if (valueEditor == null) {
            return;
        }

        commandMap.runCommands(SpinnerAction.StartEdit);
        runEffect(EFFECT_START_EDIT);

        if (this.view != null) {
            layout.removeChild(this.view);
        }

        Panel newEdit = valueEditor.startEditing(model.getObject());
        if (newEdit != edit) {
            layout.removeChild(edit);
        }

        this.edit = newEdit;
        layout.addChild(edit, 0, 0);

        requestFocus(edit);
    }

    public void stopEditing() {
        if (valueEditor == null) {
            return;
        }

        // Take what we got
        model.setObject(valueEditor.getObject());

        commandMap.runCommands(SpinnerAction.StopEdit);
        runEffect(EFFECT_STOP_EDIT);

        boolean navigateNext = edit != null && edit.getControl(GuiControl.class).isFocused();

        layout.removeChild(edit);
        edit = null;

        if (this.view != null) {
            layout.addChild(view, 0, 0);
        }

        if (navigateNext) {
            ViewState vs = GuiGlobals.getInstance().getInputMapper().get(this);
            if (vs != null) {
                vs.getNavigator().navigate(TraversalDirection.Next);
            }
        }
    }

    public boolean isEditing() {
        return edit != null;
    }

    protected void setView(Panel view) {
        if (this.view == view) {
            return;
        }

        if (this.view != null) {
            // Detach listeners from old view
            this.view.getControl(GuiControl.class).removeNavigatorListener(focusHandler);
            this.view.removeFocusListener(focusHandler);
 
            layout.removeChild(this.view);
        }

        this.view = view;

        if (this.view != null) {
            this.view.getControl(GuiControl.class).setFocusable(isEnabled());

            // New focus + nav hooks live on the focusable view
            this.view.addFocusListener(focusHandler);
            this.view.getControl(GuiControl.class).addNavigatorListener(focusHandler);

 
            if (!isEditing()) {
                layout.addChild(this.view, 0, 0);
            }
        }
    }

    protected void resetValue() {
        if (modelRef == null) {
            modelRef = model.createReference();
        }
        setView(valueRenderer.getView(modelRef.get(), false, this.view));
    }

    private void requestFocus(Spatial s) {
        ViewState vs = GuiGlobals.getInstance().getInputMapper().get(this);
        if (vs != null) {
            vs.getNavigator().focus(s);
        }
    }

 
    protected class SpinnerFocusHandler implements FocusListener, NavigatorListener {

        @Override

        public void focusGained(Spatial target) {
            if (!isEnabled()) {
                return;
            }
            commandMap.runCommands(SpinnerAction.FocusGained);
            runEffect(EFFECT_FOCUS);
        }

        @Override

        public void focusLost(Spatial target) {
            commandMap.runCommands(SpinnerAction.FocusLost);
            runEffect(EFFECT_UNFOCUS);
        }

        @Override

        public void focusAction(Spatial target, boolean pressed) {
            if (!isEnabled()) {
                return;
            }
            // Match your Button semantics: act on release.
            if (pressed) {
                return;
            }
            // Start editing (if supported). If no editor, "activate" = nextValue().
            if (valueEditor != null && !isEditing()) {
                startEditing();
            } else if (!isEditing()) {
                nextValue();
            }
        }

        @Override
        public boolean beforeNavigatorNavigate(TraversalDirection dir) {
            // Only intercept when our focusable view is actually focused.
            if (!isEnabled() || isEditing() || view == null) {
                return true;
            }
            GuiControl gc = view.getControl(GuiControl.class);
            if (gc == null || !gc.isFocused()) {
                return true;
            }

            // Consume Up/Down to spin instead of moving focus away.
            if (dir == TraversalDirection.Up) {
                nextValue();
                return false;
            }
            if (dir == TraversalDirection.Down) {
                previousValue();
                return false;
            }

            return true;
        }

        @Override
        public void focusScrollUpdate(Spatial target, ScrollDirection dir, double value) {
            if (!isEnabled() || isEditing()) {
                return;
            }
            // Interpret scroll as steps. Positive -> next, negative -> previous.
            int steps = (int) Math.round(value);
            if (steps == 0) {
                steps = value > 0 ? 1 : (value < 0 ? -1 : 0);
            }
            if (steps > 0) {
                for (int i = 0; i < steps; i++) nextValue();
            } else if (steps < 0) {
                for (int i = 0; i < -steps; i++) previousValue();
            }
        }
    }

}
