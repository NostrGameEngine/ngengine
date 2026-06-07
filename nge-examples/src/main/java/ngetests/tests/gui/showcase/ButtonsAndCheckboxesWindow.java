/**
 * Copyright (c) 2025-2026, Nostr Game Engine
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

package ngetests.tests.gui.showcase;

import com.jme3.math.Vector3f;
import org.ngengine.gui.Checkbox;
import org.ngengine.gui.CheckboxModelGroup;
import org.ngengine.gui.DefaultCheckboxModel;
import org.ngengine.gui.guix.NButton;
import org.ngengine.gui.guix.NLabel;
import org.ngengine.gui.guix.NVSpacer;
import org.ngengine.gui.guix.containers.NColumn;
import org.ngengine.gui.guix.containers.NPanel;
import org.ngengine.gui.guix.containers.NRow;
import org.ngengine.gui.guix.win.NWindow;

/**
 * Demonstrates NButton (normal, disabled) and Checkbox (standalone + grouped radio-style).
 */
public class ButtonsAndCheckboxesWindow extends NWindow<Void> {

    @Override
    protected void compose(Vector3f size, Void args) throws Throwable {
        setTitle("Buttons & Checkboxes");

        NPanel content = getContent();

        // --- Buttons section ---
        content.addChild(new NLabel("--- Buttons ---"));

        NRow row1 = content.addRow();
        NLabel clickLabel = new NLabel("(click a button)");

        NButton normal = new NButton("Normal Button");
        normal.addClickCommands(b -> clickLabel.setText("Normal clicked"));

        NButton disabled = new NButton("Disabled Button");
        disabled.setEnabled(false);

        NButton toggle = new NButton("Toggle me");
        // track state locally to show toggling text
        boolean[] on = {false};
        toggle.addClickCommands(b -> {
            on[0] = !on[0];
            toggle.setText(on[0] ? "ON" : "OFF");
            clickLabel.setText("Toggle is now: " + (on[0] ? "ON" : "OFF"));
        });

        row1.addChild(normal);
        row1.addChild(disabled);
        row1.addChild(toggle);
        content.addChild(clickLabel);

        content.addChild(new NVSpacer());

        // --- Independent checkboxes ---
        content.addChild(new NLabel("--- Independent Checkboxes ---"));

        DefaultCheckboxModel modelA = new DefaultCheckboxModel(false);
        DefaultCheckboxModel modelB = new DefaultCheckboxModel(true);
        DefaultCheckboxModel modelC = new DefaultCheckboxModel(false);

        NLabel cbStatus = new NLabel("(check some boxes)");

        Checkbox cbA = new Checkbox("Option A", modelA);
        Checkbox cbB = new Checkbox("Option B (default on)", modelB);
        Checkbox cbC = new Checkbox("Option C", modelC);

        Runnable refreshCbStatus = () -> {
            cbStatus.setText("A=" + modelA.isChecked() + "  B=" + modelB.isChecked() + "  C=" + modelC.isChecked());
        };

        cbA.addClickCommands(b -> refreshCbStatus.run());
        cbB.addClickCommands(b -> refreshCbStatus.run());
        cbC.addClickCommands(b -> refreshCbStatus.run());

        NColumn cbCol = content.addCol();
        cbCol.addChild(cbA);
        cbCol.addChild(cbB);
        cbCol.addChild(cbC);
        content.addChild(cbStatus);

        content.addChild(new NVSpacer());

        // --- Radio-style group (CheckboxModelGroup allows only one selected) ---
        content.addChild(new NLabel("--- Radio Group (mutually exclusive) ---"));

        CheckboxModelGroup radioGroup = new CheckboxModelGroup();
        DefaultCheckboxModel radioRed = new DefaultCheckboxModel(true);
        DefaultCheckboxModel radioGreen = new DefaultCheckboxModel(false);
        DefaultCheckboxModel radioBlue = new DefaultCheckboxModel(false);
        radioGroup.addModel(radioRed);
        radioGroup.addModel(radioGreen);
        radioGroup.addModel(radioBlue);

        NLabel radioStatus = new NLabel("Selected: Red");

        Checkbox radRed   = new Checkbox("Red",   radioRed);
        Checkbox radGreen = new Checkbox("Green", radioGreen);
        Checkbox radBlue  = new Checkbox("Blue",  radioBlue);

        Runnable refreshRadio = () -> {
            String sel = radioRed.isChecked() ? "Red" : radioGreen.isChecked() ? "Green" : "Blue";
            radioStatus.setText("Selected: " + sel);
        };

        radRed.addClickCommands(b -> {
            radioGroup.setSelectedModel(radioRed);
            refreshRadio.run();
        });
        radGreen.addClickCommands(b -> {
            radioGroup.setSelectedModel(radioGreen);
            refreshRadio.run();
        });
        radBlue.addClickCommands(b -> {
            radioGroup.setSelectedModel(radioBlue);
            refreshRadio.run();
        });

        NRow radioRow = content.addRow();
        radioRow.addChild(radRed);
        radioRow.addChild(radGreen);
        radioRow.addChild(radBlue);
        content.addChild(radioStatus);

        content.addChild(new NVSpacer());

        NButton back = new NButton("Back to Launcher");
        back.addClickCommands(b -> closeAndShowPrevious());
        content.addChild(back);
    }
}
