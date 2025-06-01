package org.ngengine.demo.son.gui;

import java.util.function.Consumer;

import org.ngengine.network.LobbyManager;
import org.ngengine.network.P2PChannel;

public class LobbyManagerWindowArg {
    protected LobbyManager mng;
    protected Consumer<P2PChannel> onJoin;
    public LobbyManagerWindowArg(LobbyManager mng, Consumer<P2PChannel> onJoin) {
        this.mng = mng;
        this.onJoin = onJoin;
    }
}