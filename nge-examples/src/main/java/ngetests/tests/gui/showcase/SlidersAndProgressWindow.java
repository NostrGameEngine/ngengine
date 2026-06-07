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
import org.ngengine.gui.Axis;
import org.ngengine.gui.DefaultRangedValueModel;
import org.ngengine.gui.ProgressBar;
import org.ngengine.gui.Slider;
import org.ngengine.gui.guix.NButton;
import org.ngengine.gui.guix.NLabel;
import org.ngengine.gui.guix.NVSpacer;
import org.ngengine.gui.guix.containers.NColumn;
import org.ngengine.gui.guix.containers.NPanel;
import org.ngengine.gui.guix.containers.NRow;
import org.ngengine.gui.guix.win.NWindow;

/**
 * Demonstrates horizontal and vertical sliders linked to progress bars.
 */
public class SlidersAndProgressWindow extends NWindow<Void> {

    @Override
    protected void compose(Vector3f size, Void args) throws Throwable {
        setTitle("Sliders & Progress Bars");

        NPanel content = getContent();

        // --- Horizontal slider linked to a progress bar ---
        content.addChild(new NLabel("--- Horizontal Slider → Progress Bar ---"));

        DefaultRangedValueModel hModel = new DefaultRangedValueModel(0, 100, 40);

        Slider hSlider = new Slider(hModel, Axis.X);

        ProgressBar hProgress = new ProgressBar(hModel);
        NLabel hValueLabel = new NLabel("Value: 40");

        // Poll the model value each time the slider is interacted with.
        // We use a click listener on a sibling refresh button to keep things
        // simple (no per-frame polling in compose()).
        NButton hRefresh = new NButton("Read value");
        hRefresh.addClickCommands(b -> {
            int val = (int) hModel.getValue();
            hValueLabel.setText("Value: " + val);
            hProgress.setProgressValue(val);
        });

        content.addChild(hSlider);
        content.addChild(hProgress);
        NRow hRow = content.addRow();
        hRow.addChild(hValueLabel);
        hRow.addChild(hRefresh);

        content.addChild(new NVSpacer());

        // --- Manual progress controls ---
        content.addChild(new NLabel("--- Manual Progress Controls ---"));

        DefaultRangedValueModel manualModel = new DefaultRangedValueModel(0, 100, 0);
        ProgressBar manualBar = new ProgressBar(manualModel);
        NLabel manualLabel = new NLabel("Progress: 0 %");

        NRow manualRow = content.addRow();
        NButton stepBtn = new NButton("+10 %");
        NButton resetBtn = new NButton("Reset");

        stepBtn.addClickCommands(b -> {
            double next = Math.min(manualModel.getValue() + 10, manualModel.getMaximum());
            manualModel.setValue(next);
            manualLabel.setText("Progress: " + (int) next + " %");
        });
        resetBtn.addClickCommands(b -> {
            manualModel.setValue(0);
            manualLabel.setText("Progress: 0 %");
        });

        manualRow.addChild(stepBtn);
        manualRow.addChild(resetBtn);
        content.addChild(manualBar);
        content.addChild(manualLabel);

        content.addChild(new NVSpacer());

        // --- Vertical slider (read-only display) ---
        content.addChild(new NLabel("--- Vertical Slider ---"));

        DefaultRangedValueModel vModel = new DefaultRangedValueModel(0, 10, 5);
        Slider vSlider = new Slider(vModel, Axis.Y);
        NLabel vLabel = new NLabel("V-value: 5");

        NButton vRead = new NButton("Read");
        vRead.addClickCommands(b -> vLabel.setText("V-value: " + (int) vModel.getValue()));

        NRow vRow = content.addRow();
        vRow.addChild(vSlider);
        NColumn vCol = vRow.addCol();
        vCol.addChild(vLabel);
        vCol.addChild(vRead);

        content.addChild(new NVSpacer());

        NButton back = new NButton("Back to Launcher");
        back.addClickCommands(b -> closeAndShowPrevious());
        content.addChild(back);
    }
}
