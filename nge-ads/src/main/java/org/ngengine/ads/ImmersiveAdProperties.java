package org.ngengine.ads;

import org.ngengine.platform.NGEUtils;

import com.jme3.scene.Spatial;

public class ImmersiveAdProperties {
    public static final String adspace = "nostrads.adspace"; // AdSize value as string
    public static final String categories = "nostrads.categories"; // AdTaxonomy.Term ids or paths as csv string
    public static final String languages = "nostrads.languages"; // ISO 639-1 language codes as csv string
    public static final String priceslot = "nostrads.priceslot"; // AdPriceSlot value as string
    public static final String context = "nostrads.context"; // Context string for the ad space
    public static final String whitelist = "nostrads.whitelist"; // allow only certain advertisers

    public static String getProperty(Spatial sp, String key){
        return NGEUtils.safeString(sp.getUserData(key));
    }

    public static void setProperty(Spatial sp, String key, String value){
        sp.setUserData(key, NGEUtils.safeString(value));
    }

}
