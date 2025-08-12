package org.ngengine.ads;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;
import java.util.logging.Logger;

import org.ngengine.components.Component;
import org.ngengine.components.ComponentManager;
import org.ngengine.components.fragments.AssetLoadingFragment;
import org.ngengine.components.fragments.LogicFragment;
import org.ngengine.components.fragments.MainViewPortFragment;
import org.ngengine.components.fragments.RenderFragment;
import org.ngengine.nostr4j.NostrPool;
import org.ngengine.nostr4j.NostrRelay;
import org.ngengine.nostr4j.keypair.NostrKeyPair;
import org.ngengine.nostr4j.keypair.NostrPrivateKey;
import org.ngengine.nostr4j.keypair.NostrPublicKey;
import org.ngengine.nostr4j.signer.NostrKeyPairSigner;
import org.ngengine.nostrads.client.negotiation.NegotiationHandler;
import org.ngengine.nostrads.client.services.PenaltyStorage;
import org.ngengine.nostrads.client.services.display.AdsDisplayClient;
import org.ngengine.nostrads.client.services.display.Adspace;
import org.ngengine.nostrads.protocol.AdBidEvent;
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

import com.jme3.renderer.RenderManager;


public class ImmersiveAdComponent implements Component<Object>, LogicFragment, AssetLoadingFragment, MainViewPortFragment , RenderFragment{
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
    private List<AdTaxonomy.Term> defaultCategories;
    private List<String> defaultLanguages;
    private final NostrPool pool;
    private ImmersiveAdViewer viewer;
    private RenderManager renderManager;

    public ImmersiveAdComponent(
        List<String> relays,
        NostrPublicKey appKey,
        @Nullable NostrPrivateKey userAdKey 
    ){
        this.appKey = appKey;
        this.userAdKey = userAdKey;
        this.pool = new NostrPool();
        for(String relay : relays) {
            pool.connectRelay(new NostrRelay(relay));
        }

    }

    public void receiveRenderManager(RenderManager renderer) {
        this.renderManager = renderer;
      
    }
    public void setViewer(ImmersiveAdViewer viewer) {
        this.viewer = viewer;
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
 
    @Override
    public void loadAssets(AssetManager assetManager, DataStore assetCache, Consumer<Object> preload) {
        this.assetManager = assetManager;
    }

    @Override
    public void receiveMainViewPort(ViewPort viewPort) {
        mainCamera = viewPort.getCamera();
       
        
    }
 
    @Override
    public void updateAppLogic(float tpf) {
        viewer.beginUpdate();
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
                if(!viewer.isVisible(space)) {
                    continue; // Skip if not visible
                }
                
                AdBidEvent currentBid = space.get();
                if(currentBid!=null){
                    viewer.showInfo(
                        space,
                        currentBid.getDescription(),
                        currentBid.getCallToAction(),
                        currentBid.getLink()
                    );
                }

                
                if(!space.isUpdateNeeded())continue;
                space.clearUpdateNeeded();
                System.out.println("Updating ad space: " + space);

                Adspace aspace = getAdSpace(space);
                this.displayClient.registerAdspace(aspace);
                
                this.displayClient.loadNextAd(
                    aspace, 
                    space.getSize().getWidth(), 
                    space.getSize().getHeight(), 
                (bid)->{
                    NostrPublicKey bidAuthor = bid.getPubkey();
                    // TODO
                    // if(isAdvertiserListBlack&&advertisersList!=null&&advertisersList.contains(bidAuthor)){
                    //     return NGEPlatform.get().wrapPromise((res,rej)->{
                    //         res.accept(false);
                    //     });
                    // } 
                    return NGEPlatform.get().wrapPromise((res,rej)->{
                        res.accept(true);
                    });
                }, ( bidEvent,  offer)->{         
                    System.out.println("Ad loaded: " + bidEvent.getId() );        
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
                
                            space.set(renderManager, assetManager, bidEvent, mainRunner);
                        });                        
                    });
                }, ( neg,  offer,  success,  message)->{

                    System.out.println("Completed "+success+" "+message);
                }).catchException((err)->{
                    logger.warning("Error loading ad: " + err.getMessage());
                    space.setUpdateNeeded();
                });

            }
            
        }
        viewer.endUpdate();
    }

    @Override
    public void onEnable(ComponentManager mng, Runner runner, DataStoreProvider dataStore, boolean firstTime, Object arg) {
        this.mainRunner = runner;
        DataStore store = dataStore.getDataStore("nostrads");

       if(viewer == null){
            viewer = new ImmersiveAdCameraView(mng, mainCamera);
        }

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
        displayClient = new AdsDisplayClient(pool, signer, taxonomy, penaltyStorage, (neg, offer, reason) -> {
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
