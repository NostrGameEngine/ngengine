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

import com.jme3.network.HostedConnection;
import com.jme3.network.Message;
import com.jme3.network.Server;
import com.jme3.network.base.MessageProtocol;
import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.ngengine.nostr4j.rtc.NostrRTCSocket;

public class RemotePeer implements HostedConnection {

    private static final Logger log = Logger.getLogger(RemotePeer.class.getName());

    private final P2PChannel server;
    private final NostrRTCSocket socket;
    private final int id;
    private final Map<String, Object> sessionData = new ConcurrentHashMap<>();
    private final MessageProtocol protocol;

    RemotePeer(int id, NostrRTCSocket socket, P2PChannel server, MessageProtocol protocol) {
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
        if (log.isLoggable(Level.FINER)) {
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
    public Object setAttribute(String name, Object value) {
        if (value == null) return sessionData.remove(name);
        return sessionData.put(name, value);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> T getAttribute(String name) {
        return (T) sessionData.get(name);
    }

    @Override
    public Set<String> attributeNames() {
        return Collections.unmodifiableSet(sessionData.keySet());
    }
}
