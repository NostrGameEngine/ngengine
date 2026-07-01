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
import org.ngengine.gui.style.Styles;
import org.ngengine.gui.core.VersionedReference;
import org.ngengine.gui.nav.FocusListener;
import org.ngengine.gui.nav.FocusTarget;
import org.ngengine.gui.nav.ScrollDirection;
import org.ngengine.gui.core.GuiControl;
import com.jme3.math.Vector3f;
import com.jme3.scene.Spatial;

import org.ngengine.gui.component.BorderLayout;
import org.ngengine.gui.core.AbstractGuiControlListener;



/**
 *  A composite GUI element consisting of a draggable slider
 *  with increment and decrement buttons at each end.  The slider
 *  value is managed by a RangedValueModel.
 *
 *  @author    Paul Speed
 */
public class Slider extends Panel implements FocusListener {

    public static final String ELEMENT_ID = "slider";
    /*public static final String UP_ID = "slider.up.button";
    public static final String DOWN_ID = "slider.down.button";
    public static final String LEFT_ID = "slider.left.button";
    public static final String RIGHT_ID = "slider.right.button";
    public static final String THUMB_ID = "slider.thumb.button";
    public static final String RANGE_ID = "slider.range";*/
    public static final String UP_ID = "up.button";
    public static final String DOWN_ID = "down.button";
    public static final String LEFT_ID = "left.button";
    public static final String RIGHT_ID = "right.button";
    public static final String THUMB_ID = "thumb.button";
    public static final String RANGE_ID = "range";

    private BorderLayout layout;
    private Axis axis;
    private Button increment;
    private Button decrement;
    private Panel  range;
    private Button thumb;

    private RangedValueModel model;
    private double delta = 1.0f;
    private VersionedReference<Double> state;
    private final ScrollListener scrollListener = new ScrollListener();
    private final DragListener dragListener = new DragListener();

 

    public Slider() {
        this(new DefaultRangedValueModel(), Axis.X,  new ElementId(ELEMENT_ID));
    }

    public Slider(ElementId elementId, String style) {
        this(new DefaultRangedValueModel(), Axis.X,  elementId);
    }

    public Slider(Axis axis, ElementId elementId) {
        this(new DefaultRangedValueModel(), axis,  elementId);
    }

    public Slider(Axis axis) {
        this(new DefaultRangedValueModel(), axis,  new ElementId(ELEMENT_ID));
    }

    public Slider( RangedValueModel model ) {
        this(model, Axis.X, new ElementId(ELEMENT_ID));
    }

    public Slider( RangedValueModel model, ElementId elementId ) {
        this(model, Axis.X);
    }

    public Slider( RangedValueModel model, Axis axis ) {
        this(model, axis,  new ElementId(ELEMENT_ID));
    }


    protected Slider( RangedValueModel model, Axis axis,  ElementId elementId ) {
        super( elementId);

        this.axis = axis;
        this.layout = new BorderLayout();
        getControl(GuiControl.class).setLayout(layout);
        getControl(GuiControl.class).addListener(new ReshapeListener());
        getControl(GuiControl.class).addFocusChangeListener(this);
        getControl(GuiControl.class).setFocusable(FocusTarget.FOCUS_NAVIGATION);

        this.model = model;

        switch( axis ) {
            case X:
                increment = layout.addChild(BorderLayout.Position.East,
                                            new Button( elementId.child(RIGHT_ID)));                
                decrement = layout.addChild(BorderLayout.Position.West,
                                            new Button( elementId.child(LEFT_ID)));                
                range = layout.addChild(createRangePanel(NGEStyle.px(50), NGEStyle.px(2), elementId.child(RANGE_ID)));
                break;
            case Y:
                increment = layout.addChild(BorderLayout.Position.North,
                                            new Button( elementId.child(UP_ID)));
                decrement = layout.addChild(BorderLayout.Position.South,
                                            new Button( elementId.child(DOWN_ID)));
                range = layout.addChild(createRangePanel(NGEStyle.px(2), NGEStyle.px(50), elementId.child(RANGE_ID)));
                break;
            case Z:
                throw new IllegalArgumentException("Z axis not yet supported.");
        }
        increment.getControl(GuiControl.class).setFocusable(FocusTarget.FOCUS_POINTER);
        decrement.getControl(GuiControl.class).setFocusable(FocusTarget.FOCUS_POINTER);
        setupCommands();

        thumb = new Button(elementId.child(THUMB_ID));
        thumb.getControl(GuiControl.class).setFocusable(FocusTarget.FOCUS_POINTER);
        // ButtonDragger dragger = new ButtonDragger();
        // CursorEventControl.addListenersToSpatial(thumb, dragger);
        attachChild(thumb);

        // A child that is not managed by the layout will not otherwise lay itself
        // out... so we will force it to be its own preferred size.
        thumb.getControl(GuiControl.class).setSize(thumb.getControl(GuiControl.class).getPreferredSize());

        applyStyles(Slider.class);

        increment.addFocusListener(scrollListener);
        decrement.addFocusListener(scrollListener);
        range.addFocusListener(scrollListener);
        range.addFocusListener(dragListener);
        thumb.addFocusListener(scrollListener);
        thumb.addFocusListener(dragListener);
    }

    private Panel createRangePanel(float width, float height, ElementId elementId) {
        Panel panel = new Panel(elementId);
        panel.setPreferredSize(width, height);
        return panel;
    }
    @SuppressWarnings("unchecked") // because Java doesn't like var-arg generics
    protected final void setupCommands() {
        increment.addClickCommands(new ChangeValueCommand(1));
        decrement.addClickCommands(new ChangeValueCommand(-1));
    }

    public void setModel( RangedValueModel model ) {
        if( this.model == model )
            return;
        this.model = model;
        this.state = null;
    }

    public RangedValueModel getModel() {
        return model;
    }

    public void setDelta( double delta ) {
        this.delta = delta;
    }

    public double getDelta() {
        return delta;
    }

    public Axis getAxis() {
        return axis;
    }

    public Button getIncrementButton() {
        return increment;
    }

    public Button getDecrementButton() {
        return decrement;
    }

    public Panel getRangePanel() {
        return range;
    }

    public Button getThumbButton() {
        return thumb;
    }
    
    /**
     *  Returns the slider range value for the specified location
     *  in the slider's local coordinate system.  (For example,
     *  for world space location use slider.worldToLocal() first.)
     */
    public double getValueForLocation( Vector3f loc ) {

        Vector3f relative = loc.subtract(range.getLocalTranslation());

        // Components always grow down from their location
        // so we'll invert y
        relative.y *= -1;
                
        Vector3f axisDir = axis.getDirection();
        double projection = relative.dot(axisDir);
        if( projection < 0 ) {
            if( axis == Axis.Y ) {
                return model.getMaximum();
            } else {
                return model.getMinimum();
            }
        }
        
        Vector3f rangeSize = range.getSize().clone();
         
        double rangeLength = rangeSize.dot(axisDir);
        projection = Math.min(projection, rangeLength);
        double part = projection / rangeLength;       
        double rangeDelta = model.getMaximum() - model.getMinimum();
        
        // For the y-axis, the slider is inverted from the direction
        // that the component's grow... so our part is backwards
        if( axis == Axis.Y ) {
            part = 1 - part;
        }
 
        return model.getMinimum() + rangeDelta * part;        
    }

    protected void setValueForPointer(float x, float y) {
        Vector3f local = worldToLocal(new Vector3f(x, y, 0), null);
        model.setValue(getValueForLocation(local));
    }

    @Override
    public void updateLogicalState(float tpf) {
        super.updateLogicalState(tpf);

        if( state == null || state.update() ) {
            resetStateView();
        }
    }

    protected void resetStateView() {
        if( state == null ) {
            state = model.createReference();
        }

        Vector3f pos = range.getLocalTranslation();
        Vector3f rangeSize = range.getSize();
        Vector3f thumbSize = thumb.getSize();
        Vector3f size = getSize();

        double visibleRange;
        double x;
        double y;

        switch( axis ) {
            case X:
                visibleRange = rangeSize.x - thumbSize.x;

                // Calculate where the thumb center should be
                x = pos.x + visibleRange * model.getPercent();
                y = pos.y - rangeSize.y * 0.5;

                // We cheated and included the half-thumb spacing in x already which
                // is why this is axis-specific.
                thumb.setLocalTranslation((float)x,
                                          (float)(y + thumbSize.y * 0.5),
                                          pos.z + size.z);
                break;
            case Y:
                visibleRange = rangeSize.y - thumbSize.y;

                // Calculate where the thumb center should be
                x = pos.x + rangeSize.x * 0.5;
                y = pos.y - rangeSize.y + (visibleRange * model.getPercent());

                thumb.setLocalTranslation((float)(x - thumbSize.x * 0.5),
                                          (float)(y + thumbSize.y),
                                          pos.z + size.z );
                break;
        }

    }

    private class ChangeValueCommand implements Command<Button> {

        private double scale;

        public ChangeValueCommand( double scale ) {
            this.scale = scale;
        }

        public void execute( Button source ) {
            model.setValue(model.getValue() + delta * scale);
        }
    }

    @Override
    public void focusGained(Spatial target) {
        runEffect(Button.EFFECT_FOCUS);
    }

    @Override
    public void focusLost(Spatial target) {
        runEffect(Button.EFFECT_UNFOCUS);
    }

    @Override
    public void focusAction(Spatial target, boolean pressed) {
    }

    @Override
    public void focusAction(Spatial target, boolean pressed, float x, float y) {
        if (pressed) {
            setValueForPointer(x, y);
        }
    }

    @Override
    public void focusDrag(Spatial target, float x, float y) {
        setValueForPointer(x, y);
    }

    @Override
    public void focusScrollUpdate(Spatial target, ScrollDirection dir, double v) {
        int dvalue = (int)(dir==ScrollDirection.Up||dir==ScrollDirection.Right?v:-v);
        double delta = getDelta();
        double value = getModel().getValue();
        getModel().setValue(value + delta * dvalue);
    }

    private class ReshapeListener extends AbstractGuiControlListener {
        @Override
        public void reshape( GuiControl source, Vector3f pos, Vector3f size ) {
            // Make sure the thumb is positioned appropriately
            // for the new size
            resetStateView();   
        }
    }

    private class ScrollListener implements FocusListener{

        @Override
        public void focusGained(Spatial target) {
          
        }

        @Override
        public void focusLost(Spatial target) {
           
        }

        @Override
        public void focusAction(Spatial target, boolean pressed) {
  
        }

        @Override
        public void focusScrollUpdate(Spatial target, ScrollDirection dir, double v) {
            int dvalue = (int)(dir==ScrollDirection.Up||dir==ScrollDirection.Right?v:-v);
            double delta = getDelta();
            double value = getModel().getValue();
            getModel().setValue(value + delta * dvalue);   
        }

    }

    private class DragListener implements FocusListener {
        private boolean active;

        @Override
        public void focusGained(Spatial target) {
        }

        @Override
        public void focusLost(Spatial target) {
            active = false;
        }

        @Override
        public void focusAction(Spatial target, boolean pressed) {
            active = pressed;
        }

        @Override
        public void focusAction(Spatial target, boolean pressed, float x, float y) {
            active = pressed;
            if (pressed) {
                setValueForPointer(x, y);
            }
        }

        @Override
        public void focusDrag(Spatial target, float x, float y) {
            if (active) {
                setValueForPointer(x, y);
            }
        }

        @Override
        public void focusScrollUpdate(Spatial target, ScrollDirection dir, double value) {
        }
    }
}
