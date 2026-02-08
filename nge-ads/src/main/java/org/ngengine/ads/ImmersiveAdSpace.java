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
import com.jme3.asset.TextureKey;
import com.jme3.bounding.BoundingVolume;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.TextureUnitException;
import com.jme3.texture.Texture;
import com.jme3.util.MipMapGenerator;
import java.io.ByteArrayInputStream;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.logging.Logger;
import org.ngengine.nostr4j.keypair.NostrPublicKey;
import org.ngengine.nostrads.protocol.AdBidEvent;
import org.ngengine.nostrads.protocol.types.AdMimeType;
import org.ngengine.nostrads.protocol.types.AdPriceSlot;
import org.ngengine.nostrads.protocol.types.AdSize;
import org.ngengine.nostrads.protocol.types.AdTaxonomy;
import org.ngengine.platform.NGEPlatform;
import org.ngengine.runner.Runner;

public class ImmersiveAdSpace {

    private static final Logger logger = Logger.getLogger(ImmersiveAdSpace.class.getName());
    protected final List<AdMimeType> supportedMimeTypes = new ArrayList<>();

    {
        supportedMimeTypes.add(AdMimeType.IMAGE_JPEG);
        supportedMimeTypes.add(AdMimeType.IMAGE_PNG);
    }

    protected final Supplier<BoundingVolume> boundSupplier;
    protected final Function<String, String> getProperty;
    protected final Consumer<Texture> applyTexture;
    protected final Function<AdBidEvent, Boolean> filter;

    protected Duration loadingTimeout = Duration.ofSeconds(30);

    protected volatile boolean needUpdate = true;
    protected volatile String offerId = null;
    protected volatile Consumer<Boolean> offerCallback = null;
    protected volatile AdBidEvent currentBid = null;
    protected volatile Instant loadingSince;
    protected volatile boolean confirmed = false;

    public ImmersiveAdSpace(
        Supplier<BoundingVolume> boundSupplier,
        Consumer<Texture> applyTexture,
        Function<String, String> getProperty,
        Function<AdBidEvent, Boolean> filter
    ) {
        this.boundSupplier = boundSupplier;
        this.applyTexture = applyTexture;
        this.getProperty = getProperty;
        this.filter = filter;
    }

    public Function<AdBidEvent, Boolean> getFilter() {
        return filter;
    }

    public BoundingVolume getBounds() {
        return boundSupplier.get();
    }

    public void markLoading() {
        loadingSince = Instant.now();
    }

    public void setLoadingTimeout(Duration timeout) {
        this.loadingTimeout = timeout;
    }

    public boolean isLoading() {
        if (loadingSince == null) return false;
        return Instant.now().isBefore(loadingSince.plus(loadingTimeout));
    }

    public void openLink() {
        AdBidEvent bid = get();
        if (bid != null) {
            String link = bid.getLink();
            logger.info("Opening ad link: " + link);
            NGEPlatform.get().openInWebBrowser(link);
        }
    }

    public AdSize getSize() {
        return AdSize.fromString(getProperty.apply(ImmersiveAdProperties.adspace));
    }

    public List<String> getLanguages() {
        String langStr = getProperty.apply(ImmersiveAdProperties.languages);
        if (langStr == null || langStr.isEmpty()) return null;
        return Arrays.asList(langStr.split(","));
    }

    public List<AdTaxonomy.Term> getCategories(AdTaxonomy taxonomy) {
        String catStr = getProperty.apply(ImmersiveAdProperties.categories);
        if (catStr == null || catStr.isEmpty()) return null;
        String[] ids = catStr.split(",");
        ArrayList<AdTaxonomy.Term> terms = new ArrayList<>(ids.length);
        for (int i = 0; i < ids.length; i++) {
            AdTaxonomy.Term term = taxonomy.getByPath(ids[i].trim());
            terms.add(term);
        }
        return terms;
    }

    public AdPriceSlot getPriceSlot() {
        String priceStr = getProperty.apply(ImmersiveAdProperties.priceslot);
        if (priceStr == null || priceStr.isEmpty()) return AdPriceSlot.BTC1_000;
        return AdPriceSlot.fromString(priceStr);
    }

    public List<NostrPublicKey> getAdvertisersWhitelist() {
        String whitelist = getProperty.apply(ImmersiveAdProperties.whitelist);
        if (whitelist == null || whitelist.isEmpty()) return null;
        String[] keys = whitelist.split(",");
        List<NostrPublicKey> list = new ArrayList<>(keys.length);
        for (String key : keys) {
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

    public void confirm() {
        if(confirmed) return;
        confirmed = true;
        if (offerCallback != null) {
            offerCallback.accept(true);
            offerCallback = null;
        }
    }

    public void cancel() {
        if (offerCallback != null) {
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

    public void set(RenderManager rm, AssetManager am, AdBidEvent bid, Runner runner) {
        AdMimeType mimeType = bid.getMIMEType();
        if (!getSupportedMimeTypes().contains(mimeType)) {
            throw new IllegalArgumentException("Unsupported MIME type: " + mimeType);
        }

        currentBid = bid;
        switch (mimeType) {
            case IMAGE_JPEG:
            case IMAGE_PNG:
                {
                    String url = bid.getPayload();
                    NGEPlatform
                        .get()
                        .httpRequest("GET", url, null, null, null)
                        .then(res -> {
                            byte[] data = res.body();
                            try (ByteArrayInputStream bais = new ByteArrayInputStream(data)) {
                                TextureKey k = new TextureKey(url, false);
                                Texture tx = am.loadAssetFromStream(k, bais);
                                MipMapGenerator.generateMipMaps(tx.getImage());

                                runner.enqueue(() -> {
                                    try {
                                        rm.preload(tx);
                                    } catch (TextureUnitException e) {
                                        e.printStackTrace();
                                    }
                                    runner.enqueue(() -> {
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
            default:
                {
                    throw new IllegalArgumentException("Unsupported MIME type: " + mimeType);
                }
        }
    }
}
