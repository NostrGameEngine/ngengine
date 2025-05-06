package org.ngengine.network;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ExecutionException;
import java.util.logging.Logger;

import org.ngengine.nostr4j.NostrFilter;
import org.ngengine.nostr4j.NostrPool;
import org.ngengine.nostr4j.NostrRelay;
import org.ngengine.nostr4j.event.SignedNostrEvent;
import org.ngengine.nostr4j.event.UnsignedNostrEvent;
import org.ngengine.nostr4j.keypair.NostrPrivateKey;
import org.ngengine.nostr4j.nip50.NostrSearchFilter;
import org.ngengine.nostr4j.platform.Platform;
import org.ngengine.nostr4j.signer.NostrSigner;
import org.ngengine.nostr4j.utils.NostrUtils;

public class LobbyManager {
    private final int KIND = 30078;
    private final NostrPool masterServersPool;
    private final NostrSigner localSigner;
    private final String gameName;
    private final int gameVersion;
    private final String turnServer;

    private transient Boolean isSearchSupported;

    private static final Logger log = Logger.getLogger(LobbyManager.class.getName());

    public LobbyManager( NostrSigner signer,
        String gameName,
        int gameVersion,
        Collection<String> relays,
        String turnServer
    ){

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
    }


    public List<Lobby> listLobbies(NostrFilter filter) throws InterruptedException, ExecutionException{
        List<SignedNostrEvent> events = masterServersPool.fetch(filter).await();
        List<Lobby> lobbies = new ArrayList<>();
        Platform p = NostrUtils.getPlatform();
        for(SignedNostrEvent event : events){
            try{
                String rawData = event.getContent();
                Map<String, String> data = p.fromJSON(rawData, Map.class);
                if(data==null) continue;

                String roomKey = NostrUtils.safeString(data.get("roomKey"));
                if(roomKey==null) continue;

                String roomId = event.getTagValues("d").get(0);
                Instant expiration = event.getExpiration();
                
                Lobby lobby = new Lobby(roomId, roomKey, rawData, expiration);
                for(Entry<String, List<String>> tagEntry : event.getTags().entrySet()){
                    String key = tagEntry.getKey();
                    if(key.equals("expiration")) continue; // ignore expiration tag
                    List<String> values = tagEntry.getValue();
                    lobby.setData(key,values.get(0));
                }
                
                for(Entry<String, String> entry : data.entrySet()){
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

    public List<Lobby> listLobbies(
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

        List<Lobby> lobbies = listLobbies(filter);

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
    }

    protected SignedNostrEvent lobbyToEvent(Lobby lobby) throws InterruptedException, ExecutionException{
        UnsignedNostrEvent event = new UnsignedNostrEvent().withKind(KIND);
        event.withContent(lobby.getRawData());
        for(Entry<String, String> entry : lobby.getData().entrySet()){
            String key = entry.getKey();
            String value = entry.getValue();
            event.withTag(key, value);
        }
        event.withExpiration(lobby.getExpiration());
        SignedNostrEvent signed = localSigner.sign(event).await();    
        return signed;        

    }

    public LocalLobby createLobby(
        String passphrase,
        Map<String,String> tags,
        Duration expiration
    ) throws Exception{
        NostrPrivateKey newPriv = NostrPrivateKey.generate();
        String roomKey = passphrase != null && ! passphrase.isEmpty() ? Crypto.encrypt(newPriv.asBech32(), passphrase) : newPriv.asBech32();
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

        String rawData = NostrUtils.getPlatform().toJSON(data);

        LocalLobby lobby = new LocalLobby(roomId, roomKey, rawData, Instant.now().plus(expiration), this);
        for (Entry<String, String> dataEntry : data.entrySet()) {
            String key = dataEntry.getKey();
            String value = dataEntry.getValue();
            lobby.setDataSilent(key, value);
        }     
 
        try {
            SignedNostrEvent signed = lobbyToEvent(lobby);
            masterServersPool.send(signed).await();
            return lobby;
        } catch (Exception e){
            throw new Exception("Failed to create lobby",e);
        }        
    }

    void updateLobby(LocalLobby lobby) throws Exception{
        try {
            SignedNostrEvent signed = lobbyToEvent(lobby);
            masterServersPool.send(signed).await();
        } catch (Exception e) {
            throw new Exception("Failed to update lobby", e);
        }     
    }


    public NetworkChannel connectToLobby(Lobby lobby, String passphrase) throws Exception{
        String nsec = lobby.getKey();
        if (lobby.isLocked()) {
            nsec = Crypto.decrypt(nsec, passphrase);
            if (!nsec.startsWith("nsec")) throw new IllegalArgumentException("Invalid passphrase");
        }

        NetworkChannel conn = new NetworkChannel(this.localSigner, this.gameName, this.gameVersion, nsec, turnServer, this.masterServersPool);
        return conn;        
    }
}
