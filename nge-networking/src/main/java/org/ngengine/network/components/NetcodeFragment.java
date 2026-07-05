package org.ngengine.network.components;

import java.math.BigInteger;
import java.util.LinkedHashSet;
import java.util.Set;
import org.ngengine.components.Component;
import org.ngengine.components.ComponentManager;
import org.ngengine.components.fragments.ActionBasedFragment;
import org.ngengine.network.RemotePeer;
import org.ngengine.nostr4j.keypair.NostrPublicKey;

public interface NetcodeFragment  extends ActionBasedFragment<ActionMessage> {
    
    public BigInteger getNetworkId();

   

    @Override
    default boolean checkAuthority(){
        NetcodeManagerComponent net = null;
        if(this instanceof Component){
            Component cmp = (Component)this;
            if (cmp.getComponentManager() != null) {
                net = cmp.getInstanceOf(NetcodeManagerComponent.class);
            }
        }
        if(net==null) return true;
        BigInteger networkId = getNetworkId();
        if (networkId == null || networkId.signum() < 0) {
            return true;
        }
        NostrPublicKey localPeer = net.getLocalPeerPublicKey();
        return NetcodeAuthorityAssignment.hasAuthority(
            localPeer,
            networkId,
            net.getKnownPeerPublicKeys(),
            localPeer
        );
    }

    default boolean checkAuthority(RemotePeer peer){
        NetcodeManagerComponent net = null;
        if(this instanceof Component){
            Component cmp = (Component)this;
            if (cmp.getComponentManager() != null) {
                net = cmp.getInstanceOf(NetcodeManagerComponent.class);
            }
        }
        if(net==null) return true;
        if (peer == null || peer.getRemotePeer() == null || peer.getRemotePeer().getPubkey() == null) {
            return false;
        }
        Set<org.ngengine.nostr4j.keypair.NostrPublicKey> knownPeers = new LinkedHashSet<>(net.getKnownPeerPublicKeys());
        knownPeers.add(peer.getRemotePeer().getPubkey());
        NostrPublicKey localPeer = net.getLocalPeerPublicKey();
        return NetcodeAuthorityAssignment.hasAuthority(
            peer.getRemotePeer().getPubkey(),
            getNetworkId(),
            knownPeers,
            localPeer
        );
    }

    @Override
    default void loadActions(ComponentManager mng){
        ActionBasedFragment.super.loadActions(mng);

        NetcodeManagerComponent net = null;
        if(this instanceof Component){
            Component cmp = (Component)this;
            net = cmp.getInstanceOf(NetcodeManagerComponent.class);
        }
        if(net==null) return;
        net.registerActionHandler(this);
    }
    
    @Override
    default void unloadActions(ComponentManager mng){
        ActionBasedFragment.super.unloadActions(mng);
        NetcodeManagerComponent net = null;
        if(this instanceof Component){
            Component cmp = (Component)this;
            net = cmp.getInstanceOf(NetcodeManagerComponent.class);
        }
        if(net==null) return;
        net.unregisterActionHandler(this);
    }

 
    @Override
    default void invokeAction(ActionMessage message) {
        this.invokeAction(ActionMessage.LOCAL_PEER, message);
    }


    default void invokeAction(RemotePeer target, ActionMessage message) {
        message.setComponentId(getComponentId());
        message.setSource(ActionMessage.LOCAL_PEER);     
        message.setNetworkId(getNetworkId());

        ActionBasedFragment.super.invokeAction(message);      
        NetcodeManagerComponent net = null;
        if(this instanceof Component){
            Component cmp = (Component)this;
            net = cmp.getInstanceOf(NetcodeManagerComponent.class);
        }
        if(net==null) return;

       
        if(target == null){
            net.sendMessageBroadcast(
                message, 
                message.getChannel(),
                message.isReliable()
            );
        } else {
            net.sendMessageToPeer(
                target, 
                message, 
                message.getChannel(),
                message.isReliable()
            );
        }
        
    }


	default <T extends SnapshotMessage> void onSnapshot(T actionMessage){
    }


    default <T extends SnapshotMessage> T requestSnapshot(RemotePeer target){
        return null;
    }
    
    default NetcodeBehavior getNetworkBehavior(){
        return NetcodeBehavior.DEFAULT;
    }
 
    private NetcodeDespawnActionMessage requestRemoteDespawn() {
        NetcodeDespawnActionMessage message = new NetcodeDespawnActionMessage();
        message.setComponentId( getComponentId());
        message.setSource(ActionMessage.LOCAL_PEER);
        message.setNetworkId(getNetworkId());

        ActionBasedFragment.super.invokeAction(message);
        NetcodeManagerComponent net = null;
        if (this instanceof Component) {
            Component cmp = (Component) this;
            net = cmp.getInstanceOf(NetcodeManagerComponent.class);
        }
        if (net == null) return null;
        net.sendMessageBroadcast(message, message.getChannel(), message.isReliable());
        return message;
    }
}
