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
 */

package ngetests.tests.gui.capture;

import com.jme3.math.ColorRGBA;
import java.time.Duration;
import java.util.List;
import ngetests.tests.gui.MainWindow;
import ngetests.tests.gui.showcase.ButtonsAndCheckboxesWindow;
import ngetests.tests.gui.showcase.DialogsWindow;
import ngetests.tests.gui.showcase.ListsAndTabsWindow;
import ngetests.tests.gui.showcase.ShowcaseLauncherWindow;
import ngetests.tests.gui.showcase.SlidersAndProgressWindow;
import ngetests.tests.gui.showcase.TextInputsWindow;
import org.ngengine.ViewPortManager;
import org.ngengine.auth.AuthConfig;
import org.ngengine.auth.AuthSelectionWindow;
import org.ngengine.auth.AuthStrategy;
import org.ngengine.auth.Nip46AuthStrategy;
import org.ngengine.auth.nip46.Nip46Auth;
import org.ngengine.auth.nip46.Nip46AuthWindow;
import org.ngengine.auth.nip46.Nip46ChallengeWindow;
import org.ngengine.auth.nsec.NsecAuth;
import org.ngengine.auth.nsec.NsecAuthWindow;
import org.ngengine.auth.stored.StoredAuthSelectionOptions;
import org.ngengine.auth.stored.StoredAuthSelectionWindow;
import org.ngengine.components.AbstractComponent;
import org.ngengine.components.ComponentManager;
import org.ngengine.gui.GuiContext;
import org.ngengine.gui.guix.win.NConfirmDialogOptions;
import org.ngengine.gui.guix.win.NConfirmDialogWindow;
import org.ngengine.gui.guix.win.NErrorWindow;
import org.ngengine.gui.guix.win.NHud;
import org.ngengine.gui.guix.win.NToast.ToastType;
import org.ngengine.gui.guix.win.NWindowManagerComponent;
import org.ngengine.gui.nav.Navigator;

public class GuiCaptureComponent extends AbstractComponent {

    private final String scenario;

    public GuiCaptureComponent(String scenario) {
        this.scenario = scenario;
    }

    @Override
    protected void onEnable(ComponentManager mng, boolean firstTime) {
        getInstanceOf(ViewPortManager.class).getMainSceneViewPort()
                .setBackgroundColor(new ColorRGBA(0.34f, 0.37f, 0.41f, 1f));

        NWindowManagerComponent win = getInstanceOf(NWindowManagerComponent.class);
        showScenario(win);
        placeSoftwareCursor(win);
    }

    @Override
    protected void onDisable(ComponentManager mng) {}

    private void showScenario(NWindowManagerComponent win) {
        switch (scenario) {
            case "showcase":
                win.showWindow(ShowcaseLauncherWindow.class);
                break;
            case "buttons":
                win.showWindow(ButtonsAndCheckboxesWindow.class);
                break;
            case "sliders":
                win.showWindow(SlidersAndProgressWindow.class);
                break;
            case "text":
                win.showWindow(TextInputsWindow.class);
                break;
            case "lists":
                win.showWindow(ListsAndTabsWindow.class);
                break;
            case "dialogs":
                win.showWindow(DialogsWindow.class);
                break;
            case "main":
                win.showWindow(MainWindow.class);
                break;
            case "hud":
                win.showWindow(NHud.class);
                break;
            case "toast":
                win.showWindow(NHud.class);
                win.showToast(ToastType.INFO, "Info toast from the capture harness", Duration.ofSeconds(30));
                win.showToast(ToastType.WARNING, "Warning toast from the capture harness", Duration.ofSeconds(30));
                win.showToast(ToastType.ERROR, "Error toast from the capture harness", Duration.ofSeconds(30));
                break;
            case "confirm":
                win.showWindow(NConfirmDialogWindow.class, new NConfirmDialogOptions()
                        .setText("Apply this responsive layout change?")
                        .setConfirmButtonText("Apply")
                        .setCancelButtonText("Cancel")
                        .setConfirmAction(dialog -> {})
                        .setCancelAction(dialog -> {}));
                break;
            case "error":
                win.showWindow(NErrorWindow.class, sampleError());
                break;
            case "auth-selection":
                win.showWindow(AuthSelectionWindow.class, captureAuthStrategy());
                break;
            case "nsec-auth": {
                AuthStrategy strategy = captureAuthStrategy();
                AuthConfig config = new AuthConfig(strategy);
                config.setAuth(new NsecAuth(strategy));
                win.showWindow(NsecAuthWindow.class, config);
                break;
            }
            case "nip46-auth": {
                AuthStrategy strategy = captureAuthStrategy();
                AuthConfig config = new AuthConfig(strategy);
                config.setAuth(new Nip46Auth(strategy));
                win.showWindow(Nip46AuthWindow.class, config);
                break;
            }
            case "nip46-challenge":
                win.showWindow(Nip46ChallengeWindow.class,
                        new String[] { "auth_url", "https://example.com/nip46/challenge" });
                break;
            case "stored-auth":
                win.showWindow(StoredAuthSelectionWindow.class,
                        new StoredAuthSelectionOptions("Example identity", null)
                                .setConfirmAction(dialog -> {})
                                .setCancelAction(dialog -> {})
                                .setRemoveAction(dialog -> {}));
                break;
            default:
                throw new IllegalArgumentException("Unknown GUI capture scenario: " + scenario);
        }
    }

    private AuthStrategy captureAuthStrategy() {
        return new AuthStrategy(signer -> {})
                .disableStore()
                .disableNip07Identity()
                .enableLocalIdentity()
                .enableNip46RemoteIdentity(new Nip46AuthStrategy(List.of("wss://relay.example"))
                        .withoutNostrConnectFlow()
                        .withBunkerFlow());
    }

    private Throwable sampleError() {
        RuntimeException error = new RuntimeException("Example engine error used for GUI capture.");
        error.setStackTrace(new StackTraceElement[] {
                new StackTraceElement("org.ngengine.example.RenderLoop", "update", "RenderLoop.java", 42),
                new StackTraceElement("org.ngengine.example.WindowSystem", "compose", "WindowSystem.java", 77),
                new StackTraceElement("ngetests.tests.gui.capture.GuiCaptureComponent", "showScenario",
                        "GuiCaptureComponent.java", 123)
        });
        return error;
    }

    private void placeSoftwareCursor(NWindowManagerComponent win) {
        GuiContext context = win.getManager(null).getContext();
        Navigator navigator = context.getNavigator();
        navigator.setHardwareCursor(false);
        navigator.setCursorAutoHideDelay(-1f);
        navigator.setCursor(true);

        double x = context.toGuiX(context.getPhysicalWidth() - 72);
        double y = context.toGuiY(72);
        navigator.updateCursorPosition(x, y);
    }
}
