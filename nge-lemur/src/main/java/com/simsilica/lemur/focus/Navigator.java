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

package com.simsilica.lemur.focus;

import java.lang.ref.WeakReference;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.List;
import java.util.function.Predicate;

import com.jme3.bounding.BoundingBox;
import com.jme3.bounding.BoundingVolume;
import com.jme3.renderer.ViewPort;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.control.Control;
import com.simsilica.lemur.core.GuiControl;

public class Navigator {

    private static final int MAX_FOCUS_HISTORY = 32;

    private Spatial focus;
    private List<Spatial> focusHierarchy = Collections.emptyList();

    private boolean hierarchyUpdating = false;
    private Spatial queuedFocus = null;

    private final WeakReference<ViewPort> vpRef;

    // Only used when focus gets invalid due to detach/disconnect.
    private final Deque<WeakReference<Spatial>> focusHistory = new ArrayDeque<>();
    private boolean focusLostDueToDisconnect = false;

    // If true, Next/Previous wrap around.
    private boolean wrapLinearNavigation = true;

    // Modal trap stack (last pushed = active).
    private final Deque<ModalEntry> modalStack = new ArrayDeque<>();

    public Navigator() {
        this.vpRef = null;
    }

    public Navigator(ViewPort vp) {
        this.vpRef = vp != null ? new WeakReference<>(vp) : null;
    }

    private ViewPort getViewPort() {
        return vpRef != null ? vpRef.get() : null;
    }

    public void setWrapLinearNavigation(boolean wrap) {
        this.wrapLinearNavigation = wrap;
    }

    // ------------------------------------------------------------
    // Listener traversal (short-circuit on false)
    // ------------------------------------------------------------

    private boolean foreachListener(Predicate<NavigatorListener> action) {
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
            for (NavigatorListener listener : c.getNavigatorListeners()) {
                if (!action.test(listener)) {
                    return false;
                }
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

    // ------------------------------------------------------------
    // Modal API
    // ------------------------------------------------------------

    public void pushModal(Spatial root, Runnable onClose, boolean stealFocus) {
        if (root == null) {
            return;
        }
        modalStack.addLast(new ModalEntry(root, onClose, focus));

        if (stealFocus) {
            Spatial first = pickInitialFocus(root, TraversalDirection.Next);
            setFocusInternal(first, true, false); // modal clamp should NOT push history
        }
    }

    public void pushModal(Spatial root) {
        pushModal(root, null, true);
    }

    public void popModal(Spatial root) {
        if (root == null || modalStack.isEmpty()) {
            return;
        }
        ModalEntry top = modalStack.peekLast();
        if (top.root != root) {
            return;
        }
        modalStack.removeLast();
        if (top.onClose != null) {
            top.onClose.run();
        }

        Spatial restore = top.previousFocus;
        if (restore != null) {
            focus(restore);
        }
    }

    public boolean hasModal() {
        return !modalStack.isEmpty();
    }

    private Spatial getModalRoot() {
        return modalStack.isEmpty() ? null : modalStack.peekLast().root;
    }

    // ------------------------------------------------------------
    // Public API
    // ------------------------------------------------------------

    public void scroll(final ScrollDirection dir, final double delta) {
        if (!ensureFocusForActionOrScroll(dir)) {
            return;
        }

        if (!foreachListener(new Predicate<NavigatorListener>() {
            @Override
            public boolean test(NavigatorListener l) {
                return l.beforeNavigatorScroll(dir, delta);
            }
        })) {
            return;
        }

        FocusTarget t = findFocusTarget(focus);
        if (t != null) {
            t.focusScrollUpdate(dir, delta);
        }

        foreachListener(new Predicate<NavigatorListener>() {
            @Override
            public boolean test(NavigatorListener l) {
                l.afterNavigatorScroll(dir, delta);
                return true;
            }
        });
    }

    public void action(final boolean pressed) {
        if (!foreachListener(new Predicate<NavigatorListener>() {
            @Override
            public boolean test(NavigatorListener l) {
                return l.beforeNavigatorAction(pressed);
            }
        })) {
            return;
        }

        FocusTarget t = findFocusTarget(focus);
        if (t != null) {
            t.focusAction(pressed);
        }

        foreachListener(new Predicate<NavigatorListener>() {
            @Override
            public boolean test(NavigatorListener l) {
                l.afterNavigatorAction(pressed);
                return true;
            }
        });
    }

    public Spatial navigate(final TraversalDirection dir) {
        if (!foreachListener(new Predicate<NavigatorListener>() {
            @Override
            public boolean test(NavigatorListener l) {
                return l.beforeNavigatorNavigate(dir);
            }
        })) {
            return focus;
        }

        Spatial current = focus;
        if (current == null) {
            boolean wasDisconnect = focusLostDueToDisconnect;

            if (!ensureFocusForNavigation(dir)) {
                foreachListener(new Predicate<NavigatorListener>() {
                    @Override
                    public boolean test(NavigatorListener l) {
                        l.afterNavigatorNavigate(dir);
                        return true;
                    }
                });
                return null;
            }

            if (wasDisconnect) {
                Spatial moved = moveFrom(focus, dir);
                foreachListener(new Predicate<NavigatorListener>() {
                    @Override
                    public boolean test(NavigatorListener l) {
                        l.afterNavigatorNavigate(dir);
                        return true;
                    }
                });
                return moved != null ? moved : focus;
            }

            return focus;
        }

        Spatial newFocus = moveFrom(current, dir);

        foreachListener(new Predicate<NavigatorListener>() {
            @Override
            public boolean test(NavigatorListener l) {
                l.afterNavigatorNavigate(dir);
                return true;
            }
        });

        return newFocus;
    }

    public Spatial getFocus() {
        return focus;
    }

    public void focus(final Spatial newFocus) {
        if (!foreachListener(new Predicate<NavigatorListener>() {
            @Override
            public boolean test(NavigatorListener l) {
                return l.beforeNavigatorFocus(newFocus);
            }
        })) {
            return;
        }

        // IMPORTANT:
        // - Normal focus changes should push old focus into history.
        // - Explicit clear (null) should NOT push (else you "restore" after explicit clears).
        boolean pushOld = newFocus != null;
        setFocusInternal(newFocus, true, pushOld);

        foreachListener(new Predicate<NavigatorListener>() {
            @Override
            public boolean test(NavigatorListener l) {
                l.afterNavigatorFocus(newFocus);
                return true;
            }
        });
    }

    public boolean unfocus(Spatial s) {
        if (focusHierarchy.indexOf(s) < 0) {
            return false;
        }
        focus(null);
        return true;
    }

    public void update(float tpf) {
        Spatial modalRoot = getModalRoot();
        if (modalRoot != null) {
            if (focus != null && !isDescendantOf(focus, modalRoot)) {
                Spatial first = pickInitialFocus(modalRoot, TraversalDirection.Next);
                setFocusInternal(first, true, false); // modal clamp shouldn't push history
            }
            return;
        }

        if (focus == null) {
            return;
        }

        boolean disconnected = !isConnected(focusHierarchy);
        boolean noLongerFocusable = !isFocusable(focus);

        if (disconnected || noLongerFocusable) {
            focusLostDueToDisconnect = true;
            pushHistory(focus);

            setFocusInternal(null, false, false);

            if (restoreFromHistory()) {
                focusLostDueToDisconnect = false;
            }
        }
    }

    // ------------------------------------------------------------
    // Navigation picking
    // ------------------------------------------------------------

    private Spatial moveFrom(Spatial from, TraversalDirection dir) {
        ViewPort vp = getViewPort();
        if (from == null || vp == null) {
            return null;
        }

        List<Spatial> candidates = collectFocusableCandidates(vp);

        Spatial next = null;
        if (dir == TraversalDirection.Up || dir == TraversalDirection.Down
                || dir == TraversalDirection.Left || dir == TraversalDirection.Right) {
            next = pickDirectional(from, dir, candidates);
        } else if (dir == TraversalDirection.Next) {
            next = pickLinear(from, candidates, true);
        } else if (dir == TraversalDirection.Previous) {
            next = pickLinear(from, candidates, false);
        }

        if (next != null) {
            focus(next); // pushes history
            return focus;
        }
        return null;
    }

    private boolean allowNavigateTo(final TraversalDirection dir, final Spatial from, final Spatial candidate) {
        return foreachListener(new Predicate<NavigatorListener>() {
            @Override
            public boolean test(NavigatorListener l) {
                return l.beforeNavigatorNavigateTo(dir, from, candidate);
            }
        });
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
                if (!wrapLinearNavigation) {
                    return null;
                }
                i = mod(raw, all.size());
            }

            Spatial cand = all.get(i);
            if (cand != null && allowNavigateTo(dir, from, cand)) {
                return cand;
            }
        }

        return null;
    }

    private Spatial pickDirectional(Spatial from, TraversalDirection dir, List<Spatial> candidates) {
        BoundingBox a = worldBox(from);
        if (a == null) {
            return null;
        }

        float acx = a.getCenter().x;
        float acy = a.getCenter().y;

        Spatial best = null;
        double bestScore = Double.POSITIVE_INFINITY;

        for (Spatial s : candidates) {
            if (s == null || s == from) {
                continue;
            }

            BoundingBox b = worldBox(s);
            if (b == null) {
                continue;
            }

            float bcx = b.getCenter().x;
            float bcy = b.getCenter().y;

            float dx = bcx - acx;
            float dy = bcy - acy;

            if (!inHalfPlane(dx, dy, dir)) {
                continue;
            }

            double forward = forwardDist(dx, dy, dir);
            double perp = perpDist(dx, dy, dir);
            double perpWeight = (dir == TraversalDirection.Left || dir == TraversalDirection.Right) ? 6.0 : 2.0;

            double score = forward + perp * perpWeight;

            if (score < bestScore && allowNavigateTo(dir, from, s)) {
                bestScore = score;
                best = s;
            }
        }

        return best;
    }

    private static boolean inHalfPlane(float dx, float dy, TraversalDirection dir) {
        final float eps = 0.0001f;
        if (dir == TraversalDirection.Left) return dx < -eps;
        if (dir == TraversalDirection.Right) return dx > eps;
        if (dir == TraversalDirection.Up) return dy > eps;
        if (dir == TraversalDirection.Down) return dy < -eps;
        return false;
    }

    private static double forwardDist(float dx, float dy, TraversalDirection dir) {
        if (dir == TraversalDirection.Left) return -dx;
        if (dir == TraversalDirection.Right) return dx;
        if (dir == TraversalDirection.Up) return dy;
        if (dir == TraversalDirection.Down) return -dy;
        return 0;
    }

    private static double perpDist(float dx, float dy, TraversalDirection dir) {
        if (dir == TraversalDirection.Left || dir == TraversalDirection.Right) {
            return Math.abs(dy);
        }
        if (dir == TraversalDirection.Up || dir == TraversalDirection.Down) {
            return Math.abs(dx);
        }
        return 0;
    }

    private static int mod(int x, int m) {
        int r = x % m;
        return r < 0 ? r + m : r;
    }

    // ------------------------------------------------------------
    // Ensure focus
    // ------------------------------------------------------------

    private boolean ensureFocusForNavigation(TraversalDirection initialDir) {
        return ensureFocus(initialDir != null ? initialDir : TraversalDirection.Next);
    }

    private boolean ensureFocusForActionOrScroll(ScrollDirection scrollDir) {
        return ensureFocus(mapScrollToTraversal(scrollDir));
    }

    private TraversalDirection mapScrollToTraversal(ScrollDirection dir) {
        if (dir == ScrollDirection.Up) return TraversalDirection.Up;
        if (dir == ScrollDirection.Down) return TraversalDirection.Down;
        if (dir == ScrollDirection.Left) return TraversalDirection.Left;
        if (dir == ScrollDirection.Right) return TraversalDirection.Right;
        return TraversalDirection.Next;
    }

    private boolean ensureFocus(TraversalDirection reason) {
        if (focus != null) {
            return true;
        }

        Spatial modalRoot = getModalRoot();
        if (modalRoot != null) {
            Spatial first = pickInitialFocus(modalRoot, reason);
            setFocusInternal(first, true, false);
            return focus != null;
        }

        if (focusLostDueToDisconnect) {
            focusLostDueToDisconnect = false;
            if (restoreFromHistory()) {
                return true;
            }
        }

        Spatial first = pickInitialFocus(null, reason);
        if (first != null) {
            focus(first); // keep before/afterNavigatorFocus hooks, and push history if relevant
            return focus != null;
        }

        return false;
    }

    /**
     * Picks the first focus target (top-most, then left-most) and applies
     * the beforeNavigatorNavigateTo filter so listeners can veto candidates.
     */
    private Spatial pickInitialFocus(Spatial root, TraversalDirection reason) {
        TraversalDirection dir = reason != null ? reason : TraversalDirection.Next;

        List<Spatial> list;
        if (root != null) {
            list = collectFocusableInSubtree(root);
        } else {
            ViewPort vp = getViewPort();
            list = vp != null ? collectFocusableInViewPort(vp) : Collections.<Spatial>emptyList();
        }

        Spatial best = null;
        float bestTop = -Float.MAX_VALUE;
        float bestLeft = Float.MAX_VALUE;

        for (Spatial s : list) {
            BoundingBox bb = worldBox(s);
            if (bb == null) {
                continue;
            }

            if (!allowNavigateTo(dir, null, s)) {
                continue;
            }

            float top = bb.getCenter().y + bb.getYExtent();
            float left = bb.getCenter().x - bb.getXExtent();

            if (top > bestTop || (top == bestTop && left < bestLeft)) {
                bestTop = top;
                bestLeft = left;
                best = s;
            }
        }

        return best;
    }

    // ------------------------------------------------------------
    // History (WeakReference + capped)
    // ------------------------------------------------------------

    private void pushHistory(Spatial s) {
        if (s == null) {
            return;
        }

        // Don't push duplicates on top.
        Spatial top = peekHistory();
        if (top == s) {
            return;
        }

        focusHistory.addFirst(new WeakReference<>(s));

        // Cap size
        while (focusHistory.size() > MAX_FOCUS_HISTORY) {
            focusHistory.removeLast();
        }
    }

    private Spatial peekHistory() {
        while (!focusHistory.isEmpty()) {
            WeakReference<Spatial> ref = focusHistory.peekFirst();
            Spatial s = ref != null ? ref.get() : null;
            if (s != null) {
                return s;
            }
            focusHistory.removeFirst(); // prune cleared refs
        }
        return null;
    }

    private boolean restoreFromHistory() {
        ViewPort vp = getViewPort();

        while (!focusHistory.isEmpty()) {
            WeakReference<Spatial> ref = focusHistory.removeFirst();
            Spatial cand = ref != null ? ref.get() : null;
            if (cand == null) {
                continue;
            }

            Spatial modalRoot = getModalRoot();
            if (modalRoot != null && !isDescendantOf(cand, modalRoot)) {
                continue;
            }

            if (vp != null && !isInViewPort(vp, cand)) {
                continue;
            }

            Spatial resolved = resolveRequestedFocus(cand);
            if (resolved == null) {
                continue;
            }

            List<Spatial> h = buildHierarchy(resolved);
            if (!isConnected(h)) {
                continue;
            }

            setFocusInternal(resolved, true, false);
            return focus != null;
        }

        return false;
    }

    // ------------------------------------------------------------
    // Focus internals + hierarchy bookkeeping
    // ------------------------------------------------------------

    private void setFocusInternal(Spatial requested, boolean explicitClear, boolean pushOldToHistory) {
        if (hierarchyUpdating) {
            queuedFocus = requested;
            return;
        }

        Spatial resolved = resolveRequestedFocus(requested);

        if (resolved == null && explicitClear) {
            focusLostDueToDisconnect = false;
        }

        if (focus == resolved) {
            return;
        }

        if (pushOldToHistory) {
            pushHistory(focus);
        }

        focus = resolved;

        hierarchyUpdating = true;
        try {
            updateFocusHierarchy();

            if (queuedFocus != null && queuedFocus != focus) {
                Spatial next = queuedFocus;
                queuedFocus = null;
                focus = resolveRequestedFocus(next);
                updateFocusHierarchy();
            }
        } finally {
            queuedFocus = null;
            hierarchyUpdating = false;
        }
    }

    private Spatial resolveRequestedFocus(Spatial requested) {
        if (requested == null) {
            return null;
        }

        Spatial modalRoot = getModalRoot();
        if (modalRoot != null && !isDescendantOf(requested, modalRoot)) {
            return null;
        }

        if (isFocusable(requested)) {
            return requested;
        }

        if (requested instanceof Node) {
            // preserve old behavior for explicit focus resolution
            return findTopMostFocusableInSubtree(requested);
        }

        return null;
    }

    // ------------------------------------------------------------
    // Candidate collection
    // ------------------------------------------------------------

    private List<Spatial> collectFocusableCandidates(ViewPort vp) {
        Spatial modalRoot = getModalRoot();
        if (modalRoot != null) {
            return collectFocusableInSubtree(modalRoot);
        }
        return collectFocusableInViewPort(vp);
    }

    private static List<Spatial> collectFocusableInViewPort(ViewPort vp) {
        if (vp == null) {
            return Collections.emptyList();
        }
        List<Spatial> out = new ArrayList<>();
        for (Spatial root : vp.getScenes()) {
            collectFocusableDfs(root, out);
        }
        return out;
    }

    private static List<Spatial> collectFocusableInSubtree(Spatial root) {
        if (root == null) {
            return Collections.emptyList();
        }
        List<Spatial> out = new ArrayList<>();
        collectFocusableDfs(root, out);
        return out;
    }

    private static void collectFocusableDfs(Spatial root, List<Spatial> out) {
        if (root == null) {
            return;
        }

        Deque<Spatial> stack = new ArrayDeque<>();
        stack.push(root);

        while (!stack.isEmpty()) {
            Spatial s = stack.pop();
            if (s == null) {
                continue;
            }

            if (isFocusable(s)) {
                out.add(s);
            }

            if (s instanceof Node) {
                Node n = (Node) s;
                for (int i = n.getQuantity() - 1; i >= 0; i--) {
                    stack.push(n.getChild(i));
                }
            }
        }
    }

    private Spatial findTopMostFocusableInSubtree(Spatial root) {
        List<Spatial> list = collectFocusableInSubtree(root);
        if (list.isEmpty()) {
            return null;
        }

        Spatial best = null;
        float bestTop = -Float.MAX_VALUE;
        float bestLeft = Float.MAX_VALUE;

        for (Spatial s : list) {
            BoundingBox bb = worldBox(s);
            if (bb == null) {
                continue;
            }

            float top = bb.getCenter().y + bb.getYExtent();
            float left = bb.getCenter().x - bb.getXExtent();

            if (top > bestTop || (top == bestTop && left < bestLeft)) {
                bestTop = top;
                bestLeft = left;
                best = s;
            }
        }

        return best != null ? best : list.get(0);
    }

    // ------------------------------------------------------------
    // FocusTarget helpers
    // ------------------------------------------------------------

    public static boolean isFocusable(Spatial s) {
        FocusTarget tg = findFocusTarget(s);
        return tg != null && tg.isFocusable();
    }

    public static FocusTarget findFocusTarget(Spatial s) {
        if (s == null) {
            return null;
        }
        for (int i = 0; i < s.getNumControls(); i++) {
            Control c = s.getControl(i);
            if (c instanceof FocusTarget) {
                return (FocusTarget) c;
            }
        }
        return null;
    }

    // ------------------------------------------------------------
    // Hierarchy bookkeeping (focusGained/focusLost)
    // ------------------------------------------------------------

    private static List<Spatial> buildHierarchy(Spatial s) {
        if (s == null) {
            return Collections.emptyList();
        }
        List<Spatial> result = new ArrayList<>();
        for (Spatial cur = s; cur != null; cur = cur.getParent()) {
            result.add(0, cur);
        }
        return result;
    }

    private static boolean isConnected(List<Spatial> hierarchy) {
        if (hierarchy.size() < 2) {
            return true;
        }
        Spatial last = hierarchy.get(0);
        for (int i = 1; i < hierarchy.size(); i++) {
            Spatial child = hierarchy.get(i);
            if (child.getParent() != last) {
                return false;
            }
            last = child;
        }
        return true;
    }

    private void updateFocusHierarchy() {
        List<Spatial> oldH = focusHierarchy;
        List<Spatial> newH = buildHierarchy(focus);

        int common = Math.min(oldH.size(), newH.size());
        int lca = -1;
        for (int i = 0; i < common; i++) {
            if (oldH.get(i) != newH.get(i)) {
                lca = i - 1;
                break;
            }
        }
        if (lca == -1) {
            lca = common - 1;
        }

        for (int i = oldH.size() - 1; i > lca; i--) {
            FocusTarget t = findFocusTarget(oldH.get(i));
            if (t != null) {
                t.focusLost();
            }
        }

        for (int i = lca + 1; i < newH.size(); i++) {
            FocusTarget t = findFocusTarget(newH.get(i));
            if (t != null) {
                t.focusGained();
            }
        }

        focusHierarchy = newH;
    }

    // ------------------------------------------------------------
    // BoundingBox utilities (no custom Bounds class)
    // ------------------------------------------------------------

    private static BoundingBox worldBox(Spatial s) {
        if (s == null) {
            return null;
        }
        BoundingVolume bv = s.getWorldBound();
        if (bv instanceof BoundingBox) {
            return (BoundingBox) bv;
        }
        // Fallback: point-box at world translation
        return new BoundingBox(s.getWorldTranslation().clone(), 0, 0, 0);
    }

    // ------------------------------------------------------------
    // Misc utilities
    // ------------------------------------------------------------

    private static int indexOfIdentity(List<Spatial> list, Spatial s) {
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i) == s) {
                return i;
            }
        }
        return -1;
    }

    private static boolean isDescendantOf(Spatial s, Spatial ancestor) {
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

    private static boolean isInViewPort(ViewPort vp, Spatial s) {
        if (vp == null || s == null) {
            return false;
        }
        Spatial root = s;
        while (root.getParent() != null) {
            root = root.getParent();
        }
        for (Spatial sceneRoot : vp.getScenes()) {
            if (sceneRoot == root) {
                return true;
            }
        }
        return false;
    }

    private static final class ModalEntry {
        final Spatial root;
        final Runnable onClose;
        final Spatial previousFocus;

        ModalEntry(Spatial root, Runnable onClose, Spatial previousFocus) {
            this.root = root;
            this.onClose = onClose;
            this.previousFocus = previousFocus;
        }
    }
}
