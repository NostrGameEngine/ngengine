/**
 * Copyright (c) 2025-2026, Nostr Game Engine
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
 * the BSD 3-Clause License. 
 */

package org.ngengine.network;

import com.jme3.network.HostedConnection;
import com.jme3.network.Message;
import com.jme3.network.Server;
import com.jme3.network.base.MessageProtocol;
import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.ngengine.network.protocol.DynamicSerializerProtocol;
import org.ngengine.network.protocol.messages.ClassRegistrationAckMessage;
import org.ngengine.nostr4j.rtc.NostrRTCChannel;
import org.ngengine.nostr4j.rtc.NostrRTCRoom;
import org.ngengine.nostr4j.rtc.signal.NostrRTCPeer;
import org.ngengine.platform.AsyncTask;
import org.ngengine.platform.ExecutionQueue;
import org.ngengine.platform.NGEPlatform;

public class RemotePeer implements HostedConnection {

    private static final Logger log = Logger.getLogger(RemotePeer.class.getName());

    private final P2PConnection server;
    private final NostrRTCPeer remotePeer;
    private final NostrRTCRoom room;
    private final int id;
    private final Map<String, Object> sessionData = new ConcurrentHashMap<>();
    private final MessageProtocol protocol;
    private final Map<Integer, AutoChannel> openChannels = new ConcurrentHashMap<>();

 
    private static final class AutoChannel {
        public Consumer<Void> ready;
        public Consumer<Throwable> fail;
        public ExecutionQueue queue;
        public int channel;
        
        
    }

 

    RemotePeer(  int id, NostrRTCRoom room, NostrRTCPeer localPeer, NostrRTCPeer remotePeer, P2PConnection server) {
        this.room = room;
        this.server = server;
        this.remotePeer = remotePeer;
         
        this.id = id;
        boolean side = localPeer.getPubkey().asHex().compareTo(remotePeer.getPubkey().asHex()) < 0;
        this.protocol = new DynamicSerializerProtocol(
            true,
            this::onClassRegistered,
            side ? 1L : -1L
        );
        
        
    }

    private void onClassRegistered(long id) {
        log.fine("Send registration ack for class id " + id);
        send(new ClassRegistrationAckMessage(id));
    }


    public MessageProtocol getProtocol() {
        return protocol;
    }

    public NostrRTCPeer getRemotePeer() {
        return remotePeer;
    }

    private static String channelName(int channel, boolean reliable) {
        return "nge-" + Math.max(1, channel) + (reliable ? "-r" : "-u");
    }

 

    private void autoOpen(AutoChannel achan) {
        try{
            OpenChannelMessage msg = new OpenChannelMessage(achan.channel);
            ByteBuffer buffer = protocol.toByteBuffer(msg, null);
            if (buffer == null || !buffer.hasRemaining()) {
                return;
            }
            room.send(remotePeer, buffer);
        } catch (Throwable ex) {
            log.log(Level.SEVERE, "Failed to auto-open channel " + achan.channel + " to peer " + remotePeer.getPubkey().asHex(), ex);
            achan.fail.accept(ex);
        }
    }

    void confirmOpenChannel(int channel) {
        AutoChannel achan = openChannels.get(channel);
        if(achan != null){
            log.fine("Channel " + channel + " to peer " + remotePeer.getPubkey().asHex() + " is now open");
            achan.ready.accept(null);
        } else {
            log.warning("Received confirmation for unknown channel " + channel + " from peer " + remotePeer.getPubkey().asHex());
        }
    }
 
    @Override
    public void send(Message message) {
        send(0, message);
    }

    @SuppressWarnings("unchecked")
    @Override
    public void send(int c, Message message) {
        try{
            Objects.requireNonNull(message);
            if(c < 0){
                c = 0;
            }

            int channel = c;
            ByteBuffer buffer = protocol.toByteBuffer(message, null);
            if (buffer == null || !buffer.hasRemaining()) {
                return;
            }
            if (channel == 0 && message.isReliable()) {
                room.send(remotePeer, buffer);
                return;
            }

            if(message.isReliable()){
                AutoChannel achan = openChannels.computeIfAbsent(channel, ignored -> {
                    Consumer<?> cb[]= new Consumer[2];
                    AutoChannel ac = new AutoChannel();     
                    ac.queue = NGEPlatform.get().newExecutionQueue();
                    AsyncTask<Void> task = NGEPlatform.get().wrapPromise((res,rej) -> {
                        cb[0] = res;
                        cb[1] = rej; 
                    });
                    ac.queue.enqueue((res,rej) -> {
                       task.then((v) -> {
                           res.accept(null);
                           return null;
                       }).catchException(ex -> {
                           rej.accept(ex);
                       });
                    });
                    ac.ready = (Consumer<Void>) cb[0];
                    ac.fail = (ex)->{
                        ((Consumer<Throwable>) cb[1]).accept(ex);
                        openChannels.remove(channel);
                    };
                    ac.channel = channel;
                    autoOpen(ac);
                    return ac;
                });            
                achan.queue.enqueue((res, rej) -> {
                    try{
                        NostrRTCChannel chan = room.createChannel(remotePeer, channelName(channel, true), true, true, Integer.valueOf(0), null);
                        room.send(chan, buffer);
                        res.accept(null);
                    } catch (Throwable ex) {
                        rej.accept(ex);
                    }
                });
            } else {
                NostrRTCChannel chan = room.createChannel(remotePeer, channelName(channel, false), false, false, Integer.valueOf(0), null);
                room.send(chan, buffer);      
            }
        } catch (Throwable ex) {
            log.log(Level.FINEST, "Failed to send message to peer " + remotePeer, ex);
        }
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
        return remotePeer.getPubkey().asHex();
    }

    @Override
    public void close(String reason) {
        room.disconnect(remotePeer);
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
