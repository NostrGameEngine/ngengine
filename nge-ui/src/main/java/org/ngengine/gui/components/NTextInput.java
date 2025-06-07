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
package org.ngengine.gui.components;

import com.simsilica.lemur.Axis;
import com.simsilica.lemur.Container;
import com.simsilica.lemur.FillMode;
import com.simsilica.lemur.HAlignment;
import com.simsilica.lemur.Label;
import com.simsilica.lemur.PasswordField;
import com.simsilica.lemur.TextField;
import com.simsilica.lemur.VAlignment;
import com.simsilica.lemur.component.BorderLayout;
import com.simsilica.lemur.component.DynamicInsetsComponent;
import com.simsilica.lemur.component.SpringGridLayout;
import com.simsilica.lemur.core.GuiControl;
import com.simsilica.lemur.core.GuiUpdateListener;
import com.simsilica.lemur.style.ElementId;
import com.simsilica.lemur.style.StyleAttribute;
import java.util.function.Consumer;
import java.util.function.Supplier;
import org.ngengine.platform.NGEPlatform;

public class NTextInput extends Container implements GuiUpdateListener {

    public static final String ELEMENT_ID = "advInputField";
    protected boolean show = false;
    protected TextField inputField;
    protected String text;
    protected NIconButton showBtn;
    protected NIconButton copyBtn;
    protected NIconButton pasteBtn;
    protected NIconButton generateBtn;
    protected boolean secret = false;

    protected VAlignment textVAlignment = VAlignment.Center;
    protected HAlignment textHAlignment = HAlignment.Left;
    protected Float preferredWidth;

    protected Container rightIconContainer;
    protected Container leftIconContainer;

    protected String label;
    protected Label labelComponent;

    protected Consumer<String> onTextChangeAction;
    protected String lastText = "";
    protected float textChangeDelay = 1f;
    protected boolean enabled = true;
    protected boolean singleLine = false;

    public NTextInput() {
        super(new BorderLayout(), new ElementId(ELEMENT_ID));
        Container iconContainer = new Container(new BorderLayout(), new ElementId(ELEMENT_ID).child("iconContainer"));
        addChild(iconContainer, BorderLayout.Position.South);

        rightIconContainer =
            new Container(
                new SpringGridLayout(Axis.X, Axis.Y, FillMode.None, FillMode.None),
                new ElementId(ELEMENT_ID).child("right").child("container")
            );
        rightIconContainer.setInsetsComponent(new DynamicInsetsComponent(.5f, 1f, .5f, 0f));
        iconContainer.addChild(rightIconContainer, BorderLayout.Position.East);

        leftIconContainer =
            new Container(
                new SpringGridLayout(Axis.X, Axis.Y, FillMode.None, FillMode.None),
                new ElementId(ELEMENT_ID).child("left").child("container")
            );
        leftIconContainer.setInsetsComponent(new DynamicInsetsComponent(.5f, 1f, .5f, 0f));
        iconContainer.addChild(leftIconContainer, BorderLayout.Position.West);

        setCopyAction(src -> {
            NGEPlatform platform = NGEPlatform.get();
            platform.setClipboardContent(src);
        });
        setPasteAction(() -> {
            NGEPlatform platform = NGEPlatform.get();
            String text = platform.getClipboardContent();
            setText(text);
            return text;
        });
        getControl(GuiControl.class).addUpdateListener(this);
        repaint();
    }

    public float getFontSize() {
        return inputField.getFontSize();
    }

    public void setSingleLine(boolean singleLine) {
        this.singleLine = singleLine;
        repaint();
    }

    public void setEnabled(boolean v) {
        // TODO
        // if(inputField!=null)inputField.setEnabled(v);
        if (showBtn != null) showBtn.setEnabled(v);
        if (copyBtn != null) copyBtn.setEnabled(v);
        if (pasteBtn != null) pasteBtn.setEnabled(v);
        if (generateBtn != null) generateBtn.setEnabled(v);
        enabled = v;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setTextChangeAction(Consumer<String> action) {
        this.onTextChangeAction = action;
    }

    @Override
    public void guiUpdate(GuiControl source, float tpf) {
        if (onTextChangeAction != null) {
            textChangeDelay -= tpf;
            if (textChangeDelay <= 0) {
                textChangeDelay = 1f;
                String currentText = getText();
                if (!currentText.equals(lastText)) {
                    lastText = currentText;
                    onTextChangeAction.accept(currentText);
                }
            }
        }
    }

    public void setLabel(String text) {
        this.label = text;
        repaint();
    }

    public Container getLeft() {
        return leftIconContainer;
    }

    public void setPreferredWidth(Float width) {
        preferredWidth = width;
        repaint();
    }

    public boolean isSecretInput() {
        return secret;
    }

    public void setIsSecretInput(boolean secret) {
        this.secret = secret;
        repaint();
    }

    public void setText(String text) {
        if (text == null) text = "";
        inputField.setText(text);
        getText();
    }

    public String getText() {
        text = inputField.getText();
        return text;
    }

    public void setCopyAction(Consumer<String> action) {
        if (action == null) {
            if (copyBtn != null) {
                copyBtn.removeFromParent();
                copyBtn = null;
            }
            return;
        }
        copyBtn = new NIconButton("icons/outline/copy.svg");
        copyBtn.addClickCommands(src -> {
            action.accept(getText());
        });
        repaint();
    }

    public void setPasteAction(Supplier<String> action) {
        if (action == null) {
            if (pasteBtn != null) {
                pasteBtn.removeFromParent();
                pasteBtn = null;
            }
            return;
        }
        pasteBtn = new NIconButton("icons/outline/clipboard.svg");
        pasteBtn.addClickCommands(src -> {
            String text = action.get();
            setText(text);
        });
        repaint();
    }

    public void setGenerateAction(Supplier<String> action) {
        if (action == null) {
            if (generateBtn != null) {
                generateBtn.removeFromParent();
                generateBtn = null;
            }
            return;
        }
        generateBtn = new NIconButton("icons/outline/dice.svg");
        generateBtn.addClickCommands(src -> {
            setText(action.get());
        });
        repaint();
    }

    @StyleAttribute(value = "textHAlignment", lookupDefault = false)
    public void setTextVAlignment(VAlignment alignment) {
        inputField.setTextVAlignment(alignment);
    }

    @StyleAttribute(value = "textHAlignment", lookupDefault = false)
    public void setTextHAlignment(HAlignment alignment) {
        inputField.setTextHAlignment(alignment);
    }

    protected void repaint() {
        if (labelComponent != null) {
            labelComponent.removeFromParent();
        }
        if (label != null) {
            labelComponent = new Label(label);
            addChild(labelComponent, BorderLayout.Position.North);
        }
        if (inputField != null) inputField.removeFromParent();

        inputField = !secret || show ? new TextField(text) : new PasswordField(text);
        inputField.setTextHAlignment(textHAlignment);
        inputField.setTextVAlignment(textVAlignment);
        inputField.setSingleLine(singleLine);
        if (preferredWidth != null) inputField.setPreferredWidth(preferredWidth);
        addChild(inputField, BorderLayout.Position.Center);

        if (secret) {
            if (showBtn != null) {
                showBtn.removeFromParent();
            }
            showBtn = new NIconButton(!show ? "icons/outline/eye.svg" : "icons/outline/eye-off.svg");
            showBtn.addClickCommands(src -> {
                show = !show;
                getText();
                repaint();
            });
            rightIconContainer.addChild(showBtn);
        }

        if (generateBtn != null) {
            rightIconContainer.addChild(generateBtn);
        }

        if (pasteBtn != null) {
            rightIconContainer.addChild(pasteBtn);
        }

        if (copyBtn != null) {
            rightIconContainer.addChild(copyBtn);
        }
    }
}
