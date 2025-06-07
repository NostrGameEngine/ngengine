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
 * the BSD 3-Clause License. The original jMonkeyEngine license is as follows:
 */
package org.ngengine.gui.components.containers;

import com.jme3.math.Vector3f;
import com.simsilica.lemur.Axis;
import com.simsilica.lemur.Container;
import com.simsilica.lemur.FillMode;
import com.simsilica.lemur.Label;
import com.simsilica.lemur.Panel;
import com.simsilica.lemur.component.SpringGridLayout;
import com.simsilica.lemur.core.GuiControl;
import com.simsilica.lemur.core.GuiControlListener;
import com.simsilica.lemur.core.GuiUpdateListener;
import com.simsilica.lemur.style.ElementId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Function;

public class NMultiPageList<T> extends Container implements GuiUpdateListener, GuiControlListener {

    // public static final ElementId ELEMENT_ID = new ElementId(Container.ELEMENT_ID).child("multiPageList");
    public static final String ELEMENT_ID = "multiPageList";

    protected Function<T, Panel> renderer = item -> {
        return new Label(item.toString());
    };

    private List<T> items = new ArrayList<>();

    private int page = 0;
    private float itemHeight = 12;
    private boolean invalidated = true;
    private float height = 0;
    private BiConsumer<Integer, Integer> onPageChange;

    public NMultiPageList() {
        super(new SpringGridLayout(Axis.Y, Axis.X, FillMode.None, FillMode.Even), new ElementId(ELEMENT_ID));
        GuiControl c = getControl(GuiControl.class);
        c.addUpdateListener(this);
        c.addListener(this);
    }

    public void setRenderer(Function<T, Panel> renderer) {
        this.renderer = renderer;
    }

    public void setPageChangeListener(BiConsumer<Integer, Integer> onPageChange) {
        this.onPageChange = onPageChange;
    }

    protected void callPageChangeListener() {
        if (onPageChange != null) {
            int pageCount = (int) Math.ceil(items.size() / (height / itemHeight));
            onPageChange.accept(page, pageCount);
        }
    }

    // public void nextPage(){
    //     currentPage=targetPage;
    //     targetPage++;
    // }
    // public void previousPage(){
    //     currentPage=targetPage;
    //     targetPage--;
    // }

    public void nextPage() {
        if (!hasNextPage()) {
            return;
        }
        page++;
        callPageChangeListener();
        invalidated = true;
    }

    public void previousPage() {
        if (!hasPreviousPage()) {
            return;
        }
        page--;
        callPageChangeListener();
        invalidated = true;
    }

    public boolean hasPreviousPage() {
        return page > 0;
    }

    public boolean hasNextPage() {
        return page < (items.size() / (height / itemHeight));
    }

    public void addItem(T item) {
        items.add(item);
        invalidated = true;
    }

    public void removeItem(T item) {
        items.remove(item);
        invalidated = true;
    }

    public void clear() {
        items.clear();
        invalidated = true;
    }

    public T getItem(int index) {
        return items.get(index);
    }

    public int getItemCount() {
        return items.size();
    }

    public void removeItem(int index) {
        items.remove(index);
        invalidated = true;
    }

    public void sort(Comparator<T> comparator) {
        items.sort(comparator);
        invalidated = true;
    }

    ArrayList<Panel> renderedItems = new ArrayList<>();
    boolean renderingPage = false;

    @Override
    public void guiUpdate(GuiControl source, float tpf) {
        if (invalidated) {
            invalidated = false;
            clearChildren();
            renderedItems.clear();

            int elementsPerPage = (int) (height / itemHeight);
            int start = page * elementsPerPage;
            int end = Math.min(start + elementsPerPage, items.size());
            for (int i = start; i < end; i++) {
                T item = items.get(i);
                Panel panel = renderer.apply(item);
                panel.setCullHint(CullHint.Always);
                addChild(panel);
                renderedItems.add(panel);
            }

            callPageChangeListener();

            return;
        }

        boolean needResize = false;
        for (Panel i : renderedItems) {
            Vector3f size = i.getSize();
            if (size.y > itemHeight) {
                itemHeight = size.y;
                needResize = true;
            }
        }

        if (!needResize) {
            for (Panel i : renderedItems) {
                i.setCullHint(CullHint.Never);
            }
        } else {
            invalidated = true;
        }
        // if(renderingPage){
        //     int from = lastItemI;
        //     int direction = FastMath.sign(targetPage - currentPage);
        //     int usedHeight = 0;
        //     while (usedHeight < height) {
        //         lastItemI += direction;
        //         Panel render = renderer.apply(items.get(lastItemI));
        //         if (direction < 0) {
        //             addChild(render, 0);
        //         } else {
        //             addChild(render, 0);
        //         }

        //         float h = render.getSize().y;
        //         h=10;
        //         usedHeight += h;
        //         System.out.println("lastItemI = " + lastItemI + " usedHeight = " + usedHeight + " continue");
        //     }
        //         renderingPage = false;
        //         System.out.println("STOP usedHeight = " + usedHeight);

        // }

        // if(renderingPage){
        //     int itemI = lastItemI+1;
        //     if(itemI<items.size()){
        //         T item = items.get(itemI);
        //         int usedHeight = 0;
        //         for(int i=0;i<itemI;i++){
        //             usedHeight += renderedItems.get(i).getSize().y;
        //         }
        //         int itemHeight = itemI > 0 ? usedHeight / itemI : 0;
        //         if(usedHeight+itemHeight<=height){
        //             // there is still some space left for the item
        //             Panel panel = renderer.apply(item);
        //             renderedItems.add(panel);
        //             addChild(panel);
        //             lastItemI = itemI;

        //         } else{
        //             // no more space, stop rendering
        //             System.out.println("No more space for item "+itemI);
        //             renderingPage = false;
        //             pageRanges.add(new PageRange(lastItemI-renderedItems.size(), lastItemI));

        //         }
        //     }

        // }

    }

    @Override
    public void reshape(GuiControl source, Vector3f pos, Vector3f size) {
        if (size.y > height) {
            height = size.y;
            invalidated = true;
        }
    }

    @Override
    public void focusGained(GuiControl source) {}

    @Override
    public void focusLost(GuiControl source) {}
}
