package org.ngengine.network;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.Test;
import org.ngengine.network.protocol.messages.ChatMessage;
import org.ngengine.nostr4j.keypair.NostrKeyPair;
import org.ngengine.nostr4j.keypair.NostrPrivateKey;
import org.ngengine.nostr4j.keypair.NostrPublicKey;
import org.ngengine.nostr4j.rtc.listeners.NostrRTCRoomPeerDiscoveredListener.NostrRTCRoomPeerDiscoveredState;
import org.ngengine.nostr4j.rtc.signal.NostrRTCAnnounce;
import org.ngengine.nostr4j.signer.NostrKeyPairSigner;

import com.jme3.network.AbstractMessage;
import com.jme3.network.ConnectionListener;
import com.jme3.network.HostedConnection;
import com.jme3.network.Server;
import com.jme3.network.serializing.Serializable;
import com.jme3.network.serializing.Serializer;

import groovyjarjarpicocli.CommandLine.Help.Ansi.Text;

public class TestLobbyList {

    @Test
    public void testLobbyList() throws Exception {
        NostrKeyPairSigner signer = new NostrKeyPairSigner(new NostrKeyPair());

        LobbyManager lobbyList = new LobbyManager(signer, "test-lobby-list"+Math.random(), (int)System.currentTimeMillis(), 
            List.of("wss://nostr.rblb.it"),
                "wss://nostr.rblb.it");

        List<Lobby> lobbies = lobbyList.listLobbies("", 100, null).await();
        assertEquals(lobbies.size(), 0);

        Lobby lobby = lobbyList.createLobby("", Map.of("test", "test123"), Duration.ofHours(1)).await();
        System.out.println("Lobby created: " + lobby);

        assertTrue(lobby.getId().length() > 0);

        lobbies = lobbyList.listLobbies("", 100, null).await();
        assertEquals(lobbies.size(), 1);

        System.out.println("Found lobby: " + lobbies.get(0));

        assertEquals(lobbies.get(0).toString(), lobby.toString());

        
    }

 
    @Test 
    public void testDiscover() throws Exception{    
        String gameName = "test-discover"+Math.random();
        int gameVersion =(int)System.currentTimeMillis();
        
        NostrKeyPairSigner signer1 = new NostrKeyPairSigner(new NostrKeyPair(
                NostrPrivateKey.fromHex("6670510cfa1da46cb55e44393aaf9b81e96e5e5a0f06263f3252afde66b70058")));
        System.out.println("Peer1: " + signer1.getPublicKey().await().asHex());
        
        NostrKeyPairSigner signer2 = new NostrKeyPairSigner(new NostrKeyPair(
                NostrPrivateKey.fromHex("c94254ffedbdc3fe4ad2acc9dcbb5afc30a86e760d33b7c533d94ee0ea10a179")));
        System.out.println("Peer2: " + signer2.getPublicKey().await().asHex());
        AtomicBoolean discovered = new AtomicBoolean(false);
        AtomicBoolean messageReceived = new AtomicBoolean(false);
        // peer1
        {
           

            LobbyManager mng = new LobbyManager(signer1,
                    gameName , gameVersion , List.of("wss://nostr.rblb.it"),
                    "wss://nostr.rblb.it");

            LocalLobby lobby = mng.createLobby("abc", Map.of("test", "test123"), Duration.ofHours(1)).await();
            lobby.setData("test2", "test123");

            P2PChannel chan = mng.connectToLobby(lobby, "abc");
            chan.addMessageListener((c, m) -> {
                System.out.println("(1)Received message: " + m);
                if (m instanceof ChatMessage) {
                    System.out.println("Received message: " + ((ChatMessage) m).getData());
                    if (((ChatMessage) m).getData().equals("Hello from peer2")) {
                        messageReceived.set(true);

                    }
                    
                }
            });
            chan.addDiscoveryListener(( var1,  var2,  var3) -> {
                System.out.println("(1)Discovered: " + var1);
            });
            chan.addConnectionListener(new ConnectionListener() {

                @Override
                public void connectionAdded(Server server, HostedConnection conn) {
                    System.out.println("(1)New connection: " + conn.getId());
              
                }

                @Override
                public void connectionRemoved(Server server, HostedConnection conn) {
                    System.out.println("(1)Connection removed: " + conn.getId());
                }
                
            });
            
            Serializer.registerClass(ChatMessage.class);

            chan.start();
        }

        // peer2
        {
    

            LobbyManager mng = new LobbyManager(signer2,
                    gameName,
                    gameVersion, List.of("wss://nostr.rblb.it"), "wss://nostr.rblb.it");
                
            List<Lobby> lobbies = mng.listLobbies("", 100, null).await();
            assertEquals(lobbies.size(), 1);

            System.out.println("Found lobby: " + lobbies.get(0));

            P2PChannel chan = mng.connectToLobby(lobbies.get(0), "abc");

        
            chan.addMessageListener((c, m) -> {
                System.out.println("(2)Received message: " + m);
            });
            chan.addDiscoveryListener((var1, var2, var3) -> {
                try{
                    System.out.println("(2)Discovered: " + var1);
                    if(var1.asHex().equals(signer1.getPublicKey().await().asHex())){
                        discovered.set(true);
                    }
                }catch(Exception e){
                    e.printStackTrace();
                }
            });
            chan.addConnectionListener(new ConnectionListener() {

                @Override
                public void connectionAdded(Server server, HostedConnection conn) {
                    System.out.println("(2)New connection: " + conn.getId());

                }

                @Override
                public void connectionRemoved(Server server, HostedConnection conn) {
                    System.out.println("(2)Connection removed: " + conn.getId());
                }

            });

            System.out.println("Starting peer2");
            Serializer.registerClass(ChatMessage.class);

            chan.discover();


            // wait for discovery
            long t = 0;
            while(true){
                if(discovered.get()){
                    System.out.println("Discovered peer1");
                    break;
                }
                t+=10;
                assertTrue(t < 10000);
                Thread.sleep(10);
            }

            chan.start();
            System.out.println("Peer2 started");
            chan.broadcast(new ChatMessage("Hello from peer2"));

            t=0;
            while(true){
                if(messageReceived.get()){
                    System.out.println("Message received");
                    break;
                }
                t+=10;
                assertTrue(t < 10000);
                Thread.sleep(10);
            }
            System.out.println("Peer2 finished");

            chan.close();
        }


    }
}
