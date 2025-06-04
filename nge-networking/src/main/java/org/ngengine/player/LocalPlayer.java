package org.ngengine.player;

import java.util.concurrent.ExecutionException;

import org.ngengine.nostr4j.event.UnsignedNostrEvent;
import org.ngengine.nostr4j.nip24.Nip24ExtraMetadata;
import org.ngengine.nostr4j.nip39.Nip39ExternalIdentities;
import org.ngengine.nostr4j.signer.NostrSigner;

public class LocalPlayer extends Player {
    private final NostrSigner signer;

    protected LocalPlayer(PlayerManagerComponent mng, NostrSigner signer)
            throws InterruptedException, ExecutionException {
        super(mng, signer.getPublicKey().await());
        this.signer = signer;
    }

   protected LocalPlayer(Player player,  NostrSigner signer) throws InterruptedException, ExecutionException{
        this(player.playerManager, signer);
        this.pubkey = player.pubkey;
        this.gamerTag = player.gamerTag;
        this.name = player.name;
        this.metadata = player.metadata;
        this.image = player.image;
    }
  
    public void setGamerTag(String name) {
                ensureReady();        
        Nip24ExtraMetadata meta = getMetatada();
        Nip39ExternalIdentities ids = new Nip39ExternalIdentities(meta);
        if(name==null){
            ids.removeExternalIdentity("gamertag");
        }else{
            GamerTag newTag = GamerTag.generate(getUID(), name);
            ids.setExternalIdentity("gamertag", newTag.toString(), null);
        }
        UnsignedNostrEvent uevent = ids.toUpdateEvent();
        signer.sign(uevent).then((signerEvent)->{
            getPlayerManager().getPool().send(signerEvent);
            return null;
        });
        
        resetCached();
    }

    // TODO add upload service
    // public void setImage(Texture2D image)  {
    //     ensureReady();        
    //     Nip24ExtraMetadata meta = getMetatada();
    //     meta.setPicture(url);
    //     UnsignedNostrEvent uevent = meta.toUpdateEvent();
    //     signer.sign(uevent).then((signerEvent) -> {
    //         getPlayerManager().getPool().send(signerEvent);
    //         return null;
    //     });
    //     resetCached();
    // }
}
