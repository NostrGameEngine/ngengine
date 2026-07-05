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

import java.util.EnumMap;
import java.util.Map;

import org.ngengine.gui.Container;
import org.ngengine.gui.Panel;

import com.jme3.scene.Node;
import org.ngengine.gui.component.BorderLayout;
import org.ngengine.gui.style.ElementId;

/**
 * A container that can be split into rows and columns.
 */
public class NPanel extends NContainer {
    public static final ElementId ELEMENT_ID = new ElementId("n-panel." + Panel.ELEMENT_ID);

    private Map<BorderLayout.Position, Container> containers = new EnumMap<>(BorderLayout.Position.class);


    public NPanel() {
        this(ELEMENT_ID);
    }

    public NPanel(ElementId id) {
        super(new BorderLayout(), id);
        applyElementStyles();
    }

    @Override
    public NRow addRow() {
        NRow row = new NRow();
        addChild(row, BorderLayout.Position.Center);
        return row;
    }

    @Override
    public NColumn addCol() {
        NColumn col = new NColumn();
        addChild(col, BorderLayout.Position.Center);
        return col;
    }

    @Override
    public NPanel addSubPanel() {
        NPanel panel = new NPanel();
        addChild(panel, BorderLayout.Position.Center);
        return panel;
    }

    public NRow addRow(BorderLayout.Position position) {
        NRow row = new NRow();
        addChild(row, position);
        return row;
    }

    public NColumn addCol(BorderLayout.Position position) {
        NColumn col = new NColumn();
        addChild(col, position);
        return col;
    }

    public NPanel addSubPanel(BorderLayout.Position position) {
        NPanel panel = new NPanel();
        addChild(panel, position);
        return panel;
    }

    public <T extends Node> T addChild(T child, BorderLayout.Position position) {
        Container container = this.containers.get(position);
        if(container!=null&&container.getParent() != this) {
            container = null;
        }
        if(container == null) {
            container = new NColumn();
            super.addChild(container, position);
            this.containers.put(position, container);
        }
        container.addChild(child);
        return child;
    }

    public <T extends Node> T addChild(T child) {
        return addChild(child, BorderLayout.Position.Center);
    }


    @Override
    public void clearChildren() {
        super.clearChildren();
        this.containers.clear();
    }

   
    
}
