package org.ngengine.network;

import java.io.Closeable;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.ngengine.nostr4j.NostrFilter;
import org.ngengine.nostr4j.NostrPool;
import org.ngengine.nostr4j.NostrRelay;
import org.ngengine.nostr4j.event.SignedNostrEvent;
import org.ngengine.nostr4j.event.UnsignedNostrEvent;
import org.ngengine.nostr4j.event.NostrEvent.TagValue;
import org.ngengine.nostr4j.keypair.NostrPrivateKey;
import org.ngengine.nostr4j.nip49.Nip49;
import org.ngengine.nostr4j.nip50.NostrSearchFilter;
import org.ngengine.nostr4j.signer.NostrSigner;
import org.ngengine.platform.AsyncExecutor;
import org.ngengine.platform.AsyncTask;
import org.ngengine.platform.NGEPlatform;
import org.ngengine.platform.NGEUtils;

public class LobbyManager implements Closeable {
    private final int KIND = 30078;
    private final NostrPool masterServersPool;
    private final NostrSigner localSigner;
    private final String gameName;
    private final int gameVersion;
    private final String turnServer;
    private final AsyncExecutor executor;
    private final ArrayList<WeakReference<Lobby>> trackedLobbies = new ArrayList<>();
    private volatile boolean closed = false;

    private transient Boolean isSearchSupported;

    private static final Logger log = Logger.getLogger(LobbyManager.class.getName());


    public LobbyManager( NostrSigner signer,
        String gameName,
        int gameVersion,
        Collection<String> relays,
        String turnServer
    ){

        this.executor = NGEUtils.getPlatform().newAsyncExecutor();
        this.localSigner = signer;
        this.gameName = gameName;
        this.gameVersion = gameVersion;
        this.turnServer = turnServer;
        
        this.masterServersPool = new NostrPool();
        for(String server : relays) {
            try {
                this.masterServersPool.connectRelay(new NostrRelay(server));
            } catch (Exception e) {
                log.warning("Failed to add server: " + server);
            }
        }    

        update();

    }

    protected void update() {
        this.executor.runLater(() -> {
            if (closed) return null;
            try {
                synchronized (trackedLobbies) {
                    Iterator<WeakReference<Lobby>> it = trackedLobbies.iterator();
                    while (it.hasNext()) {
                        WeakReference<Lobby> ref = it.next();
                        Lobby lobby = ref.get();
                        if (lobby == null) {
                            it.remove();
                            continue;
                        }
                        if (lobby instanceof LocalLobby) {
                            LocalLobby llobby = (LocalLobby) lobby;
                            if (llobby.isUpdateNeeded()) {
                                updateLobby((LocalLobby) lobby);
                                llobby.clearUpdateNeeded();
                            }
                        }
                    }
                }

            } catch (Exception e) {
                log.log(Level.WARNING, "Error during lobby manager update: " + e.getMessage(), e);
            }
            update();

            return null;
        }, 10000, TimeUnit.MILLISECONDS);
    }

    public void close() {
        closed = true;
        try {
            executor.close();
        } catch (Exception e) {
            log.log(Level.WARNING, "Failed to close executor: " + e.getMessage());
        }
    }

    public AsyncTask<List<Lobby>> listLobbies(NostrFilter filter)
            throws InterruptedException, ExecutionException {
        return masterServersPool.fetch(filter, 6000, TimeUnit.MILLISECONDS).then(events -> {
            List<Lobby> lobbies = new ArrayList<>();
            NGEPlatform p = NGEUtils.getPlatform();
            for (SignedNostrEvent event : events) {
                try {
                    String rawData = event.getContent();
                    Map<String, String> data = p.fromJSON(rawData, Map.class);
                    if (data == null) continue;

                    String roomKey = NGEUtils.safeString(data.get("roomKey"));
                    if (roomKey == null) continue;

                    String roomId = event.getFirstTag("d").get(0);
                    Instant expiration = event.getExpiration();

                    Lobby lobby;
                    if (event.getPubkey().equals(this.localSigner.getPublicKey().await())) {
                        lobby = new LocalLobby(roomId, roomKey, rawData, expiration);
                    } else {
                        lobby = new Lobby(roomId, roomKey, rawData, expiration);
                    }
                    // for(Entry<String, List<String>> tagEntry : event.getTags().entrySet()){
                    for (String tagKey : event.listTagKeys()) {
                        TagValue tagValue = event.getFirstTag(tagKey);
                        if (tagKey.equals("expiration")) continue; // ignore expiration tag
                        lobby.setData(tagKey, tagValue.get(0));
                    }

                    for (Entry<String, String> entry : data.entrySet()) {
                        String key = entry.getKey();
                        String value = entry.getValue();
                        lobby.setData(key, value);
                    }

                    lobbies.add(lobby);

                } catch (Exception e) {
                    log.warning("Failed to parse lobby: " + e.getMessage());
                    continue;
                }
            }
            return lobbies;
        });
    }

    private boolean isSearchSupported() throws IOException{
        if(isSearchSupported!=null) return isSearchSupported;
        for(NostrRelay relay : masterServersPool.getRelays()){
            if(!relay.getInfo().isNipSupported(50)){
                isSearchSupported = false;
                return isSearchSupported;
            }
        }
        return isSearchSupported;
    }

    public AsyncTask<List<Lobby>> listLobbies(
        String words,
        int limit,
        Map<String,String> tagsFilter
    ) throws InterruptedException, ExecutionException, IOException{
        NostrFilter filter = null;
        if(words!=null&&!words.isEmpty()&&isSearchSupported()){
            filter = new NostrSearchFilter();
            ((NostrSearchFilter)filter).search(words);
        }else{
            filter = new NostrFilter();
        }           
        filter.withKind(KIND);        
        filter.withTag("t", gameName + "/" + gameVersion);

        if(tagsFilter!=null){
            // relay side filter for 1 letter tags
            for(Map.Entry<String,String> entry : tagsFilter.entrySet()){
                if(entry.getKey().length()>1) continue;
                filter.withTag(entry.getKey(), entry.getValue());
            }
        }

        return listLobbies(filter).then(lobbies -> {
            // more client side filtering
        lobbies = lobbies.stream().filter(lobby -> {
            if(tagsFilter!=null){
                // client side filter by tags > 1 letter
                for(Entry<String, String> tagFilter : tagsFilter.entrySet()) {
                    String key = tagFilter.getKey();
                    if(key.length()==1)continue; // 1 letter tags are already filtered

                    String value = lobby.getData(key);
                    if(value==null) return false; // lobby doesn't have this tag -> filter it out

                    // lobby has this tag, but the value is not in the filter -> filter it out
                    if(!value.equals(tagFilter.getValue()))return false; 
                }
            }

            try{
                if(words!=null&&!words.isEmpty()&&!isSearchSupported()){
                    // client side filter by words
                    return lobby.matches(words.split("[ ,]+"));
                }
            }catch(IOException e){
                log.warning("Failed to check search support: " + e.getMessage());
            }

            return true;
        }).toList();
     
 
        return lobbies;
        });

    }

    protected AsyncTask<SignedNostrEvent> lobbyToEvent(Lobby lobby) {
        UnsignedNostrEvent event = new UnsignedNostrEvent().withKind(KIND);
        event.withContent(lobby.getRawData());
        for(Entry<String, String> entry : lobby.getData().entrySet()){
            String key = entry.getKey();
            String value = entry.getValue();
            event.withTag(key, value);
        }
        event.withExpiration(lobby.getExpiration());
        log.info("Signing lobby event: " + event);
        return localSigner.sign(event);

    }

    public AsyncTask<LocalLobby> createLobby(
        String passphrase,
        Map<String,String> tags,
        Duration expiration
    ) throws Exception {
        NostrPrivateKey newPriv = NostrPrivateKey.generate();
        String roomKey = passphrase != null && !passphrase.isEmpty() ? Nip49.encryptSync(newPriv, passphrase)
                                                                     : newPriv.asBech32();
        String roomId = newPriv.getPublicKey().asBech32();      

        Map<String,String> data = new HashMap<>();
        data.put("roomKey", roomKey);
        data.put("t", gameName + "/" + gameVersion);
        data.put("d", roomId);
        for(Entry<String,String> entry : tags.entrySet()){
            String key = entry.getKey();
            String value = entry.getValue();
            data.put(key, value);
        }

        String rawData = NGEUtils.getPlatform().toJSON(data);

        LocalLobby lobby = new LocalLobby(roomId, roomKey, rawData, Instant.now().plus(expiration));
        for (Entry<String, String> dataEntry : data.entrySet()) {
            String key = dataEntry.getKey();
            String value = dataEntry.getValue();
            lobby.setDataSilent(key, value);
        }     
        synchronized (trackedLobbies) {
            if (!trackedLobbies.stream().anyMatch(ref -> ref.get() == lobby)) {
                trackedLobbies.add(new WeakReference<>(lobby));
            }
        }
        return lobbyToEvent(lobby).compose(signed -> {
            log.info("Creating lobby with event " + signed.toMap());
            return masterServersPool.send(signed);
        }).then(s -> {
            return lobby;
        });

    }

    AsyncTask<Void> updateLobby(LocalLobby lobby) {
        synchronized (trackedLobbies) {
            if (!trackedLobbies.stream().anyMatch(ref -> ref.get() == lobby)) {
                trackedLobbies.add(new WeakReference<>(lobby));
            }
        }
        return lobbyToEvent(lobby).then(signed -> {
            masterServersPool.send(signed);
            return null;
        });
    }


    public P2PChannel connectToLobby(Lobby lobby, String passphrase) throws Exception {
        NostrPrivateKey privKey = lobby.getKey(passphrase);
        synchronized (trackedLobbies) {
            if (!trackedLobbies.stream().anyMatch(ref -> ref.get() == lobby)) {
                trackedLobbies.add(new WeakReference<>(lobby));
            }
        }

        P2PChannel conn = new P2PChannel(this.localSigner, this.gameName, this.gameVersion, privKey,
                turnServer, this.masterServersPool, lobby);
        conn.start();
        return conn;        
    }
}
