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
package org.ngengine.auth.stored;

import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.simsilica.lemur.Button;
import com.simsilica.lemur.HAlignment;
import com.simsilica.lemur.Label;
import com.simsilica.lemur.component.DynamicInsetsComponent;
import com.simsilica.lemur.component.IconComponent;
import com.simsilica.lemur.style.ElementId;
import org.ngengine.gui.components.NVSpacer;
import org.ngengine.gui.components.containers.NColumn;
import org.ngengine.gui.win.NWindow;

public class StoredAuthSelectionWindow extends NWindow<StoredAuthSelectionOptions> {

    @Override
    protected void compose(Vector3f size, StoredAuthSelectionOptions args) throws Throwable {
        setTitle("Identity Selected");
        Label label = new Label(args.getAlias(), new ElementId("highlighted").child(Label.ELEMENT_ID));
        label.setTextHAlignment(HAlignment.Center);

        Button confirmButton = new Button(
            "Continue as " + args.getAlias(),
            new ElementId("highlighted").child(Button.ELEMENT_ID)
        );
        confirmButton.addClickCommands(b -> {
            if (args.getConfirmAction() != null) {
                args.getConfirmAction().accept(this);
            }
            close();
        });

        Button cancelButton = new Button("Go back");
        cancelButton.addClickCommands(b -> {
            if (args.getCancelAction() != null) {
                args.getCancelAction().accept(this);
            }
            close();
        });

        Button removeButton = new Button("Delete identity", new ElementId("danger").child(Button.ELEMENT_ID));
        removeButton.addClickCommands(b -> {
            if (args.getRemoveAction() != null) {
                args.getRemoveAction().accept(this);
            }
            close();
        });
        getContent().clearChildren();
        NColumn windowContent = getContent().addCol();

        if (args.getIcon() != null) {
            IconComponent icon = new IconComponent(args.getIcon(), new Vector2f(1, 1), 0, 0, 0, false);
            float iconSize = Math.min(size.x, size.y) * 0.5f;

            icon.setIconSize(new Vector2f(iconSize, iconSize));
            icon.setMargin(0, 0);
            Label iconLabel = new Label("");
            iconLabel.setIcon(icon);
            windowContent.addChild(iconLabel);
            iconLabel.setInsetsComponent(new DynamicInsetsComponent(1, 1, 1, 1));
        }

        windowContent.addChild(label);
        windowContent.addChild(new NVSpacer());

        windowContent.addChild(confirmButton);
        windowContent.addChild(cancelButton);
        windowContent.addChild(new NVSpacer());
        windowContent.addChild(removeButton);
    }
}
