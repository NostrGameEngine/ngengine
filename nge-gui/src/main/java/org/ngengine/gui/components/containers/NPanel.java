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

import com.jme3.scene.Node;
import com.simsilica.lemur.component.BorderLayout;
import com.simsilica.lemur.style.ElementId;

public class NPanel extends NContainer {

    public NPanel() {
        super(new BorderLayout());
    }

    public NPanel(ElementId id) {
        super(new BorderLayout(), id);
    }

    public NRow addRow() {
        NRow row = new NRow();
        addChild(row, BorderLayout.Position.Center);
        return row;
    }

    public NColumn addCol() {
        NColumn col = new NColumn();
        addChild(col, BorderLayout.Position.Center);
        return col;
    }

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
        return super.addChild(child, position);
    }
}
