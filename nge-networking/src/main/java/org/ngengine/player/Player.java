package org.ngengine.player;

import java.io.ByteArrayInputStream;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.ngengine.CallbackPolicy;
import org.ngengine.nostr4j.keypair.NostrPublicKey;
import org.ngengine.nostr4j.nip24.Nip24;
import org.ngengine.nostr4j.nip24.Nip24ExtraMetadata;
import org.ngengine.nostr4j.nip39.ExternalIdentity;
import org.ngengine.nostr4j.nip39.Nip39ExternalIdentities;
import org.ngengine.platform.AsyncTask;
import org.ngengine.platform.NGEPlatform;
import com.jme3.asset.AssetManager;
import com.jme3.asset.TextureKey;
import com.jme3.texture.Texture2D;

public class Player {
    private static final Logger logger = Logger.getLogger(Player.class.getName());
    public static final List<String> SUPPORTED_PLAYER_IMAGE_EXTENSIONS = new ArrayList<>(
            List.of("png", "jpg", "jpeg", "webp"));
    public static int MAX_PLAYER_IMAGE_SIZE = 1024 * 1024 * 10; // 10MB
    protected PlayerManagerComponent playerManager;
    protected NostrPublicKey pubkey;
    protected List<Supplier<CallbackPolicy>> onUpdateCallbacks = new ArrayList<>();

    protected GamerTag gamerTag;
    protected String name;
    protected AsyncTask<Nip24ExtraMetadata> metadata;
    protected Texture2D image;

    protected Player(PlayerManagerComponent mng, NostrPublicKey pub) {
        this.pubkey = pub;
        this.playerManager = mng;
    }
    

    protected PlayerManagerComponent getPlayerManager() {
        return playerManager;
    }

    protected void callOnUpdate(){
        Iterator<Supplier<CallbackPolicy>> it = onUpdateCallbacks.iterator();
        while(it.hasNext()){
            Supplier<CallbackPolicy> callback = it.next();
            try{
                CallbackPolicy policy = callback.get();
                if(policy == CallbackPolicy.REMOVE_AFTER_CALL){
                    it.remove();
                }
            }catch(Exception e){
                logger.log(Level.WARNING, "Failed to call update callback", e);
            }
        }
        
    }

    /**
     * Reset the cached values
     */
    protected void resetCached(){
        this.gamerTag = null;
        this.name = null;
    }

    /**
     * Triggers a refresh of the player data.
     * <p>
     * The refresh runs asynchronously and can be awaited using {@link #ensureReady()}, but this is
     * discouraged as it will block the caller thread. Instead, you should use the placeholder values returned
     * by the getters and register an update listener using {@link #addUpdateListener(Runnable)} to be
     * notified when the data changes and update your logic accordingly.
     * </p>
     * 
     * <p>
     * The data will be refreshed once after you call this method and will not update automatically afterward.
     * If you need to keep the data up to date, you must call this method periodically.
     * </p>
     */
    public void refresh(){
      
        NGEPlatform platform = NGEPlatform.get();

        AssetManager assetManager = playerManager.getAssetManager();
        metadata = platform.wrapPromise((res,rej)->{
            try{
                logger.fine("Fetching metadata for player: " + pubkey.asBech32());  
                Nip24.fetch(playerManager.getPool(), pubkey).then((meta) -> {
                    logger.fine("Fetched metadata for player: " + pubkey.asBech32() + " " + meta);

                    playerManager.enqueueToRenderThread(() -> {
                        res.accept(meta);

                        resetCached();
                        callOnUpdate();
                    });
                    String imageUrl = meta.getPicture();
                    boolean validImage = imageUrl != null && !imageUrl.isEmpty()
                            && SUPPORTED_PLAYER_IMAGE_EXTENSIONS.stream()
                                    .anyMatch(ext -> imageUrl.endsWith(ext));

                    if (validImage) {
                        logger.fine("Trying to fetch image for player: " + pubkey.asBech32() + " " + imageUrl);

                        AsyncTask<byte[]> imageDataTask = platform.httpGetBytes(imageUrl, Duration.ofSeconds(15),null);
                        imageDataTask.then(imgBytes -> {
                            try{
                                if(imgBytes.length>MAX_PLAYER_IMAGE_SIZE){
                                    throw new IllegalArgumentException("Image size exceeds maximum allowed size of " + MAX_PLAYER_IMAGE_SIZE + " bytes");
                                }
                                logger.fine("Fetched " + imgBytes.length + " bytes for image: " + imageUrl+ " for player: " + pubkey.asBech32());
                                logger.fine("Attempting to load image for player: " + pubkey.asBech32() + " " + imageUrl);

                                Texture2D texture = (Texture2D) assetManager.loadAssetFromStream(new TextureKey(imageUrl), new ByteArrayInputStream(imgBytes));
                                playerManager.enqueueToRenderThread(() -> { 
                                    try{
                                        getImage().setImage(texture.getImage());
                                
                                    }catch(Exception e){
                                        logger.log(Level.WARNING, "Failed to set image", e);                                
                                    }
                                    // we got an image, hopefully
                                    resetCached();
                                    callOnUpdate();
                                });
                            }catch(Exception e){
                                // no problem, we'll live without an image.
                                logger.log(Level.FINE, "Failed to load image", e);
                            }
                            return null;
                        });
                    } else {
                        // we don't even need an image, how lucky
                        logger.fine(
                                "Player " + pubkey.asBech32() + " has no valid image, using default");
                       
                    }
                    return null;
                });
            }catch(Exception e){
                // how did we get here?
                logger.log(Level.WARNING, "Failed to fetch metadata", e);
                
                rej.accept(e);
            }
        });

      
     }

    /**
     * Waits for the player to be fully updated and ready.
     * <p>
     * This is a blocking operation and should generally be avoided. Instead, use the placeholder values
     * returned by the getters and register an update listener using {@link #addUpdateListener(Runnable)} to
     * be notified when the data changes and update your logic accordingly.
     * </p>
     */
    public void ensureReady(){
        try{
            if(!metadata.isDone()) metadata.await();
        }catch(Exception e){
            logger.log(Level.SEVERE, "Failed to ensure ready", e);
        }        
    }

    /**
     * Return the metadata if they are fetched and ready to be used.
     * Otherwise, return null.
     * @return the metadata if they are ready, null otherwise.
     */
    public Nip24ExtraMetadata getMetatada(){
        try{
            if(metadata.isDone()){
                return metadata.await();
            }else{
                return null;
            }
        }catch(Exception e){
            return null;
        }
    }

    /**
     * Return the unique user id (bech32 of the pubkey).
     * Can be used to uniquely identify the user.
     * @return the unique user id (bech32 of the pubkey).
     */
    public String getUID() {
        return pubkey.asBech32();
    }

    /**
     * Return the best name for the user.
     * 
     * This can be used to display the user name in the UI or to other players.
     * It will preferably return the gamertag if set, otherwise it will return the first
     * name-like field from the user metadata event.
     * 
     * If metadata are not available yet, this will return a shortned version of the UID.
     * 
     * Note: this can change at any time, even with no user action, as more info reach the client.
     * @return the best name for the user.
     */
    public String getName(){
        if(this.name!=null) return this.name;
        Nip24ExtraMetadata meta = getMetatada();
        if(meta != null){
            String name = null;
            GamerTag gamerTag = getGamerTag();
            if(gamerTag != null){
                name = gamerTag.getTag();
            }
            if(name==null|| name.isEmpty()){
                name = meta.getDisplayName();
            }
            if(name==null|| name.isEmpty()){
                name = meta.getName();
            }
            if(name==null|| name.isEmpty()){
                name = getUID();
            }
            this.name = name;
            return name;             
        }else{
            String fullUid =  getUID();
            // make 32 char long by removing middle chars
            int halfDesiredLength = 32/2;
            int fullLength = fullUid.length();             
            String shortUid = fullUid.substring(0, halfDesiredLength) + "..." + fullUid.substring(fullLength - halfDesiredLength, fullLength);
            this.name = shortUid;
            return shortUid;
        }
    }


    /**
     * Return the gamertag if set, null otherwise.
     * If the metadata are not available yet, this will return null.
     * 
     * The gamertag is a special uncommon identifier for the player, it can be used
     * to search for a player, or as display name, but shouldn't be trusted to be an unique identifier,
     * for that use {@link #getUID()}. 
     * 
     * @return the gamertag if set and available, null otherwise.
     */
    public GamerTag getGamerTag() {
        if(this.gamerTag!=null) return this.gamerTag;
        Nip24ExtraMetadata meta = getMetatada();
        if(meta != null){
            Nip39ExternalIdentities ids = new Nip39ExternalIdentities(meta);
            GamerTag gamerTag = null;
            if(gamerTag == null ){
                ExternalIdentity id = ids.getExternalIdentity("gamertag");
                try{
                    gamerTag = GamerTag.parse(getUID(), id.getIdentity());
                }catch(Exception e){
                    return null;
                }
                this.gamerTag = gamerTag;
            }             
            return this.gamerTag;
        }
        return null;
    }



    /**
     * Returns the image of the player.
     * <p>
     * This method returns a {@code Texture2D} object that can be used to display the player's image.
     * Initially, it will contain a default image, which will be replaced with the actual player image once it
     * is loaded from the network.
     * </p>
     * <p>
     * You can use this in a {@link com.jme3.material.Material} and it will be automatically updated.
     * Otherwise, you should register an update listener and implement your own update logic.
     * </p>
     * 
     * @return the image of the player.
     */
    public Texture2D getImage() { 
        AssetManager assetManager = playerManager.getAssetManager();
        if(image==null){
            // load a default image
            int c = Math.abs(this.pubkey.asBech32().hashCode())%5;
            image = (Texture2D) assetManager.loadTexture("defaultPlayerImages/"+c+".png");
            image.setMagFilter(Texture2D.MagFilter.Bilinear);
            image.setMinFilter(Texture2D.MinFilter.Trilinear);
            image.setWrap(Texture2D.WrapMode.EdgeClamp);           
        }
        return image;
    }

    /**
     * Add an update listener that will be called when the player data is updated.
     * @param r 
     * 
     */
    public void addUpdateListener(Supplier<CallbackPolicy> r){
        onUpdateCallbacks.add(r);
    }

    /**
     * Remove an update listener that was previously added.
     * @param r
     */
    public void removeUpdateListener(Supplier<CallbackPolicy> r){
        onUpdateCallbacks.remove(r);
    }
    
}
