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

import java.time.Duration;

import com.jme3.math.Vector3f;
import org.ngengine.gui.guix.NButton;
import org.ngengine.gui.guix.NLabel;
import org.ngengine.gui.guix.NVSpacer;
import org.ngengine.gui.guix.containers.NColumn;
import org.ngengine.gui.guix.containers.NPanel;
import org.ngengine.gui.guix.containers.NRow;
import org.ngengine.gui.guix.win.NConfirmDialogOptions;
import org.ngengine.gui.guix.win.NConfirmDialogWindow;
import org.ngengine.gui.guix.win.NToast.ToastType;
import org.ngengine.gui.guix.win.NWindow;

/**
 * Demonstrates toasts (INFO / WARNING / ERROR), confirm dialogs, and the error window.
 */
public class DialogsWindow extends NWindow<Void> {

    @Override
    protected void compose(Vector3f size, Void args) throws Throwable {
        setTitle("Toasts & Dialogs");

        NPanel content = getContent();

        // --- Toasts ---
        content.addChild(new NLabel("--- Toasts (auto-dismiss after 3 s) ---"));

        NRow toastRow = content.addRow();

        NButton infoToast = new NButton("Info Toast");
        infoToast.addClickCommands(b ->
            getManager().showToast(ToastType.INFO, "This is an info message.", Duration.ofSeconds(3))
        );

        NButton warnToast = new NButton("Warning Toast");
        warnToast.addClickCommands(b ->
            getManager().showToast(ToastType.WARNING, "Something looks off!", Duration.ofSeconds(3))
        );

        NButton errorToast = new NButton("Error Toast");
        errorToast.addClickCommands(b ->
            getManager().showToast(ToastType.ERROR, "An error occurred!", Duration.ofSeconds(3))
        );

        toastRow.addChild(infoToast);
        toastRow.addChild(warnToast);
        toastRow.addChild(errorToast);

        content.addChild(new NVSpacer());

        // --- Confirm dialog ---
        content.addChild(new NLabel("--- Confirm Dialog ---"));

        NLabel confirmResult = new NLabel("(no choice made yet)");

        NButton showConfirm = new NButton("Show Confirm Dialog");
        showConfirm.addClickCommands(b -> {
            NConfirmDialogOptions opts = new NConfirmDialogOptions()
                .setText("Do you want to proceed?")
                .setConfirmButtonText("Yes, proceed")
                .setCancelButtonText("No, cancel")
                .setConfirmAction(win -> {
                    confirmResult.setText("You chose: CONFIRM");
                    win.closeAndShowPrevious();
                })
                .setCancelAction(win -> {
                    confirmResult.setText("You chose: CANCEL");
                    win.closeAndShowPrevious();
                });
            getManager().showWindow(NConfirmDialogWindow.class, opts);
        });

        content.addChild(showConfirm);
        content.addChild(confirmResult);

        content.addChild(new NVSpacer());

        // --- Error window ---
        content.addChild(new NLabel("--- Error Window ---"));

        NButton showError = new NButton("Show Fatal Error Window");
        showError.addClickCommands(b ->
            getManager().showFatalError(new RuntimeException("Simulated fatal error from the showcase!"))
        );

        content.addChild(showError);

        content.addChild(new NVSpacer());

        NButton back = new NButton("Back to Launcher");
        back.addClickCommands(b -> closeAndShowPrevious());
        content.addChild(back);
    }
}
