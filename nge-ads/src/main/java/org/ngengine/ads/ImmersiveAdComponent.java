/**
 * Copyright (c) 2025, Nostr Game Engine
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
package org.ngengine.ads;

import com.jme3.asset.AssetManager;
import com.jme3.renderer.RenderManager;
import com.jme3.util.res.Resources;

import jakarta.annotation.Nullable;

import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Function;
import java.util.logging.Logger;
import org.ngengine.ViewPortManager;
import org.ngengine.components.Component;
import org.ngengine.components.ComponentManager;
import org.ngengine.components.fragments.LogicFragment;
import org.ngengine.export.NostrPrivateKeySavableWrapper;
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

public class ImmersiveAdComponent implements Component, LogicFragment {

    private static final Logger logger = Logger.getLogger(ImmersiveAdComponent.class.getName());
    private AdsDisplayClient displayClient;
    private AdTaxonomy taxonomy;
    private List<WeakReference<ImmersiveAdGroup>> groups = new ArrayList<>();
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
    private NostrPool pool;
    private ImmersiveAdViewer viewer;
    private Function<AdBidEvent, Boolean> filter = bid -> true;
    private final Collection<String> relays;

    public ImmersiveAdComponent(
        @Nullable Collection<String> relays, 
        NostrPublicKey appKey, 
        @Nullable NostrPrivateKey userAdKey
    ) {
        this.appKey = appKey;
        this.userAdKey = userAdKey;       
        InputStream taxonomyIs = null;
        try{
            taxonomyIs = Resources.getResourceAsStream("org/ngengine/nostrads/taxonomy/nostr-content-taxonomy.csv");
        }catch(Exception e){
            logger.warning("Could not load taxonomy resource: " + e.getMessage());
        }
        try{
            this.taxonomy = new AdTaxonomy(taxonomyIs);
        }catch(Exception e){
            logger.warning("Could not load taxonomy: " + e.getMessage());
            this.taxonomy = new AdTaxonomy();
        }
        
        this.relays = relays;
    }

    @Override
    public Component newInstance() {
        return new ImmersiveAdComponent(this.relays, this.appKey, this.userAdKey);
    }

    public void setFilter(Function<AdBidEvent, Boolean> filter) {
        this.filter = filter;
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

    public void refreshAll() {
        for (WeakReference<ImmersiveAdGroup> ref : groups) {
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

    protected Adspace getAdSpace(ImmersiveAdSpace space) {
        AdSize size = space.getSize();

        List<NostrPublicKey> whiteList = space.getAdvertisersWhitelist();
        if (whiteList == null && !isAdvertiserListBlack) whiteList = advertisersList;

        List<String> langs = space.getLanguages();
        List<AdTaxonomy.Term> cats = space.getCategories(taxonomy);
        if (langs == null) langs = defaultLanguages;
        if (cats == null) cats = defaultCategories;

        Adspace ispace = adspaceCache.computeIfAbsent(
            space,
            k -> {
                for (ImmersiveAdListener listener : listeners) {
                    try {
                        listener.onNewImmersiveAdspace(space);
                    } catch (Exception e) {
                        logger.warning("Error in listener: " + e.getMessage());
                    }
                }
                AdPriceSlot priceSlot = space.getPriceSlot();
                if (priceSlot == null) priceSlot = defaultPriceSlot;
                Adspace aspace = new Adspace(
                    appKey,
                    userAdKey.getPublicKey(),
                    size.getAspectRatio(),
                    priceSlot,
                    space.getSupportedMimeTypes()
                );
                this.displayClient.registerAdspace(aspace);
                NGEPlatform
                    .get()
                    .registerFinalizer(
                        space,
                        () -> {
                            this.displayClient.unregisterAdspace(aspace);
                        }
                    );
                return aspace;
            }
        );

        ispace.setAdvertisersWhitelist(whiteList);
        ispace.setLanguages(langs);
        ispace.setCategories(cats);

        return ispace;
    }

    public void register(ImmersiveAdGroup group) {
        groups.add(new WeakReference<ImmersiveAdGroup>(group));
    }

    public void unregister(ImmersiveAdGroup group) {
        groups.removeIf(ref -> ref.get() == null || ref.get() == group);
    }

    @Override
    public void updateAppLogic(ComponentManager mng, float tpf) {
        RenderManager renderManager = mng.getGlobalInstance(RenderManager.class);
        AssetManager assetManager = mng.getGlobalInstance(AssetManager.class);

        // update and select ad in a single pass
        viewer.beginUpdate();
        viewer.beginSelection();

        Iterator<WeakReference<ImmersiveAdGroup>> it = groups.iterator();
        while (it.hasNext()) {
            WeakReference<ImmersiveAdGroup> ref = it.next();
            ImmersiveAdGroup group = ref.get();
            if (group == null) {
                it.remove();
                continue;
            }

            List<ImmersiveAdSpace> spaces = group.getSpaces();
            for (int i = 0; i < spaces.size(); i++) {
                ImmersiveAdSpace space = spaces.get(i);
                if (!viewer.isVisible(space)) {
                    continue; // Skip if not visible
                }

                if (space.getCurrentOfferId() != null) {
                    viewer.select(space);
                }

                if (space.isUpdateNeeded() && !space.isLoading()) {
                    space.markLoading();

                    Adspace aspace = getAdSpace(space);

                    Function<AdBidEvent, Boolean> filter = space.getFilter() != null ? space.getFilter() : this.filter;

                    this.displayClient.loadNextAd(
                            aspace,
                            space.getSize().getWidth(),
                            space.getSize().getHeight(),
                            bid -> {
                                NostrPublicKey bidAuthor = bid.getPubkey();
                                return NGEPlatform
                                    .get()
                                    .wrapPromise((res, rej) -> {
                                        res.accept(
                                            !(
                                                isAdvertiserListBlack &&
                                                advertisersList != null &&
                                                advertisersList.contains(bidAuthor)
                                            ) &&
                                            (filter == null || filter.apply(bid))
                                        );
                                    });
                            },
                            (bidEvent, offer) -> {
                                return NGEPlatform
                                    .get()
                                    .wrapPromise((res, rej) -> {
                                        mainRunner.run(() -> {
                                            for (ImmersiveAdListener listener : listeners) {
                                                try {
                                                    listener.onBidAssigned(space, bidEvent);
                                                } catch (Exception e) {
                                                    logger.warning("Error in listener: " + e.getMessage());
                                                }
                                            }
                                            space.setCurrentOffer(offer.getId(), s -> res.accept(s));
                                            space.set(renderManager, assetManager, bidEvent, mainRunner);
                                            space.clearUpdateNeeded();
                                        });
                                    });
                            },
                            (neg, offer, success, message) -> {}
                        )
                        .catchException(err -> {
                            space.setUpdateNeeded();
                        });
                }
            }
        }
        viewer.endUpdate();

        ImmersiveAdSpace space = viewer.endSelection();
        AdBidEvent currentBid = space == null ? null : space.get();

        if (currentBid != null) {
            viewer.showInfo(space, currentBid.getDescription(), currentBid.getCallToAction());
        } else {
            viewer.showInfo(null, "", "");
        }
    }


    public AdTaxonomy getTaxonomy() {
        return taxonomy;
    }

    @Override
    public void onEnable(ComponentManager mng, Runner runner, DataStoreProvider dataStore, boolean firstTime) {
        this.mainRunner = runner;
        this.pool = new NostrPool();
        
        Collection<String> relays = this.relays;
        if(relays==null){
            relays = mng.getSettings().getNostrRelays().get("ads");
        }
        for (String relay : relays) {
            pool.connectRelay(new NostrRelay(relay));
        }
      

        DataStore store = dataStore.getDataStore("nostrads");

        if (viewer == null) {
            viewer = new ImmersiveAdCameraView(mng, 
                mng.getGlobalInstance(ViewPortManager.class).getMainSceneViewPort().getCamera()
            );
        }

        if (userAdKey == null) {
            try {
                if (store.exists("adkey")) {
                    NostrPrivateKeySavableWrapper wrapper = (NostrPrivateKeySavableWrapper)  store.read("adkey");
                    userAdKey = wrapper.get();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            if (userAdKey == null) {
                userAdKey = NostrPrivateKey.generate();
                try {
                    store.write("adkey", new NostrPrivateKeySavableWrapper(userAdKey));
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

        NostrKeyPairSigner signer = new NostrKeyPairSigner(new NostrKeyPair(userAdKey));
        PenaltyStorage penaltyStorage = new PenaltyStorage(store.getVStore());
        displayClient =
            new AdsDisplayClient(
                pool,
                signer,
                taxonomy,
                penaltyStorage,
                (neg, offer, reason) -> {
                    onAdRefresh(neg, offer, reason);
                }
            );
    }

    @Override
    public void onDisable(ComponentManager mng, Runner runner, DataStoreProvider dataStore) {
        this.pool.close();
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
                if (space.getCurrentOfferId() == offer.getId()) {
                    space.setUpdateNeeded();
                }
            }
        }
    }
}
