/*
 * $Id$
 *
 * Copyright (c) 2016, Simsilica, LLC
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

package com.simsilica.lemur.event;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import com.jme3.app.Application;
import com.jme3.app.SimpleApplication;
import com.jme3.app.state.BaseAppState;
import com.jme3.bounding.BoundingBox;
import com.jme3.bounding.BoundingSphere;
import com.jme3.bounding.BoundingVolume;
import com.jme3.material.Material;
import com.jme3.material.RenderState.BlendMode;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.jme3.renderer.Camera;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.shape.Quad;

import com.simsilica.lemur.Command;
import com.simsilica.lemur.GuiGlobals;
import com.simsilica.lemur.Panel;
import com.simsilica.lemur.core.GuiControl;
import com.simsilica.lemur.core.GuiMaterial;
import com.simsilica.lemur.input.InputMapper.ViewState;

/**
 *  Modal-style popup support.
 *
 *  NOTE: This version no longer relies on consuming raw mouse events.
 *  "Modal" is enforced by Navigator trapping focus inside the popup.
 *
 *  - Consume / ConsumeAndClose: traps focus inside popup until closed.
 *  - Close: not modal; focus is placed into the popup but user can navigate away.
 *  - Ignore: treated like Close (no trapping); kept for compatibility.
 *
 *  Clicking outside is not handled here anymore (by design, you said you don't use mouse events).
 *  If you want outside-close behavior, do it in your app-level input mapping (button action) or
 *  add a FocusTarget on a background blocker and close explicitly.
 */
public class PopupState extends BaseAppState {

    static Logger log = Logger.getLogger(PopupState.class.getName());

    public enum ClickMode {
        Consume,
        Close,
        ConsumeAndClose,
        Ignore
    };

    private Node guiNode;

    private ColorRGBA defaultBackgroundColor = new ColorRGBA(0, 0, 0, 0);
    private final List<PopupEntry> stack = new ArrayList<>();
    private PopupEntry current;

    public PopupState() {
    }

    public PopupState( Node guiNode ) {
        this.guiNode = guiNode;
    }

    public boolean hasActivePopups() {
        return isEnabled() && !stack.isEmpty();
    }

    public void showPopup( Spatial popup ) {
        showPopup(popup, ClickMode.Close, null, null);
    }

    public void showPopup( Spatial popup, Command<? super PopupState> closeCommand ) {
        showPopup(popup, ClickMode.Close, closeCommand, null);
    }

    public void showModalPopup( Spatial popup ) {
        showPopup(popup, ClickMode.Consume, null, null);
    }

    public void showModalPopup( Spatial popup, Command<? super PopupState> closeCommand ) {
        showPopup(popup, ClickMode.Consume, closeCommand, null);
    }

    public void showModalPopup( Spatial popup, ColorRGBA backgroundColor ) {
        showPopup(popup, ClickMode.Consume, null, backgroundColor);
    }

    public void showModalPopup( Spatial popup, Command<? super PopupState> closeCommand,
                                ColorRGBA backgroundColor ) {
        showPopup(popup, ClickMode.Consume, closeCommand, backgroundColor);
    }

    public void showPopup( Spatial popup, ClickMode clickMode,
                           Command<? super PopupState> closeCommand,
                           ColorRGBA backgroundColor ) {

        PopupEntry entry = new PopupEntry(popup, clickMode, closeCommand, backgroundColor);
        stack.add(entry);
        current = entry;
        current.show();
    }

    public boolean isPopup( Spatial s ) {
        return getEntry(s) != null;
    }

    public void closePopup( Spatial popup ) {
        PopupEntry entry = getEntry(popup);
        if( entry == null ) {
            throw new IllegalArgumentException("Popup entry not found for:" + popup);
        }
        close(entry);
    }

    protected void close( PopupEntry entry ) {
        if( !stack.remove(entry) ) {
            return;
        }

        // Release first (focus/input), then visuals.
        entry.release();

        if( !stack.isEmpty() ) {
            current = stack.get(stack.size() - 1);
        } else {
            current = null;
        }
    }

    protected PopupEntry getEntry( Spatial popup ) {
        for( PopupEntry entry : stack ) {
            if( entry.popup == popup ) {
                return entry;
            }
        }
        return null;
    }

    protected float getMaxGuiZ() {
        BoundingVolume bv = getGuiNode().getWorldBound();
        return getMaxZ(bv);
    }

    protected float getMaxZ( BoundingVolume bv ) {
        if( bv instanceof BoundingBox ) {
            BoundingBox bb = (BoundingBox)bv;
            return bb.getCenter().z + bb.getZExtent();
        } else if( bv instanceof BoundingSphere ) {
            BoundingSphere bs = (BoundingSphere)bv;
            return bs.getCenter().z + bs.getRadius();
        } else if( bv == null ) {
            return 0;
        }
        Vector3f offset = bv.getCenter().add(0, 0, 1000);
        return offset.z - bv.distanceTo(offset);
    }

    protected float getMinZ( BoundingVolume bv ) {
        if( bv instanceof BoundingBox ) {
            BoundingBox bb = (BoundingBox)bv;
            return bb.getCenter().z - bb.getZExtent();
        } else if( bv instanceof BoundingSphere ) {
            BoundingSphere bs = (BoundingSphere)bv;
            return bs.getCenter().z - bs.getRadius();
        }
        Vector3f offset = bv.getCenter().add(0, 0, -1000);
        return offset.z + bv.distanceTo(offset);
    }

    protected GuiMaterial createBlockerMaterial( ColorRGBA color ) {
        GuiMaterial result = GuiGlobals.getInstance().createMaterial(color, false);
        Material mat = result.getMaterial();
        mat.getAdditionalRenderState().setBlendMode(BlendMode.Alpha);
        return result;
    }

    protected Geometry createBlocker( float z, ColorRGBA backgroundColor ) {
        Camera cam = getApplication().getCamera();

        float width = cam.getWidth() / guiNode.getLocalScale().x;
        float height = cam.getHeight() / guiNode.getLocalScale().y;

        Quad quad = new Quad(width, height);
        Geometry result = new Geometry("blocker", quad);
        GuiMaterial guiMat = createBlockerMaterial(backgroundColor);
        result.setMaterial(guiMat.getMaterial());
        result.setLocalTranslation(0, 0, z);
        return result;
    }

    public Vector2f getGuiSize() {
        Camera cam = getApplication().getCamera();
        float width = cam.getWidth() / getGuiNode().getLocalScale().x;
        float height = cam.getHeight() / getGuiNode().getLocalScale().y;
        return new Vector2f(width, height);
    }

    public void centerInGui( Spatial s ) {
        GuiControl control = s.getControl(GuiControl.class);
        if( control == null ) {
            throw new UnsupportedOperationException("Only spatials with GuiControls are supported");
        }

        Vector2f guiSize = getGuiSize();
        Vector3f size = control.getSize();
        if( size.length() == 0 ) {
            size = control.getPreferredSize();
        }
        size = size.mult(s.getLocalScale());
        Vector3f pos = s.getWorldTranslation();
        Vector3f target = new Vector3f();

        target.x = guiSize.x * 0.5f - size.x * 0.5f;
        target.y = guiSize.y * 0.5f + size.y * 0.5f;
        target.z = pos.z;

        s.move(target.subtract(pos));
    }

    public boolean clampToGui( Spatial s ) {
        GuiControl control = s.getControl(GuiControl.class);
        if( control == null ) {
            throw new UnsupportedOperationException("Only spatials with GuiControls are supported");
        }

        Vector2f guiSize = getGuiSize();
        Vector3f size = control.getSize();
        if( size.length() == 0 ) {
            size = control.getPreferredSize();
        }
        Vector3f pos = s.getWorldTranslation();
        Vector3f delta = new Vector3f();

        if( size.x > guiSize.x ) {
            float x = guiSize.x * 0.5f - size.x * 0.5f;
            delta.x = x - pos.x;
        } else if( pos.x < 0 ) {
            delta.x = -pos.x;
        } else if( pos.x + size.x > guiSize.x ) {
            float x = guiSize.x - size.x;
            delta.x = x - pos.x;
        }

        // Y grows down
        if( size.y > guiSize.y ) {
            float y = guiSize.y * 0.5f - size.y * 0.5f;
            delta.y = y - pos.y;
        } else if( pos.y > guiSize.y ) {
            delta.y = guiSize.y - pos.y;
        } else if( pos.y - size.y < 0 ) {
            float y = size.y;
            delta.y = y - pos.y;
        }

        s.move(delta);
        return delta.length() != 0;
    }

    public void setGuiNode( Node guiNode ) {
        this.guiNode = guiNode;
    }

    public Node getGuiNode() {
        if( guiNode != null ) {
            return guiNode;
        }
        Application app = getApplication();
        if( app instanceof SimpleApplication ) {
            this.guiNode = ((SimpleApplication)app).getGuiNode();
        }
        return guiNode;
    }

    public Vector3f screenToGui( Vector3f screen ) {
        Vector3f result = screen.clone();
        result.x /= getGuiNode().getLocalScale().x;
        result.y /= getGuiNode().getLocalScale().y;
        return result;
    }

    @Override
    protected void initialize( Application app ) {
    }

    @Override
    protected void cleanup( Application app ) {
    }

    @Override
    protected void onEnable() {
    }

    @Override
    public void update( float tpf ) {
        // Close popups that were detached externally.
        if( current != null && !current.isVisible() ) {
            close(current);
        }
    }

    @Override
    protected void onDisable() {
    }

    private class PopupEntry {
        private final Spatial popup;
        private final ClickMode clickMode;
        private final Command<? super PopupState> closeCommand;
        private final ColorRGBA backgroundColor;

        private final float zBase;
        private final Geometry blocker;

        // Cached while popup is guaranteed to be attached/registered.
        private ViewState input;
        private com.simsilica.lemur.focus.Navigator navigator;

        public PopupEntry( Spatial popup,
                           ClickMode clickMode,
                           Command<? super PopupState> closeCommand,
                           ColorRGBA backgroundColor ) {
            this.popup = popup;
            this.clickMode = clickMode;
            this.closeCommand = closeCommand;
            this.backgroundColor = backgroundColor != null ? backgroundColor : defaultBackgroundColor;

            this.zBase = getMaxGuiZ() + 1;
            this.blocker = createBlocker(zBase, this.backgroundColor);
        }

        public boolean isVisible() {
            return popup.getParent() != null;
        }

        public void show() {
            float zOffset = getMinZ(popup.getWorldBound());

            getGuiNode().attachChild(blocker);
            getGuiNode().attachChild(popup);

            float zPopup = zBase + 1;
            if( zOffset < 0 ) {
                zPopup = zPopup - zOffset;
            }

            // Make sure popup is above blocker.
            popup.move(0, 0, zPopup);

            if( popup instanceof Panel ) {
                ((Panel)popup).runEffect(Panel.EFFECT_OPEN);
            }

            // Cache mapper/navigator NOW. Do not call InputMapper.get(popup) later during close.
            input = GuiGlobals.getInstance().getInputMapper().get(popup);
            navigator = input.getNavigator();

            // Original behavior: popup requests input enabled while open.
            input.requestInputEnabled(this);

            boolean modal = (clickMode == ClickMode.Consume || clickMode == ClickMode.ConsumeAndClose);

            // Modal = trap focus inside popup. Non-modal = just focus popup subtree.
            if (modal) {
                navigator.pushModal(popup, null, true);
            } else {
                navigator.focus(popup);
            }
        }

        public void release() {
            // Release input first (safe using cached mapper).
            if (input != null) {
                input.releaseInputEnabled(this);
            }

            // Release modal trap (safe using cached navigator).
            if (navigator != null) {
                navigator.popModal(popup);

                // If focus is still inside the popup subtree and we're about to remove it,
                // clear focus. Navigator.update() would also handle it, this avoids a frame of flicker.
                Spatial f = navigator.getFocus();
                if (f != null && isDescendantOf(f, popup)) {
                    navigator.focus(null);
                }
            }

            // Visual removal (effects may defer actual detach, that's fine).
            if( popup instanceof Panel && ((Panel)popup).hasEffect(Panel.EFFECT_CLOSE) ) {
                ((Panel)popup).runEffect(Panel.EFFECT_CLOSE);
            } else {
                popup.removeFromParent();
            }
            blocker.removeFromParent();

            if( closeCommand != null ) {
                closeCommand.execute(PopupState.this);
            }

            input = null;
            navigator = null;
        }
    }

    private static boolean isDescendantOf( Spatial s, Spatial ancestor ) {
        for( Spatial cur = s; cur != null; cur = cur.getParent() ) {
            if( cur == ancestor ) return true;
        }
        return false;
    }
}
