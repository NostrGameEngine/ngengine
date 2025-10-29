package org.ngengine.config;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.ngengine.export.StringSavableWrapper;
import org.ngengine.nostr4j.keypair.NostrPublicKey;
import org.ngengine.platform.NGEPlatform;
import org.ngengine.platform.NGEUtils;
import org.ngengine.platform.transport.NGEHttpResponse;
import org.ngengine.store.DataStore;

import com.jme3.export.binary.ByteUtils;
import com.jme3.system.AppSettings;
import com.jme3.util.res.Resources;

public class NGEAppSettings {
    private static final Logger log = Logger.getLogger(NGEAppSettings.class.getName());
    private NostrPublicKey appId = null;
    private AppSettings settings;
    private volatile boolean restartRequired = false;
    private volatile boolean hasUnsavedChanges = false;

    private final RelayList nostr = new RelayList(
        this,
        "nostr",
        List.of(
            "wss://relay.ngengine.org",
            "wss://relay2.ngengine.org"
        )
    );

    private final RelayList blossom = new RelayList(
        this,
        "blossom",
        List.of(
            "wss://relay.ngengine.org",
            "wss://relay2.ngengine.org"
        )
    );

    public void checkRestartRequired(String key){
        Object c = settings.get("restartRequired");
        if(c instanceof Collection){
            Collection<?> cs = (Collection<?>)c;
            for(Object e : cs){
                if(e instanceof String && ((String)e).equals(key)){
                    restartRequired = true;
                    return;
                }
            }
        }
    }

    public boolean isRestartRequired(){
        return restartRequired;
    }

    public boolean hasUnsavedChanges(){
        return hasUnsavedChanges;
    }

    @SuppressWarnings("unchecked")
    private Map<Object,Object> readConfig(String path){
        try{
            if(path.startsWith("http://") || path.startsWith("https://")){
                NGEHttpResponse resp  = NGEPlatform.get().httpRequest("GET", NGEUtils.safeURI(path).toString(), null, null, null).await();
                if(!resp.status) throw new Exception("Failed to fetch config from "+path);
                String content = new String(resp.body, StandardCharsets.UTF_8);
                return NGEPlatform.get().fromJSON(content, Map.class);
            } else{
                URL url = Resources.getResource(path);
                if(url == null) throw new Exception("Failed to find config from "+path);
                byte data[] = ByteUtils.readFully(new BufferedInputStream(url.openStream()));
                String content = new String(data, StandardCharsets.UTF_8);
                return NGEPlatform.get().fromJSON(content, Map.class);
            }
        }catch(Exception ex){
            log.log(Level.WARNING, "Failed to read config from "+path, ex);
        }
        return null;
    }

    private void mergeConfig(Map<Object,Object> raw , AppSettings settings){
        if(raw != null){
            for(Object e : raw.entrySet()){
                String key = NGEUtils.safeString(((Map.Entry)e).getKey());
                Object val = ((Map.Entry)e).getValue();
                settings.put(key, val);
            }
        }
    }

    private void mergeConfig(String ngeappConfig, AppSettings settings){
        try{
            if(ngeappConfig != null && !ngeappConfig.isBlank()){            
                Map<Object,Object> raw = readConfig(ngeappConfig);
                mergeConfig(raw, settings);
            }
        }catch(Exception ex){
            log.log(Level.WARNING, "Failed to merge config from "+ngeappConfig, ex);
        }
    }

    public void save(DataStore store) throws IOException{
        store.write("ngeapp-custom.config", new StringSavableWrapper(NGEPlatform.get().toJSON(this.getJmeAppSettings())));
        hasUnsavedChanges=false;
    }

    public void load(DataStore store) throws IOException{
        StringSavableWrapper wrapper = (StringSavableWrapper) store.read("ngeapp-custom.config");
        if(wrapper != null && wrapper.get() != null && !wrapper.get().isBlank()){
            Map<Object,Object> raw = NGEPlatform.get().fromJSON(wrapper.get(), Map.class);
            AppSettings settings = this.getJmeAppSettings();
            mergeConfig(raw, settings);
            this.settings = settings;
        }
    }

    public void reset(){
        this.settings = null;
    }

    public AppSettings getJmeAppSettings(){
        if(this.settings != null) return this.settings;
        AppSettings settings = new AppSettings(false);
        mergeConfig(System.getProperty("ngeapp-default.config", "ngeapp-default.json"), settings);
        mergeConfig(System.getProperty("ngeapp.config", "ngeapp.json"), settings);
        this.settings = settings;
        return settings;
    }

    public Number getNumber(String key, Number def){
        Object val = this.getJmeAppSettings().getOrDefault(key, def);
        long v2 = NGEUtils.safeLong(val);
        double v = NGEUtils.safeDouble(val);
        if(v == v2) return v2;
        return v;
    }

    public String getString(String key, String def){
        Object val = this.getJmeAppSettings().getOrDefault(key, def);
        return NGEUtils.safeString(val);
    }

    public boolean getBoolean(String key, boolean def){
        Object val = this.getJmeAppSettings().getOrDefault(key, def);
        return NGEUtils.safeBool(val);
    }

    public void setInt(String key, long val){
        this.getJmeAppSettings().put(key, val);
        checkRestartRequired(key);
        hasUnsavedChanges=true;
    }

    public void setFloat(String key, double val){
        this.getJmeAppSettings().put(key, val);
        checkRestartRequired(key);
        hasUnsavedChanges=true;
    }

    public <T> T get(String key){
        return (T)this.getJmeAppSettings().get(key);
    }

    public void put(String key, Object val){
        this.getJmeAppSettings().put(key, val);
        checkRestartRequired(key);
        hasUnsavedChanges=true;
    }
    
    public  void setString(String key, String val){
        this.getJmeAppSettings().put(key, val);
        checkRestartRequired(key);
        hasUnsavedChanges=true;
    }

    public  void setBoolean(String key, boolean val){
        this.getJmeAppSettings().put(key, val);
        checkRestartRequired(key);
        hasUnsavedChanges=true;
    }

    public RelayList getNostrRelays(){
        return nostr;
    }
    public RelayList getBlossomRelays(){
        return blossom;
    }

    
    public NostrPublicKey getAppId(){
        if(appId == null){
            String appIdStr = this.getString("appId", "npub146wutmuxfmnlx9fcty0lkns2rpwhnl57kpes26mmt4hygalsakrsdllryz");
            if(appIdStr != null && !appIdStr.isBlank()){
                try{
                    appId = appIdStr.startsWith("npub")?NostrPublicKey.fromBech32(appIdStr):NostrPublicKey.fromHex(appIdStr);
                }catch(Exception ex){
                    log.log(Level.WARNING, "Failed to parse appId "+appIdStr, ex);
                }
            }
        }
        return appId;

    }



    
}
