package org.ngengine.components.fragments;

import java.util.List;

import org.ngengine.components.Component;
import org.ngengine.components.ComponentManager;
import org.ngengine.components.actions.ComponentActionEvent;
import org.ngengine.components.actions.ComponentActionHandler;
import org.ngengine.components.actions.ComponentActionOrigin;

import jakarta.annotation.Nullable;

/**
 * Base contract for components that expose action handlers.
 *
 * <p>Actions are dispatched through {@link ActionDispatchRuntime} and can work in
 * both offline and online sessions. Network routing is optional and only happens
 * when a network component is present.
 */
public interface ActionBasedFragment<T extends ComponentActionEvent> extends Fragment {
    /**
     * Stable route id used by action envelopes to target a specific component kind.
     */
    default String getComponentId() {
        return this.getClass().getName();
    }

    default boolean checkAuthority(){
        return true;
    }

    default void loadActions(ComponentManager mng){

    }
    
    default void unloadActions(ComponentManager mng){

    }

    /**
     * Invokes an action for this component using default routing rules.
     */
    default void invokeAction(T action) {
        ComponentActionHandler.Selection best = ComponentActionHandler.selectBest(
            this,
            getComponentId(),
            action,
            ComponentActionOrigin.LOCAL,
            this::checkAuthority,
            () -> true
        );
        if (best == null) {
            return;
        }
        best.getHandler().invoke(this, action);
    }

    // /**
    //  * Invokes an action and optionally requests routing to a specific remote player.
    //  */
    // default boolean invokeAction(@Nullable RemotePeer target, T action) {
    //     MultiplayerManagerComponent net = null;
    //     if(this instanceof Component){
    //         Component cmp = (Component)this;
    //         net = cmp.getInstanceOf(MultiplayerManagerComponent.class);
    //     }
    //     message.setComponentId(getComponentId());
     
    //     if (net != null){
    //         if(target == null){
    //             net.sendMessageBroadcast(
    //                 message, 
    //                 message.getChannel(),
    //                 message.isReliable()
    //             );
    //         } else {
    //             net.sendMessageToPeer(
    //                 target, 
    //                 message, 
    //                 message.getChannel(),
    //                 message.isReliable()
    //             );
    //         }
    //     }
    //     return true;
    // }

    // default void mount(){
    //     MultiplayerManagerComponent net = null;
    //     if(this instanceof Component){
    //         Component cmp = (Component)this;
    //         net = cmp.getInstanceOf(MultiplayerManagerComponent.class);
    //     }
    //     if(net!=null){
    //         net.registerMessageListener(null, im->{
    //             Message message = im.getMessage();
    //             if(!(message instanceof ActionMessage)){
    //                 return;
    //             }
    //             ActionMessage actionMessage = (ActionMessage)message;
    //             if(!getComponentId().equals(actionMessage.getComponentId())){ 
    //                 // not the droid we're looking for
    //                 return;
    //             }
    //             RemotePeer from = im.getFromPeerId();

    //             ComponentActionEventHandler.Selection best = ComponentActionEventHandler.selectBest(
    //                 this,            
    //                 getComponentId(),
    //                 message,
    //                 ComponentActionEventOrigin.REMOTE,
    //                 (f)->{
    //                     // if has network component
    //                     // else
    //                     return true;
    //                 }
    //             );

    //             best.getHandler().invoke(this, from, message);
    //         });
    //     }
    // }

    // default void unmount(){
    //     MultiplayerManagerComponent net = null;
    //     if(this instanceof Component){
    //         Component cmp = (Component)this;
    //         net = cmp.getInstanceOf(MultiplayerManagerComponent.class);
    //     }
    //     if(net!=null){
    //         net.unregisterMessageListener(null);
    //     }
    // }
}
