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

package ngetests.tests.lemur;

import com.jme3.app.Application;
import com.jme3.app.state.BaseAppState;
import com.simsilica.lemur.ActionButton;
import com.simsilica.lemur.CallMethodAction;
import com.simsilica.lemur.Command;
import com.simsilica.lemur.Container;
import com.simsilica.lemur.Label;
import com.simsilica.lemur.ListBox;
import com.simsilica.lemur.NGEGui;
import com.simsilica.lemur.OptionPanelState;
import com.simsilica.lemur.Selector;
import com.simsilica.lemur.component.SpringGridLayout;
import com.simsilica.lemur.nav.PopupHandler;
import com.simsilica.lemur.style.ElementId;

public class SelectorDemoState extends BaseAppState {
    private Container window;

    public SelectorDemoState() {
    }

    protected void close() {
        getState(MainMenuState.class).closeChild(this);
        // ...which will also detach us
    }

    @Override
    protected void initialize(Application app) {
    }

    @Override
    protected void cleanup(Application app) {
    }

    @Override
    protected void onEnable() {

        window = new Container();
        window.addChild(new Label("Selector Demo", new ElementId("window.title.label")));

        Selector<String> selector = window.addChild(new Selector<String>());
        selector.getModel().add("Option 1");
        selector.getModel().add("Option 2");
        selector.getModel().add("Option 3");
      window.addChild(new ActionButton(new CallMethodAction("Close", 
                                                              window, "removeFromParent")));
        
        // Position the window and pop it up                                                             
        window.setLocalTranslation(400, 400, 100);     
       NGEGui.get(getApplication().getGuiViewPort()).getPopupHandler().showPopup(window, closeCommand);

    }

    @Override
    protected void onDisable() {
        window.removeFromParent();
    }

    /**
     * A command we'll pass to the label pop-up to let us know when the user clicks away.
     */
    private CloseCommand closeCommand = new CloseCommand();

    private class CloseCommand implements Command<Object> {

        public void execute(Object src) {
            getState(MainMenuState.class).closeChild(SelectorDemoState.this);
        }
    }
}
