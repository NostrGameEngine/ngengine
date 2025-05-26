package org.ngengine.network;

import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.ngengine.nostr4j.rtc.NostrRTCSocket;

import com.jme3.network.HostedConnection;
import com.jme3.network.Message;
import com.jme3.network.Server;
import com.jme3.network.base.MessageProtocol;

public class RemotePeer implements HostedConnection {
    private static final Logger log = Logger.getLogger(RemotePeer.class.getName());

    private final P2PChannel server;
    private final NostrRTCSocket socket;
    private final int id;
    private final Map<String,Object> sessionData = new ConcurrentHashMap<>();       
    private final MessageProtocol protocol;

    RemotePeer(
        int id,
        NostrRTCSocket socket,
        P2PChannel server,
        MessageProtocol protocol
    ){
        this.socket = socket;      
        this.server = server;
        this.id = id;
        this.protocol = protocol;
    }

    public NostrRTCSocket getSocket() {
        return socket;
    }
    
    @Override
    public void send(Message message) {
        if( log.isLoggable(Level.FINER) ) {
            log.log(Level.FINER, "send({0})", message);
        }
        ByteBuffer buffer = protocol.toByteBuffer(message, null);
        this.socket.write(buffer);
    }

    @Override
    public void send(int channel, Message message) {
        send(message);
    }

    @Override
    public Server getServer() {
        return server;
    }

    @Override
    public int getId() {
        return id;
    }

    @Override
    public String getAddress() {
        return socket.getLocalPeer().getPubkey().asHex();
    }

    @Override
    public void close(String reason) {
        this.socket.close();
    }

    @Override
    public Object setAttribute( String name, Object value )
    {
        if( value == null )
            return sessionData.remove(name);
        return sessionData.put(name, value);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> T getAttribute( String name )
    {
        return (T)sessionData.get(name);
    }             

    @Override
    public Set<String> attributeNames()
    {
        return Collections.unmodifiableSet(sessionData.keySet());
    }           
    
    
}
