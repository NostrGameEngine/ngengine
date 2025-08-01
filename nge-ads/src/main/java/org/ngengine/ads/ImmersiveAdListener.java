package org.ngengine.ads;

import org.ngengine.nostrads.protocol.AdBidEvent;

public interface ImmersiveAdListener{
    public void onNewImmersiveAdspace(ImmersiveAdSpace space);
    public void onBidAssigned(ImmersiveAdSpace space, AdBidEvent bid);
}