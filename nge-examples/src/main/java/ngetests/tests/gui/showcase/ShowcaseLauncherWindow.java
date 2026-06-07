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
import org.ngengine.gui.guix.NButton;
import org.ngengine.gui.guix.NLabel;
import org.ngengine.gui.guix.NVSpacer;
import org.ngengine.gui.guix.containers.NColumn;
import org.ngengine.gui.guix.containers.NPanel;
import org.ngengine.gui.guix.containers.NRow;
import org.ngengine.gui.guix.win.NWindow;

/**
 * Main launcher: shows a menu of demo sections, each opening a dedicated window.
 */
public class ShowcaseLauncherWindow extends NWindow<Void> {

    @Override
    protected void compose(Vector3f size, Void args) throws Throwable {
        setTitle("NGE GUI Showcase");

        NPanel content = getContent();

        content.addChild(new NLabel("Choose a section to explore:"));
        content.addChild(new NVSpacer());

        NColumn col = content.addCol();

        NButton btnButtons = new NButton("Buttons & Checkboxes");
        btnButtons.addClickCommands(b -> getManager().showWindow(ButtonsAndCheckboxesWindow.class));
        col.addChild(btnButtons);

        NButton btnSliders = new NButton("Sliders & Progress Bars");
        btnSliders.addClickCommands(b -> getManager().showWindow(SlidersAndProgressWindow.class));
        col.addChild(btnSliders);

        NButton btnText = new NButton("Text Inputs");
        btnText.addClickCommands(b -> getManager().showWindow(TextInputsWindow.class));
        col.addChild(btnText);

        NButton btnLists = new NButton("Lists & Tabbed Panels");
        btnLists.addClickCommands(b -> getManager().showWindow(ListsAndTabsWindow.class));
        col.addChild(btnLists);

        NButton btnDialogs = new NButton("Toasts & Dialogs");
        btnDialogs.addClickCommands(b -> getManager().showWindow(DialogsWindow.class));
        col.addChild(btnDialogs);

        content.addChild(new NVSpacer());

        NRow footer = content.addRow();
        NButton quit = new NButton("Quit");
        quit.addClickCommands(b -> System.exit(0));
        footer.addChild(quit);
    }
}
