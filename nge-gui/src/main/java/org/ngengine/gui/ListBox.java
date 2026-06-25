/*
 * $Id$
 * 
 * Copyright (c) 2014, Simsilica, LLC
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

import java.util.*;
import java.util.logging.Logger;

import org.ngengine.gui.ListBox.GridModelDelegate;

import com.jme3.math.*;
import com.jme3.scene.*;
import org.ngengine.gui.component.*;
import org.ngengine.gui.core.*;
import org.ngengine.gui.grid.GridModel;
import org.ngengine.gui.list.*;
import org.ngengine.gui.nav.FocusListener;
import org.ngengine.gui.nav.FocusTarget;
import org.ngengine.gui.nav.NavigatorListener;
import org.ngengine.gui.nav.ScrollDirection;
import org.ngengine.gui.nav.TraversalDirection;
import org.ngengine.gui.style.*;


/**
 *
 *  @author    Paul Speed
 */
public class ListBox<T> extends Panel  {
 
    static Logger log = Logger.getLogger(ListBox.class.getName());
    
    public static final String ELEMENT_ID = "list";
    public static final String CONTAINER_ID = "container";
    public static final String ITEMS_ID = "items";
    public static final String SLIDER_ID = "slider";
    public static final String SELECTOR_ID = "selector";

    public static final String EFFECT_PRESS = "press";
    public static final String EFFECT_RELEASE = "release";
    public static final String EFFECT_CLICK = "click";
    public static final String EFFECT_ACTIVATE = "activate";
    public static final String EFFECT_DEACTIVATE = "deactivate";
    public enum ListAction { Down, Up, Click, Entered, Exited };
    
    private ElementId baseElementId;
    private BorderLayout layout;
    private VersionedList<T> model;
    private VersionedReference<List<T>> modelRef;
    private ValueRenderer<T> cellRenderer;
    
    private SelectionModel selection;
    private VersionedReference<Set<Integer>> selectionRef;
    
    private Listener listener = new Listener();
    private CommandMap<ListBox, ListAction> commandMap
                                    = new CommandMap<ListBox, ListAction>(this);

    private GridPanel grid;
    private Slider slider;
    private Node selectorArea;
    private Panel selector;
    private Vector3f selectorAreaOrigin = new Vector3f();
    private Vector3f selectorAreaSize = new Vector3f();  
    private RangedValueModel baseIndex;  // upside down actually
    private VersionedReference<Double> indexRef;
    private int maxIndex;
    private float touchScrollSlop = 8f;
    
 
    /**
     *  Keeps track of if we've triggered 'activated' effects (and send entered events)
     */
    private boolean activated = false;
    
    /**
     *  Keeps track of whether some listener has detected enter/exit.  When this
     *  is different than activated then we need to trigger effects and fire events.
     */
    private boolean entered = false;
       
    public ListBox() {
        this((VersionedList<T>)null);
    }

    public ListBox( VersionedList<T> model ) {
        this(model, new ElementId(ELEMENT_ID));             
    }
 
    public ListBox(VersionedList<T> model, ElementId elementId ) {
        this(model, null, new SelectionModel(), elementId);             
    }

    public ListBox(VersionedList<T> model, ValueRenderer<T> renderer, ElementId elementId ) {
        this(model, renderer, new SelectionModel(), elementId);             
    }
    
    public ListBox( VersionedList<T> model, ValueRenderer<T> cellRenderer, SelectionModel selection, ElementId elementId ) {
        super(elementId.child(CONTAINER_ID));
        this.baseElementId = elementId;
         
        if( cellRenderer == null ) {
            // Create a default one
            cellRenderer = new DefaultCellRenderer<>(baseElementId.child("item"));
        } else {
            cellRenderer.configureStyle(baseElementId.child("item"));
        }
        this.cellRenderer = cellRenderer;
 
        this.layout = new BorderLayout();
        getControl(GuiControl.class).setLayout(layout);
 
        grid = new GridPanel(new GridModelDelegate(), elementId.child(ITEMS_ID));
        grid.setVisibleColumns(1);
        grid.getControl(GuiControl.class).addListener(new GridListener());
        layout.addChild(grid, BorderLayout.Position.Center);
 
        baseIndex = new DefaultRangedValueModel();
        indexRef = baseIndex.createReference();
        slider = new Slider(baseIndex, Axis.Y, elementId.child(SLIDER_ID));
        slider.getControl(GuiControl.class).setFocusable(FocusTarget.FOCUS_POINTER);
        layout.addChild(slider, BorderLayout.Position.East);
 
        // Listen to our own mouse events that don't hit something else
        addFocusListener(listener);

        // Need a spacer so that the 'selector' panel doesn't think
        // it's being managed by this panel.
        // Have to set this up after applying styles so that the default
        // styles are properly initialized the first time.
        selectorArea = new Node("selectorArea");
        attachChild(selectorArea);
        selector = new Panel(elementId.child(SELECTOR_ID));

        // Make sensible layering at the base level
        LayerComparator.setLayer(grid, 1);
        LayerComparator.setLayer(selectorArea, 2);
        
        setModel(model);                
        resetModelRange();
        setSelectionModel(selection);        

        getControl(GuiControl.class).addNavigatorListener(listener);

        // never navigate to slider (they are only for direct cursor selection (ie. mouse))
        slider.getControl(GuiControl.class).addNavigatorListener(new NavigatorListener() {
            @Override
            public boolean beforeNavigatorNavigateTo(TraversalDirection dir, Spatial from, Spatial sp) {
                if(slider.getThumbButton() == sp || slider.getDecrementButton()  == sp || slider.getIncrementButton() == sp){
                    return false;
                } else{
                    return true;
                }
            }            
        });
        applyStyles(ListBox.class);
    }
    
    
    @Override
    public void updateLogicalState( float tpf ) {
        super.updateLogicalState(tpf);
       
        if( modelRef.update() ) {
            resetModelRange();
        }
 
        boolean indexUpdate = indexRef.update();
        boolean selectionUpdate = selectionRef.update();         
        if( indexUpdate ) {
            int index = (int)(maxIndex - baseIndex.getValue());
            grid.setRow(index);
        }         

        if( selectionUpdate || indexUpdate ) {
            refreshSelector();
        }
        
        if( activated != entered ) {
            refreshActivation();
        }
    }


    protected void gridResized( Vector3f pos, Vector3f size ) {
        if( pos.equals(selectorAreaOrigin) && size.equals(selectorAreaSize) ) {
            return;
        }
        
        selectorAreaOrigin.set(pos);
        selectorAreaSize.set(size);
        
        refreshSelector();        
    }
    
    public void setModel( VersionedList<T> model ) {
        if( this.model == model && model != null ) {
            return;
        }
        
        if( this.model != null ) {
            // Clean up the old one
            detachItemListeners();
        }

        if( model == null ) {
            // Easier to create a default one than to handle a null model
            // everywhere
            model = new VersionedList<T>();
        }  
        
        this.model = model;
        this.modelRef = model.createReference();
        
        grid.setLocation(0,0);
        grid.setModel(new GridModelDelegate());  // need a new one for a new version
        resetModelRange();
        baseIndex.setValue(maxIndex);
        refreshSelector();    
    }        

    public VersionedList<T> getModel() {
        return model;
    }

    public Slider getSlider() {
        return slider;
    }
    
    public GridPanel getGridPanel() {
        return grid;
    }

    public void setTouchScrollSlop(float pixels) {
        touchScrollSlop = Math.max(0f, pixels);
    }

    public float getTouchScrollSlop() {
        return touchScrollSlop;
    }

    public Panel getSelector() {
        return selector;
    }
 
    public void setSelectionModel( SelectionModel selection ) {
        if( this.selection == selection ) {
            return;
        }
        this.selection = selection;
        this.selectionRef = selection.createReference();
        refreshSelector();
    }
    
    public SelectionModel getSelectionModel() {
        return selection;
    }

    /**
     *  Returns the currently selected list item if there is one and only
     *  one item selected.  This is a convenience method that interrogates
     *  the selection model and looks up the current value in the list model.
     */
    public T getSelectedItem() {
        Integer i = selection.getSelection();
        if( i == null ) {
            return null;
        }
        if( i < 0 || i >= getModel().size() ) {
            return null;
        }
        return getModel().get(i);     
    }
 
    /**
     *  Convenience method for setting the currently selected item.
     */   
    public void setSelectedItem( T item ) {
        int index = getModel().indexOf(item);
        if( index < 0 ) {
            return;
        }
        selection.setSelection(index);
    }    
 
    @SuppressWarnings("unchecked") // because Java doesn't like var-arg generics 
    public void addCommands( ListAction a, Command<? super ListBox>... commands ) {
        commandMap.addCommands(a, commands);
    }

    public List<Command<? super ListBox>> getCommands( ListAction a ) {
        return commandMap.get(a, false);
    }

    @StyleAttribute("listCommands")
    public void setListCommands( Map<ListAction, List<Command<? super ListBox>>> map ) {
        commandMap.clear();
        // We don't use putAll() because (right now) it would potentially
        // put the wrong list implementations into the command map.
        for( Map.Entry<ListAction, List<Command<? super ListBox>>> e : map.entrySet() ) {
            commandMap.addCommands(e.getKey(), e.getValue());
        }
    }  
        
    @StyleAttribute(value="visibleItems", lookupDefault=false)
    public void setVisibleItems( int count ) {
        grid.setVisibleRows(count);
        resetModelRange();
        refreshSelector();
    }
    
    public int getVisibleItems() {
        return grid.getVisibleRows();
    }

    @StyleAttribute(value = "rowFillMode", lookupDefault = false)
    public void setRowFillMode(FillMode fillMode) {
        grid.setRowFillMode(fillMode);
        refreshSelector();
    }

    public FillMode getRowFillMode() {
        return grid.getRowFillMode();
    }

    @StyleAttribute(value = "variableCellHeights", lookupDefault = false)
    public void setVariableCellHeights(boolean enabled) {
        setRowFillMode(enabled ? FillMode.None : FillMode.ForcedEven);
    }

    public boolean isVariableCellHeights() {
        return getRowFillMode() == FillMode.None;
    }

    @StyleAttribute(value = "cellRenderer", lookupDefault = false)
    public void setCellRenderer(ValueRenderer<T> renderer) {
        if (Objects.equals(this.cellRenderer, renderer)) {
            return;
        }
        this.cellRenderer = renderer;
        // We send through the same element ID that was provided to our constructor
        // because that's what the default cell renderer would have used.
        cellRenderer.configureStyle(baseElementId.child("item"));
        grid.refreshGrid(); // cheating
    }
    
    public ValueRenderer<T> getCellRenderer() {
        return cellRenderer;
    }    

    public void setAlpha( float alpha, boolean recursive ) {
        super.setAlpha(alpha, recursive);
        
        // Catch some of our intermediaries
        setChildAlpha(selector, alpha);
    }


    protected void refreshSelector() {    
        if( selectorArea == null ) {
            return;
        }
        Panel selectedCell = null;
        if( selection != null && !selection.isEmpty() ) {
            // For now just one item... otherwise we have to loop
            // over visible items
            int selected = selection.iterator().next();
            if( selected >= model.size() ) {
                selected = model.size() - 1;
                selection.setSelection(selected);      
            }
            selectedCell = grid.getCell(selected, 0); 
        }
                
        if( selectedCell == null ) {
            selectorArea.detachChild(selector);            
        } else {
            Vector3f size = selectedCell.getSize().clone();
            Vector3f loc = selectedCell.getLocalTranslation();
            Vector3f pos = selectorAreaOrigin.add(loc.x, loc.y, loc.z + size.z);

            selector.setLocalTranslation(pos);
            selector.setSize(size);
            selector.setPreferredSize(size);
            
            selectorArea.attachChild(selector);
            selectorArea.setLocalTranslation(grid.getLocalTranslation());            
        }
    }

    protected void resetModelRange() {    
        int count = model == null ? 0 : model.size();
        int visible = grid.getVisibleRows();
        maxIndex = Math.max(0, count - visible);
        
        // Because the slider is upside down, we have to
        // do some math if we want our base not to move as
        // items are added to the list after us
        double val = baseIndex.getMaximum() - baseIndex.getValue();
        
        baseIndex.setMinimum(0);
        baseIndex.setMaximum(maxIndex);
        baseIndex.setValue(maxIndex - val);        
    }

    protected void refreshActivation() {
        if( entered ) {
            activate();
        } else {
            deactivate();
        }
    }

    protected Panel getListCell( int row, int col, Panel existing ) {
        T value = model.get(row);
        Panel cell = cellRenderer.getView(value, false, existing);
        if( cell != existing ) {

        //     // Transfer the click listener                  
        //     CursorEventControl.addListenersToSpatial(cell, clickListener);
        //     CursorEventControl.removeListenersFromSpatial(existing, clickListener);
            if (cell!=null) cell.addFocusListener(listener);
            if(existing!=null) existing.removeFocusListener(listener);    

        }         
        return cell;
    }

    /**
     *  Used when the list model is swapped out.
     */
    protected void detachItemListeners() {
        int base = grid.getRow();
        for( int i = 0; i < grid.getVisibleRows(); i++ ) {
            Panel cell = grid.getCell(base + i, 0);
            if( cell != null ) {
                cell.removeFocusListener(listener);
            //     CursorEventControl.removeListenersFromSpatial(cell, clickListener);
            }
        }
    }

    protected void scroll( int amount ) {
        double delta = getSlider().getDelta();
        double value = getSlider().getModel().getValue();
        getSlider().getModel().setValue(value + delta * amount);   
    }

    protected void activate() {
        if( activated ) {
            return;
        }
        activated = true;
        commandMap.runCommands(ListAction.Entered);
        runEffect(EFFECT_ACTIVATE);
    }
    
    protected void deactivate() {
        if( !activated ) {
            return;
        }
        activated = false;
        commandMap.runCommands(ListAction.Exited);
        runEffect(EFFECT_DEACTIVATE);
    }
    
    @Override
    public String toString() {
        return getClass().getName() + "[elementId=" + getElementId() + "]";
    }
    private class Listener implements FocusListener, NavigatorListener {
        private Spatial lastTarget = null;
        private Spatial focusOverride = null;
        private boolean dragActive;
        private boolean dragMoved;
        private float dragStartY;
        private float dragLastY;
        private float dragRemainderY;
        @Override
        public void focusGained(Spatial target) {
            entered = true;
            for(Spatial c : grid.getChildren()){
                GuiControl gc = c.getControl(GuiControl.class);
                if (gc != null) {
                    gc.setFocusable(true);
                }
            }
            lastTarget = target;
            int cell = findCell(target);
            if (cell == -1 && target == ListBox.this && grid.getVisibleRows() > 0 && getModel().size() > 0) {
                cell = grid.getRow();
            }
            if (cell != -1) {
                selection.setSelection(cell);
                refreshSelector();
            }

        }

        private int findCell(Spatial target){
            // Find the element we clicked on
            int base = grid.getRow();
            for( int i = 0; i < grid.getVisibleRows(); i++ ) {
                Panel cell = grid.getCell( base + i, 0 );
                if( cell == target || isDescendantOf(target, cell) ) {
                    return base + i;                
                }
            }
            return -1;            
        }

        private boolean isDescendantOf(Spatial target, Spatial ancestor) {
            if (target == null || ancestor == null) {
                return false;
            }
            for (Spatial current = target; current != null; current = current.getParent()) {
                if (current == ancestor) {
                    return true;
                }
            }
            return false;
        }

        @Override
        public void focusAction(Spatial target, boolean pressed) {
            if (pressed) {
                return;
            }
            int cell = findCell(target);
            if (cell == -1) {
                Integer selected = selection.getSelection();
                cell = selected != null ? selected : -1;
            }
            if (cell == -1) {
                return;
            }
            selection.add(cell);
            commandMap.runCommands(ListAction.Click);
            runEffect(EFFECT_CLICK);
        }

        @Override
        public void focusAction(Spatial target, boolean pressed, float x, float y) {
            if (pressed) {
                dragActive = true;
                dragMoved = false;
                dragStartY = y;
                dragLastY = y;
                dragRemainderY = 0f;
                return;
            }
            boolean wasDragging = dragMoved;
            dragActive = false;
            dragMoved = false;
            dragRemainderY = 0f;
            if (!wasDragging) {
                focusAction(target, false);
            }
        }

        @Override
        public void focusDrag(Spatial target, float x, float y) {
            if (!dragActive || maxIndex <= 0) {
                return;
            }
            float dy = y - dragLastY;
            dragLastY = y;
            if (!dragMoved && Math.abs(y - dragStartY) < touchScrollSlop) {
                return;
            }
            dragMoved = true;
            dragRemainderY += dy;

            float rowHeight = getTouchScrollRowHeight();
            if (rowHeight <= 0f) {
                return;
            }
            int rows = (int) (dragRemainderY / rowHeight);
            if (rows == 0) {
                return;
            }
            dragRemainderY -= rows * rowHeight;
            scroll(-rows);
            refreshSelector();
        }

        @Override
        public boolean beforeNavigatorNavigate(TraversalDirection dir) {
            if(lastTarget==null) return true;

            int cell = findCell(lastTarget);
            if (cell < 0) return true;
            int visibleCells = Math.min(model.size() - 1, grid.getRow() + grid.getVisibleRows() -1);
            
            boolean isLastVisible = visibleCells == cell;
            boolean isFirstVisible = grid.getRow() == cell;
            
            
            if(isLastVisible&&dir==TraversalDirection.Down && cell < model.size() - 1){
                selectVisibleCell(cell + 1);
                return false;
            }

            if(isFirstVisible&&dir==TraversalDirection.Up && cell > 0){
                selectVisibleCell(cell - 1);
                return false;
            }

            return true;
        }

        private void selectVisibleCell(int cell) {
            if (model == null || model.isEmpty()) {
                return;
            }
            int selected = Math.max(0, Math.min(model.size() - 1, cell));
            int firstVisible = grid.getRow();
            int visibleRows = Math.max(1, grid.getVisibleRows());
            if (selected < firstVisible) {
                firstVisible = selected;
            } else if (selected >= firstVisible + visibleRows) {
                firstVisible = selected - visibleRows + 1;
            }
            firstVisible = Math.max(0, Math.min(maxIndex, firstVisible));
            baseIndex.setValue(maxIndex - firstVisible);
            grid.setRow(firstVisible);
            selection.setSelection(selected);
            Panel selectedCell = grid.getCell(selected, 0);
            if (selectedCell != null) {
                lastTarget = selectedCell;
                focusOverride = selectedCell;
            }
            refreshSelector();
        }

        @Override
        public Spatial getNavigatorFocusOverride() {
            Spatial result = focusOverride;
            focusOverride = null;
            return result;
        }

        @Override
        public void focusLost(Spatial target) {
            entered = false;
            if(target==lastTarget)lastTarget = null;
            dragActive = false;
            dragMoved = false;
            dragRemainderY = 0f;
        }

       
        @Override
        public void focusScrollUpdate(Spatial target, ScrollDirection dir,  double value) {
            scroll((int)(dir==ScrollDirection.Up||dir==ScrollDirection.Right?value:-value));
        }

        private float getTouchScrollRowHeight() {
            int rows = Math.max(1, grid.getVisibleRows());
            Vector3f size = grid.getSize();
            if (size != null && size.y > 0f) {
                return Math.max(1f, size.y / rows);
            }
            Vector3f preferred = grid.getPreferredSize();
            if (preferred != null && preferred.y > 0f) {
                return Math.max(1f, preferred.y / rows);
            }
            return 1f;
        }

    }   

    private class GridListener extends AbstractGuiControlListener {
        public void reshape( GuiControl source, Vector3f pos, Vector3f size ) {
            gridResized(pos, size);
            
            // If the grid was re-laid out then we probably need
            // to refresh our selector
            refreshSelector();
        }
    }
    
    protected class GridModelDelegate implements GridModel<Panel> {
        
        @Override
        public int getRowCount() {
            if( model == null ) {
                return 0;
            }
            return model.size();        
        }

        @Override
        public int getColumnCount() {
            return 1;
        }

        @Override
        public Panel getCell( int row, int col, Panel existing ) {
            return getListCell(row, col, existing);
        }
                
        @Override
        public void setCell( int row, int col, Panel value ) {
            throw new UnsupportedOperationException("ListModel is read only.");
        }

        @Override
        public long getVersion() {
            return model == null ? 0 : model.getVersion();
        }

        @Override
        public GridModel<Panel> getObject() { 
            return this;
        }

        @Override
        public VersionedReference<GridModel<Panel>> createReference() { 
            return new VersionedReference<GridModel<Panel>>(this);
        }
    }
}
