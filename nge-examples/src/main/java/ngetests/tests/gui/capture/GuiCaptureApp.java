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

import com.jme3.system.AppSettings;
import com.jme3.system.JmeCanvasContext;
import java.awt.Canvas;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import org.ngengine.Components;
import org.ngengine.NGEApplication;
import org.ngengine.NGEApplication.NGEAppRunner;
import org.ngengine.config.NGEAppSettings;
import org.ngengine.gui.guix.win.NWindowManagerComponent;
import org.ngengine.platform.NGEPlatform;
import org.ngengine.platform.jvm.JVMAsyncPlatform;

public final class GuiCaptureApp {

    private GuiCaptureApp() {}

    public static void main(String[] args) {
        NGEPlatform.set(new JVMAsyncPlatform());

        String scenario = System.getProperty("nge.capture.scenario", "showcase");
        boolean relative = Boolean.getBoolean("nge.capture.relative");
        boolean awt = Boolean.getBoolean("nge.capture.awt");
        int width = Integer.getInteger("nge.capture.width", 1280);
        int height = Integer.getInteger("nge.capture.height", 720);
        String title = "NGE GUI Capture - " + scenario + " - " + (relative ? "responsive" : "legacy");

        NGEAppSettings settings = new NGEAppSettings();
        settings.setInt("Width", width);
        settings.setInt("Height", height);
        String renderer = System.getProperty("nge.capture.renderer");
        if (renderer != null && !renderer.isBlank()) {
            settings.put("Renderer", renderer);
        } else if (awt) {
            settings.put("Renderer", AppSettings.LWJGL_OPENGL32);
        }
        settings.setString("Title", title);

        NGEAppRunner runner = NGEApplication.createApp(settings, app -> {
            NWindowManagerComponent win = new NWindowManagerComponent(relative);
            Components.mount(app, win).enable();
            win.setInteractionEnabled(true);
            Components.mount(app, new GuiCaptureComponent(scenario)).enable();
        });
        if (awt) {
            startAwt(runner.app(), title, width, height);
        } else {
            runner.run();
        }
    }

    private static void startAwt(NGEApplication app, String title, int width, int height) {
        System.setProperty("jme.awt.forceCoreProfile", "true");

        NGEApplication.Jme3Application jme = app.getJme3App();
        jme.createCanvas();

        JmeCanvasContext context = (JmeCanvasContext) jme.getContext();
        Canvas canvas = context.getCanvas();
        canvas.setSize(width, height);

        try {
            SwingUtilities.invokeAndWait(() -> {
                JFrame frame = new JFrame(title);
                frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
                frame.addWindowListener(new WindowAdapter() {
                    @Override
                    public void windowClosed(WindowEvent e) {
                        jme.stop();
                    }
                });
                frame.add(canvas);
                frame.pack();
                frame.setSize(width, height);
                frame.setLocationRelativeTo(null);
                frame.setAlwaysOnTop(true);
                frame.setVisible(true);
                frame.toFront();
                frame.requestFocus();
                System.out.println("NGE_CAPTURE_AWT_FRAME_VISIBLE " + title);
            });
        } catch (Exception e) {
            throw new RuntimeException("Unable to create GUI capture frame", e);
        }

        jme.startCanvas();
    }
}
