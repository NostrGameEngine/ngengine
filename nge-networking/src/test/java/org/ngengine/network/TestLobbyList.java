/**
 * Copyright (c) 2025, Nostr Game Engine
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 * 
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 
 * 3. Neither the name of the copyright holder nor the names of its
 *    contributors may be used to endorse or promote products derived from
 *    this software without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 * 
 * Nostr Game Engine is a fork of the jMonkeyEngine, which is licensed under
 * the BSD 3-Clause License. The original jMonkeyEngine license is as follows:
 */
package org.ngengine.network;

public class TestLobbyList {
    // @Test
    // public void testLobbyList() throws Exception {
    // NostrKeyPairSigner signer = new NostrKeyPairSigner(new NostrKeyPair());

    // LobbyManager lobbyList = new LobbyManager(
    // signer,
    // "test-lobby-list" + Math.random(),
    // (int) System.currentTimeMillis(),
    // List.of("wss://nostr.rblb.it"),
    // "wss://nostr.rblb.it"
    // );

    // List<Lobby> lobbies = listLobbies(lobbyList, "", 100, null).await();
    // assertEquals(lobbies.size(), 0);

    // Lobby lobby = createLobby(lobbyList, "", Map.of("test", "test123"), Duration.ofHours(1)).await();
    // System.out.println("Lobby created: " + lobby);

    // assertTrue(lobby.getId().length() > 0);

    // lobbies = listLobbies(lobbyList, "", 100, null).await();
    // assertEquals(lobbies.size(), 1);

    // System.out.println("Found lobby: " + lobbies.get(0));

    // assertEquals(lobbies.get(0).toString(), lobby.toString());
    // }

    // @Test
    // public void testDiscover() throws Exception {
    // String gameName = "test-discover" + Math.random();
    // int gameVersion = (int) System.currentTimeMillis();

    // NostrKeyPairSigner signer1 = new NostrKeyPairSigner(
    // new
    // NostrKeyPair(NostrPrivateKey.fromHex("6670510cfa1da46cb55e44393aaf9b81e96e5e5a0f06263f3252afde66b70058"))
    // );
    // System.out.println("Peer1: " + signer1.getPublicKey().await().asHex());

    // NostrKeyPairSigner signer2 = new NostrKeyPairSigner(
    // new
    // NostrKeyPair(NostrPrivateKey.fromHex("c94254ffedbdc3fe4ad2acc9dcbb5afc30a86e760d33b7c533d94ee0ea10a179"))
    // );
    // System.out.println("Peer2: " + signer2.getPublicKey().await().asHex());
    // AtomicBoolean discovered = new AtomicBoolean(false);
    // AtomicBoolean messageReceived = new AtomicBoolean(false);
    // // peer1
    // {
    // LobbyManager mng = new LobbyManager(
    // signer1,
    // gameName,
    // gameVersion,
    // List.of("wss://nostr.rblb.it"),
    // "wss://nostr.rblb.it"
    // );

    // LocalLobby lobby = (LocalLobby) createLobby(mng, "abc", Map.of("test", "test123"),
    // Duration.ofHours(1)).await();
    // lobby.setData("test2", "test123");

    // P2PChannel chan = mng.connectToLobby(lobby, "abc");
    // chan.addMessageListener((c, m) -> {
    // System.out.println("(1)Received message: " + m);
    // if (m instanceof TextMessage) {
    // System.out.println("Received message: " + ((TextMessage) m).getData());
    // if (((TextMessage) m).getData().equals("Hello from peer2")) {
    // messageReceived.set(true);
    // }
    // }
    // });
    // chan.addDiscoveryListener((var1, var2, var3) -> {
    // System.out.println("(1)Discovered: " + var1);
    // });
    // chan.addConnectionListener(
    // new ConnectionListener() {
    // @Override
    // public void connectionAdded(Server server, HostedConnection conn) {
    // System.out.println("(1)New connection: " + conn.getId());
    // }

    // @Override
    // public void connectionRemoved(Server server, HostedConnection conn) {
    // System.out.println("(1)Connection removed: " + conn.getId());
    // }
    // }
    // );

    // Serializer.registerClass(TextMessage.class);

    // chan.start();
    // }

    // // peer2
    // {
    // LobbyManager mng = new LobbyManager(
    // signer2,
    // gameName,
    // gameVersion,
    // List.of("wss://nostr.rblb.it"),
    // "wss://nostr.rblb.it"
    // );

    // List<Lobby> lobbies = listLobbies(mng, "", 100, null).await();
    // assertEquals(lobbies.size(), 1);

    // System.out.println("Found lobby: " + lobbies.get(0));

    // P2PChannel chan = mng.connectToLobby(lobbies.get(0), "abc");

    // chan.addMessageListener((c, m) -> {
    // System.out.println("(2)Received message: " + m);
    // });
    // chan.addDiscoveryListener((var1, var2, var3) -> {
    // try {
    // System.out.println("(2)Discovered: " + var1);
    // if (var1.asHex().equals(signer1.getPublicKey().await().asHex())) {
    // discovered.set(true);
    // }
    // } catch (Exception e) {
    // e.printStackTrace();
    // }
    // });
    // chan.addConnectionListener(
    // new ConnectionListener() {
    // @Override
    // public void connectionAdded(Server server, HostedConnection conn) {
    // System.out.println("(2)New connection: " + conn.getId());
    // }

    // @Override
    // public void connectionRemoved(Server server, HostedConnection conn) {
    // System.out.println("(2)Connection removed: " + conn.getId());
    // }
    // }
    // );

    // System.out.println("Starting peer2");
    // Serializer.registerClass(TextMessage.class);

    // chan.discover();

    // // wait for discovery
    // long t = 0;
    // while (true) {
    // if (discovered.get()) {
    // System.out.println("Discovered peer1");
    // break;
    // }
    // t += 10;
    // assertTrue(t < 10000);
    // Thread.sleep(10);
    // }

    // chan.start();
    // System.out.println("Peer2 started");
    // chan.broadcast(new TextMessage("Hello from peer2"));

    // t = 0;
    // while (true) {
    // if (messageReceived.get()) {
    // System.out.println("Message received");
    // break;
    // }
    // t += 10;
    // assertTrue(t < 10000);
    // Thread.sleep(10);
    // }
    // System.out.println("Peer2 finished");

    // chan.close();
    // }
    // }

    // private AsyncTask<List<Lobby>> listLobbies(LobbyManager mng, String words, int limit, Map<String,
    // String> tags) {
    // NGEPlatform platform = NGEPlatform.get();
    // return platform.wrapPromise((res, rej) -> {
    // mng.listLobbies(
    // words,
    // limit,
    // tags,
    // (lobbies, error) -> {
    // if (error != null) {
    // rej.accept(error);
    // } else {
    // res.accept(lobbies);
    // }
    // }
    // );
    // });
    // }

    // private AsyncTask<Lobby> createLobby(LobbyManager mng, String password, Map<String, String> data,
    // Duration duration) {
    // NGEPlatform platform = NGEPlatform.get();
    // return platform.wrapPromise((res, rej) -> {
    // mng.createLobby(
    // password,
    // data,
    // duration,
    // (lobby, error) -> {
    // if (error != null) {
    // rej.accept(error);
    // } else {
    // res.accept(lobby);
    // }
    // }
    // );
    // });
    // }
}
