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

package org.ngengine.gui.nav;

import java.io.Closeable;
import java.lang.ref.WeakReference;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

import org.ngengine.gui.NGEGui;

import com.jme3.math.FastMath;
import com.jme3.renderer.ViewPort;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;

/**
 * Handle the navigation logic on modular layers.
 * 
 * @author Riccardo Balbo, GPT-5.2
 */
final class NavigatorLayer implements Closeable {

    private static final float DIR_EPS = 0.0001f;          // half-plane threshold
    private static final float POS_TIE_EPS = 0.00001f;      // tie-breaking tolerance

    private final Spatial root;
    private final Runnable onClose;
    private final Function<Predicate<NavigatorListener>, Boolean> foreachListener;
    private final Deque<WeakReference<Spatial>> history = new ArrayDeque<>();

    private boolean enabled;
    private Spatial focus;
    private List<Spatial> focusHierarchy = Collections.emptyList();

    NavigatorLayer(Spatial root,
                   Function<Predicate<NavigatorListener>, Boolean> foreachListener,
                   Runnable onClose) {
        this.root = root;
        this.foreachListener = foreachListener;
        this.onClose = onClose;
    }

    Spatial getFocus() {
        return focus;
    }

    boolean isSameRoot(Spatial otherRoot) {
        return this.root == otherRoot;
    }

    @Override
    public void close() {
        setEnabled(false);
        if (onClose != null) {
            onClose.run();
        }
    }

    boolean isInViewPort(ViewPort vp) {
        if (vp == null || root == null) {
            return false;
        }

        Spatial sceneRoot = root;
        while (sceneRoot.getParent() != null) {
            sceneRoot = sceneRoot.getParent();
        }

        for (Spatial vpRoot : vp.getScenes()) {
            if (vpRoot == sceneRoot) {
                return true;
            }
        }
        return false;
    }

    void setEnabled(boolean enabled) {
        if (this.enabled == enabled) {
            return;
        }
        this.enabled = enabled;

        if (enabled) {
            if (restoreFocusFromHistory()) {
                return; 
            }
        } else {
            clearFocus(false);
        }

        updateFocus(true);
    }

    void updateFocus(boolean autofocus) {
        if (!enabled) {
            return;
        }

        // If focus is no longer valid for this layer, drop it.
        if (focus != null && !isValidFocus(focus)) {
            clearFocus(true);
        }

        if (focus != null) {
            return;
        }

        if (autofocus) {
            Spatial best = findVisuallyTopLeftFocusable(root);
            if (best != null) {
                focus(best);
            }
        }
    }

    void action(final boolean pressed) {
        if (!enabled) {
            return;
        }
        updateFocus(true);
        if (focus == null) {
            return;
        }

        if (!before(l -> l.beforeNavigatorAction(pressed))) {
            return;
        }

        FocusTarget t = NGEGui.findFocusTarget(focus);
        if (t != null) {
            t.focusAction(pressed);
        }

        after(l -> l.afterNavigatorAction(pressed));
    }

    Spatial navigate(final TraversalDirection dir) {
        if (!enabled) {
            return focus;
        }
        updateFocus(true);

        if (!before(l -> l.beforeNavigatorNavigate(dir))) {
            Spatial override = getFocusOverride();
            if (override != null) {
                focus(override);
            }
            return focus;
        }

        Spatial current = focus;
        Spatial result = null;

        if (current != null) {
            result = moveFrom(current, dir);
        }

        after(l -> l.afterNavigatorNavigate(dir));
        return result;
    }

    void scroll(final ScrollDirection dir, final double delta) {
        if (!enabled) {
            return;
        }
        updateFocus(true);
        if (focus == null) {
            return;
        }

        if (!before(l -> l.beforeNavigatorScroll(dir, delta))) {
            return;
        }

        FocusTarget t = NGEGui.findFocusTarget(focus);
        if (t != null) {
            t.focusScrollUpdate(dir, delta);
        }

        after(l -> l.afterNavigatorScroll(dir, delta));
    }

    void focus(Spatial requested) {
        if (!enabled) {
            return;
        }
        if (requested == null || requested == focus) {
            return;
        }
        if (!isDescendantOf(requested, root)) {
            return;
        }
        if (!before(l -> l.beforeNavigatorFocus(requested))) {
            return;
        }

        rememberCurrentFocus();
        focus = requested;
        updateFocusHierarchy();

        after(l -> l.afterNavigatorFocus(requested));
    }

    void unfocus(Spatial s) {
        if (s == null || focus != s) {
            return;
        }
        clearFocus(true);
        updateFocus(true);
    }

   
    private boolean before(Function<NavigatorListener, Boolean> call) {
        if (foreachListener == null) {
            return true;
        }
        return Boolean.TRUE.equals(foreachListener.apply(l -> Boolean.TRUE.equals(call.apply(l))));
    }

    private void after(Consumer<NavigatorListener> call) {
        if (foreachListener == null) {
            return;
        }
        foreachListener.apply(l -> {
            call.accept(l);
            return true;
        });
    }

    private boolean allowNavigateTo(TraversalDirection dir, Spatial from, Spatial candidate) {
        return before(l -> l.beforeNavigatorNavigateTo(dir, from, candidate));
    }

    private Spatial getFocusOverride() {
        final Spatial[] result = new Spatial[1];
        if (foreachListener == null) {
            return null;
        }
        foreachListener.apply(l -> {
            Spatial candidate = l.getNavigatorFocusOverride();
            if (candidate != null && result[0] == null) {
                result[0] = candidate;
            }
            return true;
        });
        return result[0];
    }



    private void rememberCurrentFocus() {
        if (focus != null) {
            history.addLast(new WeakReference<>(focus));
        }
        pruneHistoryTail();
    }

    private boolean restoreFocusFromHistory() {
        pruneHistoryTail();

        while (!history.isEmpty()) {
            Spatial candidate = history.removeLast().get();
            if (candidate != null && isDescendantOf(candidate, root)) {
                focus(candidate);
                return true;
            }
        }
        return false;
    }

    private void pruneHistoryTail() {
        while (!history.isEmpty()) {
            WeakReference<Spatial> last = history.peekLast();
            if (last == null || last.get() == null) {
                history.removeLast();
                continue;
            }
            break;
        }
    }

    private void clearFocus(boolean popHistory) {
        if (focus == null) {
            return;
        }

        Spatial old = focus;
        focus = null;

        if (popHistory) {
            WeakReference<Spatial> last = history.peekLast();
            if (last != null && last.get() == old) {
                history.removeLast();
            }
            pruneHistoryTail();
        }

        updateFocusHierarchy();
    }


    private void updateFocusHierarchy() {
        List<Spatial> oldPath = focusHierarchy;

        // If cached path no longer matches the scene graph, treat it as fully lost.
        if (!isConnected(oldPath)) {
            for (int i = oldPath.size() - 1; i >= 0; i--) {
                FocusTarget t = NGEGui.findFocusTarget(oldPath.get(i));
                if (t != null) {
                    t.focusLost();
                }
            }
            oldPath = Collections.emptyList();
        }

        List<Spatial> newPath = buildHierarchy(focus);
        int lca = lowestCommonAncestorIndex(oldPath, newPath);

        // Lose old tail (bottom-up)
        for (int i = oldPath.size() - 1; i > lca; i--) {
            FocusTarget t = NGEGui.findFocusTarget(oldPath.get(i));
            if (t != null) {
                t.focusLost();
            }
        }

        // Gain new tail (top-down)
        for (int i = lca + 1; i < newPath.size(); i++) {
            FocusTarget t = NGEGui.findFocusTarget(newPath.get(i));
            if (t != null) {
                t.focusGained();
            }
        }

        focusHierarchy = newPath;
    }

    private int lowestCommonAncestorIndex(List<Spatial> a, List<Spatial> b) {
        int common = Math.min(a.size(), b.size());
        int lca = -1;
        while (lca + 1 < common && a.get(lca + 1) == b.get(lca + 1)) {
            lca++;
        }
        return lca;
    }


    private Spatial moveFrom(Spatial from, TraversalDirection dir) {
        if (from == null) {
            return null;
        }

        List<Spatial> candidates = collectFocusableInSubtree(root);
        if (candidates.isEmpty()) {
            return null;
        }

        Spatial next = null;

        switch (dir) {
            case Up:
            case Down:
            case Left:
            case Right:
                next = pickDirectionalAligned(from, dir, candidates);
                break;
            case Next:
                next = pickLinear(from, candidates, true);
                break;
            case Previous:
                next = pickLinear(from, candidates, false);
                break;
            default:
                break;
        }

        if (next != null) {
            focus(next);
            return focus;
        }
        return null;
    }


    private Spatial pickDirectionalAligned(Spatial from, TraversalDirection dir, List<Spatial> candidates) {
        float ax = from.getWorldTranslation().x;
        float ay = from.getWorldTranslation().y;

        Spatial best = null;
        float bestPerp = Float.POSITIVE_INFINITY;
        float bestForward = Float.POSITIVE_INFINITY;

        for (Spatial s : candidates) {
            if (s == null || s == from) {
                continue;
            }

            float bx = s.getWorldTranslation().x;
            float by = s.getWorldTranslation().y;

            float dx = bx - ax;
            float dy = by - ay;

            float forward = forwardDistance(dir, dx, dy);
            if (forward <= DIR_EPS) {
                continue; // not in the requested half-plane
            }

            float perp = perpendicularDistance(dir, dx, dy);

            boolean better =
                    (perp < bestPerp - POS_TIE_EPS) ||
                    (Math.abs(perp - bestPerp) <= POS_TIE_EPS && forward < bestForward - POS_TIE_EPS);

            if (better && allowNavigateTo(dir, from, s)) {
                best = s;
                bestPerp = perp;
                bestForward = forward;
            }
        }

        return best;
    }

    private float forwardDistance(TraversalDirection dir, float dx, float dy) {
        switch (dir) {
            case Left:  return -dx;
            case Right: return  dx;
            case Up:    return  dy;
            case Down:  return -dy;
            default:    return Float.POSITIVE_INFINITY;
        }
    }

    private float perpendicularDistance(TraversalDirection dir, float dx, float dy) {
        switch (dir) {
            case Left:
            case Right:
                return Math.abs(dy); // stay on same row
            case Up:
            case Down:
                return Math.abs(dx); // stay on same column
            default:
                return Float.POSITIVE_INFINITY;
        }
    }

    private Spatial pickLinear(Spatial from, List<Spatial> all, boolean forward) {
        if (all.isEmpty()) {
            return null;
        }

        TraversalDirection dir = forward ? TraversalDirection.Next : TraversalDirection.Previous;
        int idx = indexOfIdentity(all, from);
        int start = (idx < 0) ? (forward ? 0 : all.size() - 1) : idx;

        for (int step = 1; step <= all.size(); step++) {
            int raw = forward ? (start + step) : (start - step);
            int i = raw;

            if (i < 0 || i >= all.size()) {
                i = FastMath.mod(raw, all.size());
            }

            Spatial cand = all.get(i);
            if (cand != null && allowNavigateTo(dir, from, cand)) {
                return cand;
            }
        }

        return null;
    }



    private List<Spatial> collectFocusableInSubtree(Spatial root) {
        if (root == null) {
            return Collections.emptyList();
        }

        List<Spatial> out = new ArrayList<>();
        Deque<Spatial> stack = new ArrayDeque<>();
        stack.push(root);

        while (!stack.isEmpty()) {
            Spatial s = stack.pop();
            if (s == null) {
                continue;
            }

            if (isCandidateFocusable(s)) {
                out.add(s);
            }

            if (s instanceof Node) {
                Node n = (Node) s;
                for (int i = n.getQuantity() - 1; i >= 0; i--) {
                    stack.push(n.getChild(i));
                }
            }
        }

        return out;
    }


    private Spatial findVisuallyTopLeftFocusable(Spatial root) {
        List<Spatial> list = collectFocusableInSubtree(root);
        if (list.isEmpty()) {
            return null;
        }

        Spatial best = null;
        float bestY = -Float.MAX_VALUE;
        float bestX = Float.MAX_VALUE;

        for (Spatial s : list) {
            if (s == null) continue;

            float x = s.getWorldTranslation().x;
            float y = s.getWorldTranslation().y;

            boolean better =
                    (y > bestY + POS_TIE_EPS) ||
                    (Math.abs(y - bestY) <= POS_TIE_EPS && x < bestX - POS_TIE_EPS);

            if (better) {
                best = s;
                bestY = y;
                bestX = x;
            }
        }

        return best != null ? best : list.get(0);
    }

    private boolean isCandidateFocusable(Spatial s) {
        if (s == null) return false;
        // avoid focusing the layer root itself 
        if (s == root) return false;
        return NGEGui.isFocusable(s);
    }

    private boolean isValidFocus(Spatial s) {
        return s != null && isDescendantOf(s, root) && NGEGui.isFocusable(s);
    }


    private int indexOfIdentity(List<Spatial> list, Spatial s) {
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i) == s) {
                return i;
            }
        }
        return -1;
    }

    private boolean isDescendantOf(Spatial s, Spatial ancestor) {
        if (s == null || ancestor == null) {
            return false;
        }
        for (Spatial cur = s; cur != null; cur = cur.getParent()) {
            if (cur == ancestor) {
                return true;
            }
        }
        return false;
    }

    private boolean isConnected(List<Spatial> hierarchy) {
        if (hierarchy == null || hierarchy.size() < 2) {
            return true;
        }
        Spatial parent = hierarchy.get(0);
        for (int i = 1; i < hierarchy.size(); i++) {
            Spatial child = hierarchy.get(i);
            if (child == null || child.getParent() != parent) {
                return false;
            }
            parent = child;
        }
        return true;
    }

    private List<Spatial> buildHierarchy(Spatial s) {
        if (s == null) {
            return Collections.emptyList();
        }
        List<Spatial> result = new ArrayList<>();
        for (Spatial cur = s; cur != null; cur = cur.getParent()) {
            result.add(0, cur);
        }
        return result;
    }
}
