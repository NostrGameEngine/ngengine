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
package org.ngengine.gui.win.std;

import com.jme3.math.Vector3f;
import com.simsilica.lemur.component.BorderLayout;
import com.simsilica.lemur.component.DynamicInsetsComponent;
import com.simsilica.lemur.style.ElementId;
import org.ngengine.gui.components.containers.NColumn;
import org.ngengine.gui.components.containers.NPanel;
import org.ngengine.gui.components.containers.NRow;
import org.ngengine.gui.win.NWindow;

public class NHud extends NWindow<Void> {

    private NPanel top;
    private NPanel center;
    private NPanel bottom;

    private NColumn centerCenter;
    private NColumn centerLeft;
    private NColumn centerRight;

    private NRow bottomCenter;
    private NRow bottomLeft;
    private NRow bottomRight;

    private NRow topCenter;
    private NRow topLeft;
    private NRow topRight;

    private NPanel gameConsole;
    private boolean initialized = false;

    public NHud() {
        super(new ElementId("hud"));
        top = new NPanel();
        top.setInsetsComponent(new DynamicInsetsComponent(0, 0, 1, 0));

        center = new NPanel();

        bottom = new NPanel();
        bottom.setInsetsComponent(new DynamicInsetsComponent(1, 0, 0, 0));

        centerCenter = new NColumn();
        centerLeft = new NColumn();
        centerLeft.setInsetsComponent(new DynamicInsetsComponent(0, 0, 0, 1));

        centerRight = new NColumn();
        centerRight.setInsetsComponent(new DynamicInsetsComponent(0, 1, 0, 0));

        bottomCenter = new NRow();
        bottomLeft = new NRow();
        bottomLeft.setInsetsComponent(new DynamicInsetsComponent(0, 0, 0, 1));

        bottomRight = new NRow();
        bottomRight.setInsetsComponent(new DynamicInsetsComponent(0, 1, 0, 0));

        topCenter = new NRow();
        topLeft = new NRow();
        topLeft.setInsetsComponent(new DynamicInsetsComponent(0, 0, 0, 1));

        topRight = new NRow();
        topRight.setInsetsComponent(new DynamicInsetsComponent(0, 1, 0, 0));

        top.addChild(topCenter, BorderLayout.Position.Center);
        top.addChild(topLeft, BorderLayout.Position.West);
        top.addChild(topRight, BorderLayout.Position.East);

        center.addChild(centerCenter, BorderLayout.Position.Center);
        center.addChild(centerLeft, BorderLayout.Position.West);
        center.addChild(centerRight, BorderLayout.Position.East);

        bottom.addChild(bottomCenter, BorderLayout.Position.Center);
        bottom.addChild(bottomLeft, BorderLayout.Position.West);
        bottom.addChild(bottomRight, BorderLayout.Position.East);
    }

    @Override
    protected void preCompose(Vector3f size, Void args) throws Throwable {
        if (!initialized) {
            initialize(size);
            initialized = true;
        }
        setFullscreen(true);
        setFitContent(false);
        setWithTitleBar(false);
        // setCenter(false);
        setPreferredSize(new Vector3f(getManager().getWidth(), 0f, getManager().getHeight()));

        NPanel content = getContent();
        content.addChild(top, BorderLayout.Position.North);
        content.addChild(center, BorderLayout.Position.Center);
        content.addChild(bottom, BorderLayout.Position.South);
    }

    protected void initialize(Vector3f size) throws Throwable {}

    @Override
    protected void compose(Vector3f size, Void args) throws Throwable {}

    public NRow getTop() {
        return topCenter;
    }

    public NRow getTopLeft() {
        return topLeft;
    }

    public NRow getTopRight() {
        return topRight;
    }

    public NColumn getCenter() {
        return centerCenter;
    }

    public NColumn getCenterLeft() {
        return centerLeft;
    }

    public NColumn getCenterRight() {
        return centerRight;
    }

    public NRow getBottom() {
        return bottomCenter;
    }

    public NRow getBottomLeft() {
        return bottomLeft;
    }

    public NRow getBottomRight() {
        return bottomRight;
    }
}
