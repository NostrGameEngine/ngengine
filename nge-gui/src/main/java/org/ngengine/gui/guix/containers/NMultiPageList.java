/**
 * Copyright (c) 2026, Nostr Game Engine
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
 * 
 * #########################################
 * 
 * nge-gui is built and based on Lemur, which is licensed under the BSD 3-Clause License.
 * - Copyright (c) 2012-2026 jMonkeyEngine All rights reserved. 
 * - Copyright (c) 2016-2026, Simsilica, LLC All rights reserved.
 * 
 * https://github.com/jMonkeyEngine-Contributions/Lemur
 */

package org.ngengine.gui.guix.containers;

import com.jme3.math.Vector3f;
import org.ngengine.gui.component.SpringGridLayout;
import org.ngengine.gui.core.GuiControl;
import org.ngengine.gui.core.GuiControlListener;
import org.ngengine.gui.core.GuiUpdateListener;
import org.ngengine.gui.style.ElementId;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.ngengine.gui.Axis;
import org.ngengine.gui.Container;
import org.ngengine.gui.FillMode;
import org.ngengine.gui.Label;
import org.ngengine.gui.Panel;
import org.ngengine.platform.AsyncTask;
import org.ngengine.platform.NGEPlatform;

public class NMultiPageList<T> extends Container implements GuiUpdateListener, GuiControlListener {
    private static final Logger logger = Logger.getLogger(NMultiPageList.class.getName());

    // public static final ElementId ELEMENT_ID = new ElementId(Container.ELEMENT_ID).child("multiPageList");
    public static final String ELEMENT_ID = "multiPageList";

    protected Function<T, Panel> renderer = item -> {
        if(item==null){
            Label placeHolder = new Label("");
            placeHolder.setCullHint(CullHint.Always);
            return placeHolder;
        }
        return new Label(item.toString());
    };

    private List<T> items = new ArrayList<>();

    private int page = 0;
    private float itemHeight = 12;
    private float height = 0;
    private Consumer<Integer> onPageChange;
    private BiConsumer<Integer, Consumer<LoadedItems<T>>>  onPageLoadRequest;
    private ArrayList<Panel> renderedItems = new ArrayList<>();
    private AsyncTask<LoadedItems<T>> pageLoadingTask = null;
    private boolean hasNextPage = false;
    private Duration loadingCooldown = Duration.ofMillis(200);
    private Instant lastLoading = null;
    private boolean canLoadMore = true;

    public static class LoadedItems<T> {
        protected final Collection<T> items;
        protected final boolean canLoadMore;
        public LoadedItems(Collection<T> items, boolean hasMore) {
            this.items = items;
            this.canLoadMore = hasMore;
        }
    }

    public NMultiPageList() {
        super(new SpringGridLayout(Axis.Y, Axis.X, FillMode.None, FillMode.Even), new ElementId(ELEMENT_ID));
        GuiControl c = getControl(GuiControl.class);
        c.addUpdateListener(this);
        c.addListener(this);
    }

    public void setLoadingCooldown(Duration cooldown) {
        this.loadingCooldown = cooldown;
    }

    public void setRenderer(Function<T, Panel> renderer) {
        this.renderer = renderer;
    }

    public void setPageLoadingHandler(BiConsumer<Integer, Consumer<LoadedItems<T>>> onPageLoadRequest) {
        this.onPageLoadRequest = onPageLoadRequest;
    }

    public void setPageChangeListener(Consumer<Integer> onPageChange) {
        this.onPageChange = onPageChange;
    }

    public int getElementsPerPage(){
        return (int) (height / itemHeight);
    }

    protected void callPageChangeListener() {
        if (onPageChange != null) {
            onPageChange.accept(page);
        }
    }

    public void nextPage() {
        if(!hasNextPage()){
            return;
        }
        page++;
        invalidate();
    }

    public boolean hasNextPage() {
        return hasNextPage;
    }

    public void previousPage() {
        if (!hasPreviousPage()) {
            return;
        }
        page--;
        invalidate();
    }

    public boolean hasPreviousPage() {
        return page > 0;
    }

    public void addItem(T item) {
        if(items.contains(item)) return;
        items.add(item);
        invalidate();
    }

    public void removeItem(T item) {
        items.remove(item);
        invalidate();
    }

    public void clear() {
        items.clear();
        page = 0;
        invalidate();
    }

    public T getItem(int index) {
        return items.get(index);
    }

    public int getItemCount() {
        return items.size();
    }

    public void removeItem(int index) {
        items.remove(index);
        invalidate();
    }

    public void sort(Comparator<T> comparator) {
        items.sort(comparator);
        invalidate();
        page = 0;
    }


    public void invalidate(){
        clearChildren();
        renderedItems.clear();
        if(pageLoadingTask!=null){
            pageLoadingTask.cancel();
            pageLoadingTask = null;
        }
        canLoadMore = true;
        
    }



    @Override
    public void guiUpdate(GuiControl source, float tpf) {
      
        int elementsPerPage = getElementsPerPage();
        int start = page * elementsPerPage;
        int end = start + elementsPerPage;

        if(pageLoadingTask!=null && pageLoadingTask.isDone()){
            try{
                LoadedItems<T> els = pageLoadingTask.await();
                if(els!=null){
                    for(T el : els.items){
                        addItem(el);
                    }
                    canLoadMore = els.canLoadMore;
                } else {
                    canLoadMore = false;
                }
                pageLoadingTask = null;
            }catch(Exception e){
                logger.log(Level.WARNING,"Error while loading page data", e);
            }
        }

        if(
            items.size()<end
            &&pageLoadingTask==null
            &&(lastLoading==null||Instant.now().isAfter(lastLoading.plus(loadingCooldown)))
            && canLoadMore
        ){
            pageLoadingTask = NGEPlatform.get().wrapPromise((res,rej)->{
                try{
                    onPageLoadRequest.accept(end - items.size(), res);
                }catch(Throwable e){
                    logger.log(Level.WARNING,"Error while requesting page data", e);
                    res.accept(null);
                }
            });
            lastLoading = Instant.now();
        }

        int clampedEnd = Math.min(end, items.size());
        int remainingInPage = (clampedEnd - start) - renderedItems.size();
        if(remainingInPage>0){
            for(int i = start; i<clampedEnd;i++){
                T item = items.get(i);
                Panel panel = renderer.apply(item);
                panel.setCullHint(CullHint.Always);
                addChild(panel);
                renderedItems.add(panel);
            }
            callPageChangeListener();
        }

        boolean hasNextPage = renderedItems.size() >= elementsPerPage;
        if(this.hasNextPage!=hasNextPage){
            this.hasNextPage = hasNextPage;
            callPageChangeListener();
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
        }  

    }

    @Override
    public void reshape(GuiControl source, Vector3f pos, Vector3f size) {
        if (size.y > height) {
            height = size.y;
            invalidate();
        }
    }

    @Override
    public void focusGained(GuiControl source) {}

    @Override
    public void focusLost(GuiControl source) {}
}
