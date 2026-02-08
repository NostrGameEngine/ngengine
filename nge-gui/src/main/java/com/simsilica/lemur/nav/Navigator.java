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

package com.simsilica.lemur.nav;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.function.Predicate;

import com.jme3.asset.AssetManager;
import com.jme3.math.FastMath;
import com.jme3.renderer.ViewPort;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.util.SafeArrayList;
import com.simsilica.lemur.GuiContext;
import com.simsilica.lemur.GuiContext.GuiContextHandler;
import com.simsilica.lemur.core.GuiControl;

/**
 * Handles and abstracts focus navigation for a specific ViewPort.
 * 
 * @author Riccardo Balbo
 */
public class Navigator implements GuiContextHandler, NavigatorListenerProvider {
    private final GuiContext ctx;
    private final List<NavigatorLayer> layers = new ArrayList<>();
    private final SafeArrayList<NavigatorListener> navigatorListeners = new SafeArrayList<>(
            NavigatorListener.class);
    private boolean autofocus = true;
    private double cursorX, cursorY;
    private Spatial cursorPointer = null;
    private boolean cursorVisible = false;
    private boolean enabled = true;

    public Navigator(GuiContext ctx) {
        this.ctx = ctx;
        setCursor(true);
    }

    public void setEnable(boolean v) {
        this.enabled = v;
    }

    @Override
    public void addNavigatorListener(NavigatorListener l) {
        navigatorListeners.add(l);
    }

    @Override
    public void removeNavigatorListener(NavigatorListener l) {
        if (navigatorListeners == null) {
            return;
        }
        navigatorListeners.remove(l);
    }

    @Override
    public void addNavigatorListener(int priority, NavigatorListener l) {
        if (priority >= navigatorListeners.size()) {
            navigatorListeners.add(l);
        } else {
            navigatorListeners.add(priority, l);
        }
    }

    public Spatial getCursor() {
        return cursorPointer;
    }

    public boolean isCursorVisible() {
        return cursorVisible;
    }

    public void setCursor(boolean visible) {
        if (visible) {
            float size = Math.max(ctx.getViewPort().getCamera().getWidth(),
                    ctx.getViewPort().getCamera().getHeight());
            size *= 0.02f;
            size = FastMath.clamp(size, 12, 50);
            setCursor(new DefaultCursor(ctx.getAssetManager(), size));
        } else {
            setCursor(null);
        }
    }

    public void setCursor(Spatial cursor) {
        if (cursor == null) {
            cursorVisible = false;
        } else {
            cursorVisible = true;
            this.cursorPointer = cursor;
        }
    }

    @Override
    public List<NavigatorListener> getNavigatorListeners() {
        return navigatorListeners;
    }

    public boolean updateCursorPosition(double x, double y) {
        if (!isCursorVisible()) return false;
        autofocus = false;
        this.cursorX = x;
        this.cursorY = y;
        return true;
    }

    private ViewPort getViewPort() {
        return ctx.getViewPort();
    }

    private boolean foreachListener(Predicate<NavigatorListener> action) {
        if (!foreachListener(this, action)) {
            return false;
        }

        ViewPort vp = getViewPort();
        if (vp == null) {
            return true;
        }
        for (Spatial root : vp.getScenes()) {
            if (!foreachListener(root, action)) {
                return false;
            }
        }
        return true;
    }

    private boolean foreachListener(Spatial sp, Predicate<NavigatorListener> action) {
        if (sp == null) {
            return true;
        }

        GuiControl c = sp.getControl(GuiControl.class);
        if (c != null) {
            if (!foreachListener(c, action)) {
                return false;
            }
        }

        if (sp instanceof Node) {
            Node n = (Node) sp;
            for (int i = 0; i < n.getQuantity(); i++) {
                if (!foreachListener(n.getChild(i), action)) {
                    return false;
                }
            }
        }
        return true;
    }

    private boolean foreachListener(NavigatorListenerProvider hl, Predicate<NavigatorListener> action) {
        if (hl == null) {
            return true;
        }

        for (NavigatorListener listener : hl.getNavigatorListeners()) {
            if (!action.test(listener)) {
                return false;
            }
        }

        return true;
    }

    public void pushLayer(Spatial root, Runnable onClose) {
        if (root == null) {
            return;
        }
        layers.add(new NavigatorLayer(root, this::foreachListener, onClose));
        focus(null);
    }

    public void pushLayer(Spatial root) {
        pushLayer(root, null);
    }

    public void popLayer(Spatial root) {
        if (root == null || layers.isEmpty()) {
            return;
        }
        NavigatorLayer layer = null;
        Iterator<NavigatorLayer> it = layers.iterator();
        while (it.hasNext()) {
            NavigatorLayer e = it.next();
            if (e.isSameRoot(root)) {
                layer = e;
                it.remove();
                break;
            }
        }
        if (layer != null) {
            layer.close();
        }
    }

    public NavigatorLayer getCurrentLayer() {
        if (layers.isEmpty()) return null;
        NavigatorLayer layer = null;
        for (int i = layers.size() - 1; i >= 0; i--) {
            NavigatorLayer l = layers.get(i);
            if (l.isInViewPort(getViewPort())) {
                layer = l;
                break;
            }
        }
        for (NavigatorLayer l : layers) {
            if (l != layer) {
                l.setEnabled(false);
            } else {
                l.setEnabled(true);
            }
        }
        return layer;
    }

    public Spatial navigate(TraversalDirection dir) {
        if (!enabled) return null;
        autofocus = true;
        NavigatorLayer layer = getCurrentLayer();
        if (layer == null) return null;
        return layer.navigate(dir);
    }

    public void focus(Spatial newFocus) {
        if (!enabled) return;
        NavigatorLayer layer = getCurrentLayer();
        if (layer == null) return;
        layer.focus(newFocus);
    }

    public boolean unfocus(Spatial s) {
        if (!enabled) return false;
        NavigatorLayer layer = getCurrentLayer();
        if (layer == null) return false;
        layer.unfocus(s);
        return true;
    }

    public void update(float tpf) {

        boolean cursorVisible = enabled && this.cursorVisible && this.cursorPointer != null;
        if (cursorVisible) {
            Node guiNode = ctx.getGuiNode();
            if (cursorPointer.getParent() != guiNode) {
                guiNode.attachChild(cursorPointer);
            }
            cursorPointer.setLocalTranslation((float) cursorX, (float) cursorY, 1000);
        } else if (this.cursorPointer != null && this.cursorPointer.getParent() != null) {
            this.cursorPointer.removeFromParent();
        }

        NavigatorLayer layer = getCurrentLayer();
        if (layer != null) {
            layer.updateFocus(autofocus);
        }
    }

    public Spatial getFocus() {
        NavigatorLayer layer = getCurrentLayer();
        if (layer == null) return null;
        return layer.getFocus();
    }

    public void scroll(ScrollDirection dir, final double delta) {
        NavigatorLayer layer = getCurrentLayer();
        if (layer == null) return;
        layer.scroll(dir, delta);
    }

    public void action(boolean pressed) {
        NavigatorLayer layer = getCurrentLayer();
        if (layer == null) return;
        layer.action(pressed);
    }

    @Override
    public void close() throws IOException {
        for (NavigatorLayer layer : layers) {
            layer.close();
        }
        layers.clear();

    }
}