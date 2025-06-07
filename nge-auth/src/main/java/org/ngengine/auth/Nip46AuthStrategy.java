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
package org.ngengine.auth;

import com.jme3.export.InputCapsule;
import com.jme3.export.JmeExporter;
import com.jme3.export.JmeImporter;
import com.jme3.export.OutputCapsule;
import com.jme3.export.Savable;
import com.jme3.export.SavableWrapSerializable;
import java.io.IOException;
import java.time.Duration;
import java.util.List;
import org.ngengine.nostr4j.keypair.NostrKeyPair;
import org.ngengine.nostr4j.keypair.NostrPrivateKey;
import org.ngengine.nostr4j.nip46.Nip46AppMetadata;

public class Nip46AuthStrategy implements Savable {

    protected List<String> relays = List.of("wss://relay.nsec.app", "wss://relay.ngengine.org");

    protected Nip46AppMetadata metadata = new Nip46AppMetadata().setName("ngengine.org - Unnamed App");
    protected NostrKeyPair appKeyPair;
    protected boolean allowNostrConnect = true;
    protected boolean allowBunker = true;
    protected Duration timeout = Duration.ofMinutes(60);
    protected Duration challengeTimeout = Duration.ofMinutes(20);

    public Nip46AuthStrategy(NostrKeyPair appKeyPair) {
        this.appKeyPair = appKeyPair;
    }

    public Nip46AuthStrategy setTimeout(Duration timeout) {
        this.timeout = timeout;
        return this;
    }

    public Nip46AuthStrategy setChallengeTimeout(Duration timeout) {
        this.challengeTimeout = timeout;
        return this;
    }

    public Duration getChallengeTimeout() {
        return challengeTimeout;
    }

    public Duration getTimeout() {
        return timeout;
    }

    public Nip46AuthStrategy() {
        this.appKeyPair = new NostrKeyPair(NostrPrivateKey.generate());
    }

    public Nip46AuthStrategy withBunkerFlow() {
        this.allowBunker = true;
        return this;
    }

    public Nip46AuthStrategy withNostrConnectFlow() {
        this.allowNostrConnect = true;
        return this;
    }

    public Nip46AuthStrategy withoutBunkerFlow() {
        this.allowBunker = false;
        return this;
    }

    public Nip46AuthStrategy withoutNostrConnectFlow() {
        this.allowNostrConnect = false;
        return this;
    }

    public boolean isAllowNostrConnect() {
        return allowNostrConnect;
    }

    public boolean isAllowBunker() {
        return allowBunker;
    }

    public Nip46AuthStrategy setRelays(List<String> relays) {
        this.relays = relays;
        return this;
    }

    public Nip46AuthStrategy setMetadata(Nip46AppMetadata metadata) {
        this.metadata = metadata;
        return this;
    }

    public Nip46AppMetadata getMetadata() {
        return metadata;
    }

    public List<String> getRelays() {
        return relays;
    }

    public NostrKeyPair getAppKeyPair() {
        return appKeyPair;
    }

    @Override
    public void write(JmeExporter ex) throws IOException {
        OutputCapsule capsule = ex.getCapsule(this);

        String relaysStr[] = new String[relays.size()];
        for (int i = 0; i < relays.size(); i++) relaysStr[i] = relays.get(i);

        capsule.write(relaysStr, "relays", new String[0]);

        SavableWrapSerializable savableMeta = new SavableWrapSerializable(metadata);
        capsule.write(savableMeta, "metadata", null);

        SavableWrapSerializable savableKeyPair = new SavableWrapSerializable(appKeyPair);
        capsule.write(savableKeyPair, "appKeyPair", null);
    }

    @Override
    public void read(JmeImporter im) throws IOException {
        InputCapsule capsule = im.getCapsule(this);

        String relaysStr[] = capsule.readStringArray("relays", new String[0]);
        relays = List.of(relaysStr);

        SavableWrapSerializable savableMeta = (SavableWrapSerializable) capsule.readSavable("metadata", null);
        if (metadata != null) metadata = (Nip46AppMetadata) savableMeta.get();

        SavableWrapSerializable savableKeyPair = (SavableWrapSerializable) capsule.readSavable("appKeyPair", null);
        if (savableKeyPair != null) appKeyPair = (NostrKeyPair) savableKeyPair.get();
    }
}
