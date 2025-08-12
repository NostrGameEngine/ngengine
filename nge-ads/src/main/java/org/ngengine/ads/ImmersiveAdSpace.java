package org.ngengine.ads;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import org.ngengine.nostr4j.keypair.NostrPublicKey;
import org.ngengine.nostrads.protocol.AdBidEvent;
import org.ngengine.nostrads.protocol.types.AdMimeType;
import org.ngengine.nostrads.protocol.types.AdPriceSlot;
import org.ngengine.nostrads.protocol.types.AdSize;
import org.ngengine.nostrads.protocol.types.AdTaxonomy;
import org.ngengine.platform.NGEPlatform;
import org.ngengine.runner.Runner;

import com.jme3.asset.AssetManager;
import com.jme3.asset.TextureKey;
import com.jme3.bounding.BoundingVolume;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.TextureUnitException;
import com.jme3.texture.Image;
import com.jme3.texture.Texture;
import com.jme3.util.MipMapGenerator;

public class ImmersiveAdSpace  {
    protected final List<AdMimeType> supportedMimeTypes = new ArrayList<>();
    {
        supportedMimeTypes.add(AdMimeType.IMAGE_JPEG);
        supportedMimeTypes.add(AdMimeType.IMAGE_PNG);
    }

    protected final Supplier<BoundingVolume> boundSupplier;
    protected final Function<String,String> getProperty;
    protected final  Consumer<Texture> applyTexture;

    protected volatile boolean needUpdate = true;
    protected volatile String offerId = null;
    protected volatile Consumer<Boolean> offerCallback = null;
    protected volatile AdBidEvent currentBid = null;

    public ImmersiveAdSpace(Supplier<BoundingVolume> boundSupplier, Consumer<Texture> applyTexture, Function<String,String> getProperty) {
        this.boundSupplier = boundSupplier;
        this.applyTexture = applyTexture;
        this.getProperty = getProperty;
    }

    public BoundingVolume getBounds() {
        return boundSupplier.get();
    }

   
 

    public AdSize getSize() {
        return AdSize.fromString(getProperty.apply(ImmersiveAdProperties.adspace));
    }

    public List<String> getLanguages(){
        String langStr = getProperty.apply(ImmersiveAdProperties.languages);
        if(langStr == null || langStr.isEmpty()) return null;
        return Arrays.asList(langStr.split(","));
    }

    public List<AdTaxonomy.Term> getCategories(AdTaxonomy taxonomy) {
        String catStr = getProperty.apply(ImmersiveAdProperties.categories);
        if(catStr == null || catStr.isEmpty()) return null;
        String[] ids = catStr.split(",");
        ArrayList<AdTaxonomy.Term> terms = new ArrayList<>(ids.length);
        for(int i = 0; i < ids.length; i++) {
            AdTaxonomy.Term term = taxonomy.getByPath(ids[i].trim());
            terms.add(term);
        }
        return terms;
    }

    public AdPriceSlot getPriceSlot() {
        String priceStr = getProperty.apply(ImmersiveAdProperties.priceslot);
        if(priceStr == null || priceStr.isEmpty()) return AdPriceSlot.BTC1_000;
        return AdPriceSlot.fromString(priceStr);
    }

    public List<NostrPublicKey> getAdvertisersWhitelist() {
        String whitelist = getProperty.apply(ImmersiveAdProperties.whitelist);
        if(whitelist == null || whitelist.isEmpty()) return null;
        String[] keys = whitelist.split(",");
        List<NostrPublicKey> list = new ArrayList<>(keys.length);
        for(String key : keys) {
            NostrPublicKey pk = key.startsWith("npub") ? NostrPublicKey.fromBech32(key) : NostrPublicKey.fromHex(key);
            list.add(pk);            
        }
        return list;
    }
 

    public List<AdMimeType> getSupportedMimeTypes() {
        return supportedMimeTypes;
    }

    public void setCurrentOffer(String offerId, Consumer<Boolean> callback) {
        this.offerId = offerId;
        this.offerCallback = callback;
    }

    public void confirm(){
        if(offerCallback != null){
            offerCallback.accept(true);
            offerCallback = null;
        }
    }

    public void cancel(){
        if(offerCallback != null){
            offerCallback.accept(false);
            offerCallback = null;
        }
    }


    public String getCurrentOfferId() {
        return offerId;
    }

    public void setUpdateNeeded() {
       needUpdate = true;
    }

    public boolean isUpdateNeeded() {
        return needUpdate;
    }

    public void clearUpdateNeeded() {
        needUpdate = false;
    }

    public AdBidEvent get() {
        return currentBid;
    }

    public void set(RenderManager rm, AssetManager am, AdBidEvent bid, Runner runner){
        AdMimeType mimeType = bid.getMIMEType();
        if(!getSupportedMimeTypes().contains(mimeType)){
            throw new IllegalArgumentException("Unsupported MIME type: " + mimeType);
        }

        currentBid = bid;
        switch(mimeType){
            case IMAGE_JPEG:
            case IMAGE_PNG: {
                String url = bid.getPayload();
                System.out.println("Loading ad texture from URL: " + url);
                NGEPlatform.get().httpRequest("GET", url, null, null, null).then(res->{
                    byte[] data = res.body();
                    try(ByteArrayInputStream bais = new ByteArrayInputStream(data)) {
                        TextureKey k = new TextureKey(url,false);
                        Texture tx = am.loadAssetFromStream(k, bais);
                        tx.getImage().setAsync(true);
                        MipMapGenerator.generateMipMaps(tx.getImage());
                        
                        runner.enqueue(()->{
                            try {
                                rm.preload(tx);
                            } catch (TextureUnitException e) {
                                e.printStackTrace();
                            }
                            System.out.println("Set texture for ad space: " +tx);
                            runner.enqueue(()->{
                                applyTexture.accept(tx);
                            });

                        });                                     
                    } catch (Exception e) {
                        e.printStackTrace();
                    }                   
                    return null;
                });
                break;
            }
            default: {
                throw new IllegalArgumentException("Unsupported MIME type: " + mimeType);
            }
        }
        

    }
}
