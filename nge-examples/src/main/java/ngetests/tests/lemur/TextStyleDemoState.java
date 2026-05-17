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
import org.ngengine.gui.ActionButton;
import org.ngengine.gui.CallMethodAction;
import org.ngengine.gui.Command;
import org.ngengine.gui.Container;
import org.ngengine.gui.Label;
import org.ngengine.gui.NGEGui;

import com.jme3.app.Application;
import com.jme3.app.state.BaseAppState;
import com.jme3.font.*;
import com.jme3.math.*;

import org.ngengine.gui.*;
import org.ngengine.gui.component.QuadBackgroundComponent;
import org.ngengine.gui.style.Attributes;
import org.ngengine.gui.style.ElementId;
import org.ngengine.gui.style.Styles;

/**
 *  A demo of text styling.
 *
 *  @author    Paul Speed
 */
public class TextStyleDemoState extends BaseAppState {

    private Container window;

    /**
     *  A command we'll pass to the label pop-up to let
     *  us know when the user clicks away.
     */
    private CloseCommand closeCommand = new CloseCommand();

    public TextStyleDemoState() {
    }

    @Override
    protected void initialize( Application app ) {
        // Using this as both a demo of different levels of styling as well as a
        // test of the BitmapFont compatibility versus new 'fontName' styling.
        Styles styles = NGEGui.getStyles();

        Attributes attrs;
        attrs = styles.getSelector("special1", "label", "glass");
        attrs.set("fontSize", 20);
        attrs.set("fontName", "Interface/Fonts/Console.fnt");
        attrs.set("shadowOffset", new Vector3f(2, -2, -1));
        attrs.set("color", new ColorRGBA(0.9f, 0.75f, 0.75f, 1f));

        attrs = styles.getSelector("special2", "label", "glass");
        attrs.set("fontSize", 20);
        attrs.set("shadowOffset", new Vector3f(2, -2, -1));
        BitmapFont font = NGEGui.loadFont("Interface/Fonts/Console.fnt");
        attrs.set("font", font);

        attrs = styles.getSelector("override.special2", "label", "glass");
        attrs.set("fontSize", 20);
        attrs.set("shadowOffset", new Vector3f(2, -2, -1));
        attrs.set("fontName", "Interface/Fonts/Console.fnt");
        attrs.set("color", new ColorRGBA(0.9f, 0.75f, 0.75f, 1f));
    }

    @Override
    protected void cleanup( Application app ) {
    }

    @Override
    protected void onEnable() {

        // We'll wrap the text in a window to make sure the layout is working
        window = new Container();
        window.setBackground(new QuadBackgroundComponent(ColorRGBA.DarkGray));
        window.addChild(new Label("Text Style Demo", new ElementId("window.title.label")));

        window.addChild(new Label("Default styling"));
        window.addChild(new Label("Different font name styling", new ElementId("special1.label")));
        window.addChild(new Label("Different bitmap font styling", new ElementId("special2.label")));
        window.addChild(new Label("Different override styling", new ElementId("override.special2.label")));

        window.addChild(new Label("Default styling, w/shadows"))
            .setShadowColor(ColorRGBA.Black);
        window.addChild(new Label("Different font name styling, w/shadows", new ElementId("special1.label")))
            .setShadowColor(ColorRGBA.Black);
        window.addChild(new Label("Different bitmap font styling, w/shadows", new ElementId("special2.label")))
            .setShadowColor(ColorRGBA.Black);
        window.addChild(new Label("Different override styling, w/shadows", new ElementId("override.special2.label")))
            .setShadowColor(ColorRGBA.Black);

        // Add a close button to both show that the layout is working and
        // also because it's better UX... even if the popup will close if
        // you click outside of it.
        window.addChild(new ActionButton(new CallMethodAction("Close",
                                                              window, "removeFromParent")));

        // Position the window and pop it up
        window.setLocalTranslation(400, getApplication().getCamera().getHeight() * 0.9f, 50);
       NGEGui.get(getApplication().getGuiViewPort()).getPopupHandler().showPopup(window, closeCommand);
    }

    @Override
    protected void onDisable() {
        window.removeFromParent();
    }

    private class CloseCommand implements Command<Object> {

        public void execute( Object src ) {
            getState(MainMenuState.class).closeChild(TextStyleDemoState.this);
        }
    }
}



