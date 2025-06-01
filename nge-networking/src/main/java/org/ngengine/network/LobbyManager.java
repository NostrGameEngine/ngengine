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
import java.util.function.BiConsumer;
import java.util.function.Consumer;
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
import org.ngengine.runner.PassthroughRunner;
import org.ngengine.runner.Runner;

public class LobbyManager implements Closeable {
    private final int KIND = 30078;
    private final NostrPool masterServersPool;
    private final NostrSigner localSigner;
    private final String gameName;
    private final int gameVersion;
    private final String turnServer;
    private final AsyncExecutor looper;
    private final ArrayList<WeakReference<Lobby>> trackedLobbies = new ArrayList<>();
    private final Runner dispatcher;
    private volatile boolean closed = false;

    private transient Boolean isSearchSupported;

    private static final Logger log = Logger.getLogger(LobbyManager.class.getName());

    public LobbyManager( 
        NostrSigner signer,
        String gameName,
        int gameVersion,
        Collection<String> relays,
        String turnServer
     ){
        this(signer, gameName, gameVersion, relays, turnServer, new PassthroughRunner());

    }

    public LobbyManager( 
        NostrSigner signer,
        String gameName,
        int gameVersion,
        Collection<String> relays,
        String turnServer,
        Runner dispatcher
    ){
        this.dispatcher = dispatcher;

        this.looper = NGEUtils.getPlatform().newAsyncExecutor();
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
        this.looper.runLater(() -> {
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
            looper.close();
        } catch (Exception e) {
            log.log(Level.WARNING, "Failed to close executor: " + e.getMessage());
        }
    }

    public void listLobbies(NostrFilter filter, BiConsumer<List<Lobby>, Throwable> callback) {
        masterServersPool.fetch(filter, 6000, TimeUnit.MILLISECONDS).then(events -> {
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
            this.dispatcher.run(()->{
                callback.accept(lobbies, null);
            });
            return lobbies;
        }).catchException(ex -> {
            log.log(Level.WARNING, "Failed to fetch lobbies: " + ex.getMessage(), ex);
            this.dispatcher.run(() -> {
                callback.accept(null, ex);
            });
        });
    }

    private boolean isSearchSupported() {
        if (isSearchSupported != null) return isSearchSupported;
        try {
            for (NostrRelay relay : masterServersPool.getRelays()) {
                if (!relay.getInfo().isNipSupported(50)) {
                    isSearchSupported = false;
                    return isSearchSupported;
                }
            }
            return isSearchSupported;
        } catch (Exception e) {
            log.warning("Failed to check search support: " + e.getMessage());
            isSearchSupported = false;
            return isSearchSupported;
        }
    }

    public void listLobbies(
        String words,
        int limit,
            Map<String, String> tagsFilter, BiConsumer<List<Lobby>, Throwable> callback) {
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

        listLobbies(filter, (lobbies, err) -> {
            if (err != null) {
                this.dispatcher.run(() -> {
                    callback.accept(null, err);
                });
                 return;
            }
            List<Lobby> filteredLobbies = lobbies.stream().filter(lobby -> {
                if (tagsFilter != null) {
                    // client side filter by tags > 1 letter
                    for (Entry<String, String> tagFilter : tagsFilter.entrySet()) {
                        String key = tagFilter.getKey();
                        if (key.length() == 1) continue; // 1 letter tags are already filtered

                        String value = lobby.getData(key);
                        if (value == null) return false; // lobby doesn't have this tag -> filter it out

                        // lobby has this tag, but the value is not in the filter -> filter it out
                        if (!value.equals(tagFilter.getValue())) return false;
                    }
                }

                if (words != null && !words.isEmpty() && !isSearchSupported()) {
                    // client side filter by words
                    return lobby.matches(words.split("[ ,]+"));
                }

                return true;
            }).toList();
            this.dispatcher.run(() -> {
                callback.accept(filteredLobbies,null);
            });
        });

    }

    protected void lobbyToEvent(Lobby lobby, BiConsumer<SignedNostrEvent, Throwable> callback) {
        UnsignedNostrEvent event = new UnsignedNostrEvent().withKind(KIND);
        event.withContent(lobby.getRawData());
        for(Entry<String, String> entry : lobby.getData().entrySet()){
            String key = entry.getKey();
            String value = entry.getValue();
            event.withTag(key, value);
        }
        event.withExpiration(lobby.getExpiration());
        log.info("Signing lobby event: " + event);
        localSigner.sign(event).then(signed -> {
            this.dispatcher.run(() -> {
                callback.accept(signed, null);
            });
             return null;
        }).catchException(err -> {
            log.log(Level.WARNING, "Failed to sign lobby event: " + err.getMessage(), err);
            this.dispatcher.run(() -> {
                callback.accept(null, err);
            });
         });

    }

    public void createLobby(
        String passphrase,
        Map<String,String> tags,
            Duration expiration, BiConsumer<Lobby, Throwable> callback) {

        BiConsumer<NostrPrivateKey, String> create = (newPriv, roomKey) -> {

            String roomId = newPriv.getPublicKey().asBech32();

            Map<String, String> data = new HashMap<>();
            data.put("roomKey", roomKey);
            data.put("t", gameName + "/" + gameVersion);
            data.put("d", roomId);
            for (Entry<String, String> entry : tags.entrySet()) {
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
            lobbyToEvent(lobby, (signed, error) -> {
                if (error != null) {
                    log.log(Level.WARNING, "Failed to create lobby: " + error.getMessage(), error);
                     this.dispatcher.run(() -> {
                        callback.accept(null,error);
                    });
                    return;
                }
                log.info("Creating lobby with event " + signed.toMap());
                masterServersPool.send(signed).then((acks) -> {
                    this.dispatcher.run(() -> {
                        callback.accept(lobby, null);
                    });                    
                    return null;
                }).catchException(err -> {
                    this.dispatcher.run(() -> {
                        callback.accept(null,error);
                    });
                 });
            });
        };

        NostrPrivateKey newPriv = NostrPrivateKey.generate();
        if (passphrase != null && !passphrase.isEmpty()) {
            Nip49.encrypt(newPriv, passphrase).then(roomKey -> {
                create.accept(newPriv, roomKey);
                return null;
            }).catchException(err -> {
                log.log(Level.WARNING, "Failed to encrypt private key: " + err.getMessage(), err);
                this.dispatcher.run(() -> {
                    callback.accept(null, err);
                });
            });
        } else {
            create.accept(newPriv, newPriv.asBech32());
        }

    }

    void updateLobby(LocalLobby lobby) {
        synchronized (trackedLobbies) {
            if (!trackedLobbies.stream().anyMatch(ref -> ref.get() == lobby)) {
                trackedLobbies.add(new WeakReference<>(lobby));
            }
        }
        lobbyToEvent(lobby, (signed, err) -> {
            if (err != null) {
                log.log(Level.WARNING, "Failed to update lobby: " + err.getMessage(), err);
                return;
            }
            masterServersPool.send(signed);
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
                turnServer, this.masterServersPool, lobby, dispatcher);
        conn.start();
        return conn;        
    }
}
