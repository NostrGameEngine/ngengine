package org.ngengine.auth;

import java.util.function.Consumer;

import org.ngengine.nostr4j.signer.NostrSigner;
import org.ngengine.platform.VStore;
import org.ngengine.player.PlayerManagerAppState;

public class AuthStrategy {
 

    protected Nip46AuthStrategy nip46;
    protected boolean nsec = false;
    protected VStore store;
    protected Consumer<NostrSigner> callback;
    protected PlayerManagerAppState playerManager;

 
    public AuthStrategy(Consumer<NostrSigner> callback) {
        this.callback = callback;
    }
    
    public Consumer<NostrSigner> getCallback() {
        return callback;
    }

    public AuthStrategy enableNip46(Nip46AuthStrategy nip46) {
        this.nip46 = nip46;
        return this;
    }

    public AuthStrategy enableStore(VStore store) {
        this.store = store;
        return this;
    }

    public AuthStrategy disableStore() {
        this.store = null;
        return this;
    }

    public AuthStrategy setPlayerManager(PlayerManagerAppState playerManager) {
        this.playerManager = playerManager;
        return this;
    }

    public PlayerManagerAppState getPlayerManager() {
        return playerManager;
    }

    public boolean isStoreEnabled() {
        return store != null;
    }

    public VStore getStore() {
        return store;
    }

    public AuthStrategy enableNsec( ) {
        this.nsec = true;
        return this;
    }

    public AuthStrategy disableNip46() {
        nip46 = null;
        return this;
    }

    public AuthStrategy disableNsec() {
        nsec = false;
        return this;
    }

    public AuthStrategy enableNip07(Object nip07adapter) {
         return this;
    }

    public AuthStrategy disableNip07() {
         return this;
    }

    public boolean isNip46Enabled() {
        return nip46 != null;
    }

    public boolean isNsecEnabled() {
        return nsec;
    }

    public boolean isNip07Enabled() {
        return false;
    }


    public Nip46AuthStrategy getNip46Strategy() {
        return nip46;
    }

    
}
