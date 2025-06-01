package org.ngengine.network;

import java.time.Instant;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import org.ngengine.platform.AsyncExecutor;
import org.ngengine.platform.AsyncTask;
import org.ngengine.platform.NGEPlatform;
import org.ngengine.platform.NGEUtils;

public class LocalLobby  extends Lobby {
    private static final Logger logger = Logger.getLogger(LocalLobby.class.getName());

    private transient volatile boolean updateNeeded = false;

    public LocalLobby(String roomId, String roomKey, String roomRawData, Instant expiration) {
        super(roomId, roomKey, roomRawData, expiration);

    }

    @Override
    public boolean isOwnedByLocalPeer() {
        return true;
    }
    
    public void setData(String key, String value) {
        super.setData(key, value);
        NGEPlatform p = NGEUtils.getPlatform();
        String rawData = p.toJSON(this.data);
        this.roomRawData = rawData;
        this.updateNeeded = true;

    }
    
    protected void setDataSilent(String key, String value) {
        super.setData(key, value);
    }

    protected boolean isUpdateNeeded() {
        return updateNeeded;
    }

    protected void clearUpdateNeeded() {
        this.updateNeeded = false;
    }
}
