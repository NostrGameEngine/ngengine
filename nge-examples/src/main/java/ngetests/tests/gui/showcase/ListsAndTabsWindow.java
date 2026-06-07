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
import org.ngengine.gui.ListBox;
import org.ngengine.gui.TabbedPanel;
import org.ngengine.gui.core.VersionedList;
import org.ngengine.gui.guix.NButton;
import org.ngengine.gui.guix.NLabel;
import org.ngengine.gui.guix.NVSpacer;
import org.ngengine.gui.guix.containers.NColumn;
import org.ngengine.gui.guix.containers.NPanel;
import org.ngengine.gui.guix.containers.NRow;
import org.ngengine.gui.guix.win.NWindow;

/**
 * Demonstrates ListBox (selectable items, add/remove) and TabbedPanel.
 */
public class ListsAndTabsWindow extends NWindow<Void> {

    @Override
    protected void compose(Vector3f size, Void args) throws Throwable {
        setTitle("Lists & Tabbed Panels");

        NPanel content = getContent();

        // ── Tabbed panel ──────────────────────────────────────────────────
        content.addChild(new NLabel("--- Tabbed Panel ---"));

        TabbedPanel tabs = new TabbedPanel();

        // Tab 1: fruit list
        NColumn tab1 = new NColumn();

        VersionedList<String> fruitModel = new VersionedList<>();
        fruitModel.add("Apple");
        fruitModel.add("Banana");
        fruitModel.add("Cherry");
        fruitModel.add("Durian");
        fruitModel.add("Elderberry");

        ListBox<String> fruitList = new ListBox<>(fruitModel);
        fruitList.setVisibleItems(4);
        NLabel fruitSel = new NLabel("(select a fruit)");
        fruitList.addCommands(ListBox.ListAction.Click, lb -> {
            String sel = fruitList.getSelectedItem();
            if (sel != null) fruitSel.setText("Selected: " + sel);
        });

        tab1.addChild(fruitList);
        tab1.addChild(fruitSel);
        tabs.addTab("Fruits", tab1);

        // Tab 2: dynamic add/remove list
        NColumn tab2 = new NColumn();

        VersionedList<String> dynModel = new VersionedList<>();
        dynModel.add("Item 1");
        dynModel.add("Item 2");
        dynModel.add("Item 3");

        ListBox<String> dynList = new ListBox<>(dynModel);
        dynList.setVisibleItems(4);
        NLabel dynStatus = new NLabel("Items: 3");

        int[] counter = {3};
        NButton addItem = new NButton("Add Item");
        addItem.addClickCommands(b -> {
            counter[0]++;
            dynModel.add("Item " + counter[0]);
            dynStatus.setText("Items: " + dynModel.size());
        });
        NButton removeItem = new NButton("Remove Selected");
        removeItem.addClickCommands(b -> {
            String sel = dynList.getSelectedItem();
            if (sel != null) {
                dynModel.remove(sel);
                dynStatus.setText("Items: " + dynModel.size());
            }
        });

        tab2.addChild(dynList);
        tab2.addChild(dynStatus);
        tab2.addChild(addItem);
        tab2.addChild(removeItem);
        tabs.addTab("Dynamic List", tab2);

        // Tab 3: plain info
        NColumn tab3 = new NColumn();
        tab3.addChild(new NLabel("This is the Info tab."));
        tab3.addChild(new NLabel("TabbedPanel lets you stack any Panel per tab."));
        tab3.addChild(new NLabel("Click the tab buttons above to switch."));
        tabs.addTab("Info", tab3);

        content.addChild(tabs);

        content.addChild(new NVSpacer());

        NButton back = new NButton("Back to Launcher");
        back.addClickCommands(b -> closeAndShowPrevious());
        content.addChild(back);
    }
}
