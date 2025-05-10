package org.ngengine.demo.soo;

import java.util.List;

import org.ngengine.nostr4j.keypair.NostrKeyPair;
import org.ngengine.nostr4j.keypair.NostrPrivateKey;
import org.ngengine.nostr4j.signer.NostrKeyPairSigner;
import org.ngengine.nostr4j.signer.NostrSigner;

public class Settings {
    public static final String GAME_NAME="soo";
    public static final int GAME_VERSION = 100;
    public static final List<String> RELAYS = List.of("wss://nostr.rblb.it");
    public static final String TURN_SERVER = "wss://nostr.rblb.it";
    public static final NostrSigner SIGNER = new NostrKeyPairSigner(new NostrKeyPair(NostrPrivateKey.generate()));
    
}
