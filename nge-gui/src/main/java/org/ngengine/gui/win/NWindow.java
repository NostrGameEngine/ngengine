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
package org.ngengine.gui.win;

import com.jme3.math.Vector3f;
import com.simsilica.lemur.Container;
import com.simsilica.lemur.HAlignment;
import com.simsilica.lemur.Insets3f;
import com.simsilica.lemur.Label;
import com.simsilica.lemur.VAlignment;
import com.simsilica.lemur.component.BorderLayout;
import com.simsilica.lemur.core.GuiControl;
import com.simsilica.lemur.core.GuiControlListener;
import com.simsilica.lemur.core.GuiLayout;
import com.simsilica.lemur.core.GuiUpdateListener;
import com.simsilica.lemur.style.ElementId;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.ngengine.gui.components.NIconButton;
import org.ngengine.gui.components.containers.NPanel;
import org.ngengine.gui.win.std.NErrorWindow;

public abstract class NWindow<T> extends Container implements GuiUpdateListener, GuiControlListener {

    private static final Logger log = Logger.getLogger(NWindow.class.getName());
    public static final String ELEMENT_ID = "window";
    private Container titleBar;
    private NIconButton backButton;
    private NIconButton placeHolderButton;
    private Label title;
    private NWindowManagerComponent appState;
    private NPanel windowContent;

    private boolean center = true;

    private boolean fitContent = true;
    private boolean fullscreen = false;
    private boolean withTitleBar = true;
    private T args;

    private final List<NWindowListener> closeListeners = new CopyOnWriteArrayList<>();

    protected NWindow() {
        super(new BorderLayout(), new ElementId(ELEMENT_ID));
    }

    protected NWindow(GuiLayout layout, ElementId id) {
        super(layout, id);
    }

    public void setFullscreen(boolean fullscreen) {
        this.fullscreen = fullscreen;
        invalidate();
    }

    public boolean isFullscreen() {
        return fullscreen;
    }

    public boolean isWithTitleBar() {
        return withTitleBar;
    }

    public void setWithTitleBar(boolean withTitleBar) {
        this.withTitleBar = withTitleBar;
        if (withTitleBar) {
            addChild(titleBar, BorderLayout.Position.North);
        } else {
            titleBar.removeFromParent();
        }
        invalidate();
    }

    protected NWindow(ElementId id) {
        super(new BorderLayout(), id);
    }

    final void initialize(NWindowManagerComponent appState, Consumer<NWindow<T>> backAction) {
        this.appState = appState;
        titleBar = new Container(new BorderLayout(), new ElementId("window.titleBar"));

        backButton = new NIconButton("icons/outline/chevron-left.svg");
        backButton.addClickCommands(src -> {
            if (backAction != null) {
                backAction.accept(this);
            }
        });
        placeHolderButton = new NIconButton("icons/outline/chevron-left.svg");
        placeHolderButton.setCullHint(CullHint.Always);

        title = new Label("", new ElementId("window.title"));
        title.setTextHAlignment(HAlignment.Center);
        title.setTextVAlignment(VAlignment.Center);
        float margin = title.getFontSize();
        title.setInsets(new Insets3f(0, margin, 0, margin));
        titleBar.addChild(title, BorderLayout.Position.Center);

        addChild(titleBar, BorderLayout.Position.North);

        windowContent = new NPanel(new ElementId("window.content"));
        // windowContent.setInsetsComponent(new DynamicInsetsComponent(0.5f, 0.5f, 0.5f, 0.5f));
        addChild(windowContent, BorderLayout.Position.Center);
        setBackAction(backAction);

        getControl(GuiControl.class).addListener(this);
        getControl(GuiControl.class).addUpdateListener(this);
        invalidate();
    }

    final void setArgs(T args) {
        this.args = args;
    }

    public final void setFitContent(boolean fitContent) {
        this.fitContent = fitContent;
        invalidate();
    }

    public final void addWindowListener(NWindowListener listener) {
        if (listener != null && !closeListeners.contains(listener)) {
            closeListeners.add(listener);
        }
    }

    public final void removeWindowListener(NWindowListener listener) {
        if (listener != null) {
            closeListeners.remove(listener);
        }
    }

    protected abstract void compose(Vector3f size, T args) throws Throwable;

    protected final void onShow() {
        for (NWindowListener listener : closeListeners) {
            listener.onShow(this);
        }
    }

    protected final void onHide() {
        for (NWindowListener listener : closeListeners) {
            listener.onHide(this);
        }
    }

    public final void setCenter(boolean center) {
        this.center = center;
        invalidate();
    }

    protected final NWindowManagerComponent getManager() {
        return appState;
    }

    protected NPanel getContent() {
        return windowContent;
    }

    public final void setTitle(String title) {
        this.title.setText(title);
    }

    public final void setBackAction(Consumer<NWindow<T>> backAction) {
        if (backAction == null && backButton != null) {
            backButton.removeFromParent();
            placeHolderButton.removeFromParent();
        } else {
            titleBar.addChild(backButton, BorderLayout.Position.West);
            titleBar.addChild(placeHolderButton, BorderLayout.Position.East);
        }
    }

    final void recenter(Vector3f size) {
        getManager()
            .runInThread(() -> {
                int width = getManager().getWidth();
                int height = getManager().getHeight();

                setLocalTranslation(width / 2 - size.x / 2, height / 2 + size.y / 2, 1);
            });
    }

    @Override
    public final void reshape(GuiControl source, Vector3f pos, Vector3f size) {
        if (center) {
            recenter(size);
        }
    }

    @Override
    public final void focusGained(GuiControl source) {}

    @Override
    public final void focusLost(GuiControl source) {}

    protected int initStage = 0;

    protected final void invalidate() {
        initStage = 0;
    }

    protected void preCompose(Vector3f size, T args) throws Throwable {
        // Override this method to perform any pre-composition tasks
    }

    protected final boolean reloadNow() {
        Vector3f size = getSize().clone();
        if (size.length() == 0) return false;

        getContent().clearChildren();

        try {
            preCompose(size, args);
            compose(size, args);
        } catch (Throwable e) {
            log.log(Level.SEVERE, "Failed to compose window content", e);
            getManager().closeWindow(this);
            getManager().showWindow(NErrorWindow.class, e);
            return true;
        }

        if (!fitContent) {
            int w, h;
            if (fullscreen) {
                w = getManager().getWidth();
                h = getManager().getHeight();
            } else {
                w = (int) (getManager().getWidth() * 0.8);
                h = (int) (getManager().getHeight() * 0.8);
                if (w == getManager().getWidth()) w -= 2;
                if (h == getManager().getHeight()) h -= 2;
            }
            if (w < 800) w = 800;
            if (h < 600) h = 600;
            if (w > getManager().getWidth()) w = getManager().getWidth();
            if (h > getManager().getHeight()) h = getManager().getHeight();
            setPreferredSize(new Vector3f(w, h, 0));
        } else {
            setPreferredSize(null);
        }
        return true;
    }

    @Override
    public final void guiUpdate(GuiControl source, float tpf) {
        if (initStage == 0) {
            if (reloadNow()) initStage = 1;
        } else if (initStage == 1) {
            if (reloadNow()) initStage = 2;
        }
        if (center) {
            recenter(getSize());
        }
    }

    public final void close() {
        getManager()
            .runInThread(() -> {
                getManager().closeWindow(this);
            });
    }
}
