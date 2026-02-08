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
 * the BSD 3-Clause License. 
 */

package ngetests.tests.gui;
import org.ngengine.gui.components.NButton;
import org.ngengine.gui.components.NLabel;
import org.ngengine.gui.components.NTextInput;
import org.ngengine.gui.components.NVSpacer;
import org.ngengine.gui.components.containers.NColumn;
import org.ngengine.gui.components.containers.NPanel;
import org.ngengine.gui.components.containers.NRow;
import org.ngengine.gui.win.NWindow;

import com.jme3.math.Vector3f;
import com.simsilica.lemur.FillMode;

public class MainWindow extends NWindow<Object> {

    @Override
    protected void compose(Vector3f size, Object args) throws Throwable {
        setWithTitleBar(false);
        
        NPanel panel = getContent();
        
        NRow r = panel.addRow();
        NLabel label = new NLabel("Welcome to this demo");
        r.addChild(label);
        
        r = panel.addRow();
        NColumn c1 = r.addCol();
        NColumn c2 = r.addCol();

        label = new NLabel("Write something:");
        c1.addChild(label);

        
        
        NTextInput input = new NTextInput();
        c2.addChild(input);

        r = panel.addRow();
        r.addChild(new NVSpacer());

        NButton btn = new NButton("Click me!");
        r.addChild(btn);
        btn.addClickCommands((b)->{
            System.out.println("Button clicked! Input text: " + input.getText());
        });

    }
    
}
