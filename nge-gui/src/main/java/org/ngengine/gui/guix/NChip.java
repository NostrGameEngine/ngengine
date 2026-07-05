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

package org.ngengine.gui.guix;

import com.jme3.scene.Node;
import org.ngengine.gui.HAlignment;
import org.ngengine.gui.Insets3f;
import org.ngengine.gui.NGEStyle;
import org.ngengine.gui.VAlignment;
import org.ngengine.gui.component.BorderLayout;
import org.ngengine.gui.guix.containers.NPanel;
import org.ngengine.gui.style.ElementId;

public class NChip extends NPanel {
    public static final ElementId PANEL_ID = new ElementId("chip.panel");
    public static final ElementId LABEL_ID = new ElementId("chip.label");
    public static final ElementId ICON_ID = new ElementId("chip.icon");

    private final ElementId labelId;
    private String text;
    private Node leading;
    private NLabel label;
    private float height = NGEStyle.px(32f);
    private float paddingX = NGEStyle.px(10f);
    private float paddingY = NGEStyle.px(5f);
    private float gap = NGEStyle.px(8f);
    private float minTextWidth = 0f;
    private HAlignment textHAlignment = HAlignment.Left;

    public NChip(String text) {
        this(text, null);
    }

    public NChip(String text, Node leading) {
        this(PANEL_ID, LABEL_ID, text, leading);
    }

    public NChip(ElementId panelId, ElementId labelId, String text, Node leading) {
        super(panelId == null ? PANEL_ID : panelId);
        this.labelId = labelId == null ? LABEL_ID : labelId;
        this.text = text == null ? "" : text;
        this.leading = leading;
        rebuild();
    }

    public NLabel getLabel() {
        return label;
    }

    public void setText(String text) {
        String next = text == null ? "" : text;
        if (next.equals(this.text)) {
            return;
        }
        this.text = next;
        rebuild();
    }

    public void setLeading(Node leading) {
        if (this.leading == leading) {
            return;
        }
        this.leading = leading;
        rebuild();
    }

    public void setMetrics(float height, float paddingX, float paddingY, float gap) {
        this.height = Math.max(NGEStyle.px(1f), height);
        this.paddingX = paddingX;
        this.paddingY = paddingY;
        this.gap = gap;
        rebuild();
    }

    public void setMinTextWidth(float minTextWidth) {
        this.minTextWidth = minTextWidth;
        rebuild();
    }

    public void setTextHAlignment(HAlignment alignment) {
        this.textHAlignment = alignment == null ? HAlignment.Left : alignment;
        if (label != null) {
            label.setTextHAlignment(this.textHAlignment);
        }
    }

    private void rebuild() {
        clearChildren();
        setPreferredSize(null);
        setInsets(new Insets3f(paddingY, paddingX, paddingY, paddingX));

        label = new NLabel(text, labelId);
        label.setInsets(new Insets3f(0f, leading != null ? gap : 0f, 0f, 0f));
        label.setTextHAlignment(textHAlignment);
        label.setTextVAlignment(VAlignment.Center);
        float labelWidth = Math.max(minTextWidth, label.getPreferredSize().x);
        label.setPreferredSize(labelWidth, Math.max(NGEStyle.px(1f), height - paddingY * 2f));

        if (leading != null) {
            getLayout().addChild(leading, BorderLayout.Position.West);
        }
        getLayout().addChild(label, BorderLayout.Position.Center);
    }
}
