package org.ngengine.player;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.logging.Logger;

import org.ngengine.components.Component;
import org.ngengine.components.ComponentManager;
import org.ngengine.components.fragments.AssetLoadingFragment;
import org.ngengine.components.fragments.AsyncAssetLoadingFragment;
import org.ngengine.network.P2PChannel;
import org.ngengine.network.RemotePeer;
import org.ngengine.nostr4j.NostrPool;
import org.ngengine.nostr4j.NostrRelay;
import org.ngengine.nostr4j.keypair.NostrPublicKey;
import org.ngengine.nostr4j.signer.NostrSigner;
import org.ngengine.platform.VStore;
import org.ngengine.runner.Runner;
import org.ngengine.store.DataStoreProvider;

import com.jme3.asset.AssetManager;
import com.jme3.network.HostedConnection;

public class PlayerManagerComponent implements Component<Object>, AssetLoadingFragment {
    public static final List<String> DEFAULT_ID_RELAYS = List.of(
        "wss://relay.ngengine.org",
        "wss://relay.snort.social",
        "wss://nostr.wine",
        "wss://nos.lol",
        "wss://relay.damus.io",
        "wss://relay.primal.net"
    );
    private static final Logger log = Logger.getLogger(PlayerManagerComponent.class.getName());
    protected Collection<String> connectToRelays;
    protected NostrPool nostrPool;
    protected boolean externalPool = false;
    protected Map<NostrPublicKey, Player> players = new WeakHashMap<>();
    protected Map<NostrPublicKey, LocalPlayer> localPlayers = new WeakHashMap<>();
    protected Runner runner;
    protected AssetManager assetManager;

    public PlayerManagerComponent() {
    }


    public PlayerManagerComponent( Collection<String> idRelays) {
        this.connectToRelays = idRelays;       
    }

    public PlayerManagerComponent(VStore dataStore, NostrPool pool){
        this.nostrPool = pool;
        this.externalPool = true;
        this.connectToRelays = null;
    }


    public void enqueueToRenderThread(Runnable act) {
        runner.run(act);
    }

    public NostrPool getPool(){ 
        return nostrPool;
    }

    @Override
    public void onEnable(ComponentManager mng, Runner runner, DataStoreProvider dataStoreProvider,
            boolean firstTime, Object slot) {
        if (!externalPool) {
            this.nostrPool = new NostrPool();
            if(connectToRelays==null){
                connectToRelays = DEFAULT_ID_RELAYS;
            }
            for (String relay : connectToRelays) {
                try {
                    this.nostrPool.connectRelay(new NostrRelay(relay));
                } catch (Exception e) {
                    System.err.println("Failed to connect to relay: " + relay);
                }
            }
        }
    }

    @Override
    public void onDisable(ComponentManager mng, Runner runner,
            DataStoreProvider dataStoreProvider) {
        if (!externalPool) {
            if (nostrPool != null) {
                // close and disconnect all relays
                nostrPool.close().forEach(r -> {
                    r.disconnect("Closed");
                });
            }
        }
    }

    @Override
    public void loadAssets(AssetManager assetManager) {
        this.assetManager = assetManager;
    }

    public AssetManager getAssetManager() {
        return assetManager;
    }

    @Override
    public void onAttached(ComponentManager mng, Runner runner,
            DataStoreProvider dataStoreProvider) {
        this.runner = runner;
    }

     public Player getPlayer(NostrPublicKey pubkey) {
        Player player = localPlayers.get(pubkey);
        if (player == null) {
            player = players.computeIfAbsent(pubkey, (k) -> {
                Player p = new Player(this, pubkey);
                p.refresh();
                return p;
            });
        }
        return player;
    }

    public LocalPlayer getPlayer(NostrSigner signer) {
        try {
            NostrPublicKey pubkey = signer.getPublicKey().await();
            LocalPlayer localPlayer = localPlayers.get(pubkey);
            if (localPlayer == null) {
                Player remotePlayer = players.get(pubkey);
                if (remotePlayer != null) {
                    // local player not found, but found a remote player
                    // since we have access to the signer, we can upgrade it to a local player
                    localPlayer = new LocalPlayer(remotePlayer, signer);
                    localPlayers.put(pubkey, localPlayer);
                } else {
                    // local player not found, and remote player not found
                    // create a new local player
                    localPlayer = new LocalPlayer(this, signer);
                    localPlayer.refresh();
                    localPlayers.put(pubkey, localPlayer);
                }
            }
            return localPlayer;
        } catch (Exception exc) {
            log.warning("Failed to get local player: " + exc.getMessage());
        }
        return null;
    }


    public Player getPlayer(RemotePeer peer){
        return getPlayer(peer.getSocket().getRemotePeer().getPubkey());
    }

    public LocalPlayer getPlayer(P2PChannel chan){
        return getPlayer(chan.getLocalSigner());        
    }


    public Player getPlayer(HostedConnection conn){
        if(conn instanceof RemotePeer){
            RemotePeer peer = (RemotePeer) conn;
            return getPlayer(peer);
        } else{
            return getPlayer(NostrPublicKey.fromBech32(conn.getAddress()));
        }
    }

 
}