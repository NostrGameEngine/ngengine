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

package org.ngengine.gui.win;

import com.simsilica.lemur.Container;
import com.simsilica.lemur.Label;
import com.simsilica.lemur.component.BorderLayout;
import com.simsilica.lemur.component.DynamicInsetsComponent;
import com.simsilica.lemur.style.ElementId;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;
import org.ngengine.gui.components.NIconButton;

public class NToast extends Container {

    public static final String ELEMENT_ID = "toast";

    public enum ToastType {
        INFO,
        WARNING,
        ERROR,
    }

    protected ToastType type;
    protected Label message;
    protected Duration duration;
    protected Instant creationTime;
    protected NWindowManager appState;
    protected boolean closeable = false;
    protected NIconButton closeBtn;
    protected List<Runnable> closeListeners = new CopyOnWriteArrayList<>();
    protected List<Consumer<Integer>> actionListeners = new CopyOnWriteArrayList<>();

    protected NToast(ToastType type, String message, Duration duration) {
        super(new BorderLayout(), new ElementId(type.name().toLowerCase() + "." + ELEMENT_ID));
        this.type = type;
        this.message = new Label(message, new ElementId(type.name().toLowerCase() + "." + ELEMENT_ID + ".label"));
        addChild(this.message, BorderLayout.Position.Center);

        NIconButton icon = new NIconButton(
            "icons/outline/activity.svg",
            type.name().toLowerCase() + "." + "toast." + NIconButton.ELEMENT_ID
        );

        addChild(icon, BorderLayout.Position.West);

        closeBtn =
            new NIconButton("icons/outline/x.svg", type.name().toLowerCase() + "." + "toast.close." + NIconButton.ELEMENT_ID);
        closeBtn.setInsetsComponent(new DynamicInsetsComponent(0, 1f, 1f, 0f));
        creationTime = Instant.now();

        setCloseable(false);
        setDuration(duration);
        setCloseAction(() -> {
            close();
        });
    }

    public void addCloseListener(Runnable listener) {
        closeListeners.add(listener);
    }

    public void setCloseable(boolean closeable) {
        closeBtn.removeFromParent();
        this.closeable = closeable;
        if (closeable) {
            addChild(closeBtn, BorderLayout.Position.East);
        }
    }

    public boolean isCloseable() {
        return closeable;
    }

    public void setCloseAction(Runnable action) {
        closeBtn.addClickCommands(src -> {
            if (action != null) {
                action.run();
            }
        });
    }

    public void close() {
        if (appState != null) {
            appState.closeToast(this);
        } else {
            removeFromParent();
        }
        for (Runnable listener : closeListeners) {
            listener.run();
        }
    }

    public boolean isClosed() {
        return getParent() == null;
    }

    protected void initialize(NWindowManager appState) {
        this.appState = appState;
    }

    protected NWindowManager getManager() {
        return appState;
    }

    public Instant getCreationTime() {
        return creationTime;
    }

    public void setDuration(Duration duration) {
        this.duration = duration;
        if (!closeable && duration == null) {
            setCloseable(true);
        }
    }

    public Duration getDuration() {
        return duration;
    }

    public NToast() {
        this(ToastType.INFO, "", Duration.ofSeconds(5));
    }

    public void setType(ToastType type) {
        this.type = type;
    }

    public void setMessage(String message) {
        this.message.setText(message);
    }

    public ToastType getType() {
        return type;
    }

    public String getMessage() {
        return message.getText();
    }

    public void onAction(int id) {
        actionListeners.forEach(l -> l.accept(id));
    }

    public void addActionListener(Consumer<Integer> listener) {
        actionListeners.add(listener);
    }
}
