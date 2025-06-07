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
 * the BSD 3-Clause License. The original jMonkeyEngine license is as follows:
 */
package org.ngengine.auth.nip46;

import com.jme3.math.Vector3f;
import com.simsilica.lemur.Button;
import com.simsilica.lemur.HAlignment;
import com.simsilica.lemur.Label;
import com.simsilica.lemur.VAlignment;
import java.util.logging.Logger;
import org.ngengine.gui.components.containers.NColumn;
import org.ngengine.gui.win.NToast.ToastType;
import org.ngengine.gui.win.NWindow;
import org.ngengine.platform.NGEPlatform;

public class Nip46ChallengeWindow extends NWindow<String[]> {

    private static final Logger log = Logger.getLogger(Nip46ChallengeWindow.class.getName());

    @Override
    protected void compose(Vector3f size, String[] args) throws Throwable {
        log.info("Composing Nip46ChallengeWindow with args: " + String.join(", ", args));
        String type = args[0];
        if (!type.equals("auth_url")) {
            throw new IllegalArgumentException("Unknown challenge type: " + type);
        }
        String challenge = args[1];
        setTitle("Resolve challenge");
        getContent().clearChildren();
        NColumn content = getContent().addCol();

        Label expl = new Label("Please complete the challenge on your remote signer");
        content.addChild(expl);

        Button open = new Button("Open");
        open.setTextHAlignment(HAlignment.Center);
        open.setTextVAlignment(VAlignment.Center);

        open.addClickCommands(b -> {
            try {
                NGEPlatform platform = NGEPlatform.get();
                platform.openInWebBrowser(challenge);
            } catch (Exception e) {
                getManager().showToast(e);
            }
        });
        content.addChild(open);

        Button copy = new Button("Copy link");
        copy.setTextHAlignment(HAlignment.Center);
        copy.setTextVAlignment(VAlignment.Center);

        copy.addClickCommands(b -> {
            try {
                NGEPlatform platform = NGEPlatform.get();
                platform.setClipboardContent(challenge);
                getManager().showToast(ToastType.INFO, "Link copied to clipboard");
            } catch (Exception e) {
                getManager().showToast(e);
            }
        });
        content.addChild(copy);
    }
}
