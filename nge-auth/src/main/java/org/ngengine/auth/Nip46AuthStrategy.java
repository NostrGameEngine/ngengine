package org.ngengine.auth;

import java.io.IOException;
import java.time.Duration;
import java.util.List;

import org.ngengine.nostr4j.keypair.NostrKeyPair;
import org.ngengine.nostr4j.keypair.NostrPrivateKey;
import org.ngengine.nostr4j.nip46.Nip46AppMetadata;

import com.jme3.export.InputCapsule;
import com.jme3.export.JmeExporter;
import com.jme3.export.JmeImporter;
import com.jme3.export.OutputCapsule;
import com.jme3.export.Savable;
import com.jme3.export.SavableWrapSerializable;

public class Nip46AuthStrategy implements Savable{
    protected List<String> relays = List.of(
        "wss://relay.nsec.app",
        "wss://relay.ngengine.org"    
    );
    
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
  
    public Nip46AuthStrategy withBunkerFlow(){
        this.allowBunker = true;
        return this;
    }

    public Nip46AuthStrategy withNostrConnectFlow(){
        this.allowNostrConnect = true;
        return this;
    }

    public Nip46AuthStrategy withoutBunkerFlow(){
        this.allowBunker = false;
        return this;
    }

    public Nip46AuthStrategy withoutNostrConnectFlow(){
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
        for (int i = 0; i < relays.size(); i++)  relaysStr[i] = relays.get(i);        
        
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
        if(metadata!=null) metadata = (Nip46AppMetadata) savableMeta.get();

        SavableWrapSerializable savableKeyPair = (SavableWrapSerializable) capsule.readSavable("appKeyPair", null);
        if(savableKeyPair!=null) appKeyPair = (NostrKeyPair) savableKeyPair.get();

    }

}
