/**
 * Copyright (c) 2026, Nostr Game Engine
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
 * 
 * #########################################
 * 
 * nge-gui is built and based on Lemur, which is licensed under the BSD 3-Clause License.
 * - Copyright (c) 2012-2026 jMonkeyEngine All rights reserved. 
 * - Copyright (c) 2016-2026, Simsilica, LLC All rights reserved.
 * 
 * https://github.com/jMonkeyEngine-Contributions/Lemur
 */

package org.ngengine.gui.guix.win;

import com.jme3.math.Vector3f;
import org.ngengine.gui.component.BorderLayout;
import org.ngengine.gui.guix.NTextInput;
import org.ngengine.gui.guix.containers.NPanel;

import java.util.function.Consumer;

public class NErrorWindow extends NWindow<Throwable> {

   

    @Override
    protected void compose(Vector3f size, Throwable args) throws Exception {
        String title = "Error: " + args.getClass().getSimpleName();
        setTitle(title);
        setFitContent(false);

        NPanel content = getContent();

        NTextInput errorLogField = new NTextInput();

        StringBuilder errorLog = new StringBuilder();
        Consumer<String> append = str -> {
            // split lines longer than size.x/2;
            int charsPerLine = (int) ((size.x * 1.8f) / errorLogField.getFontSize());
            if (str.length() > charsPerLine) {
                int start = 0;
                while (start < str.length()) {
                    int end = Math.min(start + charsPerLine, str.length());
                    errorLog.append(str, start, end).append("\n");
                    start = end;
                }
            } else {
                errorLog.append(str).append("\n");
            }
        };

        append.accept(args.getMessage());
        append.accept("  \n\n");

        for (StackTraceElement element : args.getStackTrace()) {
            append.accept("    " + element.toString());
            append.accept("\n");
        }
        errorLogField.setText(errorLog.toString());
        errorLogField.setSingleLine(false);
        // errorLogField.setTextVAlignment(VAlignment.Top);
        errorLogField.setPasteAction(null);
        content.addChild(errorLogField, BorderLayout.Position.Center);
    }
}
