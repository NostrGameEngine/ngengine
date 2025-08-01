package org.ngengine.ads;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Logger;

import org.ngengine.components.Component;
import org.ngengine.components.ComponentManager;
import org.ngengine.components.fragments.AssetLoadingFragment;
import org.ngengine.components.fragments.LogicFragment;
import org.ngengine.components.fragments.MainViewPortFragment;
import org.ngengine.nostr4j.keypair.NostrKeyPair;
import org.ngengine.nostr4j.keypair.NostrPrivateKey;
import org.ngengine.nostr4j.keypair.NostrPublicKey;
import org.ngengine.nostr4j.signer.NostrKeyPairSigner;
import org.ngengine.nostrads.client.negotiation.NegotiationHandler;
import org.ngengine.nostrads.client.services.PenaltyStorage;
import org.ngengine.nostrads.client.services.display.AdsDisplayClient;
import org.ngengine.nostrads.client.services.display.Adspace;
import org.ngengine.nostrads.protocol.negotiation.AdOfferEvent;
import org.ngengine.nostrads.protocol.types.AdPriceSlot;
import org.ngengine.nostrads.protocol.types.AdSize;
import org.ngengine.nostrads.protocol.types.AdTaxonomy;
import org.ngengine.platform.NGEPlatform;
import org.ngengine.runner.Runner;
import org.ngengine.store.DataStore;
import org.ngengine.store.DataStoreProvider;

import com.jme3.asset.AssetManager;
import com.jme3.renderer.Camera;
import com.jme3.renderer.ViewPort;

import jakarta.annotation.Nullable;

import com.jme3.renderer.Camera.FrustumIntersect;


public class ImmersiveAdComponent implements Component<Object>, LogicFragment, AssetLoadingFragment, MainViewPortFragment{
    private static final Logger logger = Logger.getLogger(ImmersiveAdComponent.class.getName());
    private AdsDisplayClient displayClient;
    private AdTaxonomy taxonomy;
    private List<WeakReference<ImmersiveAdGroup>> groups = new ArrayList<>();
    private AssetManager assetManager;
    private Camera mainCamera;
    private Runner mainRunner;
     
    private final NostrPublicKey appKey;

    private final CopyOnWriteArrayList<ImmersiveAdListener> listeners = new CopyOnWriteArrayList<>();
    private final Map<ImmersiveAdSpace, Adspace> adspaceCache = new WeakHashMap<>();

    private NostrPrivateKey userAdKey;
    private List<NostrPublicKey> advertisersList;
    private boolean isAdvertiserListBlack = false;
    private AdPriceSlot defaultPriceSlot = AdPriceSlot.BTC1_000;
    private  List<AdTaxonomy.Term> defaultCategories;
    private  List<String> defaultLanguages;

    public ImmersiveAdComponent(
        NostrPublicKey appKey,
        @Nullable NostrPrivateKey userAdKey 
    ){
        this.appKey = appKey;
        this.userAdKey = userAdKey;

    }

    public void addListener(ImmersiveAdListener listener) {
        listeners.addIfAbsent(listener);
    }

    public void removeListener(ImmersiveAdListener listener) {
        listeners.remove(listener);
    }

    public void refreshAll(){
          for(WeakReference<ImmersiveAdGroup> ref : groups) {
            ImmersiveAdGroup group = ref.get();
            if (group != null) {
                for (ImmersiveAdSpace space : group.getSpaces()) {
                    if (space != null) {
                        space.setUpdateNeeded();
                    }
                }
            }
        }
    }

    public void setDefaultPriceSlot(AdPriceSlot priceSlot) {
        this.defaultPriceSlot = priceSlot;
        refreshAll();
    }

    public void setAdvertiserFilterList(List<NostrPublicKey> advertisersList, boolean isBlackList) {
        this.advertisersList = advertisersList;
        this.isAdvertiserListBlack = isBlackList;
        refreshAll();
    }

    public void setDefaultCategories(List<AdTaxonomy.Term> categories) {
        this.defaultCategories = categories;
        refreshAll();
    }

    public void setDefaultLanguages(List<String> languages) {
        this.defaultLanguages = languages;
        refreshAll();
    }


    

    protected Adspace getAdSpace(ImmersiveAdSpace space){
        AdSize size = space.getSize();
     
        List<NostrPublicKey> whiteList = space.getAdvertisersWhitelist();
        if(whiteList==null&&!isAdvertiserListBlack)whiteList = advertisersList;
        
        List<String> langs = space.getLanguages();
        List<AdTaxonomy.Term> cats = space.getCategories(taxonomy);
        if(langs==null)langs=defaultLanguages;        
        if(cats==null)   cats = defaultCategories;

        Adspace ispace = adspaceCache.computeIfAbsent(space, (k)->{
            for(ImmersiveAdListener listener : listeners) {
                try{
                    listener.onNewImmersiveAdspace(space);
                } catch(Exception e){
                    logger.warning("Error in listener: " + e.getMessage());
                }
            }
            AdPriceSlot priceSlot = space.getPriceSlot();        
            if(priceSlot==null) priceSlot = defaultPriceSlot;
            return new Adspace(appKey, userAdKey.getPublicKey(), size.getAspectRatio(), priceSlot,  space.getSupportedMimeTypes());
        });

        ispace.setAdvertisersWhitelist(whiteList);
        ispace.setLanguages(langs);
        ispace.setCategories(cats);

        return ispace;
    }

    public void register(ImmersiveAdGroup group){
        groups.add(new WeakReference<ImmersiveAdGroup>(group));
    }

    public void unregister(ImmersiveAdGroup group){
        groups.removeIf(ref -> ref.get() == null || ref.get() == group);
    }
 
    public void loadAssets(AssetManager assetManager) {
        this.assetManager = assetManager;
    }

    public void receiveMainViewPort(ViewPort viewPort) {
        mainCamera = viewPort.getCamera();

    }
 
    @Override
    public void updateAppLogic(float tpf) {
        Iterator<WeakReference<ImmersiveAdGroup>> it = groups.iterator();
        while (it.hasNext()) {
            WeakReference<ImmersiveAdGroup> ref = it.next();
            ImmersiveAdGroup group = ref.get();
            if (group == null) {
                it.remove();
                continue;
            }
            Iterator<ImmersiveAdSpace> spaceIt = group.getSpaces().iterator();
            while (spaceIt.hasNext()) {

                ImmersiveAdSpace space = spaceIt.next();
                if(!space.isUpdateNeeded())continue;

                FrustumIntersect frustumState = mainCamera.contains(space.getBounds());
                if(frustumState==FrustumIntersect.Outside)continue;
                
                Adspace aspace = getAdSpace(space);
              
                this.displayClient.loadNextAd(
                    aspace, 
                    space.getSize().getWidth(), 
                    space.getSize().getHeight(), 
                (bid)->{
                    NostrPublicKey bidAuthor = bid.getPubkey();
                    if(isAdvertiserListBlack&&advertisersList!=null&&advertisersList.contains(bidAuthor)){
                        return NGEPlatform.get().wrapPromise((res,rej)->{
                            res.accept(false);
                        });
                    } 
                    return NGEPlatform.get().wrapPromise((res,rej)->{
                        res.accept(true);
                    });
                }, ( bidEvent,  offer)->{                 
                    return NGEPlatform.get().wrapPromise((res, rej)->{
                        mainRunner.run(() -> {
                            for(ImmersiveAdListener listener : listeners) {
                                try{
                                    listener.onBidAssigned(space, bidEvent);
                                } catch(Exception e){
                                    logger.warning("Error in listener: " + e.getMessage());
                                }
                            }
                            space.setCurrentOffer(offer.getId(),s->res.accept(s));
                            space.set(assetManager, bidEvent, mainRunner);
                            space.clearUpdateNeeded();
                        });                        
                    });
                }, ( neg,  offer,  success,  message)->{

                }).catchException((err)->{
                    logger.warning("Error loading ad: " + err.getMessage());
                    space.setUpdateNeeded();
                });

            }
            
        }
        
    }

    @Override
    public void onEnable(ComponentManager mng, Runner runner, DataStoreProvider dataStore, boolean firstTime, Object arg) {
        this.mainRunner = runner;
        DataStore store = dataStore.getDataStore("nostrads");

      

        AdTaxonomy taxonomy = new AdTaxonomy();

        if(userAdKey==null){
            try{
                if(store.exists("adkey")){
                    userAdKey = NostrPrivateKey.fromHex(store.read("adkey"));
                } 
            }catch(Exception e){
                e.printStackTrace();
            }

            if(userAdKey == null){
                userAdKey = NostrPrivateKey.generate();
                try {
                    store.write("adkey", userAdKey.asHex());
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

        NostrKeyPairSigner signer = new NostrKeyPairSigner(new NostrKeyPair(userAdKey));
        PenaltyStorage penaltyStorage = new PenaltyStorage(store.getVStore());
        displayClient = new AdsDisplayClient(null, signer, taxonomy, penaltyStorage, (neg, offer, reason) -> {
            onAdRefresh(neg, offer, reason);
        });
        

    
    }

    @Override
    public void onDisable(ComponentManager mng, Runner runner, DataStoreProvider dataStore) {
        if (displayClient != null) {
            displayClient.close();
            displayClient = null;
        }
    }

    public void onAdRefresh(NegotiationHandler neg, AdOfferEvent offer, String reason) {
        Iterator<WeakReference<ImmersiveAdGroup>> it = groups.iterator();
        while (it.hasNext()) {
            WeakReference<ImmersiveAdGroup> ref = it.next();
            ImmersiveAdGroup group = ref.get();
            if (group == null) {
                it.remove();
                continue;
            }
            Iterator<ImmersiveAdSpace> spaceIt = group.getSpaces().iterator();
            while (spaceIt.hasNext()) {
                ImmersiveAdSpace space = spaceIt.next();
                if (space == null) {
                    spaceIt.remove();
                    continue;
                }
                if(space.getCurrentOfferId() == offer.getId()){
                    space.setUpdateNeeded();
                }
            }
        }
       
    }

    
}
