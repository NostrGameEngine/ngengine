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

package org.ngengine.auth.nip07;

import com.jme3.math.Vector3f;

import java.util.logging.Logger;
import org.ngengine.auth.AuthConfig;
import org.ngengine.gui.HAlignment;
import org.ngengine.gui.Label;
import org.ngengine.gui.guix.NLoadingSpinner;
import org.ngengine.gui.guix.containers.NColumn;
import org.ngengine.gui.guix.win.NWindow;
import org.ngengine.nostr4j.nip46.NostrconnectUrl;
import org.ngengine.nostr4j.signer.NostrNIP07Signer;

public class Nip07AuthWindow extends NWindow<AuthConfig> {

    private static final Logger logger = Logger.getLogger(Nip07AuthWindow.class.getName());
    protected NostrconnectUrl nostrConnectUrl = null;


    @Override
    protected void compose(Vector3f size, AuthConfig opt) throws Exception {     
        Nip07Auth auth = (Nip07Auth) opt.getAuth();
        NostrNIP07Signer signer = auth.getSigner();

        setTitle("Authenticating");
        getContent().clearChildren();
        NColumn content = getContent().addCol();

        Label label = new Label("Confirm with NIP-07 extension");
        label.setTextHAlignment(HAlignment.Center);
        content.addChild(label);

        NLoadingSpinner spinner = new NLoadingSpinner();
        float dim = Math.min(size.x, size.y);
        dim = dim * 0.01f;
        if (dim < 32) dim = 32;
        spinner.setPreferredSize(new Vector3f(dim, dim, 0));
        content.addChild(spinner);


        signer.getPublicKey().then(pk->{
            opt.getStrategy().getCallback().accept(signer);
            close();
            return null;
        }).catchException(ex->{
            getManager().showToast(new Exception("Unable to authenticate with NIP-07 extension: "+ex.getMessage(), ex));
            close();
        });
        
    }


}
