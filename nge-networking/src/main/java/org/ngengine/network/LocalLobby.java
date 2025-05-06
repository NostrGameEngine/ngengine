package org.ngengine.network;

import java.time.Instant;

import org.ngengine.nostr4j.platform.Platform;
import org.ngengine.nostr4j.utils.NostrUtils;

public class LocalLobby  extends Lobby {
    private transient final LobbyManager lobbyList;
    public LocalLobby(String roomId, String roomKey, String roomRawData, Instant expiration, LobbyManager lobbyList) {  
        super(roomId, roomKey, roomRawData, expiration);
        this.lobbyList = lobbyList;
    }
    
    public void setData(String key, String value) throws Exception{
        super.setData(key, value);
        Platform p = NostrUtils.getPlatform();
        String rawData = p.toJSON(this.data);
        this.roomRawData = rawData;
        lobbyList.updateLobby(this);
    }
    
    protected void setDataSilent(String key, String value) throws Exception {
        super.setData(key, value);
    
    }
}
