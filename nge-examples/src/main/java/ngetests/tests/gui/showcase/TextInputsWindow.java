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
import org.ngengine.gui.guix.NTextInput;
import org.ngengine.gui.guix.NVSpacer;
import org.ngengine.gui.guix.containers.NColumn;
import org.ngengine.gui.guix.containers.NPanel;
import org.ngengine.gui.guix.containers.NRow;
import org.ngengine.gui.guix.win.NWindow;

/**
 * Demonstrates plain text inputs, secret (password) inputs, and dynamic label updates.
 */
public class TextInputsWindow extends NWindow<Void> {

    @Override
    protected void compose(Vector3f size, Void args) throws Throwable {
        setTitle("Text Inputs");

        NPanel content = getContent();

        // --- Plain text input ---
        content.addChild(new NLabel("--- Plain Text Input ---"));

        NRow row1 = content.addRow();
        NColumn labelCol1 = row1.addCol();
        NColumn inputCol1 = row1.addCol();
        labelCol1.addChild(new NLabel("Your name:"));
        NTextInput nameInput = new NTextInput();
        inputCol1.addChild(nameInput);

        NRow rowSingle = content.addRow();
        NColumn labelColSingle = rowSingle.addCol();
        NColumn inputColSingle = rowSingle.addCol();
        labelColSingle.addChild(new NLabel("Single line:"));
        NTextInput singleLineInput = new NTextInput();
        singleLineInput.setSingleLine(true);
        inputColSingle.addChild(singleLineInput);

        NRow rowMulti = content.addRow();
        NColumn labelColMulti = rowMulti.addCol();
        NColumn inputColMulti = rowMulti.addCol();
        labelColMulti.addChild(new NLabel("Multiline:"));
        NTextInput multilineInput = new NTextInput();
        multilineInput.setSingleLine(false);
        inputColMulti.addChild(multilineInput);

        NLabel greetLabel = new NLabel("(type a name and click Greet)");
        NButton greetBtn = new NButton("Greet");
        greetBtn.addClickCommands(b -> {
            String name = nameInput.getText();
            greetLabel.setText(name.isEmpty() ? "Hello, stranger!" : "Hello, " + name + "!");
        });

        NRow row2 = content.addRow();
        row2.addChild(greetLabel);
        row2.addChild(greetBtn);

        content.addChild(new NVSpacer());

        // --- Secret input ---
        content.addChild(new NLabel("--- Secret / Password Input ---"));

        NRow row3 = content.addRow();
        NColumn labelCol3 = row3.addCol();
        NColumn inputCol3 = row3.addCol();
        labelCol3.addChild(new NLabel("Password:"));
        NTextInput passwordInput = new NTextInput();
        passwordInput.setIsSecretInput(true);
        inputCol3.addChild(passwordInput);

        NLabel pwStatus = new NLabel("(enter a password and click Verify)");
        NButton verifyBtn = new NButton("Verify");
        verifyBtn.addClickCommands(b -> {
            String pw = passwordInput.getText();
            if (pw.isEmpty()) {
                pwStatus.setText("No password entered.");
            } else if (pw.length() < 6) {
                pwStatus.setText("Too short! (< 6 chars)");
            } else {
                pwStatus.setText("Password length OK: " + pw.length() + " chars");
            }
        });

        NRow row4 = content.addRow();
        row4.addChild(pwStatus);
        row4.addChild(verifyBtn);

        content.addChild(new NVSpacer());

        // --- Clear all ---
        NButton clearAll = new NButton("Clear All");
        clearAll.addClickCommands(b -> {
            nameInput.setText("");
            passwordInput.setText("");
            greetLabel.setText("(cleared)");
            pwStatus.setText("(cleared)");
        });
        content.addChild(clearAll);

        content.addChild(new NVSpacer());

        NButton back = new NButton("Back to Launcher");
        back.addClickCommands(b -> closeAndShowPrevious());
        content.addChild(back);
    }
}
