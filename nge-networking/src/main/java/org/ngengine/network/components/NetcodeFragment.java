package org.ngengine.network.components;

import java.math.BigInteger;
import java.time.Duration;
import java.util.LinkedHashSet;
import java.util.Set;
import org.ngengine.components.Component;
import org.ngengine.components.ComponentManager;
import org.ngengine.components.fragments.ActionBasedFragment;
import org.ngengine.network.RemotePeer;
import org.ngengine.nostr4j.keypair.NostrPublicKey;

import jakarta.annotation.Nullable;

public interface NetcodeFragment  extends ActionBasedFragment<ActionMessage> {
    /**
     * Default time allowed for the original owner of a reserved network ID to
     * reconnect before the fragment is declared orphaned.
     */
    Duration DEFAULT_ORPHAN_GRACE_PERIOD = Duration.ofMinutes(1L);
    
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
        NostrPublicKey owner = net.resolveActiveOwnerPeerPublicKey(networkId);
        return owner != null && owner.equals(localPeer);
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

    /**
     * Returns the optional delay before this fragment is declared orphaned.
     *
     * <p>The orphan lifecycle applies only to fragments with a reserved
     * network ID. Shared and persistent IDs remain eligible for normal
     * authority reassignment and never enter this lifecycle. Returning
     * {@code null} uses the fallback configured on
     * {@link NetcodeManagerComponent}; returning {@link Duration#ZERO} enables
     * immediate cleanup. Implementations should not return a negative value.</p>
     *
     * @return the fragment-specific grace period, or {@code null} to use the
     *         manager fallback
     */
    default @Nullable Duration getNetworkOrphanGracePeriod() {
        return null;
    }

    /**
     * Handles a reserved-ID fragment whose original owner remained offline for
     * the configured grace period.
     *
     * <p>The manager invokes this hook once on every surviving replica. It
     * does not assign a new owner. Implementations should always perform their
     * local, idempotent cleanup on every replica. If cleanup also needs a
     * network-visible one-shot side effect, such as spawning a replacement or
     * broadcasting a follow-up action, only the peer for which
     * {@link NetcodeOrphanContext#isCurrentPeerCleanupCoordinator()} returns
     * {@code true} should produce that effect.</p>
     *
     * <p>If the original owner reconnects before the grace period expires, the
     * pending orphan state is cancelled and this hook is not called.</p>
     *
     * @param context immutable metadata describing the orphaned fragment and
     *                the peer selected for coordinated one-shot effects
     */
    default void onNetworkOrphaned(NetcodeOrphanContext context) {
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
